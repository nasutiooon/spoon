(ns spoon.test.components.spoon-client
  (:require
   [com.stuartsierra.component :as c]))

(defrecord SpoonClient [port host-uri]
  c/Lifecycle
  (start [this]
    (assoc this :host-uri (str "http://127.0.0.1:" port)))
  (stop [this]
    (assoc this :host-uri nil)))

(defn spoon-client
  [config]
  (map->SpoonClient config))
