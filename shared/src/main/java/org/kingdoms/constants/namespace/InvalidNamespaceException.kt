package org.kingdoms.constants.namespace

class InvalidNamespaceException(val string: String?, message: String)
    : IllegalArgumentException(message)