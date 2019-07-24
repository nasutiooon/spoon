(ns spoon.components.db
  (:require
   [com.stuartsierra.component :as c]
   [gilmour.hikari :as g.hikari]
   [gilmour.migratus :as g.migratus]
   [gilmour.postgres :as g.pg]
   [gilmour.ring :as g.ring]
   [spoon.edge.db :as eg.db]))

(defrecord DbMeta []
  g.ring/RequestBinding
  (request-binding [{:keys [db]}]
    {:db db}))

(defn- db-meta
  []
  (map->DbMeta {}))

(defrecord DbMigratorRunner []
  c/Lifecycle
  (start [this]
    (eg.db/migrate! (->> (vals this)
                         (filter (partial satisfies? eg.db/Migratable))
                         (first)))
    this)
  (stop [this]
    this))

(defn- db-migrator-runner
  []
  (map->DbMigratorRunner {}))

(defrecord DurableDbGeneratorRunner []
  c/Lifecycle
  (start [this]
    (eg.db/create! (->> (vals this)
                        (filter (partial satisfies? eg.db/Creatable))
                        (first)))
    this)
  (stop [this]
    this))

(defn- durable-db-generator-runner
  []
  (map->DurableDbGeneratorRunner {}))

(defrecord EphemeralDbGeneratorRunner []
  c/Lifecycle
  (start [this]
    (eg.db/create! (->> (vals this)
                        (filter (partial satisfies? eg.db/Creatable))
                        (first)))
    this)
  (stop [this]
    (eg.db/destroy! (->> (vals this)
                         (filter (partial satisfies? eg.db/Creatable))
                         (first)))
    this))

(defn- ephemeral-db-generator-runner
  []
  (map->EphemeralDbGeneratorRunner  {}))

(defn- db-generator-runner
  [{:keys [generate]}]
  (if (:temporary? generate)
    (ephemeral-db-generator-runner)
    (durable-db-generator-runner)))

(def catalogue
  {:db                  {:pg (fn [_] (g.pg/postgres))}
   :db-impl             {:pg g.hikari/hikari}
   :db-migrator         {:pg g.migratus/migratus}
   :db-migrator-runner  {:pg (fn [_] (db-migrator-runner))}
   :db-generator        {:pg g.pg/postgres-generator}
   :db-generator-runner {:pg db-generator-runner}
   :db-meta             {:pg (fn [_] (db-meta))}})

(defn db-part
  [instance-kind {:keys [kind] :as config}]
  (if-let [ctor (get-in catalogue [instance-kind kind])]
    (-> config (get kind) ctor)
    (throw (ex-info "unsupported db" {:kind kind}))))
