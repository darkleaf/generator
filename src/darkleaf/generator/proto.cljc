(ns darkleaf.generator.proto
 (:refer-clojure :exclude [next]))

(defprotocol Generator
  (done? [this])
  (value [this])
  (next [this] [this covalue])
  (throw [this throwable])
  (return [this] [this result]))
