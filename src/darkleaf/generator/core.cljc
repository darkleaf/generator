(ns darkleaf.generator.core
  (:refer-clojure :exclude [next])
  (:require
   [darkleaf.generator.proto :as p]
   [darkleaf.generator.impl :as i])
  #?(:cljs (:require-macros [darkleaf.generator.core :refer [generator]])))

(def ^{:arglists '([gen])} done? p/done?)
(def ^{:arglists '([gen])} value p/value)
(def ^{:arglists '([gen throwable])} throw p/throw)

(defn next
  ([gen] (p/next gen nil))
  ([gen covalue] (p/next gen covalue)))

(defn return
  ([gen] (p/return gen nil))
  ([gen result] (p/return gen result)))

(defn yield
  ([] nil)
  ([value] value))

(defn- js? [env]
  (contains? env :ns))

(defmacro ^{:style/indent 0} generator [& body]
  (i/body->generator (js? &env) `yield body))

(def wrap-stack)
