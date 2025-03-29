package org.kingdoms.utils.internal;

import org.kingdoms.ide.Bookmark;
import org.kingdoms.ide.BookmarkType;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * A simpler, thread-unsafe, public ensureCapacity, non-copying, internal copy of Java's {@link java.io.ByteArrayOutputStream}.
 */
public class FastByteArrayOutputStream extends OutputStream {
    /**
     * The number of valid bytes in the buffer.
     */
    private int size;

    /**
     * The buffer where data is stored.
     */
    private byte[] buf;

    /**
     * Creates a new {@code ByteArrayOutputStream}, with a buffer capacity of
     * the specified size, in bytes.
     */
    public FastByteArrayOutputStream(int size) {
        buf = new byte[size];
    }

    private static final int SOFT_MAX_ARRAY_LENGTH = Integer.MAX_VALUE - 8;

    private static int newLength(int oldLength, int minGrowth, int prefGrowth) {
        // preconditions not checked because of inlining
        // assert oldLength >= 0
        // assert minGrowth > 0

        int prefLength = oldLength + Math.max(minGrowth, prefGrowth); // might overflow
        if (0 < prefLength && prefLength <= SOFT_MAX_ARRAY_LENGTH) {
            return prefLength;
        } else {
            // put code cold in a separate method
            // private static int hugeLength(int oldLength, int minGrowth);
            int minLength = oldLength + minGrowth;
            if (minLength < 0) { // overflow
                throw new OutOfMemoryError(
                        "Required array length " + oldLength + " + " + minGrowth + " is too large");
            } else {
                return Math.max(minLength, SOFT_MAX_ARRAY_LENGTH);
            }
        }
    }

    /**
     * Increases the capacity if necessary to ensure that it can hold
     * at least the number of elements specified by the minimum
     * capacity argument.
     *
     * @param minCapacity the desired minimum capacity.
     * @throws OutOfMemoryError if {@code minCapacity < 0} and
     *                          {@code minCapacity - buf.length > 0}.  This is interpreted as a
     *                          request for the unsatisfiably large capacity.
     *                          {@code (long) Integer.MAX_VALUE + (minCapacity - Integer.MAX_VALUE)}.
     */
    public void ensureCapacity(int minCapacity) {
        // overflow-conscious code
        int oldCapacity = buf.length;
        int minGrowth = minCapacity - oldCapacity;
        if (minGrowth > 0) {
            buf = Arrays.copyOf(buf, newLength(oldCapacity,
                    minGrowth, oldCapacity /* preferred growth */));
        }
    }

    @Override
    public void write(int b) {
        ensureCapacity(size + 1);
        buf[size] = (byte) b;
        size += 1;
    }

    public void writeEnsuredCapacity(int b) {
        buf[size] = (byte) b;
        size += 1;
    }

    /**
     * @throws NullPointerException      if {@code b} is {@code null}.
     * @throws IndexOutOfBoundsException if {@code off} is negative,
     *                                   {@code len} is negative, or {@code len} is greater than
     *                                   {@code b.length - off}
     */
    @Override
    public void write(byte[] b, int off, int len) {
        ensureCapacity(size + len);
        System.arraycopy(b, off, buf, size, len);
        size += len;
    }

    public void writeEnsuredCapacity(byte[] b, int off, int len) {
        System.arraycopy(b, off, buf, size, len);
        size += len;
    }

    /**
     * Writes the complete contents of the specified byte array
     * to this {@code ByteArrayOutputStream}.
     * This method is equivalent to {@link #write(byte[], int, int)}
     *
     * @throws NullPointerException if {@code b} is {@code null}.
     * @since 11
     */
    public void writeBytes(byte[] b) {
        write(b, 0, b.length);
    }

    public void writeBytesEnsuredCapacity(byte[] b) {
        writeEnsuredCapacity(b, 0, b.length);
    }

    /**
     * Returns the current size of the buffer.
     *
     * @return the value of the {@code count} field, which is the number
     * of valid bytes in this output stream.
     * @see FastByteArrayOutputStream#size
     */
    public int size() {
        return size;
    }

    @Override
    public String toString() {
        return "ByteArrayOutputStream(" + buf.length + '/' + size + ')';
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException();
    }

    public byte[] getArray() {
        if (buf.length == size) return buf;
        return Arrays.copyOf(buf, size);
    }

    public boolean equals(FastByteArrayOutputStream obj) {
        // Don't use Arrays.equals() it has a stupid mismatch checker...

        if (this.size != obj.size) return false;
        if (this == obj) return true;
        if (size > 1_000) {
            // Jankenpon algorithm
            if (this.buf[size / 3] != obj.buf[size / 3] ||
                    this.buf[size / 2] != obj.buf[size / 2] ||
                    this.buf[size - 10] != obj.buf[size - 10]) return false;
        }

        for (int i = 0; i < size; i++) {
            if (this.buf[i] != obj.buf[i]) return false;
        }

        return true;
    }

    /**
     * Closing a {@code ByteArrayOutputStream} has no effect. The methods in
     * this class can be called after the stream has been closed without
     * generating an {@code IOException}.
     */
    @Override
    public void close() throws IOException {}
}
