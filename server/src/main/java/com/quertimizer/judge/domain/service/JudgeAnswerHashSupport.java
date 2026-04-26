package com.quertimizer.judge.domain.service;

import com.quertimizer.problem.domain.model.ProblemAnswerFailReason;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class JudgeAnswerHashSupport {

    private static final char COLUMN_DELIMITER = 0x1F;
    private static final char ROW_DELIMITER = 0x1E;
    private static final char HEADER_DELIMITER = 0x1D;

    private JudgeAnswerHashSupport() {
    }

    public static String hashRows(List<List<String>> rows) {
        // 정답 비교용 행 집합 해시를 생성
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

    public static String hashResult(List<String> columns, List<List<String>> rows) {
        // 컬럼 순서와 행 순서를 유지한 canonical 결과 해시를 생성
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

    public static boolean matches(String answerHash, List<String> columns, List<List<String>> rows) {
        // 기존 legacy 해시와 새 canonical 해시를 모두 허용
        if (answerHash == null || answerHash.isBlank()) {
            return false;
        }

        return answerHash.equalsIgnoreCase(hashResult(columns, rows))
                || answerHash.equalsIgnoreCase(hashRows(rows));
    }

    private static String sha512(String value) {
        // SHA-512 해시 계산
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-512");
            byte[] hashBytes = messageDigest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexBuilder = new StringBuilder(hashBytes.length * 2);

            for (byte hashByte : hashBytes) {
                hexBuilder.append(String.format("%02x", hashByte));
            }

            return hexBuilder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(ProblemAnswerFailReason.HASH_CREATION_FAILED.getMessage(), exception);
        }
    }
}
