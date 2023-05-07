package com.queatz.ailaai.extensions

import android.content.Context
import android.widget.Toast
import com.queatz.ailaai.R

fun Context.showDidntWork() = Toast
    .makeText(this, getString(R.string.didnt_work), Toast.LENGTH_SHORT)
    .show()
