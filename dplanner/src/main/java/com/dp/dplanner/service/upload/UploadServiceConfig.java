package com.dp.dplanner.service.upload;

import com.amazonaws.services.s3.AmazonS3Client;
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
