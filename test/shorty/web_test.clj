(ns shorty.web-test
  (:require [clojure.test :refer :all]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.generators :as gen]
            [shorty.web :refer :all]
            [shorty.utils :refer [defn-maybe]]))

(defn- as-params [url] {:params {:url url}})
(defn- as-row [url] {:id 999 :open_count 99 :url url :code "123"})

(defmacro with-stubs [[io-name io-val] & body]
  `(let [~io-name ~io-val]
     (with-redefs [shorty.db/create-url (fn [~'_] {:id (swap! ~io-name inc)})
                   shorty.db/update-url (fn [~'_] 1)
                   shorty.db/increment-counter (fn [~'_] {:id (swap! ~io-name inc)})
                   clojure.core/future (fn [~'f] ~'f)]
       ~@body)))

(deftest shorten-success
  (with-stubs [db (atom 12345)]
    (are [x y]
         (let [[http-code code] x
               resp (-> y as-params shorten)]
           (and (= http-code (:status resp))
                (re-find code (:body resp))))
         [200 #"hjY"]  "http://ya.ru"
         [200 #"hjZ"]  "https://ya.ru")))

(deftest shorten-fail
  (with-stubs [db (atom 12345)]
    (are [x y] (= x (-> y as-params shorten ((juxt :status :body))))
         [400 "url is invalid"]  "ya.ru"
         [400 "url is invalid"]  "http://yaru"
         [400 "url is invalid"]  "http://yaru"
         [400 "url is invalid"]  "http://y.a.r.u"
         [400 "url is invalid"]  "htp://ya.ru"
         [400 "url is invalid, can't be blank"]  "")))

(deftest stats-side-effect
  (with-stubs [db (atom 1)]
    (is (= {} (inc-stats {})))
    (is (= 2 @db))))

(deftest maybe-chain-success
  (with-stubs [db (atom 99)]
    (is (= (resp 302 {"Location" "http://ya.ru"} "Redirected to http://ya.ru")
           (>>= "http://ya.ru" as-row inc-stats redirect)))

    (is (= (resp 200 "http://ya.ru")
           (>>= "http://ya.ru" as-row expand)))

    (is (= (resp 200 "99")
           (>>= "http://ya.ru" as-row stats)))))

(deftest maybe-chain-fallback
  (let [fail-fn (constantly nil)]
    (is (= (resp 404 "No such code found: 1") (>>= 1 fail-fn redirect)))
    (is (= (resp 404 "No such code found: 2") (>>= 2 fail-fn expand)))
    (is (= (resp 404 "No such code found: 3") (>>= 3 fail-fn inc-stats)))))

