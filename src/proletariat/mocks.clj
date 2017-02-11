(ns proletariat.mocks
  "Utilties for mocking parts of a program for testing different scenarios.
  Should *only* be used in unit tests as we are mucking about under the hood."
  (:import [clojure.lang MultiFn]))

(defn reset-methods
  ""
  [multi m]
  (remove-all-methods multi)
  (doseq [[dispatch f] m]
    (.addMethod ^MultiFn multi dispatch f)))

(defn replace-method
  ""
  [multi dispatch f]
  (remove-method multi dispatch)
  (.addMethod ^MultiFn multi dispatch f))

(defmacro multimock
  ""
  [multi bindings & body]
  ;; TODO: Validate Inputs
  `(let [orig# (methods ~multi)]
     (doseq [[dispatch# f#] (partition 2 ~bindings)]
       (replace-method ~multi dispatch# f#))
     ~@body
     (reset-methods ~multi orig#)))
