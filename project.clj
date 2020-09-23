(defproject darkleaf/generator "0.0.26"
  :description "JS-like generators"
  :url "https://github.com/darkleaf/generators/"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v20.html"}
  :dependencies [[org.clojure/clojure "1.10.1" :scope "provided"]
                 [org.clojure/clojurescript "1.10.597" :scope "provided"]
                 [cloroutine "10"]]
  :repl-options {:init-ns darkleaf.generator.core})
