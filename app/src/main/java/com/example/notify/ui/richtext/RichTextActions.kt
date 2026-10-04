package com.example.notify.ui.richtext

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Supported inline styling options for rich text.
 */
enum class RichStyle {
    BOLD,
    ITALIC,
    STRIKETHROUGH
}

/**
 * Represents a styled span across character indices [start, end).
 */
data class StyleSpan(
    val start: Int,
    val end: Int,
    val style: RichStyle
)

/**
 * Handles WYSIWYG text formatting, typing updates, list continuation,
 * and Markdown serialization/deserialization for rich text editing.
 * Follows the seamless behavior of Google Chat and Microsoft Teams chatbox.
 */
object RichTextActions {

    /**
     * Normalizes and merges overlapping or contiguous spans of the same style.
     */
    fun normalizeSpans(spans: List<StyleSpan>, textLength: Int): List<StyleSpan> {
        if (spans.isEmpty() || textLength <= 0) return emptyList()

        val result = mutableListOf<StyleSpan>()
        for (style in RichStyle.values()) {
            val styleSpans = spans
                .filter { it.style == style }
                .map { StyleSpan(it.start.coerceIn(0, textLength), it.end.coerceIn(0, textLength), style) }
                .filter { it.start < it.end }
                .sortedBy { it.start }

            if (styleSpans.isEmpty()) continue

            var current = styleSpans[0]
            for (i in 1 until styleSpans.size) {
                val next = styleSpans[i]
                if (next.start <= current.end) {
                    current = current.copy(end = maxOf(current.end, next.end))
                } else {
                    result.add(current)
                    current = next
                }
            }
            result.add(current)
        }

        return result.sortedWith(compareBy({ it.start }, { it.end }))
    }

    /**
     * Checks if a style is currently active for the given selection or cursor position.
     */
    fun isStyleActive(
        selection: TextRange,
        spans: List<StyleSpan>,
        style: RichStyle,
        activeStyles: Set<RichStyle>
    ): Boolean {
        if (selection.collapsed) {
            val pos = selection.start
            if (style in activeStyles) return true
            return spans.any { it.style == style && it.start < pos && it.end >= pos }
        } else {
            val min = selection.min
            val max = selection.max
            return spans.any { it.style == style && it.start < max && it.end > min }
        }
    }

    /**
     * Checks if the line where the selection/cursor resides is bulleted.
     */
    fun isLineBulleted(text: String, selection: TextRange): Boolean {
        if (text.isEmpty()) return false
        val pos = selection.min.coerceIn(0, text.length)
        val prevNl = text.lastIndexOf('\n', (pos - 1).coerceAtLeast(0))
        val lineStart = if (prevNl == -1) 0 else prevNl + 1
        val nextNl = text.indexOf('\n', pos)
        val lineEnd = if (nextNl == -1) text.length else nextNl
        val line = text.substring(lineStart, lineEnd)
        return Regex("""^\s*•""").containsMatchIn(line)
    }

    /**
     * Checks if the line where the selection/cursor resides is numbered.
     */
    fun isLineNumbered(text: String, selection: TextRange): Boolean {
        if (text.isEmpty()) return false
        val pos = selection.min.coerceIn(0, text.length)
        val prevNl = text.lastIndexOf('\n', (pos - 1).coerceAtLeast(0))
        val lineStart = if (prevNl == -1) 0 else prevNl + 1
        val nextNl = text.indexOf('\n', pos)
        val lineEnd = if (nextNl == -1) text.length else nextNl
        val line = text.substring(lineStart, lineEnd)
        return Regex("""^\s*\d+\.""").containsMatchIn(line)
    }

