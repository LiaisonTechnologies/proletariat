(defproject com.liaison/proletariat "0.6.0"
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

  :dependencies [[aysylu/loom "1.0.0"]
                 [com.taoensso/encore "2.90.1"]
                 [com.taoensso/timbre "4.8.0"]
                 [commons-codec/commons-codec "1.10"]
                 [im.chit/hara.event "2.5.2"]
                 [org.clojure/clojure "1.9.0-alpha15"]
                 [org.clojure/test.check "0.9.0"]])
