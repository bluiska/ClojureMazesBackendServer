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
             :weighted_mazes {:name "weighted_mazes" :id-col "weighted_maze_id"}
             :maze_solutions {:name "maze_solutions" :id-col "maze_solution_id"}
             })

;Insert a new maze into the database and return the ID of it.
(defn insert-new-maze
  "width: int
   height: int
   json: json stringified already
   svg: string svg xml"
  [width height json svg]
  (let
    [success (jdbc/execute! db-spec
                            [(str "INSERT INTO mazes (width, height, json, svg)
                              VALUES (" width ","
                                        height ",'"
                                        json "',"
                                        (if (empty? svg) "''" (str "'" svg "'"))
                                     ");")]
                            )]
    (if success (conj {:succesds true} (first (jdbc/query db-spec ["SELECT TOP 1 maze_id FROM mazes ORDER BY maze_id DESC"])))
                {:success false :data {:maze-id -1}})))


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

(defn )






