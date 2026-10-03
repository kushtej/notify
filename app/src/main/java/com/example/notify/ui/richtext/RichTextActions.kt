package com.example.notify.ui.richtext

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Text manipulation helpers for rich text formatting (Bold, Italic, Strikethrough, Bullets, Numbers).
 * Follows Google Chat / Microsoft Teams editor behavior.
 */
object RichTextActions {

    fun applyBold(value: TextFieldValue): TextFieldValue =
        applyInlineWrapper(value, "**", "**")

    fun applyItalic(value: TextFieldValue): TextFieldValue =
        applyInlineWrapper(value, "*", "*")

    fun applyStrikethrough(value: TextFieldValue): TextFieldValue =
        applyInlineWrapper(value, "~~", "~~")

    private fun applyInlineWrapper(
        value: TextFieldValue,
        prefix: String,
        suffix: String
    ): TextFieldValue {
        val text = value.text
        val start = value.selection.min
        val end = value.selection.max

        if (start != end) {
            val selected = text.substring(start, end)
            // Case 1: Selection itself starts and ends with the wrapper
            if (selected.startsWith(prefix) && selected.endsWith(suffix) && selected.length >= prefix.length + suffix.length) {
                val unwrapped = selected.substring(prefix.length, selected.length - suffix.length)
                val newText = text.replaceRange(start, end, unwrapped)
                return TextFieldValue(newText, TextRange(start, start + unwrapped.length))
            }

            // Case 2: Text immediately outside the selection is the wrapper
            if (start >= prefix.length && end + suffix.length <= text.length) {
                val before = text.substring(start - prefix.length, start)
                val after = text.substring(end, end + suffix.length)
                if (before == prefix && after == suffix) {
                    val newText = text.substring(0, start - prefix.length) + selected + text.substring(end + suffix.length)
                    return TextFieldValue(newText, TextRange(start - prefix.length, end - prefix.length))
                }
            }

            // Case 3: Wrap selection
            val wrapped = "$prefix$selected$suffix"
            val newText = text.replaceRange(start, end, wrapped)
            return TextFieldValue(newText, TextRange(start, start + wrapped.length))
        } else {
            // Collapsed cursor: insert wrapper tags and position cursor in between
            val newText = text.substring(0, start) + prefix + suffix + text.substring(start)
            return TextFieldValue(newText, TextRange(start + prefix.length))
        }
    }

