(require 'cljs.closure)

(cljs.closure/watch "src"
  {:main 'coolant.demo
   :output-to "out/main.js"})
