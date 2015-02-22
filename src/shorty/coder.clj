;; ## Алгоритм сокращения
(ns shorty.coder
  (:require [environ.core :refer [env]]))

;; Замечательным свойством Clojure является инплементация протокола `IFn`
; для почти всех базовых типов. Так вектор является фукцией и возвращает
;; элемент по индексу, хеш - значение по ключу, сет - элемент, входящий в него
;; или `nil`, даже `keyword` это функция, которая применимо к хешу достает нужное
;; значение, например:
;;
;;     ({:a 1} :a)                ;; => 1
;;     ({:a 1} ;b)                ;; => nil
;;     (:a {:a 1})                ;; => 1
;;     ((comp :b :a) {:a {:b 1}}) ;; => 1
;;
;;     (#{1 2 3} 2) ;; => 2
;;     (#{1 2 3} 4) ;; => nil
;;
;;     ([10 20] 1)  ;; => 20
;;     (alphabet (rem i base)
;;
;; Также само собой разумеется, что set, sequence, vector, hash-map (и любой свой defrecord)
;; универсально обрабатываются в map/filter (интерфейс ISeq). В отличие, например, от Go, где
;; до сих пор [нет универсального итератора](https://groups.google.com/forum/#!topic/golang-nuts/v6m86sTRbqA)
(def alphabet (vec (or (:alphabet env) "jdxXVYrtpv3k7BFsMyzNbh9wqRcJS865WKCGT2L4mDfgZHPQn")))
(def base (count alphabet))

;; ### Рекурсия
;;
;; Основными заменами инмперативным циклам `for` являются идеология `map/filter/reduce`, а
;; также рекурсивные вызовы. Если вы знакомы с JVM, то знаете, что платформа не делает хвостовую
;; оптимизацию рекурсии, и все же в Clojure можно писать рекурсии, не подверженные StackOverfow
;; с помощью функций `loop` и `recur`, которые внутри транслируются в `while` цикл.
(defn decode [s]
  (loop [id 0 acc (seq s)]
    (if-let [[chr & tail] acc]
      (recur (+ (* id base) (-> alphabet (.indexOf chr))) tail)
      id)))

;; ### Apply
;;
;; Отдельно хочется упомянуть очень полезную функцию `apply`, которая пригодится чтобы
;; "распаковать" коллекцию перед применением функции (т.е. вызвать функцию не с одним аргументом -
;; коллекцией, а как бы со всеми ее элементами в качестве аргументов):
;;
;;     (+ (filter odd? (range 10)))       ;; => Cannot cast clojure.lang.LazySeq
;;                                        ;;             to java.lang.Number
;;     (apply + (filter odd? (range 10))) ;; => 25
;;
;; Также стоит отметить, что деление возвращает рациональное число:
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

