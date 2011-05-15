(ns cljfe.feinput
  (:gen-class 
    :name         cljfe.feinput
    :extends      java.io.FilterInputStream
    :prefix       fe-
    :state        state
    :init         init
    :constructors {[java.io.InputStream] [java.io.InputStream]}
    :methods      [[getstate [] clojure.lang.Atom]
                   [getin [] java.io.InputStream]
                   [setavailable [int] int]]
    (:import (java.io InputStream FilterInputStream)))
  (:require [cljfe.input :as input]
            [cljfe.util :as util]))

(in-ns 'cljfe.feinput)

(defn fe-init
  "Initialize the object state."
  ([in]
    [[in] (atom {:in in :myavail 0})]))

(defn fe-getstate
  "Get the object state. Useful for repl debugging."
  ([this] (.state this)))

(defn fe-getin
  "Get the input stream."
  ([this] (:in @(.state this))))

(defn fe-markSupported
  "Set mark is not supported."
  ([this] false))

(defn fe-available
  "Get number of bytes available."
  ([this] 
    (let [avail (:myavail @(.state this))]
      avail)))

(defn fe-setavailable
  "Set and return number of bytes available."
  ([this avail]
    (swap! (.state this) assoc :myavail avail)
    avail))

(defn fe-read
  "Read bytes from input stream filtering for commands."
  ; read a byte from the input stream
  ([this]
    (let [buf (byte-array 1)
          num (.read this buf 0 1)]
      (if (> num 0)
        (aget buf 0)
        num)))

  ; read a buffer from the input stream
  ([this ^bytes buf]
    (.read this buf 0 (alength buf)))

  ; read available data into buffer given offset upto
  ; the given length.
  ([this ^bytes buf offset len]
    (loop [in (.getin this) avl (.available this)]
      (if (neg? avl)
        avl
        (if (pos? avl)
          (let [rnum (.read in buf offset (min avl len))]
            (.setavailable this (- avl rnum))
            rnum)
          (let [inavl (.available in)]
            (if (neg? inavl)
              inavl
              (let [avl (input/read-available-input in)]
                (recur in (.setavailable this avl))))))))))
  
(comment
(import (java.io StringBufferInputStream InputStreamReader BufferedReader))
(def ibuffer (StringBufferInputStream. 
  "/in[15]apple\r\nbutter\r\n/in[14]another line\r\n/in[9]no more\r\n"))
(def fe  (cljfe.feinput. ibuffer))
(def isr (InputStreamReader. fe))
(def bfr (BufferedReader. isr)))
