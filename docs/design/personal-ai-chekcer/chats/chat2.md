# Settings & AI Bridge Simplification

_Started 2026-05-19 18:10 UTC_

---

## User

i'm ok with onboarding part and mostly with dashboard one. 

But settings and AI bridge page i think are wrong. We probably can just simplify as i don't we can retireve all this info

probably some buttons to set up a skill on claude , codex , cursor etc. and a way to explain how to install the cli is enough for initial style

## Assistant

_[tool: list_files]_

_[tool: read_file]_

_[tool: read_file]_

_[tool: read_file]_

_[tool: read_file]_

_[tool: read_file]_

_[tool: write_file]_

_[tool: write_file]_

Got it. I'll simplify both screens — strip out the over-claimed model picker / live sessions / privacy redaction and replace with what we can actually deliver: install the CLI, then one-tap "Set up skill" buttons for Claude Code, Codex, and Cursor. Settings becomes a slim page with only things we genuinely have (account, linked banks, which agents are connected, data export).

Let me note the choice out loud: **keeping the existing Ledger aesthetic (warm minimal, Inter + Instrument Serif)**, no new fonts/colors. Avoiding any attempt to reproduce the Claude/Codex/Cursor logos — I'll use plain letter-marks so we don't reproduce branded UI.

_[tool: grep]_

_[tool: done]_

Simplified both screens.

**AI bridge** is now just three steps: install the CLI (with Brew / npm / Linux tabs), set up a skill in Claude Code / Codex / Cursor (one card each with a "Set up skill" button and the config path that gets written), and a sample prompt to try. Cut the fake live sessions, fake tool list, and routing chain.

**Settings** is now Account → Bank accounts → AI bridge → Data. No model picker, no fallback chain, no redaction toggles — just things we'd actually have (email, plan, weekly digest toggle, linked banks list, which agents are connected, export / delete).

Agent marks are plain letter tiles (C / O / K) — didn't reproduce any real logos.

_[tool: fork_verifier_agent]_

Done — let me know if either screen still feels off or if you want me to push further (e.g. drop the "AI bridge" section from Settings since it duplicates the dedicated page, or collapse Settings to a single column).

## User

cool! now i like the aurora style so let's remove the rest.

But not convinced with all the gradients , can we make the color more solid?

## Assistant

_[tool: read_file]_

_[tool: write_file]_

I'll keep only Aurora and solidify everything — no gradient backdrops, no translucent cards.Now simplify app.jsx — remove theme switcher entirely, remove the "00 themes" compare row.

_[tool: write_file]_

_[tool: grep]_

_[tool: grep]_

_[tool: grep]_

_[tool: done]_

Done. Now Aurora-only, solid surfaces:

- **One theme**: dropped Ledger / Editorial / Terminal entirely. Removed the floating theme switcher and the "00 Themes" compare row.
- **Solid colors**: no more radial-gradient backdrop on artboards. Cards are now solid `#141a30` (one lift above the `#0d1224` background) instead of translucent white-on-blur. No `backdrop-filter` on cards. Pills lift onto solid surfaces too.
- **Tweaks**: simplified to a single Light/Dark toggle.
- Kept the Instrument Serif accent for `.serif` headings since it played nicely against Geist; everything else is Geist / Geist Mono.

## User

why did you changed typgraphy?

## Assistant

_[tool: str_replace_edit]_

_[tool: str_replace_edit]_

My mistake — I swapped in Instrument Serif for headings when the original Aurora used Geist for everything (sans + "serif" both Geist). Reverting.Reverted — back to Geist for everything (the original Aurora setup). The `.serif` class now just renders Geist at a slightly tighter letter-spacing, matching what the headings looked like before.

## User

Apply comment

## Assistant

_[tool: grep]_

_[tool: read_file]_

_[tool: str_replace_edit]_

Removed.

One thing to flag: there's also a second dashboard artboard labeled **"Overview · AI panel open"** that shows a full AI chat thread inside the app. If we don't open threads in-app, that whole variant is misleading too — want me to remove it (and drop the AI panel code) along with the button? Or keep it for some other reason?

## User

the Overview · AI panel open , does not make sene any more, remove it

## Assistant

_[tool: read_file]_

_[tool: read_file]_

_[tool: str_replace_edit]_

_[tool: str_replace_edit]_

_[tool: done]_

Removed the "Overview · AI panel open" artboard and stripped the `AiPanel` / `ChatBubble` components and the `withAiPanel` prop from the Dashboard. The Dashboard section now has just one artboard.

I left the AI-flavored bits that still make sense on the dashboard itself (the "Insights from Claude" card, the `✶ ai-categorized` tags on transactions) — these are passive outputs the bridge produces, not threads inside the app. Let me know if you want those gone too.

