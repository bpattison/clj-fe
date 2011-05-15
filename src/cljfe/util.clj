(ns cljfe.util
  (:gen-class)
  (:require [clojure.contrib.duck-streams :as ds]))

(defn to-string
  "Turns sequence of data into a string."
  ([data]
    (if-let [s (seq data)]
      (apply str (map char s))
      "")))

(defn logc
  "Log a character to a log file."
  ([fname c]
   (ds/append-spit fname c)))

(defn log
  "Logs a line of data to a log file."
  ([fname data]
    (ds/append-spit fname data)
    (ds/append-spit fname "\n"))
  ([fname data & more]
    (ds/append-spit fname data)
    (apply (partial log fname) more)))
