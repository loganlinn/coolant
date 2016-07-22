(ns coolant.core)

(defrecord Core [stores observers state evals])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO decide if worth keeping

(defprotocol Message
  (-message-type [this])
  (-message-data [this]))

(defn message
  ([k] (message k nil))
  ([k data]
   (reify Message
     (-message-type [_] k)
     (-message-data [_] data))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol Evaluator
  (-evaluate [this state]))

(defprotocol Store
  (-store-key [this])
  (-init-state [this])
  (-handle [this state message]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Core Operations

(defn- make-observer
  [evaluator f]
  {:pre [(satisfies? Evaluator evaluator)]}
  {:evaluator evaluator :notify! f})

(defn- add-store
  "Adds store to core, returning updated core"
  [core store]
  {:pre [(satisfies? Store store)
         (not (contains? (:stores core) (-store-key store)))]}
  (let [k (-store-key store)]
    (-> core
        (assoc-in [:stores k] store)
        (assoc-in [:state k] (-init-state store)))))

(defn- add-observer
  "Adds observer to core, returning updated core"
  [core observer]
  (update core :observers conj observer))

(defn- remove-observer
  "Removes observer from core, returning updated core"
  [core observer]
  (update core :observers (partial remove #(identical? observer %))))

(defn- set-state!
  "Transitions core to next-state.
   Notifies observers if their watched value has changed."
  [core next-state]
  (let [{:keys [observers state evals]} @core]
    (swap! core assoc :state next-state
           :evals
           (reduce
            (fn [next-evals {:keys [evaluator notify!]}]
              (let [prev (if (contains? evals evaluator)
                           (get evals evaluator)
                           (-evaluate evaluator state))
                    curr (if (contains? next-evals evaluator)
                           (get next-evals evaluator)
                           (-evaluate evaluator next-state))]
                (when-not (= prev curr)
                  ;; TODO setImmediate?
                  (notify! curr))
                (assoc next-evals evaluator curr)))
            {} observers))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Public

(defn core
  "Creates a new core. Optionally takes collection of stores to register."
  ([] (core nil))
  ([stores]
   (atom (reduce add-store (Core. {} [] nil {}) stores))))

(defn store
  "Returns a Store instance to be registered at key, k in core, with initial state, init.
   handlers is a map from message-type to function taking current state and
   message-data.
   The returned Store is evaluatable against the core to get the current state.
   This means you can use the Store as a dependency to getters."
  [k init handlers]
  (reify
    Store
    (-store-key [_] k)
    (-init-state [_] init)
    (-handle [_ state message]
      (if-let [handler (get handlers (-message-type message))]
        (handler state (-message-data message))
        state))
    Evaluator
    (-evaluate [_ state]
      (get state k))
    Object
    (toString [_]
      (str "Store " (str k)))))

#?(:clj (def ^:private lookup-sentinel
          "Clojure equivalent of the ClojureScript lookup sentinel."
          (gensym "lookup-sentinel")))

(defn getter
  "Returns a evaluatable object that produces a value given a non-empty sequence
   of evaluatable dependencies (ie, stores or other getters) and a function
   that's invoked whenever one or more dependencies change.
   The function must be pure."
  [deps f]
  {:pre [(seq deps) (every? #(satisfies? Evaluator %) deps) (fn? f)]}
  (let [mem (atom {})]
    (reify
      Evaluator
      (-evaluate [_ state]
        (let [args (map #(-evaluate % state) deps)
              v (get @mem args lookup-sentinel)]
          (if (identical? v lookup-sentinel)
            (let [v (apply f args)]
             (reset! mem {args v}) ;; cache last value. TODO: protocol for diff strategies?
             v)
            v)))
      Object
      (toString [this]
        (str "Getter deps: " (pr-str deps))))))


(defn dispatch!
  "Dispatch a message to the registered stores to update state.
   Notifies observers if their watched value has changed.
   Returns updated core."
  [core message]
  (let [{:keys [state stores]} @core
        next-state (reduce-kv
                    (fn [next-state k store]
                      (let [v (get state k)
                            next-v (-handle store v message)]
                        (if-not (identical? v next-v)
                          (assoc next-state k next-v)
                          next-state)))
                    state stores)]
    (set-state! core next-state)
    core))

(defn observe!
  "Registers an observer of evaluated value.
   When value changes, f will be will be invoked with new value.
   Returns function to unregister observer."
  [core evaluator f]
  (let [observer (make-observer evaluator f)]
    (swap! core add-observer observer)
    (fn [] (swap! core remove-observer observer))))

(defn register-stores!
  "Adds stores to core. Returns updated core."
  ([core stores]
   (register-stores! core stores false))
  ([core stores silent?]
   (swap! core (partial reduce add-store) stores)
   (when-not silent?
     (set-state! core (:state @core)))
   core))

(defn get-state
  "Returns current state of all stores in core"
  [core]
  (:state @core))

;; TODO rename to get? read?
(defn evaluate
  "Returns value produced by evaluator against current state of core."
  [core evaluator]
  (-evaluate evaluator (:state @core)))
