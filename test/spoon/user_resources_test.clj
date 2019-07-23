(ns spoon.user-resources-test
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

(defn- db
  []
  (:db (g.test/get-system)))

(defn- rand-str
  [len]
  (apply str (take len (repeatedly #(char (+ (rand 26) 65))))))

(defn- register-user-req
  [option]
  (t.c.client.req/register-user (spoon-client) option))

(defn- request-auth-token-req
  [username option]
  (t.c.client.req/request-auth-token (spoon-client) username option))

(defn- delete-user-req
  [username option]
  (t.c.client.req/delete-user (spoon-client) username option))

(defn- update-user-password-req
  [username option]
  (t.c.client.req/update-user-password (spoon-client) username option))

(defn- reset-user-password-req
  [username option]
  (t.c.client.req/reset-user-password (spoon-client) username option))

(deftest register-user
  (testing "able to create a user"
    (let [username (rand-str 15)
          password (rand-str 15)
          f1       register-user-req
          f2       (partial request-auth-token-req username)]
      (is (pred/bad-request? (f1 {})))
      (is (pred/bad-request? (f1 {:form-params {}})))
      (let [form-params {:user {:username username
                                :password password}}]
        (is (pred/ok? (f1 {:form-params form-params})))
        (is (pred/conflict? (f1 {:form-params form-params}))))
      (is (pred/ok? (f2 {:basic-auth [username password]}))))))

(deftest request-auth-token
  (testing "fetching an auth token"
    (let [username       (rand-str 15)
          password       (rand-str 15)
          other-username (rand-str 15)
          form-params    {:user {:username username
                                 :password password}}
          _              (register-user-req {:form-params form-params})
          f1             (partial request-auth-token-req other-username)
          f2             (partial request-auth-token-req username)]
      (is (pred/unauthorized? (f1 {})))
      (is (pred/unauthorized? (f2 {})))
      (let [basic-auth [username password]]
        (is (pred/forbidden? (f1 {:basic-auth basic-auth})))
        (is (pred/ok? (f2 {:basic-auth basic-auth})))))))

(deftest delete-user
  (testing "erase user with matching username"
    (let [username       (rand-str 15)
          password       (rand-str 15)
          other-username (rand-str 15)
          form-params    {:user {:username username
                                 :password password}}
          _              (register-user-req {:form-params form-params})
          f1             (partial delete-user-req other-username)
          f2             (partial delete-user-req username)
          f3             (partial request-auth-token-req username)]
      (is (pred/unauthorized? (f1 {})))
      (is (pred/unauthorized? (f2 {})))
      (let [basic-auth [username password]]
        (is (pred/forbidden? (f1 {:basic-auth basic-auth})))
        (is (pred/no-content? (f2 {:basic-auth basic-auth})))
        (is (pred/unauthorized? (f3 {:basic-auth basic-auth})))))))


(deftest update-user-password
  (testing "able to update a user's password"
    (let [username       (rand-str 15)
          password       (rand-str 15)
          other-username (rand-str 15)
          form-params    {:user {:username username
                                 :password password}}
          _              (register-user-req {:form-params form-params})
          new-password   (rand-str 15)
          f1             (partial update-user-password-req other-username)
          f2             (partial update-user-password-req username)
          f3             (partial request-auth-token-req username)]
      (is (pred/bad-request? (f1 {})))
      (is (pred/bad-request? (f2 {})))
      (let [form-params {:user {:new-password new-password
                                :old-password password}}]
        (is (pred/unauthorized? (f1 {:form-params form-params})))
        (is (pred/unauthorized? (f2 {:form-params form-params})))
        (let [basic-auth [username password]]
          (is (pred/forbidden? (f1 {:form-params form-params
                                    :basic-auth  basic-auth})))
          (let [bad-form-params {:user {:new-password new-password
                                        :old-password "incorrectoldpassword"}}]
            (is (pred/forbidden? (f2 {:form-params bad-form-params
                                      :basic-auth  basic-auth})))
            (is (pred/ok? (f2 {:form-params form-params
                               :basic-auth  basic-auth})))
            (is (pred/unauthorized? (f3 {:basic-auth basic-auth})))
            (is (pred/ok? (f3 {:basic-auth [username new-password]})))))))))

(deftest reset-user-password
  (testing "able to reset a user's password"
    (let [username       (rand-str 15)
          password       (rand-str 15)
          other-username (rand-str 15)
          form-params    {:user {:username username
                                 :password password}}
          _              (register-user-req {:form-params form-params})
          new-password   (rand-str 15)
          f1             (partial reset-user-password-req other-username)
          f2             (partial reset-user-password-req username)
          f3             (partial request-auth-token-req username)]
      (is (pred/bad-request? (f1 {})))
      (let [form-params {:user {:new-password new-password}}]
        (is (pred/unauthorized? (f1 {:form-params form-params})))
        (is (pred/unauthorized? (f2 {:form-params form-params})))
        (let [reset-auth (eg.db/find-user-by-username (db) username)]
          (is (pred/forbidden? (f1 {:form-params form-params
                                    :reset-auth  reset-auth})))
          (is (pred/ok? (f2 {:form-params form-params
                             :reset-auth  reset-auth})))
          (is (pred/unauthorized? (f3 {:basic-auth [username password]})))
          (is (pred/ok? (f3 {:basic-auth [username new-password]}))))))))
