package com.quertimizer.judge.domain.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class JudgeTemplateVersionSupport {

    private JudgeTemplateVersionSupport() {
    }

    public static String createVersion(String ddl, String actualDataSql) {
        // DDL + actualDataSql 조합으로 template version 식별자를 생성
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((normalize(ddl) + "\u001e" + normalize(actualDataSql)).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("template version 해시를 생성할 수 없다.", exception);
        }
    }

    private static String normalize(String value) {
        return value != null ? value.replace("\r\n", "\n").trim() : "";
    }
}
