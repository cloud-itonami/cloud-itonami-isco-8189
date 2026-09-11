(ns stationaryplant.advisor
  "PlantOperationsAdvisor — the advisor named in this repository's
  README, proposing a plant operation (approve a monitoring cycle,
  approve pressurized-system proximity, approve startup/shutdown
  sequence) from a production order, operating procedure and safety
  envelope. Swappable mock/llm; the advisor ONLY proposes —
  `stationaryplant.governor` checks the pressure envelope and
  maintenance-due ceiling independently and always escalates
  pressurized-proximity/startup-shutdown decisions. Modeled on
  cloud-itonami-isco-4311's advisor.

  A proposal: {:op :approve-monitoring-cycle|:approve-pressurized-system-proximity|:approve-startup-shutdown-sequence
               :effect :propose :plant-id str :pressure-kpa number
               :operating-hours-since-maintenance number :stake kw
               :confidence n :rationale str}"
  ;; clojure.edn, not clojure.core/read-string: this parses untrusted
  ;; advisor output, and the core reader executes #=(...) at read time.
  (:require [clojure.edn :as edn]))

(defprotocol Advisor
  (-advise [advisor store request] "request -> proposal map"))

(defn- infer [_store {:keys [op stake plant-id pressure-kpa operating-hours-since-maintenance] :as request}]
  {:op op
   :effect :propose
   :plant-id plant-id
   :pressure-kpa pressure-kpa
   :operating-hours-since-maintenance operating-hours-since-maintenance
   :stake (or stake :low)
   :confidence (case (or stake :low) :high 0.7 :medium 0.85 :low 0.95)
   :rationale (str "proposed " (name op) " for client " (:client-id request))})

(defn mock-advisor []
  (reify Advisor
    (-advise [_ store request] (infer store request))))

(def ^:private system-prompt
  "You are a stationary-plant-operations advisor. Given a request,
   propose an :op, the :plant-id, :pressure-kpa and
   :operating-hours-since-maintenance, an honest :confidence and a
   :stake. Never call an out-of-envelope pressure or an overdue-
   maintenance cycle conforming — the governor checks both against
   the registered plant record. Pressurized-proximity and startup/
   shutdown decisions always require human sign-off regardless of
   confidence.")

(defn- parse-proposal [content]
  (try
    (let [p (edn/read-string content)]
      (if (map? p)
        (assoc p :effect :propose)
        {:op :unknown :effect :propose :confidence 0.0 :stake :high
         :rationale "unparseable LLM response"}))
    (catch #?(:clj Exception :cljs js/Error) _
      {:op :unknown :effect :propose :confidence 0.0 :stake :high
       :rationale "LLM response parse failure"})))

(defn llm-advisor
  [chat-model model-generate-fn gen-opts]
  (reify Advisor
    (-advise [_ _store request]
      (let [msgs [{:role :system :content system-prompt}
                  {:role :user :content (str "operation request: " (pr-str request))}]
            resp (model-generate-fn chat-model msgs gen-opts)]
        (parse-proposal (:content resp))))))
