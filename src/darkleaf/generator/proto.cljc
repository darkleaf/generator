(ns darkleaf.generator.proto
 (:refer-clojure :exclude [next -next]))

(defprotocol Generator
  (-done? [this])
  (-value [this])
  (-next [this covalue])
  (-throw [this throwable])
  (-return [this result]))
