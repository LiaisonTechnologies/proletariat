(ns proletariat.crypto
  "Opinionated cryptographic functions library"
  (:require [clojure.spec.alpha :as spec])
  (:import [java.nio.charset Charset]
           [javax.crypto KeyGenerator Cipher]
           [javax.crypto.spec SecretKeySpec]
           [org.apache.commons.codec.binary Hex]))

(def +symmetric-key-algo+
  "Default Symmetric Key Algorithm"
  "AES")

(def +symmetric-key-length+
  "Default Symmetric Key Length"
  256)

(def #^Charset +default-charset+
  "Default Charset"
  (Charset/forName "UTF-8"))

(spec/fdef symmetric-key*
  :args (spec/cat :algo string?
                  :key-len int?)
  :ret bytes?)

(defn symmetric-key*
  "Generates a symmetric key with the provided
  [algorithm](http://bit.ly/2gEZiFl) and key length and uses the SecureRandom
  implementation of the highest-priority installed  provider as the source of
  randomness.  This can be configured in the `java.security` file in the JDK to
  specify a specific implementation.  Returns the key as a byte array."
  [algo key-len]
  (let [kg (KeyGenerator/getInstance algo)]
    (.init kg (int key-len))
    (-> kg
        .generateKey
        .getEncoded)))

(spec/fdef symmetric-key
  :args empty?
  :ret string?)

(defn symmetric-key
  "Generates a symmetric key with the SecureRandom implementation of the
  highest-priority installed provider as the source of randomness.  This can be
  configured in the `java.security` file in the JDK to specify a specific
  implementation.  Uses the `AES` algorithm with `256` keysize. Returns a
  hexadecimal encoded String representing the Key."
  []
  (-> (symmetric-key* +symmetric-key-algo+ +symmetric-key-length+)
      Hex/encodeHex
      String.))

(spec/fdef encrypt*
  :args (spec/cat :algo string?
                  :key bytes?
                  :data bytes?)
  :ret bytes?)

(defn encrypt*
  "Encrypts the input data bytes with the provided byte array key and algorithm
  and returns the encrypted bytes."
  [algo key data]
  (let [sks (SecretKeySpec. key algo)
        cipher (Cipher/getInstance algo)]
    (.init cipher Cipher/ENCRYPT_MODE sks)
    (.doFinal cipher data)))

(spec/fdef encrypt
  :args (spec/cat :key string?
                  :data bytes?)
  :ret string?)

(defn encrypt
  "Encrypts the input data byte array with the provided hexadecimal encoded
  string key and returns the hexadecimal encoded string result."
  [key data]
  (-> (encrypt* +symmetric-key-algo+
                (Hex/decodeHex (.toCharArray key))
                data)
      Hex/encodeHex
      String.))

(spec/fdef encrypt-str
  :args (spec/cat :key string?
                  :data string?)
  :ret string?)

(defn encrypt-str
  "Encrypts the input data string with the provided hexadecimal encoded string
  key and returns the hexadecimal encoded string result."
  [key data]
  (encrypt key (.getBytes data +default-charset+)))

(spec/fdef decrypt*
  :args (spec/cat :algo string?
                  :key bytes?
                  :data bytes?)
  :ret bytes?)

(defn decrypt*
  "Decrypts the encrypted bytes with the provided byte array key and algorithm
  and returns the decrypted bytes."
  [algo key encrypted]
  (let [sks (SecretKeySpec. key algo)
        cipher (Cipher/getInstance algo)]
    (.init cipher Cipher/DECRYPT_MODE sks)
    (.doFinal cipher encrypted)))

(spec/fdef decrypt
  :args (spec/cat :key string?
                  :data string?)
  :ret bytes?)

(defn decrypt
  "Decryptes the encrypted hexadecimal string with the provided hexadecimal
  encoded string key and returns the decrypted byte array."
  [key encrypted]
  (decrypt* +symmetric-key-algo+
            (Hex/decodeHex (.toCharArray key))
            (Hex/decodeHex (.toCharArray encrypted))))

(spec/fdef decrypt-str
  :args (spec/cat :key string?
                  :data string?)
  :ret string?)

(defn decrypt-str
  "Decryptes the encrypted hexadecimal string with the provided hexadecimal
  encoded string key and returns the decrypted data as a String."
  [key encrypted]
  (String.
    #^bytes (decrypt key encrypted)
    +default-charset+))
