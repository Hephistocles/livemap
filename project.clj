(defproject testclj "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [yieldbot/marceline "0.2.1"]
                 [com.github.ptgoetz/storm-jms "0.9.0"]
                 [org.apache.activemq/activemq-all "5.13.0"]
                 [clamq/clamq-activemq "0.4"]
                 [ring/ring-core "1.2.1"]
                 [ring/ring-jetty-adapter "1.2.1"]
                 [ring/ring-json "0.3.1"]
                 [compojure "1.1.8"]
                 [org.clojure/math.numeric-tower "0.0.4"]
                 [cheshire "5.5.0"]]
  :target-path "target/%s"
  :profiles {
             :provided {:dependencies [[org.apache.storm/storm-core "0.10.0"]]}})
