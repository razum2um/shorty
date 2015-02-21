(ns shorty.utils
  (:import [org.apache.commons.validator.routines UrlValidator]))

(defmacro defn-maybe [x & body]
  `(do (defmulti ~x class)
       (defmethod ~x nil [~'_] nil)
       (defmethod ~x :default ~@body)))

(defn presence [x] (if (empty? x) nil x))

(defn url-validator [s]
  (.isValid (UrlValidator.) s))

