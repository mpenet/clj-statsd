(ns clj-statsd
  "Send metrics to statsd."
  (:import [java.util Random])
  (:import [java.net DatagramPacket DatagramSocket InetAddress]))

(def
  ^{:doc "Atom holding the socket configuration"}
  cfg
  (atom nil))

(def
  ^{:doc "Atom holding the datagram socket"}
  sockagt
  (agent nil))

(defn setup
  "Initialize configuration"
  [host port]
  (send sockagt #(or % (DatagramSocket.)))
  (swap! cfg #(or % {:random (Random.)
                     :host   (InetAddress/getByName host)
                     :port   port})))

(defn publish
  "Send a metric over the network. This should be a fully formatted
   statsd metric line."
  [^String content rate]
  (when (or (>= rate 1.0) (<= (.nextDouble ^Random (:random @cfg)) rate))
    (when-let [packet (try
                        (DatagramPacket.
                         ^"[B" (.getBytes content)
                         ^Integer (count content)
                         ^InetAddress (:host @cfg)
                         ^Integer (:port @cfg))
                        (catch Exception e
                          nil))]
      (send sockagt #(doto ^DatagramSocket %1 (.send %2)) packet))))

(defn increment
  "Increment a counter at specified rate, defaults to a one increment
  with a 1.0 rate"
  ([k]        (increment k 1 1.0))
  ([k v]      (increment k v 1.0))
  ([k v rate] (publish (format "%s:%d|c" (name k) v) rate)))

(defn timing
  "Time an event at specified rate, defaults to 1.0 rate"
  ([k v]      (timing k v 1.0))
  ([k v rate] (publish (format "%s:%d|ms" (name k) v) rate)))

(defn decrement
  "Decrement a counter at specified rate, defaults to a one decrement
  with a 1.0 rate"
  ([k]        (increment k -1 1.0))
  ([k v]      (increment k (* -1 v) 1.0))
  ([k v rate] (increment k (* -1 v) rate)))
