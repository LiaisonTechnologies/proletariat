(defproject com.liaison/proletariat "0.7.1"
  :description "Library of the Commons. A hard-working library of common utilities."
  :url "https://github.com/LiaisonTechnologies/proletariat"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :profiles {:dev     {:plugins [[lein-ancient "0.6.10"]]}
             :uberjar {:source-paths ["src"]}}

  :dependencies [[aero "1.1.1"]
                 [aleph "0.4.3"]
                 [aysylu/loom "1.0.0" :exclusions [tailrecursion/cljs-priority-map]]
                 [commons-codec/commons-codec "1.10"]
                 [im.chit/hara.event "2.5.2"]
                 [org.clojure/clojure "1.9.0-alpha17"]
                 [org.clojure/test.check "0.9.0"]
                 [org.clojure/tools.logging "0.4.0"]]

  :plugins [[lein-codox "0.10.3"]]
  :codox
  {:output-path "docs"
   :metadata    {:doc/format :markdown}
   :source-uri  "https://github.com/LiaisonTechnologies/proletariat/blob/{version}/{filepath}/#L{line}"})
