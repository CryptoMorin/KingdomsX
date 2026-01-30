package org.kingdoms.utils.internal.string

/**
 * A class that pads words and componenets in a sentence to have the same spacing.
 * 1. (Bob) (is) (going) (to) (work).
 * 2. (Jack) (is) (not going) (to) (the hospital).
 *
 * Padding: (Note: the following docs spacing will not show correctly in preview mode)
 *
 * 1. Bob  is going     to work.
 * 2. Jack is not going to the hospital.
 */
class StringPadder {
    private val sentences: MutableList<Sentence> = mutableListOf()
    private var expectedSentenceWordCount: Int = -1
    private var defaultValue: String? = null

    private class Sentence(var words: MutableList<String>)

    fun pad(vararg words: Any): StringPadder {
        require(words.isNotEmpty()) { "Cannot pad sentence with no words" }
        require(expectedSentenceWordCount == -1 || words.size == expectedSentenceWordCount)
        { "Expected sentence word count $expectedSentenceWordCount but this one is ${words.size} (${words.contentToString()})" }

        this.expectedSentenceWordCount = words.size
        this.sentences.add(Sentence(words.map { it.toString() }.toMutableList()))
        return this
    }

    fun pad(rows: List<Map<*, *>>): StringPadder {
        rows.forEach { row ->
            val elements = mutableListOf<String>()
            row.forEach { column ->
                elements.apply {
                    add(column.key.toString())
                    if (column.value !== null && column.value.toString().isNotEmpty()) {
                        add(": " + column.value.toString())
                    } else {
                        add("")
                    }
                    add(" | ")
                }
            }

            elements.removeLast() // Remove the last " | "
            pad(*elements.toTypedArray())
        }
        return this
    }

    fun padIfEmpty(string: String): StringPadder {
        this.defaultValue = string
        return this
    }

    fun getPadded(): List<String> {
        if (expectedSentenceWordCount == -1 && defaultValue !== null) return arrayListOf(defaultValue!!)
        require(expectedSentenceWordCount != -1) { "No sentences added to pad ($sentences)" }

        for (i in 0..<expectedSentenceWordCount) {
            val words = ArrayList<String>(this.sentences.size)
            for (sentence in this.sentences) {
                words.add(sentence.words[i])
            }
            val maxLength = words.maxBy { it.length }.length
            for (sentence in sentences) {
                val word = sentence.words[i]
                if (word.length < maxLength) {
                    val padded = word.padEnd(length = maxLength, padChar = ' ')
                    sentence.words[i] = padded
                }
            }
        }

        return this.sentences.map { it.words.joinToString("") }
    }

    fun getPaddedString(separator: String): String = getPadded().joinToString(separator)

    @Suppress("POTENTIALLY_NON_REPORTED_ANNOTATION")
    @Deprecated(
        message = "Use getPadded() instead",
        replaceWith = ReplaceWith("getPadded()"),
        level = DeprecationLevel.ERROR
    )
    // How the hell do we suppress: "Deprecations and opt-ins on a method overridden from 'Any' may not be reported."
    override fun toString() = throw UnsupportedOperationException("Use getPadded() instead")
}