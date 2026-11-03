package org.kingdoms.editor;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.regex.Pattern;

final class KingdomsEditorSecurity {
    private KingdomsEditorSecurity() {}

    private static final byte[]
            MAGIC = "KINGDOMSX".getBytes(StandardCharsets.UTF_8),
            LINK_DERIVATION_SALT = "KingdomsX-Editor-Link-v1".getBytes(StandardCharsets.UTF_8),
            SESSION_ID_INFO = "KingdomsX-Editor/v1/session-id".getBytes(StandardCharsets.UTF_8),
            AES_KEY_INFO = "KingdomsX-Editor/v1/aes-256-gcm-key".getBytes(StandardCharsets.UTF_8),
            BROWSER_CAPABILITY_INFO = "KingdomsX-Editor/v1/browser-capability".getBytes(StandardCharsets.UTF_8);

    private static final int VERSION = 1;
    private static final int VERSION_OFFSET = MAGIC.length;
    private static final int KIND_OFFSET = VERSION_OFFSET + 1;
    private static final int REVISION_OFFSET = KIND_OFFSET + 1;
    private static final int NONCE_OFFSET = REVISION_OFFSET + 4;
    private static final int HEADER_BYTES = NONCE_OFFSET + 12;

    private static final int TAG_BYTES = 16;
    private static final int LINK_SEED_BYTES = 32;

    private static final Pattern SEED_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{43}$");
    private static final Pattern SESSION_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{22}$");

    public enum RemotePayloadKind {
        ORIGINAL(1), RESULT(2);

        private final int code;

        RemotePayloadKind(int code) {this.code = code;}

        public int getCode() {
            return code;
        }
    }

    private static final SecureRandom RANDOM = new SecureRandom();

    public static final class RemoteEditorSecrets {
        public final String id;
        public final String key;
        public final String browserToken;

        public RemoteEditorSecrets(
                String id,
                String key,
                String browserToken) {
            this.id = id;
            this.key = key;
            this.browserToken = browserToken;
        }
    }

    // ---------------------------------------------------------
    // HKDF-SHA256 secret derivation
    // ---------------------------------------------------------

    public static RemoteEditorSecrets deriveRemoteEditorSecrets(
            String seed) throws GeneralSecurityException {

        if (seed == null || !SEED_PATTERN.matcher(seed).matches()) {
            throw new IllegalArgumentException("The editor link seed is invalid.");
        }

        byte[] seedBytes = decodeBase64Url(seed, LINK_SEED_BYTES);

        if (!encodeBase64Url(seedBytes).equals(seed)) {
            throw new IllegalArgumentException("The editor link seed is invalid.");
        }

        byte[] sessionId = hkdfSha256(
                seedBytes, LINK_DERIVATION_SALT,
                SESSION_ID_INFO, 16);

        byte[] aesKey = hkdfSha256(
                seedBytes, LINK_DERIVATION_SALT,
                AES_KEY_INFO, 32);

        byte[] browserCapability = hkdfSha256(
                seedBytes, LINK_DERIVATION_SALT,
                BROWSER_CAPABILITY_INFO, 32);

        return new RemoteEditorSecrets(
                encodeBase64Url(sessionId),
                encodeBase64Url(aesKey),
                encodeBase64Url(browserCapability));
    }

    /**
     * HKDF-Extract followed by HKDF-Expand using HMAC-SHA256.
     */
    private static byte[] hkdfSha256(
            byte[] inputKeyMaterial,
            byte[] salt,
            byte[] info,
            int outputLength) throws GeneralSecurityException {

        // HKDF-Extract
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(salt, "HmacSHA256"));
        byte[] pseudorandomKey = mac.doFinal(inputKeyMaterial);

        // HKDF-Expand
        byte[] output = new byte[outputLength];
        byte[] previousBlock = new byte[0];
        int offset = 0;
        int blockNumber = 1;

        while (offset < outputLength) {
            mac.init(new SecretKeySpec(pseudorandomKey, "HmacSHA256"));

            mac.update(previousBlock);
            mac.update(info);
            mac.update((byte) blockNumber);

            previousBlock = mac.doFinal();

            int copyLength = Math.min(
                    previousBlock.length,
                    outputLength - offset);

            System.arraycopy(
                    previousBlock, 0,
                    output, offset, copyLength);

            offset += copyLength;
            blockNumber++;
        }

        Arrays.fill(pseudorandomKey, (byte) 0);
        Arrays.fill(previousBlock, (byte) 0);

