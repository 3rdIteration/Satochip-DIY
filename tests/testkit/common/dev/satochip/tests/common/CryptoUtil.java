package dev.satochip.tests.common;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;

/**
 * Small JCE helpers mirroring the primitives used by the applets.
 */
public final class CryptoUtil {

    private CryptoUtil() {
    }

    public static byte[] hmacSha1(byte[] key, byte[] data) {
        return mac("HmacSHA1", key, data);
    }

    public static byte[] hmacSha1(byte[] key, byte[]... parts) {
        return mac("HmacSHA1", key, concat(parts));
    }

    public static byte[] hmacSha256(byte[] key, byte[] data) {
        return mac("HmacSHA256", key, data);
    }

    public static byte[] hmacSha512(byte[] key, byte[] data) {
        return mac("HmacSHA512", key, data);
    }

    private static byte[] mac(String algorithm, byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(key, algorithm));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] sha512(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-512").digest(data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] aesCbcEncrypt(byte[] key, byte[] iv, byte[] data) {
        return aesCbc(Cipher.ENCRYPT_MODE, key, iv, data);
    }

    public static byte[] aesCbcDecrypt(byte[] key, byte[] iv, byte[] data) {
        return aesCbc(Cipher.DECRYPT_MODE, key, iv, data);
    }

    private static byte[] aesCbc(int mode, byte[] key, byte[] iv, byte[] data) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(mode, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
            return cipher.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] pkcs7Pad(byte[] data, int blockSize) {
        int pad = blockSize - (data.length % blockSize);
        byte[] out = new byte[data.length + pad];
        System.arraycopy(data, 0, out, 0, data.length);
        for (int i = data.length; i < out.length; i++) {
            out[i] = (byte) pad;
        }
        return out;
    }

    public static byte[] pkcs7Unpad(byte[] data) {
        if (data.length == 0) {
            return data;
        }
        int pad = data[data.length - 1] & 0xFF;
        if (pad < 1 || pad > 16 || pad > data.length) {
            return data;
        }
        for (int i = data.length - pad; i < data.length; i++) {
            if ((data[i] & 0xFF) != pad) {
                return data;
            }
        }
        byte[] out = new byte[data.length - pad];
        System.arraycopy(data, 0, out, 0, out.length);
        return out;
    }

    public static byte[] concat(byte[]... parts) {
        int len = 0;
        for (byte[] p : parts) {
            len += p.length;
        }
        byte[] out = new byte[len];
        int off = 0;
        for (byte[] p : parts) {
            System.arraycopy(p, 0, out, off, p.length);
            off += p.length;
        }
        return out;
    }
}
