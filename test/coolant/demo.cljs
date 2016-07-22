(ns coolant.demo
  (:require [coolant.core :as coolant]
            [clojure.browser.repl :as repl]))

(enable-console-print!)

#_
(defonce conn (repl/connect "http://localhost:9000/repl"))

(defn- spy [& xs]
  (apply println xs)
  (last xs))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def tax-percent
  (coolant/store
   :tax-percent
   0.10
   {:set-tax-percent (fn [_ n]
                       (println "[store: tax-percent]" :set-tax-percent n)
                       n)}))

(def items
  (coolant/store
   :items
   [{:name "Soap" :price 5 :quantity 2}
    {:name "The Adventures of Pluto Nash DVD" :price 10 :quantity 1}
    {:name "Fig Bar" :price 3 :quantity 10}]
   {:add-item (fn [xs x]
                (spy "[store: items]" :add-item
                     (conj xs x)))
    :clear-items (fn [xs _]
                   (spy "[store: items]" :clear-items
                        (empty xs)))}))

(def core (coolant/core [tax-percent items]))

(def subtotal
  (coolant/getter
   [items]
   (fn [items]
     (spy "[getter: subtotal]"
          (reduce + (map #(* (:price %) (:quantity %)) items))))))

(println "Curent subtotal:" (coolant/evaluate core subtotal))

(def total
  (coolant/getter
   [subtotal tax-percent]
   (fn [subtotal tax-percent]
     (spy "[getter: total]"
          (* subtotal (+ 1 tax-percent))))))

(println "Total:" (coolant/evaluate core total))

(def unobserve-total!
  (coolant/observe!
   core total
   (fn [total] (println "[observer: total]" total))))

(coolant/dispatch! core (coolant/message :set-tax-percent 0.04))

(println "Adding sf-total observer")

(def sf-total
  (coolant/getter
   [subtotal]
   (fn [subtotal]
     (spy "[getter: sf-total]"
          (* subtotal 1.87)))))

(def unobserve-sf-total!
  (coolant/observe!
   core sf-total
   (fn [total] (println "[observer: sf-total]" total))))

(println "Changing tax percent...")

(coolant/dispatch! core (coolant/message :set-tax-percent 0.055))

(println "Observing total again")

(def unobserve-total2!
  (coolant/observe!
   core total
   (fn [total] (println "[observer: total #2]" total))))

(coolant/dispatch! core (coolant/message :set-tax-percent 0.05))

(coolant/dispatch!
 core
 (coolant/message :add-item {:name "Pen" :price 3 :quantity 1}))

(println "Unobserving total #2...")

(unobserve-total2!)

(coolant/dispatch! core (coolant/message :clear-items))

(println "Unobserving sf-total...")

(unobserve-sf-total!)

(coolant/dispatch!
 core
 (coolant/message :add-item {:name "Pabst Blue Ribbon" :price 1 :quantity 12}))

(println "Unobserving total...")

(unobserve-total!)

(coolant/dispatch! core (coolant/message :set-tax-percent 0.00))

(println "Done.")

(println "Current total:" (coolant/evaluate core total))
