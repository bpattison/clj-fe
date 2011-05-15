(ns cljfe.output
  (:gen-class) 
  (:import (java.io IOException OutputStream FilterOutputStream))
  (:require
    [clojure.contrib.duck-streams :as ds]))

;(defn bytes-to-string
;  "Turns a vector of integer data to string."
;  [data]
;    (apply str (map char (vec data))))

(defn write-filtered-output
  "Write filter with offset and length."
  ([o cmd #^bytes b offset length]
    (let [c (.getBytes cmd)
          l (.getBytes (format "[%d]" length))]
        (.write o c 0 (alength c))
        (.write o l 0 (alength l))
        (.write o b 0 length))))

(defn- filteroutputstream-proxy
  "Defines a proxy for the FilterOutputStream to send server commands."
  ([o cmd]
    (proxy [FilterOutputStream] [#^java.io.OutputStream o]
      (write
        ([b]
          (if (= java.lang.Integer (type b))
            (.write this (byte-array 1 (byte b)) 0 1)
            (.write this b 0 (alength b))))
        ([#^bytes b offset length]
          (write-filtered-output o cmd b offset length))))))

(defn create-filtered-output
  "Creates a filtered output stream."
  ([o cmd]
    (filteroutputstream-proxy o cmd)))

(comment 
(def fos (create-filtered-output System/out "/err"))
(.write System/out (.getBytes "out") 0 3)
(println "") 
(.write fos (.getBytes "alpha\n"))
(.write fos (.getBytes "beta\n"))
(.write fos (.getBytes "four score and seven years ago, our founding fathers set upon this nation\n"))
(.write fos (.getBytes "44\n"))
(import (java.io PrintStream))
(def ps (PrintStream. fos)) 
(.println ps 3.4) 
(.print ps "this is a string") 
(def b (.getBytes "humma humma\n"))
(.write ps b 0 (alength b))
(.write ps (int 65))
(.write ps (int 66))
(.write ps (int 67))
(.write ps (int \n))
)
