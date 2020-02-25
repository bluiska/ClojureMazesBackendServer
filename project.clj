 (defproject compojure-api-test "0.1.0-SNAPSHOT"
   :description "FIXME: write description"
   :dependencies [[org.clojure/clojure "1.10.0"]
                  [metosin/compojure-api "2.0.0-alpha30"]]
   :ring {:handler compojure-api-test.handler/app}
   :uberjar-name "server.jar"
   :profiles {:dev {:dependencies [[javax.servlet/javax.servlet-api "3.1.0"]
                                   [org.clojure/data.json "1.0.0"]
                                   [quil "3.1.0"]
                                   [juxt/dirwatch "0.2.5"]
                                   [ring-cors "0.1.13"]
                                   [com.h2database/h2 "1.4.199"]
                                   [org.clojure/java.jdbc "0.7.11"]]
                   :plugins [[lein-ring "0.12.5"]]}})
