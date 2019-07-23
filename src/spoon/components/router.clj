(ns spoon.components.router
  (:require
   [com.stuartsierra.component :as c]
   [gilmour.ring :as g.ring]
   [muuntaja.core :as muuntaja]
   [reitit.coercion.spec :as spec]
   [reitit.ring :as ring]
   [reitit.ring.coercion :as coercion]
   [reitit.ring.middleware.muuntaja :as middleware.muuntaja]
   [reitit.ring.middleware.parameters :as middleware.params]
   [spoon.middleware.app :as m.app]))

(defprotocol RouteProvider
  (routes [this]))

(defrecord RingRouter [router]
  c/Lifecycle
  (start [this]
    (let [attached-routes (->> (vals this)
                               (filter (partial satisfies? RouteProvider))
                               (mapcat routes))
          middleware      [m.app/log-middleware
                           middleware.params/parameters-middleware
                           middleware.muuntaja/format-middleware
                           m.app/exception-middleware
                           [m.app/cors-middleware this]
                           coercion/coerce-exceptions-middleware
                           coercion/coerce-request-middleware
                           coercion/coerce-response-middleware]
          router          (ring/router
                           attached-routes
                           {:data {:coercion   spec/coercion
                                   :muuntaja   muuntaja/instance
                                   :middleware middleware}})]
      (assoc this :router router)))
  (stop [this]
    (assoc this :router nil))

  g.ring/RequestHandler
  (request-handler [_]
    (ring/ring-handler
     router
     (ring/routes
      (ring/redirect-trailing-slash-handler)
      (ring/create-default-handler)))))

(defn ring-router
  [config]
  (map->RingRouter config))

(defrecord RoutesHook [routes]
  RouteProvider
  (routes [_] routes))

(defn routes-hook
  [config]
  (map->RoutesHook config))
