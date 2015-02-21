(ns shorty.core
  (:require [compojure.core :refer [GET POST defroutes]]
            [environ.core :refer [env]]
            [onelog.core :as log]
            [org.httpkit.server :as http]
            [ring.middleware.defaults :refer [api-defaults
                                              wrap-defaults]]
            [ring.middleware.logger :refer [wrap-with-logger]]
            [shorty.db :refer [fetch-url]]
            [shorty.web :refer [>>= expand inc-stats redirect shorten
                                stats]])
  (:gen-class))

(log/start! "log/shorty.log")
(log/set-debug!)

(defroutes routes
  (POST "/shorten" [] shorten)
  (GET  "/statictics/:code" [code] (>>= code fetch-url stats))
  (GET  "/expand/:code" [code] (>>= code fetch-url expand))
  (GET  "/:code" [code] (>>= code fetch-url inc-stats redirect)))

(def stop-server-fn (atom nil))

(defn stop []
  (when (and @stop-server-fn (fn? @stop-server-fn))
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

