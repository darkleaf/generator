+ [![CircleCI](https://circleci.com/gh/darkleaf/generator.svg?style=svg)](https://circleci.com/gh/darkleaf/generator)
+ [![Clojars Project](https://img.shields.io/clojars/v/darkleaf/generator.svg)](https://clojars.org/darkleaf/generator)

# Generator

The generator library brings [js-like generators](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Generator),
also known as continuations, to Clojure(Script).

It is useful for building effect systems like:

* [redux-saga](https://redux-saga.js.org/) for JavaScript. This is an awesome example of using generators. Check it out first!
* [darkleaf/effect](https://github.com/darkleaf/effect)  for Clojure(Script)

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

For more examples, see [the test suite](test/darkleaf/generator/core_test.cljc).

Continuations are not first class citizens in an underluing platform like JVM or V8, so we face with
[colored functions](http://journal.stuffwithstuff.com/2015/02/01/what-color-is-your-function/).

Functions that return a generator are red in this terminology, and regular functions are blue.
We can't pass our red functions to blue ones. For example we can't pass them to functions like `map` or `reduce`.
So the library provides `gen/mapv*` and `gen/reduce*`.

By default generators are stackless, so
if you want to call one red function from another one, you have to use `gen/wrap-stack` middleware:

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

Fortunately, there is [Project Loom](https://openjdk.java.net/projects/loom/)
will bring first-class continuations on the JVM.
You can play with it right now with [early access builds](https://jdk.java.net/loom/)
and [experemental version of this library](https://clojars.org/darkleaf/generator/versions/1.0.0-loom).
Check out pull-requests [1](https://github.com/darkleaf/generator/pull/1) and [2](https://github.com/darkleaf/generator/pull/3).

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
