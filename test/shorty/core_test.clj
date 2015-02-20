(ns shorty.core-test
  (:require [clojure.test :refer :all]
            [shorty.core :refer :all]))

(deftest a-test
  (testing "start assigns stop-server-fn"
    (stop)
    (binding [run-server (constantly true)]
      (start)
      (is (= true @stop-server-fn)))))

