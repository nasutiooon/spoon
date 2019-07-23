(ns spoon.edge.pg
  (:require
   [gilmour.postgres :as g.pg]
   [gilmour.sql :as g.sql])
  (:import
   [gilmour.postgres Postgres]))

(extend-protocol g.sql/SQLPool
  Postgres
  (pool [this]
    (->> (vals this)
         (filter (partial satisfies? g.sql/SQLPool))
         (map g.sql/pool)
         (first)))
  (pool-spec [this]
    (->> (vals this)
         (filter (partial satisfies? g.sql/SQLPool))
         (map g.sql/pool-spec)
         (first))))
