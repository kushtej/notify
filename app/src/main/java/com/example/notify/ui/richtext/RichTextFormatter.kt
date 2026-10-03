package com.example.notify.ui.richtext

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

/**
 * Handles formatting and visual transformation of markdown-based rich text
 * for Jetpack Compose UI components.
 */
object RichTextFormatter {

    /**
     * Visual transformation for the editor that highlights markdown syntax in-place
     * without altering character offsets, guaranteeing zero cursor jumping or lag.
     */
    class MarkdownVisualTransformation(
        private val primaryColor: Color,
        private val syntaxColor: Color
    ) : VisualTransformation {
        override fun filter(text: AnnotatedString): TransformedText {
            val raw = text.text
            val builder = AnnotatedString.Builder(raw)

            // 1. Highlight **bold**
            val boldRegex = Regex("""\*\*(.+?)\*\*""")
            for (match in boldRegex.findAll(raw)) {
                val range = match.range
                builder.addStyle(SpanStyle(color = syntaxColor), range.first, range.first + 2)
                builder.addStyle(SpanStyle(color = syntaxColor), range.last - 1, range.last + 1)
                builder.addStyle(SpanStyle(fontWeight = FontWeight.Bold), range.first + 2, range.last - 1)
            }

            // 2. Highlight ~~strikethrough~~
            val strikeRegex = Regex("""~~(.+?)~~""")
            for (match in strikeRegex.findAll(raw)) {
                val range = match.range
                builder.addStyle(SpanStyle(color = syntaxColor), range.first, range.first + 2)
                builder.addStyle(SpanStyle(color = syntaxColor), range.last - 1, range.last + 1)
                builder.addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), range.first + 2, range.last - 1)
            }

            // 3. Highlight *italic* and _italic_
            val italicRegex = Regex("""(?<!\*)\*([^*]+?)\*(?!\*)|(?<!_)_([^_]+?)_(?!_)""")
            for (match in italicRegex.findAll(raw)) {
                val range = match.range
                builder.addStyle(SpanStyle(color = syntaxColor), range.first, range.first + 1)
                builder.addStyle(SpanStyle(color = syntaxColor), range.last, range.last + 1)
                builder.addStyle(SpanStyle(fontStyle = FontStyle.Italic), range.first + 1, range.last)
            }

            // 4. Highlight bullet markers
            val bulletRegex = Regex("""(?m)^(\s*)(•|-|\*)(\s+)""")
            for (match in bulletRegex.findAll(raw)) {
                val symbolGroup = match.groups[2]
                if (symbolGroup != null) {
                    builder.addStyle(
                        SpanStyle(color = primaryColor, fontWeight = FontWeight.Bold),
                        symbolGroup.range.first,
                        symbolGroup.range.last + 1
                    )
                }
            }

            // 5. Highlight numbered list markers
            val numRegex = Regex("""(?m)^(\s*)(\d+\.)(\s+)""")
            for (match in numRegex.findAll(raw)) {
                val numGroup = match.groups[2]
                if (numGroup != null) {
                    builder.addStyle(
                        SpanStyle(color = primaryColor, fontWeight = FontWeight.SemiBold),
                        numGroup.range.first,
                        numGroup.range.last + 1
                    )
                }
            }

