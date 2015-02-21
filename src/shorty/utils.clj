(ns shorty.utils
  (:require [environ.core :refer [env]])
  (:import [org.apache.commons.validator.routines UrlValidator]))

(defn env!
  ([k]
   (or (k env)
       (throw (Throwable. (str "specify -D" (name k) "=...")))))
  ([k & tail]
   (into {} (map (juxt identity env!) (conj tail k)))))

(defn presence [x] (if (empty? x) nil x))

(defmacro defn-maybe [x & body]
  `(do (defmulti ~x class)
       (defmethod ~x nil [~'_] nil)
       (defmethod ~x :default ~@body)))

(defn url-validator [s]
  (.isValid (UrlValidator.) s))

