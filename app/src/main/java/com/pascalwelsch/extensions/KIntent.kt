package com.pascalwelsch.extensions

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle

/**
 * Extensions for simpler launching of Activities
 */

inline fun <reified T : Any> Activity.launchActivity(
        requestCode: Int = -1,
        options: Bundle? = null,
        noinline init: Intent.() -> Unit = {}) {

    val intent = newIntent<T>(this)
    intent.init()
    startActivityForResult(intent, requestCode, options)
}

@SuppressLint("ObsoleteSdkInt")
inline fun <reified T : Any> Context.launchActivity(
        options: Bundle? = null,
        noinline init: Intent.() -> Unit = {}) {

    val intent = newIntent<T>(this)
    intent.init()
    startActivity(intent, options)
}

inline fun <reified T : Any> newIntent(context: Context): Intent =
        Intent(context, T::class.java)