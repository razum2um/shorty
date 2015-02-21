;; ## Алгоритм сокращения
(ns shorty.coder)

;; Замечательным свойством Clojure является инплементация протокола `IFn`
;; для почти всех базовых типов. Так вектор является фукцией и возвращает
;; элемент по индексу, хеш - значение по ключу, сет - элемент, входящий в него
;; или `nil`, например:
;;
;;     ({:a 1} :a)  ;; => 1
;;     ({:a 1} ;b)  ;; => nil
;;
;;     (#{1 2 3} 2) ;; => 2
;;     (#{1 2 3} 4) ;; => nil
;;
;;     ([10 20] 1)  ;; => 20
;;     (alphabet (rem i base)
;;
;; Также, можно самостоятельно реализовать `IFn` для своего "класса" - `defrecord`
(def alphabet (vec "bcdfghjkmnpqrstvwxyz23456789BCDFGHJKLMNPQRSTVWXYZ"))
(def base (count alphabet))

;; Также стоит отметить, что деление возвращает рациональное число
;;
;;     (/ 10 3) ;; => 10/3
;;
;; поэтому нужно либо явно приводить его к типу `int`, либо использовать более низкоуровневую
;; функцию `unchecked-divide-int`
(defn encode
  ([i] (encode '() i))
  ([acc i]
   (if (> i 0)
     (let [next-sym (alphabet (rem i base))
           next-i (unchecked-divide-int i base)]
       (recur (conj acc next-sym) next-i))
     (apply str acc))))

;; `.indexOf`
;; `recur`
(defn decode [s]
  (loop [id 0 acc (seq s)]
    (if-let [[chr & tail] acc]
      (recur (+ (* id base) (-> alphabet (.indexOf chr))) tail)
      id)))

