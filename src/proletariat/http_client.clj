(ns proletariat.http-client
  "Provides simplified [Aleph HTTP Client](https://github.com/ztellman/aleph)
  builders and utilities"
  (:require [aleph.http :as aleph-http]
            [clojure.java.io :as io]
            [clojure.spec.alpha :as spec]
            [proletariat.core :as core])
  (:import [io.netty.handler.ssl SslContextBuilder SslProvider]
           [java.io FileInputStream]
           [java.security KeyStore]
           [javax.net.ssl TrustManagerFactory]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Private Implementation Fn's
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:private +timeouts+
  "The default timeouts for the http call"
  {:connection-timeout 60000
   :request-timeout    60000})

(defn- ->char-array
  [x]
  (cond (core/chars? x) x
        (string? x)     (.toCharArray x)
        (bytes? x)      (.toCharArray (String. ^bytes x))
        :default        (.toCharArray "")))

(defn- trust-manager-factory
  [^KeyStore trust-store]
  (doto
    (TrustManagerFactory/getInstance
      (TrustManagerFactory/getDefaultAlgorithm))
    (.init trust-store)))

(defn- ssl-context
  "Takes a path string to a TrustStore and a string password and returns
  a Netty SSLContext that trusts the CA's in the JKS."
  [trust-store trust-store-pass]
  (let [ts-file (io/file trust-store)
        ts-pw   (->char-array trust-store-pass)
        ts      (KeyStore/getInstance (KeyStore/getDefaultType))]
    (with-open [fis (FileInputStream. ts-file)]
      (.load ts fis ts-pw))
    (-> (SslContextBuilder/forClient)
        (.sslProvider SslProvider/JDK)
        (.trustManager ^TrustManagerFactory (trust-manager-factory ts))
        .build)))

(defn- parse-pool-cfg
  "Formats the pool-cfg configuration map"
  [{{:keys [trust-store trust-store-pass]} :connection-options :as opts}]
  (if (or trust-store trust-store-pass)
    (assoc-in opts
              [:connection-options :ssl-context]
              (ssl-context trust-store trust-store-pass))
    opts))

(defn- conn-pool
  "Creates a custom connection pool and returns the option map with the pool."
  [opts]
  (if (empty? opts)
    {:pool aleph-http/default-connection-pool}
    {:pool (aleph-http/connection-pool (parse-pool-cfg opts))}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; HTTP Client Configuration Spec
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(spec/def ::pool-timeout
  (spec/nilable integer?))

(spec/def ::connection-timeout
  (spec/nilable integer?))

(spec/def ::request-timeout
  (spec/nilable integer?))

(spec/def ::timeouts
  (spec/keys :opt-un [::pool-timeout
                      ::connection-timeout
                      ::request-timeout]))

(spec/def ::connections-per-host
  (spec/nilable integer?))

(spec/def ::total-connections
  (spec/nilable integer?))

(spec/def ::target-utilization
  (spec/nilable (spec/and number? #(< 0 % 1))))

(spec/def ::control-period
  (spec/nilable integer?))

(spec/def ::max-queue-size
  (spec/nilable integer?))

(spec/def ::pool-options
  (spec/keys :opt-un [::connections-per-host
                      ::total-connections
                      ::target-utilization
                      ::control-period
                      ::max-queue-size]))

(spec/def ::response-buffer-size
  (spec/nilable integer?))

(spec/def ::keep-alive?
  boolean?)

(spec/def ::max-header-size
  (spec/nilable integer?))

(spec/def ::max-chunk-size
  (spec/nilable integer?))

(spec/def ::insecure?
  boolean?)

(spec/def ::trust-store
  (spec/nilable string?))

(spec/def ::trust-store-password
  (spec/nilable
    (spec/or :bytes bytes?
             :chars core/chars?)))

(spec/def ::connection-options
  (spec/keys :opt-un [::response-buffer-size
                      ::keep-alive?
                      ::max-header-size
                      ::max-chunk-size
                      ::insecure?
                      ::trust-store
                      ::trust-store-pass]))

(spec/def ::config
  (spec/keys :opt-un [::timeouts
                      ::pool-options
                      ::connection-options]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public API
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn invoke!
  "Using the previously created client, invokes an HTTP request using the
  provided method on the provided URL. Can optionally specify a map of
  configurations including `:headers`, `:query-params`, and `body`. Returns
  a manifold deferred wrapping a Ring response map. The `body` in the response
  map will be an input stream."
  ([client method url]
    (invoke! client method url {}))
  ([client method url opts]
    (let [opts (merge client opts)]
      (case method
        :get     (aleph-http/get url opts)
        :head    (aleph-http/head url opts)
        :put     (aleph-http/put url opts)
        :post    (aleph-http/post url opts)
        :delete  (aleph-http/delete url opts)
        :patch   (aleph-http/patch url opts)
        :options (aleph-http/options url opts)))))

(defn invoke-sync!
  "Provides the same functionality as `invoke!`, except will make the call
  synchronously and return the response map instead of a manifold deferred.
  Will timeout after `request-timeout` milliseconds."
  ([client method url]
    (invoke-sync! client method url {}))
  ([client method url opts]
    (if-let [resp (deref (invoke! client method url opts)
                         (:request-timeout client)
                         nil)]
      resp
      (throw (Exception. "Connection timed out.")))))

(defn create
  "Creates an Aleph HTTP Client from the provided configurations map.

   Below is an example annotated configuration with defaults. All settings are
   optional. For the default client, simply call the no-arg arity.

   {;; Map with timeout configurations
    :timeouts {;; timeout in milliseconds for the pool to generate a
               ;; connection
               :pool-timeout         nil

               ;; timeout in milliseconds for the connection to become
               ;; established
               :connection-timeout   60000

               ;; timeout in milliseconds for the arrival of a response
               ;; over the established connection
               :request-timeout      60000}

    ;; Map with Connection Pool configurations
    :pool-options {;; the maximum number of simultaneous connections to any
                   ;; host
                   :connections-per-host 8

                   ;; the maximum number of connections across all hosts
                   :total-connections    1024

                   ;; the target utilization of connections per host,
                   ;; within `[0,1]`
                   :target-utilization   0.9

                   ;; the interval, in milliseconds, between use of the
                   ;; controller to adjust the size of the pool
                   :control-period       60000

                   ;; the maximum number of pending acquires from the pool
                   ;; that are allowed before `acquire` will start to throw
                   ;; a `java.util.concurrent.RejectedExecutionException`
                   :max-queue-size       65536}

    ;; Map with default options across all connections
    :connection-options  {;; the amount of the response, in bytes, that is
                          ;; buffered before the request returns. This does
                          ;; *not* represent the maximum size response that the
                          ;; client can handle (which is unbounded), and is
                          ;; only a means of maximizing performance.
                          :response-buffer-size 65536

                          ;; if `true`, attempts to reuse connections for
                          ;; multiple requests
                          :keep-alive?          true

                          ;; the maximum characters that can be in a single
                          ;; header entry of a response
                          :max-header-size      8192

                          ;; the maximum characters that can be in a single
                          ;; chunk of a streamed response
                          :max-chunk-size       8192

                          ;; If true, will ignore certificates and establish
                          ;; connection. Should only be used for development
                          :insecure?       false

                          ;; Path to the JKS Truststore to use for validating
                          ;; server certificates
                          :trust-store     \"/path/to/truststore.jks\"

                          ;; Truststore password as UTF-8 byte array, char
                          ;; array, or String. It is recommended to not use a
                          ;; plain String except in development.
                          :trust-store-pass [B}}
    "
  ([]
    (create {}))
  ([config]
   (let [pool-config (merge {}
                            (:pool-options config)
                            (select-keys config [:connection-options]))]
     (merge (conn-pool pool-config)
            +timeouts+
            (:timeouts config)))))

(spec/fdef create
  :args (spec/cat :config (spec/? ::config))
  :ret  map?)
