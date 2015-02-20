(ns shorty.web
  (:require [org.httpkit.server :as http]
            [compojure.core :refer [defroutes GET POST]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.logger :refer [wrap-with-logger]]
            [environ.core :refer [env]]
            [clojure.core.async :refer [thread-call]]
            [onelog.core :as log]
            [shorty.utils :refer [presence defn-maybe]]
            [shorty.db :as db]
            [shorty.coder :refer [encode]]))

(def domain "http://some-doma.in")

(defn error-resp [body]
  {:status 400 :body body})

(defn not-found-resp [code]
  {:status 404 :body (str "No such code found: " code)})

(defn show-code-resp [code]
  {:status 200 :body (str domain "/" code)})

(defn-maybe stats [{:keys [hits] :as row}]
  (log/debug row)
  {:status 200 :body (str hits)})

(defn-maybe expand [{:keys [url] :as row}]
  {:status 200 :body (str url)})

(defn-maybe inc-stats [{:keys [id] :as row}]
  (thread-call (fn [] (db/increment-counter id) nil))
  row)

(defn-maybe redirect [{:keys [url] :as row}]
  {:status 302 :headers {"Location" url} :body (str "Redirected to " url)})

(defmacro validate-presence [binds then]
  (let [[bind-name bind-form] binds
        param-name (last bind-form)
        error-msg (str "Please provide " param-name " param")]
    `(if-let [temp# (presence ~bind-form)]
       (let [~bind-name temp#]
         ~then)
       (error-resp ~error-msg))))

(defn shorten [req]
  (validate-presence [url (-> req :params :url)]
                     (let [{:keys [id]} (db/create-url url)]
                       (show-code-resp (encode id)))))

(defmacro >>= [x & fns]
  `(or (-> ~x ~@fns)
       (not-found-resp ~x)))

