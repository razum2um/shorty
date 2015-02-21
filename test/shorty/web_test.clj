(ns shorty.web-test
  (:require [clojure.test :refer :all]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.generators :as gen]
            [shorty.web :refer :all]
            [shorty.utils :refer [defn-maybe]]))

(alter-var-root #'shorty.web/domain (constantly "http://some-doma.in"))

(defn- as-params [url] {:params {:url url}})
(defn- as-row [url] {:id 999 :hits 0 :url url})

(defmacro with-stubs [[io-name io-val] & body]
  `(let [~io-name ~io-val]
     (with-redefs [shorty.db/create-url (fn [~'_] {:id (swap! ~io-name inc)})
                   shorty.db/increment-counter (fn [~'_] {:id (swap! ~io-name inc)})
                   clojure.core/future identity]
       ~@body)))

(deftest shorten-success
  (with-stubs [db (atom 12345)]
    (are [x y] (= x (-> y as-params shorten ((juxt :status :body))))
         [200 "http://some-doma.in/hjY"]  "http://ya.ru"
         [200 "http://some-doma.in/hjZ"]  "https://ya.ru"
         )))

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
  (with-stubs [db (atom 0)]
    (is (= {:status 302, :headers {"Location" "http://ya.ru"}, :body "Redirected to http://ya.ru"}
           (>>= "http://ya.ru" as-row redirect)))

    (is (= {:status 200, :body "http://ya.ru"}
           (>>= "http://ya.ru" as-row expand)))

    (is (= {:id 999, :hits 0, :url "http://ya.ru"}
           (>>= "http://ya.ru" as-row inc-stats)))))

(deftest maybe-chain-fallback
  (let [fail-fn (constantly nil)]
    (is (= {:status 404, :body "No such code found: 1"} (>>= 1 fail-fn redirect)))
    (is (= {:status 404, :body "No such code found: 2"} (>>= 2 fail-fn expand)))
    (is (= {:status 404, :body "No such code found: 3"} (>>= 3 fail-fn inc-stats)))))