    /**
     * Toggles a style on the selected text range, or toggles pending active style
     * if selection is collapsed.
     */
    fun toggleStyleAction(
        textFieldValue: TextFieldValue,
        spans: List<StyleSpan>,
        style: RichStyle,
        activeStyles: Set<RichStyle>
    ): Triple<TextFieldValue, List<StyleSpan>, Set<RichStyle>> {
        val selection = textFieldValue.selection
        val text = textFieldValue.text

        if (selection.collapsed) {
            val newActive = if (style in activeStyles) {
                activeStyles - style
            } else {
                activeStyles + style
            }
            return Triple(textFieldValue, spans, newActive)
        }

        val start = selection.min
        val end = selection.max

        // Check if the entire selection is already covered by this style
        val styleSpans = spans.filter { it.style == style }
        val isFullyCovered = (start until end).all { pos ->
            styleSpans.any { it.start <= pos && it.end > pos }
        }

        val updatedSpans = if (isFullyCovered) {
            // Remove style from [start, end]
            val result = mutableListOf<StyleSpan>()
            for (span in spans) {
                if (span.style != style) {
                    result.add(span)
                    continue
                }
                if (span.end <= start || span.start >= end) {
                    result.add(span)
                } else {
                    if (span.start < start) {
                        result.add(span.copy(end = start))
                    }
                    if (span.end > end) {
                        result.add(span.copy(start = end))
                    }
                }
            }
            result
        } else {
            // Add style to [start, end]
            spans + StyleSpan(start, end, style)
        }

        val normalized = normalizeSpans(updatedSpans, text.length)
        return Triple(textFieldValue, normalized, activeStyles)
    }

    /**
     * Clears all formatting across the selected text range, or resets pending styles
     * if selection is collapsed.
     */
    fun clearFormattingAction(
        textFieldValue: TextFieldValue,
        spans: List<StyleSpan>
    ): Triple<TextFieldValue, List<StyleSpan>, Set<RichStyle>> {
        val selection = textFieldValue.selection
        val text = textFieldValue.text

        if (selection.collapsed) {
            return Triple(textFieldValue, spans, emptySet())
        }

        val start = selection.min
        val end = selection.max

        val result = mutableListOf<StyleSpan>()
        for (span in spans) {
            if (span.end <= start || span.start >= end) {
                result.add(span)
            } else {
                if (span.start < start) {
                    result.add(span.copy(end = start))
                }
                if (span.end > end) {
                    result.add(span.copy(start = end))
                }
            }
        }

        // Also strip list markers from selected lines if any
        val lines = text.split('\n')
        var lineStart = 0
        val newLines = mutableListOf<String>()
        var textChanged = false

        for (line in lines) {
            val lineEnd = lineStart + line.length
            if (lineEnd >= start && lineStart <= end) {
                val stripped = line
                    .replace(Regex("""^(\s*)•\s*"""), "$1")
                    .replace(Regex("""^(\s*)\d+\.\s*"""), "$1")
                newLines.add(stripped)
                if (stripped != line) textChanged = true
            } else {
                newLines.add(line)
            }
            lineStart = lineEnd + 1
        }

        if (textChanged) {
            val newText = newLines.joinToString("\n")
            val diff = newText.length - text.length
            val newSelection = TextRange(start, (end + diff).coerceIn(start, newText.length))
            val adjustedSpans = adjustSpansForBlockReplacement(result, start, end, end - start + diff)
            return Triple(TextFieldValue(newText, newSelection), normalizeSpans(adjustedSpans, newText.length), emptySet())
        }

        val normalized = normalizeSpans(result, text.length)
        return Triple(textFieldValue, normalized, emptySet())
    }

