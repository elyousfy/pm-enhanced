# PM (Enhanced): design tokens and Compose spec

Version 1 · 2026-10-30 · dark theme only

- Interactive mockup (source of truth for motion and behaviour): `docs/design/mockup.html`, published at https://claude.ai/artifact/5jem9CfCd82cGYgD5HLLNo
- Design language: Ahmed's RF operations dashboard system (`rf-dashboard-web/app/proto.css`). It uses hairlines instead of elevation, uppercase tracked eyebrows, one accent, the `(0.2, 0, 0, 1)` easing, and Public Sans with Source Serif 4. It is adapted here to a dark phone UI.
- Do not use IBM Plex Mono, or any mono or pixel face, as a design feature. For numbers, use Public Sans with tabular figures.

All sizes are in dp and sp. The mockup's CSS px map 1:1 to dp at a 360 dp-wide screen.

---

## 1. Colour tokens

The neutrals are muted graphite with a slight green bias: not pure black, not navy. Green comes in two strengths:

- **`acc`** is the deep fill green. It is the background for buttons, the "on" switch track and "on" chips, and white text on it passes AA.
- **`accText`** is the soft sage green used as text, icons and the countdown ring on dark surfaces.

Never put `acc` text on a dark surface (3.32:1), and never put white text on `accText` (2.15:1).

| Token | Hex | Compose ARGB | Use |
|---|---|---|---|
| `bg` | `#121413` | `0xFF121413` | Window and scaffold background |
| `surface` | `#181B19` | `0xFF181B19` | Cards, bottom sheets, navigation bar |
| `raised` | `#1F2321` | `0xFF1F2321` | Stepper buttons, selected segment thumb, icon tiles |
| `well` | `#0E100F` | `0xFF0E100F` | Recessed track of the segmented control |
| `line` | `#2A2F2C` | `0xFF2A2F2C` | Hairline card borders, row dividers, switch-off track |
| `line2` | `#3A413D` | `0xFF3A413D` | Strong hairline: outlined buttons, inputs, chips (off), switch-off border, sheet top edge |
| `text` | `#F2F4F3` | `0xFFF2F4F3` | Primary text |
| `sub` | `#A9B0AC` | `0xFFA9B0AC` | Secondary text, eyebrows, switch-off knob |
| `faint` | `#838B86` | `0xFF838B86` | Tertiary text, disabled-prayer rows, chevrons |
| `acc` | `#2A7A56` | `0xFF2A7A56` | Green fill (button, switch on, chip on) |
| `accPress` | `#236749` | `0xFF236749` | Pressed or hovered green fill |
| `onAcc` | `#FFFFFF` | `0xFFFFFFFF` | Text and knob on `acc` |
| `accText` | `#7BBF9A` | `0xFF7BBF9A` | Green text, icons, countdown ring, focus ring, selected-row bar |
| `accWash` | `#26322B` | `0xFF26322B` | `accText` at 14% over `surface`: status pill "ok", nav indicator, silent-now row |
| `accWashLt` | `#1F2622` | `0xFF1F2622` | `accText` at 7% over `surface`: next-prayer row, selected list option, hover |
| `silentBorder` | `#3C5A49` | `0xFF3C5A49` | Hero card border while DND is active |
| `warn` | `#D9A54A` | `0xFFD9A54A` | "Experimental", "Needed", warning icon |
| `warnWash` | `#332E20` | `0xFF332E20` | Warning pill and banner background |
| `warnBorder` | `#4A3F28` | `0xFF4A3F28` | Warning banner border |
| `bad` | `#E07A6E` | `0xFFE07A6E` | Errors |
| `badWash` | `#342825` | `0xFF342825` | Error pill background |
| `neutralPill` | `#252826` | `0xFF252826` | Neutral pill background ("Sound on", "Off today") |
| `inverseSurface` | `#E6E9E7` | `0xFFE6E9E7` | Snackbar background |
| `inverseOnSurface` | `#151816` | `0xFF151816` | Snackbar text |
| `scrim` | `#000000` at 55% | `0x8C000000` | Bottom-sheet scrim |

If a wash is drawn over a surface other than `surface`, use these alpha forms: `accWash` = `accText` at alpha 0.14 (`0x247BBF9A`), `accWashLt` = `accText` at alpha 0.07 (`0x127BBF9A`), `warnWash` = `warn` at alpha 0.14.

### 1.1 Measured contrast (WCAG 2.x)

