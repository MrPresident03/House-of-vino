# Household sync protocol

How House of Vino phones share one cellar without a server. The decision and its trade-offs
are in ADR-13; this file is the exact format, so other transports and future versions stay
compatible.

## Concepts

| Term | Meaning |
|---|---|
| Household | A shared cellar. Its id is the local `collection.id`. |
| Device | One install of the app. Its id is the `device_id` setting (a UUID). |
| Change | One journal entry: the fields of one row that one device changed in one write. |
| HLC | Hybrid logical clock. Orders changes across devices even when wall clocks disagree. |
| Version vector | For each device, the highest `device_seq` a phone has seen. |
| Bundle | A batch of changes (plus photos) sent from one phone to others. |

## Hybrid logical clock

Encoded as a string that sorts in clock order:

```
<millis, 15 digits>-<counter, 6 digits>-<device id>
000001760000000-000003-4f1c…
```

- Local event: `millis = max(wall, last.millis)`; the counter resets to 0 when millis
  advances, otherwise increments.
- Receiving a change advances the clock to at least the received value, so a phone's next
  edit always sorts after everything it has seen.
- Ties are impossible across devices because the device id is part of the value.

## Changes

Mutable tables (everything except `movement`):

```json
{"d":"<device>","s":42,"h":"<hlc>","c":"<household>","t":"vintage","r":"<row id>",
 "w":[["drink_until",2035,"<base hlc or null>"],["notes",null,"<base hlc>"]]}
```

- `w` lists only the fields this write changed: `[column, value, base]`. Values are JSON
  strings, integers or null. `base` is the clock of the value the author replaced (null if
  the field was new to them).
- The first change for a row contains every column, so the row can be created anywhere.
- The `rev` column is never synced; `photo.local_path` is device-specific and never synced.

Append-only table `movement`:

```json
{"d":"<device>","s":43,"h":"<hlc>","c":"<household>","t":"movement","r":"<row id>","row":{…every column…}}
```

### Merge rules (receiver)

For each field write `(value, clock, base)` against the local field state `(current, cur)`:

1. No local state → take the write.
2. `clock == cur` → duplicate, ignore.
3. `base == cur` → the author saw the current value: take the write.
4. Otherwise the two writes were made independently. The higher `(clock)` wins. If the values
   differ, the losing value is recorded in `sync_conflict` so the user can choose.

Movements are inserted if absent. After every import, negative stock is recorded as a stock
conflict. Changes are applied in HLC order inside one transaction; a bundle is either fully
applied or not at all.

## Bundles

Plaintext (UTF-8 JSON):

```json
{"format":"hov-bundle","v":1,"household":"<id>","from":"<device>","fromName":"Ana's iPhone",
 "createdAt":1760000000000,"vector":{"<device>":43},
 "changes":[…],"photos":[{"id":"<photo id>","data":"<base64 JPEG>"}]}
```

- `vector` is the sender's version vector. The receiver stores it, so its next bundle to the
  sender contains only what the sender lacks.
- An export contains every change the receiving phones lack, for every device, so delivery is
  causal: a phone never receives an edit without the row it edits.

### Envelope

```
"HOVS" | 0x01 | key id (8 bytes) | nonce (24 bytes) | ciphertext ‖ Poly1305 tag (16 bytes)
```

- Cipher: XChaCha20-Poly1305 (IETF construction), random 24-byte nonce, the household key.
- Associated data: the first 13 bytes (magic, version, key id), so the header can't be swapped.
- Key id: first 8 bytes of SHA-256(`"hov-keyid"` ‖ key).

### Sync file (text armour)

```
House of Vino: encrypted cellar changes. Open House of Vino › Sharing › Receive changes.
-----BEGIN HOUSE OF VINO CHANGES-----
<base64 of the envelope, 76 characters per line>
-----END HOUSE OF VINO CHANGES-----
```

The file name ends in `.hovsync`. Messaging apps and email pass it through unchanged.

## Invites

```
HOV1-<base64url of {"v":1,"c":"<household>","n":"<name>","k":"<base64url key>","d":"<device>","dn":"<device name>"}>
```

Shown as a QR code and as copyable text. The invite **is** the key: it should be shown in
person. Both phones then display the same safety code, the first 6 bytes of
SHA-256(`"hov-safety"` ‖ key) as three groups of four digits, so partners can confirm they
joined the same household.

## Joining

1. The joining phone validates the invite and stores the key.
2. If it already has a cellar, every row is re-keyed to the household id (one transaction,
   with ledger immutability lifted only for that transaction).
3. Its journal is rebuilt from the current rows with fresh clocks, so its whole cellar goes
   to the partner in the next bundle.
4. The inviter is recorded as a peer. The first bundle from the joining phone tells the
   inviter about it.

## Removing a partner

The remaining phone generates a new key, retires the old one and marks the partner removed.
Bundles from removed devices, and bundles under retired keys, are rejected. Other remaining
phones re-join with a new invite.