    /**
     * Toggles bullet list formatting on the currently selected line(s).
     */
    fun toggleBulletList(
        textFieldValue: TextFieldValue,
        spans: List<StyleSpan>
    ): Pair<TextFieldValue, List<StyleSpan>> {
        val text = textFieldValue.text
        val start = textFieldValue.selection.min
        val end = textFieldValue.selection.max

        val lineStart = if (start <= 0) 0 else {
            val prevNl = text.lastIndexOf('\n', start - 1)
            if (prevNl == -1) 0 else prevNl + 1
        }
        val lineEnd = if (end >= text.length) text.length else {
            val nextNl = text.indexOf('\n', end)
            if (nextNl == -1) text.length else nextNl
        }

        val block = text.substring(lineStart, lineEnd)
        val lines = block.split('\n')

        val bulletRegex = Regex("""^(\s*)•\s*(.*)$""")
        val numRegex = Regex("""^(\s*)\d+\.\s*(.*)$""")

        val nonEmptyLines = lines.filter { it.isNotBlank() }
        val allBulleted = nonEmptyLines.isNotEmpty() && nonEmptyLines.all { bulletRegex.matches(it) }

        val newLines = lines.map { line ->
            if (allBulleted) {
                val m = bulletRegex.find(line)
                if (m != null) "${m.groupValues[1]}${m.groupValues[2]}" else line
            } else {
                val bm = bulletRegex.find(line)
                val nm = numRegex.find(line)
                when {
                    bm != null -> "${bm.groupValues[1]}• ${bm.groupValues[2]}"
                    nm != null -> "${nm.groupValues[1]}• ${nm.groupValues[2]}"
                    line.isEmpty() && lines.size == 1 -> "• "
                    else -> {
                        val trimmed = line.trimStart()
                        val indent = line.substring(0, line.length - trimmed.length)
                        "$indent• $trimmed"
                    }
                }
            }
        }

        val newBlock = newLines.joinToString("\n")
        val newText = text.replaceRange(lineStart, lineEnd, newBlock)

        val adjustedSpans = adjustSpansForBlockReplacement(spans, lineStart, lineEnd, newBlock.length)

        val newSelection = if (start == end) {
            val diff = newBlock.length - block.length
            TextRange((start + diff).coerceIn(0, newText.length))
        } else {
            TextRange(lineStart, (lineStart + newBlock.length).coerceIn(0, newText.length))
        }

        return Pair(TextFieldValue(newText, newSelection), normalizeSpans(adjustedSpans, newText.length))
    }

    /**
     * Toggles numbered list formatting on the currently selected line(s).
     */
    fun toggleNumberedList(
        textFieldValue: TextFieldValue,
        spans: List<StyleSpan>
    ): Pair<TextFieldValue, List<StyleSpan>> {
        val text = textFieldValue.text
        val start = textFieldValue.selection.min
        val end = textFieldValue.selection.max

        val lineStart = if (start <= 0) 0 else {
            val prevNl = text.lastIndexOf('\n', start - 1)
            if (prevNl == -1) 0 else prevNl + 1
        }
        val lineEnd = if (end >= text.length) text.length else {
            val nextNl = text.indexOf('\n', end)
            if (nextNl == -1) text.length else nextNl
        }

        val block = text.substring(lineStart, lineEnd)
        val lines = block.split('\n')

        val numRegex = Regex("""^(\s*)\d+\.\s*(.*)$""")
        val bulletRegex = Regex("""^(\s*)•\s*(.*)$""")

        val nonEmptyLines = lines.filter { it.isNotBlank() }
        val allNumbered = nonEmptyLines.isNotEmpty() && nonEmptyLines.all { numRegex.matches(it) }

        var counter = 1
        val newLines = lines.map { line ->
            if (allNumbered) {
                val m = numRegex.find(line)
                if (m != null) "${m.groupValues[1]}${m.groupValues[2]}" else line
            } else {
                val nm = numRegex.find(line)
                val bm = bulletRegex.find(line)
                val index = counter++
                when {
                    nm != null -> "${nm.groupValues[1]}$index. ${nm.groupValues[2]}"
                    bm != null -> "${bm.groupValues[1]}$index. ${bm.groupValues[2]}"
                    line.isEmpty() && lines.size == 1 -> "1. "
                    else -> {
                        val trimmed = line.trimStart()
                        val indent = line.substring(0, line.length - trimmed.length)
                        "$indent$index. $trimmed"
                    }
                }
            }
        }

        val newBlock = newLines.joinToString("\n")
        val newText = text.replaceRange(lineStart, lineEnd, newBlock)

        val adjustedSpans = adjustSpansForBlockReplacement(spans, lineStart, lineEnd, newBlock.length)

        val newSelection = if (start == end) {
            val diff = newBlock.length - block.length
            TextRange((start + diff).coerceIn(0, newText.length))
        } else {
            TextRange(lineStart, (lineStart + newBlock.length).coerceIn(0, newText.length))
        }

        return Pair(TextFieldValue(newText, newSelection), normalizeSpans(adjustedSpans, newText.length))
    }

