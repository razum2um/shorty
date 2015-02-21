(ns shorty.db-test
  (:require [clojure.test :refer :all]
            [clojure.java.jdbc :as jdbc]
            [korma.db :refer :all]
            [korma.core :refer :all]
            [shorty.db :refer :all]))

;; ### Макрос - основное эквивалент `before/alter` блоков
(defmacro with-clean-db [& body]
  `(try
     (jdbc/db-do-commands jdbc-spec "TRUNCATE urls;")
     (jdbc/db-do-commands jdbc-spec "ALTER SEQUENCE urls_id_seq RESTART WITH 5000;")
     ~@body
     (finally
       (jdbc/db-do-commands jdbc-spec "TRUNCATE urls;")
       )))

(deftest insertion
  (with-clean-db
    (create-url {:url "http://ya.ru"})
    (is (= '(["http://ya.ru" nil 0])
           (->> (select urls) (map (juxt :url :code :open_count)))))))

(deftest fetching
  (with-clean-db
    (let [{:keys [id] :as url} (insert urls (values {:url "http://ya.ru"}))
          fetched (find-url id)]
      (is (= url fetched)))))

(deftest updating
  (with-clean-db
    (let [url (insert urls (values {:url "http://ya.ru"}))]
      (update-url (assoc url :code "123"))
      (is (= '(["http://ya.ru" "123" 0])
             (->> (select urls) (map (juxt :url :code :open_count))))))))

