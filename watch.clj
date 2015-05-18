(require '[cljs.closure :as cljsc])
(require '[clojure.java.shell :as shell])
(require '[clojure.java.io :as io])

(defn source-dirs
  "Helper to compile mutiple source directories.
   Ideally, cljs.closure handled this for us."
  [& dirs]
  (let [paths (mapv io/file dirs)]
    (reify
      cljsc/Inputs
      (-paths [_] paths)
      cljsc/Compilable
      (-compile [_ opts]
        (mapcat #(cljsc/-compile % opts) paths)))))

(cljsc/watch
 (source-dirs "src" "test")
 {:main 'coolant.test-runner
  :output-to "out/test.js"
  :target :nodejs
  :compiler-stats true
  :watch-fn #(-> (shell/sh "node" "out/test.js") :out println)} ;; TODO move this out
 )
