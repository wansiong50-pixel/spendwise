# Design System: SpendWise v2 — Soft Fintech with Intent

> **Source of truth.** Implements the May 2026 redesign handoff (`SpendWise Redesign.html`). When code disagrees with this document, fix the code; when this document disagrees with the handoff, fix this document. The earlier "minimal warm-cream ledger" direction has been retired in full and should not be reintroduced.

## 1. The direction in one sentence

Soft tinted-lavender canvas, white pebble cards floating on it, exactly **one confident black or violet object per screen**, tabular Plus Jakarta money with a muted "RM" prefix. Moves the product from paper-quiet editorial toward **soft-fintech with intent**.

## 2. Pillars

1. **Pebble surfaces.** r=24 white cards on a lavender wash. No outlines, only a faint vertical violet-tinted drop shadow.
2. **One loud thing.** Each screen has exactly one black or violet object — the FAB, a selected chip, a Save button. Everything else is pastel.
3. **Money is the type.** Plus Jakarta ExtraBold at the screen scale, tabular, -0.02em. The "RM" prefix runs at 55% opacity and 55% of the digit size so it reads as a label, not a digit.
4. **Quiet motion.** 220ms cubic-bezier scale-in modals, cross-fade tab swaps (no slides), reduced motion honored. Press feedback never below 0.95 scale.

## 3. Color tokens

All tokens live in `Theme.kt` as fields on `SpendWiseColors`. The legacy names (`plumPrimary`, `coralContainer`, etc.) are kept so old call sites compile, but they resolve to the v2 hues below. New code should reach for the canonical `sw*` accessors (`SwInk`, `SwViolet`, `SwMint`, `SwHeat3`, etc.).

### Surfaces (light)

| Token | Hex | Role |
|---|---:|---|
| `appBackground` | `#E8E3F4` | Lavender canvas. Wraps the whole app — the only thing visible *between* pebbles. |
| `appSurface` | `#FFFFFF` | White pebble. The dominant card material. |
| `appSurfaceLow` | `#F1EEF7` | Inactive segmented controls, input fills, recessed wells. |
| `appSurfaceContainer` | `#E8E4F2` | Dividers, sheet grabbers, low hairlines. |
| `appSurfaceContainerHigh` | `#D6CBF1` | Strong hairlines, chart baselines, input outlines. |
| `appOnSurface` (`SwInk`) | `#15121F` | Primary text, the "loud thing" fill, FAB ring. |
| `appOnSurfaceVariant` | `#8E8AA0` | Subtitles, date stamps, eyebrow labels. |

### Accents (light)

| Token | Hex | Role |
|---|---:|---|
| `SwViolet` | `#6E5BF2` | FAB fill, selected indicator, primary bar. |
| `SwVioletSoft` | `#C5BCF8` | Hero gradient end, soft selected, Housing category. |
| `SwPeach` | `#F8C99B` | Food category tile. |
| `SwPeachSoft` | `#FCE0BC` | Hero gradient start. |
| `SwMint` | `#9FE3C5` | Income, Groceries, positive semantics. |
| `SwPink` | `#F8C0D6` | Entertainment. |
| `SwSky` | `#B8D5FF` | Transport. |
| `SwButter` | `#FFE08A` | Bills. |
| `SwCoral` | `#F4A39B` | Gifts, low-emphasis red. |
| `SwPos` | `#1FA971` | Income green. Reserved — never used on a surface, only on amounts. |
| `SwNeg` | `#E54B5B` | Expense red. Reserved for actions like Archive/Delete. |

### Heatmap ramp

A five-step lavender ramp (`SwHeat0` → `SwHeat4`) keeps the spending heatmap on-brand and reserves green exclusively for income. Today's date is outlined in ink, not filled — the green channel is busy carrying intensity.

| Step | Light | Dark |
|---|---:|---:|
| `SwHeat0` | `#EDE9F5` | `#1A1825` |
| `SwHeat1` | `#D6CBF1` | `#2E2940` |
| `SwHeat2` | `#B8A6EA` | `#4A3F75` |
| `SwHeat3` | `#8E72E3` | `#6E58B5` |
| `SwHeat4` | `#5B40D6` | `#C5BCF8` |

### Dark mode

Surface roles invert with the same palette logic — `appBackground` becomes `#0F0D17`, `appSurface` becomes `#1A1825`. Accent hues desaturate ~25% (a pure pastel on `#0F0D17` reads as glow). Violet stays the brand anchor; ink flips to `#F4F1FA`.

## 4. Typography

Plus Jakarta Sans (system fallback on Android) at the following scale. Every numeric Text passes `fontFeatureSettings = FinanceNumFeatures` (= `tnum`) so columns of money line up across rows.