    /**
     * Handles typing changes, backspaces, pastes, enter-key list continuation,
     * and auto-formatting (like typing "- " or "* " to start a bullet point).
     */
    fun handleUserInput(
        oldVal: TextFieldValue,
        newVal: TextFieldValue,
        currentSpans: List<StyleSpan>,
        currentActiveStyles: Set<RichStyle>
    ): Triple<TextFieldValue, List<StyleSpan>, Set<RichStyle>> {
        val oldText = oldVal.text
        val newText = newVal.text

        // Case 1: Cursor moved without text modification
        if (oldText == newText) {
            val newActive = if (newVal.selection.collapsed) {
                val pos = newVal.selection.start
                RichStyle.values().filter { style ->
                    currentSpans.any { it.style == style && it.start < pos && it.end >= pos }
                }.toSet()
            } else {
                emptySet()
            }
            return Triple(newVal, currentSpans, newActive)
        }

        // Case 2: Enter pressed - list auto-continuation or exit
        if (newText.length == oldText.length + 1 &&
            oldVal.selection.collapsed &&
            newVal.selection.start == oldVal.selection.start + 1
        ) {
            val insertPos = oldVal.selection.start
            if (insertPos < newText.length && newText[insertPos] == '\n') {
                val textBeforeNl = oldText.substring(0, insertPos)
                val prevNl = textBeforeNl.lastIndexOf('\n')
                val lineStart = if (prevNl == -1) 0 else prevNl + 1
                val prevLine = textBeforeNl.substring(lineStart)

                // 2a. Enter on empty bullet item -> Exit bullet list
                val emptyBulletMatch = Regex("""^(\s*)•\s*$""").matchEntire(prevLine)
                if (emptyBulletMatch != null) {
                    val beforeLine = oldText.substring(0, lineStart)
                    val afterCursor = oldText.substring(insertPos)
                    val resultText = beforeLine + afterCursor
                    val newSel = TextRange(lineStart)
                    val adjusted = adjustSpansForEdit(currentSpans, oldText, resultText, currentActiveStyles)
                    return Triple(TextFieldValue(resultText, newSel), adjusted, currentActiveStyles)
                }

                // 2b. Enter on empty numbered item -> Exit numbered list
                val emptyNumMatch = Regex("""^(\s*)\d+\.\s*$""").matchEntire(prevLine)
                if (emptyNumMatch != null) {
                    val beforeLine = oldText.substring(0, lineStart)
                    val afterCursor = oldText.substring(insertPos)
                    val resultText = beforeLine + afterCursor
                    val newSel = TextRange(lineStart)
                    val adjusted = adjustSpansForEdit(currentSpans, oldText, resultText, currentActiveStyles)
                    return Triple(TextFieldValue(resultText, newSel), adjusted, currentActiveStyles)
                }

                // 2c. Enter on active bullet item -> Continue bullet list
                val bulletMatch = Regex("""^(\s*)•\s+(.+)$""").matchEntire(prevLine)
                if (bulletMatch != null) {
                    val indent = bulletMatch.groupValues[1]
                    val continuation = "$indent• "
                    val beforeCursor = newText.substring(0, insertPos + 1)
                    val afterCursor = newText.substring(insertPos + 1)
                    val resultText = beforeCursor + continuation + afterCursor
                    val newSel = TextRange(insertPos + 1 + continuation.length)
                    val adjusted = adjustSpansForEdit(currentSpans, oldText, resultText, currentActiveStyles)
                    return Triple(TextFieldValue(resultText, newSel), adjusted, currentActiveStyles)
                }

                // 2d. Enter on active numbered item -> Continue numbered list
                val numMatch = Regex("""^(\s*)(\d+)\.\s+(.+)$""").matchEntire(prevLine)
                if (numMatch != null) {
                    val indent = numMatch.groupValues[1]
                    val currentNum = numMatch.groupValues[2].toIntOrNull() ?: 1
                    val continuation = "$indent${currentNum + 1}. "
                    val beforeCursor = newText.substring(0, insertPos + 1)
                    val afterCursor = newText.substring(insertPos + 1)
                    val resultText = beforeCursor + continuation + afterCursor
                    val newSel = TextRange(insertPos + 1 + continuation.length)
                    val adjusted = adjustSpansForEdit(currentSpans, oldText, resultText, currentActiveStyles)
                    return Triple(TextFieldValue(resultText, newSel), adjusted, currentActiveStyles)
                }
            }
        }

        // Case 3: Auto-bullet on typing "- " or "* " at start of line
        if (newText.length == oldText.length + 1 &&
            oldVal.selection.collapsed &&
            newVal.selection.start == oldVal.selection.start + 1
        ) {
            val insertPos = oldVal.selection.start
            if (insertPos < newText.length && newText[insertPos] == ' ') {
                val textBeforeSpace = newText.substring(0, insertPos + 1)
                val prevNl = textBeforeSpace.lastIndexOf('\n', (insertPos - 1).coerceAtLeast(0))
                val lineStart = if (prevNl == -1) 0 else prevNl + 1
                val lineSoFar = textBeforeSpace.substring(lineStart)
                val dashOrStarMatch = Regex("""^(\s*)(•|-|\*)\s$""").matchEntire(lineSoFar)
                if (dashOrStarMatch != null) {
                    val indent = dashOrStarMatch.groupValues[1]
                    val replacement = "$indent• "
                    val afterCursor = newText.substring(insertPos + 1)
                    val resultText = newText.substring(0, lineStart) + replacement + afterCursor
                    val newSel = TextRange(lineStart + replacement.length)
                    val adjusted = adjustSpansForEdit(currentSpans, oldText, resultText, currentActiveStyles)
                    return Triple(TextFieldValue(resultText, newSel), adjusted, currentActiveStyles)
                }
            }
        }

        // Default: Adjust spans for the text difference
        val adjustedSpans = adjustSpansForEdit(currentSpans, oldText, newText, currentActiveStyles)
        return Triple(newVal, adjustedSpans, currentActiveStyles)
    }

