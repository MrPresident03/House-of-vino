# Design system

## Point of view

The cellar ledger. Collectors think in vintages, so the **vintage year is the signature
element**: it is set in a high-contrast Didone, the type of Bordeaux château labels, and it
leads every inventory row like an entry in a cellar book. Everything else stays quiet: dense,
aligned, readable controls in a neutral grotesk. Burgundy is used only for primary actions,
selection and focus. It is never decoration.

## Colour tokens

| Token | Dark (default) | Light (cream) | Use |
|---|---|---|---|
| `bg` | `#1B1819` charcoal | `#F2EBDD` parchment | app background |
| `surface` | `#231F21` | `#FAF6EE` | sheets, rows, cards |
| `surfaceRaised` | `#2C2729` | `#EFE6D6` | pressed / selected rows, inputs |
| `outline` | `#3D3639` | `#D8CCBA` | hairlines, input borders |
| `ink` | `#EEE7E1` | `#2A2023` | primary text |
| `inkMuted` | `#A99F9A` | `#6E625F` | secondary text, metadata |
| `accent` | `#D47A8A` | `#7B1E34` | primary buttons, selection, focus ring |
| `onAccent` | `#1B1819` | `#FFFFFF` | text and icons on accent fills |
| `accentContainer` | `#4A1622` | `#F0D9DC` | selected chips, quantity badge |
| `onAccentContainer` | `#F3D8DD` | `#4A1622` | text on the above |
| `danger` | `#E07A6E` | `#A3352A` | destructive actions, validation errors |

Status colours for drinking windows are muted on purpose. Every status also carries a text
label, so colour is never the only signal:

| Status | Dark | Light |
|---|---|---|
| Hold (too young) | `#8C9BB0` | `#4F5F75` |
| Ready (in window) | `#8FB089` | `#3F6B3A` |
| Drink soon (final year) | `#D2A55A` | `#8A5A12` |
| Past window | `#B88A80` | `#7A4A40` |

Wine colour swatches are used for bottle silhouettes when there is no photo: red `#5E1A28`,
white `#D9C98E`, rosé `#E2A1A0`, orange `#C9803F`, unknown `#6B6466`.

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
