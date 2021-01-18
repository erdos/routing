(ns erdos.routing-test
  (:require [clojure.test :refer :all]
            [erdos.routing :refer :all]))

(def test-routes {})

(defn- gethandler [m uri] (get-handler test-routes {:uri uri :request-method m}))

(defreq test-routes * "/" :root-any)
(defreq test-routes POST "/" :root-post)
(defreq test-routes GET "/" :root-get)

(deftest default-routing
  (testing "Can distinguish GET and POST requests"
    (is (= :root-post (:route/handler (gethandler :post "/"))))
    (is (= :root-get (:route/handler (gethandler :get "/"))))
    (is (= :root-any (:route/handler (gethandler :put "/"))))))

(defreq test-routes POST "/api/policy/v1/policies/:ref" :policy/manage)
(defreq test-routes GET "/api/policy/v1/policies/:ref" :policy/get)
(defreq test-routes POST "/api/policy/v1/policies/:ref/print" :policy/print)
(defreq test-routes * "/api/policy/:url-tail" :policy/proxy)
(defreq test-routes GET "/api/policy/info" :policy/info)

(defreq test-routes GET "/api/policy/:policy-ref/insured/:insured-ref" :policy/insured)

(deftest policy-routing
  (testing "Handling policy routes"
    (is (= :policy/manage (:route/handler (gethandler :post "/api/policy/v1/policies/PO000001"))))
    (is (= :policy/get (:route/handler (gethandler :get "/api/policy/v1/policies/PO000001"))))
    (is (= :policy/print (:route/handler (gethandler :post "/api/policy/v1/policies/001/print"))))
    (is (= :policy/proxy (:route/handler (gethandler :post "/api/policy/info/something/else"))))
    (is (= :policy/insured (:route/handler (gethandler :get "/api/policy/REF001/insured/INS001")))))
  (testing "Same url, different request method"
    (is (= :policy/info (:route/handler (gethandler :get "/api/policy/info"))))
    (is (= :policy/proxy (:route/handler (gethandler :post "/api/policy/info"))))))

(deftest test-params
  (testing "multiple params"
    (is (= {:route/params {:policy-ref "PO001" :insured-ref "INS001"}
            :route/pattern "/api/policy/:policy-ref/insured/:insured-ref"
            :route/meta    nil}
           (select-keys (gethandler :get "/api/policy/PO001/insured/INS001")
                        [:route/params :route/pattern :route/meta])))))

(defreq test-routes GET "/test-meta" ^:proba-meta (fn [_] 1))

(deftest test-manual-meta
  (is (= {:proba-meta true} (:route/meta (gethandler :get "/test-meta")))))

(deftest test-handle-routes
  (defreq GET "/test-ring" (fn [req] "OK"))
  (is (= "OK" (handle-routes {:request-method :get :uri "/test-ring"}))))