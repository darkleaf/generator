(ns darkleaf.generator.impl
  (:require
   [darkleaf.generator.proto :as p]
   [darkleaf.generator.stack :as stack]
   [cloroutine.core :refer [cr]]))

(defn wrap-gen-reject-done [gen]
  (let [check! #(when (p/-done? gen)
                  (throw (ex-info "Generator is done" {:type :illegal-state})))]
    (reify
      p/Generator
      (-done? [_]
        (p/-done? gen))
      (-value [_]
        (p/-value gen))
      (-next [_ covalue]
        (check!)
        (p/-next gen covalue))
      (-throw [_ throwable]
        (check!)
        (p/-throw gen throwable))
      (-return [_ result]
        (check!)
        (p/-return gen result)))))

(def interrupted-exception
  #?(:clj  (Error. "Interrupted generator")
     :cljs ::interrupted-generator))

(declare ->Generator)

(defn body->generator [js? yield body]
  `(let [done?#      (atom false)
         value#      (atom nil)
         covalue-fn# (atom (fn [] nil))
         return#     (atom nil)
         resume#     (fn [] (@covalue-fn#))
         coroutine#  (cr {~yield resume#}
                         (try
                           ~@body
                           (catch ~(if js? :default 'Error) ex#
                             (if (identical? interrupted-exception ex#)
                               (do (reset! value# @return#)
                                   @return#)
                               (throw ex#)))
                           (finally
                             (reset! done?# true))))]
     (reset! value# (coroutine#))
     (-> (->Generator done?# value# covalue-fn# return# coroutine#)
         wrap-gen-reject-done)))

(deftype Generator [done? value covalue-fn return coroutine]
  p/Generator
  (-done? [_] @done?)
  (-value [_] @value)
  (-next [this covalue]
    (reset! covalue-fn (fn [] covalue))
    (reset! value (coroutine))
    nil)
  (-throw [this throwable]
    (reset! covalue-fn (fn [] (throw throwable)))
    (reset! value (coroutine))
    nil)
  (-return [this result]
    (reset! return result)
    (reset! covalue-fn (fn [] (throw interrupted-exception)))
    (reset! value (coroutine))
    nil))

(defn- one? [coll]
  (= 1 (count coll)))

(defn- join [stack]
  (let [gen   (stack/peek stack)
        value (p/-value gen)]
    (if (p/-done? gen)
      (cond
        (satisfies? p/Generator value)
        (do (stack/pop! stack)
            (stack/push! stack value)
            (recur stack))
        (one? stack)
        nil
        :else
        (do (stack/pop! stack)
            (p/-next (stack/peek stack) value)
            (recur stack)))
      (cond
        (satisfies? p/Generator value)
        (do (stack/push! stack value)
            (recur stack))
        :else
        nil))))

(defn wrap-stack [f*]
  (fn [& args]
    (let [gen   (apply f* args)
          stack (stack/make)]
      (stack/push! stack gen)
      (join stack)
      (reify
        p/Generator
        (-done? [_]
          (-> stack (stack/peek) (p/-done?)))
        (-value [_]
          (-> stack (stack/peek) (p/-value)))
        (-next [this covalue]
          (try
            (-> stack (stack/peek) (p/-next covalue))
            (join stack)
            (catch #?(:clj Throwable :cljs :default) ex
              (if (one? stack)
                (throw ex)
                (do (stack/pop! stack)
                    (p/-throw this ex))))))
        (-throw [_ throwable]
          (let [gen       (stack/peek stack)
                throwable (try
                            (p/-throw gen throwable)
                            nil
                            (catch #?(:clj Throwable :cljs :default) ex ex))]
            (when (some? throwable)
              (if (one? stack)
                (throw throwable))
              (stack/pop! stack)
              (recur throwable))))
        (-return [_ result]
          (let [gen (stack/peek stack)]
            (p/-return gen result)
            (when-not (one? stack)
              (stack/pop! stack)
              (recur result))))))))
