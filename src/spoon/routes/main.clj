(ns spoon.routes.main
  (:require
   [ring.util.http-response :as res]
   [ring.util.http-status :as st]
   [spoon.components.router :as c.router]
   [spoon.edge.signer :as eg.signer]
   [spoon.middleware.auth :as m.auth]))

(defn- medical-checkup
  [_]
  (res/ok {:app {:name     "spoon"
                 :healthy? true}}))

(defn- request-reset-token
  [{:keys [passport identity]}]
  (let [token (eg.signer/reset-token passport identity)]
    (res/ok {:token {:reset-token token}})))

(defn main-routes-hook
  []
  (c.router/routes-hook
   {:routes
    [["/api/health"
      {:name ::health
       :get  {:responses {st/ok {:body {:app {:name     string?
                                              :healthy? boolean?}}}}
              :handler   medical-checkup}}]
     ["/api/token/:username/reset"
      {:name       ::app
       :parameters {:path {:username string?}}
       :post       {:responses {st/ok {:body {:token {:reset-token string?}}}}
                    :handler   request-reset-token}
       :middleware [m.auth/app-auth-middleware
                    m.auth/auth-middleware
                    m.auth/creator-middleware]}]]}))
