package com.houseofvino.core.id

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Generates stable identifiers. Injected so tests can be deterministic. */
fun interface IdGenerator {
    fun newId(): String

    companion object {
        @OptIn(ExperimentalUuidApi::class)
        val Random: IdGenerator = IdGenerator { Uuid.random().toString() }

        /** Deterministic generator for tests: "00000000-0000-4000-8000-000000000001", … */
        fun sequential(prefix: String = "00000000-0000-4000-8000-"): IdGenerator {
            var n = 0L
            return IdGenerator { prefix + (++n).toString().padStart(12, '0') }
        }
    }
}

object Ids {
    private val UUID = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")

    fun isValid(id: String): Boolean = UUID.matches(id)
}
