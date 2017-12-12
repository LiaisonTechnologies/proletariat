(ns proletariat.core
  "Library of the Commons.  A hard-working library of common utilities."
  (:require [clojure.instant :as instant]
            [clojure.edn :as edn]
            [clojure.spec.alpha :as spec]
            [clojure.string :as string]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.tools.logging :as log]
            [clojure.zip :as zip]
            [hara.event :as event]
            [clojure.test.check.rose-tree :as rose]
            [clojure.test.check.generators :as cgen])
  (:import [java.util UUID]
           [java.time Instant]
           [java.net InetAddress]
           [clojure.lang IFn]))

(spec/fdef chars?
  :args (spec/cat :x any?)
  :ret boolean?)

(defn chars?
  "Returns true if `x` is an array of char's"
  [x]
  (instance? (Class/forName "[C") x))

(spec/fdef chars?
  :args (spec/cat :x any?)
  :ret boolean?)

(defn characters?
  "Returns true if `x` is an array of Character's"
  [x]
  (instance? (Class/forName "[Ljava.lang.Character;") x))

(spec/fdef leap-year?
  :args (spec/cat :year pos-int?)
  :ret  boolean?)

(defn leap-year?
  "Is the given year a leap year"
  [year]
  (or (= 0 (mod year 400))
      (and (not= 0 (mod year 100)) (= 0 (mod year 4)))))

(def instant-generator
  "No-arg function that returns a generator for java.time.Instant"
  (fn []
    (cgen/->Generator
      (fn [_ _]
        (rose/pure
          (let [month-days {2 28
                            4 30
                            6 30
                            9 30
                            11 30}
                y (+ 1970 (rand-int 100))
                M (+ 1 (rand-int 12))
                max-d (if (and (= M 2) (leap-year? y))
                        29 ;; leap year
                        (or (get month-days M) 31))
                d (+ 1 (rand-int max-d))
                h (rand-int 24)
                m (rand-int 60)
                s (rand-int 60)
                f (rand-int 1000000)
                text (format "%04d-%02d-%02dT%02d:%02d:%02d.%06dZ"
                             y M d h m s f)]
            (java.time.Instant/parse text)))))))

