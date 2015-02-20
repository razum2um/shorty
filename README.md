# shorty

FIXME: description

## Installation

Download from http://example.com/FIXME.

## Usage

FIXME: explanation

    $ java -jar shorty-0.1.0-standalone.jar [args]

## Options

FIXME: listing of options this app accepts.

## REPL tricks

    ;; refreshes the code
    (clojure.tools.namespace.repl/refresh)

    ;; restarts webserver
    (do (shorty.core/stop) (clojure.tools.namespace.repl/refresh) (shorty.core/start))

    ;; disconnects from db
    (do (if-let [p (-> korma.db/_default deref :pool)] (-> p deref :datasource .close)) (reset! korma.db/_default nil))

### Bugs

...

### Any Other Sections
### That You Think
### Might be Useful

## License

Copyright © 2015 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
