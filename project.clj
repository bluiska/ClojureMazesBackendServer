 (defproject clojure-maze-backend "0.1.0-SNAPSHOT"
   :description "Clojure backend for generating, solving and rendering a maze."
   :dependencies [[org.clojure/clojure "1.10.0"]
                  [metosin/compojure-api "2.0.0-alpha30"]]
   :ring {:handler clojure-maze-backend.handler/app}
   :uberjar-name "server.jar"
   :profiles {:dev {:dependencies [[javax.servlet/javax.servlet-api "3.1.0"]
                                   [org.clojure/data.json "1.0.0"]
                                   [quil "3.1.0"]
                                   [juxt/dirwatch "0.2.5"]
                                   [ring-cors "0.1.13"]
                                   [com.h2database/h2 "1.4.199"]
                                   [org.clojure/java.jdbc "0.7.11"]
                                   [ring/ring-json "0.5.0"]]
                   :plugins [[lein-ring "0.12.5"]]}})
