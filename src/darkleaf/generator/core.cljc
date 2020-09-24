(ns darkleaf.generator.core
  (:refer-clojure :exclude [next])
  (:require
   [darkleaf.generator.proto :as p]
   [darkleaf.generator.impl :as i])
  #?(:cljs (:require-macros [darkleaf.generator.core :refer [generator]])))

(set! *warn-on-reflection* true)

(def ^{:arglists '([gen])} done? p/done?)
(def ^{:arglists '([gen])} value p/value)
(def ^{:arglists '([gen] [gen covalue])} next p/next)
(def ^{:arglists '([gen throwable])} throw p/throw)
(def ^{:arglists '([gen] [gen result])} return p/return)

(defn yield
  ([] nil)
  ([value] value))

(defmacro ^{:style/indent 0} generator [& body]
  (i/body->generator `yield body))

(def wrap-stack)
