(ns stationaryplant.store
  "SSoT for the ISCO-08 8189 independent stationary plant operations
  actor (itonami actor pattern, ADR-2607011000 / CLAUDE.md Actors
  section; README's 'Robotics premise' — a plant-monitoring robot
  performs gauge reading, temperature sensing and sample collection
  under this advisor/governor pair, which never dispatches hardware
  itself). Modeled on cloud-itonami-isco-4311's bookkeeping.store.

  Domain:

    client — a registered organization (:client-id, :name)
    plant  — a registered stationary plant {:plant-id :client-id
             :name :min-safe-pressure-kpa number
             :max-safe-pressure-kpa number
             :maintenance-due-hours number}.
             `:min-safe-pressure-kpa`/`:max-safe-pressure-kpa` is the
             registered safety-envelope band a proposed monitoring
             cycle's measured pressure must fall inside;
             `:maintenance-due-hours` is the registered ceiling a
             proposed cycle's operating hours since last maintenance
             must not exceed — operating past the maintenance-due
             hour count is a mechanical risk, not a scheduling
             inconvenience.
    record — a committed operating record (approved monitoring cycle)
             — written ONLY via commit-record!.
    ledger — append-only audit trail, commit or hold."
  )

(defprotocol Store
  (client [s client-id])
  (plant [s plant-id])
  (records-of [s client-id])
  (ledger [s])
  (register-client! [s client])
  (register-plant! [s p])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (client [_ client-id] (get-in @a [:clients client-id]))
  (plant [_ plant-id] (get-in @a [:plants plant-id]))
  (records-of [_ client-id] (filter #(= client-id (:client-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-client! [s client]
    (swap! a assoc-in [:clients (:client-id client)] client) s)
  (register-plant! [s p]
    (swap! a assoc-in [:plants (:plant-id p)] p) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:clients {} :plants {} :records [] :ledger []}
                                   seed)))))
