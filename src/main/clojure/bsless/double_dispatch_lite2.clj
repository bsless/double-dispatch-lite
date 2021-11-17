(ns bsless.double-dispatch-lite2
  (:import
   (io.github.bsless.double_dispatch_lite DoubleDispatchFn)))

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

(defmacro defmulti*
  [name dispatch]
  `(def ~name (DoubleDispatchFn/create ~dispatch (fn [x#] (compile-mapping (deref x#))))))

(defmacro defmethod*
  [name dispatch-val & fn-tail]
  `(.add ~(with-meta name {:tag "io.github.bsless.double_dispatch_lite.IDoubleDispatch"}) ~dispatch-val (fn ~@fn-tail)))
