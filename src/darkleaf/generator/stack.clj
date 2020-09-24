(ns darkleaf.generator.stack
  (:refer-clojure :exclude [pop! peek])
  (:import
   [java.util Deque ArrayDeque]))

(set! *warn-on-reflection* true)

(defn make []
  (ArrayDeque.))

(defn push! [^Deque stack value]
  (.push stack value))

(defn pop! [^Deque stack]
  (.pop stack))

(defn peek [^Deque stack]
  (.peek stack))
