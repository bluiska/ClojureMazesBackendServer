(ns clojure-maze-backend.handler
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [ring.middleware.json :refer [wrap-json-response]]
            [schema.core :as s]
            [clojure-maze-backend.mazegenerator :as mazegen]
            [clojure-maze-backend.mazesolver :as mazesolve]
            [clojure-maze-backend.queries :as queries]
            [clojure.java.io :as io]
            [ring.middleware.cors :refer [wrap-cors]]
            [schema.core :as s])
  (:import (clojure.lang PersistentArrayMap)
           (com.fasterxml.jackson.databind.jsonFormatVisitors JsonFormatVisitorWithSerializerProvider)))

;Defines a maze that is used as the request parameters of new_masked_maze.
;Used for validation and documentation
(s/defschema MaskedMaze
  {:size Long
   (s/optional-key :mask) s/Str
   (s/optional-key :maskSize) Long})

;Defines a sub-schema that is used to wrap other schemas. In my case, the maze is a vector of cells (with varying attributes,
;depending on the situation). It can be seen in MazeJson how this Vector schema is used.
;However, using this structure is not fully supported in Swagger so unfortunately this means, it does not show up in the documentation.
(defn Vector [inner-schema]
  (s/both (s/pred vector? "vector")
               [inner-schema]))

;Defines the structure od a standard maze that has not yet been solved.
(s/defschema MazeJson
  (Vector (Vector {(s/required-key :north) s/Num
                   (s/required-key :east) s/Num
                   (s/required-key :south) s/Num
                   (s/required-key :west) s/Num
                   (s/required-key :visited) s/Num
                   (s/required-key :weight) s/Num
                   (s/required-key :mask) s/Num})))

;Defines a schema that matches the database structure too.
;Used to verify that the retrieved mazes from the database match this schema.
(s/defschema StoredMaze
  {:id Long
   :name s/Str
   :size BigDecimal
   :json MazeJson
   :svg s/Str})

;Defines a standard maze that is returned when the backend is asked for a new maze.
(s/defschema StandardMaze
  {(s/required-key :svg) s/Str (s/required-key :maze) MazeJson})

;Defines the schema for the maze that is returned when it has been solved.
;I wasn't able to easily reuse the schema above due to the requirement of different keys so I just copied this and added
;the extra :path key to the schema.
(s/defschema SolvedMaze
  {:svg s/Str :maze (Vector (Vector {(s/required-key :north) s/Num
                      (s/required-key :east) s/Num
                      (s/required-key :south) s/Num
                      (s/required-key :west) s/Num
                      (s/required-key :visited) s/Num
                      (s/required-key :weight) s/Num
                      (s/required-key :mask) s/Num
                      (s/required-key :path) s/Num}))})

;Defines the schema for the maze that is to be solved.
;It has the optinal key of :path in case the same maze is asked for different paths.
(s/defschema MazeToSolve
  {:maze (Vector (Vector {(s/required-key :north) s/Num
                          (s/required-key :east) s/Num
                          (s/required-key :south) s/Num
                          (s/required-key :west) s/Num
                          (s/required-key :visited) s/Num
                          (s/optional-key :weight) s/Num
                          (s/required-key :mask) s/Num
                          (s/optional-key :path) s/Num}))
   :start-row Long
   :start-col Long
   :goal-row Long
   :goal-col Long})

