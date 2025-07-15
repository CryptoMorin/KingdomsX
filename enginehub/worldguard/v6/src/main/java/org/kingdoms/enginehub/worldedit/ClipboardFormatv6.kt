package org.kingdoms.enginehub.worldedit

import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter
import com.sk89q.worldedit.world.registry.LegacyWorldData
import java.io.File
import java.io.InputStream
import java.io.OutputStream

class ClipboardFormatv6(override val worldeditObject: ClipboardFormat) : XClipboardFormat {
    override val name: String get() = worldeditObject.name
    override val aliases: Set<String> get() = worldeditObject.aliases
    override val primaryFileExtension: String get() = worldeditObject.aliases.first()
    override val fileExtensions: Set<String> get() = worldeditObject.aliases

    override fun getReader(inputStream: InputStream): ClipboardReader {
        return worldeditObject.getReader(inputStream)
    }

    override fun read(inputStream: InputStream): Clipboard {
        try {
            return getReader(inputStream).read(LegacyWorldData.getInstance())
        } catch (ex: Throwable) {
            throw IllegalStateException("Failed to read schematic using $worldeditObject format", ex)
        }
    }

    override fun write(outputStream: OutputStream, clipboard: Clipboard) {
        try {
            getWriter(outputStream).write(clipboard, LegacyWorldData.getInstance())
        } catch (ex: Throwable) {
            throw IllegalStateException("Failed to read schematic using $worldeditObject format", ex)
        }
    }

    override fun getWriter(outputStream: OutputStream): ClipboardWriter {
        return worldeditObject.getWriter(outputStream)
    }

    override fun isFormat(file: File): Boolean {
        return worldeditObject.isFormat(file)
    }
}