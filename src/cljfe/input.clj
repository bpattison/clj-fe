(ns cljfe.input
  (:gen-class) 
  (:import (java.io InputStream))
  (:require 
    [clojure.contrib.def :as def]
    [cljfe.util :as util]))

(def/defvar- *cmd-namespace* (ref []))

(defn add-ns
  "Add namespace to search for procesing commands."
  ([ns]
    (dosync (alter *cmd-namespace* conj ns))))

(defn- fn-for
  "Return the first functions found in the given namespaces."
  ([cmdstr]
    (let [cmdname (str "cmd-" cmdstr)]
      (first 
        (remove nil?
          (for [ns @*cmd-namespace*]
             (find-var (symbol (str ns) cmdname))))))))

(add-ns *ns*)

(defn read-data
  "Read length amount of data from input.
   Return number of bytes read, with data." 
  ([^java.io.InputStream input length]
    (let [b (byte-array length)
          n (.read input b 0 length)]
;         (util/log "reader.log" "n=" n " b=" (String. b 0 n))
          [n b])))

(defn read-until-while
  "Read bytes from input until delimeter d reached and while
   function f is valid and while eof isn't reached."
  ([^java.io.InputStream i d f]
    (loop [[n b] (read-data i 1) data []]
      (if (neg? n)
        [n]
        (let [c (char (first b))]
          (if (= c d)
            data
            (if (f c)
              (recur (read-data i 1) (conj data c))
              data)))))))

(defn read-cmd
  "Read command from input."
  ([^java.io.InputStream i]
    (try
      (let [[n b] (read-data i 1)]
        (if (neg? n)
          {:cmd "eof" :length -1}
          (if (= (char (first b)) \/)
            (let [cmd (read-until-while i \[ #(Character/isLetter %))
                  len (read-until-while i \] #(Character/isDigit  %))]
              (if (or (some #{-1} cmd) (some #{-1} len))
                {:cmd "eof" :length -1}
                (if (or (empty? cmd) (empty? len))
                  (do 
                    (util/log "reader.log" "cmd=" cmd "len=" len)
                    {:cmd :unknown :length 0})
                  {:cmd (util/to-string cmd)
                   :length (Integer. (util/to-string len))})))
            (recur i))))
        (catch Exception x {:cmd "eof" :length -1}))))

(defn- apply-cmd
  "Looks a command function and applies it."
  ([^java.io.InputStream i cmd]
;   (util/log "reader.log" cmd)
    (if-let [cmd-fn (fn-for (:cmd cmd))]
      (try
        (apply cmd-fn (list cmd i))
        (catch Exception x (.printStackTrace x))))))

(defn read-cmds-until
  "Read commands and processes commands from input, until given
   command is found, or eof occurs. Return the available :length."
  ([^java.io.InputStream i match]
    (loop [cmd (read-cmd i)]
;     (if (or (= (:cmd cmd) match) (= (:cmd cmd) "eof"))
      (if (= (:cmd cmd) match)
        (:length cmd)
        (let [n (apply-cmd i cmd)]
          (if (= n -1)
            -1
            (recur (read-cmd i))))))))

(defn read-available-input
  "Read commands from input until an input command is found,
   then get how much input data is available."
  ([^java.io.InputStream i]
    (read-cmds-until i "in")))

(defn cmd-eof
  "Process client input and return eof."
  ([cmd ^java.io.InputStream input]
   (.close input)
    -1))