    fun applyBulletList(value: TextFieldValue): TextFieldValue {
        val text = value.text
        val start = value.selection.min
        val end = value.selection.max

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

        val bulletRegex = Regex("""^(\s*)(•|-|\*)\s*(.*)$""")
        val numRegex = Regex("""^(\s*)(\d+\.)\s*(.*)$""")

        val nonEmptyLines = lines.filter { it.isNotBlank() }
        val allBulleted = nonEmptyLines.isNotEmpty() && nonEmptyLines.all { bulletRegex.matches(it) }

        val newLines = lines.map { line ->
            if (allBulleted) {
                val m = bulletRegex.find(line)
                if (m != null) "${m.groupValues[1]}${m.groupValues[3]}" else line
            } else {
                val bm = bulletRegex.find(line)
                val nm = numRegex.find(line)
                when {
                    bm != null -> "${bm.groupValues[1]}• ${bm.groupValues[3]}"
                    nm != null -> "${nm.groupValues[1]}• ${nm.groupValues[3]}"
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

        val newSelection = if (start == end) {
            val diff = newBlock.length - block.length
            TextRange((start + diff).coerceIn(0, newText.length))
        } else {
            TextRange(lineStart, lineStart + newBlock.length)
        }

        return TextFieldValue(newText, newSelection)
    }

    fun applyNumberedList(value: TextFieldValue): TextFieldValue {
        val text = value.text
        val start = value.selection.min
        val end = value.selection.max

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

        val numRegex = Regex("""^(\s*)(\d+\.)\s*(.*)$""")
        val bulletRegex = Regex("""^(\s*)(•|-|\*)\s*(.*)$""")

        val nonEmptyLines = lines.filter { it.isNotBlank() }
        val allNumbered = nonEmptyLines.isNotEmpty() && nonEmptyLines.all { numRegex.matches(it) }

        var counter = 1
        val newLines = lines.map { line ->
            if (allNumbered) {
                val m = numRegex.find(line)
                if (m != null) "${m.groupValues[1]}${m.groupValues[3]}" else line
            } else {
                val nm = numRegex.find(line)
                val bm = bulletRegex.find(line)
                val index = counter++
                when {
                    nm != null -> "${nm.groupValues[1]}$index. ${nm.groupValues[3]}"
                    bm != null -> "${bm.groupValues[1]}$index. ${bm.groupValues[3]}"
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

        val newSelection = if (start == end) {
            val diff = newBlock.length - block.length
            TextRange((start + diff).coerceIn(0, newText.length))
        } else {
            TextRange(lineStart, lineStart + newBlock.length)
        }

        return TextFieldValue(newText, newSelection)
    }

    fun clearFormatting(value: TextFieldValue): TextFieldValue {
        val text = value.text
        val start = value.selection.min
        val end = value.selection.max

        if (start != end) {
            val selected = text.substring(start, end)
            val cleaned = selected
                .replace("**", "")
                .replace("~~", "")
                .replace(Regex("""(?<!\*)\*(?!\*)"""), "")
                .replace(Regex("""(?<!_)_(?!_)"""), "")
                .replace(Regex("""(?m)^(\s*)(•|-|\*)\s*"""), "$1")
                .replace(Regex("""(?m)^(\s*)\d+\.\s*"""), "$1")
            val newText = text.replaceRange(start, end, cleaned)
            return TextFieldValue(newText, TextRange(start, start + cleaned.length))
        }
        return value
    }

    /**
     * Automatically continues bullet or numbered lists on Enter, or exits the list
     * when Enter is pressed on an empty list item (identical to Google Chat, Teams, Slack).
     */
    fun handleValueChange(oldValue: TextFieldValue, newValue: TextFieldValue): TextFieldValue {
        if (newValue.text.length == oldValue.text.length + 1 &&
            oldValue.selection.collapsed &&
            newValue.selection.start == oldValue.selection.start + 1
        ) {
            val insertPos = oldValue.selection.start
            if (insertPos < newValue.text.length && newValue.text[insertPos] == '\n') {
                val textBeforeNl = oldValue.text.substring(0, insertPos)
                val prevNl = textBeforeNl.lastIndexOf('\n')
                val lineStart = if (prevNl == -1) 0 else prevNl + 1
                val prevLine = textBeforeNl.substring(lineStart)

                // Check if prevLine is empty bullet (user pressed Enter on empty bullet -> exit list)
                val emptyBulletMatch = Regex("""^(\s*)(•|-|\*)\s*$""").matchEntire(prevLine)
                if (emptyBulletMatch != null) {
                    val beforeLine = oldValue.text.substring(0, lineStart)
                    val afterCursor = oldValue.text.substring(insertPos)
                    val newText = beforeLine + afterCursor
                    return TextFieldValue(newText, TextRange(lineStart))
                }

                // Check if prevLine is empty number (user pressed Enter on empty number -> exit list)
                val emptyNumMatch = Regex("""^(\s*)(\d+\.)\s*$""").matchEntire(prevLine)
                if (emptyNumMatch != null) {
                    val beforeLine = oldValue.text.substring(0, lineStart)
                    val afterCursor = oldValue.text.substring(insertPos)
                    val newText = beforeLine + afterCursor
                    return TextFieldValue(newText, TextRange(lineStart))
                }

                // Check if prevLine is active bullet item with text
                val bulletMatch = Regex("""^(\s*)(•|-|\*)\s+(.+)$""").matchEntire(prevLine)
                if (bulletMatch != null) {
                    val indent = bulletMatch.groupValues[1]
                    val continuation = "$indent• "
                    val beforeCursor = newValue.text.substring(0, insertPos + 1)
                    val afterCursor = newValue.text.substring(insertPos + 1)
                    val newText = beforeCursor + continuation + afterCursor
                    return TextFieldValue(newText, TextRange(insertPos + 1 + continuation.length))
                }

                // Check if prevLine is active numbered item with text
                val numMatch = Regex("""^(\s*)(\d+)\.\s+(.+)$""").matchEntire(prevLine)
                if (numMatch != null) {
                    val indent = numMatch.groupValues[1]
                    val currentNum = numMatch.groupValues[2].toIntOrNull() ?: 1
                    val continuation = "$indent${currentNum + 1}. "
                    val beforeCursor = newValue.text.substring(0, insertPos + 1)
                    val afterCursor = newValue.text.substring(insertPos + 1)
                    val newText = beforeCursor + continuation + afterCursor
                    return TextFieldValue(newText, TextRange(insertPos + 1 + continuation.length))
                }
            }
        }
        return newValue
    }
}
