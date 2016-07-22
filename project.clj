(defproject coolant "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript  "1.9.93" :scope "provided"]]
  :plugins [[lein-doo "0.1.6"]]
  :aliases
  {"ci" ["do"
         ["clean"]
         ["test" "coolant.core-test"]
         ["doo" "node" "node" "once"]
         ["doo" "phantom" "none" "once"]
         ["doo" "phantom" "advanced" "once"]]}
  :cljsbuild
  {:builds
   [{:id "none"
     :compiler
     {:main 'coolant.test.runner
      :optimizations :none
      :output-dir "target/none"
      :output-to "target/none.js"}
     :source-paths ["src" "test"]}
    {:id "node"
     :compiler
     {:main 'coolant.test.runner
      :optimizations :none
      :output-dir "target/node"
      :output-to "target/node.js"
      :target :nodejs}
     :source-paths ["src" "test"]}
    {:id "advanced"
     :compiler
     {:main 'coolant.test.runner
      :optimizations :advanced
      :output-dir "target/advanced"
      :output-to "target/advanced.js"}
     :source-paths ["src" "test"]}]})
