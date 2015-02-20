(ns shorty.utils
  (:require [org.httpkit.server :as http]
            [compojure.core :refer [defroutes GET POST ANY context]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.logger :refer [wrap-with-logger]]
            [environ.core :refer [env]]
            [clojure.core.async :refer [thread-call]]
            [onelog.core :as log]
            [korma.core :refer :all]
            [korma.db :refer :all]))

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