        return output;
    }

    public static byte[] encryptRemotePayload(
            byte[] plaintext,
            String sessionId,
            String key,
            int kind,
            long revision) throws GeneralSecurityException {

        if (plaintext == null) {
            throw new IllegalArgumentException("Expected a byte array.");
        }

        byte[] keyBytes = decodeBase64Url(key, 32);
        byte[] header = envelopeHeader(kind, revision);

        // Generate a fresh, cryptographically secure 12-byte nonce.
        byte[] nonce = new byte[12];
        RANDOM.nextBytes(nonce);

        System.arraycopy(nonce, 0, header, NONCE_OFFSET, nonce.length);

        byte[] aad = additionalData(header, sessionId);

        SecretKey cryptoKey = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

        cipher.init(
                Cipher.ENCRYPT_MODE,
                cryptoKey,
                new GCMParameterSpec(128, nonce));

        cipher.updateAAD(aad);

        // Java GCM returns ciphertext followed by the 16-byte tag,
        // matching Web Crypto AES-GCM output.
        byte[] ciphertext = cipher.doFinal(plaintext);
        byte[] envelope = new byte[HEADER_BYTES + ciphertext.length];

        System.arraycopy(header, 0, envelope, 0, HEADER_BYTES);
        System.arraycopy(ciphertext, 0, envelope, HEADER_BYTES, ciphertext.length);

        return envelope;
    }

    // ---------------------------------------------------------
    // Decrypt remote payload
    // ---------------------------------------------------------

    public static byte[] decryptRemotePayload(
            byte[] envelope,
            String sessionId,
            String key,
            int kind,
            long revision) throws GeneralSecurityException {

        if (envelope == null || envelope.length < HEADER_BYTES + TAG_BYTES) {
            throw new IllegalArgumentException("The encrypted editor payload is incomplete.");
        }

        // Verify magic bytes and format version.
        for (int i = 0; i < MAGIC.length; i++) {
            if (envelope[i] != MAGIC[i]) {
                throw new IllegalArgumentException("The editor payload uses an unsupported format.");
            }
        }

        if ((envelope[VERSION_OFFSET] & 0xFF) != VERSION) {
            throw new IllegalArgumentException("The editor payload uses an unsupported format.");
        }

        // Verify operation kind and revision.
        int actualKind = envelope[KIND_OFFSET] & 0xFF;
        long actualRevision = readRevision(envelope);

        if (actualKind != kind || actualRevision != revision) {
            throw new IllegalArgumentException("The editor payload does not match this session operation.");
        }

        byte[] keyBytes = decodeBase64Url(key, 32);
        byte[] header = Arrays.copyOfRange(envelope, 0, HEADER_BYTES);
        byte[] nonce = Arrays.copyOfRange(envelope, NONCE_OFFSET, HEADER_BYTES);
        byte[] aad = additionalData(header, sessionId);

        SecretKey cryptoKey = new SecretKeySpec(keyBytes, "AES");

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

        cipher.init(
                Cipher.DECRYPT_MODE,
                cryptoKey,
                new GCMParameterSpec(128, nonce));

        cipher.updateAAD(aad);

        try {
            return cipher.doFinal(
                    envelope,
                    HEADER_BYTES,
                    envelope.length - HEADER_BYTES);
        } catch (GeneralSecurityException e) {
            // Includes an invalid GCM authentication tag.
            throw new GeneralSecurityException("The encrypted editor payload failed its integrity check.");
        }
    }

    // ---------------------------------------------------------
    // SHA-256 hexadecimal digest
    // ---------------------------------------------------------

    static String sha256Hex(byte[] bytes) {
        try {
            byte[] digest = MessageDigest
                    .getInstance("SHA-256")
                    .digest(bytes);

            StringBuilder result = new StringBuilder(digest.length * 2);

            for (byte value : digest) {
                result.append(
                        Character.forDigit((value >>> 4) & 0xF, 16)
                );
                result.append(
                        Character.forDigit(value & 0xF, 16)
                );
            }

            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is not available in this Java runtime.",
                    exception
            );
        }
    }

    // ---------------------------------------------------------
    // Base64 URL-safe encoding and decoding
    // ---------------------------------------------------------

    public static byte[] decodeBase64Url(String value, int expectedBytes) {

        if (value == null || !value.matches("^[A-Za-z0-9_-]+$")) {
            throw new IllegalArgumentException("The editor session secret is invalid.");
        }

        final byte[] bytes;

        try {
            bytes = Base64.getUrlDecoder().decode(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("The editor session secret is invalid.");
        }

        if (expectedBytes > 0 && bytes.length != expectedBytes) {
            throw new IllegalArgumentException("The editor session secret has an invalid length.");
        }

        return bytes;
    }

    public static String encodeBase64Url(byte[] value) {
        if (value == null) {
            throw new IllegalArgumentException("Expected a byte array.");
        }

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value);
    }

    // ---------------------------------------------------------
    // Binary envelope helpers
    // ---------------------------------------------------------

    private static byte[] envelopeHeader(
            int kind,
            long revision) {

        if (kind != RemotePayloadKind.ORIGINAL.getCode() && kind != RemotePayloadKind.RESULT.getCode()) {
            throw new IllegalArgumentException("The editor payload kind is invalid.");
        }

        if (revision < 0 || revision > 0xFFFFFFFFL) {
            throw new IllegalArgumentException("The editor payload revision is invalid.");
        }

        byte[] header = new byte[HEADER_BYTES];
        System.arraycopy(MAGIC, 0, header, 0, MAGIC.length);

        header[VERSION_OFFSET] = (byte) VERSION;
        header[KIND_OFFSET] = (byte) kind;

        ByteBuffer buffer = ByteBuffer.wrap(header).order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(REVISION_OFFSET, (int) revision);

        return header;
    }

    private static long readRevision(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
        return Integer.toUnsignedLong(buffer.getInt(REVISION_OFFSET));
    }

    private static byte[] additionalData(byte[] header, String sessionId) {
        if (sessionId == null || !SESSION_ID_PATTERN.matcher(sessionId).matches()) {
            throw new IllegalArgumentException("The editor session identifier is invalid.");
        }

        byte[] id = sessionId.getBytes(StandardCharsets.UTF_8);
        byte[] data = new byte[NONCE_OFFSET + id.length];

        // Include the header through the revision, excluding nonce.
        System.arraycopy(header, 0, data, 0, NONCE_OFFSET);
        System.arraycopy(id, 0, data, NONCE_OFFSET, id.length);

        return data;
    }
}
