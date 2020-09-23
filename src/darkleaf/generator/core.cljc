(ns darkleaf.generator.core
  (:refer-clojure :exclude [next])
  (:require
   [darkleaf.generator.proto :as p])
  #?(:cljs (:require-macros [darkleaf.generator.core :refer [generator]])))

(def ^{:arglists '([gen])} done? p/done?)
(def ^{:arglists '([gen])} value p/value)
(def ^{:arglists '([gen] [gen covalue])} next p/next)
(def ^{:arglists '([gen throwable])} throw p/throw)
(def ^{:arglists '([gen] [gen result])} return p/return)

(defn yield
  ([] nil)
  ([value] value))

(defmacro generator [& body])

(def wrap-stack)
