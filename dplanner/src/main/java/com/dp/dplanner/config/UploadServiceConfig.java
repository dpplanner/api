package com.dp.dplanner.config;

import com.amazonaws.services.s3.AmazonS3Client;
import com.dp.dplanner.service.upload.UploadService;
import com.dp.dplanner.service.upload.UploadServiceS3Impl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class UploadServiceConfig {
    @Autowired
    private AmazonS3Client amazonS3Client;

    @Bean
    public UploadService uploadService() {
        return new UploadServiceS3Impl(amazonS3Client);
    }

}
