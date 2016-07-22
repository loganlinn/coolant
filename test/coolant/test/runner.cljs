(ns coolant.test.runner
  (:require [coolant.core-test]
            [doo.runner :refer-macros [doo-tests]]))

(doo-tests 'coolant.core-test)