| Role | Size | Weight | Tracking | Use |
|---|---:|---:|---:|---|
| Display | 42sp | 800 | -0.5sp | Hero amount on Dashboard, primary account amount. |
| Greeting | 26sp | 700 | -0.3sp | "Here's where your money is." |
| Section / Card title | 15sp | 700 | -0.15sp | "Where you spent", "Recent activity". |
| Row title | 14.5sp | 600 | 0sp | Merchant name, account name. |
| Body | 14sp | 500 | 0sp | Subtitles, descriptions. |
| Secondary body | 12.5sp | 500 | 0sp | Category · date metadata. |
| Chip label | 12.5sp | 700 | 0sp | Filter chips, pill buttons. |
| Eyebrow | 11sp | 700 | 0.8sp | UPPERCASE "TOTAL BALANCE", "MAY 2026", section labels in Add. |

`platformStyle = NoFontPadding` + `lineHeightStyle = TrimBoth` are applied at the type scale level so measured bounds match visible ink — required for any text that shares a `Row(CenterVertically)` with an icon.

## 5. Radii & spacing

| Radius | Use |
|---:|---|
| 10 | Inputs (small) |
| 14 | Chips, sheet grabbers |
| 18 | Buttons, segmented controls, soft tile interiors |
| 24 | Cards (default), transaction-day grouped cards |
| 28-30 | Hero pebble, primary account card |
| 44 | Phone frame |
| 999 (pill) | Bottom nav, FAB, filter chips, segmented toggle |

Spacing scale follows the 4-base system: **4 · 8 · 12 · 16 · 20 · 24 · 32 · 40 · 48**. Card padding sits at **18-22**. Card-to-card gap inside a column is **12-16**. Major section gap is **22-28**.

## 6. Shadow

Exactly one shadow recipe, applied to every floating white surface:

```kotlin
Modifier.shadow(
    elevation = 6.dp,   // see premiumCardShadowElevation()
    shape = RoundedCornerShape(24.dp),
    clip = false,
    ambientColor = Color(0xFF281E50).copy(alpha = 0.25f),
    spotColor   = Color(0xFF281E50).copy(alpha = 0.25f),
)
```

The shadow color is violet-tinted (`#281E50`) so it reads warm on the lavender canvas instead of cold gray-blue. **Never use a border** to define a pebble; use the shadow alone.

## 7. The six screens

### 7.1 Dashboard

1. **Header** — month pill (`May 2026 ▾`) on the left, white-disc icon buttons (search + theme toggle) on the right.
2. **Greeting** — "Good evening, Amir" (muted 14sp) → "Here's where your money is." (26sp 700).
3. **Hero gradient pebble** — 135° peach→violet gradient, two soft caustic discs (top-right white, bottom-right violet), eyebrow "TOTAL BALANCE", ExtraBold amount with muted RM prefix, subtitle "Across N accounts · tap to manage", circular `↗` affordance. Inline `EARNED` / `SPENT` chips inside the pebble (translucent white) collapse what used to be a second screen of stats.
4. **Net cashflow row** — small signed amount in `SwPos` / `SwNeg`, with a quiet "You're keeping X%" stat on the right.
5. **Spending heatmap card** — pebble card, 7-column wall calendar grid shaded with the lavender ramp. Today outlined in ink. Legend (`Less · ■■■■■ · More`) at the bottom.
6. **Top categories card** — pebble card with "Where you spent" / "See all" head, then rows of `[category tile] · name · progress bar · amount`. The progress bar is `SwInk` on `appSurfaceLow`.
7. **Recent activity card** — pebble card with three most-recent entries.
8. **Floating black bottom nav** — pill carrying Home/Activity/Insights icons + a violet FAB ringed by the pill. This is the **one loud thing** on Dashboard.

### 7.2 Activity (Transactions)

1. Header — back / "Activity" / search circle.
2. Month eyebrow + total spent display.
3. Filter chip row — `[All]` is `Dark`, the rest are `Soft`. Horizontal-scrollable.
4. Day-grouped pebble cards. Each day card has a UPPERCASE eyebrow row (`TODAY · SUN 18 MAY    RM 52.10`) above the rows.

### 7.3 Insights (Breakdown)

1. Header.
2. Year selector — big "2026" display + "Change ▾" Soft pill.
3. **Dark cashflow card** — ink-fill pebble (the one loud thing here is the *card itself*, not the FAB) with year cashflow bars (12 months: spend bar `#FFFFFF`, earn bar `SwMint`). Selected month highlighted with a `SwViolet` bar + amount tooltip.
4. Spend / Income segmented toggle.
5. Donut card — donut + 4-item legend with `+N more` overflow.
6. Category list card.

### 7.4 Add Expense / Income (bottom sheet)

Modal as bottom sheet — 32dp top radius, grabber at the top. **No centered scale-in modal**: centered modals fight one-handed Android use.

1. Close circle / title / spacer.
2. Expense / Income segmented toggle (`Dark` on selected side).
3. Big amount display (56sp ExtraBold, muted RM prefix). Underline accent.
4. Date / repeat chip row.
5. Category strip (horizontal-scroll). Selected category tile turns ink, white text.
6. Account strip (horizontal-scroll). Selected account adopts that account's tile color.
7. Merchant search field (filled `appSurfaceLow`) + recent-merchant chips.
8. Notes field (filled `appSurfaceLow`).
9. **Save expense / income** — ink CTA pill, full width, 56dp tall, with a check glyph.

