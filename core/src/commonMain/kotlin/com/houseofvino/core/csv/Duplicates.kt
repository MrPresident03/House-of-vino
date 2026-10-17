package com.houseofvino.core.csv

import com.houseofvino.core.model.VintageYear
import com.houseofvino.core.money.Money
import com.houseofvino.core.text.Fuzzy
import com.houseofvino.core.time.IsoDate

/** A vintage already in the collection, as seen by duplicate detection. */
data class ExistingVintage(
    val vintageId: String,
    val wineId: String,
    val producer: String,
    val name: String?,
    val vintage: VintageYear,
)

/** An existing acquisition batch, used to spot re-imports of the same file. */
data class ExistingLot(
    val lotId: String,
    val vintageId: String,
    val formatMl: Int,
    val acquiredOn: IsoDate?,
    val unitPrice: Money?,
    val source: String?,
    val quantityPurchased: Int,
)

/** What the importer should do with one record. Defaults never merge different vintages. */
sealed interface ImportDecision {
    /** Create a new wine and vintage. */
    data object CreateNew : ImportDecision
    /** Same producer/name/vintage already exists: add a new lot to it. */
    data class AddToVintage(val vintageId: String) : ImportDecision
    /** Wine exists in another vintage: create this vintage under the existing wine. */
    data class AddVintageToWine(val wineId: String) : ImportDecision
    data object Skip : ImportDecision
}

data class DuplicateAssessment(
    val record: ImportRecord,
    val kind: Kind,
    val candidates: List<ExistingVintage>,
    val defaultDecision: ImportDecision,
    /** True when the user must look at this row before committing. */
    val needsReview: Boolean,
    val explanation: String,
) {
    enum class Kind { NEW, SAME_VINTAGE, OTHER_VINTAGE_OF_WINE, POSSIBLE_DUPLICATE, POSSIBLE_SAME_WINE, LIKELY_REIMPORT, DUPLICATE_IN_FILE }
}

/**
 * Classifies import records against the collection and against each other. Only exact
 * identity matches are pre-selected; fuzzy matches always need explicit confirmation, and a
 * different vintage is never treated as the same wine record.
 */
class DuplicateDetector(
    existingVintages: List<ExistingVintage>,
    private val existingLots: List<ExistingLot>,
    private val fuzzyThreshold: Int = 80,
) {
    private val byIdentity = existingVintages.groupBy { IdentityKey.of(it.producer, it.name, it.vintage) }
    private val byWine = existingVintages.groupBy { IdentityKey.of(it.producer, it.name, it.vintage).wineKey }
    private val all = existingVintages

    fun assess(records: List<ImportRecord>): List<DuplicateAssessment> {
        val seenInFile = HashMap<String, ImportRecord>()
        return records.map { r ->
            val lotFingerprint = "${r.identityKey}|${r.formatMl}|${r.acquiredOn}|${r.unitPrice}|${r.source}|${r.quantity}|${r.locationPath}|${r.bin}"
            val earlier = seenInFile[lotFingerprint]
            if (earlier == null) seenInFile[lotFingerprint] = r
            assessOne(r, earlier)
        }
    }

    private fun assessOne(r: ImportRecord, earlierIdentical: ImportRecord?): DuplicateAssessment {
        val key = r.identityKey
        val exact = byIdentity[key].orEmpty()
        if (exact.isNotEmpty()) {
            val target = exact.first()
            val reimport = existingLots.firstOrNull { lot ->
                lot.vintageId == target.vintageId && lot.formatMl == r.formatMl && lot.acquiredOn == r.acquiredOn &&
                    lot.unitPrice == r.unitPrice && lot.quantityPurchased == r.quantity &&
                    (lot.source ?: "") == (r.source ?: "") && (r.acquiredOn != null || r.unitPrice != null)
            }
            if (reimport != null) {
                return DuplicateAssessment(r, DuplicateAssessment.Kind.LIKELY_REIMPORT, exact, ImportDecision.Skip, needsReview = true,
                    explanation = "A purchase with the same date, price, size and quantity already exists. Skipped unless you choose to add it.")
            }
            return DuplicateAssessment(r, DuplicateAssessment.Kind.SAME_VINTAGE, exact, ImportDecision.AddToVintage(target.vintageId),
                needsReview = exact.size > 1,
                explanation = "Already in your cellar; these bottles will be added as a new purchase.")
        }
        if (earlierIdentical != null) {
            return DuplicateAssessment(r, DuplicateAssessment.Kind.DUPLICATE_IN_FILE, emptyList(), ImportDecision.CreateNew, needsReview = true,
                explanation = "Identical to row ${earlierIdentical.row}. Both will be imported unless you skip one.")
        }
        val sameWine = byWine[key.wineKey].orEmpty()
        if (sameWine.isNotEmpty()) {
            return DuplicateAssessment(r, DuplicateAssessment.Kind.OTHER_VINTAGE_OF_WINE, sameWine,
                ImportDecision.AddVintageToWine(sameWine.first().wineId), needsReview = false,
                explanation = "New vintage (${r.vintage}) of a wine you already have.")
        }
        val fuzzy = all.filter { e ->
            e.vintage == r.vintage &&
                Fuzzy.similarity(e.producer, r.producer) >= fuzzyThreshold &&
                Fuzzy.similarity(e.name, r.name) >= fuzzyThreshold
        }
        if (fuzzy.isNotEmpty()) {
            return DuplicateAssessment(r, DuplicateAssessment.Kind.POSSIBLE_DUPLICATE, fuzzy, ImportDecision.CreateNew, needsReview = true,
                explanation = "Looks like ${fuzzy.first().producer}${fuzzy.first().name?.let { " $it" } ?: ""} ${fuzzy.first().vintage}. Choose whether to add to it or keep it separate.")
        }
        val fuzzyWine = all.filter { e ->
            Fuzzy.similarity(e.producer, r.producer) >= fuzzyThreshold && Fuzzy.similarity(e.name, r.name) >= fuzzyThreshold
        }.distinctBy { it.wineId }
        if (fuzzyWine.isNotEmpty()) {
            val w = fuzzyWine.first()
            return DuplicateAssessment(r, DuplicateAssessment.Kind.POSSIBLE_SAME_WINE, fuzzyWine, ImportDecision.CreateNew, needsReview = true,
                explanation = "May be a new vintage of ${w.producer}${w.name?.let { " $it" } ?: ""}. Choose whether to file it under that wine.")
        }
        return DuplicateAssessment(r, DuplicateAssessment.Kind.NEW, emptyList(), ImportDecision.CreateNew, needsReview = false,
            explanation = "New wine.")
    }
}
