(ns darkleaf.generator.proto
 (:refer-clojure :exclude [next -next]))

;; The `throw` method does not work with advanced compilation in cljs.

(defprotocol Generator
  (-done? [this])
  (-value [this])
  (-next [this covalue])
  (-throw [this throwable])
  (-return [this result]))
