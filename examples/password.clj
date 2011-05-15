(import '(java.lang System))

(let [console (System/console)
     pwd      (.readPassword console)]
   (println "your password is " pwd))
