package com.houseofvino.data.repo

import com.houseofvino.core.model.MerchantLink
import com.houseofvino.core.model.VintageYear
import com.houseofvino.core.model.WishlistItem
import com.houseofvino.core.model.WishlistPriority
import com.houseofvino.core.model.WishlistStatus
import com.houseofvino.core.money.Money
import com.houseofvino.core.time.IsoDate
import com.houseofvino.data.db.LocalDatabase
import com.houseofvino.data.sync.SyncTables

data class WishlistInput(
    val vintageId: String? = null,
    val wineId: String? = null,
    val producer: String? = null,
    val name: String? = null,
    val vintage: VintageYear? = null,
    val formatMl: Int? = null,
    val desiredQuantity: Int = 1,
    val targetPrice: Money? = null,
    val priority: WishlistPriority = WishlistPriority.MEDIUM,
    val notes: String? = null,
)

data class MerchantLinkInput(val merchant: String, val url: String?, val lastPrice: Money?, val checkedOn: IsoDate?)

class InvalidStatusChangeException(from: WishlistStatus, to: WishlistStatus) :
    IllegalStateException("Can't change a wishlist item from ${from.name.lowercase()} to ${to.name.lowercase()}")

class WishlistRepository(private val database: LocalDatabase, private val cellar: CellarRepository) {
    private val journal get() = database.journal

    fun item(id: String): WishlistItem? = journal.wishlistItemById(id).executeAsOneOrNull()?.toDomain()

    fun links(itemId: String): List<MerchantLink> = journal.linksForItem(itemId).executeAsList().map { it.toDomain() }

    fun add(collectionId: String, input: WishlistInput): String = database.write {
        validate(input)
        val id = database.newId()
        journal.insertWishlistItem(
            id, collectionId, input.vintageId, input.wineId, input.producer.clean(), input.name.clean(), input.vintage?.kind,
            input.vintage?.yearOrNull?.toLong(), input.formatMl?.toLong(), input.desiredQuantity.toLong(), input.targetPrice?.minor,
            input.targetPrice?.currency?.code, input.priority.name, WishlistStatus.WANTED.name, input.notes.clean(), null, now, now,
        )
        touched(SyncTables.WISHLIST_ITEM, id)
        id
    }

    fun update(id: String, input: WishlistInput) = database.write {
        validate(input)
        val current = requireNotNull(item(id)) { "Wishlist item not found" }
        journal.updateWishlistItem(
            vintageId = input.vintageId, wineId = input.wineId, producer = input.producer.clean(), name = input.name.clean(),
            vintageKind = input.vintage?.kind, year = input.vintage?.yearOrNull?.toLong(), formatMl = input.formatMl?.toLong(),
            desiredQty = input.desiredQuantity.toLong(), targetPriceMinor = input.targetPrice?.minor,
            targetPriceCurrency = input.targetPrice?.currency?.code, priority = input.priority.name, status = current.status.name,
            notes = input.notes.clean(), acquiredLotId = current.acquiredLotId, updatedAt = now, id = id,
        )
        touched(SyncTables.WISHLIST_ITEM, id)
    }

    fun setStatus(id: String, status: WishlistStatus) = database.write {
        val current = requireNotNull(item(id)) { "Wishlist item not found" }
        if (current.status == status) return@write
        if (!current.status.canTransitionTo(status)) throw InvalidStatusChangeException(current.status, status)
        require(status != WishlistStatus.ACQUIRED) { "Use “Add to cellar” to mark an item as acquired" }
        writeItem(current.copy(status = status), now)
        touched(SyncTables.WISHLIST_ITEM, id)
    }

    fun delete(id: String) = database.write {
        journal.softDeleteWishlistItem(now, id)
        touched(SyncTables.WISHLIST_ITEM, id)
    }

