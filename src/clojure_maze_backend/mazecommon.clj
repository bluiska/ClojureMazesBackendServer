(ns clojure-maze-backend.mazecommon)

;This is just a helper array to map values to keywords
(def sides [:north :east :south :west])
(def sides_backtracker {:north 0 :east 1 :south 2 :west 3})

;The directions defs define how much to add or take away from the row and column values to go either way
(def Direction_R {:north -1, :east 0, :south 1, :west 0})
(def Direction_C {:north 0, :east 1, :south 0, :west -1})

;Returns true if a cell has not been visited previously
;If a mask key exists in the cell, it checks to see if a mask exists at that cell (mask = 1)
;If no mask exists, returns true if a mask exists returns false as that cell should not be visited then.
(defn unvisited-cell?
  [r c grid]
  (let [cell (get-in grid [r c])]
    (and (= 0 (cell :visited))
         (if (nil? (cell :mask)) true (= 0 (cell :mask))))))

;To visit a cell we set the :visited key to 1
(defn visit-cell
  [r c grid]
  (assoc-in grid [r c] (assoc ((grid r) c) :visited 1)))

;Initialise the maze for walking using the Dijkstras algorithm by adding a :weight key set to 0 and setting the starting
;location to have been visited already
(defn init-maze-for-dijkstas
  [r c maze]
  (->
    (into [] (map #(vec (map (fn [cell] (assoc cell :visited 0 :weight 0)) %)) maze))
    (assoc-in [r c] (assoc ((maze r) c) :visited 1 :weight 0))))

;When working out the weights in the maze, we require a slightly different check on the cell so we expand it with an extra predicate to check
;if the direction we want to travel in has a link to where we are
(defn dijkstras-valid-unvisited-cell?
  [direction r c grid]
  (and
    (<= 0 r (dec (count grid)))
    (<= 0 c (dec (count (nth grid r))))
    (unvisited-cell? r c grid)
    (= 1 (get (get-in grid [r c]) (nth sides (mod (+ 2 (get sides_backtracker direction)) 4))))
    ))

;Add the given weight to the cell
(defn addWeight
  [r c weight grid]
  (assoc-in grid [r c] (assoc ((grid r) c) :weight weight)))

;Uses the recursive backtraker algorithm but instead of creating the maze, it traverses it, incrementing the distance as
;it moves along. The function is almost the same as generating the maze.
(defn dijkstras
  ([current_row current_col current_grid]
   (reduce (fn [grid direction]
             (let [link-r (+ current_row (Direction_R direction))
                   link-c (+ current_col (Direction_C direction))]
               (if (dijkstras-valid-unvisited-cell? direction link-r link-c grid)
                 (->>
                   (visit-cell link-r link-c grid)
                   (addWeight link-r link-c (inc ((get-in grid [current_row current_col]) :weight)))
                   (dijkstras link-r link-c)) grid))) current_grid (shuffle [:north :east :south :west]))
   )
  ([maze] (dijkstras 0 0 (init-maze-for-dijkstas 0 0 maze))))

