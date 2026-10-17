# jvm-android: failure

- commit: 44f2a6671615f8893c9d240e4ed4f39218f9c5bd
- run: https://github.com/MrPresident03/House-of-vino/actions/runs/39013590434
- tests: 78 run, 0 failed, 0 skipped

## Compiler errors (494)
```
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/io/ImportExport.kt:168:46 Smart cast to 'String' is impossible, because 'barcode' is a public API property declared in different module.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/io/ImportExport.kt:168:61 Smart cast to 'String' is impossible, because 'barcode' is a public API property declared in different module.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/CellarRepository.kt:135:101 Overload resolution ambiguity between candidates:
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/CellarRepository.kt:144:79 Overload resolution ambiguity between candidates:
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/CellarRepository.kt:146:81 Overload resolution ambiguity between candidates:
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/CellarRepository.kt:147:79 Cannot infer type for type parameter 'R'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/CellarRepository.kt:147:88 Overload resolution ambiguity between candidates:
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/CellarRepository.kt:191:118 Overload resolution ambiguity between candidates:
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/CellarRepository.kt:454:119 Overload resolution ambiguity between candidates:
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/CellarRepository.kt:455:27 Unresolved reference 'transferGroupId'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/CellarRepository.kt:455:44 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/CellarRepository.kt:455:44 Cannot infer type for type parameter 'R'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/CellarRepository.kt:455:50 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/CellarRepository.kt:455:109 Cannot infer type for type parameter 'R'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/CellarRepository.kt:455:118 Overload resolution ambiguity between candidates:
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/CellarRepository.kt:456:16 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/CellarRepository.kt:457:56 Unresolved reference 'it'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/CellarRepository.kt:458:48 Unresolved reference 'collectionId'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/CellarRepository.kt:459:66 Unresolved reference 'lotId'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/CellarRepository.kt:461:27 Unresolved reference 'it'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/CellarRepository.kt:461:84 Unresolved reference 'lotId'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/CellarRepository.kt:461:91 Unresolved reference 'it'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/CellarRepository.kt:530:83 Overload resolution ambiguity between candidates:
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/JournalRepository.kt:30:87 Overload resolution ambiguity between candidates:
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/JournalRepository.kt:32:119 Overload resolution ambiguity between candidates:
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/LocationRepository.kt:30:81 Overload resolution ambiguity between candidates:
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/LocationRepository.kt:32:88 Overload resolution ambiguity between candidates:
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:28:32 Unresolved reference 'Exchange_rate'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:29:32 Unresolved reference 'Wine_grape'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:30:32 Unresolved reference 'Wishlist_item'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:31:32 Unresolved reference 'Location'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:32:32 Unresolved reference 'Lot'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:33:32 Unresolved reference 'Merchant_link'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:34:32 Unresolved reference 'Movement'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:35:32 Unresolved reference 'Tag'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:36:32 Unresolved reference 'Tasting'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:37:32 Unresolved reference 'Valuation'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:38:32 Unresolved reference 'Vintage'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:39:32 Unresolved reference 'Wine'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:44:14 Unresolved reference 'WineRow'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:44:44 Unresolved reference 'Wine_grape'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:44:59 Inapplicable candidate(s): constructor(id: String, collectionId: String, producer: String, name: String?, color: WineColor?, style: WineStyle?, country: String?, region: String?, subRegion: String?, appellation: String?, classification: String?, grapes: List<GrapeShare>, notes: String?, createdAt: Long, updatedAt: Long): Wine
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:45:10 Unresolved reference 'id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:45:29 Unresolved reference 'collection_id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:45:55 Unresolved reference 'producer'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:45:72 Unresolved reference 'name'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:46:13 Unresolved reference 'color'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:46:20 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:46:20 Cannot infer type for type parameter 'R'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:46:24 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:46:59 Unresolved reference 'style'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:46:66 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:46:66 Cannot infer type for type parameter 'R'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:46:70 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:47:15 Unresolved reference 'country'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:47:33 Unresolved reference 'region'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:47:53 Unresolved reference 'sub_region'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:47:79 Unresolved reference 'appellation'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:48:22 Unresolved reference 'classification'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:48:71 Unresolved reference 'it'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:48:81 Unresolved reference 'it'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:49:13 Unresolved reference 'notes'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:49:32 Unresolved reference 'created_at'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:49:56 Unresolved reference 'updated_at'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:52:14 Unresolved reference 'VintageRow'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:53:10 Unresolved reference 'id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:53:29 Unresolved reference 'collection_id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:53:53 Unresolved reference 'wine_id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:54:39 Unresolved reference 'vintage_kind'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:54:53 Unresolved reference 'year'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:55:17 Unresolved reference 'drink_from'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:55:51 Unresolved reference 'drink_until'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:55:93 Unresolved reference 'drink_window_source'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:56:22 Unresolved reference 'abv_bp'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:56:47 Unresolved reference 'notes'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:56:69 Unresolved reference 'label_photo_id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:57:17 Unresolved reference 'created_at'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:57:41 Unresolved reference 'updated_at'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:60:14 Unresolved reference 'LotRow'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:60:34 Inapplicable candidate(s): constructor(id: String, collectionId: String, vintageId: String, formatMl: Int, acquiredOn: IsoDate?, source: String?, orderRef: String?, unitPrice: Money?, packaging: Packaging?, bottlesPerCase: Int?, wishlistItemId: String?, notes: String?, createdAt: Long, updatedAt: Long): Lot
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:61:10 Unresolved reference 'id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:61:29 Unresolved reference 'collection_id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:61:56 Unresolved reference 'vintage_id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:61:79 Unresolved reference 'format_ml'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:62:38 Unresolved reference 'acquired_on'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:62:61 Unresolved reference 'source'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:62:80 Unresolved reference 'order_ref'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:63:23 Unresolved reference 'unit_price_minor'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:63:41 Unresolved reference 'unit_price_currency'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:63:75 Unresolved reference 'packaging'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:63:86 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:63:86 Cannot infer type for type parameter 'R'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:63:90 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:64:22 Unresolved reference 'bottles_per_case'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:64:66 Unresolved reference 'wishlist_item_id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:64:92 Unresolved reference 'notes'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:65:17 Unresolved reference 'created_at'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:65:41 Unresolved reference 'updated_at'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:68:14 Unresolved reference 'LocationRow'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:69:10 Unresolved reference 'id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:69:29 Unresolved reference 'collection_id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:69:55 Unresolved reference 'parent_id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:69:73 Unresolved reference 'name'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:69:107 Unresolved reference 'kind'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:70:16 Unresolved reference 'grid_rows'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:70:47 Unresolved reference 'grid_cols'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:70:79 Unresolved reference 'sort_order'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:70:107 Unresolved reference 'notes'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:71:17 Unresolved reference 'created_at'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:71:41 Unresolved reference 'updated_at'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:74:14 Unresolved reference 'MovementRow'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:75:10 Unresolved reference 'id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:75:29 Unresolved reference 'collection_id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:75:52 Unresolved reference 'lot_id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:75:73 Unresolved reference 'location_id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:75:94 Unresolved reference 'delta'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:76:33 Unresolved reference 'kind'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:76:53 Unresolved reference 'occurred_at'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:76:84 Unresolved reference 'transfer_group_id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:77:18 Unresolved reference 'reversal_of'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:77:46 Unresolved reference 'amount_minor'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:77:60 Unresolved reference 'amount_currency'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:77:93 Unresolved reference 'counterparty'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:78:12 Unresolved reference 'note'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:78:29 Unresolved reference 'device_id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:78:52 Unresolved reference 'tasting_id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:78:76 Unresolved reference 'created_at'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:81:14 Unresolved reference 'TastingRow'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:81:38 Inapplicable candidate(s): constructor(id: String, collectionId: String, vintageId: String, tastedOn: IsoDate, rating: Int?, structured: StructuredNotes, notes: String?, occasion: String?, foodPairing: String?, companions: String?, place: String?, formatMl: Int?, movementId: String?, wouldBuyAgain: Boolean?, createdAt: Long, updatedAt: Long): Tasting
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:82:10 Unresolved reference 'id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:82:29 Unresolved reference 'collection_id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:82:56 Unresolved reference 'vintage_id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:82:93 Unresolved reference 'tasted_on'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:83:14 Unresolved reference 'rating'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:85:22 Unresolved reference 'appearance'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:85:58 Unresolved reference 'nose_intensity'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:85:94 Unresolved reference 'sweetness'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:86:19 Unresolved reference 'acidity'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:86:45 Unresolved reference 'tannin'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:86:71 Unresolved reference 'alcohol'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:86:95 Unresolved reference 'body'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:87:28 Unresolved reference 'flavour_intensity'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:87:64 Unresolved reference 'finish'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:88:18 Unresolved reference 'aromas'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:90:13 Unresolved reference 'notes'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:90:31 Unresolved reference 'occasion'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:90:55 Unresolved reference 'food_pairing'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:90:82 Unresolved reference 'companions'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:90:102 Unresolved reference 'place'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:91:16 Unresolved reference 'format_ml'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:91:49 Unresolved reference 'movement_id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:91:78 Unresolved reference 'would_buy_again'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:91:95 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:91:95 Cannot infer type for type parameter 'R'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:91:99 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:92:17 Unresolved reference 'created_at'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:92:41 Unresolved reference 'updated_at'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:97:14 Unresolved reference 'Wishlist_item'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:97:41 Inapplicable candidate(s): constructor(id: String, collectionId: String, vintageId: String?, wineId: String?, producer: String?, name: String?, vintage: VintageYear?, formatMl: Int?, desiredQuantity: Int, targetPrice: Money?, priority: WishlistPriority, status: WishlistStatus, notes: String?, acquiredLotId: String?, createdAt: Long, updatedAt: Long): WishlistItem
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:98:10 Unresolved reference 'id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:98:29 Unresolved reference 'collection_id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:98:56 Unresolved reference 'vintage_id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:98:77 Unresolved reference 'wine_id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:98:97 Unresolved reference 'producer'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:98:114 Unresolved reference 'name'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:99:15 Unresolved reference 'vintage_kind'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:99:29 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:99:29 Cannot infer type for type parameter 'R'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:99:33 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:99:63 Unresolved reference 'year'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:99:92 Unresolved reference 'format_ml'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:100:23 Unresolved reference 'desired_qty'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:100:64 Unresolved reference 'target_price_minor'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:100:84 Unresolved reference 'target_price_currency'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:101:41 Unresolved reference 'priority'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:101:84 Unresolved reference 'status'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:101:101 Unresolved reference 'notes'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:102:21 Unresolved reference 'acquired_lot_id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:102:50 Unresolved reference 'created_at'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:102:74 Unresolved reference 'updated_at'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:105:14 Unresolved reference 'MerchantLinkRow'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:106:10 Unresolved reference 'id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:106:31 Unresolved reference 'wishlist_item_id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:106:60 Unresolved reference 'merchant'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:106:76 Unresolved reference 'url'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:107:23 Unresolved reference 'last_price_minor'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:107:41 Unresolved reference 'last_price_currency'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:107:95 Unresolved reference 'checked_on'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:110:14 Unresolved reference 'ValuationRow'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:111:10 Unresolved reference 'id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:111:29 Unresolved reference 'collection_id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:111:56 Unresolved reference 'vintage_id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:111:79 Unresolved reference 'format_ml'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:112:20 Unresolved reference 'amount_minor'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:112:34 Unresolved reference 'amount_currency'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:112:63 Unresolved reference 'source'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:112:92 Unresolved reference 'as_of'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:112:107 Unresolved reference 'note'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:115:14 Unresolved reference 'TagRow'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:115:43 Unresolved reference 'id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:115:62 Unresolved reference 'collection_id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:115:84 Unresolved reference 'name'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:115:98 Unresolved reference 'color'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:117:14 Unresolved reference 'Exchange_rate'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:118:34 Unresolved reference 'base'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:118:57 Unresolved reference 'quote'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:118:65 Unresolved reference 'rate_e8'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:118:88 Unresolved reference 'as_of'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:118:96 Unresolved reference 'source'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/PhotoRepository.kt:4:32 Unresolved reference 'Photo'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/PhotoRepository.kt:18:60 Unresolved reference 'Photo'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/PhotoRepository.kt:20:27 Unresolved reference 'Photo'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/SettingsRepository.kt:57:91 Overload resolution ambiguity between candidates:
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/WishlistRepository.kt:34:94 Overload resolution ambiguity between candidates:
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/WishlistRepository.kt:36:107 Overload resolution ambiguity between candidates:
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/WishlistRepository.kt:87:70 Smart cast to 'String' is impossible, because 'vintageId' is a public API property declared in different module.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/WishlistRepository.kt:88:64 Smart cast to 'String' is impossible, because 'wineId' is a public API property declared in different module.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/search/SearchEngine.kt:239:126 Smart cast to 'Int' is impossible, because 'minRating' is a public API property declared in different module.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/search/SearchEngine.kt:309:67 Smart cast to 'Int' is impossible, because 'minRating' is a public API property declared in different module.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/sync/Outbox.kt:4:32 Unresolved reference 'Sync_outbox'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/sync/Outbox.kt:49:20 Unresolved reference 'Sync_outbox'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/sync/Outbox.kt:49:97 Unresolved reference 'payload'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/sync/SyncEngine.kt:4:32 Unresolved reference 'Sync_outbox'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/sync/SyncEngine.kt:165:30 Unresolved reference 'Sync_outbox'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/sync/SyncEngine.kt:166:42 Unresolved reference 'entity'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/sync/SyncEngine.kt:167:36 Unresolved reference 'op_id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/sync/SyncEngine.kt:168:52 Unresolved reference 'entity_id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/sync/SyncEngine.kt:169:83 Unresolved reference 'entity'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/sync/SyncEngine.kt:169:105 Unresolved reference 'entity_id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/sync/SyncEngine.kt:169:128 Unresolved reference 'base_rev'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/sync/SyncEngine.kt:177:71 Unresolved reference 'Sync_outbox'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/sync/SyncEngine.kt:178:42 Unresolved reference 'entity'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/sync/SyncEngine.kt:179:59 Unresolved reference 'entity'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/sync/SyncEngine.kt:179:70 Unresolved reference 'entity_id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/sync/SyncEngine.kt:179:125 Unresolved reference 'payload'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/sync/SyncEngine.kt:180:46 Unresolved reference 'entity'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/sync/SyncEngine.kt:180:57 Unresolved reference 'entity_id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/sync/SyncEngine.kt:181:36 Unresolved reference 'op_id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/sync/SyncEngine.kt:182:47 Unresolved reference 'entity'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/sync/SyncEngine.kt:182:58 Unresolved reference 'entity_id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/sync/SyncEngine.kt:184:52 Unresolved reference 'entity'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/sync/SyncEngine.kt:184:75 Unresolved reference 'entity_id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/sync/SyncEngine.kt:184:105 Unresolved reference 'collection_id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/sync/SyncEngine.kt:191:31 Unresolved reference 'entity_id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/sync/SyncEngine.kt:195:35 Unresolved reference 'Sync_outbox'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/sync/SyncEngine.kt:196:26 Unresolved reference 'attempts'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/sync/SyncEngine.kt:199:96 Unresolved reference 'op_id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/io/ImportExport.kt:168:46 Smart cast to 'String' is impossible, because 'barcode' is a public API property declared in different module.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/io/ImportExport.kt:168:61 Smart cast to 'String' is impossible, because 'barcode' is a public API property declared in different module.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/CellarRepository.kt:135:101 Overload resolution ambiguity between candidates:
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/CellarRepository.kt:144:79 Overload resolution ambiguity between candidates:
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/CellarRepository.kt:146:81 Overload resolution ambiguity between candidates:
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/CellarRepository.kt:147:79 Cannot infer type for type parameter 'R'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/CellarRepository.kt:147:88 Overload resolution ambiguity between candidates:
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/CellarRepository.kt:191:118 Overload resolution ambiguity between candidates:
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/CellarRepository.kt:454:119 Overload resolution ambiguity between candidates:
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/CellarRepository.kt:455:27 Unresolved reference 'transferGroupId'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/CellarRepository.kt:455:44 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/CellarRepository.kt:455:44 Cannot infer type for type parameter 'R'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/CellarRepository.kt:455:50 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/CellarRepository.kt:455:109 Cannot infer type for type parameter 'R'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/CellarRepository.kt:455:118 Overload resolution ambiguity between candidates:
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/CellarRepository.kt:456:16 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/CellarRepository.kt:457:56 Unresolved reference 'it'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/CellarRepository.kt:458:48 Unresolved reference 'collectionId'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/CellarRepository.kt:459:66 Unresolved reference 'lotId'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/CellarRepository.kt:461:27 Unresolved reference 'it'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/CellarRepository.kt:461:84 Unresolved reference 'lotId'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/CellarRepository.kt:461:91 Unresolved reference 'it'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/CellarRepository.kt:530:83 Overload resolution ambiguity between candidates:
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/JournalRepository.kt:30:87 Overload resolution ambiguity between candidates:
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/JournalRepository.kt:32:119 Overload resolution ambiguity between candidates:
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/LocationRepository.kt:30:81 Overload resolution ambiguity between candidates:
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/LocationRepository.kt:32:88 Overload resolution ambiguity between candidates:
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:28:32 Unresolved reference 'Exchange_rate'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:29:32 Unresolved reference 'Wine_grape'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:30:32 Unresolved reference 'Wishlist_item'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:31:32 Unresolved reference 'Location'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:32:32 Unresolved reference 'Lot'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:33:32 Unresolved reference 'Merchant_link'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:34:32 Unresolved reference 'Movement'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:35:32 Unresolved reference 'Tag'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:36:32 Unresolved reference 'Tasting'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:37:32 Unresolved reference 'Valuation'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:38:32 Unresolved reference 'Vintage'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:39:32 Unresolved reference 'Wine'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:44:14 Unresolved reference 'WineRow'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:44:44 Unresolved reference 'Wine_grape'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:44:59 Inapplicable candidate(s): constructor(id: String, collectionId: String, producer: String, name: String?, color: WineColor?, style: WineStyle?, country: String?, region: String?, subRegion: String?, appellation: String?, classification: String?, grapes: List<GrapeShare>, notes: String?, createdAt: Long, updatedAt: Long): Wine
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:45:10 Unresolved reference 'id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:45:29 Unresolved reference 'collection_id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:45:55 Unresolved reference 'producer'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:45:72 Unresolved reference 'name'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:46:13 Unresolved reference 'color'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:46:20 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:46:20 Cannot infer type for type parameter 'R'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:46:24 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:46:59 Unresolved reference 'style'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:46:66 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:46:66 Cannot infer type for type parameter 'R'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:46:70 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:47:15 Unresolved reference 'country'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:47:33 Unresolved reference 'region'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:47:53 Unresolved reference 'sub_region'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:47:79 Unresolved reference 'appellation'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:48:22 Unresolved reference 'classification'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:48:71 Unresolved reference 'it'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:48:81 Unresolved reference 'it'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:49:13 Unresolved reference 'notes'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:49:32 Unresolved reference 'created_at'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:49:56 Unresolved reference 'updated_at'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:52:14 Unresolved reference 'VintageRow'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:53:10 Unresolved reference 'id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:53:29 Unresolved reference 'collection_id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:53:53 Unresolved reference 'wine_id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:54:39 Unresolved reference 'vintage_kind'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:54:53 Unresolved reference 'year'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:55:17 Unresolved reference 'drink_from'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:55:51 Unresolved reference 'drink_until'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:55:93 Unresolved reference 'drink_window_source'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:56:22 Unresolved reference 'abv_bp'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:56:47 Unresolved reference 'notes'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:56:69 Unresolved reference 'label_photo_id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:57:17 Unresolved reference 'created_at'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:57:41 Unresolved reference 'updated_at'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:60:14 Unresolved reference 'LotRow'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:60:34 Inapplicable candidate(s): constructor(id: String, collectionId: String, vintageId: String, formatMl: Int, acquiredOn: IsoDate?, source: String?, orderRef: String?, unitPrice: Money?, packaging: Packaging?, bottlesPerCase: Int?, wishlistItemId: String?, notes: String?, createdAt: Long, updatedAt: Long): Lot
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:61:10 Unresolved reference 'id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:61:29 Unresolved reference 'collection_id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:61:56 Unresolved reference 'vintage_id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:61:79 Unresolved reference 'format_ml'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:62:38 Unresolved reference 'acquired_on'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:62:61 Unresolved reference 'source'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:62:80 Unresolved reference 'order_ref'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:63:23 Unresolved reference 'unit_price_minor'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:63:41 Unresolved reference 'unit_price_currency'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:63:75 Unresolved reference 'packaging'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:63:86 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:63:86 Cannot infer type for type parameter 'R'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:63:90 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:64:22 Unresolved reference 'bottles_per_case'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:64:66 Unresolved reference 'wishlist_item_id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:64:92 Unresolved reference 'notes'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:65:17 Unresolved reference 'created_at'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:65:41 Unresolved reference 'updated_at'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:68:14 Unresolved reference 'LocationRow'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:69:10 Unresolved reference 'id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:69:29 Unresolved reference 'collection_id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:69:55 Unresolved reference 'parent_id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:69:73 Unresolved reference 'name'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:69:107 Unresolved reference 'kind'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:70:16 Unresolved reference 'grid_rows'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:70:47 Unresolved reference 'grid_cols'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:70:79 Unresolved reference 'sort_order'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:70:107 Unresolved reference 'notes'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:71:17 Unresolved reference 'created_at'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:71:41 Unresolved reference 'updated_at'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:74:14 Unresolved reference 'MovementRow'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:75:10 Unresolved reference 'id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:75:29 Unresolved reference 'collection_id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:75:52 Unresolved reference 'lot_id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:75:73 Unresolved reference 'location_id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:75:94 Unresolved reference 'delta'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:76:33 Unresolved reference 'kind'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:76:53 Unresolved reference 'occurred_at'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:76:84 Unresolved reference 'transfer_group_id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:77:18 Unresolved reference 'reversal_of'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:77:46 Unresolved reference 'amount_minor'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:77:60 Unresolved reference 'amount_currency'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:77:93 Unresolved reference 'counterparty'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:78:12 Unresolved reference 'note'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:78:29 Unresolved reference 'device_id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:78:52 Unresolved reference 'tasting_id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:78:76 Unresolved reference 'created_at'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:81:14 Unresolved reference 'TastingRow'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:81:38 Inapplicable candidate(s): constructor(id: String, collectionId: String, vintageId: String, tastedOn: IsoDate, rating: Int?, structured: StructuredNotes, notes: String?, occasion: String?, foodPairing: String?, companions: String?, place: String?, formatMl: Int?, movementId: String?, wouldBuyAgain: Boolean?, createdAt: Long, updatedAt: Long): Tasting
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:82:10 Unresolved reference 'id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:82:29 Unresolved reference 'collection_id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:82:56 Unresolved reference 'vintage_id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:82:93 Unresolved reference 'tasted_on'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:83:14 Unresolved reference 'rating'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:85:22 Unresolved reference 'appearance'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:85:58 Unresolved reference 'nose_intensity'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:85:94 Unresolved reference 'sweetness'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:86:19 Unresolved reference 'acidity'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:86:45 Unresolved reference 'tannin'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:86:71 Unresolved reference 'alcohol'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:86:95 Unresolved reference 'body'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:87:28 Unresolved reference 'flavour_intensity'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:87:64 Unresolved reference 'finish'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:88:18 Unresolved reference 'aromas'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:90:13 Unresolved reference 'notes'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:90:31 Unresolved reference 'occasion'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:90:55 Unresolved reference 'food_pairing'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:90:82 Unresolved reference 'companions'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:90:102 Unresolved reference 'place'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:91:16 Unresolved reference 'format_ml'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:91:49 Unresolved reference 'movement_id'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:91:78 Unresolved reference 'would_buy_again'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/repo/Mappers.kt:91:95 Cannot infer type for type parameter 'T'. Specify it explicitly.
```

