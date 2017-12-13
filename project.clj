(defproject com.liaison/proletariat "0.7.3"
  :description "Library of the Commons. A hard-working library of common utilities."
  :url "https://github.com/LiaisonTechnologies/proletariat"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :profiles {:dev     {:plugins [[lein-ancient "0.6.10"]]}
             :uberjar {:source-paths ["src"]}}

  :dependencies [[aero "1.1.2"]
                 [aleph "0.4.4"]
                 [aysylu/loom "1.0.0" :exclusions [tailrecursion/cljs-priority-map]]
                 ;; This is a fix for a [bug](https://github.com/ztellman/aleph/issues/326)
                 ;; with nuisance error messages. When Aleph is upgraded to this version
                 ;; the explict dependency can be removed
                 [byte-streams "0.2.4-alpha3"]
                 [commons-codec/commons-codec "1.10"]
                 [im.chit/hara.event "2.5.10"]
                 [org.clojure/clojure "1.9.0"]
                 [org.clojure/test.check "0.9.0"]
                 [org.clojure/tools.logging "0.4.0"]]

  :plugins [[lein-codox "0.10.3"]]
  :codox
  {:output-path "docs"
   :metadata    {:doc/format :markdown}
   :source-uri  "https://github.com/LiaisonTechnologies/proletariat/blob/{version}/{filepath}/#L{line}"})
