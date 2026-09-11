package com.xuanji.qqbot.webhook;

import com.xuanji.qqbot.exception.QqBotException;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;

import java.nio.charset.StandardCharsets;

/**
 * QQ 机器人 Webhook 使用的 Ed25519 签名。
 * <p>
 * 种子 = AppSecret 反复拼接至长度 >= 32，再取前 32 字节（官方算法）。
 * 签名 / 验签 / 公钥派生使用 BouncyCastle（符合 RFC 8032）。
 */
public final class Ed25519 {
    private Ed25519() {
    }

    public static byte[] seedFromSecret(String botSecret) {
        if (botSecret == null || botSecret.isEmpty()) {
            throw new IllegalArgumentException("botSecret empty");
        }
        StringBuilder sb = new StringBuilder(botSecret);
        while (sb.length() < 32) {
            sb.append(sb);
        }
        return sb.substring(0, 32).getBytes(StandardCharsets.UTF_8);
    }

    public static Ed25519PrivateKeyParameters privateKeyFromSeed(byte[] seed) {
        if (seed == null || seed.length != 32) {
            throw new IllegalArgumentException("seed must be 32 bytes");
        }
        return new Ed25519PrivateKeyParameters(seed, 0);
    }

    public static Ed25519PublicKeyParameters publicKeyFromSeed(byte[] seed) {
        return privateKeyFromSeed(seed).generatePublicKey();
    }

    public static Ed25519PublicKeyParameters publicKeyFromSecret(String botSecret) {
        return publicKeyFromSeed(seedFromSecret(botSecret));
    }

    public static byte[] sign(String botSecret, byte[] message) {
        try {
            Ed25519Signer signer = new Ed25519Signer();
            signer.init(true, privateKeyFromSeed(seedFromSecret(botSecret)));
            signer.update(message, 0, message.length);
            return signer.generateSignature();
        } catch (Exception e) {
            throw new QqBotException("Ed25519 sign failed", e);
        }
    }

    public static boolean verifyWithSecret(String botSecret, byte[] message, byte[] signature) {
        return verify(publicKeyFromSecret(botSecret), message, signature);
    }

    public static boolean verify(Ed25519PublicKeyParameters publicKey, byte[] message, byte[] signature) {
        try {
            if (signature == null || signature.length != 64) {
                return false;
            }
            Ed25519Signer verifier = new Ed25519Signer();
            verifier.init(false, publicKey);
            verifier.update(message, 0, message.length);
            return verifier.verifySignature(signature);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean verify(byte[] publicKeyRaw, byte[] message, byte[] signature) {
        if (publicKeyRaw == null || publicKeyRaw.length != 32) {
            return false;
        }
        return verify(new Ed25519PublicKeyParameters(publicKeyRaw, 0), message, signature);
    }
}
