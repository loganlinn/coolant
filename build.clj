(require 'cljs.closure)

(cljs.closure/build "src"
  {:main 'coolant.core
   :output-to "out/main.js"})
