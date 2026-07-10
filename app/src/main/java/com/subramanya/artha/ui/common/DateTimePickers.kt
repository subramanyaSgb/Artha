package com.subramanya.artha.ui.common

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.subramanya.artha.R
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * Shared Material3 date + time pickers so any screen that edits a timestamp behaves the
 * same. Both return via callbacks; the caller owns the epoch-millis state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArthaDatePickerDialog(initialMillis: Long, onConfirm: (Long) -> Unit, onDismiss: () -> Unit) {
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { pickerState.selectedDateMillis?.let(onConfirm) ?: onDismiss() }) {
                Text(stringResource(R.string.common_ok))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    ) {
        DatePicker(state = pickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArthaTimePickerDialog(initialMillis: Long, onConfirm: (hour: Int, minute: Int) -> Unit, onDismiss: () -> Unit) {
    val initial = remember(initialMillis) {
        Instant.fromEpochMilliseconds(initialMillis).toLocalDateTime(TimeZone.currentSystemDefault())
    }
    val pickerState = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(pickerState.hour, pickerState.minute) }) {
                Text(stringResource(R.string.common_ok))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
        text = { TimePicker(state = pickerState) },
    )
}

/**
 * Replaces the time-of-day on [dateMillis] with [hour]:[minute], keeping the calendar date.
 * Pure epoch math via kotlinx-datetime, so DST/zone handling is correct.
 */
fun mergeTimeKeepingDate(hour: Int, minute: Int, dateMillis: Long): Long {
    val tz = TimeZone.currentSystemDefault()
    val date = Instant.fromEpochMilliseconds(dateMillis).toLocalDateTime(tz).date
    val newDateTime = LocalDateTime(date.year, date.monthNumber, date.dayOfMonth, hour, minute)
    return newDateTime.toInstant(tz).toEpochMilliseconds()
}
