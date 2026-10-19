package com.muaythai.arena.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent

/**
 * Hands plain text to the system share sheet. The app itself has no network access: where the text goes next
 * (a messenger, email, a note) is the moderator's choice in the sheet.
 */
fun shareText(context: Context, subject: String, text: String): Boolean {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    return try {
        context.startActivity(Intent.createChooser(send, subject))
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}
