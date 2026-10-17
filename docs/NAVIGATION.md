# Navigation map

The root has five bottom tabs. Each tab keeps its own back stack, and switching tabs keeps
each tab's scroll and stack. Routes are `@Serializable` types (type-safe Navigation Compose).

```
Launch
 ├─ FirstRun (only on first launch): currency, appearance → start, or join a partner's cellar
 └─ Main (bottom bar)
     ├─ Cellar ─────────────── inventory list/grid, sort, quick filters, summary strip
     │    ├─ VintageDetail(vintageId)
     │    │    ├─ EditWine(wineId) / EditVintage(vintageId)
     │    │    ├─ AddLot(vintageId)            purchase / opening balance
     │    │    ├─ StockAction(lotId, kind)     consume · sell · gift · loss · adjust
     │    │    ├─ Transfer(lotId)
     │    │    ├─ MovementHistory(vintageId)   ledger + undo (reversal)
     │    │    ├─ AddTasting(vintageId) → TastingDetail
     │    │    └─ Valuations(vintageId)
     │    └─ AddWine (FAB)
     │         ├─ Manual  → WineEditor → AddLot
     │         ├─ ScanBarcode → Candidates (confidence) → WineEditor(prefilled)
     │         └─ ScanLabel  → OCR fields (confidence) → WineEditor(prefilled)
     ├─ Search ─────────────── scopes: Cellar · Wishlist · Journal · Discover
     │    ├─ Filters (sheet)   colour, style, country, region, grape, vintage range,
     │    │                    format, location, tags, price range, in stock, window status
     │    ├─ SavedSearches · RecentQueries
     │    └─ result → VintageDetail / WishlistDetail / TastingDetail / DiscoverDetail
     ├─ Journal ───────────── tasting history (timeline), rating filter
     │    └─ TastingDetail(tastingId) → EditTasting
     ├─ Wishlist ──────────── grouped by status, sorted by priority
     │    └─ WishlistDetail(itemId) → EditWishlist · MerchantLinks · ConvertToInventory
     └─ More
          ├─ Locations ───── tree → LocationDetail (grid of slots, contents) → EditLocation
          ├─ Insights ────── totals, cost, value (with sources), windows, consumption, breakdowns
          ├─ ImportExport ── CsvImport: pick → map columns → validate → duplicates → commit
          │                  Export: inventory · tasting history · financial records
          ├─ Sharing ─────── not shared: Share this cellar → Invite (QR + code) · Join → scan/paste → confirm
          │                  shared: Send changes · Receive changes · phones · Invite · Remove · Stop sharing
          │                  └─ Conflicts ("Changed on both phones") → keep / use the other
          └─ Settings ────── Sharing · Currency & rates · Preferences · Privacy · Notifications ·
                             Backup and export · Conflicts · Erase this phone
```

## Deep links (Android App Links and iOS universal links, future)

- `houseofvino://vintage/{id}`
- Opening a `.hovsync` file from another app → Receive changes

## State rules

- Each screen has a ViewModel exposing `StateFlow<UiState>`. Screens are stateless composables
  that take state and callbacks, so UI tests can drive them directly.
- `UiState` is one of Loading / Content / Empty / Error, and every list screen renders all four.
  The permission-denied state is a cross-cutting component.
