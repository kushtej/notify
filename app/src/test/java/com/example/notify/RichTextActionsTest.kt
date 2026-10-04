package com.example.notify

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.example.notify.ui.richtext.RichStyle
import com.example.notify.ui.richtext.RichTextActions
import com.example.notify.ui.richtext.RichTextFormatter
import com.example.notify.ui.richtext.StyleSpan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RichTextActionsTest {

    @Test
    fun testParseMarkdownSimpleBold() {
        val (plainText, spans) = RichTextActions.parseMarkdown("Hello **world**")
        assertEquals("Hello world", plainText)
        assertEquals(1, spans.size)
        assertEquals(StyleSpan(6, 11, RichStyle.BOLD), spans[0])
    }

    @Test
    fun testParseMarkdownMultipleStyles() {
        val (plainText, spans) = RichTextActions.parseMarkdown("**Bold** and *italic* and ~~strike~~")
        assertEquals("Bold and italic and strike", plainText)
        assertTrue(spans.any { it.style == RichStyle.BOLD && it.start == 0 && it.end == 4 })
        assertTrue(spans.any { it.style == RichStyle.ITALIC && it.start == 9 && it.end == 15 })
        assertTrue(spans.any { it.style == RichStyle.STRIKETHROUGH && it.start == 20 && it.end == 26 })
    }

    @Test
    fun testParseMarkdownNestedStyles() {
        val (plainText, spans) = RichTextActions.parseMarkdown("***Important***")
        assertEquals("Important", plainText)
        assertTrue(spans.any { it.style == RichStyle.BOLD && it.start == 0 && it.end == 9 })
        assertTrue(spans.any { it.style == RichStyle.ITALIC && it.start == 0 && it.end == 9 })
    }

    @Test
    fun testSerializeToMarkdown() {
        val plainText = "Hello world"
        val spans = listOf(StyleSpan(6, 11, RichStyle.BOLD))
        val serialized = RichTextActions.serializeToMarkdown(plainText, spans)
        assertEquals("Hello **world**", serialized)
    }

    @Test
    fun testSerializeMultipleSpans() {
        val plainText = "Important task completed"
        val spans = listOf(
            StyleSpan(0, 9, RichStyle.BOLD),
            StyleSpan(10, 14, RichStyle.ITALIC),
            StyleSpan(15, 24, RichStyle.STRIKETHROUGH)
        )
        val serialized = RichTextActions.serializeToMarkdown(plainText, spans)
        assertEquals("**Important** *task* ~~completed~~", serialized)
    }

    @Test
    fun testRoundTripMarkdown() {
        val input = "**Urgent:**\n• Buy *groceries*\n• Call ~~doctor~~"
        val (plain, spans) = RichTextActions.parseMarkdown(input)
        val serialized = RichTextActions.serializeToMarkdown(plain, spans)
        assertEquals(input, serialized)
    }

    @Test
    fun testToggleBoldOnSelection() {
        val initial = TextFieldValue("Hello world", TextRange(6, 11))
        val (val1, spans1, _) = RichTextActions.toggleStyleAction(
            initial, emptyList(), RichStyle.BOLD, emptySet()
        )
        assertEquals("Hello world", val1.text)
        assertEquals(1, spans1.size)
        assertEquals(StyleSpan(6, 11, RichStyle.BOLD), spans1[0])

        // Toggle bold off on the same selection
        val (_, spans2, _) = RichTextActions.toggleStyleAction(
            val1, spans1, RichStyle.BOLD, emptySet()
        )
        assertTrue(spans2.isEmpty())
    }

    @Test
    fun testToggleItalicAndStrike() {
        val initial = TextFieldValue("Quick brown fox", TextRange(0, 5))
        val (_, spans1, _) = RichTextActions.toggleStyleAction(
            initial, emptyList(), RichStyle.ITALIC, emptySet()
        )
        assertEquals(1, spans1.size)
        assertEquals(StyleSpan(0, 5, RichStyle.ITALIC), spans1[0])

        val selectFox = TextFieldValue("Quick brown fox", TextRange(12, 15))
        val (_, spans2, _) = RichTextActions.toggleStyleAction(
            selectFox, spans1, RichStyle.STRIKETHROUGH, emptySet()
        )
        assertEquals(2, spans2.size)
        assertTrue(spans2.any { it.style == RichStyle.ITALIC && it.start == 0 && it.end == 5 })
        assertTrue(spans2.any { it.style == RichStyle.STRIKETHROUGH && it.start == 12 && it.end == 15 })
    }

    @Test
    fun testTypingInsideSpanExpandsSpan() {
        val oldText = "Hello world"
        val spans = listOf(StyleSpan(6, 11, RichStyle.BOLD)) // "world" is bold
        val newText = "Hello wonderful world" // inserted "wonderful " at index 6
        val adjusted = RichTextActions.adjustSpansForEdit(spans, oldText, newText, emptySet())
        assertEquals(1, adjusted.size)
        assertEquals(StyleSpan(6, 21, RichStyle.BOLD), adjusted[0])
    }

    @Test
    fun testDeletingInsideSpanShrinksSpan() {
        val oldText = "Hello world"
        val spans = listOf(StyleSpan(6, 11, RichStyle.BOLD)) // "world" is bold
        val newText = "Hello word" // deleted 'l' at index 9
        val adjusted = RichTextActions.adjustSpansForEdit(spans, oldText, newText, emptySet())
        assertEquals(1, adjusted.size)
        assertEquals(StyleSpan(6, 10, RichStyle.BOLD), adjusted[0])
    }

    @Test
    fun testBulletListToggle() {
        val initial = TextFieldValue("Milk\nEggs\nBread", TextRange(0, 15))
        val (bulleted, _) = RichTextActions.toggleBulletList(initial, emptyList())
        assertEquals("• Milk\n• Eggs\n• Bread", bulleted.text)

        // Toggle bullets off
        val selectAll = TextFieldValue(bulleted.text, TextRange(0, bulleted.text.length))
        val (unbulleted, _) = RichTextActions.toggleBulletList(selectAll, emptyList())
        assertEquals("Milk\nEggs\nBread", unbulleted.text)
    }

    @Test
    fun testNumberedListToggle() {
        val initial = TextFieldValue("Item A\nItem B", TextRange(0, 13))
        val (numbered, _) = RichTextActions.toggleNumberedList(initial, emptyList())
        assertEquals("1. Item A\n2. Item B", numbered.text)

        val selectAll = TextFieldValue(numbered.text, TextRange(0, numbered.text.length))
        val (unnumbered, _) = RichTextActions.toggleNumberedList(selectAll, emptyList())
        assertEquals("Item A\nItem B", unnumbered.text)
    }

    @Test
    fun testAutoContinuationBulletOnEnter() {
        val oldVal = TextFieldValue("• Milk", TextRange(6))
        val newVal = TextFieldValue("• Milk\n", TextRange(7))
        val (result, _, _) = RichTextActions.handleUserInput(oldVal, newVal, emptyList(), emptySet())
        assertEquals("• Milk\n• ", result.text)
        assertEquals(9, result.selection.start)
    }

    @Test
    fun testEmptyBulletExitsListOnEnter() {
        val oldVal = TextFieldValue("• Item 1\n• ", TextRange(11))
        val newVal = TextFieldValue("• Item 1\n• \n", TextRange(12))
        val (result, _, _) = RichTextActions.handleUserInput(oldVal, newVal, emptyList(), emptySet())
        assertEquals("• Item 1\n", result.text)
    }

    @Test
    fun testAutoContinuationNumberedOnEnter() {
        val oldVal = TextFieldValue("1. Task one", TextRange(11))
        val newVal = TextFieldValue("1. Task one\n", TextRange(12))
        val (result, _, _) = RichTextActions.handleUserInput(oldVal, newVal, emptyList(), emptySet())
        assertEquals("1. Task one\n2. ", result.text)
        assertEquals(15, result.selection.start)
    }

    @Test
    fun testEmptyNumberedExitsListOnEnter() {
        val oldVal = TextFieldValue("1. Task one\n2. ", TextRange(15))
        val newVal = TextFieldValue("1. Task one\n2. \n", TextRange(16))
        val (result, _, _) = RichTextActions.handleUserInput(oldVal, newVal, emptyList(), emptySet())
        assertEquals("1. Task one\n", result.text)
    }

    @Test
    fun testAutoBulletOnTypingDashOrAsterisk() {
        val oldVal = TextFieldValue("-", TextRange(1))
        val newVal = TextFieldValue("- ", TextRange(2))
        val (result, _, _) = RichTextActions.handleUserInput(oldVal, newVal, emptyList(), emptySet())
        assertEquals("• ", result.text)
        assertEquals(2, result.selection.start)
    }

    @Test
    fun testClearFormattingAction() {
        val text = "Hello world"
        val spans = listOf(
            StyleSpan(0, 5, RichStyle.BOLD),
            StyleSpan(6, 11, RichStyle.ITALIC)
        )
        val initial = TextFieldValue(text, TextRange(0, 11))
        val (_, clearedSpans, _) = RichTextActions.clearFormattingAction(initial, spans)
        assertTrue(clearedSpans.isEmpty())
    }

    @Test
    fun testIsStyleActive() {
        val spans = listOf(StyleSpan(0, 5, RichStyle.BOLD))
        // Inside bold range
        assertTrue(RichTextActions.isStyleActive(TextRange(2, 4), spans, RichStyle.BOLD, emptySet()))
        // Outside bold range
        assertFalse(RichTextActions.isStyleActive(TextRange(6, 10), spans, RichStyle.BOLD, emptySet()))
        // Collapsed cursor at boundary inside
        assertTrue(RichTextActions.isStyleActive(TextRange(3), spans, RichStyle.BOLD, emptySet()))
        // Collapsed cursor in pending active styles
        assertTrue(RichTextActions.isStyleActive(TextRange(8), spans, RichStyle.BOLD, setOf(RichStyle.BOLD)))
    }

    @Test
    fun testPlainTextSummary() {
        val raw = "**Urgent:**\n• Buy groceries\n• Call doctor"
        val summary = RichTextFormatter.toPlainTextSummary(raw)
        assertEquals("Urgent: • Buy groceries • Call doctor", summary)
    }
}
