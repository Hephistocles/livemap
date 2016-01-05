(ns testclj
  (:use backtype.storm.clojure backtype.storm.config)
  (:gen-class)
  (:import [backtype.storm LocalCluster]))


(defspout word-spout ["sentence"]
          [conf context collector]
          (let [completed (atom true)]
            (spout
              (nextTuple []
                         ;; nextTuple is called repeatedly, so avoid CPU spam with timeout
                         (Thread/sleep 100)
                         ;; in this case I only ever want to call this once
                         (if (deref completed)
                           (do
                             (println (str "\n\nTime for more fun? " (deref completed)))
                             (emit-spout! collector ["Yep Christopher Hephistocles"])
                             ;; `not` will be called to update the value of `completed` (to false)
                             (swap! completed not)))))))


(defbolt print-bolt ["word"] [tuple collector]
         (println (str "HELLO THE WORD IS HERE: " (.getString tuple 0) "\n\n"))
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
    (.submitTopology cluster "word-count" {TOPOLOGY-DEBUG false} (mk-topology))
    (Thread/sleep 5000)
    (.shutdown cluster)))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (run-local!))

(-main)
