# Database schema

The local SQLite schema (SQLDelight). Every phone in a household has the same synced tables;
changes travel between phones through the change journal (docs/SYNC.md). There is no server
schema.

## Conventions

| Column | Type | Meaning |
|---|---|---|
| `id` | TEXT | client-generated UUID v4, stable for life |
| `collection_id` | TEXT | owning collection (the household's id once shared) |
| `created_at`, `updated_at` | INTEGER | epoch ms, client clock |
| `deleted_at` | INTEGER / bigint NULL | tombstone; rows are never hard-deleted while syncing |
| `rev` | INTEGER NULL / bigint | server revision; NULL locally until first sync |

- Money: `*_minor INTEGER` + `*_currency TEXT(3)` (ISO-4217). Never REAL.
- Calendar dates: `TEXT` ISO `YYYY-MM-DD` locally, `date` on the server.
- Enums: `TEXT` with CHECK constraints (stable names, never ordinals).
- Foreign keys are `ON DELETE RESTRICT`. Deletes are soft, so a restrict failure is a bug signal.

## Collections and households (server-authoritative)

```
collection(id, name, owner_id → auth.users, created_at, updated_at, deleted_at, rev)
collection_member(collection_id, user_id, role ∈ {owner, editor, viewer}, added_at)
collection_invite(id, collection_id, email, role, token_hash, expires_at, accepted_at, created_by)
```

## Wine identity

```
wine(
  id, collection_id,
  producer TEXT NOT NULL, name TEXT NULL,          -- cuvée; NULL = producer's base wine
  color ∈ {RED, WHITE, ROSE, ORANGE} NULL,
  style ∈ {STILL, SPARKLING, FORTIFIED, SWEET} NULL,
  country, region, sub_region, appellation, classification  TEXT NULL,
  notes TEXT NULL, + sync columns)

wine_grape(id, collection_id, wine_id → wine, grape TEXT NOT NULL, percent INTEGER NULL 0..100,
           position INTEGER, + sync columns)

vintage(
  id, collection_id, wine_id → wine,
  vintage_kind ∈ {YEAR, NV, UNKNOWN}, year INTEGER NULL  -- CHECK (kind=YEAR) = (year IS NOT NULL)
  drink_from INTEGER NULL, drink_until INTEGER NULL, drink_window_source TEXT NULL,
  abv_bp INTEGER NULL,                                  -- alcohol in basis points (13.5% = 1350)
  notes TEXT NULL, label_photo_id TEXT NULL, + sync columns)
  UNIQUE(wine_id, vintage_kind, year) WHERE deleted_at IS NULL

barcode(id, collection_id, code TEXT, symbology TEXT, wine_id → wine,
        vintage_id → vintage NULL, format_ml INTEGER NULL, + sync columns)
        -- many-to-one by design: one code can map to several vintages and formats

tag(id, collection_id, name TEXT, color TEXT NULL, + sync)   UNIQUE(collection_id, lower(name))
vintage_tag(id, collection_id, vintage_id, tag_id, + sync)
```

## Inventory

```
location(id, collection_id, parent_id → location NULL,
         name TEXT, kind ∈ {CELLAR, FRIDGE, RACK, SHELF, BIN, CASE, BOX, OTHER},
         grid_rows INTEGER NULL, grid_cols INTEGER NULL, sort_order INTEGER, notes, + sync)

lot(                                    -- acquisition batch
  id, collection_id, vintage_id → vintage, format_ml INTEGER NOT NULL,
  acquired_on TEXT NULL, source TEXT NULL, order_ref TEXT NULL,
  unit_price_minor INTEGER NULL, unit_price_currency TEXT NULL,   -- both or neither
  packaging ∈ {LOOSE, OWC, OC, CARTON} NULL, bottles_per_case INTEGER NULL,
  wishlist_item_id TEXT NULL, notes, + sync)

movement(                               -- append-only ledger, no UPDATE, no DELETE
  id, collection_id, lot_id → lot, location_id → location NULL,   -- NULL = unassigned
  delta INTEGER NOT NULL CHECK (delta <> 0),
  kind ∈ {PURCHASE, OPENING, CONSUME, SELL, GIFT_OUT, GIFT_IN, LOSS, TRANSFER_OUT,
          TRANSFER_IN, ADJUST, REVERSAL},
  occurred_at INTEGER, transfer_group_id TEXT NULL, reversal_of → movement NULL,
  amount_minor INTEGER NULL, amount_currency TEXT NULL,  -- sale price / gift value
  counterparty TEXT NULL, note TEXT NULL, device_id TEXT, tasting_id TEXT NULL,
  created_at, rev)                                       -- no updated_at/deleted_at

stock_level(lot_id, location_key, quantity)   -- LOCAL CACHE ONLY; trigger-maintained
                                              -- location_key = location_id or '' for unassigned
slot(id, collection_id, location_id → location, row INTEGER, col INTEGER,
     lot_id → lot NULL, + sync)          UNIQUE(location_id, row, col) WHERE deleted_at IS NULL
```

Sign rules (enforced in `core` and by CHECK): PURCHASE/OPENING/GIFT_IN/TRANSFER_IN > 0;
CONSUME/SELL/GIFT_OUT/LOSS/TRANSFER_OUT < 0; ADJUST/REVERSAL either sign. A transfer is two
rows sharing `transfer_group_id` with equal and opposite deltas, inserted in one transaction.

## Journal, wishlist, valuation, media

```
tasting(id, collection_id, vintage_id → vintage, tasted_on TEXT, rating INTEGER NULL 0..100,
        appearance, nose_intensity, sweetness, acidity, tannin, alcohol, body,
        flavour_intensity, finish  -- each ∈ {LOW, MEDIUM_MINUS, MEDIUM, MEDIUM_PLUS, HIGH} NULL
        aromas TEXT NULL,          -- comma-separated descriptors
        notes TEXT NULL, occasion TEXT NULL, food_pairing TEXT NULL, companions TEXT NULL,
        place TEXT NULL, format_ml INTEGER NULL, movement_id TEXT NULL,
        would_buy_again INTEGER NULL, + sync)

wishlist_item(id, collection_id, vintage_id NULL, wine_id NULL,
        producer TEXT NULL, name TEXT NULL, vintage_kind NULL, year NULL,   -- free-form target
        format_ml INTEGER NULL, desired_qty INTEGER NOT NULL DEFAULT 1,
        target_price_minor NULL, target_price_currency NULL,
        priority ∈ {LOW, MEDIUM, HIGH, MUST} , status ∈ {WANTED, WATCHING, ORDERED, ACQUIRED, CANCELLED},
        notes, acquired_lot_id NULL, + sync)
        CHECK (vintage_id IS NOT NULL OR wine_id IS NOT NULL OR producer IS NOT NULL)

merchant_link(id, collection_id, wishlist_item_id, merchant TEXT, url TEXT NULL,
        last_price_minor NULL, last_price_currency NULL, checked_on TEXT NULL, + sync)

valuation(id, collection_id, vintage_id, format_ml, amount_minor NOT NULL,
        amount_currency NOT NULL, source TEXT NOT NULL, as_of TEXT NOT NULL, note, + sync)

photo(id, collection_id, owner_type ∈ {VINTAGE, TASTING, LOT}, owner_id, local_path NULL,
      remote_path NULL, mime TEXT, width INTEGER, height INTEGER, bytes INTEGER, + sync)

exchange_rate(id, collection_id, base TEXT, quote TEXT, rate_e8 INTEGER, as_of TEXT,
              source TEXT, + sync)

saved_search(id, collection_id, name, scope, query TEXT, filters_json TEXT, sort TEXT, + sync)
```

## Local-only tables

```
recent_query(query TEXT, scope TEXT, used_at INTEGER)  PRIMARY KEY(query, scope)
setting(key TEXT PRIMARY KEY, value TEXT)
journal(seq PK AUTOINCREMENT, device_id, device_seq, hlc, collection_id, tbl, row_id,
        body TEXT,   -- the change as sent on the wire (docs/SYNC.md)
        received_at)  UNIQUE(device_id, device_seq)
row_clock(tbl, row_id, state TEXT)   -- per-field {value, hlc} merge state  PRIMARY KEY(tbl, row_id)
peer(device_id PK, collection_id, name, known_vector TEXT, first_seen_at, last_heard_at,
     last_sent_at, removed_at NULL)
household_key(key_id PK, collection_id, secret TEXT, created_at, retired_at NULL)
              -- at most one current (retired_at NULL) key per collection
sync_conflict(id, entity, entity_id, collection_id,
              local_payload,    -- the values that lost (the other phone's)
              server_payload,   -- the values every phone shows now
              reason, detected_at, resolved_at NULL)
search_fts      FTS4(doc_key, scope, producer, name, vintage, place, grapes, tags, notes)
search_terms    fts4aux(search_fts)
search_doc(doc_key TEXT PK, scope, entity_id, docid INTEGER)
```

The `rev` column on synced tables is left over from the server design (schema v1) and is never
synced. Migration 2 dropped `sync_outbox` and `sync_cursor`.

## Key indexes

- `vintage(wine_id)`, `lot(vintage_id)`, `movement(lot_id)`, `movement(location_id)`, `stock_level(lot_id)`.
- `tasting(vintage_id, tasted_on)`, `wishlist_item(status, priority)`.
- `barcode(code)`, `journal(device_id, device_seq)`, `journal(hlc)`, `journal(tbl, row_id)`.

## Migrations

- Local: `data/src/commonMain/sqldelight/com/houseofvino/data/db/N.sqm`. `DatabaseMigrationTest` runs
  1→latest and checks that the schema equals a fresh create.
- On first open after an upgrade, rows written before the journal existed are journaled once,
  so they reach a partner when the cellar is first shared.
