# Design system

## Point of view

Modern and high-contrast (0.4). Content sits on rounded cards that stand clearly off the
background, one bright accent marks actions and selection, and colour carries meaning: a strip
on every wine for its colour, and bright drinking-window statuses. Type is IBM Plex Sans
throughout by default; the Bodoni, Playfair and Garamond pairings remain as options.

## Colour themes

Three themes, each with a light and a dark palette (`ThemeStyle` in `Tokens.kt`). New installs
start on **Bold, dark**. `DesignTokensTest` runs every contrast rule against all six palettes.

| Token | Bold dark (default) | Bold light | Use |
|---|---|---|---|
| `bg` | `#0B0B0E` | `#FFFFFF` | app background |
| `surface` | `#1A1A20` | `#F4F4F6` | cards, rows, bottom bar |
| `surfaceRaised` | `#24242C` | `#EAEAEE` | inputs, chips, secondary buttons |
| `outline` | `#34343E` | `#D9D9DF` | hairlines |
| `ink` | `#F5F5F7` | `#111114` | primary text |
| `inkMuted` | `#9E9EAA` | `#5E5E69` | secondary text |
| `accent` | `#FF4D6D` | `#C8103E` | primary buttons, selected chips and tab |
| `onAccent` | `#1A0208` | `#FFFFFF` | text on accent |
| `danger` | `#FF7A6B` | `#B3261E` | destructive actions, errors |
| Hold / Ready / Drink soon / Past | `#6EA8FF` `#3DDC84` `#FFB547` `#C9A29A` | `#2F5FC4` `#16753F` `#8F5600` `#7A4A40` | drinking-window pills |

**Clean** is the same layout in white with a deep wine accent (`#B0123E`); **Colour-coded** is
warm off-white with an indigo accent (`#4B3FD1`). Their dark variants follow the Bold pattern.
Corners: 12 dp fields, 14 dp buttons, 16 dp cards, pills for chips and statuses.

Wine colours (the strip on each row and the bottle silhouette when there is no photo): red
`#D12E4E`, white `#F2C94C`, rosé `#F58FA8`, orange `#F08A3C`, unknown `#8A8A96`.

Every text/background pair meets WCAG AA: 4.5:1 for body text, 3:1 for large text and UI glyphs.
`DesignTokensTest` checks the pairs, including the status colours, which appear as caption text.
(The dark accent was first drafted as `#C9677A`; it measured 4.42:1 on `bg` and 3.99:1 against
`onAccent`, so it was lightened to `#D47A8A`.)

## Typography

- **Bodoni Moda** (OFL) is used for the vintage numerals, screen titles and wine names at ≥ 20 sp.
  Static instances are cut from the variable font at opsz 72 (display) and opsz 28 (headings).
  It is never used below 20 sp.
- **IBM Plex Sans** (OFL) is used for all controls, body text and metadata. Instances: 400, 500, 600.
  Numbers use tabular figures (`tnum`) so quantities and prices align in columns.

The pairing above is **Classic**. Settings › Preferences offers four more (`FontChoice`):
**Editorial** (Playfair Display headings), **Garamond** (Cormorant Garamond headings, set 12 % larger
because of its small x-height), **Modern** (IBM Plex Sans throughout) and **Device** (the phone's own
font). Body text stays IBM Plex Sans in all but Device. Only the chosen pairing is loaded. Playfair
Display is shipped unmodified (reserved font name); the Cormorant files are subset to Latin.

| Role | Face | Size / line | Notes |
|---|---|---|---|
| `vintageDisplay` | Bodoni Moda 72 / 500 | 56 / 60 | bottle detail header |
| `title` | Bodoni Moda 28 / 600 | 28 / 34 | screen titles |
| `wineName` | Bodoni Moda 28 / 500 | 20 / 26 | detail & row wine name |
| `vintageRow` | Bodoni Moda 28 / 500 | 22 / 26, tnum | the ledger column |
| `body` | Plex 400 | 16 / 24 | |
| `bodyDense` | Plex 400 | 14 / 20 | row metadata |
| `label` | Plex 500 | 14 / 20 | buttons, chips (sentence case, no tracking) |
| `caption` | Plex 400 | 12 / 16 | provenance ("Source: …, 3 Mar 2026") |
| `figure` | Plex 500, tnum | 16 / 20 | quantities, money |

All sizes are in `sp` and scale with the user's font size. Layouts are tested at 200% scale.
There are no all-caps labels and no letter-spaced eyebrows.

## Layout and shape

- 4 dp base grid. Spacing tokens: 4, 8, 12, 16, 24, 32, 48.
- **Ledger row** (Cellar, Search): 72 dp tall (grows with font scale).
  `[vintage 64dp] [producer / wine / region · format] [qty, right-aligned, tnum]`.
  Separated by 1 dp `outline` hairlines inset to the text column.
- Grid view: 2 columns on phones (3+ on tablets), bottle photo or silhouette at 3:4, then the
  vintage and wine below it.
- Corner radius: 6 dp on controls, chips and inputs; 16 dp on sheets and dialogs; 0 on list rows.
- Elevation: none on rows. Sheets use a scrim, not shadows.
- Touch targets ≥ 48 dp. Quantity steppers use 56 dp targets.

## Components (composeApp/ui/design)

`HvTheme`, `LedgerRow`, `BottleTile`, `BottleSilhouette`, `QuantityBadge`, `MoneyText`,
`ProvenanceCaption`, `WindowStatusPill`, `RatingBar`, `FilterChipRow`, `HvTextField`,
`MoneyField`, `DateField`, `QuantityStepper`, `SegmentedChoice`, `HvButton`
(primary/secondary/destructive), `HvTopBar`, `SectionHeader`, `StatBlock`.

State components: `LoadingRows` (skeleton ledger rows), `EmptyState`, `ErrorState`
(states what failed and offers retry), `OfflineBanner`, `PermissionDenied` (explains why and
links to settings), `SuccessSnackbar` (names the action: "Drank 1 bottle", with Undo).

## Motion

- Default transitions: 200 ms ease-out for sheets and screens. iOS keeps the native
  edge-swipe back gesture.
- **One signature moment**: after a stock action, the quantity figure rolls to its new value
  (vertical slide, 240 ms) and the Undo snackbar appears.
- `LocalReducedMotion` disables the roll and screen slides when the OS reduce-motion setting is on.

## Accessibility

- Every icon-only control has a content description. Ledger rows merge their semantics into
  one spoken summary, e.g. "2016 Château Margaux, Margaux, 6 bottles, ready to drink".
- Focus order follows reading order. The focus ring is the 2 dp `accent` outline.
- Errors are announced through live regions and are never shown by colour alone.

## Writing

Sentence case. Buttons say what happens: "Add purchase", "Drink 1 bottle", "Move bottles".
Errors say what happened and what to do: "Couldn't reach the server. Changes are saved on
this device and will sync when you're back online."
