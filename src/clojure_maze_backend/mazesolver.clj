(ns clojure-maze-backend.mazesolver
  (:require [clojure-maze-backend.mazecommon :as common]))

;Initialise the maze to be solved by adding a :path field to every cell set to 0.
;This path field will be used to track which cell is part of the path.
(defn init-maze-for-solving
  [r c maze]
  (->
    (into [] (map #(vec (map (fn [cell] (assoc cell :path 0)) %)) maze))
    (assoc-in [r c] (assoc ((maze r) c) :path 2))))

;Finally, when solving the maze to find the shortest path from r,c to link-r,link-c
;we also need to check if the weight of the cell we want to visit is lower than the weight of the cell we are in
(defn solver-valid-lower-weight-cell?
  [direction r c link-r link-c grid]
  (and
    (<= 0 link-r (dec (count grid)))
    (<= 0 link-c (dec (count (nth grid link-r))))
    (= 1 (get (get-in grid [link-r link-c]) (nth common/sides (mod (+ 2 (get common/sides_backtracker direction)) 4))))
    (if-not (nil? ((get-in grid [link-r link-c]) :weight)) (< ((get-in grid [link-r link-c]) :weight) ((get-in grid [r c]) :weight)) false)
    ))

;Marks a cell as being part of the path to the solution of the maze.
(defn addPath
  [r c grid]
  (assoc-in grid [r c] (assoc ((grid r) c) :path 1)))

;Uses recursion to follow the shortest route through the maze. Starts from finish and goes to start.
(defn find-route
  [start-row start-col finish-row finish-col weighted-maze]
  (loop [r finish-row c finish-col maze (init-maze-for-solving finish-row finish-col weighted-maze)]
    (cond
      (= 1 ((get-in maze [start-row start-col]) :path))
        (assoc-in maze [start-row start-col] (assoc ((maze start-row) start-col) :path 3))
      :else
      (let [lowest-weight (first (filter #(solver-valid-lower-weight-cell? % r c (+ r (common/Direction_R %)) (+ c (common/Direction_C %)) maze) common/sides))
            link-r (+ r (common/Direction_R lowest-weight))
            link-c (+ c (common/Direction_C lowest-weight))]
        (->>
          (addPath link-r link-c maze)
          (recur link-r link-c))
        ))))

;Recalculates the weights using Dijkstras algorithm from the starting point
;then finds the shortest path to the finish.
(defn solve-maze
  [start-r start-c finish-r finish-c maze]
  (find-route start-r start-c finish-r finish-c (common/dijkstras start-r start-c (common/init-maze-for-dijkstas start-r start-c maze))))