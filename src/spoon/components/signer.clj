(ns spoon.components.signer)

(defrecord SignerMeta [])

(defn signer-meta
  []
  (map->SignerMeta {}))
