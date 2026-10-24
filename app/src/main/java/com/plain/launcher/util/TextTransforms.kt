package com.plain.launcher.util

import com.plain.launcher.data.model.AppTextCase
import java.util.Locale

object TextTransforms {

    fun applyCase(text: String, textCase: AppTextCase): String {
        return when (textCase) {
            AppTextCase.AS_IS -> text
            AppTextCase.LOWERCASE -> text.lowercase(Locale.getDefault())
            AppTextCase.UPPERCASE -> text.uppercase(Locale.getDefault())
            AppTextCase.CAPITALIZED -> {
                text.split(" ").joinToString(" ") { word ->
                    word.replaceFirstChar { ch ->
                        if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
                    }
                }
            }
        }
    }
}
