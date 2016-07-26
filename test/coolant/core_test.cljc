(ns coolant.core-test
  (:require #?(:clj [clojure.test :refer :all]
               :cljs [cljs.test :refer-macros [deftest is testing]])
            [coolant.core :as coolant]))

(defn- arity2 [f] (fn [x _] (f x)))
(def empty-store (arity2 empty))

(deftest core-test
  (is (= @(coolant/core) @(coolant/core [])))

  (let [s1 (coolant/store :s1 {} {})]
    (is (= @(coolant/core [s1])
           @(coolant/register-stores! (coolant/core) [s1]))))

  (let [store1 (coolant/store :s1 {::init-value? true} {:conj conj :empty empty-store})
        store2 (coolant/store :s2 '() {:conj conj :reverse (arity2 reverse)})
        core (coolant/core [store1 store2])]
    (is (= {::init-value? true} (coolant/-init-state store1)))
    (is (= [] (coolant/-init-state store2)))
    (is (= {:s1 {::init-value? true} :s2 '()} (coolant/get-state core)))

    (coolant/dispatch! core (coolant/message :conj [:a 1]))
    (is (= {:a 1 ::init-value? true} (coolant/evaluate core store1)))
    (is (= (list [:a 1])) (coolant/evaluate core store2))

    (coolant/dispatch! core (coolant/message :conj [:b 2]))
    (is (= {:a 1 :b 2 ::init-value? true} (coolant/evaluate core store1)))
    (is (= (list [:b 2] [:a 1])) (coolant/evaluate core store2))

    (coolant/dispatch! core (coolant/message :reverse))
    (is (= {:a 1 :b 2 ::init-value? true} (coolant/evaluate core store1)))
    (is (= (list [:b 2] [:a 1])) (coolant/evaluate core store2))

    (coolant/dispatch! core (coolant/message :empty))
    (is (= {} (coolant/evaluate core store1)))
    (is (= (list [:b 2] [:a 1])) (coolant/evaluate core store2))

    (let [last-val1 (atom nil)
          last-val2 (atom nil)
          f1 (coolant/observe! core store1 (fn [s1] (reset! last-val1 s1)))
          f2 (coolant/observe! core store1 (fn [s1] (reset! last-val2 s1))) ]
      (coolant/dispatch! core (coolant/message :conj [1 2]))
      (is (= {1 2} @last-val1 @last-val2))
      (coolant/dispatch! core (coolant/message :conj [2 3]))
      (is (= {1 2 2 3} @last-val1 @last-val2))
      (f1)
      (coolant/dispatch! core (coolant/message :conj [3 4]))
      (is (= {1 2 2 3} @last-val1))
      (is (= {1 2 2 3 3 4} @last-val2))
      (f2)
      (coolant/dispatch! core (coolant/message :conj [4 5]))
      (is (= {1 2 2 3} @last-val1))
      (is (= {1 2 2 3 3 4} @last-val2))
      (is (= {1 2 2 3 3 4 4 5} (coolant/evaluate core store1)))

      (coolant/dispatch! core (coolant/message :empty))
      (is (= {1 2 2 3} @last-val1))
      (is (= {1 2 2 3 3 4} @last-val2))
      (is (= {} (coolant/evaluate core store1))))))

(deftest getter-test
  (let [s1 (coolant/store :s1 [] {:conj1 conj :empty empty-store})
        s2 (coolant/store :s2 [] {:conj2 conj :empty empty-store})
        g1 (coolant/getter [s1] (partial map inc))
        g2 (coolant/getter [s1 s2] (partial map +))
        core (coolant/core [s1 s2])]
    (is (satisfies? coolant/Evaluator g1))
    (is (satisfies? coolant/Evaluator g2))
    (is (= []
           (coolant/evaluate core s1)
           (coolant/evaluate core s2)
           (coolant/evaluate core g1)
           (coolant/evaluate core g2)))

    (is (= core (coolant/dispatch! core (coolant/message :conj1 50))))
    (is (= core (coolant/dispatch! core (coolant/message :conj2 30))))

    (is (= [50] (coolant/evaluate core s1)))
    (is (= [30] (coolant/evaluate core s2)))
    (is (= [51] (coolant/evaluate core g1)))
    (is (= [80] (coolant/evaluate core g2)))

    (coolant/dispatch! core (coolant/message :conj1 25))
    (is (= [51 26] (coolant/evaluate core g1)))
    (is (= [80] (coolant/evaluate core g2)))

    (coolant/dispatch! core (coolant/message :conj2 -25))
    (is (= [51 26] (coolant/evaluate core g1)))
    (is (= [80 0] (coolant/evaluate core g2)))

    (let [cnt (atom 0)
          unsub-g1 (coolant/observe! core g1 (fn [_] (swap! cnt inc)))]
      (coolant/dispatch! core (coolant/message :empty))
      (coolant/dispatch! core (coolant/message :conj1 1))
      (is (= 2 @cnt))
      (coolant/dispatch! core (coolant/message :conj2 2))
      (is (= 2 @cnt))
      (unsub-g1)
      (coolant/dispatch! core (coolant/message :conj1 1))
      (is (= [1 1] (coolant/evaluate core s1)))
      (is (= 2 @cnt)))))

;; TODO getter-memoization-test
