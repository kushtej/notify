package com.example.notify.ui.richtext

import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import androidx.core.text.HtmlCompat

/**
 * Converts formatted reminder descriptions into styled Spanned text
 * for high-priority Android Notifications (BigTextStyle).
 */
object NotificationFormatter {

    /**
     * Converts markdown description into a rich Android Spanned object.
     * Supports bold (<b>), italic (<i>), strikethrough (<s>), bullet points, and numbered lists.
     */
    fun toNotificationSpanned(desc: String): Spanned {
        if (desc.isBlank()) return SpannableString("")

        val lines = desc.split('\n')
        val htmlLines = lines.map { line ->
            val trimmed = line.trimStart()
            val indentLevel = (line.length - trimmed.length) / 2
            val indentHtml = "&nbsp;&nbsp;".repeat(indentLevel)

            val bulletMatch = Regex("""^(•|-|\*)\s*(.*)$""").find(trimmed)
            val numMatch = Regex("""^(\d+\.)\s*(.*)$""").find(trimmed)

            val (prefixHtml, rawContent) = when {
                bulletMatch != null -> "&#8226;&nbsp;&nbsp;" to bulletMatch.groupValues[2]
                numMatch != null -> "<b>${numMatch.groupValues[1]}</b>&nbsp;" to numMatch.groupValues[2]
                else -> "" to line
            }

            var escaped = TextUtils.htmlEncode(rawContent)

            // Convert markdown inline styles to HTML tags
            // 1. Combined bold + italic
            escaped = escaped.replace(Regex("""\*\*\*(.+?)\*\*\*""")) { "<b><i>${it.groupValues[1]}</i></b>" }
            escaped = escaped.replace(Regex("""\*\*_(.+?)_\*\*""")) { "<b><i>${it.groupValues[1]}</i></b>" }
            escaped = escaped.replace(Regex("""_\*\*(.+?)\*\*_""")) { "<i><b>${it.groupValues[1]}</b></i>" }

            // 2. Bold: **text** -> <b>text</b>
            escaped = escaped.replace(Regex("""\*\*(.+?)\*\*""")) { "<b>${it.groupValues[1]}</b>" }
            // 3. Strikethrough: ~~text~~ -> <s>text</s>
            escaped = escaped.replace(Regex("""~~(.+?)~~""")) { "<s>${it.groupValues[1]}</s>" }
            // 4. Italic: *text* or _text_ -> <i>text</i>
            escaped = escaped.replace(Regex("""(?<!\*)\*([^*]+?)\*(?!\*)""")) { "<i>${it.groupValues[1]}</i>" }
            escaped = escaped.replace(Regex("""(?<!_)_([^_]+?)_(?!_)""")) { "<i>${it.groupValues[1]}</i>" }

            indentHtml + prefixHtml + escaped
        }

        val fullHtml = htmlLines.joinToString("<br/>")
        return HtmlCompat.fromHtml(fullHtml, HtmlCompat.FROM_HTML_MODE_COMPACT)
    }

    /**
     * Plain text single-line summary for the notification's collapsed contentText.
     */
    fun toPlainTextSummary(desc: String): String {
        return RichTextFormatter.toPlainTextSummary(desc)
    }
}
