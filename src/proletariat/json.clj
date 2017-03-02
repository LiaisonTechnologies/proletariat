(ns proletariat.json
  "Provides utilities for working with JSON including an implementation of
  a clojure.zip zipper geared towards JSON for walking and manipulating the
  tree."
  (:require [clojure.zip :as zip]))

;;
;; JSON Zip
;;

;;;; Json Node Functions

(defn json-entry?
  "Tests whether `node` is an entry in a JSON object.  Just a wrapper
  for `map-entry?` for naming consistency"
  [node]
  (map-entry? node))

(defn json-array?
  "Tests whether `node` is an array-like data structure: ie: an unbounded,
  sequential list that allows duplicates."
  [node]
  (and (not (json-entry? node)) (sequential? node)))

(defn json-obj?
  "Tests whether `node` is a json object data structure.  Just a wrapper
  for `map?` for naming consistency"
  [node]
  (map? node))

(defn json-coll?
  "Tests whether `node` is a json collection type: a map or an `array`"
  [node]
  (or (json-obj? node) (json-array? node)))

(defn json-entry-coll?
  "Tests whether `node` is a map-entry and if so whether its value is
  a collection type."
  [node]
  (and (json-entry? node) (json-coll? (second node))))

(defn json-entry-obj?
  "Tests whether `node` is a map-entry and if so whether its value is
  a map type"
  [node]
  (and (json-entry? node) (json-obj? (second node))))

(defn json-entry-array?
  "Tests whether `node` is a map-entry and if so whether its value is
  an array type"
  [node]
  (and (json-entry? node) (json-array? (second node))))

(defn json-branch?
  "Tests whether `node` would qualify as a branch node in the context
  of a `json-zipper`"
  [node]
  (or (json-obj? node) (json-array? node) (json-entry-coll? node)))

(defn json-leaf?
  "Tests whether `node` would qualify as a leaf node in the context
  of a `json-zipper`"
  [node]
  (not (json-branch? node)))

(defn json-root-node?
  "Tests whether `node` is the JSON root node in the context of
  a `json-zipper`"
  [node]
  (boolean (get (meta node) :json-root)))

(defn json-entry-leaf?
  "Tests whether `node` is a map-entry and if so whether it's a
  leaf node"
  [node]
  (and (json-entry? node) (json-leaf? node)))

(defn json-array-element?
  "Tests whether `node` is a JSON array element which in itself
  could be an object, array, or scalar"
  [node]
  (and (not (json-root-node? node))
       (not (json-entry? node))))

(defn json-array-element-leaf?
  "Tests whether `node` is a JSON array element and if so
  whether it is also a leaf node"
  [node]
  (and (json-array-element? node) (json-leaf? node)))

(defn json-array-element-coll?
  "Tests whether `node` is a JSON array element and if so
  whether it is a collection type"
  [node]
  (and (json-coll? node) (json-array-element? node)))

;;;; Json Location Functions

(defn root?
  "Returns true if the loc is at the root of the tree, otherwise false"
  [loc]
  (when loc
    (boolean (not (zip/up loc)))))

(defn json-node
  "Works like `clojure.zip/node` except provides nil punning."
  [loc]
  (try
    (zip/node loc)
    (catch NullPointerException _)))

(defn walk-seq
  "Returns a Lazy Sequence of all locations returned from a
  depth-first walk of a zipper location"
  [loc]
  (take-while
    (complement zip/end?)
    (iterate zip/next loc)))

;;;; Json Zipper

(defn- json-zip*
  [m]
  (zip/zipper
    json-branch?
    (fn [x] (seq (if (json-entry? x) (second x) x)))
    (fn [x children]
      (let [ret (cond
                  (json-obj? x) (into {} children)
                  (json-array? x) (vec children)
                  (json-entry-obj? x) (assoc x 1 (into {} children))
                  :default (assoc x 1 (vec children)))]
        (if (json-root-node? x)
          (with-meta ret {:json-root true})
          ret)))
    m))

(defn json-zip
  "Returns a JSON zipper for a clojure data structure generated from
  JSON data (as from json/parse)."
  [m]
  (let [zipper (json-zip* m)]
    (with-meta
      [(with-meta (first zipper) {:json-root true}) nil]
      (meta zipper))))
