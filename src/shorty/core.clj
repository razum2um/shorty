(ns shorty.core
  (:require [org.httpkit.server :as http]
            [compojure.core :refer [defroutes GET POST ANY context]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.logger :refer [wrap-with-logger]]
            [environ.core :refer [env]]
            [clojure.core.async :refer [thread-call]]
            [onelog.core :as log]
            [korma.core :refer :all]
            [korma.db :refer :all])
  (:gen-class))

(log/start! "shorty.log")
(log/set-debug!)

(use 'aprint.core)
(use 'debugger.core)

(defn env!
  ([k]
   (or (k env)
       (throw (Throwable. (str "specify -D" (name k) "=...")))))
  ([k & tail]
   (into {} (map (juxt identity env!) (conj tail k)))))

(defn presence [x] (if (empty? x) nil x))

;; coder

(def domain "http://some-doma.in")
(def alphabet (vec "bcdfghjkmnpqrstvwxyz23456789BCDFGHJKLMNPQRSTVWXYZ"))
(def base (count alphabet))

(defn encode
  ([i] (encode '() i))
  ([acc i]
   (if (> i 0)
     (let [next-sym (alphabet (rem i base))
           ;; (/ i base) is rational
           next-i (unchecked-divide-int i base)]
       (recur (conj acc next-sym) next-i))
     (apply str acc))))

(defn decode [s]
  (loop [id 0 acc (seq s)]
    (if-let [[chr & tail] acc]
      (recur (+ (* id base) (.indexOf alphabet chr)) tail)
      id)))

;; db

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

;; web

(defn error-resp [body]
  {:status 400 :body body})

(defn not-found-resp [code]
  {:status 404 :body (str "No such code found: " code)})

(defn show-code-resp [code]
  {:status 200 :body (str domain "/" code)})

(defmacro defn-maybe [x & body]
  `(do (defmulti ~x class)
       (defmethod ~x nil [~'_] nil)
       (defmethod ~x :default ~@body)))

(defn-maybe stats [{:keys [hits] :as row}]
  (log/debug row)
  {:status 200 :body (str hits)})

(defn-maybe expand [{:keys [url] :as row}]
  {:status 200 :body (str url)})

(defn-maybe inc-stats [{:keys [id] :as row}]
  (thread-call (fn [] (increment-counter id) nil))
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
                     (let [{:keys [id]} (create-url url)]
                       (show-code-resp (encode id)))))

(defmacro >>= [x & fns]
  `(or (-> ~x ~@fns)
       (not-found-resp ~x)))

;;

(defroutes routes
  (POST "/shorten" [] shorten)
  (GET  "/statictics/:code" [code] (>>= code fetch-url stats))
  (GET  "/expand/:code" [code] (>>= code fetch-url expand))
  (GET  "/:code" [code] (>>= code fetch-url inc-stats redirect))
  )

(defonce stop-server-fn (atom nil))

(defn stop []
  (when @stop-server-fn
    (@stop-server-fn :timeout 100)))

(defn start []
  (reset! stop-server-fn
          (http/run-server (-> #'routes
                               (wrap-defaults api-defaults)
                               wrap-with-logger)
                           {:port (or (:port env) 8080)})))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

(set! *warn-on-reflection* true)