    /**
     * Converts a wishlist item into stock in one transaction: creates the wine/vintage when the
     * item was free-form, records the purchase, links lot ↔ item and marks the item acquired.
     * Returns the vintage id.
     */
    fun convertToInventory(itemId: String, target: WineTarget?, vintage: VintageInput?, lot: LotInput, allowDuplicateWine: Boolean = false): String =
        database.write {
            val item = requireNotNull(item(itemId)) { "Wishlist item not found" }
            if (!item.status.canTransitionTo(WishlistStatus.ACQUIRED)) throw InvalidStatusChangeException(item.status, WishlistStatus.ACQUIRED)
            val resolvedTarget = target ?: when {
                item.vintageId != null -> WineTarget.ExistingVintage(item.vintageId)
                item.wineId != null -> WineTarget.ExistingWine(item.wineId)
                else -> WineTarget.NewWine(WineInput(producer = item.producer.orEmpty(), name = item.name))
            }
            val resolvedVintage = vintage ?: VintageInput(item.vintage ?: VintageYear.Unknown)
            val vintageId = with(cellar) { resolveTarget(item.collectionId, resolvedTarget, resolvedVintage, allowDuplicateWine) }
            val lotId = with(cellar) {
                addLotInScope(item.collectionId, vintageId, lot.copy(wishlistItemId = itemId, formatMl = lot.formatMl))
            }
            writeItem(item.copy(status = WishlistStatus.ACQUIRED, acquiredLotId = lotId, vintageId = vintageId), now)
            touched(SyncTables.WISHLIST_ITEM, itemId)
            vintageId
        }

    /**
     * After bottles for a wishlist item were added through the wine editor (the lot carries the
     * item id), closes the item and links it to that purchase.
     */
    fun markAcquiredIfOpen(itemId: String, vintageId: String) = database.write {
        val item = item(itemId) ?: return@write
        if (!item.status.isOpen) return@write
        val lotId = database.inventory.lotsForVintage(vintageId).executeAsList().lastOrNull { it.wishlist_item_id == itemId }?.id
        writeItem(item.copy(status = WishlistStatus.ACQUIRED, acquiredLotId = lotId ?: item.acquiredLotId, vintageId = vintageId), now)
        touched(SyncTables.WISHLIST_ITEM, itemId)
    }

    fun addLink(collectionId: String, itemId: String, input: MerchantLinkInput): String = database.write {
        val link = MerchantLink("pending", itemId, input.merchant.trim(), input.url.clean(), input.lastPrice, input.checkedOn) // validates
        val id = database.newId()
        journal.insertMerchantLink(id, collectionId, itemId, link.merchant, link.url, link.lastPrice?.minor,
            link.lastPrice?.currency?.code, link.checkedOn?.toString(), now, now)
        touched(SyncTables.MERCHANT_LINK, id)
        id
    }

    fun updateLink(linkId: String, input: MerchantLinkInput) = database.write {
        val link = MerchantLink(linkId, "x", input.merchant.trim(), input.url.clean(), input.lastPrice, input.checkedOn)
        journal.updateMerchantLink(link.merchant, link.url, link.lastPrice?.minor, link.lastPrice?.currency?.code,
            link.checkedOn?.toString(), now, linkId)
        touched(SyncTables.MERCHANT_LINK, linkId)
    }

    fun deleteLink(linkId: String) = database.write {
        journal.softDeleteMerchantLink(now, linkId)
        touched(SyncTables.MERCHANT_LINK, linkId)
    }

    private fun writeItem(i: WishlistItem, now: Long) {
        journal.updateWishlistItem(
            vintageId = i.vintageId, wineId = i.wineId, producer = i.producer, name = i.name, vintageKind = i.vintage?.kind,
            year = i.vintage?.yearOrNull?.toLong(), formatMl = i.formatMl?.toLong(), desiredQty = i.desiredQuantity.toLong(),
            targetPriceMinor = i.targetPrice?.minor, targetPriceCurrency = i.targetPrice?.currency?.code,
            priority = i.priority.name, status = i.status.name, notes = i.notes, acquiredLotId = i.acquiredLotId,
            updatedAt = now, id = i.id,
        )
    }

    private fun validate(input: WishlistInput) {
        require(input.vintageId != null || input.wineId != null || !input.producer.isNullOrBlank()) {
            "Choose a wine from your cellar or enter at least a producer"
        }
        require(input.desiredQuantity >= 1) { "Quantity must be at least 1" }
        require(input.targetPrice == null || !input.targetPrice.isNegative) { "Target price can't be negative" }
    }
}
