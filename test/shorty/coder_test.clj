;; # Tests
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

;; Quick check в Haskell, EQS в Erlang, ScalaCheck в Scala
;; 100 - это количество попыток "взломать" тест
;; [видеообзор от автора](http://youtu.be/JMhNINPo__g) (на английском)
(defspec roundtrip 100
  (prop/for-all [i gen/pos-int]
                (= i (decode (encode i)))))

