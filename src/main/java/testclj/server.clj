(ns main.java.testclj.server
  (:require [compojure.core :refer :all]
            [compojure.handler :as handler]
            [ring.middleware.json :as middleware]
            [compojure.route :as route]
            [clamq.protocol.connection :as connection]
            [clamq.protocol.producer :as producer]
            [cheshire.core :refer :all])
  (:use [ring.adapter.jetty]
        clamq.activemq)
  (:import (backtype.storm.utils DRPCClient)))

(def connection (activemq-connection "tcp://localhost:61616"))
(def producer (connection/producer connection))

(defn post-data [req]
  (if-let [msg (or (get-in req [:params :msg])
                            (get-in req [:body :msg]))]
             (do (producer/publish producer "testerman" msg)
                 {:status 200
                  :body   {:msg msg :success true}})
             {:status 401
              :body   {:success false :desc "No message was received"}}))

(defn mk-query-data [drpc]
  (fn [req]
     (if-let [qry (or (get-in req [:params :query])
                      (get-in req [:body :query]))]
       (let [json-results (parse-string (.execute drpc "counts" qry)
                                        ;; this is ugly but I'm fed up of trying to get keywords mapping to strings correctly!
                                        (fn [k] (subs k 1)))]
         {:status 200
          :body   {:counts
                   ;; this is ugly too but again cba
                   (first (first json-results))}})
       {:status 401
        :body   {:success false :desc "No message was received"}})))

(defn mk-routes [drpc]
  (routes
    (POST "/" [] post-data)
    (GET "/" [] (mk-query-data drpc))
    (route/not-found "Not Found")
    ))

(defn mk-server [drpc]
  (let [app-routes (mk-routes drpc)
        app
          (-> (handler/site app-routes)
              (middleware/wrap-json-body {:keywords? true})
              middleware/wrap-json-response )
        ]
    (run-jetty app {:port 3000 :join? false}))
  )

(defn run-server! [^DRPCClient drpc]
  (let [server (mk-server drpc)]
    (.start server)
    (println "Press Enter to quit.")
    (read-line)
    (.stop server)
    (System/exit 0)))
