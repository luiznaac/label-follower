package com.rafaelfo.labelfollower.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withPackage
import com.lemonappdev.konsist.api.verify.assertFalse
import io.kotest.core.spec.style.StringSpec

// Mechanism-level guard for the "generic port in usecases/, vendor-specific implementation in
// integrations/" rule from AGENTS.md — usecases/ must stay ignorant of both the integrations/
// package and Spotify-specific types, even though nothing in the compiler enforces that on its
// own (unlike the Gradle multi-module split in chameidor/portfolio-2).
class LayeringTest :
    StringSpec({
        "usecases must not import integrations" {
            Konsist
                .scopeFromProject()
                .files
                .withPackage("com.rafaelfo.labelfollower.usecases..")
                .assertFalse { file ->
                    file.imports.any { it.name.startsWith("com.rafaelfo.labelfollower.integrations") }
                }
        }

        "usecases must not reference Spotify-specific types" {
            Konsist
                .scopeFromProject()
                .files
                .withPackage("com.rafaelfo.labelfollower.usecases..")
                .assertFalse { file ->
                    file.imports.any { it.name.contains("Spotify") }
                }
        }
    })
