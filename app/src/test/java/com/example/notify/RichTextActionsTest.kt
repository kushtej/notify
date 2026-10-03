package com.example.notify

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.example.notify.ui.richtext.RichTextActions
import com.example.notify.ui.richtext.RichTextFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class RichTextActionsTest {

    @Test
    fun testBoldWrapAndUnwrap() {
        val initial = TextFieldValue("Hello world", TextRange(6, 11))
        val bolded = RichTextActions.applyBold(initial)
        assertEquals("Hello **world**", bolded.text)

        // Select the bolded word and unwrap
        val selectBold = TextFieldValue(bolded.text, TextRange(8, 13))
        val unbolded = RichTextActions.applyBold(selectBold)
        assertEquals("Hello world", unbolded.text)
    }

    @Test
    fun testBoldEmptySelection() {
        val initial = TextFieldValue("Hello ", TextRange(6))
        val bolded = RichTextActions.applyBold(initial)
        assertEquals("Hello ****", bolded.text)
        assertEquals(8, bolded.selection.start)
    }

    @Test
    fun testItalicWrap() {
        val initial = TextFieldValue("Hello world", TextRange(0, 5))
        val italicized = RichTextActions.applyItalic(initial)
        assertEquals("*Hello* world", italicized.text)
    }

    @Test
    fun testStrikethroughWrap() {
        val initial = TextFieldValue("Important task", TextRange(0, 9))
        val striked = RichTextActions.applyStrikethrough(initial)
        assertEquals("~~Important~~ task", striked.text)
    }

    @Test
    fun testBulletListToggle() {
        val initial = TextFieldValue("Milk\nEggs\nBread", TextRange(0, 15))
        val bulleted = RichTextActions.applyBulletList(initial)
        assertEquals("• Milk\n• Eggs\n• Bread", bulleted.text)

        // Toggle bullets off
        val selectAll = TextFieldValue(bulleted.text, TextRange(0, bulleted.text.length))
        val unbulleted = RichTextActions.applyBulletList(selectAll)
        assertEquals("Milk\nEggs\nBread", unbulleted.text)
    }

    @Test
    fun testNumberedListToggle() {
        val initial = TextFieldValue("Item A\nItem B", TextRange(0, 13))
        val numbered = RichTextActions.applyNumberedList(initial)
        assertEquals("1. Item A\n2. Item B", numbered.text)

        val selectAll = TextFieldValue(numbered.text, TextRange(0, numbered.text.length))
        val unnumbered = RichTextActions.applyNumberedList(selectAll)
        assertEquals("Item A\nItem B", unnumbered.text)
    }

    @Test
    fun testBulletAutoContinuationOnEnter() {
        val oldVal = TextFieldValue("• Milk", TextRange(6))
        val newVal = TextFieldValue("• Milk\n", TextRange(7))
        val result = RichTextActions.handleValueChange(oldVal, newVal)
        assertEquals("• Milk\n• ", result.text)
        assertEquals(9, result.selection.start)
    }

    @Test
    fun testNumberedAutoContinuationOnEnter() {
        val oldVal = TextFieldValue("1. Task one", TextRange(11))
        val newVal = TextFieldValue("1. Task one\n", TextRange(12))
        val result = RichTextActions.handleValueChange(oldVal, newVal)
        assertEquals("1. Task one\n2. ", result.text)
        assertEquals(15, result.selection.start)
    }

    @Test
    fun testEmptyBulletExitsListOnEnter() {
        val oldVal = TextFieldValue("• Item 1\n• ", TextRange(11))
        val newVal = TextFieldValue("• Item 1\n• \n", TextRange(12))
        val result = RichTextActions.handleValueChange(oldVal, newVal)
        assertEquals("• Item 1\n", result.text)
    }

    @Test
    fun testPlainTextSummary() {
        val raw = "**Urgent:**\n• Buy groceries\n• Call doctor"
        val summary = RichTextFormatter.toPlainTextSummary(raw)
        assertEquals("Urgent: • Buy groceries • Call doctor", summary)
    }
}
