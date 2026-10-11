package com.dp.dplanner.service.upload;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * 더 이상 쓰지 않는 업로드 파일(탈퇴·클럽 탈퇴 회원의 프로필 사진, 바뀐 프로필 사진)을 지운다.
 * DB 변경이 커밋된 뒤에만 지우고(롤백되면 파일 유지), 지우기 실패는 요청을 실패시키지 않고 로그만 남긴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileCleanup {

    private final UploadService uploadService;

    public void deleteAfterCommit(Collection<String> urls) {
        List<String> targets = urls.stream()
                .filter(Objects::nonNull)
                .filter(url -> !url.isBlank())
                .distinct()
                .toList();
        if (targets.isEmpty()) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteAll(targets);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteAll(targets);
            }
        });
    }

    private void deleteAll(List<String> urls) {
        for (String url : urls) {
            try {
                uploadService.deleteByUrl(url);
            } catch (RuntimeException e) {
                log.warn("file delete failed: {} ({})", url, e.getMessage());
            }
        }
    }
}
