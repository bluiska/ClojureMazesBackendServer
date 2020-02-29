(ns compojure-api-test.mazegenerator
  (:require [quil.core :as q]
            [quil.middleware :as m]
            [clojure.string :as str]))

(defn str->int [str]
  (let [n (read-string str)]
    (if (number? n) n nil)))

(defn getImageArrayFromString
  [mask size]
  (let [maskString mask]
    (vec (map #(vec %) (partition size (mapv #(if (= 255 (str->int (first %))) 1 0) (partition 4 (str/split maskString #","))))))))

(def cell {:north 0 :east 0 :south 0 :west 0 :visited 0})

;The directions defs define how much to add or take away from the row and column values to go either way
(def Direction_R {:north -1, :east 0, :south 1, :west 0})
(def Direction_C {:north 0, :east 1, :south 0, :west -1})

;This is just a helper array to map values to keywords
(def sides [:north :east :south :west])
(def sides_backtracker {:north 0 :east 1 :south 2 :west 3})

;Set the cell at 0,0 to have been visited since we start there to generate the maze.
(defn init-maze
  [maze]
  (assoc-in maze [0 0] (assoc ((maze 0) 0) :visited 1)))

(defn init-maze-for-dijkstas
  [maze]
  (->
    (into [] (map #(vec (map (fn [cell] (assoc cell :visited 0 :weight 0)) %)) maze))
    (assoc-in [0 0] (assoc ((maze 0) 0) :visited 1 :weight 0))))

(defn init-maze-for-solving
  [r c maze]
  (->
    (into [] (map #(vec (map (fn [cell] (assoc cell :path 0)) %)) maze))
    (assoc-in [r c] (assoc ((maze r) c) :path 1))))

;Generates a rectangular maze.
(defn rectangle-maze
  [row col]
  (vec (take row (repeat (vec (take col (repeat cell)))))))

(defn unvisited-cell?
  [r c mask grid]
  (let [cell (get-in grid [r c])
        maskCell (get (get mask r) c)]
    (and (= 0 (cell :visited))
         (if (nil? maskCell) true (= 0 maskCell)))))

(defn valid-unvisited-cell?
  [r c mask grid]
  (and
    (<= 0 r (dec (count grid)))
    (<= 0 c (dec (count (nth grid r))))
    (unvisited-cell? r c mask grid)))

(defn solver-valid-unvisited-cell?
  [direction r c mask grid]
  (and
    (<= 0 r (dec (count grid)))
    (<= 0 c (dec (count (nth grid r))))
    (unvisited-cell? r c mask grid)
    (= 1 (get (get-in grid [r c]) (nth sides (mod (+ 2 (get sides_backtracker direction)) 4))))
    ))

(defn solver-valid-lower-weight-cell?
  [direction r c link-r link-c grid]
  (and
    (<= 0 link-r (dec (count grid)))
    (<= 0 link-c (dec (count (nth grid link-r))))
    (= 1 (get (get-in grid [link-r link-c]) (nth sides (mod (+ 2 (get sides_backtracker direction)) 4))))
    (if-not (nil? ((get-in grid [link-r link-c]) :weight)) (< ((get-in grid [link-r link-c]) :weight) ((get-in grid [r c]) :weight)) false)
    ))

(defn reset-visited
  [maze]
  (into [] (map #(vec (map (fn [cell] (when (= 1 (cell :path)) (assoc cell :visited 1))) %)) maze)))

(defn visit-cell
  [r c grid]
  (assoc-in grid [r c] (assoc ((grid r) c) :visited 1)))

(defn join-cells
  [cell1_r cell1_c cell2_r cell2_c direction grid]
  (let [linkKey (nth sides (mod (+ 2 (get sides_backtracker direction)) 4))]
    (->
      (assoc-in grid [cell1_r cell1_c] (assoc ((grid cell1_r) cell1_c) direction 1))
      (assoc-in [cell2_r cell2_c] (assoc ((grid cell2_r) cell2_c) linkKey 1)))))

(defn recursive-backtracking-maze
  ([current_row current_col mask current_grid]
   (reduce (fn [grid direction]
             (let [link-r (+ current_row (Direction_R direction))
                   link-c (+ current_col (Direction_C direction))]
               (if (valid-unvisited-cell? link-r link-c mask grid)
                 (->>
                   (visit-cell link-r link-c grid)
                   (join-cells current_row current_col link-r link-c direction)
                   (recursive-backtracking-maze link-r link-c mask)) grid))) current_grid (shuffle [:north :east :south :west])))
  ([mask maze] (recursive-backtracking-maze 0 0 mask (init-maze maze)))) ;start at a random point

(defn addWeight
  [r c weight grid]
  (assoc-in grid [r c] (assoc ((grid r) c) :weight weight)))

(defn dijkstras
  ([current_row current_col mask current_grid]
   (reduce (fn [grid direction]
             (let [link-r (+ current_row (Direction_R direction))
                   link-c (+ current_col (Direction_C direction))]
               (if (solver-valid-unvisited-cell? direction link-r link-c mask grid)
                 (->>
                   (visit-cell link-r link-c grid)
                   (addWeight link-r link-c (inc ((get-in grid [current_row current_col]) :weight)))
                   (dijkstras link-r link-c mask)) grid))) current_grid (shuffle [:north :east :south :west]))
   )
  ([mask maze] (dijkstras 0 0 mask (init-maze-for-dijkstas maze))))

(defn addPath
  [r c grid]
  (assoc-in grid [r c] (assoc ((grid r) c) :path 1)))

(defn find-route
  [start-row start-col finish-row finish-col weighted-maze]
  (loop [r finish-row c finish-col maze (init-maze-for-solving finish-row finish-col weighted-maze)]
    (cond
      (= 1 ((get-in maze [start-row start-col]) :path)) maze
      :else
      (let [lowest-weight (first (filter #(solver-valid-lower-weight-cell? % r c (+ r (Direction_R %)) (+ c (Direction_C %)) maze) sides))
            link-r (+ r (Direction_R lowest-weight))
            link-c (+ c (Direction_C lowest-weight))]
        (->>
          (addPath link-r link-c maze)
          (recur link-r link-c))
        ))))

(defn make-maze [r c mask]
  (recursive-backtracking-maze mask (rectangle-maze r c)))

(defn get-drawn-maze
  [maze-to-draw mask]
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
                    maskCell (get (get mask y) x)]
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
                (when (= 1 (cell :path))
                  (do
                    (q/fill 200 240 0)
                    (q/no-stroke)
                    (q/rect (+ xv (/ (/ x-cell-size 2) 2)) (+ yv (/ (/ y-cell-size 2) 2)) (/ x-cell-size 2) (/ y-cell-size 2))))

                (q/stroke 0)
                (q/stroke-weight 5)
                (q/fill 0)
                (when (= 0 (cell :east)) (when (or (nil? maskCell) (= 0 maskCell)) (q/line xv2 yv xv2 yv2)))
                (when (= 0 (cell :west)) (when (or (nil? maskCell) (= 0 maskCell)) (q/line xv yv xv yv2)))
                (when (= 0 (cell :south)) (when (or (nil? maskCell) (= 0 maskCell)) (q/line xv yv2 xv2 yv2)))
                (when (= 0 (cell :north)) (when (or (nil? maskCell) (= 0 maskCell)) (q/line xv yv xv2 yv)))
                (when (> 14 (count maze)) (q/text-size 20))
                (when (> 14 (count maze)) (q/text (str (cell :weight)) (+ xv (/ x-cell-size 2)) (+ yv (/ y-cell-size 2))))))))))))

(defn generate-maze
  [size mask maskSize]
  (dijkstras mask (make-maze size size (getImageArrayFromString mask maskSize))))

(defn create-maze-file
  "maze: the 2D matrix of the maze cells
   size: int (preferable up to 32 as the algorithm I have causes a stack overflow
   mask: string (the 2d array of values where 1 will be the mask and 0 is ignored"
  [maze mask maskSize]
  (let [*agnt* (agent {})]
    (send-off *agnt* (fn [state]
                       (q/sketch
                         :draw (fn []
                                 (q/do-record (q/create-graphics 750 750 :svg "test.svg")
                                              (q/no-loop)
                                              (get-drawn-maze maze (getImageArrayFromString mask maskSize)))
                                 (q/exit)
                                 ))
                       (assoc state :done true)))

    (await *agnt*)
    ))

;(q/sketch
;  :draw (fn []
;          (q/do-record (q/create-graphics 750 750 :svg "test.svg")
;                       (q/no-loop)
;                       (get-drawn-maze {:maze-size 32 :mask [] :width 750 :height 750})
;                       )
;          (q/exit)
;          ))