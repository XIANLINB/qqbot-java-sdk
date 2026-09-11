package com.xuanji.qqbot.webhook;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Ed25519Test {

    @Test
    void rfc8032Test1PublicKey() {
        byte[] seed = hex("9d61b19deffd5a60ba844af492ec2cc44449c5697b326919703bac031cae7f60");
        byte[] pub = Ed25519.publicKeyFromSeed(seed).getEncoded();
        assertArrayEquals(
                hex("d75a980182b10ab7d54bfed3c964073a0ee172f3daa62325af021a68f707511a"),
                pub
        );
    }

    @Test
    void seedIsThirtyTwoBytes() {
        byte[] seed = Ed25519.seedFromSecret("naOC0ocQE3shWLAfffVLB1rhYPG7");
        assertEquals(32, seed.length);
        assertArrayEquals(
                "naOC0ocQE3shWLAfffVLB1rhYPG7naOC".getBytes(StandardCharsets.UTF_8),
                seed
        );
    }

    @Test
    void signThenVerifyRoundtrip() {
        String secret = "naOC0ocQE3shWLAfffVLB1rhYPG7";
        byte[] msg = "1725442341hello".getBytes(StandardCharsets.UTF_8);
        byte[] sig = Ed25519.sign(secret, msg);
        assertEquals(64, sig.length);
        assertTrue(Ed25519.verifyWithSecret(secret, msg, sig));
        assertFalse(Ed25519.verifyWithSecret(secret, "other".getBytes(StandardCharsets.UTF_8), sig));
    }

    @Test
    void webhookValidationResponseShape() {
        WebhookCodec codec = WebhookCodec.of("naOC0ocQE3shWLAfffVLB1rhYPG7");
        byte[] body = "{\"op\":13,\"d\":{\"plain_token\":\"Arq0D5A61EgUu4OxUvOp\",\"event_ts\":\"1725442341\"}}"
                .getBytes(StandardCharsets.UTF_8);
        assertTrue(codec.isValidation(body));
        Map<String, String> resp = codec.validationResponse(body);
        assertEquals("Arq0D5A61EgUu4OxUvOp", resp.get("plain_token"));
        assertEquals(128, resp.get("signature").length());
    }

    @Test
    void verifySignatureWithHeaders() {
        String secret = "naOC0ocQE3shWLAfffVLB1rhYPG7";
        String ts = "1725442341";
        byte[] body = "{\"op\":0,\"t\":\"C2C_MESSAGE_CREATE\",\"d\":{\"id\":\"1\"}}"
                .getBytes(StandardCharsets.UTF_8);
        byte[] tsBytes = ts.getBytes(StandardCharsets.UTF_8);
        byte[] msg = new byte[tsBytes.length + body.length];
        System.arraycopy(tsBytes, 0, msg, 0, tsBytes.length);
        System.arraycopy(body, 0, msg, tsBytes.length, body.length);
        byte[] sig = Ed25519.sign(secret, msg);
        String hexSig = java.util.HexFormat.of().formatHex(sig);
        WebhookCodec codec = WebhookCodec.of(secret);
        assertTrue(codec.verify(hexSig, ts, body));
        assertFalse(codec.verify(hexSig, "0", body));
    }

    private static byte[] hex(String s) {
        return java.util.HexFormat.of().parseHex(s);
    }
}
