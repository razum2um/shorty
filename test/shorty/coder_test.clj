(ns shorty.coder-test
  (:require [clojure.test :refer :all]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.generators :as gen]
            [shorty.coder :refer :all]))

(deftest encoding
  (is (= "dQ9" (encode 6789))))

(deftest decoding
  (is (= 6789 (decode "dQ9"))))

(defspec roundtrip
  100 ;; the number of iterations for test.check to test
  (prop/for-all [i gen/pos-int]
                (= i (decode (encode i)))))

