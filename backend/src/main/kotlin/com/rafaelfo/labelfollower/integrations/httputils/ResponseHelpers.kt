package com.rafaelfo.labelfollower.integrations.httputils

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

inline fun <reified T> HttpResult.parsedBody(): T =
    GsonBuilder().create().fromJson(body, object : TypeToken<T>() {}.type)

fun Any.toJson(): String = GsonBuilder().create().toJson(this)
