;; ## "Model" layer
;;
;; В основном все функции в этом ns это [korma](http://sqlkorma.com/).
(ns shorty.db
  (:require [korma.core :refer :all]
            [korma.db :refer :all]
            [environ.core :refer [env]]
            [shorty.coder :refer [decode]]))

(def db-host (or (env :host) "localhost"))

(def jdbc-spec
  (merge {:classname "org.postgresql.Driver"
          :minimum-pool-size 100
          :maximum-pool-size 500
          :subprotocol "postgresql"
          :subname (str "//" db-host "/" (env :db))}
         (select-keys env [:db :user :password])))

;; ### Delay
;;
;; Несмотря на то, что `shorty-db` является значением и содержит в себе объект подключения,
;; инициализация соединения откладывается до рантайма вот таким
;; [кодом](https://github.com/korma/Korma/blob/master/src/korma/db.clj#L53L56).
(defdb shorty-db (postgres jdbc-spec))

;; ### Где мой ActiveRecord?
;;
;;
(defentity urls
  (fields :id :url :code :open_count))

;; ### Destruction
;;
;; Обратите внимание на запись параметров функции:
;;
;;     [{:keys [id] :as url}]
;;
;; Советую отличное [разъяснение](http://blog.jayfields.com/2010/07/clojure-destructuring.html)
;; как оно работает на примерах.
(defn update-url [{:keys [id] :as url}]
  (update urls
          (set-fields url)
          (where {:id [= id]})))

;; ### SQL Injection
;;
;; Проблема экранирования здесь отсутствует как класс, т.к. все параметризованные
;; запросы транслируются в `prepared statements`:
;;
;;     LOG:  execute <unnamed>: INSERT INTO "urls" ("url") VALUES ($1) RETURNING *
;;     DETAIL:  parameters: $1 = 'http://is.gd'
(defn create-url [{:keys [url] :as row}]
  (insert urls (values {:url url})))

(defn increment-counter [id]
  (update urls
          (set-fields {:open_count (raw "urls.open_count + 1")})
          (where {:id [= id]})))


(defn find-url [id]
  (-> (select* urls)
      (where {:id [= id]})
      select
      first))

