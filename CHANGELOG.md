# Changelog

All notable changes to SpendWise are documented here.

## [Unreleased]

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
