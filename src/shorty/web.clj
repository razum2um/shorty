(ns shorty.web
  (:require [validateur.validation :refer :all]
            [clojure.string :refer [join]]
            [clojure.set :refer [union]]
            [onelog.core :as log]
            [shorty.coder :refer [encode]]
            [shorty.db :as db]
            [shorty.utils :refer [defn-maybe presence url-validator]]))

(def ^:dynamic domain "http://some-doma.in")

(defn error-resp [errors]
  {:status 400 :body (->> errors vals flatten (apply union) (join ", "))})

(defn not-found-resp [code]
  {:status 404 :body (str "No such code found: " code)})

(defn show-code-resp [code]
  {:status 200 :body (str domain "/" code)})

(defn-maybe stats [{:keys [hits] :as row}]
  {:status 200 :body (str hits)})

(defn-maybe expand [{:keys [url] :as row}]
  {:status 200 :body (str url)})

(defn-maybe inc-stats [{:keys [id] :as row}]
  (future (db/increment-counter id))
  row)

(defn-maybe redirect [{:keys [url] :as row}]
  {:status 302 :headers {"Location" url} :body (str "Redirected to " url)})

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

