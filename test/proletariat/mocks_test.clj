(ns proletariat.mocks-test
  (:require [clojure.test :refer [deftest testing is]]
            [proletariat.mocks :refer :all]))

(defmulti foo (fn [a b] a))

(defmethod foo :bar [a b] [a b])

(defmethod foo :baz [a b] [a b])

(defn new-impl
  [a b]
  [a (inc b)])

(deftest multi-mocks
  (testing "multimock:"
    (testing "uses new impl"
      (multimock foo [:bar new-impl]
        (is (= [:bar 2] (foo :bar 1)))))

    (testing "only changes specified"
      (multimock foo [:bar new-impl]
        (is (= [:baz 1] (foo :baz 1)))))

    (testing "resets when done"
      (is (= [:bar 1] (foo :bar 1))))))