    /**
     * Adjusts style spans across a text edit (typing, deleting, pasting).
     */
    fun adjustSpansForEdit(
        spans: List<StyleSpan>,
        oldText: String,
        newText: String,
        activeStyles: Set<RichStyle>
    ): List<StyleSpan> {
        var prefixLen = 0
        val minLen = minOf(oldText.length, newText.length)
        while (prefixLen < minLen && oldText[prefixLen] == newText[prefixLen]) {
            prefixLen++
        }

        var suffixLen = 0
        val maxSuffix = minOf(oldText.length - prefixLen, newText.length - prefixLen)
        while (suffixLen < maxSuffix && oldText[oldText.length - 1 - suffixLen] == newText[newText.length - 1 - suffixLen]) {
            suffixLen++
        }

        val deletedStart = prefixLen
        val deletedEnd = oldText.length - suffixLen
        val insertedStart = prefixLen
        val insertedEnd = newText.length - suffixLen
        val insertedLen = insertedEnd - insertedStart
        val deletedLen = deletedEnd - deletedStart
        val delta = insertedLen - deletedLen

        val result = mutableListOf<StyleSpan>()
        for (span in spans) {
            if (span.end <= deletedStart) {
                // Completely before edit
                result.add(span)
            } else if (span.start >= deletedEnd) {
                // Completely after edit
                result.add(span.copy(start = span.start + delta, end = span.end + delta))
            } else {
                // Overlaps edit region
                if (deletedStart >= span.start && deletedEnd <= span.end) {
                    // Typed/edited inside the span
                    val newEnd = span.end + delta
                    if (span.start < newEnd) {
                        result.add(span.copy(end = newEnd))
                    }
                } else {
                    // Boundary edit
                    val newStart = if (span.start < deletedStart) span.start else deletedStart + insertedLen
                    val newEnd = if (span.end > deletedEnd) span.end + delta else deletedStart
                    if (newStart < newEnd) {
                        result.add(span.copy(start = newStart, end = newEnd))
                    }
                }
            }
        }

        // Apply active typing styles to inserted characters
        if (insertedLen > 0 && activeStyles.isNotEmpty()) {
            for (style in activeStyles) {
                val alreadyCovered = result.any { it.style == style && it.start <= insertedStart && it.end >= insertedEnd }
                if (!alreadyCovered) {
                    result.add(StyleSpan(insertedStart, insertedEnd, style))
                }
            }
        }

        return normalizeSpans(result, newText.length)
    }

