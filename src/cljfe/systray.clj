(ns cljfe.systray
  (:gen-class)
  (:import 
    (java.lang System)
    (java.io File)
    (javax.imageio ImageIO)
    (javax.swing JFrame JLabel JPanel ImageIcon JOptionPane JRootPane)
    (java.awt SystemTray TrayIcon Image)
    (java.awt PopupMenu MenuItem AWTException BorderLayout)
    (java.awt Color Graphics)
    (java.awt.event ActionListener ActionEvent)
    (java.awt.image BufferedImage))
  (:require
    [clojure.contrib.def :as def]
    [cljfe.util :as util]
    [cljfe.server]))
  
(def java-version (System/getProperty "java.version" "Not defined"))

; holds the server singleton
(def/defvar- *server* (atom nil))

(defn- start-server
  "Start the clj-fe servers."
  ([]
    (swap! *server*
      (fn [s]
        (when-not s
          (cljfe.server/start-server))))))

(defn- stop-server
  "Stops the clj-fe servers."
  ([]
    (swap! *server*
      (fn [s]
        (when s 
          (cljfe.server/stop-server s)) nil))))

(defn- server-started?
  "Checks if the server is started."
  ([] (if @*server* true false)))

(defn- pathcat
  "Concatenate two string paths."
  ([s1 s2]
   (apply str
     (concat
       (.replace s1 \\ \/) 
       (.replace s2 \\ \/)))))

(defn- read-image
  "Reads an icon from the file f."
  ([f]
    (try 
      (ImageIO/read (new File f))
      (catch Exception e
        (println "Unable to read file " f)))))

(defn- create-panel
  "Create a panel proxy and override paint method."
  ([image]
    (proxy [JPanel] []
      (paint [^Graphics g]
        (.drawImage g image 0 0 nil)))))

(defn- setWindowStyle
  "Set the windows style for the frame."
  ([frame style]
    (let [root (.getRootPane frame)]
      (.setWindowDecorationStyle root style))))

(defn- about-action
  "Create an About ActionListener proxy with the given
  image directory."
  ([img-dir]
    (proxy [ActionListener] []
      (actionPerformed [^ActionEvent event]
        (let [image (read-image (pathcat img-dir "/clj-fe-splash-screen.bmp"))
              panel (create-panel image)]
          (doto (JFrame.)
            (.setTitle "About Clj-Fe")
            (.add panel)
            (.setUndecorated true)
            (.setLocationRelativeTo nil)
            (.setResizable false)
            (setWindowStyle JRootPane/PLAIN_DIALOG)
            (.pack)
            (.setSize (.getWidth image) (+ (.getHeight image) 30))
            (.show)))))))
  
(defn- start-action
  "Create a start ActionListener proxy, that closes over
  start & stop menu items."
  ([start-item stop-item]
    (proxy [ActionListener] []
      (actionPerformed [^ActionEvent event]
        (start-server)
        (.setEnabled start-item (not (server-started?)))
        (.setEnabled stop-item (server-started?))))))

(defn- really-stop?
  ([] 
    (let [pane (JOptionPane.
           "Stop Clj-Fe server and all running\n applications?")]
      (.setOptions pane (object-array ["Yes" "No"]))
      (doto (.createDialog pane nil "Clj-Fe")
        (.dispose)
        (.setUndecorated true)
        (setWindowStyle JRootPane/PLAIN_DIALOG)
        (.show))
      (= (.getValue pane) "Yes"))))

(defn- stop-action
  "Create a stop ActionListener proxy, that closes over
  start & stop menu items."
  ([start-item stop-item]
    (proxy [ActionListener] []
      (actionPerformed [^ActionEvent event]
        (when (really-stop?)
          (stop-server)
          (.setEnabled start-item (not (server-started?)))
          (.setEnabled stop-item (server-started?)))))))

(def exit-action
  (proxy [ActionListener] []
    (actionPerformed [^ActionEvent event] 
      (when (really-stop?)
        (stop-server)
        (System/exit 0)))))

(defn- build-tray
  "Build tray icon with popups."
  ([img-dir]
    (let [tray-icon  (new TrayIcon 
                       (read-image (pathcat img-dir "/clj-fe-logo.jpg")) "")
          popup-menu (new PopupMenu)
          start-item (new MenuItem "Start")
          stop-item  (new MenuItem "Stop")]
      (.setEnabled start-item (not (server-started?)))
      (.setEnabled stop-item (server-started?))
      (doto tray-icon
        (.setImageAutoSize true)
        (.setPopupMenu
          (doto popup-menu
            (.add 
              (doto (new MenuItem "About")
                (.addActionListener (about-action img-dir))))
            (.add
              (doto start-item
                (.addActionListener (start-action start-item stop-item))))
            (.add
              (doto stop-item
                (.addActionListener (stop-action start-item stop-item))))
            (.addSeparator)
            (.add
              (doto (new MenuItem "Exit")
                (.addActionListener exit-action)))))))))

(defn -main
  "Main function to start the server"
  ([] (-main "images"))
  ([img-dir]
    (start-server)
    (if-not (SystemTray/isSupported)
      (println "System tray not supported")
      (try
        (.add (SystemTray/getSystemTray)
          (build-tray img-dir))
        (catch AWTException e
          (println "Clojure Front-End system tray could not be added."))))))
