(ns shorty.core
  (:require [org.httpkit.server :as http]
            [compojure.core :refer [defroutes GET POST ANY context]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.logger :refer [wrap-with-logger]]
            [environ.core :refer [env]]
            [clojure.core.async :refer [thread-call]]
            [onelog.core :as log]
            [korma.core :refer :all]
            [korma.db :refer :all]
            [shorty.db :refer [fetch-url]]
            [shorty.web :refer [>>= shorten stats expand inc-stats redirect]])
  (:gen-class))

(log/start! "shorty.log")
(log/set-debug!)

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

