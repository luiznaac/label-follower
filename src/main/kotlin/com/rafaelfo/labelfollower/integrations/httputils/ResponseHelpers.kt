package com.rafaelfo.labelfollower.integrations.httputils

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import okhttp3.Response

inline fun <reified T> Response.parsedBody(): T =
    GsonBuilder().create().fromJson(body!!.string(), object: TypeToken<T>() {}.type)

fun Any.toJson(): String = GsonBuilder().create().toJson(this)
