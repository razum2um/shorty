;; # Сокращалка URL или практическое введение  в Clojure
;;
;; <form action="/shorten" method="POST"><input name="url" placeholder="Исходная ссылка" required><button>Сократить</button></form>
;; <div id="result"><span>Сокращенная ссылка:</span><span id="shortened"></span></div>
;;
;; А теперь разберем, как это написано и как работает.
;;
;; ## Core
;; Как правило все либы и приложения на Clojure имеют namespace `core`
;; и разбор следует начинать с него.

;; По факту `ns` - макрос (как и `defn`, между прочим), который обслуживает импорты
;; это не более чем порядок. В любой момент можно сделать как в REPL так и в файле:
;;
;;     (require '[shorty.db :as db :refer [find-url]])
;;
;; Обратите внимание на `'` в консольном варианте. Однако, чтобы поиграть с либой в REPL
;; не надо руками импортировать все, а затем перезаругжать их после изменения файлов.
;; Для этого существует прекрасная библиотека `tools.namespace` и она подключена к этому проекту,
;; поэтому запустите REPL из корня библиотеки:
;;
;;     lein repl
;;
;; и выполните (после этого вы сможете копировать и пробовать у себя команды ниже):
;;
;;     (clojure.tools.namespace.repl/refresh)
(ns shorty.core
  (:require [compojure.core :refer [GET POST defroutes routes]]
            [environ.core :refer [env]]
            [onelog.core :as log]
            [org.httpkit.server :as http]
            [ring.middleware.defaults :refer [api-defaults
                                              wrap-defaults]]
            [ring.middleware.logger :refer [wrap-with-logger]]
            ;; импорт определенных функций из namespace
            [shorty.coder :refer [decode]]
            ;; или alias для него
            [clojure.core.memoize :as memo]
            ;; возможны оба варианта сразу
            [shorty.db :as db :refer [find-url]]
            [shorty.web :refer [>>= expand inc-stats redirect shorten
                                stats]])
  ;; дань JVM, обязательная для того, чтобы можно было скомпилировать `jar`
  ;; со входной точкой = этим классом и методом `-main` в нем
  (:gen-class))

;; Так определяются константы и функции.
(def log "log/shorty.log")


;; ### Префиксная нотация
;; Это, что может пугать в начале (помимо скобочек, естественно):
;; функции, операторы (которые, вообще-то тоже или функции, или макросы)
;; и методы JVM объектов приходится писать на первом месте (ниже я покажу как с этим бороться тоже)
;;
;;     pred1 || pred2 || pred3
;;
;; Однако в отличие от операторов в середине это не накладывает ограничение на количество:
;;
;;     (or pred1 pred2 pred3)
;;
;; А конкретно для логических операторов это может быть полезнее с точки зрения читателя. Что легче?
;;
;;     (puts "1") && (puts "2") or (puts "3") || (puts "4") and (puts "5")
;;
;; или
;;
;;     (or (and (println "1") (println "2"))
;;         (println "3")
;;         (and (println "4") (println "5")))
;;
;; Если вы думаете о маньяке, который знает, где вы живете, то все равно напишите скобочки
;;
;;     (((puts "1") && (puts "2")) or (puts "3")) || ((puts "4") and (puts "5"))
(def lru-threshold (or (:cache env) 500))

(log/start! log)
(log/set-debug!)

;; Одна из обобенностей ФП это использование функций высшего порядка
;; для получения новых функций с дополнительным поведением.
;;
;; Здесь функции `decode`, которая производит сложное вычисление,
;; и `find-url`, которая делает запрос к базе данных, "оборачиваются"
;; кеширующим поведением - функцией `clojure.core.memo/lru`, которая
;; работает как LRU кеш, т.е. N последних значений будут закешированы
;; в зависимости от аргументов, переданных исходным функциям.
;;
;; Этот кеш не учитывает ничего кроме аргументов, и одному набору может
;; соответствовать только одно закешированное значение, именно поэтому
;; важно писать "чистые" функции.
(def decode* (memo/lru decode :lru/threshold lru-threshold))
(def find-url* (memo/lru find-url :lru/threshold lru-threshold))

;; Другая из замечательнейших особенностей ФП:
;; способность легко сочетать функции. В принципе это эквивалентно
;;
;;     (defn decode+find-url [& args]
;;       (find-url* (apply decode args))
;;
;; В принципе, можно сочетать больше функций, и чем их больше, тем привлекательнее
;; становится `comp`, например:
;;
;;     (def next-ansi-for-first-letter (comp char inc int first))
;;     (defn next-ansi-for-first-letter [s]
;;       (char (inc (int (first s)))))
;;
;; Оба варианта работают одинаково:
;;
;;     (next-ansi-for-first-letter "a") ;; => \b
;;
;; Но, объективно говоря, от сбокочек можно избавиться с помощью макроса `clojure.core/->`,
;; поэтому, если вы дочитали до этого места, вы больше не будете троллить кложуристов скобочками,
;; т.к. их количество резко снижается так:
;;
;;     (defn next-ansi-for-first-letter [s]
;;       (-> s first int inc char))
;;
;; Кстати, порядок функций поменялся на естественный :)
;; Остальные нагромождения скобочек - это как правило "code smell"
(def decode+find-url (comp find-url* decode*))

;; Лиспы в общем-то знамениты именно способностью делать DSL
;; тут
;; Подробнее можно 
(defroutes post-routes
  (POST "/shorten" [] shorten))

;; `>>=` это тоже макрос, похожий на `->`, но добавляющий в конце "fallback" в виде
;; ответа 404, если сокращенного кода не зарегестрировано в базе.
;;
;; Посмотреть, что же получается после однократного раскрытия макросов можно так:
;;
;;     (macroexpand-1 '(shorty.web/>>= "value" identity))
;;
;;     (clojure.core/or
;;       (clojure.core/-> "value" identity)
;;       (shorty.web/not-found-resp "value"))
;;
;; Обратите внимание на ответ, где `or` и `->` - тоже макросы.
;; Полностью раскрывает макрос:
;;
;;     (clojure.walk/macroexpand-all '(shorty.web/>>= "value" identity))
;;
;;     (let* [or__3975__auto__ (identity "value")]
;;       (if or__3975__auto__
;;         or__3975__auto__
;;         (shorty.web/not-found-resp "value")))
;;
;; `>>=` имеет немного общего с хаскельным `>>=`, но это разные вещи.
;; Тот, кому знакомы монады, может представить, как будто `code` оборачивается в монаду
;; `Maybe` и затем передается по цепочке "раскодировать" - "найти в базе" - "отдать ответ"
;;
;; Остальные могут представить себе, что существует несколько функций для каждого этапа
;; обработки и выбор между ними основывается на результате выполнения еще одной функции.
;; Все это называется "мультиметодом", а та "другая" функция "диспатч-функция". Например:
;;
;;     (defmulti inc-unless-nil class)
;;     (defmethod inc-unless-nil nil [_] nil)
;;     (defmethod inc-unless-nil :default [i] (inc i))
;;
;;     (inc-unless-nil nil) ;; => nil
;;     (inc-unless-nil 1)   ;; => 2
;;
;; Таким образом, если мы уверены, что каждый этап цепочки обрабатывает мультиметод,
;; который "диспатчится" по классу (а `nil` это тоже особый класс), то их соединение через `->`
;; приведет к тому, что в результате мы либо получим результат, либо `nil` и произойдет вызов `else`
;; ветви из кода, который сгенерировал макрос `>>=`.
;; Такие мультиметоды генерируются макросом `shorty.utils/def-maybe`.
(defroutes code-routes
  (GET  "/statistics/:code" [code] (>>= code decode* find-url stats))
  (GET  "/expand/:code"     [code] (>>= code decode+find-url expand))
  (GET  "/:code"            [code] (>>= code decode+find-url inc-stats redirect)))

;; Роуты композитятся так же легко как и функции. Сопоставление будет происходить
;; "сверху-вниз", т.е. сначала REQUEST_URI будет сопоставляться с `/shorten`
(defroutes all-routes
  (routes
    post-routes
    code-routes))

;; `atom` - это один из 3х способов обрабатывать состояние в Clojure. Подробнее о нем и
;; остальных вы можете на [отличном докладе Николая Рыжикова](http://youtu.be/nfKrSI7OQnI?t=2h21m1s)
;; (тоже на русском, кстати)
(def stop-server-fn (atom nil))

(defn stop []
  (when (and @stop-server-fn
             (fn? @stop-server-fn))
    (@stop-server-fn :timeout 100)))

;; Функция в Clojure может принимать разное количество аргументов
(defn start
  ([] (start nil))
  ([port]
   (let [port* (int (or port (:port env) 8080))]
     (reset! stop-server-fn
             (http/run-server
               (-> #'all-routes (wrap-defaults api-defaults) wrap-with-logger)
               {:port port*}))
     (println (str "Started listening on http://127.0.0.1:" port*)))))

;; По умолчанию этот метод вызывается, если запускать запакованный `jar`
(defn -main
  [& args]
  (start)
  (println "Check the log:" log))

(set! *warn-on-reflection* true)

