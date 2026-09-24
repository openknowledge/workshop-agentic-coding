---
description: Compact the current conversation into a handoff document for another agent to pick up.
---

Write a handoff document summarising the current conversation so a fresh agent can continue the work. Save to `docs/handoffs/<slug>.md` in this repository.

Include a "Recommended next step" section describing the concrete next action, referencing this repo's own commands/docs where relevant.

Do not duplicate content already captured in other artifacts (specs, plans, ADRs, issues, commits, diffs). Reference them by path or URL instead.

Redact any sensitive information, such as API keys, passwords, or personally identifiable information.

What the next session will be used for (may be empty): $ARGUMENTS

If this is given, tailor the doc accordingly.
