package dev.satochip.tests.common;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.KeyAgreement;
import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECFieldFp;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;

/**
 * secp256k1 helpers for the test-side secure channel client, built on BouncyCastle.
 * (jCardsim shades its own BouncyCastle, so this dependency does not clash.)
 */
public final class EcUtil {

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private static final String CURVE = "secp256k1";

    private EcUtil() {
    }

    public static ECParameterSpec curveSpec() {
        try {
            AlgorithmParameters ap = AlgorithmParameters.getInstance("EC", "BC");
            ap.init(new ECGenParameterSpec(CURVE));
            return ap.getParameterSpec(ECParameterSpec.class);
        } catch (Exception e) {
            throw new RuntimeException("Unable to resolve secp256k1 parameters", e);
        }
    }

    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", "BC");
            kpg.initialize(curveSpec(), new SecureRandom());
            return kpg.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] encodeUncompressed(ECPublicKey pub) {
        ECPoint w = pub.getW();
        return Hex.concat(new byte[]{0x04},
                Hex.padLeft(toFixed(w.getAffineX()), 32),
                Hex.padLeft(toFixed(w.getAffineY()), 32));
    }

    public static ECPublicKey decode(byte[] uncompressed) {
        BigInteger x = new BigInteger(1, Hex.slice(uncompressed, 1, 32));
        BigInteger y = new BigInteger(1, Hex.slice(uncompressed, 33, 32));
        return decode(x, y);
    }

    public static ECPublicKey decode(BigInteger x, BigInteger y) {
        try {
            KeyFactory kf = KeyFactory.getInstance("EC", "BC");
            return (ECPublicKey) kf.generatePublic(new ECPublicKeySpec(new ECPoint(x, y), curveSpec()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** ECDH shared X coordinate, left padded to 32 bytes (matches the applets' key derivation). */
    public static byte[] ecdhX(PrivateKey priv, PublicKey pub) {
        try {
            KeyAgreement ka = KeyAgreement.getInstance("ECDH", "BC");
            ka.init(priv);
            ka.doPhase(pub, true);
            return Hex.padLeft(ka.generateSecret(), 32);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean verifySha256(PublicKey pub, byte[] msg, byte[] derSig) {
        try {
            Signature s = Signature.getInstance("SHA256withECDSA", "BC");
            s.initVerify(pub);
            s.update(msg);
            return s.verify(derSig);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Recovers the card's ephemeral public key. The applets return only the X coordinate plus a
     * self-signature over [0x0020 || X]; secp256k1 has p = 3 mod 4 so the two Y candidates are
     * easily computed, and the signature identifies the right one.
     */
    public static ECPublicKey recoverFromXAndSignature(byte[] x32, byte[] signedMessage, byte[] derSig) {
        ECParameterSpec spec = curveSpec();
        BigInteger p = ((ECFieldFp) spec.getCurve().getField()).getP();
        BigInteger x = new BigInteger(1, x32);
        BigInteger rhs = x.modPow(BigInteger.valueOf(3), p).add(BigInteger.valueOf(7)).mod(p);
        BigInteger y = rhs.modPow(p.add(BigInteger.ONE).shiftRight(2), p);
        BigInteger[] candidates = {y, p.subtract(y)};
        for (BigInteger cand : candidates) {
            ECPublicKey pk = decode(x, cand);
            if (verifySha256(pk, signedMessage, derSig)) {
                return pk;
            }
        }
        throw new IllegalStateException("Could not recover ephemeral public key from self-signature");
    }

    private static byte[] toFixed(BigInteger value) {
        byte[] b = value.toByteArray();
        if (b.length > 1 && b[0] == 0) {
            return Hex.slice(b, 1, b.length - 1);
        }
        return b;
    }
}
