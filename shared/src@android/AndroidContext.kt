package id.andreasmlbngaol.nas_project.data

import android.content.Context

/**
 * The Android `Context`, set once from the Application/Activity. Platform code
 * (session storage, file picking) needs it, and it only exists after startup.
 */
internal var androidContext: Context? = null

fun initAndroid(context: Context) {
    androidContext = context.applicationContext
}

internal fun requireContext(): Context =
    requireNotNull(androidContext) { "initAndroid() not called" }
