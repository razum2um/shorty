;; ## "Model" layer
;;
;; В основном все функции в этом `ns` это [korma](http://sqlkorma.com/).
(ns shorty.db
  (:require [korma.core :refer :all]
            [korma.db :refer :all]
            [environ.core :refer [env]]
            [shorty.coder :refer [decode]]))

(def db-host (or (env :host) "localhost"))

;; ### JDBC
;;
;; Основным способом коннекта из JVM в базу данных является jdbc, и он прекрасен.
;; Если бы не одно но. Если вы ищете в Clojure ту степень конкурентности, что есть в NodeJS,
;; то скорее всего сильно расстроитесь, найдя здесь богомерзкие эксепшены, способность JVM
;; загрузить все ядра, но не найдя прекрасных коллбеков на каждый чих и "лесенок" из них ;)
;;
;; А если серьезно, то даже если использовать асинхронные веб-сервера (или
;; тот же `http-kit`, что и это приложение), а так же `core.async` для "выпрямления" кода,
;; и сделаете все приложение асинхронным, то все равно обнаружите, что запросы к базе
;; синхронизируются внутри jdbc и с этим пока ничего не поделать :(
;;
;; Хотя на свой страх и риск - [вот](https://github.com/alaisi/postgres.async).
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
;; инициализация соединения откладывается до рантайма вот
;; [таким кодом](https://github.com/korma/Korma/blob/master/src/korma/db.clj#L53L56).
(defdb shorty-db (postgres jdbc-spec))

;; ### Где моя ActiveRecord?
;;
;; Она утонула.
;;
;; В Clojure вряд ли появятся монстры как Rails. Simplicity über alles!
;; Однако, в примерах `korma` можно найти как связать `entities` связями
;; `belongs_to`, `has_many`, а также получать эти релейшены без билда явного
;; запроса и т.д.
(defentity urls
  (fields :id :url :code :open_count))

;; ### SQL Injection
;;
;; Проблема экранирования здесь отсутствует как класс, т.к. все параметризованные
;; запросы транслируются в `prepared statements`:
;;
;;     LOG:  execute <unnamed>: INSERT INTO "urls" ("url") VALUES ($1) RETURNING *
;;     DETAIL:  parameters: $1 = 'http://is.gd'
(defn create-url [{:keys [url] :as row}]
  (insert urls (values {:url url})))

(defn update-url [{:keys [id] :as url}]
  (update urls
          (set-fields url)
          (where {:id [= id]})))

(defn increment-counter [id]
  (update urls
          (set-fields {:open_count (raw "urls.open_count + 1")})
          (where {:id [= id]})))

;; ### Scopes
;;
;; Однако данную удобную фичу ActiveRecord очень легко реализовать используя все тот же
;; `->` макрос и `select*` из `korma`:
;;
;;     (let [google-urls   (-> (select* urls) (where {:url [like "%google%"]}))
;;           top-urls      (-> google-urls (where {:open_count [> 1000]}))
;;           some-top-urls (-> top-url (limit 10))]
;;       (select some-top-urls))
;;
;; Запрос будет сделан всего 1 раз со обоими ограничениями и лимитом.
(defn find-url [id]
  (-> (select* urls)
      (where {:id [= id]})
      select
      first))

