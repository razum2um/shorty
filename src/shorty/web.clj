;; ## Controller & View layers
;;
;; В данном конкретном проекте ответы сервера очень просты и не требуют
;; шаблонов, поэтому можно рассматривать их вместе с "контроллерами" (в терминах Rails).
(ns shorty.web
  (:require [validateur.validation :refer :all]
            [environ.core :refer [env]]
            [clojure.string :refer [join]]
            [clojure.set :refer [union]]
            [onelog.core :as log]
            [shorty.coder :refer [encode]]
            [shorty.db :as db]
            [shorty.utils :refer [defn-maybe presence url-validator]]))

;; `^:dynamic` - знак для того, чтобы иметь возможность переопределять
;; переменную в скопе теста с помощью `binding`
(def ^:dynamic domain (or (:domain env) "http://127.0.0.1"))

(defn resp
  ([code body] (resp code {"Content-Type" "text/plain"} body))
  ([code headers body]
   {:status code :headers headers :body body}))

;; ### Threading macros
;;
;; По аналогии с `clojure.core/->` существует `clojure.core/->>`, который
;; подставляет результат каждого шага из цепочки в качестве последнего
;; агрумента нового шага.
(defn error-resp [errors]
  (resp 400 (->> errors vals flatten (apply union) (join ", "))))

(defn not-found-resp [code]
  (resp 404 (str "No such code found: " code)))

;; ### Destruction (~ Parrent Matching)
;;
;; Обратите внимание на запись параметров функции:
;;
;;     [{:keys [code] :as row}]
;;
;; Если мы передаем в функцию агрумент `{:code "123"}`, то внутри функции локальная переменная
;; `url` равна `{:code "123"}`, a `code` - "123".
;;
;; Советую отличное [разъяснение](http://blog.jayfields.com/2010/07/clojure-destructuring.html)
;; как оно работает на примерах.
(defn show-code-resp [{:keys [code] :as row}]
  (resp 200 (str domain "/" code)))

(defn-maybe stats [{:keys [open_count] :as row}]
  (resp 200 (str open_count)))

(defn-maybe expand [{:keys [url] :as row}]
  (resp 200  (str url)))

;; ### Future
;;
;; Простой способ получить фоновый поток. Возвращает promise, который можно блокирующе
;; получить через `deref` или `@`
;;
;; А еще можно активно использовать `map` и в один момент заменить его на `pmap` :)
(defn-maybe inc-stats [{:keys [id] :as row}]
  (future (db/increment-counter id))
  row)

(defn-maybe redirect [{:keys [url] :as row}]
  (resp 302 {"Location" url} (str "Redirected to " url)))

(def shorten-validator
  (validation-set
    (presence-of :url)
    (validate-by :url url-validator :message "url is invalid")))

;; ### Immutability
;;
;; Иммутабельность в этом конкретном примере заставляет получать новый
;; объект-хеш `url*` с помощью `asssoc`. Смысл этого в том, что мы могли бы
;; спокойно отдать старый `url` в другой тред не беспокоясь о синхронизации
;; доступа. В данном случае старый объект остался бы без изменений, как был.
;; Такой подход к написанию многопоточных программ снижает количество
;; дедлоков и "гонок".
;;
;; Важно заметить, что при `assoc` не происходит двойного выделения памяти,
;; а структуры "шарят" общую часть и новая структура лишь содержит измененную часть +
;; ссылку на "хвост" (что немного напоминает `LinkedList`).
;; Есть [подробное видео](http://www.youtube.com/watch?v=mS264h8KGwk) про ту же идею
;; на фронтенде с детальным объяснением `persisted data structures`.
;;
;; Для совместного доступа к общим данным существуют `atom`, `agent`, `ref`.
;; Ссылка на доклад по ним дана выше.
(defn shorten [{:keys [params] :as req}]
  (if (valid? shorten-validator params)
    (let [{:keys [id] :as url} (db/create-url (select-keys params [:url]))
          code (encode id)
          url* (assoc url :code code)]
      (future (db/update-url url*))
      (show-code-resp url*))
    (-> params shorten-validator error-resp)))

(defmacro >>= [x & fns]
  `(or (-> ~x ~@fns)
       (not-found-resp ~x)))

