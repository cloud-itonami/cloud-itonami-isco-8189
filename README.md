# cloud-itonami-isco-8189

Open Occupation Blueprint for **ISCO-08 8189**: Stationary Plant and Machine Operators Not Elsewhere Classified.

**Maturity: `:implemented`** — PlantOperationsAdvisor ⊣
StationaryPlantGovernor as a langgraph StateGraph
(`intake → advise → govern → decide → commit/hold`, human-approval
interrupt), modeled on cloud-itonami-isco-4311's bookkeeping actor.
14 tests / 30 assertions green. The governor never dispatches
hardware — it only gates what the plant-monitoring robot below may
execute.

The monitoring-cycle HARD invariants — interval containment and
arithmetic, not a scheduling inconvenience:

1. **Pressure envelope** — the measured pressure must fall inside the
   registered safety-envelope band.
2. **Maintenance-due ceiling** — operating hours since last
   maintenance must not exceed the registered ceiling (a mechanical
   risk, not a scheduling inconvenience).

`:approve-pressurized-system-proximity` and
`:approve-startup-shutdown-sequence` **always** escalate to human
sign-off regardless of confidence, per this repo's Trust Controls
(business-model.md).

This repository designs a forkable OSS business for an independent stationary plant operator: a plant-monitoring robot performs gauge reading and sampling near operating equipment under a governor-gated actor, so the operator keeps their own process and safety records instead of renting a closed plant-control SaaS.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a plant-monitoring robot performs gauge reading, temperature sensing and sample collection near operating equipment under an actor that proposes
actions and an independent **Stationary Plant Governor** that gates them. The governor never
dispatches hardware itself; `:high`/`:safety-critical` actions (such as
operating near pressurized systems, or during startup/shutdown sequences) require human sign-off.

A live sample of the operator console (robotics safety console, shared template) is rendered in [docs/samples/operator-console.html](docs/samples/operator-console.html) — pure-data HTML output of `kotoba.robotics.ui`.

## Core Contract

```text
production order + operating procedure + safety envelope
        |
        v
Plant Operations Advisor -> Stationary Plant Governor -> operate/monitor, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, suppress
an operating record, or disclose sensitive data without governor approval and
audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `8189`). Required capabilities:

- :robotics
- :telemetry
- :dmn
- :bpmn
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
