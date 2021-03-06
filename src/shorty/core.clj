(ns shorty.core
  (:require [compojure.core :refer [GET POST defroutes routes]]
            [compojure.route :refer [resources not-found]]
            [environ.core :refer [env]]
            [onelog.core :as log]
            [org.httpkit.server :as http]
            [ring.middleware.defaults :refer [api-defaults
                                              wrap-defaults]]
            [ring.middleware.logger :refer [wrap-with-logger]]
            [shorty.coder :refer [decode]]
            [clojure.core.memoize :as memo]
            [shorty.db :as db :refer [find-url]]
            [shorty.web :refer [>>= expand inc-stats redirect shorten
                                stats]])
  (:gen-class))

(def log "log/shorty.log")

(def lru-threshold (or (:cache env) 500))

(log/start! log)
(log/set-debug!)

(def decode* (memo/lru decode :lru/threshold lru-threshold))
(def find-url* (memo/lru find-url :lru/threshold lru-threshold))

(def decode+find-url (comp find-url* decode*))

(defroutes post-routes
  (POST "/shorten" [] shorten))

(defroutes code-routes
  (GET  "/statistics/:code" [code] (>>= code decode* find-url stats))
  (GET  "/expand/:code"     [code] (>>= code decode+find-url expand))
  (GET  "/:code"            [code] (>>= code decode+find-url inc-stats redirect)))

(defroutes all-routes
  (routes
    post-routes
    (resources "/")
    code-routes
    (not-found "Not found")))

(def stop-server-fn (atom nil))

(defn stop []
  (when (and @stop-server-fn
             (fn? @stop-server-fn))
    (@stop-server-fn :timeout 100)))

(defn start
  ([] (start nil))
  ([port]
   (let [port* (int (or port (:port env) 8080))]
     (reset! stop-server-fn
             (http/run-server
               (-> #'all-routes (wrap-defaults api-defaults) wrap-with-logger)
               {:port port*}))
     (println (str "Started listening on http://127.0.0.1:" port*)))))

(defn -main
  [& args]
  (start)
  (println "Check the log:" log))

(set! *warn-on-reflection* true)

