package com.dp.dplanner.service;

import com.amazonaws.services.s3.AmazonS3Client;
import com.dp.dplanner.service.upload.UploadServiceS3Impl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
public class UploadServiceS3ImplTest {

    @Mock
    AmazonS3Client amazonS3Client;
    UploadServiceS3Impl uploadService;

    @BeforeEach
    void setUp() {
        uploadService = new UploadServiceS3Impl(amazonS3Client);
        ReflectionTestUtils.setField(uploadService, "bucket", "baggytsk-dplanner-files");
    }

    @Test
    @DisplayName("저장된 url 에서 key 를 꺼내 지운다 (https:// 유무 상관없음)")
    void deleteByUrl() {
        uploadService.deleteByUrl("baggytsk-dplanner-files.s3.ap-northeast-2.amazonaws.com/0f8e-uuid");
        uploadService.deleteByUrl("https://baggytsk-dplanner-files.s3.ap-northeast-2.amazonaws.com/1a2b-uuid");

        then(amazonS3Client).should().deleteObject("baggytsk-dplanner-files", "0f8e-uuid");
        then(amazonS3Client).should().deleteObject("baggytsk-dplanner-files", "1a2b-uuid");
    }

    @Test
    @DisplayName("다른 버킷 주소나 key 없는 주소는 지우지 않는다")
    void skipOtherBucket() {
        uploadService.deleteByUrl("dplanner-resource-server.s3.ap-northeast-2.amazonaws.com/old-uuid");
        uploadService.deleteByUrl("baggytsk-dplanner-files.s3.ap-northeast-2.amazonaws.com/");
        uploadService.deleteByUrl("baggytsk-dplanner-files.evil.com");

        then(amazonS3Client).should(never()).deleteObject(anyString(), anyString());
    }
}
