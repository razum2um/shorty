(ns shorty.coder
  (:require [org.httpkit.server :as http]
            [compojure.core :refer [defroutes GET POST ANY context]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.logger :refer [wrap-with-logger]]
            [environ.core :refer [env]]
            [clojure.core.async :refer [thread-call]]
            [onelog.core :as log]
            [korma.core :refer :all]
            [korma.db :refer :all]))

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

