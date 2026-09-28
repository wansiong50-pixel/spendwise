# Changelog

All notable changes to SpendWise are documented here.

## [1.8.2] - 2026-09-28

### Fixed
- Pulling the add/edit form up past its end no longer opens a see-through
  gap onto the screen behind the card. The card now continues below the
  form in its own colour and springs back when you let go.

## [1.8.1] - 2026-09-28

### Fixed
- The keyboard no longer leaves a gap. Typing in the add/edit card or a
  sheet used to lift it far above the keyboard, with the app showing
  through in between; it now sits right on top of the keyboard.
- The field you're typing in stays visible above the Save button instead
  of sliding behind it.
- Opening a sheet, the calendar or the add/edit card closes the keyboard,
  so the calendar's month arrows are no longer cut off and typing can't
  land in a hidden field (such as Activity's search behind the add card).
- Amounts no longer show a stray blue cursor handle after you tap them.

## [1.8] - 2026-09-27

### Changed
- **A new look: the Botanical redesign.** Every screen has been redrawn.
  Home opens on a painted flower with your total balance, then this month's
  income, spending and budgets, your top spending categories, the spending
  heatmap and recent activity. Activity, Insights, Accounts, Categories,
  Recurring and Settings sit on midnight navy with white cards. Categories
  and accounts have illustrated icons, and each account card has a landscape.
- A new add/edit screen. Amounts fill in from the right (typing 1, 2, 3 reads
  RM 0.01, 0.12, 1.23). Dates come from an illustrated calendar, recent
  merchants are one tap away, and closing with unsaved changes asks before
  discarding them. Transfers can now be edited and shared like any other
  entry.
- **Fluid motion.** Everything moves on springs that keep their speed when
  interrupted, so nothing jumps or restarts. Sheets and the calendar rise from
  the bottom and can be dragged down or flicked away. The add/edit card opens
  over the app, which recedes behind it, and pulls down to close. Pages slide
  in over the previous one, and on Android 14+ the back gesture drags them
  away under your finger. Lists bounce at their edges, and Home's painting
  stretches when pulled down. Headline amounts roll their digits, typed
  amounts slide along as you type, and segments, switches, charts and
  progress bars glide. Android's "Remove animations" setting still turns
  motion off.
- Insights has Month and Year views, income and expense cards compared with
  the previous period, and a 12-month cash-flow chart.
- Settings is now a full page, and the app has one appearance: the light/dark
  toggle is gone.

### Added
- Activity filters: account, category and a custom date range (it can span
  several months), shown as removable chips. The Expense and Income tabs
  show a six-month trend.
- Restoring a backup now shows what the file holds (entries, transfers,
  accounts, categories, recurring rules) before anything is replaced.
- CSV export for a month or a whole year from Insights, with transfers listed
  separately.
- Built-in categories can be renamed, given a new icon, and given a monthly
  budget. They still can't be deleted.
- Accounts can have a negative balance, such as an amount already owed on a
  credit card.

### Removed
- The month picker's quick ranges (year to date, all time). Use Insights'
  Year view or an Activity custom range instead.

## [1.7] - 2026-07-22

### Changed
- Deleting an expense, income entry, or account transfer now requires explicit
  confirmation. The dialog identifies the selected entry by name, amount,
  category/date or account route before anything is removed.
- Cancel, the system Back action, and tapping outside the confirmation all
  preserve the entry and return to its detail sheet. The permanent Room delete
  runs only after the destructive **Delete** action is confirmed.

## [1.6] — 2026-07-12

### Fixed
- Money figures no longer wrap onto two lines on devices with large font
  sizes — amounts now shrink slightly to fit their space instead. Hardened
  across every surface: the income/expense summary cards, income-source
  rows, day headers, budget lines, insights categories, the transaction
  history strip, and transfer/recurring rows.
- Fixed the reverse case too: a very large figure (RM millions) no longer
  starves the caption beside it — "income · 8 entries" and "You're keeping
  X%" stay on one line while the amount shrinks to share the row.

## [1.5] — 2026-07-12

### Added
- **Transfers between accounts** — the add sheet gains a third mode next to
  Expense and Income. A transfer moves money from one account to another
  (bank withdrawal, e-wallet top-up, credit-card bill payment) in a single
  atomic record: both balances update, but spending and income statistics
  are untouched — moving your own money between pockets is not spending.
  Transfers appear in the Activity timeline as neutral "From → To" rows,
  open into a detail sheet, and can be deleted from there. Included in
  backups; accounts with transfer history can't be archived.

## [1.4] — 2026-07-11

### Added
- **Recurring transactions** — define rules for rent, subscriptions, and
  salary (weekly / monthly / yearly). Due occurrences are logged
  automatically when the app opens; time away is backfilled with correct
  dates (capped at 36 per rule). Month-end anchors survive short months
  (a rule on the 31st fires Feb 28, then returns to Mar 31). Rules can be
  paused, edited, and deleted from Settings → Recurring transactions, and
  are included in backups.

### Fixed (pre-release hardening of recurring rules)
- Editing a rule's schedule (cadence or start date) now applies to upcoming
  occurrences only — it previously replayed the whole history from the
  anchor date, duplicating every already-logged entry.
- Resuming a paused rule now continues from the next future occurrence
  instead of backfilling the entire paused gap.
- The due-rule check now runs inside a single database transaction and also
  fires when the app returns to the foreground and after a backup restore —
  previously concurrent checks could double-log, and a long-resident app
  process would never log newly due rules until fully restarted.
- The recurring form's Save button ignores double-taps (no duplicate rules).
- An account referenced by a recurring rule can no longer be archived.

## [1.3] — 2026-07-10

First open-source release.

### Added
- **Automatic daily backups** — a background job writes a dated JSON backup
  (`spendwise-auto-YYYYMMDD-HHmm.json`) into a folder you choose, once a day.
  Pick a cloud-synced folder (Drive, OneDrive…) and your data leaves the
  phone automatically. The newest 7 auto-backups are kept; older ones are
  pruned. Manual backups are never touched.
- Settings → **Automatic backups** section: on/off toggle, backup folder
  picker, "Back up now", and a last-backup / error status line.

### Changed
- Project is now version-controlled and published on GitHub under the MIT
  license.

## [1.2] and earlier — internal development

Built up the core app: dashboard with spending heatmap, activity list with
month scoping and search, insights (yearly trends), categories with budgets,
multiple accounts with archiving, dark mode, manual JSON backup/restore, and
CSV export.