(spec/fdef to-clj-instant
  :args
  (spec/cat :jtime-instant #(instance? java.time.Instant %))
  :ret
  inst?)

(defn to-clj-instant
  "Creates a clojure instant literal a given java.time.Instant"
  [^Instant jtime-instant]
  (instant/read-instant-date (str jtime-instant)))

(spec/fdef now
  :args empty?
  :ret #(instance? Instant %))

(defn now
  "Creates a java.time.Instant for the current time"
  []
  (Instant/now))

(spec/fdef named?
  :args (spec/cat :x any?)
  :ret boolean?)

(defn named?
  "Returns true if `x` is a namespaced keyword (an instance of
  `clojure.lang.Named`."
  [x]
  (instance? clojure.lang.Named x))

(spec/fdef uuid
  :args empty?
  :ret uuid?)

(defn uuid
  "Generate a random UUID suitable for use as a GUID"
  []
  (UUID/randomUUID))

(spec/fdef remove-nil-values
  :args (spec/cat :m map?)
  :ret map?
  :fn (fn [spec]
        (map (fn [[k v]] (and (some? k) (some? v)))
             (:ret spec))))

(defn remove-nil-values
  "Removes elements from a map that have nil values; does not recurse"
  [m]
  (into {} (remove (comp nil? second) m)))

(spec/fdef remove-nils
  :args (spec/cat :m map?)
  :ret (spec/nilable map?))

(defn remove-nils
  "Removes elements from a (possibly nested) map that have nil
  values. Returns `nil` if all values in the map are `nil`
  Src: http://stackoverflow.com/a/29363255"
  [nm]
  (clojure.walk/postwalk
   (fn [el]
     (if (map? el)
       (let [m (into {} (remove (comp nil? second) el))]
         (when (seq m)
           m))
       el))
   nm))

(spec/fdef get-host-address
  :args empty?
  :ret string?)

(defn get-host-address
  "Return the local host IP address"
  []
  (.getHostAddress (InetAddress/getLocalHost)))

(spec/fdef whoami
  :args (spec/cat :default (spec/* string?))
  :ret string?)

(defn whoami
  "Runs `whoami` on the OS and returns the user that owns the JVM process.
  Only for Linux based systems.  Optional arity as default in case of failure."
  ([]
   (whoami ""))
  ([default]
   (or
     (try
       (let [nm (string/trim
                  (:out (shell/sh "whoami")))]
         (when-not (string/blank? nm)
           nm))
       (catch Exception _))
     default)))

(spec/fdef deep-merge
  :args
  (spec/cat :maps (spec/* any?))
  :ret
  (spec/nilable map?))

(defn deep-merge
  "Performs a deep merge on `maps`, resolves conflicts by choosing the last
  value. See `deep-merge-with` for custom conflict resolution."
  [& maps]
  (if (every? map? maps)
    (apply merge-with deep-merge maps)
    (last maps)))


(spec/fdef deep-merge-with
  :args
  (spec/cat :func (spec/fspec :args (spec/cat :m1 any? :m2 any?) :ret any?)
            :maps (spec/cat :maps (spec/* any?)))
  :ret
  (spec/nilable map?))

(defn deep-merge-with
  "Performs a deep merge on `maps` using `func` to resolve conflicts."
  [func & maps]
  (let [par-func (partial deep-merge-with func)]
    (if (every? map? maps)
      (apply merge-with par-func maps)
      (apply func maps))))

(spec/fdef flatten-map
  :args (spec/cat :m any?)
  :ret (spec/nilable map?))

(defn flatten-map
  "Takes a map, possibly nested, and 'flattens' it by removing any nesting and
  placing all keys in the root map. Any keys that refer to nested maps will be
  removed. Duplicate keys will be overwritten based on the order of reduce,
  where later keys will overwrite earlier keys."
  [m]
  (reduce-kv
    (fn [acc k v]
      (if (map? v)
        (merge acc (flatten-map v))
        (assoc acc k v)))
    {}
    m))

(defmacro do-until
  "Executes the body until `test` returns true."
  [test & body]
  `(loop []
     ~@body
     (when (not ~test)
       (recur))))

(defmacro do-while
  "Executes the while `test` returns true."
  [test & body]
  `(loop []
     ~@body
     (when ~test
       (recur))))

(defn slurp-resource
  "Works like `clojure.core/slurp` except it resolves the resource file
  descriptor `fd` relative to the classpath instead of the file system."
  [fd & opts]
  (apply slurp
         (io/resource fd)
         opts))

(defn read-edn-resource
  "Works like `clojure.edn/read-string` except reads the EDN from a classpath
  resource with the given file descriptor `fd` instead of a passed in string."
  ([fd]
   (-> fd slurp-resource edn/read-string))
  ([opts fd]
   (edn/read-string opts (slurp-resource fd))))

(defn delete-file!
  "Deletes a file at the path."
  [file-path]
  (if (.exists (clojure.java.io/file file-path))
    (try
      (clojure.java.io/delete-file file-path)
      (catch Exception e (log/error "delete file exception: " (.getMessage e))))
    false))

(defn delete-directory!
  "Deletes a directory"
  [directory-path]
  (let [directory-contents (file-seq (clojure.java.io/file directory-path))
        files-to-delete (filter #(.isFile %) directory-contents)]
    (doseq [file files-to-delete]
      (delete-file! (.getPath file)))
    (delete-file! directory-path)))

;;; TODO: move the below to `proletariat.retry`

(defn thread-name
  "Convenience for returning the current thread name"
  []
  (.getName (Thread/currentThread)))

(defmacro interruptable
  "Wrapper for catching InterruptedException and running the provided function.
  The function will receive the exception as input and the response will be
  returned to the caller."
  [f & body]
  `(try
     ~@body
     (catch InterruptedException e#
       (~f e#))))

(defmacro interruptable->ex
  "Wrapper for catching InterruptedException and returning an ExceptionInfo with
  provided map `m` as data."
  [m & body]
  `(interruptable
     (fn [e#] (throw (ex-info (.getMessage e#) ~m e#)))
     ~@body))

(defn retry*
  "By default, will execute `thunk` and return the result.  If an exception is
  thrown, will retry `n` times (making total executions `n + 1` times).  If all
  retries are exhausted, the final exception is thrown.  Optional arities are
  provided for customization.

  Args

    n        - the number of times to retry.  If < 0, will retry indefinitely
    thunk    - no-arg function to execute and catch any exceptions for retry
    on-retry - (optional arity) function that is called prior to the retry when
               an exception is thrown, but there are still retries available.
               Default is a no-op.  The function takes 2 arguments: first is
               the number of times retried (first retry will be 0, second will
               be 1, etc) and the second is the exception that was thrown.
               It is presumed that this funciton is for side effects (eg:
               logging) and any result will be ignored.
    on-fail  - (optional arity) function that is called after all retries have
               been exhausted.  The result of this function will be returned as
               the final result of `retry*`.  The default is to throw the final
               exception.  The function takes 1 argument: the final exception.

  Note: if an exception is thrown from either `on-retry` or `on-fail`
  processing will stop immediately and that exception thrown, with no further
  retries.
  "
  ([n thunk]
   (retry* n thunk (fn [_ _])))
  ([n thunk on-retry]
   (retry* n thunk on-retry #(throw %)))
  ([n thunk on-retry on-fail]
   {:pre [(and on-retry on-fail)]}
   (loop [c n
          t 0]
     (if-let [result (try
                       [(thunk)]
                       (catch Exception e
                         (if (zero? c)
                           (or (on-fail e) [nil])
                           (do (on-retry t e) nil))))]
       (result 0)
       (recur (dec c) (inc t))))))

(defmacro retry
  "Will execute `body` and return the result.  If an exception is thrown, will
  retry `n` times (making total executions `n + 1` times).  If all retries are
  exhausted, the final exception is thrown.
  "
  {:style/indent 1}
  [n & body]
  `(retry* ~n (fn [] ~@body)))

(defmacro retry-backoff
  "Will execute `body` and return the result.  If an exception is thrown, will
  sleep for `previous retries * b` milliseconds and then retry up to `n` times
  (making total executions `n + 1` times).  The first retry will not sleep, the
  second will sleep for `b` milliseconds and subsequent retries will sleep for
  retries * `b` milliseconds.  If all retries are exhausted, the final
  exception is thrown.  Can throw InterruptedExcpetion if the processing thread
  is interupted during a backoff sleep.
  "
  {:style/indent 2}
  [n b & body]
  `(retry* ~n (fn [] ~@body) (fn [r# _#] (Thread/sleep (* ~b r#)))))

(defn keep-sc
  "Works like `clojure.core/keep` except will short circuit if an exception is
  thrown by the processing function and return the list of results processed
  prior to the exception.  Optional arity allows the user to pass in a
  `(atom nil)` which will be `reset!` with the exception that was thrown.
  Note: since `map-sc` is lazy, the atom will not contain the exception until
  the sequence is realized.

  Has same performance enhancement for chunked-seq's as `clojure.core/keep` but
  will guarantee that no elements after the exception will be processed even if
  in the middle of a chunk when the exception is thrown.
  "
  ([f messages]
   (keep-sc f messages (atom nil)))
  ([f messages exp]
   (lazy-seq
     (when-let [s (seq messages)]
       (if (chunked-seq? s)
         (let [c (chunk-first s)
               size (count c)
               b (chunk-buffer size)]
           (loop [i 0]
             (when (< i size)
               (let [x (try (f (.nth c i))
                            (catch Exception e e))]
                 (if (instance? Exception x)
                   (reset! exp x)
                   (do
                     (when-not (nil? x)
                       (chunk-append b x))
                     (recur (inc i)))))))
           (if @exp
             (chunk-cons (chunk b) (keep-sc f '() exp))
             (chunk-cons (chunk b) (keep-sc f (chunk-rest s) exp))))
         (let [x (try (f (first s))
                      (catch Exception e e))]
           (cond
             (instance? Exception x)
             (do (reset! exp x) (keep-sc f '() exp))

             (nil? x)
             (keep-sc f (rest s) exp)

             :else
             (cons x (keep-sc f (rest s) exp)))))))))

(defn map-sc
  "Works like `clojure.core/map` except will short circuit if an exception is
  thrown by the processing function and return the list of results processed
  prior to the exception.  Optional arity allows the user to pass in a
  `(atom nil)` which will be `reset!` with the exception that was thrown.
  Note: since `map-sc` is lazy, the atom will not contain the exception until
  the sequence is realized.

  Has same performance enhancement for chunked-seq's as `clojure.core/map` but
  will guarantee that no elements after the exception will be processed even if
  in the middle of a chunk when the exception is thrown.
  "
  ([f messages]
   (map-sc f messages (atom nil)))
  ([f messages exp]
   (lazy-seq
     (when-let [s (seq messages)]
       (if (chunked-seq? s)
         (let [c (chunk-first s)
               size (count c)
               b (chunk-buffer size)]
           (loop [i 0]
             (when (< i size)
               (let [x (try (f (.nth c i))
                            (catch Exception e e))]
                 (if (instance? Exception x)
                   (reset! exp x)
                   (do
                     (chunk-append b x)
                     (recur (inc i)))))))
           (if @exp
             (chunk-cons (chunk b) (map-sc f '() exp))
             (chunk-cons (chunk b) (map-sc f (chunk-rest s) exp))))
         (let [x (try (f (first s))
                      (catch Exception e e))]
           (if (instance? Exception x)
             (do (reset! exp x) (map-sc f '() exp))
             (cons x (map-sc f (rest s) exp)))))))))

(defmacro dofor
  "Behaves like a combination of `clojure.core/for` and `clojure.core/doseq`.
  Intended for side-effects, but will return a realized seq of the results.
  Has the following behaviors:

    - Not-lazy, will cause the entire seq to reside in memory at one time
    - Supports the `for` modifers :let, :while, :when
    - wraps `body` in an implicit do (unlike `for`)
    - returns a seq of the results from each iteration (unlike `doseq`)

  Unlike `for` or `doseq`, provides support for Hara Events, by catching any
  exceptions and instead using `event/raise` with name `:exception` and map
  containing a single element `:e` with the exception that was thrown.  This
  allows users to leverage all of Hara Event's utilities for managing
  Exceptions via conditional restarts (eg: using `on`/`continue` to provide
  a default in the event of an exception)"
  {:style/indent [1]}
  [seq-exprs & body]
  `(doall
     (for
       ~seq-exprs
       (try
         (do
           ~@body)
         (catch Exception e#
           (event/raise [:exception {:e e#}]))))))

(defn group-by-with
  "Like clojure.core/group-by except adds an additional parameter allowing the
  user to specify the collection that will be used for collecting the grouped
  items instead of defaulting to vector."
  [f bucket coll]
  (let [bucket (or bucket [])]
    (persistent!
      (reduce
        (fn [ret x]
          (let [k (f x)]
            (assoc! ret k (conj (get ret k bucket) x))))
        (transient {}) coll))))

;;; TODO: move the below to `proletariat.zip`

(spec/fdef get-zipper-path
  :args
  (spec/cat :loc vector?)
  :ret
  string?)

(defn get-zipper-path
  "Returns the path of a location in a zipper. `loc` should *not* be a node."
  [loc]
  (->> loc
       (zip/path)
       (map first)
       (clojure.string/join "/")
       (str "/")))


(spec/fdef zipper-map
  :args
  (spec/cat
   :f (spec/fspec
       :args (spec/cat :loc vector?)
       :ret any?)
   :z vector?)
  :ret
  vector?)

(defn zipper-map [f z]
  "Map `f` over every node of the zipper."
  (loop [z z]
    (if (identical? (zip/next z) z)
      (zip/root z)
      (if (zip/branch? z)
        (recur (zip/next z))
        (recur (do (f z) (zip/next z)))))))

(defn flip
  "Returns a function with reversed arity of `f`"
  [f]
  (fn
    ([] (f))
    ([x] (f x))
    ([x y] (f y x))
    ([x y z] (f z y x))
    ([a b c d] (f d c b a))
    ([a b c d & rest]
     (->> rest
          (concat [a b c d])
          reverse
          (apply f)))))

(spec/fdef sreduce
  :args (spec/cat :f #(instance? IFn %)
                  :acc any?
                  :state any?
                  :coll coll?)
  :ret any?)

(defn sreduce
  "Works like reduce, but also allows functional state to be passed
  and returned between iterations.  `f` should be a function of three
  arguments: the accumulator, the next value, and the state.  `acc`
  is an accumulator that will be the final return value.  `state` is
  the starting state of the reduction.  `coll` is the collection to
  iterate."
  [f acc state coll]
  (if (empty? coll)
    acc
    (let [[new-acc new-state] (f acc (first coll) state)]
      (recur f new-acc new-state (rest coll)))))

(spec/fdef conjv
  :args (spec/cat :coll coll?
                  :x    any?)
  :ret vector?)

(defn conjv
  "Add element `x` to a vectorized version of `coll` and return the new vector"
  [coll x]
  (conj (vec coll) x))

(spec/fdef conj-when
  :args (spec/cat :coll coll?
                  :f    #(instance? IFn %)
                  :x    any?)
  :ret coll?)

(defn conj-when
  "Conjoins `x` to `coll` per the predicate `f`, otherwise returns `coll`
  unchanged."
  [coll f x]
  (if (f x)
    (conj coll x)
    coll))

(spec/fdef acat
  :args (spec/cat :seq seq?)
  :ret seq?)

(defn acat
  "Fully lazy equivalent of `(apply cat ...)`"
  [s]
  (lazy-cat (first s) (when-let [n (next s)] (acat n))))

(defn bytes->string
  "Convert a byte array to string."
  [#^bytes byte-array]
  (String. byte-array))
