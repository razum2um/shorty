(ns shorty.coder)

(def alphabet (vec "bcdfghjkmnpqrstvwxyz23456789BCDFGHJKLMNPQRSTVWXYZ"))
(def base (count alphabet))

(defn encode
  ([i] (encode '() i))
  ([acc i]
   (if (> i 0)
     (let [next-sym (alphabet (rem i base))
           ;; (/ i base) is rational
           next-i (unchecked-divide-int i base)]
       (recur (conj acc next-sym) next-i))
     (apply str acc))))

(defn decode [s]
  (loop [id 0 acc (seq s)]
    (if-let [[chr & tail] acc]
      (recur (+ (* id base) (.indexOf alphabet chr)) tail)
      id)))

