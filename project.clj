(defproject shorty "0.1.0-SNAPSHOT"
  :description "URL shortening service"
  :url "http://github.com/razum2um/shorty"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [commons-validator/commons-validator "1.4.1"]
                 [com.novemberain/validateur "2.4.2"]
                 [http-kit "2.1.19"]
                 [compojure "1.3.2"]
                 [postgresql "9.3-1102.jdbc41"]
                 [korma "0.4.0" :exclusions [c3p0]]
                 [com.mchange/c3p0 "0.9.5"]
                 [ring/ring-defaults "0.1.4"]
                 [ring.middleware.logger "0.5.0"]
                 [environ "1.0.0"]]
  :plugins [[lein-environ "1.0.0"]
            [lein-pprint "1.1.1"]]
  :main ^:skip-aot shorty.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :dev [:test-common :dev-common :dev-overrides]
             :dev-common {:dependencies [[javax.servlet/servlet-api "2.5"]
                                         [slamhound "1.5.5"]
                                         ;; [org.clojure/clojure "1.7.0-alpha5"]
                                         [org.clojure/tools.namespace "0.2.9"]
                                         [debugger "0.1.4"]
                                         [aprint "0.1.3"]]
                          :env {:port 8080}}
             :dev-overrides {} ;; see profiles.clj
             :test [:test-common :test-overrides]
             :test-common {:dependencies [[org.clojure/test.check "0.7.0"]]
                           :env {:port 7070}}
             :test-overrides {}}
  :jvm-opts ["-XX:-OmitStackTraceInFastThrow"])

