(ns shorty.db
  (:require [org.httpkit.server :as http]
            [compojure.core :refer [defroutes GET POST ANY context]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.logger :refer [wrap-with-logger]]
            [environ.core :refer [env]]
            [clojure.core.async :refer [thread-call]]
            [onelog.core :as log]
            [korma.core :refer :all]
            [korma.db :refer :all]
            [shorty.utils :refer [env! defn-maybe]]
            [shorty.coder :refer [decode]]))

(defdb shorty-db (postgres (merge {:make-pool? true}
                                  (env! :db :user :password))))

(defentity urls
  (fields :id :url :hits))

(defn create-url [url]
  (insert urls (values {:url url})))

(defn increment-counter [id]
  (Thread/sleep 10000)
  (update urls
          (set-fields {:hits (raw "urls.hits + 1")})
          (where {:id [= id]})))

(defn fetch-url [code]
  (-> (select* urls)
      (where {:id [= (decode code)]})
      select
      first))

