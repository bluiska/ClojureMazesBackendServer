(ns compojure-api-test.handler
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [schema.core :as s]
            [clojure.data.json :as json]
            [compojure-api-test.mazegenerator :as mazegen]
            [juxt.dirwatch :as dirwatch]
            [clojure.java.io :as io]
            [ring.middleware.cors :refer [wrap-cors]]
            [schema.core :as s]))

(s/defschema MaskedMaze
  {:size Long
   (s/optional-key :mask) s/Str})

(def app
  (api
    {:swagger
     {:ui "/"
      :spec "/swagger.json"
      :data {:info {:title "ClojureMaze API"
                    :description "An API to interact with the server solution written for the Functional Programming assignment (Task 2)."}
             :tags [{:name "ClojureMazeApi", :description "An API to access and interact with mazes generated in Clojure."}]}}}

    (context "/mazes" []
      :tags ["mazes"]
      :middleware [[wrap-cors :access-control-allow-origin [#"http://localhost:8100"] :access-control-allow-methods [:get :put :post :delete]]]

      (GET "/new_maze" []
        :return {:result String}
        :query-params [width :- Long
                       ;height :- Long
                       ;response-type :- String
                       ]
        :summary "Generates a new maze and returns it in the requested format ("
        (ok {:result (let []
                       (when (.exists (io/as-file "test.svg")) (io/delete-file "test.svg"))
                       (mazegen/create-maze width "")
                       (while (try (while (empty? (slurp "test.svg")) (slurp "test.svg")) (catch Exception e "")))
                       (Thread/sleep 200)
                       ;(slurp "test.svg")
                       (let [image (slurp "test.svg")]
                         (while (.exists (io/as-file "test.svg"))
                           (try
                             (io/delete-file "test.svg") (catch Exception e)))
                         image)

                       )}))

      (POST "/new_masked_maze" []
        :return {:result String}
        :body [maskedMaze MaskedMaze]
        :summary "Generates a new maze and returns it in the requested format ("
        (ok {:result (let []
                       (mazegen/create-maze (maskedMaze :size) (maskedMaze :mask))
                       (while (try (while (empty? (slurp "test.svg")) (slurp "test.svg")) (catch Exception e "")))
                       (Thread/sleep 200)
                       ;(slurp "test.svg")
                       (let [image (slurp "test.svg")]
                         (while (.exists (io/as-file "test.svg"))
                           (try
                             (io/delete-file "test.svg") (catch Exception e)))
                         image)

                       )}))


      (GET "/get_maze_image" []
        :return {:result String}
        ;:headers [{"Access-Control-Allow-Origin" "http://localhost:8100"}
        ;          {"Access-Control-Allow-Headers" "Origin, X-Requested-With, Content-Type, Accept"}]
        :query-params [size :- Long]
        :summary "Generates a new maze and returns it as an SVG XML string."
        (ok {:result (let []
                       (mazegen/create-maze size "")
                       (while (try (while (empty? (slurp "test.svg")) (slurp "test.svg")) (catch Exception e "")))
                       (Thread/sleep 200)
                       ;(slurp "test.svg")
                       (let [image (slurp "test.svg")]
                         (while (.exists (io/as-file "test.svg"))
                           (try
                             (io/delete-file "test.svg") (catch Exception e)))
                         image)

                       )}))

      ;(POST "/echo" []
      ;  :return Pizza
      ;  :body [pizza Pizza]
      ;  :summary "echoes a Pizza"
      ;  (ok pizza))
      )))


