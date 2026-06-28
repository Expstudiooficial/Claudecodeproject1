package com.expstudio.localai.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** Labelled float slider with a live value readout. */
@Composable
fun FloatParam(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    help: String? = null,
    format: (Float) -> String = { "%.2f".format(it) },
    onChange: (Float) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(format(value), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
        if (help != null) {
            Text(
                help,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(value = value, onValueChange = onChange, valueRange = range, steps = steps)
    }
}

/** Labelled integer slider built on top of [FloatParam]. */
@Composable
fun IntParam(
    label: String,
    value: Int,
    range: IntRange,
    steps: Int = 0,
    help: String? = null,
    onChange: (Int) -> Unit,
) {
    FloatParam(
        label = label,
        value = value.toFloat(),
        range = range.first.toFloat()..range.last.toFloat(),
        steps = steps,
        help = help,
        format = { it.roundToInt().toString() },
        onChange = { onChange(it.roundToInt()) },
    )
}

/** Labelled on/off row with optional helper text. */
@Composable
fun SwitchParam(
    label: String,
    checked: Boolean,
    help: String? = null,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            if (help != null) {
                Text(
                    help,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
