(ns spoon.middleware.app
  (:require
   [gilmour.postgres :as g.pg]
   [reitit.ring.middleware.exception :as exception]
   [ring.logger :as logger]
   [ring.middleware.cors :as cors]
   [ring.util.http-response :as res]
   [taoensso.timbre :as log]))

(defn- pg-ex-handler
  [ex _]
  (case (g.pg/error-code ex)
    "23505" (res/conflict)
    (res/internal-server-error)))

(def exception-middleware
  (exception/create-exception-middleware
   (merge
    exception/default-handlers
    {org.postgresql.util.PSQLException pg-ex-handler})))

(def cors-middleware
  {:name        ::cors
   :description "middleware to handle cors"
   :wrap        (fn [handler {{:keys [origins methods]} :cors}]
                  (let [origins (map re-pattern origins)]
                    (cors/wrap-cors handler
                                    :access-control-allow-origin origins
                                    :access-control-allow-headers methods)))})

(def log-middleware
  {:name        ::log
   :description "middleware that logs every request and response made"
   :wrap        (fn [handler]
                  (let [log-fn (fn [{:keys [level throwable message]}]
                                 (log/log level throwable message))]
                    (logger/wrap-with-logger handler {:log-fn log-fn})))})
