package com.dp.dplanner;

import com.dp.dplanner.service.upload.UploadService;
import com.dp.dplanner.service.upload.UploadServiceImpl;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

@TestConfiguration
@Profile("test")
public class TestConfig {

    @Bean
    public UploadService uploadService() {
        return new UploadServiceImpl();
    }
}
