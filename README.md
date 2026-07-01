# cloud-itonami-0810

Open Business Blueprint for **ISIC Rev.5 0810**: dimension-stone quarrying and aggregate supply for local construction.

This repository designs a forkable OSS business for community quarry and stone supply:
run by a qualified operator so a community keeps its own operating records
instead of renting a closed SaaS.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here an extraction robot performs drilling, cutting and loadout at the quarry face under an actor that proposes
actions and an independent **Quarry Governor** that gates them. The governor never
dispatches hardware itself; `:high`/`:safety-critical` actions (such as
operating at a quarry face, near personnel or near blast zones) require human sign-off.

## Core Contract

```text
intake + identity + identity records
        |
        v
Advisor -> Quarry Governor -> proceed, hold, or human approval
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, suppress
an operating record, or disclose sensitive data without governor approval and
audit evidence.

## Capability layer

Resolves via [`kotoba-lang/industry`](https://github.com/kotoba-lang/industry)
(ISIC `0810`). Required capabilities:

- `:robotics`
- `:identity`
- `:forms`
- `:dmn`
- `:bpmn`
- `:audit-ledger`
- `:telemetry`

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
