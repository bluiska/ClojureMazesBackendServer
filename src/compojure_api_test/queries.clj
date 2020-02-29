(ns compojure-api-test.queries
  (:require [clojure.java.jdbc :as jdbc]))

(defn clob-to-string [row]
  (assoc row :json
             (with-open [rdr (java.io.BufferedReader. (.getCharacterStream (:json row)))]
               (apply str (line-seq rdr)))))

(def db-spec {:subprotocol "h2"
              :subname "./data/mazes"
              :user "sa"
              :password ""})

(def tables {
             :mazes {:name "mazes" :id-col "maze_id"}
             })

;Insert a new maze into the database and return the ID of it.
(defn insert-new-maze
  "size: int
   name: string
   json: json stringified already
   svg: string svg xml"
  [size name json svg]
  ;(let
  ;  [success (jdbc/execute! db-spec
  ;                          [(str "INSERT INTO mazes (size, name, json, svg)
  ;                            VALUES ('" size "','"
  ;                                      name "','"
  ;                                      json "','"
  ;                                      svg "');")]
  ;                          )]
  ;  (if success (conj {:success true} (first (jdbc/query db-spec ["SELECT TOP 1 maze_id FROM mazes ORDER BY maze_id DESC"])))
  ;              {:success false :data {:maze-id -1}}))
  (let
    [success (jdbc/insert! db-spec
                            "mazes" {:size size :name name :json json :svg svg}
                            )]
    (if success (conj {:success true} (first (jdbc/query db-spec ["SELECT TOP 1 maze_id FROM mazes ORDER BY maze_id DESC"])))
                {:success false :data {:maze-id -1}}))
  )


;Update the svg image string for the given maze
(defn update-maze-svg
   "table: keyword from table def
    id: int
    svg: stringified svg xml"
  [table id svg]
  (let
    [success (jdbc/execute! db-spec
                            [(str "UPDATE " ((tables table) :name) " "
                                  "SET svg = '" svg "' "
                                  "WHERE " ((tables table) :id-col) " = " id
                            )])]
    {:success (boolean (first success))}))






