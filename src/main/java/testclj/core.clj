(ns testclj
  (:use backtype.storm.clojure backtype.storm.config)
  (:require [clojure.java.io])
  (:refer [clojure.java.io])
  (:gen-class)
  (:import [backtype.storm LocalCluster LocalDRPC]
           [clojure.java.io]))


(defspout word-spout ["sentence"]
          [conf context collector]
          (let [completed (atom true)]
            (spout
              (nextTuple []
                         (with-open [rdr (reader "/tmp/test.txt")]
                           (doseq [line (line-seq rdr)]
                             (println line)))
                         ;; nextTuple is called repeatedly, so avoid CPU spam with timeout
                         (Thread/sleep 100)
                         ;; in this case I only ever want to call this once
                         (if (deref completed)
                             (do
                               (println (str "Time for more fun? " (deref completed)))
                               (emit-spout! collector [ans])
                               ;; `not` will be called to update the value of `completed` (to false)
                               (swap! completed not)))))))


(defbolt print-bolt ["word"] [tuple collector]
         (println (str "HELLO THE WORD IS HERE: " (.getString tuple 0)))
         (ack! collector tuple))

(defn mk-topology []
  (topology

    ;; spout definitions
    {"1" (spout-spec word-spout :p 1)}

    ;; bolt definitions
    {"2" (bolt-spec
           {"1" :shuffle}
           print-bolt :p 1)}))

(defn run-local! []
  (let [cluster (LocalCluster.)]
    (println "Submitting")
    (.submitTopology cluster "word-count" {TOPOLOGY-DEBUG false} (mk-topology))
    (Thread/sleep 5000)
    (println "Shutting down")
    (.shutdown cluster)))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (run-local!))

