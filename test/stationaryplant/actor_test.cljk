(ns stationaryplant.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [stationaryplant.actor :as actor]
            [stationaryplant.store :as store]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-client! st {:client-id "client-1" :name "Kobo Plant Ops"})
    (store/register-plant! st {:plant-id "P-1" :client-id "client-1"
                               :name "boiler-3"
                               :min-safe-pressure-kpa 100
                               :max-safe-pressure-kpa 500
                               :maintenance-due-hours 2000})
    st))

(deftest commits-an-in-envelope-in-maintenance-cycle
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :approve-monitoring-cycle :stake :low
                 :plant-id "P-1" :pressure-kpa 300 :operating-hours-since-maintenance 1000}
        result (actor/run-request! graph request {} "thread-1")]
    (is (= :done (:status result)))
    (is (some? (get-in result [:state :record])))
    (is (= 1 (count (store/records-of st "client-1"))))))

(deftest holds-an-overdue-maintenance-cycle
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :approve-monitoring-cycle :stake :low
                 :plant-id "P-1" :pressure-kpa 300 :operating-hours-since-maintenance 5000}
        result (actor/run-request! graph request {} "thread-2")]
    (is (= :hold (:disposition (:state result))))
    (is (empty? (store/records-of st "client-1")))))

(deftest interrupts-then-approves-startup-shutdown-on-human-approval
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :approve-startup-shutdown-sequence :stake :low
                 :plant-id "P-1"}
        interrupted (actor/run-request! graph request {} "thread-3")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "client-1")))
    (let [resumed (actor/approve! graph "thread-3")]
      (is (= :done (:status resumed)))
      (is (= 1 (count (store/records-of st "client-1")))))))
