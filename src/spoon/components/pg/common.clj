(ns spoon.components.pg.common
  (:require
   [camel-snake-kebab.extras :refer [transform-keys]]
   [camel-snake-kebab.core :refer [->kebab-case-keyword]]
   [clj-time.coerce :as coerce]
   [honeysql.core :as sql]
   [honeysql.format :as sql.fmt]
   [honeysql.helpers :as sql.help]
   [jdbc.core :as jdbc]
   [jdbc.proto :as jdbc.proto]))

(defn ->citext
  [v]
  (doto (org.postgresql.util.PGobject.)
    (.setType "citext")
    (.setValue v)))

(defn- sanitize-keys
  [m]
  (transform-keys ->kebab-case-keyword m))

(defn fetch-one
  [conn statement]
  (when-let [result (jdbc/fetch-one conn (sql/format statement))]
    (sanitize-keys result)))

(defn execute!
  [conn statement]
  (jdbc/execute conn (sql/format statement)))

(defmethod sql.fmt/format-clause :returning
  [[_ vs] _]
  (str "RETURNING " (sql.fmt/comma-join (map sql.fmt/to-sql vs))))

(sql.help/defhelper returning
  [m args]
  (assoc m :returning args))

(extend-protocol jdbc.proto/ISQLResultSetReadColumn
  java.sql.Timestamp
  (from-sql-type [this _ _ _]
    (coerce/from-sql-time this))

  org.postgresql.util.PGobject
  (from-sql-type [this _ _ _]
    (let [kind  (.getType this)
          value (.getValue this)]
      (case kind
        "citext" (str value)
        :else    value))))
