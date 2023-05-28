package com.queatz.ailaai.extensions

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import com.queatz.ailaai.R

fun Context.showDidntWork() = toast(getString(R.string.didnt_work))

fun Context.toast(@StringRes stringRes: Int) = toast(getString(stringRes))

fun Context.toast(text: String, duration: Int = Toast.LENGTH_SHORT) = Toast
    .makeText(this, text, duration)
    .show()