    private fun adjustSpansForBlockReplacement(
        spans: List<StyleSpan>,
        replacedStart: Int,
        replacedEnd: Int,
        insertedLength: Int
    ): List<StyleSpan> {
        val delta = insertedLength - (replacedEnd - replacedStart)
        val result = mutableListOf<StyleSpan>()
        for (span in spans) {
            if (span.end <= replacedStart) {
                result.add(span)
            } else if (span.start >= replacedEnd) {
                result.add(span.copy(start = span.start + delta, end = span.end + delta))
            } else {
                val newStart = span.start.coerceAtMost(replacedStart)
                val newEnd = (span.end + delta).coerceAtLeast(replacedStart + insertedLength)
                if (newStart < newEnd) {
                    result.add(span.copy(start = newStart, end = newEnd))
                }
            }
        }
        return result
    }

    /**
     * Parses standard Markdown into clean plain text (with bullets/numbers intact)
     * and a list of style spans. Strips asterisks, tildes, and underscores.
     */
    fun parseMarkdown(markdown: String): Pair<String, List<StyleSpan>> {
        if (markdown.isEmpty()) return Pair("", emptyList())

        val lines = markdown.split('\n')
        val plainBuilder = StringBuilder()
        val spans = mutableListOf<StyleSpan>()

        for ((index, rawLine) in lines.withIndex()) {
            if (index > 0) {
                plainBuilder.append('\n')
            }
            val lineStartOffset = plainBuilder.length

            // Normalize bullet markers (- or * at start) to •
            var lineContent = rawLine
            val bulletMatch = Regex("""^(\s*)(•|-|\*)\s+(.*)$""").find(rawLine)
            if (bulletMatch != null) {
                val indent = bulletMatch.groupValues[1]
                val rest = bulletMatch.groupValues[3]
                lineContent = "$indent• $rest"
            }

            parseInlineTokens(lineContent, plainBuilder, spans, emptySet())
        }

        val plainText = plainBuilder.toString()
        val normalized = normalizeSpans(spans, plainText.length)
        return Pair(plainText, normalized)
    }

