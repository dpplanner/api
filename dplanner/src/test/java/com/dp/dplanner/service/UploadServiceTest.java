package com.dp.dplanner.service;

import com.dp.dplanner.service.upload.UploadService;
import com.dp.dplanner.service.upload.UploadServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.io.FileInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;


@ExtendWith(MockitoExtension.class)
public class UploadServiceTest {

    @InjectMocks
    UploadService uploadService = new UploadServiceImpl();

    @Test
    public void UploadService_init_ReturnVoid() {
        assertAll(() -> uploadService.init());
    }
    @Test
    public void UploadService_uploadFile_ReturnUrl() throws Exception {

        String fileName = "testUpload";
        String contentType = "jpg";
        String filePath = "src/test/resources/test/testUpload.jpg";
        String savedPath = uploadService.getDir() + fileName + "." + contentType;
        FileInputStream fileInputStream = new FileInputStream(filePath);
        MockMultipartFile multipartFile = new MockMultipartFile(fileName, fileName + "." + contentType, contentType, fileInputStream);

        String url = uploadService.uploadFile(multipartFile);

        File savedFile = new File(url);
        assertThat(url).isEqualTo(savedPath);
        assertThat(savedFile.exists()).isTrue();
    }




}
