package com.dp.dplanner.service.upload;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RequiredArgsConstructor
public class UploadServiceS3Impl implements UploadService {

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    private final AmazonS3Client amazonS3Client;

    @Override
    public void init() {

    }

    @Override
    public String uploadFile(MultipartFile multipartFile) {
        try {

            String fileName = UUID.randomUUID().toString();
            ObjectMetadata metadata= new ObjectMetadata();
            metadata.setContentType(multipartFile.getContentType());
            metadata.setContentLength(multipartFile.getSize());
            amazonS3Client.putObject(bucket,fileName,multipartFile.getInputStream(),metadata);
            return amazonS3Client.getUrl(bucket, fileName).toString().split("//")[1];

        } catch (IOException e) {
            throw new RuntimeException("S3 Upload fail",e);
        }

    }

    public void delete(String path) {
        amazonS3Client.deleteObject(bucket, path);
    }

    @Override
    public String getDir() {
        return null;
    }
}
