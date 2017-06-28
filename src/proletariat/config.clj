(ns proletariat.config
  (:require [aero.core :as aero]
            [clojure.string :as string]
            [proletariat.core :as core]
            [proletariat.crypto :as crypto]
            [clojure.tools.logging :as log]
            [clojure.java.io :as io]))

(def ^:dynamic *silent* false)

(defmethod aero/reader 'encrypted
  [_ _ [k data]]
  (aero/deferred
    (try
      (let [file-resource (io/file k)
            contents      (if (.exists file-resource)
                            (slurp file-resource)
                            (core/slurp-resource k))]
        (-> contents
            string/trim-newline
            (crypto/decrypt data)))
      (catch Exception e
        (when (not *silent*)
          (log/error e "Failed to parse encrypted configuration")
          (throw e))))))

(defn read-config
  [profile resource]
  (let [file-resource (io/file resource)
        source        (if (.exists file-resource)
                        file-resource
                        (io/resource resource))]
    (aero/read-config source {:profile profile})))
