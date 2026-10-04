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
 * Handles visual styling and transformations for rich text components.
 */
object RichTextFormatter {

    /**
     * Visual transformation for the WYSIWYG editor. Renders style spans directly
     * over plain text without inserting or deleting characters, guaranteeing
     * OffsetMapping.Identity (zero offset bugs or cursor jitter).
     */
    class WysiwygVisualTransformation(
        private val spans: List<StyleSpan>,
        private val primaryColor: Color
    ) : VisualTransformation {
        override fun filter(text: AnnotatedString): TransformedText {
            val raw = text.text
            val builder = AnnotatedString.Builder(raw)

            // 1. Apply style spans (Bold, Italic, Strikethrough)
            for (span in spans) {
                val start = span.start.coerceIn(0, raw.length)
                val end = span.end.coerceIn(0, raw.length)
                if (start < end) {
                    val spanStyle = when (span.style) {
                        RichStyle.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
                        RichStyle.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
                        RichStyle.STRIKETHROUGH -> SpanStyle(textDecoration = TextDecoration.LineThrough)
                    }
                    builder.addStyle(spanStyle, start, end)
                }
            }

            // 2. Highlight bullet markers
            val bulletRegex = Regex("""(?m)^(\s*)(•)(\s+)""")
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

            // 3. Highlight numbered list markers
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
     * Parses markdown text into a fully formatted AnnotatedString with styled spans,
     * bullets, and numbers.
     */
    fun formatToAnnotatedString(text: String, primaryColor: Color): AnnotatedString {
        if (text.isEmpty()) return AnnotatedString("")

        val (plain, spans) = RichTextActions.parseMarkdown(text)
        val builder = AnnotatedString.Builder(plain)

        for (span in spans) {
            val start = span.start.coerceIn(0, plain.length)
            val end = span.end.coerceIn(0, plain.length)
            if (start < end) {
                val spanStyle = when (span.style) {
                    RichStyle.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
                    RichStyle.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
                    RichStyle.STRIKETHROUGH -> SpanStyle(textDecoration = TextDecoration.LineThrough)
                }
                builder.addStyle(spanStyle, start, end)
            }
        }

        // Highlight bullet markers
        val bulletRegex = Regex("""(?m)^(\s*)(•)(\s+)""")
        for (match in bulletRegex.findAll(plain)) {
            val symbolGroup = match.groups[2]
            if (symbolGroup != null) {
                builder.addStyle(
                    SpanStyle(color = primaryColor, fontWeight = FontWeight.Bold),
                    symbolGroup.range.first,
                    symbolGroup.range.last + 1
                )
            }
        }

        // Highlight numbered list markers
        val numRegex = Regex("""(?m)^(\s*)(\d+\.)(\s+)""")
        for (match in numRegex.findAll(plain)) {
            val numGroup = match.groups[2]
            if (numGroup != null) {
                builder.addStyle(
                    SpanStyle(color = primaryColor, fontWeight = FontWeight.SemiBold),
                    numGroup.range.first,
                    numGroup.range.last + 1
                )
            }
        }

        return builder.toAnnotatedString()
    }

    /**
     * Converts markdown text into a clean single-line summary with markup stripped
     * and lines joined by " • ". Ideal for list items and notification collapsed text.
     */
    fun toPlainTextSummary(text: String): String {
        if (text.isBlank()) return ""
        val (plain, _) = RichTextActions.parseMarkdown(text)
        val lines = plain.lines()
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
