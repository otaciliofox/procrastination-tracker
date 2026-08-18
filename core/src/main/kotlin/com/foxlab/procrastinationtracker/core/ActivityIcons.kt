package com.foxlab.procrastinationtracker.core

import java.text.Normalizer

/**
 * Which icon *key* an activity should carry. The keys are the shared vocabulary; turning a key
 * into an actual drawable is each platform's job (Material icons on the phone, whatever fits a
 * round screen on the watch), which is why only this half lives in `:core`.
 *
 * The guessing rule matters beyond looks: a user who types "Academia" gets the training icon
 * without opening the picker, and both apps have to guess the same way.
 */
object ActivityIcons {

    const val WORK = "work"
    const val SCHOOL = "school"
    const val COFFEE = "coffee"
    const val FOCUS = "focus"
    const val FITNESS = "fitness"
    const val BOOK = "book"
    const val HOBBY = "hobby"
    const val REST = "rest"
    const val HOME = "home"
    const val SLEEP = "sleep"
    const val FOOD = "food"
    const val STAR = "star"

    val ALL_KEYS = listOf(WORK, SCHOOL, COFFEE, FOCUS, FITNESS, BOOK, HOBBY, REST, HOME, SLEEP, FOOD, STAR)

    /**
     * The key for an activity: an explicit choice wins, otherwise it is inferred from the title,
     * and [STAR] is the catch-all.
     */
    fun keyFor(title: String, explicitKey: String? = null): String {
        if (!explicitKey.isNullOrBlank() && explicitKey in ALL_KEYS) return explicitKey

        val normalized = Normalizer.normalize(title, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase()

        return when {
            ActivityRules.isProcrastination(title) -> COFFEE
            "trabalh" in normalized || "job" in normalized || "work" in normalized -> WORK
            "estud" in normalized || "aul" in normalized || "curso" in normalized || "facul" in normalized -> SCHOOL
            "foco" in normalized || "meta" in normalized -> FOCUS
            "trein" in normalized || "exerc" in normalized || "academia" in normalized || "corr" in normalized -> FITNESS
            "leitur" in normalized || "livro" in normalized || "ler" in normalized -> BOOK
            "hobby" in normalized || "lazer" in normalized || "arte" in normalized || "desenh" in normalized -> HOBBY
            "pausa" in normalized || "intervalo" in normalized || "descans" in normalized ||
                "relax" in normalized || "medit" in normalized -> REST
            "casa" in normalized || "tarefa" in normalized || "limp" in normalized -> HOME
            "sono" in normalized || "dormir" in normalized || "soneca" in normalized -> SLEEP
            "comer" in normalized || "almoc" in normalized || "jantar" in normalized ||
                "refeic" in normalized || "lanche" in normalized -> FOOD
            else -> STAR
        }
    }
}
