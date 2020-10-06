(ns darkleaf.generator.loom-test
  (:require
   [darkleaf.generator.core :as gen]
   [darkleaf.generator.loom :refer [generator yield]]
   [clojure.test :as t]
   [clojure.template :refer [do-template]])
  (:import [clojure.lang ExceptionInfo]))

(t/deftest loom-killer-feature-test
  (let [f   (fn [x]
              (yield [:inc x]))
        gen (generator
             (mapv f [0 1 2]))]

    (t/is (= [:inc 0] (gen/value gen)))
    (gen/next gen 1)
    (t/is (= [:inc 1] (gen/value gen)))
    (gen/next gen 2)
    (t/is (= [:inc 2] (gen/value gen)))
    (gen/next gen 3)

    (t/is (= [1 2 3] (gen/value gen)))
    (t/is (gen/done? gen))))

;; ~~~~~~~~~~~~~~~~ core-test ~~~~~~~~~~~~~~~~

(defmacro with-wrappers [w-name & body]
  `(do-template [~w-name] (t/testing [:wrapper ~w-name]
                            ~@body)
                #'identity
                #'gen/wrap-stack))

(t/deftest null-test
  (with-wrappers wrap
    (let [gen (generator)
          gen (wrap gen)]
      (t/is (nil? (gen/value gen)))
      (t/is (gen/done? gen)))))

(t/deftest yield-0-&-next-1-test
  (with-wrappers wrap
    (let [gen (generator
                (yield))
          gen (wrap gen)]
      (t/is (nil? (gen/value gen)))
      (gen/next gen)
      (t/is (nil? (gen/value gen)))
      (t/is (gen/done? gen)))))

(t/deftest yield-1-&-next-2-test
  (with-wrappers wrap
    (let [gen (generator
                (yield :my-value))
          gen (wrap gen)]
      (t/is (= :my-value (gen/value gen)))
      (gen/next gen :my-covalue)
      (t/is (= :my-covalue (gen/value gen)))
      (t/is (gen/done? gen)))))

(t/deftest throw-test
  (with-wrappers wrap
    (let [gen (generator
                (yield :a)
                (throw (ex-info "My error" {})))
          gen (wrap gen)]
      (t/is (= :a (gen/value gen)))
      (t/is (thrown? ExceptionInfo (gen/next gen)))
      (t/is (gen/done? gen)))))

(t/deftest gen-throw-&-catch-test
  (with-wrappers wrap
    (let [gen (generator
                (try
                  (yield)
                  (catch ExceptionInfo ex
                    (ex-message ex))))
          gen (wrap gen)]
      (gen/throw gen (ex-info "My error" {}))
      (t/is (= "My error" (gen/value gen)))
      (t/is (gen/done? gen)))))

(t/deftest gen-throw-&-finally-test
  (with-wrappers wrap
    (let [gen (generator
                (try
                  (yield)
                  (catch ExceptionInfo ex
                    (ex-message ex))
                  (finally
                    (yield :from-finally))))
          gen (wrap gen)]
      (gen/throw gen (ex-info "My error" {}))
      (t/is (= :from-finally (gen/value gen)))
      (gen/next gen)
      (t/is (= "My error" (gen/value gen)))
      (t/is (gen/done? gen)))))

(t/deftest return-1-test
  (with-wrappers wrap
    (let [gen (generator
                (yield :a)
                (yield :b)
                42)
          gen (wrap gen)]
      (gen/return gen)
      (t/is (nil? (gen/value gen)))
      (t/is (gen/done? gen)))))

(t/deftest return-2-test
  (with-wrappers wrap
    (let [gen (generator
                (yield :a)
                (yield :b)
                42)
          gen (wrap gen)]
      (gen/return gen 0)
      (t/is (= 0 (gen/value gen)))
      (t/is (gen/done? gen)))))

(t/deftest return-&-catch-test
  (with-wrappers wrap
    (let [gen (generator
                (try
                  (yield :a)
                  (yield :b)
                  42
                  ;; this types and its subtypes are only allowed
                  (catch Exception ex
                    nil)))
          gen (wrap gen)]
      (gen/return gen 0)
      (t/is (= 0 (gen/value gen)))
      (t/is (gen/done? gen)))))

(t/deftest return-&-finally-test
  (with-wrappers wrap
    (let [gen (generator
                (try
                  (yield :a)
                  42
                  (finally
                    (yield :b))))
          gen (wrap gen)]
      (t/is (= :a (gen/value gen)))
      (gen/return gen 0)
      (t/is (= :b (gen/value gen)))
      (gen/next gen)
      (t/is (= 0 (gen/value gen)))
      (t/is (gen/done? gen)))))

(t/deftest illegal-state-test
  (with-wrappers wrap
    (let [gen (generator)
          gen (wrap gen)]
      (t/are [form] (thrown-with-msg? ExceptionInfo
                                      #"^Generator is done$"
                                      form)
        (gen/next gen)
        (gen/throw gen (ex-info "My error" {}))
        (gen/return gen)))))

(t/deftest stack-test
  (let [nested (generator
                 [(yield :a)
                  (yield :b)])
        gen    (generator
                 [(yield :start)
                  (yield nested)
                  (yield :finish)])
        gen    (gen/wrap-stack gen)]
    (t/is (= :start (gen/value gen)))
    (gen/next gen 1)
    (t/is (= :a (gen/value gen)))
    (gen/next gen 2)
    (t/is (= :b (gen/value gen)))
    (gen/next gen 3)
    (t/is (= :finish (gen/value gen)))
    (gen/next gen 4)
    (t/is (= [1 [2 3] 4] (gen/value gen)))
    (t/is (gen/done? gen))))

(t/deftest stack-test-2
  (let [nested (generator
                 (yield :a))
        gen    (generator
                 (yield nested))
        gen    (gen/wrap-stack gen)]
    (t/is (= :a (gen/value gen)))
    (gen/next gen :my-value)
    (t/is (= :my-value (gen/value gen)))
    (t/is (gen/done? gen))))

(t/deftest stack-&-finally-test
  (let [nested (generator
                 (yield :a))
        gen    (generator
                 (yield :start)
                 (try
                   (yield nested)
                   (finally
                     (yield :finish))))
        gen    (gen/wrap-stack gen)]
    (t/is (= :start (gen/value gen)))
    (gen/next gen)
    (t/is (= :a (gen/value gen)))
    (gen/next gen :my-value)
    (t/is (= :finish (gen/value gen)))
    (gen/next gen)
    (t/is (= :my-value (gen/value gen)))
    (t/is (gen/done? gen))))

(t/deftest tail-call-test
  (let [nested (generator
                 (yield :a))
        gen    (generator
                 (yield :start)
                 nested)
        gen    (gen/wrap-stack gen)]
    (t/is (= :start (gen/value gen)))
    (gen/next gen)
    (t/is (= :a (gen/value gen)))
    (gen/next gen :my-value)
    (t/is (= :my-value (gen/value gen)))
    (t/is (gen/done? gen))))

(t/deftest tail-call-&-finally-test
  (let [nested (generator
                 (yield :a))
        gen    (generator
                 (yield :start)
                 (try
                   nested
                   (finally
                     (yield :finish))))
        gen    (gen/wrap-stack gen)]
    (t/is (= :start (gen/value gen)))
    (gen/next gen)
    (t/is (= :finish (gen/value gen)))
    (gen/next gen)
    (t/is (= :a (gen/value gen)))
    (gen/next gen :my-value)
    (t/is (= :my-value (gen/value gen)))
    (t/is (gen/done? gen))))

(t/deftest thrown-in-stack-test
  (let [nested (generator
                 (yield :a)
                 (try
                   (throw (ex-info "My error" {}))
                   (finally
                     (yield :b))))
        gen    (generator
                 (yield :start)
                 (try
                   (yield nested)
                   (catch ExceptionInfo ex
                     (ex-message ex))
                   (finally
                     (yield :finish))))
        gen    (gen/wrap-stack gen)]
    (t/is (= :start (gen/value gen)))
    (gen/next gen)
    (t/is (= :a (gen/value gen)))
    (gen/next gen)
    (t/is (= :b (gen/value gen)))
    (gen/next gen)
    (t/is (= :finish (gen/value gen)))
    (gen/next gen)
    (t/is (= "My error" (gen/value gen)))
    (t/is (gen/done? gen))))

(t/deftest thrown-in-stack-test-2
  (let [nested-1 (generator
                   (yield :a))
        nested-2 (generator
                   (yield nested-1)
                   (throw (ex-info "My error" {})))
        gen      (generator
                   (yield :start)
                   (try
                     (yield nested-2)
                     (catch ExceptionInfo ex
                       (ex-message ex))
                     (finally
                       (yield :finish))))
        gen      (gen/wrap-stack gen)]
    (t/is (= :start (gen/value gen)))
    (gen/next gen)
    (t/is (= :a (gen/value gen)))
    (gen/next gen)
    (t/is (= :finish (gen/value gen)))
    (gen/next gen)
    (t/is (= "My error" (gen/value gen)))
    (t/is (gen/done? gen))))

(t/deftest gen-throw-in-stack
  (let [nested (generator
                 (try
                   (yield :a)
                   (yield :b)
                   (finally
                     (yield :nested))))
        gen    (generator
                 (try
                   (yield nested)
                   42
                   (finally
                     (yield :finish))))
        gen    (gen/wrap-stack gen)]
    (t/is (= :a (gen/value gen)))
    (gen/throw gen (ex-info "My error" {}))
    (t/is (= :nested (gen/value gen)))
    (gen/next gen)
    (t/is (= :finish (gen/value gen)))
    (t/is (thrown? ExceptionInfo (gen/next gen)))))

(t/deftest gen-throw-in-stack-2
  (let [nested (generator
                 (try
                   (yield :a)
                   (yield :b)))
        gen    (generator
                 (try
                   (yield nested)
                   42
                   (finally
                     (yield :finish))))
        gen    (gen/wrap-stack gen)]
    (t/is (= :a (gen/value gen)))
    (gen/throw gen (ex-info "My error" {}))
    (t/is (= :finish (gen/value gen)))
    (t/is (thrown? ExceptionInfo (gen/next gen)))))

(t/deftest return-in-stack
  (let [nested (generator
                 (yield :a)
                 (yield :b))
        gen    (generator
                 (try
                   (yield nested)
                   42
                   (finally
                     (yield :finish))))
        gen    (gen/wrap-stack gen)]
    (t/is (= :a (gen/value gen)))
    (gen/return gen 0)
    (t/is (= :finish (gen/value gen)))
    (gen/next gen)
    (t/is (= 0 (gen/value gen)))))