    private fun parseInlineTokens(
        text: String,
        builder: StringBuilder,
        spans: MutableList<StyleSpan>,
        inheritedStyles: Set<RichStyle>
    ) {
        val regex = Regex(
            """(\*\*\*(.+?)\*\*\*)|(\*\*_(.+?)_\*\*)|(_\*\*(.+?)\*\*_)|(\*\*(.+?)\*\*)|(~~(.+?)~~)|((?<!\*)\*([^*]+?)\*(?!\*))|((?<!_)_([^_]+?)_(?!_))"""
        )

        var currentIndex = 0
        for (match in regex.findAll(text)) {
            val range = match.range
            if (range.first > currentIndex) {
                val plainChunk = text.substring(currentIndex, range.first)
                val chunkStart = builder.length
                builder.append(plainChunk)
                val chunkEnd = builder.length
                for (style in inheritedStyles) {
                    spans.add(StyleSpan(chunkStart, chunkEnd, style))
                }
            }

            val full = match.value
            when {
                // Bold + Italic
                full.startsWith("***") && full.endsWith("***") && full.length >= 6 -> {
                    val inner = full.substring(3, full.length - 3)
                    parseInlineTokens(inner, builder, spans, inheritedStyles + RichStyle.BOLD + RichStyle.ITALIC)
                }
                full.startsWith("**_") && full.endsWith("_**") && full.length >= 6 -> {
                    val inner = full.substring(3, full.length - 3)
                    parseInlineTokens(inner, builder, spans, inheritedStyles + RichStyle.BOLD + RichStyle.ITALIC)
                }
                full.startsWith("_**") && full.endsWith("**_") && full.length >= 6 -> {
                    val inner = full.substring(3, full.length - 3)
                    parseInlineTokens(inner, builder, spans, inheritedStyles + RichStyle.BOLD + RichStyle.ITALIC)
                }
                // Bold
                full.startsWith("**") && full.endsWith("**") && full.length >= 4 -> {
                    val inner = full.substring(2, full.length - 2)
                    parseInlineTokens(inner, builder, spans, inheritedStyles + RichStyle.BOLD)
                }
                // Strikethrough
                full.startsWith("~~") && full.endsWith("~~") && full.length >= 4 -> {
                    val inner = full.substring(2, full.length - 2)
                    parseInlineTokens(inner, builder, spans, inheritedStyles + RichStyle.STRIKETHROUGH)
                }
                // Italic
                (full.startsWith("*") && full.endsWith("*") && full.length >= 2) ||
                (full.startsWith("_") && full.endsWith("_") && full.length >= 2) -> {
                    val inner = full.substring(1, full.length - 1)
                    parseInlineTokens(inner, builder, spans, inheritedStyles + RichStyle.ITALIC)
                }
                else -> {
                    val chunkStart = builder.length
                    builder.append(full)
                    val chunkEnd = builder.length
                    for (style in inheritedStyles) {
                        spans.add(StyleSpan(chunkStart, chunkEnd, style))
                    }
                }
            }
            currentIndex = range.last + 1
        }

        if (currentIndex < text.length) {
            val tail = text.substring(currentIndex)
            val chunkStart = builder.length
            builder.append(tail)
            val chunkEnd = builder.length
            for (style in inheritedStyles) {
                spans.add(StyleSpan(chunkStart, chunkEnd, style))
            }
        }
    }

    /**
     * Serializes plain text and style spans back to standard Markdown format.
     */
    fun serializeToMarkdown(plainText: String, spans: List<StyleSpan>): String {
        if (plainText.isEmpty()) return ""

        val lines = plainText.split('\n')
        val result = StringBuilder()
        var lineStartOffset = 0

        for ((lineIdx, line) in lines.withIndex()) {
            if (lineIdx > 0) {
                result.append('\n')
            }
            val lineEndOffset = lineStartOffset + line.length

            if (line.isEmpty()) {
                lineStartOffset = lineEndOffset + 1
                continue
            }

            // Find spans overlapping this line
            val lineSpans = spans.filter { it.start < lineEndOffset && it.end > lineStartOffset }

            var openBold = false
            var openItalic = false
            var openStrike = false

            for (i in 0 until line.length) {
                val absIdx = lineStartOffset + i
                val isBold = lineSpans.any { it.style == RichStyle.BOLD && absIdx >= it.start && absIdx < it.end }
                val isItalic = lineSpans.any { it.style == RichStyle.ITALIC && absIdx >= it.start && absIdx < it.end }
                val isStrike = lineSpans.any { it.style == RichStyle.STRIKETHROUGH && absIdx >= it.start && absIdx < it.end }

                // Close tags if needed (inside-out order: Italic, Bold, Strike)
                if (openItalic && !isItalic) {
                    result.append('*')
                    openItalic = false
                }
                if (openBold && !isBold) {
                    result.append("**")
                    openBold = false
                }
                if (openStrike && !isStrike) {
                    result.append("~~")
                    openStrike = false
                }

                // Open tags if needed (outside-in order: Strike, Bold, Italic)
                if (!openStrike && isStrike) {
                    result.append("~~")
                    openStrike = true
                }
                if (!openBold && isBold) {
                    result.append("**")
                    openBold = true
                }
                if (!openItalic && isItalic) {
                    result.append('*')
                    openItalic = true
                }

                result.append(line[i])
            }

            // Close any tags still open at the end of the line
            if (openItalic) result.append('*')
            if (openBold) result.append("**")
            if (openStrike) result.append("~~")

            lineStartOffset = lineEndOffset + 1
        }

        return result.toString()
    }
}
