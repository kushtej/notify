package com.example.notify.ui.richtext

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A rich text editor resembling Google Chat / Microsoft Teams chatbox.
 * Offers live WYSIWYG formatting without displaying raw markdown syntax (such as asterisks),
 * with active toolbar states for Bold, Italic, Strikethrough, Bulleted and Numbered lists.
 */
@Composable
fun RichTextEditor(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Details",
    placeholder: String = "Add notes, bullet points, numbers, formatting..."
) {
    // Internal plain-text state and style spans
    var plainTextValue by remember {
        val (text, parsedSpans) = RichTextActions.parseMarkdown(value)
        mutableStateOf(TextFieldValue(text = text, selection = TextRange(text.length)))
    }
    var spans by remember {
        val (_, parsedSpans) = RichTextActions.parseMarkdown(value)
        mutableStateOf(parsedSpans)
    }
    var activeStyles by remember {
        mutableStateOf(setOf<RichStyle>())
    }

    // Keep internal state synchronized when parent changes value externally
    LaunchedEffect(value) {
        val currentSerialized = RichTextActions.serializeToMarkdown(plainTextValue.text, spans)
        if (value != currentSerialized) {
            val (newPlainText, newSpans) = RichTextActions.parseMarkdown(value)
            plainTextValue = plainTextValue.copy(
                text = newPlainText,
                selection = TextRange(newPlainText.length)
            )
            spans = newSpans
            activeStyles = emptySet()
        }
    }

    var isFocused by remember { mutableStateOf(false) }
    val primaryColor = MaterialTheme.colorScheme.primary
    val visualTransformation = remember(spans, primaryColor) {
        RichTextFormatter.WysiwygVisualTransformation(spans, primaryColor)
    }

    val borderColor = when {
        isFocused -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    }

    // Active status for formatting buttons based on current selection / cursor
    val isBoldActive = RichTextActions.isStyleActive(plainTextValue.selection, spans, RichStyle.BOLD, activeStyles)
    val isItalicActive = RichTextActions.isStyleActive(plainTextValue.selection, spans, RichStyle.ITALIC, activeStyles)
    val isStrikeActive = RichTextActions.isStyleActive(plainTextValue.selection, spans, RichStyle.STRIKETHROUGH, activeStyles)
    val isBulletActive = RichTextActions.isLineBulleted(plainTextValue.text, plainTextValue.selection)
    val isNumberActive = RichTextActions.isLineNumbered(plainTextValue.text, plainTextValue.selection)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, borderColor),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Column {
            // Header label
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 14.dp, top = 10.dp, end = 14.dp, bottom = 4.dp)
            )

            // WYSIWYG input area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 110.dp, max = 220.dp)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                BasicTextField(
                    value = plainTextValue,
                    onValueChange = { newVal ->
                        val (processedVal, updatedSpans, newActiveStyles) =
                            RichTextActions.handleUserInput(
                                oldVal = plainTextValue,
                                newVal = newVal,
                                currentSpans = spans,
                                currentActiveStyles = activeStyles
                            )
                        plainTextValue = processedVal
                        spans = updatedSpans
                        activeStyles = newActiveStyles

                        val serialized = RichTextActions.serializeToMarkdown(processedVal.text, updatedSpans)
                        onValueChange(serialized)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp)
                        .onFocusChanged { isFocused = it.isFocused },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    visualTransformation = visualTransformation,
                    decorationBox = { innerTextField ->
                        if (plainTextValue.text.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                            )
                        }
                        innerTextField()
                    }
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Bottom formatting toolbar (Google Chat / Teams chatbox style)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Bold (B)
                ToolbarButton(
                    isActive = isBoldActive,
                    onClick = {
                        val (newVal, newSpans, newActive) = RichTextActions.toggleStyleAction(
                            plainTextValue,
                            spans,
                            RichStyle.BOLD,
                            activeStyles
                        )
                        plainTextValue = newVal
                        spans = newSpans
                        activeStyles = newActive
                        onValueChange(RichTextActions.serializeToMarkdown(newVal.text, newSpans))
                    }
                ) {
                    Text("B", fontWeight = FontWeight.Black, fontSize = 15.sp)
                }

                // Italic (I)
                ToolbarButton(
                    isActive = isItalicActive,
                    onClick = {
                        val (newVal, newSpans, newActive) = RichTextActions.toggleStyleAction(
                            plainTextValue,
                            spans,
                            RichStyle.ITALIC,
                            activeStyles
                        )
                        plainTextValue = newVal
                        spans = newSpans
                        activeStyles = newActive
                        onValueChange(RichTextActions.serializeToMarkdown(newVal.text, newSpans))
                    }
                ) {
                    Text(
                        "I",
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        fontSize = 15.sp
                    )
                }

                // Strikethrough (S)
                ToolbarButton(
                    isActive = isStrikeActive,
                    onClick = {
                        val (newVal, newSpans, newActive) = RichTextActions.toggleStyleAction(
                            plainTextValue,
                            spans,
                            RichStyle.STRIKETHROUGH,
                            activeStyles
                        )
                        plainTextValue = newVal
                        spans = newSpans
                        activeStyles = newActive
                        onValueChange(RichTextActions.serializeToMarkdown(newVal.text, newSpans))
                    }
                ) {
                    Text(
                        "S",
                        textDecoration = TextDecoration.LineThrough,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                ToolbarDivider()

                // Bulleted List
                ToolbarButton(
                    isActive = isBulletActive,
                    onClick = {
                        val (newVal, newSpans) = RichTextActions.toggleBulletList(plainTextValue, spans)
                        plainTextValue = newVal
                        spans = newSpans
                        onValueChange(RichTextActions.serializeToMarkdown(newVal.text, newSpans))
                    }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("•", fontWeight = FontWeight.Black, fontSize = 16.sp)
                        Spacer(Modifier.width(2.dp))
                        Text("≡", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                // Numbered List
                ToolbarButton(
                    isActive = isNumberActive,
                    onClick = {
                        val (newVal, newSpans) = RichTextActions.toggleNumberedList(plainTextValue, spans)
                        plainTextValue = newVal
                        spans = newSpans
                        onValueChange(RichTextActions.serializeToMarkdown(newVal.text, newSpans))
                    }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("1.", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(Modifier.width(1.dp))
                        Text("≡", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                ToolbarDivider()

                // Clear Formatting
                ToolbarButton(
                    isActive = false,
                    onClick = {
                        val (newVal, newSpans, newActive) = RichTextActions.clearFormattingAction(
                            plainTextValue,
                            spans
                        )
                        plainTextValue = newVal
                        spans = newSpans
                        activeStyles = newActive
                        onValueChange(RichTextActions.serializeToMarkdown(newVal.text, newSpans))
                    }
                ) {
                    Text(
                        "T̶",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolbarButton(
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val bgColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val contentColor = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val border = if (isActive) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        border = border,
        modifier = modifier.size(36.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                content()
            }
        }
    }
}

@Composable
private fun ToolbarDivider() {
    Box(
        modifier = Modifier
            .height(20.dp)
            .width(1.dp)
            .padding(horizontal = 2.dp)
            .heightIn(max = 20.dp)
            .size(width = 1.dp, height = 20.dp)
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(width = 1.dp, height = 20.dp)) {
            drawLine(
                color = Color.Gray.copy(alpha = 0.35f),
                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                end = androidx.compose.ui.geometry.Offset(0f, size.height),
                strokeWidth = 2f
            )
        }
    }
}
