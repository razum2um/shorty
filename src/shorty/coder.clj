(ns shorty.coder)

(def alphabet (vec "bcdfghjkmnpqrstvwxyz23456789BCDFGHJKLMNPQRSTVWXYZ"))
(def base (count alphabet))

(defn decode [s]
  (loop [id 0 acc (seq s)]
    (if-let [[chr & tail] acc]
      (recur (+ (* id base) (-> alphabet (.indexOf chr))) tail)
      id)))

(defn encode
  ([i] (encode '() i))
  ([acc i]
   (if (> i 0)
     (let [next-sym (alphabet (rem i base))
           next-i (unchecked-divide-int i base)]
       (recur (conj acc next-sym) next-i))
     (apply str acc))))

