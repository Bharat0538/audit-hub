package in.audithub.sdk.util;

import java.security.MessageDigest;

public class WebhookVerifier {

    public static boolean verify(String body, String signatureHeader, String secret) {
        if (body == null || signatureHeader == null || secret == null) {
            return false;
        }

        if (!signatureHeader.startsWith("sha256=")) {
            return false;
        }

        String receivedSignature = signatureHeader.substring("sha256=".length());
        String expectedSignature = computeHmacSha256(body, secret);

        return MessageDigest.isEqual(
                receivedSignature.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                expectedSignature.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );
    }

    private static String computeHmacSha256(String data, String key) {
        try {
            javax.crypto.Mac sha256HMAC = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(key.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
            sha256HMAC.init(secretKey);
            byte[] hash = sha256HMAC.doFinal(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute HMAC-SHA256", e);
        }
    }
}
