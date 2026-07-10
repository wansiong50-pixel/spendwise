# Product

## Register

product

## Users

A single individual tracking their own day-to-day spending on Android. Specifically the project owner (based in Malaysia, currency hardcoded to MYR / RM, time zone `Asia/Kuala_Lumpur`). The app runs entirely on-device: no accounts, no cloud, no sync, no telemetry. The user opens it briefly and often, usually right after a transaction or at the end of a day, to log an expense or scan how a month is shaping up. The "job" is **see where my money is going, with as little ceremony as possible** — not collaborate, not budget rigorously, not invest.

## Product Purpose

SpendWise is a local-first personal expense tracker. Every expense lives in a Room database on the user's phone. The product exists because cloud-backed budget apps (Mint-style) are heavier than the user wants and trade away privacy for sync and collaboration the user doesn't need. Success looks like: the user can add a real-world expense in under ten seconds, and at any moment can answer "how much have I spent this month, broken down by category" in under three seconds of looking at the app.

Three surfaces:

- **Dashboard** — current month at a glance (total, hero bar chart, category breakdown, recent transactions).
- **Transactions** — searchable, dated list of every expense for editing or deletion.
- **Breakdown** — period-aware analytics (today / month / year), with a donut chart, category totals, and a time-series trend.

Plus a modal **Add / Edit Expense** flow with category management.

## Brand Personality

**Editorial. Precise. Calm.**

- *Editorial* — typography carries hierarchy more than card chrome does; the numbers are the typography. Considered whitespace, not packed dashboards.
- *Precise* — tight grids, deliberate alignment, money formatting that doesn't drift. Decimals are not decoration; they are accurate or they are wrong.
- *Calm* — low stimulation. The app deals with money, which is already an emotionally loaded topic; the interface should not amplify that. No anxiety colors, no urgent micro-animations on routine taps, no "you've spent X% of your budget!" interruptions.

The existing visual system reflects this: deep plum (`#270F2E`) anchors typography in light mode, vivid coral (`#FF987A`) appears as accent (badges, FAB, primary CTA) but never as wall-to-wall surface. Light/dark are treated as full siblings — not "dark mode is the second-class theme."

## Anti-references

What SpendWise must **not** look like:

- **Navy + gold trad-banking** (Chase, HSBC, Capital One). Conservative, corporate, cold. Personal finance ≠ institutional finance. Reject the implication that the user's money belongs to a bank.
- By extension: any palette that signals "your money is being managed by serious adults in suits" — navy, slate gray, gold rule lines, beveled buttons, dense tables.

What this means in practice: deep plum is a typographic color, not a banking color. Coral is warmth, not a "buy now" CTA color. Charts are explanatory, not surveillance dashboards.

## Design Principles

1. **The numbers are the protagonist.** Hero typography goes to the totals. Chrome, icons, decoration recede. If a card is competing with the number it contains, the card is wrong.
2. **Animations earn their place.** Daily interactions (tab swap, list scroll, the FAB tap that opens the add-expense modal) stay invisible or near-invisible; rare moments (first dashboard load, donut sweep, modal entry) can breathe. Never animate something the user sees hundreds of times a day.
3. **Local-first means quiet.** No cloud-sync nags, no "create an account" prompts, no "share your insights." The app is a private tool, and the UI should never imply otherwise.
4. **Plum and coral, not navy and gold.** This is a personal-finance app, not a bank. Warmth is intentional and is the differentiator from every other finance app on the store.
5. **Light and dark are siblings.** Coral stays vivid (`#FF987A`) in both. Plum becomes lavender (`#F1D9F7`) in dark mode for typography only — the brand "primary" token (`AppSWPrimary`) flips correctly so this is invisible to feature code.

## Accessibility & Inclusion

- **WCAG 2.1 AA** for body text and primary numbers in both light and dark modes. Contrast checked against the surface tokens, not against pure white/black.
- **prefers-reduced-motion** respected via `Settings.Global.ANIMATOR_DURATION_SCALE`. When the user has animations off, transforms (scale, translate, slide) collapse to instant; opacity / color crossfades remain because they aid comprehension. Reduced motion ≠ no motion.
- **Touch targets** at 48dp minimum for primary actions (FAB, save, transaction rows in edit affordances).
- **No emoji in UI strings** (sticky user preference). Pure text + Material icons only.
