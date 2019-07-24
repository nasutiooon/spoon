(ns spoon.components.signer
  (:require
   [gilmour.ring :as g.ring]))

(defrecord SignerMeta []
  g.ring/RequestBinding
  (request-binding [{:keys [guardian passport]}]
    {:guardian guardian
     :passport passport}))

(defn signer-meta
  []
  (map->SignerMeta {}))


