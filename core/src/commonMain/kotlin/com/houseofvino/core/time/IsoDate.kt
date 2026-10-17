package com.houseofvino.core.time

/**
 * A calendar date without time zone (proleptic Gregorian), stored and exchanged as
 * ISO-8601 `YYYY-MM-DD`. Kept dependency-free so the domain module needs only the stdlib.
 */
class IsoDate private constructor(val year: Int, val month: Int, val day: Int) : Comparable<IsoDate> {

    /** Days since 1970-01-01. */
    val epochDay: Long get() = daysFromCivil(year, month, day)

    fun plusDays(days: Long): IsoDate = ofEpochDay(epochDay + days)

    override fun compareTo(other: IsoDate): Int = epochDay.compareTo(other.epochDay)
    override fun equals(other: Any?): Boolean =
        other is IsoDate && other.year == year && other.month == month && other.day == day
    override fun hashCode(): Int = (year * 31 + month) * 31 + day
    override fun toString(): String =
        "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"

    companion object {
        fun of(year: Int, month: Int, day: Int): IsoDate {
            require(year in 1..9999) { "Year out of range: $year" }
            require(month in 1..12) { "Month out of range: $month" }
            require(day in 1..lengthOfMonth(year, month)) { "Day out of range: $year-$month-$day" }
            return IsoDate(year, month, day)
        }

        fun parse(text: String): IsoDate {
            val t = text.trim()
            val m = ISO.matchEntire(t) ?: throw IllegalArgumentException("Not an ISO date: $text")
            return of(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt())
        }

        fun parseOrNull(text: String?): IsoDate? = text?.let { runCatching { parse(it) }.getOrNull() }

        fun ofEpochDay(epochDay: Long): IsoDate {
            // Howard Hinnant's civil_from_days.
            val z = epochDay + 719468
            val era = (if (z >= 0) z else z - 146096) / 146097
            val doe = z - era * 146097
            val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
            val y = yoe + era * 400
            val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
            val mp = (5 * doy + 2) / 153
            val d = doy - (153 * mp + 2) / 5 + 1
            val m = if (mp < 10) mp + 3 else mp - 9
            return of((if (m <= 2) y + 1 else y).toInt(), m.toInt(), d.toInt())
        }

        fun fromEpochMillis(epochMillis: Long, utcOffsetMinutes: Int = 0): IsoDate {
            val shifted = epochMillis + utcOffsetMinutes * 60_000L
            return ofEpochDay(shifted.floorDiv(86_400_000L))
        }

        fun isLeapYear(year: Int): Boolean = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0

        fun lengthOfMonth(year: Int, month: Int): Int = when (month) {
            2 -> if (isLeapYear(year)) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }

        private fun daysFromCivil(year: Int, month: Int, day: Int): Long {
            val y = (if (month <= 2) year - 1 else year).toLong()
            val era = (if (y >= 0) y else y - 399) / 400
            val yoe = y - era * 400
            val mp = (month + 9) % 12
            val doy = (153 * mp + 2) / 5 + day - 1
            val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
            return era * 146097 + doe - 719468
        }

        private val ISO = Regex("""(\d{4})-(\d{2})-(\d{2})""")
    }
}

/** Injectable time source so domain logic and tests never read the wall clock directly. */
fun interface Clock {
    fun nowMillis(): Long

    companion object {
        @OptIn(kotlin.time.ExperimentalTime::class)
        val System: Clock = Clock { kotlin.time.Clock.System.now().toEpochMilliseconds() }
    }
}

/** Parses dates in the formats commonly found in cellar exports, given an explicit order. */
enum class DateOrder { YMD, DMY, MDY }

object FlexibleDateParser {
    private val SEPARATED = Regex("""^(\d{1,4})[./\-](\d{1,2})[./\-](\d{1,4})$""")

    /**
     * Parses [text] using [order] for ambiguous numeric dates. ISO `YYYY-MM-DD` is always
     * accepted. A bare year ("2019") is not a date and returns null.
     */
    fun parse(text: String, order: DateOrder): IsoDate? {
        val t = text.trim()
        if (t.isEmpty()) return null
        IsoDate.parseOrNull(t.take(10).takeIf { t.length == 10 || t.getOrNull(10) == 'T' || t.getOrNull(10) == ' ' })
            ?.let { return it }
        val m = SEPARATED.matchEntire(t) ?: return null
        val a = m.groupValues[1]
        val b = m.groupValues[2].toInt()
        val c = m.groupValues[3]
        return runCatching {
            when {
                a.length == 4 -> IsoDate.of(a.toInt(), b, c.toInt())
                order == DateOrder.DMY -> IsoDate.of(expandYear(c), b, a.toInt())
                order == DateOrder.MDY -> IsoDate.of(expandYear(c), a.toInt(), b)
                else -> null
            }
        }.getOrNull()
    }

    /** Two-digit years are interpreted as 20xx when ≤ 69, else 19xx (Excel/POSIX convention). */
    private fun expandYear(y: String): Int {
        val n = y.toInt()
        return when (y.length) {
            2 -> if (n <= 69) 2000 + n else 1900 + n
            4 -> n
            else -> throw IllegalArgumentException("Bad year")
        }
    }
}
