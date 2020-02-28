(ns compojure-api-test.handler
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [ring.middleware.json :refer [wrap-json-response]]
            [schema.core :as s]
            [clojure.data.json :as json]
            [compojure-api-test.mazegenerator :as mazegen]
            [juxt.dirwatch :as dirwatch]
            [clojure.java.io :as io]
            [ring.middleware.cors :refer [wrap-cors]]
            [schema.core :as s])
  (:import (clojure.lang PersistentArrayMap)))

(s/defschema Maze
  {:size Long
   (s/optional-key :mask) s/Str})

(s/defschema MazeToSolve
  {:maze s/Str
   :start-row Long
   :start-col Long
   :goal-row Long
   :goal-col Long})



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
        :return {:result PersistentArrayMap}
        :query-params [size :- Long]
        :summary "Generates a new maze and returns it as a JSON as well as an SVG XML string"
        (ok {:result (let []
                       (when (.exists (io/as-file "test.svg")) (io/delete-file "test.svg"))
                       (let [generatedMaze (mazegen/generate-maze size "")]
                         (mazegen/create-maze-file generatedMaze "")
                         (while (try (while (empty? (slurp "test.svg")) (slurp "test.svg")) (catch Exception e "")))
                         (Thread/sleep 200)
                         (let [image (slurp "test.svg")]
                           (while (.exists (io/as-file "test.svg"))
                             (try
                               (io/delete-file "test.svg") (catch Exception e)))
                           {:svg image :maze generatedMaze})))}))

      (POST "/new_masked_maze" []
        :return {:result PersistentArrayMap}
        :body [maskedMaze Maze]
        :summary "Generates a new maze with the given mask over it."
        (ok {:result (let []
                       (when (.exists (io/as-file "test.svg")) (io/delete-file "test.svg"))
                       (let [generatedMaze (mazegen/generate-maze (maskedMaze :size) (maskedMaze :mask))]
                         (mazegen/create-maze-file generatedMaze (maskedMaze :mask))
                         (while (try (while (empty? (slurp "test.svg")) (slurp "test.svg")) (catch Exception e "")))
                         (Thread/sleep 200)
                         (let [image (slurp "test.svg")]
                           (while (.exists (io/as-file "test.svg"))
                             (try
                               (io/delete-file "test.svg") (catch Exception e)))
                           {:svg image :maze generatedMaze})))})))

    (context "/solver" []
      :tags ["solver"]
      :middleware [[wrap-cors :access-control-allow-origin [#"http://localhost:8100"] :access-control-allow-methods [:get :put :post :delete]]]

      (POST "/solve_maze" []
        :return {:result String}
        :query-params [maze :- MazeToSolve]
        :summary "Solves the given maze and returns it as an SVG XML string."
        (ok {:result (let []
                       (when (.exists (io/as-file "test.svg")) (io/delete-file "test.svg"))
                       (let [solvedMaze (mazegen/find-route (maze :start-row) (maze :start-col) (maze :goal-row) (maze :goal-col) (json/read-json (maze :maze)))]
                         (mazegen/create-maze-file solvedMaze "")
                         (while (try (while (empty? (slurp "test.svg")) (slurp "test.svg")) (catch Exception e "")))
                         (Thread/sleep 200)
                         (let [image (slurp "test.svg")]
                           (while (.exists (io/as-file "test.svg"))
                             (try
                               (io/delete-file "test.svg") (catch Exception e)))
                           (json/write-str {:svg image :maze solvedMaze})))

                       )}))
      )))




