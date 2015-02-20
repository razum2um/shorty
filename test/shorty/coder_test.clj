(ns shorty.coder-test
  (:require [clojure.test :refer :all]
            [clojure.test.check.clojure-test :refer [defspec]]
            [shorty.coder :refer :all]))

(deftest encoding
  (is (= "dQ9" (encode 6789))))

(deftest decoding
  (is (= 6789 (decode "dQ9"))))

