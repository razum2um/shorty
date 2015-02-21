# shorty

Yet another URL shortening service. Meant for educational purposes.

## Installation

    ./setup.sh

## Usage

    java -jar shorty-0.1.0-standalone.jar

    # shorten
    curl -i -X POST -H "Content-Type:application/x-www-form-urlencoded" -d "url=http://url-to-short.com/with?params=true" http://127.0.0.1:8080/shorten

    # redirect ("dgd" is the code)
    curl -i http://127.0.0.1:8080/dgd

    # expand ("dgd" is the code)
    curl -i http://127.0.0.1:8080/expand/dgd

    # stats ("dgd" is the code)
    curl -i http://127.0.0.1:8080/statictics/dgd

## REPL tricks

    ;; after setup fire in bash
    lein repl

    ;; the first thing you need: refreshing the code
    (clojure.tools.namespace.repl/refresh)

    ;; then just use the code, e.g this restarts webserver
    (do (shorty.core/stop) (clojure.tools.namespace.repl/refresh) (shorty.core/start))

    ;; run some test
    (clojure.test/run-tests 'shorty.web-test)

    ;; run all tests
    (clojure.test/run-all-tests)

    ;; update settings
    (alter-var-root #'environ.core/env #(merge % {:db "shorty_test" :user "razum2um" :password ""}))

    ;; disconnect from db
    (do (if-let [p (-> korma.db/_default deref :pool)] (-> p deref :datasource .close)) (reset! korma.db/_default nil))

## License

Copyright © 2015 Vlad Bokov

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

