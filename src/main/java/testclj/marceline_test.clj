(ns main.java.testclj.marceline-test
  (:require [marceline.storm.trident :as t]
            [clamq.protocol.connection :as connection :only [producer]]
            [clamq.protocol.producer :as producer]
            [clojure.string :as string :only [split]]
            [main.java.testclj.server :refer [run-server!]]
            [cheshire.core :refer :all]
            )
  (:use [backtype.storm clojure config]
        clamq.activemq
        clojure.math.numeric-tower)
  (:import [storm.trident TridentTopology]
           [backtype.storm StormSubmitter LocalCluster LocalDRPC]
           (backtype.storm.contrib.jms TridentJmsSpout)
           (javax.jms Session)
           (main.java.testclj SimpleJmsProvider SimpleTupleProducer PlotPoint AlphaIntensity HeatState PointArrayAggregator)
           (storm.trident.testing MemoryMapState$Factory)
           (storm.trident.operation.builtin MapGet))
  (:gen-class))


(defn interpolate
  [[r1 g1 b1] [r2 g2 b2] t]
  [(+ r1 (* t (- r2 r1)))
   (+ g1 (* t (- g2 g1)))
   (+ b1 (* t (- b2 b1)))])

(defn get-colour
  "Get an RGB thruple corresponding to the gradient at position t"
  [t]
  (cond
    (< t 0.25) [0 0 255]
    (< t 0.55) (interpolate [0 0 255] [0 255 0] (/ (- t 0.25) 0.3))
    (< t 0.85) (interpolate [0 255 0] [255 255 0] (/ (- t 0.55) 0.3))
    (< t 1) (interpolate [255 255 0] [255 0 0] (/ (- t 0.85) 0.15))
    :else [255 0 0])
  )

(t/deftridentfn
  parse-json
  [tuple coll]
  (let [j (t/first tuple)
        r (parse-string j true)
        ]
      (t/emit-fn coll (:intensity r) (:smoothing r) (:radius r) (:size r) [(:x r) (:y r)])
    ))

(t/deftridentfn
  colourise [tuple coll]
  (let [point (t/get tuple "intensity")
        [r g b] (get-colour point)]
    (t/emit-fn coll (* 255 point) r g b)))

;(t/deftridentfn
;  get-canvas-pixels [tuple coll]
;  (let [x (t/get tuple "x")
;        y (t/get tuple "y")
;        intensity (t/get tuple "intensity")
;        [r g b] (t/get tuple "coloured-intensity")
;        screen-size (t/get tuple "sizex")
;        i (* 4 (+ y (* screen-size x)))]
;    (t/emit-fn coll x y intensity r g b)
;    (t/emit-fn coll (+ 1 i) g )
;    (t/emit-fn coll (+ 2 i) b )
;    (t/emit-fn coll (+ 3 i) (* 255 intensity))
;    ))

(t/defcombineraggregator comma-join
                         ([] "")
                         ([tuple] (t/first tuple))
                         ([t1 t2] (str t1 "," t2)))

;; define a trident spout which will connect to a local JMS/ActiveMQ queue and consume messages from it
(defn mk-mq-spout []
  (doto (TridentJmsSpout.)
    (.named "jmsSpout")
    (.withJmsAcknowledgeMode Session/AUTO_ACKNOWLEDGE)
    (.withJmsProvider (SimpleJmsProvider. "tcp://localhost:61616" "testerman"))
    (.withTupleProducer (SimpleTupleProducer.))))

(t/deftridentfn parse-size
                [tuple coll]
                (let [
                      args (t/get tuple "args")
                      split (string/split args #" ")
                      sizes (map #(Integer/parseInt %) split)]
                  (t/emit-fn coll (first sizes) (second sizes))
                  ))

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
           collect-stream (t/new-stream trident-topology "updates" (mk-mq-spout))
           query-stream (t/drpc-stream trident-topology "counts" drpc)
           heatmap (-> collect-stream
                           ;; for every item in the stream split into words
                           (t/each ["message"]
                                   parse-json
                                   ["intensity" "smoothing" "radius" "size" "coords"])
                           (t/each ["intensity" "smoothing" "radius" "size" "coords"]
                                   (PlotPoint.)
                                   ["x" "y" "intensity-update"])
                           (t/group-by ["x" "y"])
                           (t/persistent-aggregate (MemoryMapState$Factory.)
                                                   ["intensity-update"]
                                                   (AlphaIntensity.)
                                                   ["full-intensity"]))
           ]
       ;; finally use the drpc stream to respond to queries
(-> query-stream
   (t/each ["args"]
           parse-size
           ["sizex" "sizey"])
   (t/state-query heatmap ["sizex" "sizey"]
                  (HeatState.)
                  ["x" "y" "intensity"])
   (t/each ["intensity"]
           colourise
           ["a" "r" "g" "b"])
   (t/aggregate ["x" "y" "a" "r" "g" "b" "sizex" "sizey"]
                (PointArrayAggregator.)
                ["image"]))
       (.build trident-topology)))))

(defn -main
  "Submit a topology to the cluster. Will be called after running `bin/storm jar`"

  ([]                                                       ; this is for running locally

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

  ([name]                                                   ; this is for running on the cluster
   (StormSubmitter/submitTopology name
                                  ;; for now I don't have any special config
                                  {}
                                  (build-topology nil))))

(-main)
