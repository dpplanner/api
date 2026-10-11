package com.dp.dplanner.service;

import com.dp.dplanner.service.upload.FileCleanup;
import com.dp.dplanner.service.upload.UploadService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Arrays;
import java.util.List;

import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
public class FileCleanupTest {

    @Mock
    UploadService uploadService;
    @InjectMocks
    FileCleanup fileCleanup;

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("트랜잭션 안에서는 커밋된 뒤에만 지운다")
    void deletesOnlyAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();

        fileCleanup.deleteAfterCommit(List.of("url-1"));
        then(uploadService).shouldHaveNoInteractions();

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        then(uploadService).should().deleteByUrl("url-1");
    }

    @Test
    @DisplayName("롤백되면 지우지 않는다")
    void doesNotDeleteOnRollback() {
        TransactionSynchronizationManager.initSynchronization();

        fileCleanup.deleteAfterCommit(List.of("url-1"));
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        then(uploadService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("null·빈 값·중복은 건너뛰고, 하나가 실패해도 나머지는 지운다")
    void skipsInvalidAndContinuesOnFailure() {
        willThrow(new RuntimeException("AccessDenied")).given(uploadService).deleteByUrl("url-1");

        fileCleanup.deleteAfterCommit(Arrays.asList(null, "", "url-1", "url-1", "url-2"));

        then(uploadService).should(times(1)).deleteByUrl("url-1");
        then(uploadService).should().deleteByUrl("url-2");
        then(uploadService).shouldHaveNoMoreInteractions();
    }
}
