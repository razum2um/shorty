(ns shorty.web
  (:require [validateur.validation :refer :all]
            [clojure.string :refer [join]]
            [clojure.set :refer [union]]
            [onelog.core :as log]
            [shorty.coder :refer [encode]]
            [shorty.db :as db]
            [shorty.utils :refer [defn-maybe presence url-validator]]))

(def ^:dynamic domain "http://some-doma.in")

(defn resp
  ([code body] (resp code {"Content-Type" "text/plain"} body))
  ([code headers body]
   {:status code :headers headers :body body}))

(defn error-resp [errors]
  (resp 400 (->> errors vals flatten (apply union) (join ", "))))

(defn not-found-resp [code]
  (resp 404 (str "No such code found: " code)))

(defn show-code-resp [code]
  (resp 200 (str domain "/" code)))

(defn-maybe stats [{:keys [hits] :as row}]
  (resp 200 (str hits)))

(defn-maybe expand [{:keys [url] :as row}]
  (resp 200  (str url)))

(defn-maybe inc-stats [{:keys [id] :as row}]
  (future (db/increment-counter id))
  row)

(defn-maybe redirect [{:keys [url] :as row}]
  (resp 302 {"Location" url} (str "Redirected to " url)))

(def shorten-validator
  (validation-set
    (presence-of :url)
    (validate-by :url url-validator :message "url is invalid")))

(defn shorten [{:keys [params] :as req}]
  (if (valid? shorten-validator params)
    (let [{:keys [id]} (db/create-url (:url params))]
      (-> id encode show-code-resp))
    (-> params shorten-validator error-resp)))

(defmacro >>= [x & fns]
  `(or (-> ~x ~@fns)
       (not-found-resp ~x)))