            return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
        }
    }

    /**
     * Parses markdown text into a fully formatted AnnotatedString with syntax markers stripped.
     */
    fun formatToAnnotatedString(text: String, primaryColor: Color): AnnotatedString {
        if (text.isEmpty()) return AnnotatedString("")

        val builder = AnnotatedString.Builder()
        val lines = text.split('\n')

        lines.forEachIndexed { index, line ->
            if (index > 0) {
                builder.append("\n")
            }
            val trimmed = line.trimStart()
            val indent = line.substring(0, line.length - trimmed.length)

            val bulletMatch = Regex("""^(•|-|\*)\s*(.*)$""").find(trimmed)
            val numMatch = Regex("""^(\d+\.)\s*(.*)$""").find(trimmed)

            when {
                bulletMatch != null -> {
                    builder.append(indent)
                    builder.pushStyle(SpanStyle(color = primaryColor, fontWeight = FontWeight.Bold))
                    builder.append("•  ")
                    builder.pop()
                    val content = bulletMatch.groupValues[2]
                    appendInlineFormatted(builder, content)
                }
                numMatch != null -> {
                    builder.append(indent)
                    builder.pushStyle(SpanStyle(color = primaryColor, fontWeight = FontWeight.SemiBold))
                    builder.append("${numMatch.groupValues[1]} ")
                    builder.pop()
                    val content = numMatch.groupValues[2]
                    appendInlineFormatted(builder, content)
                }
                else -> {
                    appendInlineFormatted(builder, line)
                }
            }
        }
        return builder.toAnnotatedString()
    }

    private fun appendInlineFormatted(builder: AnnotatedString.Builder, text: String) {
        val regex = Regex("""(\*\*(.+?)\*\*)|(~~(.+?)~~)|((?<!\*)\*(.+?)\*(?!\*))|((?<!_)_(.+?)_(?!_))""")
        var currentIndex = 0
        for (match in regex.findAll(text)) {
            if (match.range.first > currentIndex) {
                builder.append(text.substring(currentIndex, match.range.first))
            }
            val full = match.value
            when {
                full.startsWith("**") && full.endsWith("**") && full.length >= 4 -> {
                    val inner = full.substring(2, full.length - 2)
                    builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    appendInlineFormattedSub(builder, inner, isBold = true)
                    builder.pop()
                }
                full.startsWith("~~") && full.endsWith("~~") && full.length >= 4 -> {
                    val inner = full.substring(2, full.length - 2)
                    builder.pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                    appendInlineFormattedSub(builder, inner, isStrike = true)
                    builder.pop()
                }
                (full.startsWith("*") && full.endsWith("*") && full.length >= 2) ||
                (full.startsWith("_") && full.endsWith("_") && full.length >= 2) -> {
                    val inner = full.substring(1, full.length - 1)
                    builder.pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    builder.append(inner)
                    builder.pop()
                }
                else -> {
                    builder.append(full)
                }
            }
            currentIndex = match.range.last + 1
        }
        if (currentIndex < text.length) {
            builder.append(text.substring(currentIndex))
        }
    }

    private fun appendInlineFormattedSub(
        builder: AnnotatedString.Builder,
        text: String,
        isBold: Boolean = false,
        isStrike: Boolean = false
    ) {
        val subRegex = Regex("""((?<!\*)\*(.+?)\*(?!\*))|((?<!_)_(.+?)_(?!_))""")
        var currentIndex = 0
        for (match in subRegex.findAll(text)) {
            if (match.range.first > currentIndex) {
                builder.append(text.substring(currentIndex, match.range.first))
            }
            val full = match.value
            val inner = full.substring(1, full.length - 1)
            builder.pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
            builder.append(inner)
            builder.pop()
            currentIndex = match.range.last + 1
        }
        if (currentIndex < text.length) {
            builder.append(text.substring(currentIndex))
        }
    }

    /**
     * Converts markdown text into a clean single-line summary with markup stripped
     * and lines joined by " • ". Ideal for list items and notification collapsed text.
     */
    fun toPlainTextSummary(text: String): String {
        if (text.isBlank()) return ""
        val clean = text
            .replace(Regex("""\*\*(.+?)\*\*"""), "$1")
            .replace(Regex("""~~(.+?)~~"""), "$1")
            .replace(Regex("""(?<!\*)\*([^*]+?)\*(?!\*)"""), "$1")
            .replace(Regex("""(?<!_)_([^_]+?)_(?!_)"""), "$1")

        val lines = clean.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { line ->
                line.replace(Regex("""^(•|-|\*)\s*"""), "")
            }

        return lines.joinToString(" • ")
    }
}

/**
 * Reusable Composable to render formatted rich text with proper styling.
 */
@Composable
fun FormattedText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val annotatedString = remember(text, primaryColor) {
        RichTextFormatter.formatToAnnotatedString(text, primaryColor)
    }

    Text(
        text = annotatedString,
        modifier = modifier,
        style = style.copy(lineHeight = 22.sp),
        color = color
    )
}
