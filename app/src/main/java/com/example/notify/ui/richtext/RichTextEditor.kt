package com.example.notify.ui.richtext

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A rich text editor resembling Google Chat / Microsoft Teams textbox.
 * Features a dedicated formatting toolbar (Bold, Italic, Strikethrough, Bullets, Numbers, Clear)
 * and an interactive Write / Preview toggle.
 */
@Composable
fun RichTextEditor(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Details (Optional)",
    placeholder: String = "Add notes, bullet points, numbers, formatting..."
) {
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = value, selection = TextRange(value.length)))
    }

    // Keep internal state synchronized if parent changes value externally
    LaunchedEffect(value) {
        if (value != textFieldValue.text) {
            textFieldValue = textFieldValue.copy(
                text = value,
                selection = TextRange(value.length)
            )
        }
    }

    var isPreviewMode by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val syntaxColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    val visualTransformation = remember(primaryColor, syntaxColor) {
        RichTextFormatter.MarkdownVisualTransformation(primaryColor, syntaxColor)
    }

    val borderColor = when {
        isFocused -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, borderColor),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Column {
            // Header: Label and Write/Preview tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Segmented toggle: Write vs Preview
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    SegmentButton(
                        text = "Write",
                        isSelected = !isPreviewMode,
                        onClick = { isPreviewMode = false }
                    )
                    SegmentButton(
                        text = "Preview",
                        isSelected = isPreviewMode,
                        onClick = { isPreviewMode = true }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            if (!isPreviewMode) {
                // Input area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 240.dp)
                        .padding(12.dp)
                ) {
                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = { newVal ->
                            val processed = RichTextActions.handleValueChange(textFieldValue, newVal)
                            textFieldValue = processed
                            onValueChange(processed.text)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp)
                            .onFocusChanged { isFocused = it.isFocused },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 22.sp
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        visualTransformation = visualTransformation,
                        decorationBox = { innerTextField ->
                            if (textFieldValue.text.isEmpty()) {
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

                // Bottom Formatting Toolbar (Teams / Google Chat style)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Bold
                    ToolbarButton(
                        onClick = {
                            val updated = RichTextActions.applyBold(textFieldValue)
                            textFieldValue = updated
                            onValueChange(updated.text)
                        }
                    ) {
                        Text("B", fontWeight = FontWeight.Black, fontSize = 15.sp)
                    }

                    // Italic
                    ToolbarButton(
                        onClick = {
                            val updated = RichTextActions.applyItalic(textFieldValue)
                            textFieldValue = updated
                            onValueChange(updated.text)
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

                    // Strikethrough
                    ToolbarButton(
                        onClick = {
                            val updated = RichTextActions.applyStrikethrough(textFieldValue)
                            textFieldValue = updated
                            onValueChange(updated.text)
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

                    // Bullet List
                    ToolbarButton(
                        onClick = {
                            val updated = RichTextActions.applyBulletList(textFieldValue)
                            textFieldValue = updated
                            onValueChange(updated.text)
                        }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("•", fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(2.dp))
                            Text("≡", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    // Numbered List
                    ToolbarButton(
                        onClick = {
                            val updated = RichTextActions.applyNumberedList(textFieldValue)
                            textFieldValue = updated
                            onValueChange(updated.text)
                        }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("1.", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(1.dp))
                            Text("≡", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    ToolbarDivider()

                    // Clear Formatting
                    ToolbarButton(
                        onClick = {
                            val updated = RichTextActions.clearFormatting(textFieldValue)
                            textFieldValue = updated
                            onValueChange(updated.text)
                        }
                    ) {
                        Text(
                            "T̶",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        )
                    }
                }
            } else {
                // Preview mode
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp)
                        .padding(14.dp)
                ) {
                    if (textFieldValue.text.isBlank()) {
                        Text(
                            text = "No details to preview. Switch to \"Write\" to add formatted notes.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    } else {
                        FormattedText(
                            text = textFieldValue.text,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolbarButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = Color.Transparent,
        modifier = modifier.size(34.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}

@Composable
private fun ToolbarDivider() {
    Box(
        modifier = Modifier
            .height(18.dp)
            .width(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    )
}

@Composable
private fun SegmentButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}
