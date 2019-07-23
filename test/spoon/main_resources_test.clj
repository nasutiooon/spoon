(ns spoon.main-resources-test
  (:require
   [clojure.test :refer [deftest testing is use-fixtures]]
   [gilmour.dev.test :as g.test]
   [ring.util.http-predicates :as pred]
   [spoon.edge.db :as eg.db]
   [spoon.test.components.spoon-client.request :as t.c.client.req]
   [spoon.test.fixtures :as t.fxt]))

(use-fixtures :once t.fxt/with-system)

(defn- spoon-client
  []
  (:spoon-client (g.test/get-system)))

(defn- rand-str
  [len]
  (apply str (take len (repeatedly #(char (+ (rand 26) 65))))))

(defn- medical-checkup-req
  [option]
  (t.c.client.req/medical-checkup (spoon-client) option))

(defn- register-user-req
  [option]
  (t.c.client.req/register-user (spoon-client) option))

(defn- request-reset-token-req
  [username option]
  (t.c.client.req/request-reset-token (spoon-client) username option))

(deftest medical-checkup
  (testing "check if we're able to reach an endpoint"
    (is (pred/ok? (medical-checkup-req {})))))

(deftest request-reset-token
  (testing "exchange app token for reset token"
    (let [username       (rand-str 15)
          password       (rand-str 15)
          other-username (rand-str 15)
          form-params    {:user {:username username :password password}}
          _              (register-user-req {:form-params form-params})
          f1             (partial request-reset-token-req other-username)
          f2             (partial request-reset-token-req username)]
      (is (pred/unauthorized? (f1 {})))
      (is (pred/unauthorized? (f2 {})))
      (let [app-auth {:username username}]
        (is (pred/forbidden? (f1 {:app-auth app-auth})))
        (is (pred/ok? (f2 {:app-auth app-auth})))))))
