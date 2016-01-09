(ns main.java.testclj.marceline-test
  (:require [marceline.storm.trident :as t]
            [clamq.protocol.connection :as connection :only [producer]]
            [clamq.protocol.producer :as producer]
            [clojure.string :as string :only [split]]
            [main.java.testclj.server :refer [run-server!]])
  (:use [backtype.storm clojure config]
        clamq.activemq)
  (:import [storm.trident TridentTopology]
           [backtype.storm StormSubmitter LocalCluster LocalDRPC]
           (backtype.storm.contrib.jms TridentJmsSpout)
           (javax.jms Session)
           (main.java.testclj SimpleJmsProvider SimpleTupleProducer)
           (storm.trident.testing MemoryMapState$Factory FixedBatchSpout)
           (storm.trident.operation.builtin MapGet))
  (:gen-class))


;; define a trident spout which will connect to a local JMS/ActiveMQ queue and consume messages from it
(defn mk-mq-spout []
  (doto (TridentJmsSpout.)
    (.named "jmsSpout")
    (.withJmsAcknowledgeMode Session/AUTO_ACKNOWLEDGE)
    (.withJmsProvider (SimpleJmsProvider. "tcp://localhost:61616" "testerman"))
    (.withTupleProducer (SimpleTupleProducer.))))

;; define a trident function which splits the input by spaces
(t/deftridentfn split-args
                [tuple coll]
                (let [words (string/split (t/first tuple) #" ")]
                  ;; emit a new tuple for each word in the sentence
                  (doseq [word words]
                    (t/emit-fn coll word))))

;; define an aggregator to count words
(t/defcombineraggregator count-words
  ;; the first overload is called if there are no tuples in the partition
  ([] 0)
  ;; the second overload is called is run on each input tuple to get a value
  ([tuple] 1)
  ;; the third overload is called to combine values until there is only one left
  ([t1 t2] (+ t1 t2)))

(t/defcombineraggregator mapify
     ([] [])
     ([tuple] [{(keyword (t/get tuple :word)) (or (t/get tuple :count) 0)}])
     ([t1 t2] (concat t1 t2)))

(defn build-topology
  "build a topology containing two main streams - one reading from an activeMQ spout which will process incoming
  data (produced onto the queue by a separate REST server), and one responding to DRPC calls for querying."
  ([drpc]
    ;; first define an empty topology
   (let [trident-topology (TridentTopology.)]
     ;; the collect-stream reads data from an activemq queue, which will have content produced by our REST server
     (let [
           ;collect-stream  (t/new-stream trident-topology "word-counts" (mk-fixed-batch-spout 3))
           collect-stream (t/new-stream trident-topology "words" (mk-mq-spout))
           query-stream (t/drpc-stream trident-topology "counts" drpc)
           word-counts (-> collect-stream
                           ;; for every item in the stream split into words
                           (t/each ["message"]
                                   split-args
                                   ["word"])
                           ;; tuples will look like ["the" ["the foo the" "the foo the"]] ["foo" ["the foo the"]
                           ;; so strip the original input and just retain "the" "foo"
                           (t/project ["word"])
                           ;; Group this stream by `word`
                           (t/group-by ["word"])
                           ;; aggregate stream items using count-words and persist the results using an in-memory hashmap
                           (t/persistent-aggregate (MemoryMapState$Factory.)
                                                   ["word"]
                                                   count-words
                                                   ["count"])
                           )
           ]
       ;; finally use the drpc stream to respond to queries
       (-> query-stream
           (t/each ["args"]
                   split-args
                   ["word"])
           (t/project ["word"])
           (t/group-by ["word"])
           (t/state-query word-counts
                          ["word"]
                          (MapGet.)
                          ["count"])
           (t/aggregate ["word" "count"]
                   mapify
                   ["count-map"])
           )
       (.build trident-topology)))))

(defn -main
  "Submit a topology to the cluster. Will be called after running `bin/storm jar`"

  ([] ; this is for running locally

    ;; set up the cluster and build the topology
   (println "Setting up...")
   (let [cluster (LocalCluster.)
         drpc (LocalDRPC.)]
     (.submitTopology cluster "wordcounter"
                      {}
                      (build-topology drpc))

     ;; since it's local we can test it. Stuff comes from an activemq queue, so we can produce content here.
     (Thread/sleep 4000)
     (println "Ready!")
     (run-server! drpc)))

  ([name] ; this is for running on the cluster
   (StormSubmitter/submitTopology name
                                  ;; for now I don't have any special config
                                  {}
                                  (build-topology nil))))

(-main)
