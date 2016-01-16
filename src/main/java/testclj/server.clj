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
  (if-let [msg (:body req)]
             (do (producer/publish producer "testerman" (generate-string msg))
                 {:status 200
                  :body   {:msg msg :success true}})
             {:status 401
              :body   {:success false :desc "No message was received"}}))

(defn mk-query-data [drpc]
  (fn [req]
       (let [png-string (.execute drpc "counts" (:size (:params req)))]
         {:status 200
          :headers {"Content-Type" "text/plain; charset=utf-8"}
          :body   png-string})))

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
