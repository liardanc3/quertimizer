package com.quertimizer.problem.domain.policy;

import com.quertimizer.problem.domain.model.ProblemAnswerFailReason;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.quertimizer.problem.domain.model.ProblemAnswerHashConstant.COLUMN_DELIMITER;
import static com.quertimizer.problem.domain.model.ProblemAnswerHashConstant.HEADER_DELIMITER;
import static com.quertimizer.problem.domain.model.ProblemAnswerHashConstant.ROW_DELIMITER;

final class ProblemAnswerHashSupport {
    private ProblemAnswerHashSupport() {
    }

    static String hashRows(List<List<String>> rows) {
        // 기존 정답 데이터 호환용 행 내부 값과 행 목록 정렬 해시 생성
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

    static String hashResult(List<String> columns, List<List<String>> rows) {
        // 컬럼 순서와 행 순서를 보존한 현재 정답 비교용 canonical 해시 생성
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
        // 정답 비교에 사용할 SHA-512 문자열 계산
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
