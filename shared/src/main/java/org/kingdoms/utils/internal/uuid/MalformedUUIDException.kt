package org.kingdoms.utils.internal.uuid

class MalformedUUIDException(val uuid: CharSequence, throwable: Throwable) :
    IllegalArgumentException("'$uuid'", throwable)