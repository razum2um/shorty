;; # Сокращалка URL или практическое введение  в Clojure
;;
;; <form action="/shorten" method="POST"><input name="url" placeholder="Исходная ссылка" required><button>Сократить</button></form>
;; <div id="result" style="display: none"><span>Сокращенная ссылка:</span><span id="shortened"></span></div>
;;
;; После того, как мы разберем, как это написано и как работает, я думаю, что вы сможете
;; писать на Clojure, причем на достаточно *адекватном* уровне (подразумевая, что вы умеете программировать, конечно).
;; Я подчеркиваю это, потому что есть много руководств по Scala, которые обещают вам быстрое "погружение",
;; но на самом деле все сложно :)
;;
;; Порог вхождения у *идеоматичной* Scala намного выше, чем у Clojure (я писал на обоих). И я уверен,
;; что вам должно хватить этого введения, чтобы разобраться в коде *любой* pure-clojure библиотеки, а потом
;; посмотрите исходники скала-библиотек - *slick*, например (а потом библиотекой может оказаться код модуля
;; в *вашем* проекте, который писали не вы).
;;
;; Однако главной задачей этого введения является не только ознакомить,
;; но и показать инструменты, с помощью которых дальнейшие самостоятельные шаги в мире Clojure
;; будут приятны и осознаны.
;;
;; ## Core
;; Как правило все либы и приложения на Clojure имеют namespace `core`
;; и разбор следует начинать с него.

