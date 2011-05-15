(ns cljfe.server
  (:gen-class)
  (:import
    (java.io InputStreamReader OutputStreamWriter)
    (java.io PrintWriter PrintStream)
    (java.io BufferedReader PushbackReader)
    (java.io InputStream BufferedInputStream)
    (java.io DataInputStream LineNumberReader)
    (clojure.lang LineNumberingPushbackReader))
  (:require
    [clojure.contrib.server-socket :as ss]
    [clojure.contrib.seq-utils :as su]
    [clojure.contrib.def :as def]
    [cljfe.input :as in]
    [cljfe.client]
    [cljfe.feinput]
    [cljfe.feinputstreamreader]
    [cljfe.output :as out]
    [cljfe.util :as util]
    [clojure.main :only (repl)]))

(in/add-ns *ns*)

(defn- cmd-arg
  "Process argument data for the client and 
   return number of bytes available as zero."
  ([cmd input]
    (let [[client data] (cljfe.client/get-client-data input (:length cmd))]
      (cljfe.client/update-list-data client :args data))
    0))

(defn- cmd-env
  "Process environment data for the client and 
   return number of bytes available as zero."
  ([cmd input]
    (let [[client data] (cljfe.client/get-client-data input (:length cmd))]
      (cljfe.client/update-list-data client :env data))
    0))

(defn- cmd-dir
  "Process working directory data for the client and 
   return number of bytes available as zero."
  ([cmd input]
    (let [[client data] (cljfe.client/get-client-data input (:length cmd))
           dir          (util/to-string data)]
      (cljfe.client/update-key-data client :workdir dir))
    0))

(defn- cmd-main
  "Process main command, invoking clojure.main or the clojure repl.
   return number bytes available as -1 indicating eof or done processing."
  ([cmd input]
    (let [[client data] (cljfe.client/get-client-data input (:length cmd))]
      (if (empty? data)
        (binding [*in*  (LineNumberingPushbackReader. (:input client))
                  *out* (PrintWriter. (:output client))
                  *err* (PrintWriter. (:err client))]
          (clojure.main/repl))
        (binding [*in*  (BufferedReader. (:input client))
                  *out* (OutputStreamWriter. (:output client))
                  *err* (PrintWriter. (:err client))]
          (apply clojure.main/main
            (concat (list (util/to-string data)) (:args client))))))
    -1))

(defn- handle-connection
  "Handles the client connection."
  ([input output]
    (let [client (cljfe.client/create-client input output)]
      (try
        (loop [n (.read (:input client))]
          (if (> n -1)
            (recur (.read (:input client)))))
        (catch Exception x
          (binding [*out* (OutputStreamWriter. System/err)
                    *err* (OutputStreamWriter. System/err)]
            (.printStackTrace x)))
        (finally (cljfe.client/free-client client))))))

; atomic set of servers
(def/defvar- *servers* (atom #{}))

(defn is-server-started
  "If the server is in the server set, its started."
  ([server]
   (@*servers* server)))

(defn- add-server
  "Add server to server set, return server."
  ([server]
    (swap! *servers* #(conj % server))
     server))

(defn- remove-server
  "Remove server from server set."
  ([server]
    (swap! *servers* #(disj % server))
     server))

(defn start-server
  "Starts clj-fe server on the given port and returns it"
  ([]
    (start-server 4442))
  ([port]
    (add-server (ss/create-server port handle-connection))))

(defn stop-server
  "Stops the clj-fe server"
  ([server]
    (ss/close-server (remove-server server))))
