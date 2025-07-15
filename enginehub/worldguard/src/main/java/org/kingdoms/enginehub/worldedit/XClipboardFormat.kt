package org.kingdoms.enginehub.worldedit

import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * To prevent invokevirtual and invokeinterface bytecode incompatibilities
 * between WorldEdit v6 (enum) and v7 (interface)
 *
 * Methods [read] and [write] are there to support non-closeable (v6)
 * and [AutoCloseable] (v7) writers/readers.
 */
interface XClipboardFormat {
    val worldeditObject: ClipboardFormat

    val name: String

    val aliases: Set<String>

    @Throws(IOException::class)
    fun getReader(inputStream: InputStream): ClipboardReader

    fun read(inputStream: InputStream): Clipboard

    @Throws(IOException::class)
    fun getWriter(outputStream: OutputStream): ClipboardWriter

    fun write(outputStream: OutputStream, clipboard: Clipboard)

    fun isFormat(file: File): Boolean

    val primaryFileExtension: String

    val fileExtensions: Set<String>
}