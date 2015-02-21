(ns shorty.db
  (:require [korma.core :refer :all]
            [korma.db :refer :all]
            [environ.core :refer [env]]
            [shorty.coder :refer [decode]]
            [shorty.utils :refer [env!]]))

(def db-host (or (env :host) "localhost"))

(def jdbc-spec
  (merge {:classname "org.postgresql.Driver"
          :subprotocol "postgresql"
          :subname (str "//" db-host "/" (env :db))}
         (select-keys env [:db :user :password])))

(defdb shorty-db (postgres jdbc-spec))

(defentity urls
  (fields :id :url :code :open_count))

(defn create-url [{:keys [url] :as row}]
  (insert urls (values {:url url})))

(defn increment-counter [id]
  (update urls
          (set-fields {:open_count (raw "urls.open_count + 1")})
          (where {:id [= id]})))

(defn update-url [{:keys [id] :as url}]
  (update urls
          (set-fields url)
          (where {:id [= id]})))

(defn find-url [id]
  (-> (select* urls)
      (where {:id [= id]})
      select
      first))

