+ [![CircleCI](https://circleci.com/gh/darkleaf/generator.svg?style=svg)](https://circleci.com/gh/darkleaf/generator)
+ [![Clojars Project](https://img.shields.io/clojars/v/darkleaf/generator.svg)](https://clojars.org/darkleaf/generator)

# Generator

The generator library brings js-like generators, also known as continuations, to Clojure(Script).

```clojure
(require '[darkleaf.generator.core :as gen :refer [generator yield]])

(let [f*  (fn []
            (generator
             (yield :my-value)))
      gen (f*)]
  (assert (= :my-value (gen/value gen)))
  (gen/next gen :my-covalue)
  (assert (= :my-covalue (gen/value gen)))
  (assert (gen/done? gen)))
```

For more examples, see [the test suite]().

Continuations are not first class citizens in underluing platform like JVM or V8, so we face with colored functions problem.
We can't pass our red functions to blue ones. For example we can't pass them to functions like `map` or `reduce`.
So the library provides `gen/mapv*` and `gen/reduce*`.

If you want call one red fn from another one, you should use `gen/wrap-stack` middleware:

```clojure
(let [nested* (fn []
                (generator
                 [(yield :a)
                  (yield :b)]))
      f*      (fn []
                (generator
                 [(yield :start)
                  (yield (nested*))
                  (yield :finish)]))
      f*      (gen/wrap-stack f*)
      gen     (f*)]
  (assert (= :start (gen/value gen)))
  (gen/next gen 1)
  (assert (= :a (gen/value gen)))
  (gen/next gen 2)
  (assert (= :b (gen/value gen)))
  (gen/next gen 3)
  (assert (= :finish (gen/value gen)))
  (gen/next gen 4)
  (assert (= [1 [2 3] 4] (gen/value gen)))
  (assert (gen/done? gen)))
```

Fortunately, there is the Project Loom brings first class continuations on the JVM.
You can play with in right now. You should install early access build.


## Pro tips

You can use threading macors like this:

```clojure
(generator
 (-> value
     regular-fn
     (-> gen-fn* yield)
     other-regular-fn
     (-> other-gen-fn* yield)))
```