| Pair | Ratio | Verdict |
|---|---|---|
| `text` on `bg` / `surface` / `raised` | 16.75 / 15.72 / 14.39 | AAA |
| `sub` on `bg` / `surface` / `raised` | 8.36 / 7.85 / 7.19 | AAA |
| `faint` on `bg` / `surface` / `raised` | 5.29 / 4.96 / 4.55 | AA. Keep `faint` off anything lighter than `raised`. |
| White on `acc` (primary button) | **5.23** | AA |
| White on `accPress` | 6.76 | AA |
| `text` (#F2F4F3) on `acc` | 4.77 | AA |
| `accText` on `bg` / `surface` / `raised` | 8.60 / 8.07 / 7.39 | AAA |
| `accText` on `accWash` / `accWashLt` | 6.20 / 7.18 | AA |
| `text` on `accWash` | 12.08 | AAA |
| `warn` on `warnWash` | 6.08 | AA |
| `bad` on `badWash` | 4.86 | AA |
| `sub` on `neutralPill` | 6.73 | AA |
| Snackbar `#151816` on `#E6E9E7` | 14.63 | AAA |
| `acc` fill vs `surface` (non-text, switch on) | 3.32 | Passes 3:1 |
| Switch-off knob `sub` vs track `line` | 6.16 | Passes 3:1. The off state reads from the knob, not the track. |
| Upstream suggestion `#2FA36B` with white text | 3.20 | **Fails.** This is why the fill green was darkened to `#2A7A56`. |

---

## 2. Material 3 `darkColorScheme` mapping

Use `darkColorScheme(...)` with the values below. Set `surfaceTint = Color.Transparent` and use `tonalElevation = 0.dp` everywhere. The system has no elevation tint and no shadows: hierarchy comes from fills and 1 dp borders.

| M3 role | Token | Hex |
|---|---|---|
| `primary` | `acc` | `#2A7A56` |
| `onPrimary` | `onAcc` | `#FFFFFF` |
| `primaryContainer` | `accWash` | `#26322B` |
| `onPrimaryContainer` | `accText` | `#7BBF9A` |
| `inversePrimary` | `accText` | `#7BBF9A` |
| `secondary` | `accText` | `#7BBF9A` |
| `onSecondary` | (dark green) | `#0F2419` (7.59:1) |
| `secondaryContainer` | `accWash` | `#26322B` (NavigationBar indicator) |
| `onSecondaryContainer` | `accText` | `#7BBF9A` (selected nav icon) |
| `tertiary` | `warn` | `#D9A54A` |
| `onTertiary` | (dark amber) | `#2A1E05` (7.33:1) |
| `tertiaryContainer` | `warnWash` | `#332E20` |
| `onTertiaryContainer` | `warn` | `#D9A54A` |
| `error` | `bad` | `#E07A6E` |
| `onError` | (dark red) | `#3A0F0A` (5.73:1) |
| `errorContainer` | `badWash` | `#342825` |
| `onErrorContainer` | `bad` | `#E07A6E` |
| `background` | `bg` | `#121413` |
| `onBackground` | `text` | `#F2F4F3` |
| `surface` | `bg` | `#121413` (the Scaffold surface is the page) |
| `onSurface` | `text` | `#F2F4F3` |
| `surfaceVariant` | `raised` | `#1F2321` |
| `onSurfaceVariant` | `sub` | `#A9B0AC` |
| `surfaceDim` | `well` | `#0E100F` |
| `surfaceBright` | (n/a) | `#2A2F2C` |
| `surfaceContainerLowest` | `well` | `#0E100F` |
| `surfaceContainerLow` | (n/a) | `#151816` |
| `surfaceContainer` | `surface` | `#181B19` (cards, sheet, NavigationBar) |
| `surfaceContainerHigh` | `raised` | `#1F2321` |
| `surfaceContainerHighest` | (n/a) | `#262A28` |
| `outline` | `line2` | `#3A413D` |
| `outlineVariant` | `line` | `#2A2F2C` (HorizontalDivider default) |
| `inverseSurface` | `inverseSurface` | `#E6E9E7` |
| `inverseOnSurface` | `inverseOnSurface` | `#151816` |
| `scrim` | `scrim` | `#000000`; apply 0.55 alpha in `ModalBottomSheet(scrimColor = …)` |
| `surfaceTint` | (none) | `Color.Transparent` |

**Overrides required.** M3 uses `primary` as a foreground colour in several places, and `#2A7A56` is too dark to be text on our surfaces. Override these to `accText`:

- `TextButton` content colour.
- `RadioButton` selected colour.
- `CircularProgressIndicator` and the custom countdown ring.
- `OutlinedTextField` focused border, label and cursor.
- Any link text.
- The focus indicator.

Put the tokens M3 has no slot for in a `CompositionLocal` (for example `LocalPmColors`): `accText`, `accWashLt`, `accPress`, `faint`, `line2`, `well`, `warnWash`, `warnBorder`, `neutralPill`, `silentBorder`.

System bars: edge-to-edge, light status-bar icons. The gesture-bar area takes `surface` (it continues the bottom nav) or `bg` on screens without a nav.

---

## 3. Typography

### 3.1 Font files (both SIL OFL 1.1, from Google Fonts)

`minSdk` is 24, and variable-font axes only apply on API 26 and up, so **ship static TTFs** in `res/font/`.

| Family | Weights to ship | Notes |
|---|---|---|
| **Public Sans** | 400 Regular, 500 Medium, 600 SemiBold, 700 Bold (`PublicSans-Regular/Medium/SemiBold/Bold.ttf` from the zip's `static/` folder) | Body and UI. Has `tnum`. |
| **Source Serif 4** | 600 SemiBold, 700 Bold | Display titles only. The Google Fonts zip ships static instances per optical size. Use the **display-size instance** (36pt or 48pt, e.g. `SourceSerif4_36pt-SemiBold.ttf`; check the exact filename in the zip) for sizes ≥ 24 sp. Use the default or 18pt instance for anything smaller. The dashboard found that the wrong optical size renders display titles noticeably wider. |
| Arabic, Urdu | none bundled | Falls back to the system **Noto Sans Arabic** / Noto Nastaliq Urdu. Do not set Source Serif on Arabic text: the serif has no Arabic glyphs, so the platform falls back anyway. Use the locale rules in section 3.3. |

Apply tabular figures to every time, countdown, duration and count: `TextStyle(fontFeatureSettings = "tnum")`.

### 3.2 Type scale mapped to M3 slots

Letter spacing given in em converts to sp as em × size. For example, 0.08 em at 11 sp is 0.88 sp.

| M3 slot | Family | Weight | Size / line height (sp) | Letter spacing | tnum | Used for |
|---|---|---|---|---|---|---|
| `displayLarge` | Source Serif 4 (display) | 600 | 34 / 38 | −0.34 sp (−0.01 em) | no | Hero prayer name |
| `displayMedium` | Source Serif 4 (display) | 600 | 30 / 35 | −0.30 sp | no | Onboarding headline |
| `headlineMedium` | Source Serif 4 (display) | 600 | 26 / 30 | 0 | no | Bottom-sheet title |
| `headlineSmall` | Source Serif 4 (display) | 600 | 24 / 29 | −0.24 sp | no | Top-bar screen title |
| `titleLarge` | Public Sans | 600 | 21 / 26 | −0.2 sp | **yes** | Countdown in the ring |
| `titleMedium` | Public Sans | 600 | 17 / 22 | 0 | **yes** | Prayer time in a row; 18 sp in the sheet's "today's silence" box |
| `titleSmall` | Public Sans | 600 | 15.5 / 20 | 0 | no | Prayer name in a row |
| `bodyLarge` | Public Sans | 500 | 15 / 20 | 0 | no | List-row title, option title |
| `bodyMedium` | Public Sans | 400 | 14 / 21 | 0 | no | Lead and body text, row values |
| `bodySmall` | Public Sans | 400 | 12.5 / 18 | 0 | yes for times | Supporting text, helper text, prayer subtitle |
| `labelLarge` | Public Sans | 600 | 15 / 20 | 0 | no | Primary button |
| `labelMedium` | Public Sans | 600 | 13.5 / 18 | 0 | no | Secondary button; segmented control at 13 sp (500 off, 600 on) |
| `labelSmall` | Public Sans | 600 | 11 / 14 | **+0.88 sp**, UPPERCASE | no | Eyebrows (section labels) |

Extra styles:

- **pill**: 11 / 14, 600, +0.44 sp, uppercase.
- **navLabel**: 11.5 / 14, 500 (600 when selected).
- **ringLabel**: 10.5 / 13, 400, colour `sub`.
- **statusBar**: system.

### 3.3 Locale rules (ar, ur, and any RTL language)

- Eyebrows and pills use **no uppercase and letter spacing 0** (tracking breaks Arabic joining), at 12 sp.
- For display styles, set letter spacing 0 and line height × 1.3. Use weight 700, because Noto Sans Arabic 600 looks light next to Latin serif titles. Reduce size by about 10%: hero 30 sp, top bar 22 sp, onboarding 26 sp.
- Digits stay Latin (0–9) in every locale, for example `ar-AE-u-nu-latn` (see Open questions). Wrap every time range ("15:17 – 15:37", "05:05 → 05:07") in Unicode LTR isolates `⁦…⁩` or `BidiFormatter`, so the range never reorders inside an RTL sentence.

---

## 4. Shapes

The dashboard's radii are cards 6, controls 4 and pills 999. On a 360 dp phone with 48 dp touch targets those read as sharp and slightly cramped, so cards and controls are **doubled**, which keeps the dashboard's 3 : 2 card-to-control ratio. Pills stay fully round.

| M3 shape slot | Radius | Used for |
|---|---|---|
| `extraSmall` | 6 dp | Segmented thumb, focus rings, inner selections |
| `small` | 8 dp | Buttons, steppers, inputs, segmented track, snackbar, "today's silence" box |
| `medium` | 12 dp | Cards, banners |
| `large` | 20 dp (top corners only) | Bottom sheet |
| `extraLarge` | 20 dp | (unused) |
| Full (`CircleShape` / 50%) | n/a | Pills, day chips, switch track (13 dp), nav indicator (15 dp), drag handle |

Borders are 1 dp `line` on cards and 1 dp `line2` on interactive outlines. There are no shadows. The sheet has a 1 dp `line2` top edge.

---

## 5. Spacing (dp)

The grid is 4 dp, with 2 dp half-steps for tight inner gaps.

| Token | Value | Use |
|---|---|---|
| `space2` | 2 | Title-to-subtitle gap inside a row |
| `space4` | 4 | Eyebrow-to-heading; stepper button gap; mini-day gap |
| `space6` | 6 | Day-chip gap; pill inner gap |
| `space8` | 8 | Eyebrow-to-card gap; sheet field label-to-control |
| `space12` | 12 | Row inner horizontal gap; card-to-card in a stack |
| `space16` | 16 | **Screen gutter**; card horizontal padding; row horizontal padding |
| `space18` | 18 | Hero padding and gap; gap between sheet fields |
| `space20` | 20 | **Gap between sections**; top-bar and sheet horizontal padding |
| `space24` | 24 | Bottom padding of scroll content |

Fixed sizes:

- Top bar: min height 64, padding 12 / 20.
- List row: min height 56, padding 12 / 16.
- Prayer row: min height 64, padding 10 / 16.
- Sunrise row: min height 44.
- Bottom nav: height 72 (8 top padding).
- Touch targets: all ≥ 48. The 46 × 26 switch and 36 × 36 stepper buttons get padding to a 48 dp hit box (`Modifier.minimumInteractiveComponentSize()`).

---

## 6. Motion tokens

```
EaseStandard = CubicBezierEasing(0.2f, 0f, 0f, 1f)      // dashboard ease, used for everything by default
EaseSpringy  = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f) // mockup's chip pop; in Compose prefer the spring below
```

| Token | Spec | Applies to |
|---|---|---|
| `DurPress` | 120 ms, `EaseStandard` | Press/hover colour, button scale 1 → 0.98, stepper scale 1 → 0.92, chip colour |
| `DurSwitch` | 160 ms, `EaseStandard` | Switch knob offset 2 → 22 dp, track and knob colour; knob widens 20 → 23 dp while pressed |
| `DurEnter` | 200 ms, `EaseStandard` | Screen enter: fade 0 → 1 + translateY 4 dp → 0; nav indicator scaleX 0.4 → 1 + fade |
| `Stagger` | `DurEnter`, delay 60 ms + 40 ms × index | Prayer rows (6 rows including Sunrise) on Today enter |
| `DurCollapse` | 220 ms, `EaseStandard` | `AnimatedVisibility(expandVertically + fadeIn + slideInVertically(-6 dp → 0))`, reversed on exit |
| `SwapOut` / `SwapIn` | 140 ms fade + slide up 8 dp, then 220 ms fade + rise from 10 dp | Hero `AnimatedContent` when the next prayer or silent state changes (`togetherWith`, `SizeTransform(clip = false)`) |
| `DurSheet` | 320 ms, `EaseStandard` | Bottom sheet translateY 100% → 0 and scrim 0 → 0.55. M3 `ModalBottomSheet` uses its own spring; that is acceptable, but if tuned, use `tween(320, easing = EaseStandard)`. |
| `RingTick` | `tween(1000, easing = LinearEasing)` | Countdown arc sweep, retargeted every second so it moves continuously |
| `RingJump` | `tween(600, easing = EaseStandard)` | Arc when its value jumps by > 4% (a new prayer, or entering or leaving silence) |
| `ChipSpring` | `spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium)` | Day chip scale 0.9 → 1 on toggle (one visible overshoot) |
| `Breath` | `infiniteRepeatable(keyframes { 2400 ms; scale 1 → 2.6 and alpha 0.55 → 0 over the first 70%; hold 0 }, RepeatMode.Restart)` | Halo behind the dot in the "Silent now" pill and the row "Now" pill. The pill background also oscillates `accWash` ↔ `#2C3C33` (2400 ms, ease-in-out, reverse). |
| `GrantPop` | `ChipSpring` | "Granted" pill appearing after a permission is allowed |

**Reduced motion.** When `Settings.Global.ANIMATOR_DURATION_SCALE == 0` (the system "Remove animations" setting), use `snap()` for every spec, stop `Breath`, and drop the stagger. Countdown text still updates each second.

---

## 7. Component specs

### Switch
- Track 46 × 26, radius 13, 1 dp border. Knob 20 dp circle, 2 dp inset, travels 2 → 22 dp (mirrored in RTL). 160 ms.
- Off: track `line`, border `line2`, knob `sub`.
- On: track and border `acc`, knob `onAcc`.
- Pressed: knob 23 dp wide. Disabled: 40% alpha.
- Implement as a custom composable or a styled M3 `Switch` with the thumb icon removed and these colours. M3's default 52 × 32 is too big for this language.
- Always pair it with a label (`Modifier.toggleable(role = Role.Switch)` on the whole row when the row has no other action).

### Primary button
- Height 48, radius 8, horizontal padding 20, full width in sheets and onboarding.
- `acc` background, `onAcc` text in `labelLarge`.
- Pressed: `accPress` + scale 0.98. Disabled: 38% alpha, no press.

### Secondary button
- Height 36 (48 dp hit box), radius 8, padding 0 / 14, 1 dp `line2` border, transparent fill, `text`, `labelMedium`.
- Pressed: `accWash` fill + scale 0.97.

### Text button
- `accText`, 13 sp, 600, padding 6 / 4. Used for "Edit".

### Chip (day chip)
- Height 40, `CircleShape`, equal weight in a row of 7 with 6 dp gaps.
- Off: 1 dp `line2` border, `sub` text, 500.
- On (filled): `acc` fill and border, `onAcc` text, 600.
- Colour changes at `DurPress`; scale runs `ChipSpring` on toggle. Role: checkbox, with the full day name as the content description.

### Mini day row (Today, read-only)
- 7 equal pills, 11 sp / 600, padding 5 vertical.
- Off: 1 dp `line` border, `faint` text. On: `accWash` fill, `accText`.
- Today's day gets an extra 1 dp `accText` ring.

### Segmented control
- Track: `well` fill, 1 dp `line` border, radius 8, 3 dp padding.
- Thumb: `raised` fill, 1 dp `line2` border, radius 6, slides between positions in 200 ms `EaseStandard`.
- Items: height 36, 13 sp. Off: `sub` 500. On: `text` 600.
- Single choice; role radio group.

### Stepper
- `[−] value [+]`. Buttons 36 × 36, radius 8, `raised` fill, 1 dp `line2` border, 18 sp glyph.
- Value: min width 64, centred, 14 sp / 600, tnum.
- A button disables at its bound (35% alpha). Press scale 0.92 with `accWash` fill.
- Units: "20 min", "+2 min" / "−3 min" / "0 min", or "HH:MM" for time steppers.

### List row
- Min height 56, padding 12 / 16, 12 dp gap.
- Title `bodyLarge`; optional subtitle `bodySmall` in `sub`; trailing value `bodyMedium` in `sub`, then a chevron (18 dp, `faint`, mirrored in RTL) or a control.
- Rows are separated by 1 dp `line` dividers inside a card (none above the first row).
- Pressed: `accWash`. Hover/focus: `accWashLt`.
- Selected (pickers): 2 dp `accText` bar on the start edge + `accWashLt` fill.

### Card
- `surface` fill, 1 dp `line` border, radius 12, no elevation; clips its children.

### Prayer row (Today list)
- Layout, start to end: name (`titleSmall`) over subtitle (`bodySmall`, tnum) | time (`titleMedium`, tnum, min width 52, end-aligned) | **switch** (per-prayer enable).
- Min height 64, padding 10 / 16.
- Tapping the row (not the switch) opens the prayer sheet. Toggling the switch does not open it.
- Subtitle by state:
  - Off: "Silence off".
  - At adhan: "20 min from adhan".
  - Ablution: "Ablution · 30 min".
  - Iqama: "Iqama +5 · 20 min".
  - Jumuʿah: "Fixed · 12:30 – 13:15".
- States, which can combine:

| State | Rule | Visual |
|---|---|---|
| next | First prayer after now, today, when nothing is silent | `accWashLt` fill + 2 dp `accText` start bar (animates scaleY 0 → 1, 200 ms) |
| now | Inside an enabled prayer's silence window | `accWash` fill + start bar + small "NOW" ok-pill with breathing dot next to the name |
| past | Time ≤ now | Time in `sub` |
| off | Prayer switch off | Name, time and subtitle in `faint` |

- Sunrise row: informational only. Min height 44, 13 sp `sub` name, 14 sp `sub` time, a 46 dp spacer where the switch would be. It never gets a switch or a tap action.
- On Fridays with Jumuʿah enabled, the Dhuhr row becomes **Jumuʿah**: it shows the fixed time (12:45) and takes its window from the Jumuʿah settings. Its switch is still Dhuhr's.
- Whole list: 55% alpha when the master switch is off or today isn't an active day.

### Next-prayer hero
- Card (section 7, Card), padding 18, row layout with 18 dp gap: countdown ring on the start side, info column on the end side.
- Info column (4 dp gaps):
  - Eyebrow: "NEXT PRAYER", "SILENT NOW" or "PRAYER MODE OFF".
  - Prayer name in `displayLarge`.
  - "Adhan **15:12**" in 15 sp `sub` with the time in `text` 600 tnum. On Jumuʿah the label is "Prayer".
  - Window line in 12.5 sp / 500 `accText`. It reads "Silent 15:17 – 15:37", or "No silence for this prayer", "Turn on to silence prayers", or "Not active on Saturday".
- Silent state: `accWashLt` fill and `silentBorder` border (300 ms). Master off: whole card at 60% alpha.
- `AnimatedContent` keyed on (prayer, silent?, tomorrow?) with `SwapOut`/`SwapIn`. The countdown itself is not animated; it just updates.

### Countdown ring
- 112 dp box. Track circle 100 dp diameter, 4 dp stroke `line`. Progress arc in `accText`, 4 dp, round caps, starting at 12 o'clock and running clockwise. In RTL it mirrors to counter-clockwise (see Open questions).
- Centre: countdown in `titleLarge` tnum ("H:MM:SS" when an hour or more is left, otherwise "MM:SS"). Below it, a ring label at max width 80 ("until adhan", "until prayer" or "until sound returns").
- Progress value:
  - Normal: (now − previous prayer) / (next prayer − previous prayer).
  - Silent: (now − window start) / (window end − window start).
- Animate with `RingTick`, or `RingJump` when the change is > 4%.
- Draw with `Canvas` + `drawArc`.

### Bottom sheet
- `ModalBottomSheet` with `containerColor = surface`, top radius 20, 1 dp `line2` top border, scrim black at 55%, max height 88%.
- Drag handle 32 × 4, `line2`, radius 2, 10 dp top padding.
- Header: padding 6 / 20 / 14, `line` divider below.
- Body: padding 16 / 20 / 20, fields 18 dp apart, scrolls.
- Esc / back or a scrim tap dismisses.
- Picker variant: header with title + 36 dp close button, a search field, then the option list (see Location screen).

### Search field
- Height 44, radius 8, 1 dp `line2` border (`accText` when focused), `bg` fill.
- Leading 18 dp search icon in `sub`; 15 sp input; placeholder "Search" in `sub`.
- Filters as you type on both the English and native names. Empty result: "No matches. Check the spelling."

### Status pill
- Height 24, `CircleShape`, padding 0 / 10, `pill` text style, optional 7 dp dot (`currentColor`).

| Variant | Fill / text |
|---|---|
| ok | `accWash` / `accText` |
| warn | `warnWash` / `warn` |
| bad | `badWash` / `bad` |
| neutral | `neutralPill` / `sub` |

- The home status pill shows the first case that applies:
  1. Master off: neutral "PRAYER MODE OFF".
  2. DND access missing: warn "NEEDED · DND".
  3. Today not active: neutral "OFF TODAY".
  4. Inside a window: ok "SILENT NOW", with `Breath`.
  5. Otherwise: neutral "SOUND ON", dot at 60% alpha.

### Warning banner
- `warnWash` fill, 1 dp `warnBorder` border, radius 12, padding 14 / 16.
- 20 dp warning icon in `warn`; title 14 / 600 `text`; body 12.5 `sub`; trailing secondary button.
- Collapses with `DurCollapse` when resolved.

### Source option (radio card)
- A row inside a card: 18 dp radio (2 dp `line2` ring; when selected, `accText` ring + 8 dp `accText` dot), title 15 / 600 (+ optional pill), subtitle 12.5 `sub`.
- Selected row: `accWashLt`. Disabled: 45% alpha.

### Top bar
- No `TopAppBar` container colour: it sits on `bg` and does not change on scroll.
- Min height 64, padding 12 / 20.
- Title in `headlineSmall`.
- Today screen: date eyebrow above the title ("FRIDAY 30 OCTOBER"), and the **master switch** at the end.
- Other screens: title only.

### Bottom navigation
- `NavigationBar`, `containerColor = surface`, 1 dp `line` top border, height 72. Four items: Today (clock), Schedule (calendar), Location (map pin), Settings (sliders).
- Icons 22 dp, 1.75 dp stroke style (Material Symbols Rounded, weight 300, is a fine match).
- Indicator 56 × 30, radius 15, `accWash`. On select it animates scaleX 0.4 → 1 + fade, 200 ms.
- Selected: icon `accText`, label `text` 600. Unselected: icon and label `sub` 500.
- Hidden on onboarding.

### Snackbar (toast)
- `inverseSurface` fill, `inverseOnSurface` text 13.5 / 500, radius 8, 16 dp side insets, sits 12 dp above the nav.
- Enter: fade + rise 8 dp, 200 ms. Auto-dismiss after 2.2 s.

---

## 8. Screens

The scroll content of every tab is: top bar, then a column with a 16 dp gutter and 20 dp between sections. Each section is an **eyebrow** (4 dp start inset), 8 dp gap, then a **card**, with optional **helper text** (12.5 sp `sub`, 4 dp inset) below.

### 8.1 Onboarding (first run; no bottom nav)
1. Step indicator: two 24 × 4 bars, the first in `accText` and the second in `line2`.
2. Eyebrow "STEP 1 OF 2 · PERMISSIONS", then the headline "Silence your phone for every prayer" (`displayMedium`, balanced wrap), then the lead paragraph (14 / 21, `sub`).
3. Card with 4 permission rows. Each row: a 36 dp icon tile (radius 10, `raised`; `accWash` + `accText` icon once granted), a tiny "REQUIRED" or "RECOMMENDED" label (10.5 sp, tracked, `faint`), the title (14.5 / 600), a description (12.5 `sub`), and at the end either an **Allow** secondary button or an ok pill "✓ GRANTED" (pops in with `GrantPop`). The rows:
   - Do Not Disturb access (required). Opens `ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS`.
   - Alarms & reminders (required). Opens `ACTION_REQUEST_SCHEDULE_EXACT_ALARM`.
   - Notifications (recommended). `POST_NOTIFICATIONS` runtime request.
   - Unrestricted battery (recommended). Opens `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.
4. Footer pinned to the bottom of the scroll: "N of 4 allowed" (tnum), then a full-width primary button **"Continue to city"**. The button is disabled until both required permissions are granted, with the hint "Allow the two required items to continue".
5. Step 2 is the **Location & source** screen. The onboarding flow ends when a city is chosen.

Re-check permission state in `onResume`, because grants happen in system settings.

### 8.2 Today (Home)
1. Top bar: date eyebrow + "Prayer Mode", with the master switch at the end. Toggling it shows the snackbar "Prayer Mode on" or "Prayer Mode off".
2. Conditional **warning banner** when DND access is missing: "Do Not Disturb access is off. PM can't silence your phone until you allow it." with an [Allow] button.
3. Status row: the status pill (section 7) on the start side, and on the end side a location shortcut (pin icon + "Abu Dhabi · Aladhan", 12.5 sp `sub`, ellipsised) that opens Location.
4. **Next-prayer hero** with the countdown ring.
5. Section "TODAY'S TIMES". The eyebrow row also shows the Hijri date at the end ("19 Jumada I 1448 AH", Umm al-Qura calendar, 12 sp `sub`). Card list order: Fajr, Sunrise (info row), Dhuhr (Jumuʿah on Friday), Asr, Maghrib, Isha. Rows stagger in on enter.
6. Section "ACTIVE DAYS" with an "Edit" text button (goes to Schedule). Card containing the mini day row and a summary line: "Every day", "Mon, Tue, Wed, Thu, Sun", or "No days selected".

Time strings are 24-hour HH:MM (see Open questions). The countdown recomposes every second. Use a single `LaunchedEffect` ticker aligned to the wall-clock second, and only while the screen is visible (`repeatOnLifecycle(STARTED)`).

Silence window rules, applied per prayer after its tune offset:
- At adhan: start = adhan.
- Ablution: start = adhan − 10 min.
- Iqama: start = adhan + iqama minutes.
- In all three cases, end = start + duration.
- Jumuʿah: start = fixed time − "before", end = fixed time + "after".

Keep the upstream scheduler's semantics if they differ, and tell design so the copy can match.

### 8.3 Prayer detail (bottom sheet, opened from a prayer row)
1. Header: eyebrow "PRAYER" over the prayer name (`headlineMedium`) over "Adhan 15:12". At the end: eyebrow "SILENCE" over the **enable switch**, the same state as the row switch.
2. Body, which collapses away (`DurCollapse`) when the prayer is disabled:
   1. "TODAY'S SILENCE" box: `bg` fill, 1 dp `line`, radius 8, padding 12 / 14. Eyebrow on the start side, "15:17 → 15:37" at the end in 18 sp / 600 tnum, LTR-isolated.
   2. **Silence starts**: segmented control with "At adhan", "Ablution" and "After iqama". Choosing one is the only way to set ablution or iqama, which makes them mutually exclusive by construction. Helper text below changes with the choice:
      - At adhan: "Starts the minute the adhan is called."
      - Ablution: "Starts 10 min before adhan, time for wudu."
      - After iqama: "Waits after adhan, then silences for the prayer."
   3. Iqama mode only (collapses in): a "Iqama after" stepper, 5–30 min in steps of 5.
   4. **Silence length** stepper: 15–60 min in steps of 5.
   5. **Adjust adhan time** stepper: −30 to +30 min in steps of 1. This is the same value as the Location screen's tuning.
   6. On Dhuhr only, a helper line: "On Fridays the Jumuʿah settings in Schedule apply."
3. Full-width primary button **Done**.

Edits save immediately and reschedule. Unlike upstream, there is no "turn the switch off before editing" lock (see Open questions).

### 8.4 Schedule
1. **ACTIVE DAYS** (the eyebrow row shows the summary at the end). The card holds:
   - Seven day chips, **Monday first** by default.
   - "Week starts on", then a segmented control [Monday | Sunday] that reorders the chips and the mini row.
   - Helper text: "PM stays off on days you clear."
2. **BEFORE SILENCE**: a row "Play takbir first" / "Short takbir audio as silence begins." with a switch.
3. **FRIDAY JUMUʿAH**: a row "Use Jumuʿah timing" / "Replaces Dhuhr every Friday." with a switch. When on, these collapse in:
   - "Jumuʿah time" with an ok pill "UAE FIXED" (shown only when the country is UAE) and a time stepper. Default **12:45**, range 10:00–15:00 in steps of 5.
   - "Silence before" stepper, 0–60 in steps of 5, default 15.
   - "Silence after" stepper, 10–90 in steps of 5, default 30.
   - Summary line in `accText`: "Fridays: silent 12:30 – 13:15".
4. **RAMADAN & EID**: a row "Ramadan schedule" / "Turns on by itself during Ramadan (Umm al-Qura calendar)." with a switch. When on, these collapse in:
   - "Isha & Taraweeh" / "Replaces the Isha window", stepper 30–150 in steps of 15.
   - "Tahajjud" / "Replaces the Fajr window", with a switch. When on, a "Silence length" stepper (15–90) collapses in.

   Always visible below those:
   - "Eid prayer time", time stepper 05:00–09:00 in steps of 5.
   - "Eid silence", stepper 15–90 in steps of 15.

### 8.5 Location & times (also onboarding step 2)
1. **LOCATION** card:
   - Country row (value + chevron). Opens a picker sheet with a search field over all countries (English + native names).
   - City row. Opens a picker for that country's cities, also searchable.
   - "Time zone" row, read-only, "Asia/Dubai · GMT+4".

   Helper text: "Set by hand. PM never uses GPS." Changing the country resets the method to Auto, picks the first city, and forces the source to Aladhan if the country is not UAE.
2. **TIME SOURCE** card with two radio cards:
   - **Aladhan**: "Any country · calculated by aladhan.com".
   - **UAE Awqaf official**, with a warn pill "EXPERIMENTAL": "UAE cities only · published official times". Disabled (45% alpha) outside the UAE.
3. **CALCULATION** card:
   - With Aladhan:
     - "Method" row showing the method name, with the subtitle "Auto for United Arab Emirates · 16" or "Chosen by you · N". It opens a searchable picker: "Auto (recommended)" first, then all methods, each with its Aladhan id. Default per country: AE 16, SA 4, QA 10, KW 9, BH/OM 8, EG 5, JO 23, PK/IN 1, GB 15, TR 13, MY 17, ID 20, MA 21.
     - "Asr time" segmented control: [Standard | Hanafi] (school 0 / 1).
   - With Awqaf: those rows collapse and a note collapses in: "Awqaf publishes final times, so method and Asr settings are not used."
4. **MATCH YOUR MOSQUE** card: five rows (Fajr … Isha). Each has the name, a preview subtitle ("18:55 → 18:57", tnum, LTR-isolated) and a tune stepper (−30 to +30, "+2 min"). Helper text: "Nudge any time by a few minutes so silence lines up with your mosque."

### 8.6 Settings
1. **NOTIFICATIONS** card: switch rows "When silence starts and ends" and "Morning summary of today's times".
2. **LANGUAGE** card: one row with the current language's native name. It opens a picker (radio list) of English, العربية, اردو, Français, Español, Português, Türkçe, Bahasa Indonesia, Bahasa Melayu, বাংলা and 中文. Apply with `AppCompatDelegate.setApplicationLocales`. RTL follows the locale.
3. **PERMISSIONS** card: one row per permission (the same four as onboarding). The end of each row shows an ok pill "✓ GRANTED", or a warn pill "NEEDED" + a secondary **Fix** button that deep-links to the system screen.
4. **ABOUT** card:
   - Version: "1.0.0 · upstream 1.8.5".
   - Licence: "GPL-3.0", opens the licence text.
   - Quick start guide, Privacy notice, Terms of service.
   - Source code, with the subtitle github.com/prayermode/prayermode.

   Helper text below: "PM (Enhanced) is a fork of Prayer Mode by Yahyaoui (github.com/prayermode/prayermode). Free software under GPL-3.0."

---

## 9. RTL and accessibility checklist
- Use start/end everywhere (padding, the selected-row bar, chevrons via `autoMirror`, and the switch knob direction). The status-bar clock and LTR-isolated times stay LTR.
- Every switch, chip and segment has a role and a content description. Prayer rows are a single clickable with the switch as a separate toggleable.
- The countdown is not a live region; announce only state changes ("Silence started for Asr").
- Never reduce text below 10.5 sp. Respect font scale: rows grow, and nothing has a fixed height except controls.
- Focus indicator: a 2 dp `accText` outline at 2 dp offset.

## 10. Open questions for Ahmed
1. Should times be 24-hour ("15:12") or 12-hour ("3:12 PM")? Should Arabic show Latin or Arabic-Indic digits? The mockup uses 24-hour and Latin digits.
2. Upstream locks settings while Prayer Mode is on. Is it fine to drop that lock and reschedule on every edit?
3. In RTL, the countdown ring currently fills counter-clockwise (mirrored). Keep that, or always fill clockwise?
4. Should Jumuʿah silence be anchored to the fixed 12:45 (before/after), or should it start at Dhuhr adhan?
