(ns bsless.double-dispatch-lite-test
  (:require
   [clojure.test :as t]
   [bsless.double-dispatch-lite :as dd]))


(dd/defmulti* wizardry8 even?)

(dd/defmethod* wizardry8 true [_] "even!")
(dd/defmethod* wizardry8 false [_] "odd!")

(t/deftest dispatch
  (t/is (= "even!" (wizardry8 8)))
  (t/is (= "odd!" (wizardry8 7))))
