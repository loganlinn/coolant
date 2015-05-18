(ns coolant.test-runner
  (:require
   [cljs.test :as test]
   [cljs.nodejs :as nodejs]
   ;; require test namespaces to register test
   coolant.core-test
   ))

(nodejs/enable-util-print!)

(defn -main [& args]
  (test/run-tests
   'coolant.core-test))

(set! *main-cli-fn* -main)
