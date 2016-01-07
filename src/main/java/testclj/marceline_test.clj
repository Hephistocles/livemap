(ns main.java.testclj.marceline-test
  (:require [marceline.storm.trident :as t]
            [clojure.string :as string :only [split]])
  (:use [backtype.storm clojure config])
  (:import [storm.trident TridentTopology]
           [backtype.storm StormSubmitter])
  (:gen-class))

;; define a trident function which adds exclamation marks to the first element of the input tuple
(t/deftridentfn exlaimer-fn
                [tuple coll]
                (t/emit-fn coll (str (t/first tuple) "!!!!"))
                )

(defn build-topology
  "build a topology containing our trident functions embedded within a drpc stream"
  ([]
    ;; first define an empty topology
   (let [trident-topology (TridentTopology.)]
     ;; create a DRPC stream within the topology called "words"
     (-> (t/drpc-stream trident-topology "words")
         ;; for every item in the stream, add exclamation marks
         (t/each ["args"]
                 exclaimer-fn
                 ["word"])
         ;; t/each will append the result to the input tuple, so here
         ;; I project onto "word" so that the original input tuple disappears
         (t/project ["word"]))
     ;; finally "build" this topology for submission
     (.build trident-topology))))

(defn -main
  "Submit a topology to the cluster. Will be called after running `bin/storm jar`"
  ([name]
   (StormSubmitter/submitTopology name
                                  ;; for now I don't have any special config
                                  {}
                                  (build-topology))))