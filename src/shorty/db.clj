(ns shorty.db
  (:require [korma.core :refer :all]
            [korma.db :refer :all]
            [shorty.coder :refer [decode]]
            [shorty.utils :refer [env!]]))

(defdb shorty-db (postgres (merge {:make-pool? true}
                                  (env! :db :user :password))))

(defentity urls
  (fields :id :url :code :open_count))

(defn create-url [{:keys [url] :as row}]
  (insert urls (values {:url url})))

(defn increment-counter [id]
  (update urls
          (set-fields {:hits (raw "urls.open_count + 1")})
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

