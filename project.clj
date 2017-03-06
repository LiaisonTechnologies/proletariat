(defproject com.liaison/proletariat "0.5.0"
  :description "Library of the Commons. A hard-working library of common utilities."
  :url "https://github.com/LiaisonTechnologies/proletariat"

  :repositories ^:replace
  [["liaison-d2-public" {:url       "http://10.10.20.53:8081/nexus/repository/maven-public/"
                         :snapshots true
                         :update    :always}]

   ["MapR"              {:url       "http://repository.mapr.com/maven/"
                         :snapshots false
                         :update    :always}]]

  :profiles {:uberjar
             {:source-paths ["src"]}}

  :dependencies [[aysylu/loom "0.6.0"]
                 [com.taoensso/encore "2.68.1"]
                 [com.taoensso/timbre "4.7.0"]
                 [commons-codec/commons-codec "1.10"]
                 [im.chit/hara.event "2.3.7"]
                 [org.clojure/clojure "1.9.0-alpha14"]
                 [org.clojure/test.check "0.9.0"]])
