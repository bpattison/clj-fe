(loop [l (read-line)]
  (println ">" l)
  (when-not (= "quit" l)
    (recur (read-line))))
