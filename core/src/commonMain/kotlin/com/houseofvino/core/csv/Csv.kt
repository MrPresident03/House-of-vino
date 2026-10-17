package com.houseofvino.core.csv

/** RFC 4180 CSV reading and writing, with delimiter detection and BOM handling. */
object Csv {

    data class Table(val header: List<String>, val rows: List<List<String>>, val delimiter: Char) {
        /** 1-based line numbers of each data row in the source (multi-line fields span lines). */
        var sourceLines: List<Int> = emptyList()
            internal set
    }

    class ParseException(message: String, val line: Int) : Exception("Line $line: $message")

    fun detectDelimiter(text: String): Char {
        val firstLine = text.removePrefix("﻿").lineSequence().firstOrNull { it.isNotBlank() } ?: return ','
        val candidates = listOf(',', ';', '\t', '|')
        return candidates.maxBy { c -> countOutsideQuotes(firstLine, c) }.takeIf { countOutsideQuotes(firstLine, it) > 0 } ?: ','
    }

    private fun countOutsideQuotes(line: String, c: Char): Int {
        var inQuotes = false
        var n = 0
        for (ch in line) {
            if (ch == '"') inQuotes = !inQuotes else if (ch == c && !inQuotes) n++
        }
        return n
    }

    /** Parses all records. The first non-empty record is the header. Fully blank rows are skipped. */
    fun read(text: String, delimiter: Char = detectDelimiter(text)): Table {
        val records = ArrayList<List<String>>()
        val lines = ArrayList<Int>()
        val s = text.removePrefix("﻿")
        var i = 0
        var line = 1
        val field = StringBuilder()
        var record = ArrayList<String>()
        var recordStartLine = 1
        var inQuotes = false
        var fieldStarted = false
        var quotedField = false
        while (i < s.length) {
            val c = s[i]
            if (inQuotes) {
                when {
                    c == '"' && i + 1 < s.length && s[i + 1] == '"' -> { field.append('"'); i++ }
                    c == '"' -> inQuotes = false
                    else -> { if (c == '\n') line++; field.append(c) }
                }
            } else {
                when (c) {
                    '"' -> {
                        if (quotedField) throw ParseException("Text after a closing quote", line)
                        if (field.isNotBlank()) {
                            field.append(c) // lenient: a stray quote inside an unquoted field is literal
                        } else {
                            field.clear(); inQuotes = true; fieldStarted = true; quotedField = true
                        }
                    }
                    delimiter -> { record += field.toString(); field.clear(); fieldStarted = false; quotedField = false }
                    '\r' -> Unit
                    '\n' -> {
                        record += field.toString(); field.clear()
                        if (record.any { it.isNotBlank() }) { records += record; lines += recordStartLine }
                        record = ArrayList(); fieldStarted = false; quotedField = false
                        line++; recordStartLine = line
                    }
                    else -> {
                        if (quotedField && !c.isWhitespace()) {
                            throw ParseException("Text after a closing quote", line)
                        }
                        if (!quotedField) field.append(c)
                        fieldStarted = true
                    }
                }
            }
            i++
        }
        if (inQuotes) throw ParseException("Unterminated quoted field", recordStartLine)
        if (fieldStarted || field.isNotEmpty() || record.isNotEmpty()) {
            record += field.toString()
            if (record.any { it.isNotBlank() }) { records += record; lines += recordStartLine }
        }
        if (records.isEmpty()) return Table(emptyList(), emptyList(), delimiter)
        val header = records.first().map { it.trim() }
        val table = Table(header, records.drop(1), delimiter)
        table.sourceLines = lines.drop(1)
        return table
    }

    fun write(header: List<String>, rows: Sequence<List<String?>>, delimiter: Char = ','): String = buildString {
        writeTo(this, header, rows, delimiter)
    }

    fun writeTo(out: Appendable, header: List<String>, rows: Sequence<List<String?>>, delimiter: Char = ',') {
        out.append(header.joinToString(delimiter.toString()) { escape(it, delimiter) }).append("\r\n")
        for (row in rows) {
            require(row.size == header.size) { "Row has ${row.size} fields, header has ${header.size}" }
            out.append(row.joinToString(delimiter.toString()) { escape(it ?: "", delimiter) }).append("\r\n")
        }
    }

    /**
     * Quotes fields containing delimiters, quotes or newlines, and neutralises spreadsheet
     * formula injection (cells starting with = + - @ are prefixed with an apostrophe) unless the
     * field is a plain number.
     */
    fun escape(value: String, delimiter: Char = ','): String {
        var v = value
        if (v.isNotEmpty() && v[0] in "=+-@\t\r" && !NUMBER.matches(v)) v = "'$v"
        val needsQuotes = v.any { it == delimiter || it == '"' || it == '\n' || it == '\r' } || v != v.trim()
        return if (needsQuotes) "\"" + v.replace("\"", "\"\"") + "\"" else v
    }

    private val NUMBER = Regex("""^[-+]?\d+(\.\d+)?$""")
}
