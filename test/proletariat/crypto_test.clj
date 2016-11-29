(ns proletariat.crypto-test
  (:require [clojure.test :refer [deftest testing is]]
            [proletariat.crypto :as crypto]))

(deftest encrypt&decrypt
  (testing "Decrypted text same as input"
    (let [input "Super secret code"
          k (crypto/symmetric-key)]
      (is (= input
             (->> input
                  (crypto/encrypt-str k)
                  (crypto/decrypt-str k)))))))