## Gradle failures
```
* What went wrong:
Execution failed for task ':androidApp:checkDebugAarMetadata' (registered by plugin 'com.android.internal.application').
> A failure occurred while executing com.android.build.gradle.internal.tasks.CheckAarMetadataWorkAction
   > 18 issues were found when checking AAR metadata:
     
       1.  Dependency 'io.coil-kt.coil3:coil-compose-android:3.6.3' requires libraries and applications that
           depend on it to compile against version 37 or later of the
           Android APIs.
---
* What went wrong:
Execution failed for task ':data:compileKotlinJvm' (registered in build file 'data/build.gradle.kts').
> A failure occurred while executing org.jetbrains.kotlin.compilerRunner.btapi.BuildToolsApiCompilationWork
   > Kotlin compiler: SMARTCAST_IMPOSSIBLE
       Kotlin compiler error
         Smart cast to 'String' is impossible, because 'barcode' is a public API property declared in different module.
         Location: /home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/io/ImportExport.kt line 168

---
* What went wrong:
Execution failed for task ':data:compileAndroidMain' (registered by plugin 'com.android.internal.kotlin.multiplatform.library').
> A failure occurred while executing org.jetbrains.kotlin.compilerRunner.btapi.BuildToolsApiCompilationWork
   > Kotlin compiler: SMARTCAST_IMPOSSIBLE
       Kotlin compiler error
         Smart cast to 'String' is impossible, because 'barcode' is a public API property declared in different module.
         Location: /home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/io/ImportExport.kt line 168

```

