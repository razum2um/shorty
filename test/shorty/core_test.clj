(ns shorty.core-test
  (:require [clojure.test :refer :all]
            [clojure.string :as s]
            [korma.core :refer :all]
            [org.httpkit.client :as http]
            [shorty.core :refer :all]
            [shorty.db :as db]
            [shorty.db-test :refer [with-clean-db]]))

(deftest fill-stop-server-fn
  (testing "start assigns stop-server-fn"
    (stop)
    (with-redefs [org.httpkit.server/run-server (constantly true)]
      (start)
      (is (= true @stop-server-fn)))))

;; integration

(def port 7070)
(alter-var-root #'shorty.web/domain (constantly (str "http://127.0.0.1:" port)))

(def status+body (juxt :status :body))

(defn ->url [s]
  (if (.startsWith s shorty.web/domain)
    s
    (str "http://127.0.0.1:" port s)))

(defn post! [url-path payload]
  (-> url-path
      ->url
      (http/post {:form-params payload})
      deref
      status+body))

(defn get! [& url-paths]
  (-> (apply str url-paths) ->url (http/get {:follow-redirects false}) deref status+body))

(defmacro with-server [& body]
  `(with-redefs [clojure.core/future (fn [~'f] ~'f)]
     (with-clean-db
       (start port)
       ~@body
       (stop))))

(deftest shortening
  (with-server
    (are [x y] (= x y)
         [200 (str shorty.web/domain "/dgd")] (post! "/shorten" {:url "http://ya.ru?x=1&y=2"})
         [200 (str shorty.web/domain "/dgf")] (post! "/shorten" {:url "http://google.com"}))))

(deftest shortening-fail
  (with-server
    (are [x y] (= x y)
          [400 "url is invalid"]                 (post! "/shorten" {:url "gabrage"})
          [400 "url is invalid, can't be blank"] (post! "/shorten" {:url ""}))))

(deftest redirecting
  (with-server
    (let [[_ url] (post! "/shorten" {:url "http://ya.ru?x=1&y=2"})]
      (are [x y] (= x y)
           [302 "Redirected to http://ya.ru?x=1&y=2"] (get! url)
           [404 "No such code found: fake-code"]      (get! "/fake-code")))))

(deftest expanding
  (with-server
    (let [[_ url] (post! "/shorten" {:url "http://ya.ru?x=1&y=2"})
          code (s/replace url shorty.web/domain "")]
      (are [x y] (= x y)
           [200 "http://ya.ru?x=1&y=2"] (get! "/expand" code)
           [404 "No such code found: fake"] (get! "/expand" "/fake")))))

(deftest statistics
  (with-server
    (let [[_ url] (post! "/shorten" {:url "http://ya.ru?x=1&y=2"})
          code (s/replace url shorty.web/domain "")
          _ (get! url)
          _ (get! url)]
      (are [x y] (= x y)
           [200 "2"]                       (get! "/statistics" code)
           [404 "No such code found: bad"] (get! "/statistics" "/bad")))))

