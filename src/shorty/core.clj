(ns shorty.core
  (:require [compojure.core :refer [GET POST defroutes]]
            [environ.core :refer [env]]
            [onelog.core :as log]
            [org.httpkit.server :as http]
            [ring.middleware.defaults :refer [api-defaults
                                              wrap-defaults]]
            [ring.middleware.logger :refer [wrap-with-logger]]
            [shorty.coder :refer [decode]]
            [shorty.db :refer [find-url]]
            [shorty.web :refer [>>= expand inc-stats redirect shorten
                                stats]])
  (:gen-class))

(def log "log/shorty.log")

(log/start! log)
(log/set-debug!)

(defroutes routes
  (POST "/shorten" [] shorten)
  (GET  "/statistics/:code" [code] (>>= code decode find-url stats))
  (GET  "/expand/:code" [code] (>>= code decode find-url expand))
  (GET  "/:code" [code] (>>= code decode find-url inc-stats redirect)))

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
             (http/run-server (-> #'routes
                                  (wrap-defaults api-defaults)
                                  wrap-with-logger)
                              {:port port*}))
     port*)))

(defn -main
  [& args]
  (println (str "Started listening on http://127.0.0.1:" (start)))
  (println "Check the log:" log))

(set! *warn-on-reflection* true)

