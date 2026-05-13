package com.quertimizer.judge.domain.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import static com.quertimizer.judge.domain.model.JudgeFailReason.SQL_RESULT_HASH_CREATION_FAILED;
import static com.quertimizer.judge.domain.model.SqlResultHashConstant.COLUMN_DELIMITER;
import static com.quertimizer.judge.domain.model.SqlResultHashConstant.HEADER_DELIMITER;
import static com.quertimizer.judge.domain.model.SqlResultHashConstant.ROW_DELIMITER;

public final class SqlResultHashSupport {

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
            throw new IllegalStateException(SQL_RESULT_HASH_CREATION_FAILED.getMessage(), exception);
        }
    }
}
