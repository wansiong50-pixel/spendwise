# Changelog

All notable changes to SpendWise are documented here.

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