### 7.5 Accounts

1. Header — back / "All accounts" / `+` circle.
2. Net worth eyebrow + display amount.
3. **Featured primary-account gradient pebble** — same gradient hero pattern as Dashboard but anchored to the user's top account. Carries a Soft `[pencil] Edit` pill.
4. Smaller pebble rows for the remaining accounts, with category-tile icons.
5. Dashed-outline `+ Add account` row.
6. Collapsible `ARCHIVED · N` section with reduced-opacity rows and a `Restore` chip.

### 7.6 Account form (bottom sheet)

1. Back / title / spacer.
2. Live preview row (icon + editable name + meta).
3. Type grid (Cash / Bank / E-wallet / Credit) — 2-column tile grid, selected tile turns ink with a `✓` glyph.
4. Starting balance — filled input with muted RM prefix and ExtraBold value.
5. Icon picker — horizontal-scroll tiles, selected has a `SwVioletSoft` background and an ink ring.
6. Tile color picker — 7 colored discs, selected has a double-ring (white + ink).
7. Save (ink CTA) + (edit only) Archive (red outlined CTA).

## 8. Motion

| Token | Value | Use |
|---|---:|---|
| Press | 140ms | Buttons, rows, icon actions. Scale 0.97 (small), 0.985 (wide surfaces). |
| Tooltip | 160ms | Color tint, micro-animations. |
| Dropdown | 200ms | Filter chips, sub-menus. |
| Modal | 240ms enter / 180ms exit | Add Expense / Account Form. Scale-in from 0.96 + fade. |
| Sheet | 280ms | Bottom sheets (Month picker, Add). iOS-style drawer curve. |
| Stagger | 50ms step | Card entrance on first composition. |

**Reduced motion** drops all transforms but keeps opacity crossfades — reduced motion ≠ no motion.

Tab swaps are a cross-fade with a 20%-width horizontal slide in the direction of `screenOrder()`. No slide if the routes have no defined order.

## 9. Anti-patterns

Do not reintroduce any of the following:

- Warm cream/paper backgrounds (`#FCFAF7`). The canvas is now lavender — every screen.
- Full-bleed unframed dashboard sections. Everything that's a "section" sits on a pebble.
- Slate-gray as a primary color. The new neutral is tinted lavender; ink is `#15121F` only where contrast matters.
- Plum/coral brand-heavy palettes. Hue choices live entirely in the v2 pastel family above.
- Centered modals for the Add flow. Use the bottom sheet.
- Heavy elevation or hard outlines on cards. Pebble = subtle shadow, no border.
- Decorative illustrations, emojis, gradient text, glassmorphism.
- All-caps date headers. Use sentence-case ("Sat 17 May"), with the EYEBROW style only for category/meta labels.

## 10. Implementation map

| Token | Where it lives | Notes |
|---|---|---|
| Surface system | `app/src/main/java/com/spendwise/app/ui/theme/Theme.kt` | `SpendWiseColors` data class + light/dark instances. |
| v2 primitives | `app/src/main/java/com/spendwise/app/ui/theme/SpendWisePrimitives.kt` | `PebbleCard`, `GradientHeroCard`, `Amount`, `CategoryTile`, `PillChip`, `CircleIconButton`, `SectionHead`, `EyebrowText`, `CashflowChip`. |
| Hero composition | `ExpenseTrackerApp.kt::SummaryCard` | Gradient pebble + caustic discs + inline cashflow chips + heatmap card. |
| Pebble shadow | `ExpenseTrackerApp.kt::premiumCardShadowElevation()` | Returns `6.dp`; pair with violet-tinted `ambientColor`/`spotColor`. |
| Heatmap ramp | `MonthCalendar` inside `ExpenseTrackerApp.kt` | Uses the lavender ramp directly; consumers should reference `SwHeat0`-`SwHeat4` going forward. |
| Eyebrow labels | `ExpenseTrackerApp.kt::SectionLabel` | Uppercases input, applies 11sp / 700 / 0.8sp tracking. |
| Bottom nav | `ExpenseTrackerApp.kt::SpendWiseBottomBar` + `AddExpenseFab` | Floating black pill + violet FAB with ink ring. The one loud thing on every screen. |

When adding a new surface, reach for `PebbleCard` (or `PremiumSurface` if you're inside `ExpenseTrackerApp.kt` already). When showing money, reach for `Amount` (or `HeroAmount` for the hero variant). When making a chip, reach for `PillChip`. Don't roll your own.

## 11. Success check

The app is on-system when:

- The first impression is a lavender wash with white pebbles — not warm white space.
- The Dashboard hero reads as a single gradient pebble, not two separate cards.
- Money numbers are big, tabular, ExtraBold, with a quiet "RM" prefix.
- There is exactly one black or violet object per screen, and it's always the action surface.
- The bottom nav is a floating black pill, never a Material `NavigationBar`.
- Category icons live in rounded-square tiles (not circles, not unframed), and their pastel color matches the v2 family.
- The spending heatmap is lavender — never green.
- Press feedback is felt everywhere, but never below 0.95 scale.
