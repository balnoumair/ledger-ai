# Design references

This directory holds the design handoffs that inform the product. They are
reference material, not source code — nothing here is built or shipped.

## `personal-ai-chekcer/`

The original handoff bundle exported from Claude Design (`claude.ai/design`).
Start with [`personal-ai-chekcer/README.md`](personal-ai-chekcer/README.md) —
it explains the bundle's structure and tells you to read the chat transcripts
first, since the chats are where the user iterated and landed on the final
visual direction (the Aurora theme: deep navy `#0d1224`, card `#141a30`,
Geist + Geist Mono, solid surfaces with no gradients).

A few caveats worth knowing before you dig in:

- `project/design-canvas.jsx` is **truncated**. The export tar was cut off
  past roughly line 638, so the `PlaidConnect`, `AccountSelect`, `Dashboard`,
  `AiBridge`, and `Settings` component bodies are missing. The visual system
  (CSS tokens, font choices, surface treatment) is still recoverable from the
  chats, the CSS at the top of the file, and `project/app.jsx`. Implementers
  should treat the design as a visual spec, not a layout spec.
- `project/.design-canvas.state.json` references a deleted artboard
  (`Overview · AI panel open`) — that's from an intermediate design iteration
  the user removed; ignore it.
- The original Claude Design share URL was
  `https://api.anthropic.com/v1/design/h/b7nbo9mouThgmt0vRJ2HZg`. It may
  expire; this committed copy is the authoritative reference going forward.
