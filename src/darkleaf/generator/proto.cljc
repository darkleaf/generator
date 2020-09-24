(ns darkleaf.generator.proto
 (:refer-clojure :exclude [next]))

;; The `throw` method does not work with advanced compilation in cljs.

(defprotocol Generator
  (done? [this])
  (value [this])
  (next [this covalue])
  (raise [this throwable])
  (return [this result]))
