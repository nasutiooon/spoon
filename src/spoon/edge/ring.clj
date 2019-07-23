(ns spoon.edge.ring
  (:require
   [gilmour.ring :as g.ring]
   [spoon.components.db :as c.db]
   [spoon.components.signer :as c.signer])
  (:import
   [spoon.components.db DbMeta]
   [spoon.components.signer SignerMeta]))

(extend-protocol g.ring/RequestBinding
  SignerMeta
  (request-binding [{:keys [guardian passport]}]
    {:guardian guardian
     :passport passport})

  DbMeta
  (request-binding [{:keys [db]}]
    {:db db}))
