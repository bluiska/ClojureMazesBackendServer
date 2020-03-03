(ns compojure-api-test.queries
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.data.json :as json]))

;Function to convert the CLOB format of the maze and the svg to json and string respectively
(defn retrieve-row-function [row]
  (->
    (assoc row :json
               (json/read-str (with-open [rdr (java.io.BufferedReader. (.getCharacterStream (:json row)))]
               (apply str (line-seq rdr))) :key-fn keyword))
    (assoc :svg (with-open [rdr (java.io.BufferedReader. (.getCharacterStream (:svg row)))]
                  (apply str (line-seq rdr))))))

(def db-spec {:subprotocol "h2"
              :subname "./data/mazes"
              :user "sa"
              :password ""})

;Insert a new maze into the database and return the ID of it.
;Used during development
(defn insert-new-maze
  "size: int
   name: string
   json: json stringified already
   svg: string svg xml"
  [size name json svg]
  (let
    [success (jdbc/insert! db-spec
                            "mazes" {:size size :name name :json json :svg svg}
                            )]
    (if success (conj {:success true} (first (jdbc/query db-spec ["SELECT TOP 1 ID FROM mazes ORDER BY ID DESC"])))
                {:success false :data {:maze-id -1}}))
  )


;Retrieves all the mazes from the database
(defn retrieve-all-mazes
  []
  (let
    [success (jdbc/query db-spec
                            ["SELECT * FROM MAZES"] {:row-fn retrieve-row-function})]
    success))






