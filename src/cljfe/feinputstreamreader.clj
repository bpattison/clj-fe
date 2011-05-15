(ns cljfe.feinputstreamreader
  (:gen-class 
    :name            cljfe.feinputstreamreader
    :extends         java.io.InputStreamReader
    :prefix          fe-
    :state           state
    :init            init
    :constructors    {[java.io.InputStream] [java.io.InputStream]}
    :methods         [[getin [] java.io.InputStream]]
    (:import (java.io BufferedReader)))
  (:require [cljfe.util :as util]))

(in-ns 'cljfe.feinputstreamreader)

(defn fe-init
  "Initialize the object state."
  ([in]
    [[in] (atom {:in in})]))

(defn fe-getin
  "Get the input stream"
  ([this] (:in @(.state this))))

(defn fe-read
  "Read input from input stream."

  ; read a single character
  ([this]
    (.read (.getin this)))

  ; read characters into a portion of an array
  ([this cbuf off len]
    (let [buf (byte-array len)
          num (.read (.getin this) buf 0 len)
          str (String. buf 0 num)
          src (.toCharArray str)]
       (System/arraycopy src 0 cbuf off num)
       num)))

(defn fe-ready
  "Tell whether this stream is ready to be read."
  ([this]
    (> (.available (.getin this) 0))))
