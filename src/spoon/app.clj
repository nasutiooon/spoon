(ns spoon.app
  (:require
   [com.stuartsierra.component :as c]
   [gilmour.aleph :as g.aleph]
   [gilmour.jwt-encoder :as g.jwt]
   [gilmour.ring :as g.ring]
   [spoon.components.db :as c.db]
   [spoon.components.router :as c.router]
   [spoon.components.signer :as c.signer]
   [spoon.edge.pg :as eg.pg]
   [spoon.routes.main :as r.main]
   [spoon.routes.user :as r.user]))

(defn system
  [{:spoon/keys [http-server ring-router guardian passport db]}]
  (-> (c/system-map
       :http-server (g.aleph/http-server http-server)
       :ring-handler (g.ring/ring-head)
       :ring-router (c.router/ring-router ring-router)
       :db (c.db/db-part :db db)
       :db-impl (c.db/db-part :db-impl db)
       :db-migrator (c.db/db-part :db-migrator db)
       :db-generator (c.db/db-part :db-generator db)
       :db-meta (c.db/db-part :db-meta db)
       :guardian (g.jwt/jwt-encoder guardian)
       :passport (g.jwt/jwt-encoder passport)
       :signer-meta (c.signer/signer-meta)
       :main-routes (r.main/main-routes-hook)
       :user-routes (r.user/user-routes-hook))
      (c/system-using
       {:http-server  [:ring-handler]
        :ring-handler [:ring-router :db-meta :signer-meta]
        :ring-router  [:main-routes :user-routes]
        :db           [:db-impl]
        :db-migrator  [:db]
        :db-meta      [:db]
        :signer-meta  [:guardian :passport]})))
