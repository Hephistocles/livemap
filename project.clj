(defproject testclj "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.apache.storm/storm-core "0.10.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 ]
  :main ^:skip-aot testclj.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})