(import '(java.io File))

(doseq [f (file-seq (new File "."))]
  (println (.getPath f)))

; (println "Hit return key to exit...")
; (println (read-line))
