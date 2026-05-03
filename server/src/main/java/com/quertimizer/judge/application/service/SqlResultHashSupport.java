package com.quertimizer.judge.application.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public final class SqlResultHashSupport {

    private static final char COLUMN_DELIMITER = 0x1F;
    private static final char ROW_DELIMITER = 0x1E;
    private static final char HEADER_DELIMITER = 0x1D;

    private SqlResultHashSupport() {
    }

    public static String hashResult(List<String> columns, List<List<String>> rows) {
        List<String> normalizedColumns = new ArrayList<>(columns.size());
        for (String column : columns) {
            normalizedColumns.add(column != null ? column : "null");
        }

        List<String> normalizedRows = new ArrayList<>(rows.size());
        for (List<String> row : rows) {
            List<String> normalizedValues = new ArrayList<>(row.size());
            for (String value : row) {
                normalizedValues.add(value != null ? value : "null");
            }
            normalizedRows.add(String.join(String.valueOf(COLUMN_DELIMITER), normalizedValues));
        }

        return sha512(
                String.join(String.valueOf(COLUMN_DELIMITER), normalizedColumns)
                        + HEADER_DELIMITER
                        + String.join(String.valueOf(ROW_DELIMITER), normalizedRows)
        );
    }

    private static String sha512(String value) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-512");
            byte[] hashBytes = messageDigest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexBuilder = new StringBuilder(hashBytes.length * 2);

            for (byte hashByte : hashBytes) {
                hexBuilder.append(String.format("%02x", hashByte));
            }

            return hexBuilder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SQL 결과 해시 생성 실패", exception);
        }
    }
}