;Main routing handler
(def app
  (api
    {:swagger
     {:ui "/"
      :spec "/swagger.json"
      :data {:info {:title "ClojureMaze API"
                    :description "An API to interact with the server solution written for the Functional Programming assignment (Task 2)."}
             :tags [{:name "ClojureMazeApi", :description "An API to access and interact with mazes generated in Clojure."}]}}}

    (context "/generator" []
      :tags ["generator"]
      :middleware [[wrap-cors :access-control-allow-origin [#"http://localhost:8100"] :access-control-allow-methods [:get :put :post :delete]]
                   [wrap-json-response]]

      (GET "/new_maze" []
        :return {:result StandardMaze}
        :query-params [size :- Long]
        :summary "Generates a new maze and returns it as a JSON as well as an SVG XML string"
        (ok {:result (let []
                       (when (.exists (io/as-file "maze.svg")) (io/delete-file "maze.svg"))
                       (let [generatedMaze (mazegen/generate-maze size "" 1)]
                         (mazegen/create-maze-file generatedMaze)
                         (while (try (while (empty? (slurp "maze.svg")) (slurp "maze.svg")) (catch Exception e "")))
                         (Thread/sleep 200)
                         (let [image (slurp "maze.svg")]
                           (while (.exists (io/as-file "maze.svg"))
                             (try
                               (io/delete-file "maze.svg") (catch Exception e)))

                           ;To save the generated maze, uncomment the below line and don't forget to change the name
                           ;(queries/insert-new-maze size "The G - 32x32" (json/write-str generatedMaze) image)
                           {:svg image :maze generatedMaze})))}))

      ;Creates a new masked maze, renders it and returns it.
      (POST "/new_masked_maze" []
        :return {:result StandardMaze}
        :body [maskedMaze MaskedMaze]
        :summary "Generates a new maze with the given mask over it."
        (ok {:result (let []
                       (when (.exists (io/as-file "maze.svg")) (io/delete-file "maze.svg"))
                       (let [generatedMaze (mazegen/generate-maze (maskedMaze :size) (maskedMaze :mask) (maskedMaze :maskSize))]
                         (mazegen/create-maze-file generatedMaze)
                         (while (try (while (empty? (slurp "maze.svg")) (slurp "maze.svg")) (catch Exception e "")))
                         (Thread/sleep 200)
                         (let [image (slurp "maze.svg")]
                           (while (.exists (io/as-file "maze.svg"))
                             (try
                               (io/delete-file "maze.svg") (catch Exception e)))

                           ;To save the generated maze, uncomment the below line and don't forget to change the name
                           ;(queries/insert-new-maze (maskedMaze :size) "Obstacles - 25x25" (json/write-str generatedMaze) image)
                           {:svg image :maze generatedMaze})))})))

    ;Solver API - Solves the given mazes
    (context "/solver" []
      :tags ["solver"]
      :middleware [[wrap-cors :access-control-allow-origin [#"http://localhost:8100"] :access-control-allow-methods [:get :put :post :delete]]
                   [wrap-json-response]]

      (POST "/solve_maze" []
        :return {:result SolvedMaze}
        :body[maze MazeToSolve]
        :summary "Solves the given maze and returns it as an SVG XML string."
        (ok {:result (let []
                       (when (.exists (io/as-file "maze.svg")) (io/delete-file "maze.svg"))
                       (let [solvedMaze (mazesolve/solve-maze (maze :start-row) (maze :start-col) (maze :goal-row) (maze :goal-col) (maze :maze))]
                         (mazegen/create-maze-file solvedMaze)
                         (while (try (while (empty? (slurp "maze.svg")) (slurp "maze.svg")) (catch Exception e "")))
                         (Thread/sleep 200)
                         (let [image (slurp "maze.svg")]
                           (while (.exists (io/as-file "maze.svg"))
                             (try
                               (io/delete-file "maze.svg") (catch Exception e)))
                           {:svg image :maze solvedMaze}))
                       )})
        )
      )
    (context "/existing_mazes" []
      :tags ["existing_mazes"]
      :middleware [[wrap-cors :access-control-allow-origin [#"http://localhost:8100"] :access-control-allow-methods [:get :put :post :delete]]
                   [wrap-json-response]]

      (GET "/available_mazes" []
        :return {:result (Vector StoredMaze)}
        :query-params []
        :summary "Returns the available mazes in the database"
        (ok {:result (vec (queries/retrieve-all-mazes))})
        )
      )
    ))




