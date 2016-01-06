(ns main.java.testclj.marceline-test
  (:require [marceline.storm.trident :as t]
            [clojure.string :as string :only [split]])
  (:use [backtype.storm clojure config])
  (:import [storm.trident TridentTopology]
           [backtype.storm LocalDRPC LocalCluster StormSubmitter])
  (:gen-class))

(t/deftridentfn split-args
                [tuple coll]
                (t/emit-fn coll (str (t/first tuple) "%%%%"))
                (t/emit-fn coll (str (t/first tuple) "!!!!"))
                )

(defn build-topology
  ([]
   (let [trident-topology (TridentTopology.)]
     (-> (t/drpc-stream trident-topology "words")
         (t/each ["args"]
                 split-args
                 ["word"])
         (t/project ["word"]))
     (.build trident-topology)))
  ([drpc]
   (let [trident-topology (TridentTopology.)]
     (-> (t/drpc-stream trident-topology "words" drpc)
         (t/each ["args"]
                 split-args
                 ["word"])
         (t/project ["word"]))
     (.build trident-topology))))


(defn setup-local! []
  (let [cluster (LocalCluster.)
        local-drpc (LocalDRPC.)]
    (.submitTopology cluster "wordcounter"
                     {}
                     (build-topology local-drpc))
    [cluster local-drpc]))

(defn runTestQuery! []
  (let [[cluster my-drpc] (setup-local!)]
    (Thread/sleep 3000)
    (let [results (.execute my-drpc "words" "the evil vessel")]
      (println results)
      (read-line)
      (.shutdown cluster)
      (System/exit 0))))

(defn setup-remote! [name]
    (StormSubmitter/submitTopology name
     {TOPOLOGY-DEBUG true
     TOPOLOGY-WORKERS 3}
                     (build-topology)))

(defn -main
  "I don't do a whole lot ... yet."
  ([] (runTestQuery!))
  ([name]
   (setup-remote! name)))