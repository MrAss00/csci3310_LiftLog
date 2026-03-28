package edu.cuhk.csci3310.liftlog

fun String.titlecase(): String {
    return this.split(' ').joinToString(" ") { word ->
        word.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase() else it.toString()
        }
    }
}
