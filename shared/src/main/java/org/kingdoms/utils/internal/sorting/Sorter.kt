package org.kingdoms.utils.internal.sorting

enum class SortingStrategy {
    AUTOMATIC, IN_PLACE, COPY
}

open class SortingProcessor<T>(
    val original: Iterable<T>,
    val sortingStrategy: SortingStrategy
) {
    companion object {
        @JvmStatic fun <T> copy(list: Iterable<T>) = SortingProcessor(list, SortingStrategy.COPY)
    }

    private var processed0: MutableList<T>? = null

    val processed: MutableList<T> get() = this.processed0 ?: throw IllegalStateException("Not processed yet")

    open fun copyTo(): MutableList<T> = (original as? MutableList)?.size?.let { ArrayList(it) } ?: ArrayList()

    private fun inPlace(): MutableList<T> = original as? MutableList
        ?: throw UnsupportedOperationException("Original list doesn't support in-place sorting")

    fun process(sorter: Sorter<T>): SortingProcessor<T> {
        this.processed0 = process0(sorter)
        return this
    }

    private fun process0(sorter: Sorter<T>): MutableList<T> =
        if (processed0 != null) {
            val nonNullProcessed = processed0!!
            if (sorter.isInPlace(nonNullProcessed)) {
                sorter.sort(nonNullProcessed)
                nonNullProcessed
            } else {
                processed0 = sorter.sort(nonNullProcessed)
                nonNullProcessed
            }
        } else if (sorter.isInPlace(original)) {
            when (sortingStrategy) {
                SortingStrategy.AUTOMATIC, SortingStrategy.IN_PLACE -> {
                    val inPlace = inPlace()
                    sorter.sort(inPlace)
                    inPlace
                }

                SortingStrategy.COPY -> {
                    val copy = copyTo()
                    sorter.sort(copy)
                    copy
                }
            }
        } else {
            when (sortingStrategy) {
                SortingStrategy.AUTOMATIC, SortingStrategy.COPY -> {
                    sorter.sort(original)
                }

                SortingStrategy.IN_PLACE -> {
                    val inPlace = inPlace()
                    val copy = sorter.sort(inPlace)
                    inPlace.clear()
                    inPlace.addAll(copy)
                    inPlace
                }
            }
        }
}

interface Sorter<T> {
    /**
     * Sorts the list with the most efficient way possible.
     * The provided [list] may or may not be sorted in-place or copied
     * depending on [isInPlace].
     */
    fun sort(iterable: Iterable<T>): MutableList<T>

    fun isInPlace(iterable: Iterable<T>): Boolean

    companion object {
        @JvmStatic fun inPlaceIfList(iterable: Iterable<*>) = iterable is MutableList
    }
}
