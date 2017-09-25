(defproject com.liaison/proletariat "0.7.1"
  :description "Library of the Commons. A hard-working library of common utilities."
  :url "https://github.com/LiaisonTechnologies/proletariat"

  :repositories ^:replace
  [["liaison-d2-public" {:url       "http://10.10.20.53:8081/nexus/repository/maven-public/"
                         :snapshots true
                         :update    :always}]

   ["MapR"              {:url       "http://repository.mapr.com/maven/"
                         :snapshots false
                         :update    :always}]]

  :profiles {:dev {:plugins [[lein-ancient "0.6.10"]]}
             :uberjar {:source-paths ["src"]}}

  :dependencies [[aero "1.1.1"]
                 [aleph "0.4.3"]
                 [aysylu/loom "1.0.0" :exclusions [tailrecursion/cljs-priority-map]]
                 ;; This is a fix for a [bug](https://github.com/ztellman/aleph/issues/326)
                 ;; with nuisance error messages. When Aleph is upgraded to this version
                 ;; the explict dependency can be removed
                 [byte-streams "0.2.4-alpha3"]
                 [commons-codec/commons-codec "1.10"]
                 [im.chit/hara.event "2.5.2"]
                 [org.clojure/clojure "1.9.0-beta1"]
                 [org.clojure/test.check "0.9.0"]
                 [org.clojure/tools.logging "0.4.0"]])
