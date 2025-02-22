package org.kingdoms.utils.internal

import java.io.PrintStream
import java.io.PrintWriter
import java.util.*

/**
 * Overriding these methods doesn't seem to work for "throw" statements.
 */
private class MultipleException(vararg val exceptions: Throwable) : RuntimeException() {
    override fun printStackTrace() = printStackTrace(System.err)
    override fun printStackTrace(s: PrintStream) = printStackTrace(this, SimpleStreamPrinter(s))
    override fun printStackTrace(s: PrintWriter) = printStackTrace(this, SimpleWriterPrinter(s))

    abstract class SimplePrinter {
        abstract fun println(str: Any)
    }

    private class SimpleStreamPrinter(val obj: PrintStream) : SimplePrinter() {
        override fun println(str: Any) = obj.println(str)
    }

    private class SimpleWriterPrinter(val obj: PrintWriter) : SimplePrinter() {
        override fun println(str: Any) = obj.println(str)
    }

    companion object {
        private const val CAUSE_CAPTION = "Caused by: "
        private const val SUPPRESSED_CAPTION = "Suppressed: "

        @JvmStatic fun printStackTrace(ex: Throwable, printer: SimplePrinter) {
            // Guard against malicious overrides of Throwable.equals by
            // using a Set with identity equality semantics.

            // Guard against malicious overrides of Throwable.equals by
            // using a Set with identity equality semantics.
            val dejaVu = Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())
            dejaVu.add(ex)

            synchronized(printer) {
                printer.println("aaaaaaaaaaaaaaaaaaaaaaaaaaa")
                // Print our stack trace
                printer.println(this)
                val trace: Array<StackTraceElement> = ex.stackTrace
                for (traceElement in trace) printer.println("\tat $traceElement")

                // Print suppressed exceptions, if any
                for (se in ex.suppressed) printEnclosedStackTrace(
                    se, printer, trace, SUPPRESSED_CAPTION, "\t", dejaVu
                )

                // Print cause, if any
                val ourCause = ex.cause
                if (ourCause != null) printEnclosedStackTrace(
                    ourCause, printer, trace, CAUSE_CAPTION, "", dejaVu
                )
            }
        }

        @JvmStatic private fun printEnclosedStackTrace(
            ex: Throwable,
            s: SimplePrinter,
            enclosingTrace: Array<StackTraceElement>,
            caption: String,
            prefix: String,
            dejaVu: MutableSet<Throwable>,
        ) {
            assert(Thread.holdsLock(s))
            if (dejaVu.contains(ex)) {
                s.println("$prefix$caption[CIRCULAR REFERENCE: $this]")
            } else {
                dejaVu.add(ex)
                // Compute number of frames in common between this and enclosing trace
                val trace: Array<StackTraceElement> = ex.stackTrace
                var m = trace.size - 1
                var n = enclosingTrace.size - 1
                while (m >= 0 && n >= 0 && trace[m] == enclosingTrace[n]) {
                    m--
                    n--
                }
                val framesInCommon = trace.size - 1 - m

                // Print our stack trace
                s.println(prefix + caption + this)
                for (i in 0..m) s.println(prefix + "\tat " + trace[i])
                if (framesInCommon != 0) s.println("$prefix\t... $framesInCommon more")

                // Print suppressed exceptions, if any
                for (se in ex.suppressed) printEnclosedStackTrace(
                    ex, s, trace, SUPPRESSED_CAPTION, prefix + "\t", dejaVu
                )

                // Print cause, if any
                val ourCause = ex.cause
                if (ourCause != null) printEnclosedStackTrace(ourCause, s, trace, CAUSE_CAPTION, prefix, dejaVu)
            }
        }
    }
}