(ns cljfe.client
  (:gen-class)
  (:import
    (java.io InputStreamReader OutputStreamWriter)
    (java.io DataInputStream LineNumberReader)
    (clojure.lang LineNumberingPushbackReader)
    (java.io InputStream BufferedInputStream)
    (java.io BufferedReader PushbackReader)
    (java.io PrintWriter PrintStream))
  (:require
    [clojure.contrib.server-socket :as ss]
    [clojure.contrib.seq-utils :as su]
    [clojure.contrib.def :as def]
    [cljfe.feinputstreamreader]
    [cljfe.output :as out]
    [cljfe.input :as in]
    [cljfe.feinput]
    [cljfe.util :as util]))

(def/defvar- *clients* (ref []))

(defrecord Client
  [socket        ; socket stream
   input         ; input stream
   output        ; output stream
   err           ; err stream
   args          ; arguments
   env           ; environment properties
   workdir])     ; working directory

(defn- find-client
  "Finds a client based on the input."
  ([input]
    (su/find-first #(= input (:socket %)) @*clients*)))

(defn- sync-client
  "Updated client in the clients vector." 
  ([old-client new-client] 
    (dosync 
      (alter *clients*
        #(replace {old-client new-client} %)))
    new-client))

(defn get-client-data
  "Gets the client and reads number of bytes
   from the input returning both."
  ([input number]
    (let [[n b] (in/read-data input number)]
      [(find-client input) b])))

(defn update-key-data
  "Updates data for the key and syncs the client." 
  ([client k data] 
    (sync-client client (assoc client k data))))

(defn update-list-data
  "Updates the value of a list in a hash identified by k."
  ([client k data]
    (let [s (util/to-string data)]
      (update-key-data client k (concat (k client) (list s))))))

(defn create-client
  "Create and add new client to the client list"
  ([sockin sockout]
    (let [client
           (Client.
             sockin                                         ; :socket
             (cljfe.feinputstreamreader.
               (cljfe.feinput. sockin))                     ; :input
             (PrintStream.
               (out/create-filtered-output sockout "/out")) ; :output
             (PrintStream.
               (out/create-filtered-output sockout "/err")) ; :err
             '() '() "")]                                   ; :args, :env, :workdir
      (dosync (alter *clients* conj client))
      client)))

(defn free-client
  "Remove client from client list and free reference to it."
  ([client]
    (dosync
      (alter *clients*
        (partial remove #(identical? client %))))))
