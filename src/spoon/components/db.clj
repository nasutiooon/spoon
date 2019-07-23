(ns spoon.components.db
  (:require
   [gilmour.hikari :as g.hikari]
   [gilmour.migratus :as g.migratus]
   [gilmour.postgres :as g.pg]))

(def catalogue
  {:db           {:pg (fn [_] (g.pg/postgres))}
   :db-impl      {:pg g.hikari/hikari}
   :db-migrator  {:pg g.migratus/migratus}
   :db-generator {:pg g.pg/postgres-generator}})

(defn db-part
  [instance-kind {:keys [kind] :as config}]
  (if-let [ctor (get-in catalogue [instance-kind kind])]
    (-> config (get kind) ctor)
    (throw (ex-info "unsupported db" {:kind kind}))))

(defrecord DbMeta [])

(defn db-meta
  []
  (map->DbMeta {}))
