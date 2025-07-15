package org.kingdoms.enginehub.worldedit

import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter
import java.io.File
import java.io.InputStream
import java.io.OutputStream

class ClipboardFormatv7(override val worldeditObject: ClipboardFormat) : XClipboardFormat {
    override val name: String get() = worldeditObject.name
    override val aliases: Set<String> get() = worldeditObject.aliases
    override val primaryFileExtension: String get() = worldeditObject.primaryFileExtension
    override val fileExtensions: Set<String> get() = worldeditObject.fileExtensions

    override fun getReader(inputStream: InputStream): ClipboardReader {
        return worldeditObject.getReader(inputStream)
    }

    override fun read(inputStream: InputStream): Clipboard {
        return getReader(inputStream).use { reader -> reader.read() }
    }

    override fun getWriter(outputStream: OutputStream): ClipboardWriter {
        return worldeditObject.getWriter(outputStream)
    }

    override fun write(outputStream: OutputStream, clipboard: Clipboard) {
        getWriter(outputStream).use { writer -> writer.write(clipboard) }
    }

    override fun isFormat(file: File): Boolean {
        return worldeditObject.isFormat(file)
    }
}