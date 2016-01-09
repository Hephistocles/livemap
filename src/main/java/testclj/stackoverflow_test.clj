(ns main.java.testclj.stackoverflow-test
  (:require [marceline.storm.trident :as t]
            [clojure.string :as string :only [split]]
            [marceline.storm.trident :as t])
  (:use [backtype.storm clojure config])
  (:import [storm.trident TridentTopology]
           [backtype.storm StormSubmitter LocalDRPC LocalCluster]
           (storm.trident.testing MemoryMapState$Factory FixedBatchSpout))
  (:gen-class))


;; define an aggregator to count words
(t/defcombineraggregator
  count-words
  ;; the first overload is called if there are no tuples in the partition
  ([] 0)
  ;; the second overload is called is run on each input tuple to get a value
  ([tuple] 1)
  ;; the third overload is called to combine values until there is only one left
  ([t1 t2] (+ t1 t2)))

(t/deftridentfn split-args
                [tuple coll]
                (when-let [args (t/first tuple)]
                  (let [words (string/split args #" ")]
                    (doseq [word words]
                      (t/emit-fn coll word)))))

(t/deftridentfn exclaim
                [tuple coll]
                (when-let [args (t/first tuple)]
                      (t/emit-fn coll (str args "!!!"))))

(defn build-topology
  "build a topology containing our trident functions embedded within a drpc stream"
  ([drpc]
   (let [trident-topology (TridentTopology.)
     (let [
           ;; ### Two alternatives here ###
           ;collect-stream (t/new-stream trident-topology "words" (mk-fixed-batch-spout 3))
           collect-stream (t/drpc-stream trident-topology "words" drpc)
           ]
       (-> collect-stream
           (t/each ["args"]
                   split-args
                   ;exclaim
                   ["word"]))
       (-> collect-stream
           (t/each ["args"]
                   exclaim
                   ;split-args
                   ["word"])
           (t/persistent-aggregate (MemoryMapState$Factory.)
                                   ["word"]
                                   count-words
                                   ["count"])
           ;(t/new-values-stream)
           )
       (.build trident-topology)))))

(defn -main
  "Submit a topology to the cluster. Will be called after running `bin/storm jar`"
  ([] ; this is for running locally
   (let [cluster (LocalCluster.)
         drpc (LocalDRPC.)]
     (.submitTopology cluster "wordcounter"
                      {}
                      (build-topology drpc))
     (Thread/sleep 3000)
     (println "What would you like to process?")
     (let [results (.execute drpc "words" (read-line))]
       (.shutdown cluster)
       (println results)
       (System/exit 0))))
  ([name] ; this is for running on the cluster
   (StormSubmitter/submitTopology name
                                  ;; for now I don't have any special config
                                  {}
                                  (build-topology nil))))

(-main)