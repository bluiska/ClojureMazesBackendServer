(ns clojure-maze-backend.mazegenerator
  (:require [clojure-maze-backend.mazecommon :as common]
            [clojure.string :as str]
            [quil.core :as q]))

;Convert a string to an integer
(defn str->int [str]
  (let [n (read-string str)]
    (if (number? n) n nil)))

;Function used to convert the string mask provided by the client into a 2D array
(defn getImageArrayFromString
  [mask size]
  (let [maskString mask]
    (vec (map #(vec %) (partition size (mapv #(if (= 255 (str->int (first %))) 1 0) (partition 4 (str/split maskString #","))))))))

;Defines a cell of a standard maze without a mask and a path param, both of which are added as needed by respective algorithms
(def cell {:north 0 :east 0 :south 0 :west 0 :visited 0})

;Set the cell at 0,0 to have been visited since we start there to generate the maze.
(defn init-maze
  [maze]
  (assoc-in maze [0 0] (assoc ((maze 0) 0) :visited 1)))

;Generates a rectangular maze.
(defn rectangle-maze
  [row col]
  (vec (take row (repeat (vec (take col (repeat cell)))))))

;Adds boundary checking to the unvisited cell checking to ensure the row and col attempted to be accessed is within bounds.
(defn valid-unvisited-cell?
  [r c grid]
  (and
    (<= 0 r (dec (count grid)))
    (<= 0 c (dec (count (nth grid r))))
    (common/unvisited-cell? r c grid)))

;Join two cells by setting the the correct directions key to 1 in each cell
(defn join-cells
  [cell1_r cell1_c cell2_r cell2_c direction grid]
  (let [linkKey (nth common/sides (mod (+ 2 (get common/sides_backtracker direction)) 4))]
    (->
      (assoc-in grid [cell1_r cell1_c] (assoc ((grid cell1_r) cell1_c) direction 1))
      (assoc-in [cell2_r cell2_c] (assoc ((grid cell2_r) cell2_c) linkKey 1)))))

;The recursive backtracker algorithm to create the maze
;Uses reduce to execute the path branching in every direction at each cell as well as to enabled the manipulation
;of the grid in an immutable way.
;The directions are shuffled and for each direction in the array it recurses and executes the same function.
;This will result in the algorithm executing the recursive backtracking. However, as this function is not tail recursive,
;it runs out of stack space for mazes with sizes over 32 (more like 64 but 32 is a safe number).
;The implementation was inspired by the following source:
;https://stackoverflow.com/questions/24255360/idiomatic-clojure-implementation-of-maze-generation-algorithm
(defn recursive-backtracking-maze
  ([current_row current_col current_grid]
   (reduce (fn [grid direction]
             (let [link-r (+ current_row (common/Direction_R direction))
                   link-c (+ current_col (common/Direction_C direction))]
               (if (valid-unvisited-cell? link-r link-c grid)
                 (->>
                   (common/visit-cell link-r link-c grid)
                   (join-cells current_row current_col link-r link-c direction)
                   (recursive-backtracking-maze link-r link-c)) grid))) current_grid (shuffle [:north :east :south :west])))
  ([maze] (recursive-backtracking-maze 0 0 (init-maze maze)))) ;start at 0,0. Haven't implemented starting at random point due to masking

;Tried using list comprehension however found it easier to work with map-indexed
;Adds the mask to the maze by looping through each cell and checking if the corresponding cell is set in the mask
(defn add-mask
  [maze mask]
  (vec (map-indexed (fn [r row] (vec (map-indexed (fn [c col] (assoc col :mask (if (= 1 (get (get mask r) c)) 1 0))) row))) maze)))

;A wrapper function to make a maze with a mask (if provided)
(defn make-maze
  [r c mask maskSize]
  (recursive-backtracking-maze (add-mask (rectangle-maze r c) (getImageArrayFromString mask maskSize))))

;The following function uses quil to render the maze as a 750x750 image. It applies a small margin to it too so that
;the rendered maze looks a bit cleaner. It traverses the maze cell by cell with a nested "for" loop using dotimes.
;If mask is set on the cell, it colours it green. If there is no mask on the cell colour it slightly less green. (These
;are the areas of the mask which are enclosed so the algorithm couldn't visit it). If a cell has been visited and has no
;mask set, the colour of the cell will be set based on how far it is from the starting point of the maze. (I take away
;the weight of the cell from 255 for the RED value of the colour.)
;If the maze was solved and contains a path key, I add a smaller yellow rectangle to the maze to mark it as part of the path.
;The starting point of the path is coloured a ligher blue colour and the finish colour green.
;When the maze size is smaller than 14, I also print the weights inside the cell.
;The following code is based on and is an extension to a possible solution provided at:
;https://github.com/fbeline/maze/blob/master/src/maze/quil.clj
(defn get-drawn-maze
  [maze-to-draw]
  (let [maze maze-to-draw margin 20]
    (q/fill 0)
    (q/background 240)
    (q/line margin margin (- 750 margin) margin)
    (let [y-cell-size (/ (- 750 (* 2 margin)) (count maze))
          x-cell-size (/ (- 750 (* 2 margin)) (-> maze first count))]
      (dotimes [y (count maze)]
        (let [yv (+ margin (* y y-cell-size))
              yv2 (+ y-cell-size yv)]
          (q/line margin yv margin yv2)
          (dotimes [x (-> maze first count)]
            (let [xv (+ margin (* x x-cell-size))
                  xv2 (+ x-cell-size xv)]
              (let [cell (get-in maze [y x])
                    maskCell ((get-in maze [y x]) :mask)]
                (if (or (nil? maskCell) (= 0 maskCell)) (if (= 0 (cell :visited))
                                                          (do
                                                            (q/fill 200 255 200)
                                                            (q/no-stroke)
                                                            (q/rect (+ 3 xv) (+ 3 yv) x-cell-size y-cell-size))
                                                          (do
                                                            (q/fill (- 255 (cell :weight)) 50 100)
                                                            (q/no-stroke)
                                                            (q/rect (+ 3 xv) (+ 3 yv) x-cell-size y-cell-size)))
                                                        (do
                                                          (q/fill 100 255 100)
                                                          (q/no-stroke)
                                                          (q/rect (+ 3 xv) (+ 3 yv) x-cell-size y-cell-size)
                                                          ))
                (q/no-stroke)
                (when (not= 0 (cell :path))
                  (do
                    (cond
                      (= 1 (cell :path)) (do
                                           (q/fill 200 240 0)
                                           (q/rect (+ xv (/ (/ x-cell-size 2) 2)) (+ yv (/ (/ y-cell-size 2) 2)) (/ x-cell-size 2) (/ y-cell-size 2)))
                      (= 2 (cell :path)) (do
                                           (q/fill 0 200 0)
                                           (q/rect (+ 3 xv) (+ 3 yv) x-cell-size y-cell-size))
                      (= 3 (cell :path)) (do
                                           (q/fill 50 50 255)
                                           (q/rect (+ 3 xv) (+ 3 yv) x-cell-size y-cell-size)))))

              (q/stroke 0)
              (q/stroke-weight 5)
              (q/fill 0)
              (when (= 0 (cell :east)) (when (or (nil? maskCell) (= 0 maskCell)) (q/line xv2 yv xv2 yv2)))
              (when (= 0 (cell :west)) (when (or (nil? maskCell) (= 0 maskCell)) (q/line xv yv xv yv2)))
              (when (= 0 (cell :south)) (when (or (nil? maskCell) (= 0 maskCell)) (q/line xv yv2 xv2 yv2)))
              (when (= 0 (cell :north)) (when (or (nil? maskCell) (= 0 maskCell)) (q/line xv yv xv2 yv)))
              (when (> 14 (count maze)) (q/text-size 20))
              (when (> 14 (count maze)) (q/text (str (cell :weight)) (+ xv (/ x-cell-size 2)) (+ yv (/ y-cell-size 2))))))))))))

;Creates a maze of the given size with the given mask and also works out weights from 0 0 coordinates
;The weights are needed to colour the maze intially
(defn generate-maze
  [size mask maskSize]
  (common/dijkstras (make-maze size size mask maskSize)))

;The function used to create the maze and write it to a file.
;It uses an agent to execute this asynchronously.
(defn create-maze-file
  "maze: the 2D matrix of the maze cells
   size: int (preferable up to 32 as the algorithm I have causes a stack overflow
   mask: string (the 2d array of values where 1 will be the mask and 0 is ignored"
  [maze]
  (let [*agnt* (agent {})]
    (send-off *agnt* (fn [state]
                       (q/sketch
                         :draw (fn []
                                 (q/do-record (q/create-graphics 750 750 :svg "maze.svg")
                                              (q/no-loop)
                                              (get-drawn-maze maze))
                                 (q/exit)
                                 ))
                       (assoc state :done true)))

    (await *agnt*)
    ))