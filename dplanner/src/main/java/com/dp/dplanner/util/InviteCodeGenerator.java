package com.dp.dplanner.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class InviteCodeGenerator {
    public static String generateInviteCode(String seed) {
        return encrypt(seed);
    }

    public static boolean verify(String seed, String inviteCode) {
        return inviteCode.equals(encrypt(seed));
    }

    private static String encrypt(String text) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException();
        }
        md.update(text.getBytes());

        return bytesToHex(md.digest());
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }
}
