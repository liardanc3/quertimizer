package com.quertimizer.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ProblemAnswerHashSupport {

    private static final char COLUMN_DELIMITER = 0x1F;
    private static final char ROW_DELIMITER = 0x1E;

    private ProblemAnswerHashSupport() {
    }

    public static String hashRows(List<List<String>> rows) {
        List<String> normalizedRows = new ArrayList<>(rows.size());

        for (List<String> row : rows) {
            List<String> normalizedValues = new ArrayList<>(row.size());

            for (String value : row) {
                normalizedValues.add(value != null ? value : "null");
            }

            normalizedValues.sort(Comparator.naturalOrder());
            normalizedRows.add(String.join(String.valueOf(COLUMN_DELIMITER), normalizedValues));
        }

        normalizedRows.sort(Comparator.naturalOrder());
        return sha512(String.join(String.valueOf(ROW_DELIMITER), normalizedRows));
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
            throw new IllegalStateException("SHA-512 해시를 생성할 수 없다.", exception);
        }
    }
}
