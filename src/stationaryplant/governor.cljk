(ns stationaryplant.governor
  "StationaryPlantGovernor — the independent safety/traceability
  layer named in this repository's README/business-model.md, gating
  the robot-dispensed physical work (gauge reading, temperature
  sensing, sample collection) an advisor may propose. The governor
  never dispatches hardware itself. Modeled on
  cloud-itonami-isco-4311's bookkeeping.governor. Cycle twist: a
  proposed monitoring cycle's measured pressure must fall inside the
  registered safety-envelope band, and operating hours since last
  maintenance is arithmetic comparison against the registered
  maintenance-due ceiling — operating past it is a mechanical risk,
  not a scheduling inconvenience.

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. client provenance — the organization must be registered.
    2. no-actuation      — proposal :effect must be :propose (the
                           governor never dispatches hardware; it only
                           gates what the robot may execute).
    3. plant basis          — a monitoring approval must cite a
                           REGISTERED plant belonging to this client.
    4. pressure envelope    — the proposed measured pressure must
                           fall inside the plant's registered
                           [:min-safe-pressure-kpa,
                           :max-safe-pressure-kpa] band.
    5. maintenance-due ceiling — the proposed operating hours since
                           last maintenance must not exceed the
                           plant's registered
                           :maintenance-due-hours (a mechanical risk,
                           not a scheduling inconvenience).
  ESCALATION invariants (:escalate? true, ALWAYS human sign-off per
  business-model.md's Trust Controls — these are :high/
  :safety-critical regardless of confidence):
    6. :op :approve-pressurized-system-proximity (no robot dispatch
                           near pressurized systems without the
                           governor gate).
    7. :op :approve-startup-shutdown-sequence (startup/shutdown
                           sequences require human sign-off).
    8. low confidence (< `confidence-floor`)."
  (:require [stationaryplant.store :as store]))

(def confidence-floor 0.6)

(def ^:private always-escalate-ops #{:approve-pressurized-system-proximity
                                     :approve-startup-shutdown-sequence})

(defn- hard-violations [{:keys [request proposal]} client-record p]
  (let [{:keys [op pressure-kpa operating-hours-since-maintenance]} proposal
        cycle? (= :approve-monitoring-cycle op)]
    (cond-> []
      (nil? client-record)
      (conj {:rule :no-client :detail "未登録 client"})

      (not= :propose (:effect proposal))
      (conj {:rule :no-actuation :detail "effect は :propose のみ許可（governor はハードウェアを直接起動しない）"})

      (and cycle? (nil? p))
      (conj {:rule :unknown-plant :detail "未登録 plant への監視承認は不可"})

      (and cycle? p (not= (:client-id p) (:client-id request)))
      (conj {:rule :plant-wrong-client :detail "plant が別 client のもの"})

      (and cycle? p (number? pressure-kpa)
           (or (< pressure-kpa (:min-safe-pressure-kpa p))
               (> pressure-kpa (:max-safe-pressure-kpa p))))
      (conj {:rule :pressure-out-of-envelope
             :detail (str "測定圧力 " pressure-kpa "kPa が登録済み安全域 ["
                          (:min-safe-pressure-kpa p) ", " (:max-safe-pressure-kpa p)
                          "]kPa の外")})

      (and cycle? p (number? operating-hours-since-maintenance)
           (> operating-hours-since-maintenance (:maintenance-due-hours p)))
      (conj {:rule :maintenance-overdue
             :detail (str "保守後稼働時間 " operating-hours-since-maintenance
                          "h > 登録済み保守期限 " (:maintenance-due-hours p)
                          "h（期限超過稼働は機械的リスクであってスケジュールの都合ではない）")}))))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `stationaryplant.store/Store`. Pure — never
  mutates the store, never dispatches the robot."
  [request context proposal store]
  (let [client-record (store/client store (:client-id request))
        p (some->> (:plant-id proposal) (store/plant store))
        hard (hard-violations {:request request :proposal proposal}
                              client-record p)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        always-risky? (contains? always-escalate-ops (:op proposal))]
    {:ok? (and (not hard?) (not low?) (not always-risky?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? always-risky?))}))
