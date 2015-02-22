# shorty

Yet another URL shortening service. Meant for educational purposes.

[![Build Status][BS img]][Build Status]

## Installation

    ./setup.sh

## Usage

    # give database credentials to run jar
    java -Ddb=foo -Duser=bar -Dpassword=baz -jar shorty-0.1.0-standalone.jar

    # or export them as shell vars
    DB=foo USER=bar PASSWORD=baz java -jar shorty-0.1.0-standalone.jar

    # shorten
    curl -i -X POST -H "Content-Type:application/x-www-form-urlencoded" -d "url=http://url-to-short.com/with?params=true" http://127.0.0.1:8080/shorten

    # redirect ("dgd" is the code)
    curl -i http://127.0.0.1:8080/dgd

    # expand ("dgd" is the code)
    curl -i http://127.0.0.1:8080/expand/dgd

    # stats ("dgd" is the code)
    curl -i http://127.0.0.1:8080/statistics/dgd

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
    (alter-var-root #'environ.core/env
                    #(merge % {:db "foo" :user "bar" :password "baz"}))

    ;; disconnect from db
    (do (if-let [p (-> korma.db/_default deref :pool)]
        (-> p deref :datasource .close))
        (reset! korma.db/_default nil))

    ;; generate docs
    (marginalia.core/run-marginalia
      ["-c" "style.css"]
      {:marginalia {:ordering ["src/shorty/core.clj" "src/shorty/utils.clj" "src/shorty/coder.clj"
                               "src/shorty/db.clj" "src/shorty/web.clj"
                               "test/shorty/coder_test.clj" "test/shorty/db_test.clj"
                               "test/shorty/web_test.clj" "test/shorty/core_test.clj"]}})

    ;; build project for deploy
    lein uberjar

    ;; run test (bash) after ./setup.sh
    DB="shorty_test" lein with-profile +test test

## Benchmark

    $ siege -b -q -r 50 -c 50 -i -f urls.txt

    Transactions            : 3794 hits
    Availability            : 100.00 %
    Elapsed time            : 11.20 secs
    Data transferred        : 32.14 MB
    Response time           : 0.11 secs
    Transaction rate        : 338.75 trans/sec
    Throughput              : 2.87 MB/sec
    Concurrency             : 37.88
    Successful transactions : 3794
    Failed transactions     : 0
    Longest transaction     : 0.90
    Shortest transaction    : 0.00

## License

Copyright © 2015 Vlad Bokov

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

[BS img]: https://travis-ci.org/razum2um/shorty.png
[Build Status]: https://travis-ci.org/razum2um/shorty

