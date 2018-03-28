(ns proletariat.core-test
  (:require [clojure.test :refer [deftest is]]
            [proletariat.core :as core]))

(deftest bytes->string-test
  (let [ba (byte-array [(byte 0x43)
                        (byte 0x6c)
                        (byte 0x6f)
                        (byte 0x6a)
                        (byte 0x75)
                        (byte 0x72)
                        (byte 0x65)
                        (byte 0x21)])]
    (is (= "Clojure!" (core/bytes->string ba)))))
