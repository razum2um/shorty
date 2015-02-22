;; ## Java interop
;;
;; Clojure очень тесно связан с JVM платформой.
(ns shorty.utils
  (:import [org.apache.commons.validator.routines UrlValidator]))

;; Обратите внимание на `~'_`
(defmacro defn-maybe [x & body]
  `(do (defmulti ~x class)
       (defmethod ~x nil [~'_] nil)
       (defmethod ~x :default ~@body)))

;; Потренируетесь и посмотрите исходник `defn`.
;; Это то же самое, что и:
;;
;;     (def presence (fn [x] (if (empty? x) nil x)))
(defn presence [x]
  (if (nil? x) nil x))

;; ### Переменные
;;
;; Основной заменой инмперативному `x = 1` в Clojure является `let`
;;
;; Вызов конструктора Java-объектов делается так:
;;
;;     (BigDecimal. "1.0") ;; => 1.0M
;;
;; вызов метода (т.к. `(class "str") ;; => java.lang.String`):
;;
;;     (.length "str") ;; => 3
;;
;; статические вызовы делаются так:
;;
;;     Integer/MAX_VALUE                ;; => 2147483647
;;     (System/getProperty "user.home") ;; => "/Users/razum2um"
(defn url-validator [s]
  (let [v (UrlValidator.)]
    (-> v (.isValid s))))

