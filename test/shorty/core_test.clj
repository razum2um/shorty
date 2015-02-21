(ns shorty.core-test
  (:require [clojure.test :refer :all]
            [shorty.core :refer :all]))

(deftest fill-stop-server-fn
  (testing "start assigns stop-server-fn"
    (stop)
    (with-redefs [org.httpkit.server/run-server (constantly true)]
      (start)
      (is (= true @stop-server-fn)))))

