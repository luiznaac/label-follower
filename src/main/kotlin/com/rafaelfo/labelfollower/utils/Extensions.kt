package com.rafaelfo.labelfollower.utils

fun <T, R> Iterable<T>.mapToSet(func: (T) -> R) = map(func).toSet()
