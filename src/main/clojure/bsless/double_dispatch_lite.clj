(ns bsless.double-dispatch-lite
  (:import
   (clojure.lang IFn)))

(defprotocol IDoubleDispatch
  (-add [this k f])
  (-remove [this k]))

(defmulti ->= type)
(defmethod ->= String [^String x] #(.equals x %))
(defmethod ->= clojure.lang.Keyword [^clojure.lang.Keyword x] #(.equals x %))
(defmethod ->= clojure.lang.Symbol [^clojure.lang.Symbol x] #(.equals x %))
(defmethod ->= Long [^long x]
  #(if (int? %) (= x (unchecked-long %)) false))

(defmethod ->= Integer [x]
  (let [x (long x)]
    #(if (int? %) (= x (unchecked-long %)) false)))

(defmethod ->= Boolean [x] (if (true? x) #(.equals Boolean/TRUE %) #(.equals Boolean/FALSE %)))
(defmethod ->= :default [x] #(= x %))


(defn on-failure
  [& args]
  (throw (ex-info "No dispatch found" {:args args})))

(defn compile-mapping
  [mapping]
  (let [default (or (:default mapping) on-failure)]
    (reduce
     (fn [f* [k f]]
       (let [=* (->= k)]
         (fn
           ([x a] (if (=* x) (f a) (f* x a)))
           ([x a b] (if (=* x) (f a b) (f* x a b)))
           ([x a b c] (if (=* x) (f a b c) (f* x a b c)))
           ([x a b c d] (if (=* x) (f a b c d) (f* x a b c d)))
           )))
     default
     (dissoc mapping :default))))

(deftype DoubleDispatchFn [^IFn dispatch-fn mapping ^:unsynchronized-mutable ^IFn f*]
  IFn
  (invoke [_ a] (.invoke f* (.invoke dispatch-fn a) a))
  (invoke [_ a b] (.invoke f* (.invoke dispatch-fn a b) a b))
  (invoke [_ a b c] (.invoke f* (.invoke dispatch-fn a b c) a b c))
  (invoke [_ a b c d] (.invoke f* (.invoke dispatch-fn a b c d) a b c d))

  IDoubleDispatch
  (-add [_ k f] (set! f* (compile-mapping (swap! mapping assoc k f))))
  (-remove [_ k] (set! f* (compile-mapping (swap! mapping dissoc k)))))

(defmacro defmulti*
  [name dispatch]
  `(def ~name (DoubleDispatchFn. ~dispatch (atom {}) (compile-mapping {}))))

(defmacro defmethod*
  [name dispatch-val & fn-tail]
  `(-add ~name ~dispatch-val (fn ~@fn-tail)))
