package org.kingdoms.enginehub.schematic

import java.nio.file.Path

class UnknownClipboardFormatException(val path: Path, message: String) : RuntimeException(message)