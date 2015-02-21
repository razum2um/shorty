(ns shorty.db
  (:require [korma.core :refer :all]
            [korma.db :refer :all]
            [shorty.coder :refer [decode]]
            [shorty.utils :refer [env!]]))

(defdb shorty-db (postgres (merge {:make-pool? true}
                                  (env! :db :user :password))))

(defentity urls
  (fields :id :url :hits))

(defn create-url [url]
  (insert urls (values {:url url})))

(defn increment-counter [id]
  (update urls
          (set-fields {:hits (raw "urls.hits + 1")})
          (where {:id [= id]})))

(defn fetch-url [code]
  (-> (select* urls)
      (where {:id [= (decode code)]})
      select
      first))