;; По факту `ns` - макрос (как и `defn`, между прочим, но о них чуть ниже), который обслуживает импорты
;; это не более, чем принятый порядок. Например, в Python:
;;
;;     import foo
;;     from foo import bar
;;
;; В любой момент можно сделать как в REPL так и в файле (но только для библиотек-зависимостей проекта):
;;
;;     (require '[shorty.db :as db :refer [find-url]])
;;
;; О файле `project.clj` также будет несколько слов ниже, а пока обратите внимание на `'` в консольном варианте.
;; Однако, чтобы поиграть с либой в REPL не надо торопиться руками импортировать все, а затем перезаругжать 
;; их после изменения файлов. Для этого существует прекрасная библиотека `tools.namespace`
;; и она подключена к этому проекту, поэтому запустите REPL из корня репозитория
;; (предварительно [установив lein](http://leiningen.org/)):
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

;; ### Константы и функции
;;
;; Для определения используется `def`, а знак `=` является функцией сравнения:
;;
;;     (= 2 2 2) ;; => true
;;     (= 1 1.0) ;; => false
;;
;; В этом можно убедиться так:
;;
;;     (clojure.repl/source =)
;;
;;     (defn =
;;       "Equality. Returns true if x equals y, false if not. Same as
;;       Java x.equals(y) except it also works for nil, and compares
;;       numbers and collections in a type-independent manner.  Clojure's immutable data
;;       structures define equals() (and thus =) as a value, not an identity,
;;       comparison."
;;       {:inline (fn [x y] `(. clojure.lang.Util equiv ~x ~y))
;;        :inline-arities #{2}
;;        :added "1.0"}
;;       ([x] true)
;;       ([x y] (clojure.lang.Util/equiv x y))
;;       ([x y & more]
;;        (if (clojure.lang.Util/equiv x y)
;;          (if (next more)
;;            (recur y (first more) (next more))
;;            (clojure.lang.Util/equiv y (first more)))
;;          false)))
;;
;; В принципе, код любой pure-clojure функции можно так происпектировать как из
;; любопытства, так и чтобы быстро вспомнить сигнатуру функции.
;;
;; О том, что использовать вместо императивного `x = 1`, речь ниже.
(def log "log/shorty.log")


;; ### Префиксная нотация
;;
;;     (or (:cache env) 500)
;;
;; Это то, что может беспокоить в начале (помимо скобочек, естественно):
;; функции, операторы (которые, вообще-то тоже или функции, или макросы)
;; и методы Java-объектов приходится писать на первом месте (ниже я покажу как с этим бороться)
;; вместо привычного:
;;
;;     pred1 || pred2 || pred3
;;
;; Префиксная нотация в отличие от операторов в середине это не накладывает ограничение на количество:
;;
;;     (or pred1 pred2 pred3)
;;
;; А конкретно для логических операторов это может быть полезнее с точки зрения читателя.
;; Что легче понять (пример на Ruby):
;;
;;     (puts "1") && (puts "2") or (puts "3") || (puts "4") and (puts "5")
;;
;; или
;;
;;     (or (and (println "1") (println "2"))
;;         (println "3")
;;         (and (println "4") (println "5")))
;;
;; Поэтому, если вы думаете о том самом маньяке, который знает, где вы живете,
;; то все равно напишите скобочки даже в императивном коде:
;;
;;     (((puts "1") && (puts "2")) or (puts "3")) || ((puts "4") and (puts "5"))
(def lru-threshold (or (:cache env) 500))

;; ### Вызов функции
;;
;; Более того, в Clojure, делая макрос, вы можете быть уверенными, что в начале каждого
;; уровня следует функция.
(log/start! log)

;; ### Debug
;; Вначале весьма сложно разобраться в "выхлопе" stacktrace, непонятных ошибках и весьма нехватает всей мощи брекпойнтов
;; в теплой ламповой IDE. Скажу честно, дела с поддержкой Clojure в традиционных Java IDE еще неважны. Основной
;; вектор развития направлен на [cider](https://github.com/clojure-emacs/cider) проект для Emacs.
;;
;; C дебаггерами в экосистеме еще хуже :(
;; Есть попытка решить эту проблему в виде библиотеки [debugger](https://github.com/razum2um/clj-debugger),
;; но она пока очень ограничена в возможностях.
;;
;; В общем, хорошее логгирование - это наше все :) Однако и тут лучше сразу использовать
;; [aprint](https://github.com/razum2um/aprint) вместо стандартных `println`
;; или `clojure.pprint/pprint` для "ad-hoc" дебага, или [одну](https://github.com/pjlegato/onelog)
;; [из](https://github.com/ptaoussanis/timbre) [систем](https://github.com/clojure/tools.logging) логгирования.
;;
;; Сборники ссылок на библиотеки по категориям (в том числе для дебага) можно найти в списке
;; [awesome-clojure](https://github.com/razum2um/awesome-clojure) и в [clojure toolbox](http://www.clojure-toolbox.com).
(log/set-debug!)

;; ### Higher-order functions
;;
;; Одна из обобенностей ФП это использование функций высшего порядка
;; для получения новых функций с дополнительным поведением.
;;
;; Здесь функции `decode`, которая производит сложное вычисление,
;; и `find-url`, которая делает запрос к базе данных, "оборачиваются"
;; кеширующим поведением - функцией `clojure.core.memo/lru`, которая
;; работает как LRU кеш, т.е. N последних использованных значений остаются в кеше
;; в зависимости от аргументов, переданных исходным функциям.
;;
;; Этот кеш не учитывает ничего кроме аргументов, и одному набору может
;; соответствовать только одно закешированное значение, именно поэтому
;; важно писать "чистые" функции - они беспроблемно композитятся.
(def decode* (memo/lru decode :lru/threshold lru-threshold))
(def find-url* (memo/lru find-url :lru/threshold lru-threshold))

;; ### Композиция
;;
;; Другая из замечательнейших особенностей ФП:
;; способность легко сочетать функции. В принципе, это эквивалентно
;;
;;     (defn decode+find-url [& args]
;;       (find-url* (apply decode args))
;;
;; Можно сочетать больше функций, и чем их больше, тем привлекательнее
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
;; поэтому, если вы дочитали до этого места, вы больше не будете троллить лисперов скобочками,
;; т.к. их количество резко снижается таким макросом:
;;
;;     (defn next-ansi-for-first-letter [s]
;;       (-> s first int inc char))
;;
;; Кстати, порядок функций поменялся на естественный :)
;; Остальные нагромождения скобочек - это как правило "code smell"
(def decode+find-url (comp find-url* decode*))

;; ### DSL
;;
;; Лиспы в общем-то знамениты именно способностью делать DSL.
;;
;; Подробнее о правилах роутинга и захвата переменных из url можно
;; [в вики](https://github.com/weavejester/compojure/wiki/Routes-In-Detail) `compojure`.
(defroutes post-routes
  (POST "/shorten" [] shorten))

;; ### Макросы
;;
;; О макросах можно думать, как о коде, который генерирует (чаще всего шаблонный) код. Компилироваться
;; он будет так, как будто вы не поленились и <s>написали</s> скопипастили и поправили его везде.
;;
;; `>>=` это тоже макрос, похожий на `clojure.core/->`, но добавляющий в конце "fallback" в виде
;; ответа 404 от обработчика `shorty.web/not-found`, если сокращенного кода не зарегестрировано в базе.
;;
;; Убедиться в этом можно, посмотрев, что же получается после однократного раскрытия макросов:
;;
;;     (macroexpand-1 '(shorty.web/>>= "value" identity))
;;
;;     (clojure.core/or
;;       (clojure.core/-> "value" identity)
;;       (shorty.web/not-found-resp "value"))
;;
;; Обратите внимание на ответ, где `or` и `->` - тоже макросы.
;; Полностью раскрыть макросы можно так:
;;
;;     (clojure.walk/macroexpand-all '(shorty.web/>>= "value" identity))
;;
;;     (let* [or__3975__auto__ (identity "value")]
;;       (if or__3975__auto__
;;         or__3975__auto__
;;         (shorty.web/not-found-resp "value")))
;;
;; Если заменить "страшые" имена (munged, введенные для исключения пересечений), то
;; получится простой и "прямой" код, без труда доступный для понимания.
;;
;; `>>=` имеет немного общего с хаскельным `>>=`, но это разные вещи.
;; Тот, кому знакомы монады, может представить, как будто `code` оборачивается в монаду
;; `Maybe` и затем передается по цепочке "раскодировать" - "найти в базе" - "отдать ответ".
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
;; Такие мультиметоды генерируются макросом `shorty.utils/defn-maybe`.
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

