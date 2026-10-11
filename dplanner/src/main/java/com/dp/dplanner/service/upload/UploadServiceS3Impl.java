package com.dp.dplanner.service.upload;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Slf4j
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

    /**
     * url 형식: "{bucket}.s3.{region}.amazonaws.com/{key}" (uploadFile 반환값, 앞에 https:// 가 붙어 있어도 됨).
     * 다른 버킷 주소면 지우지 않는다.
     */
    @Override
    public void deleteByUrl(String url) {
        String address = url.contains("//") ? url.substring(url.indexOf("//") + 2) : url;
        int slash = address.indexOf('/');
        if (!address.startsWith(bucket + ".") || slash < 0 || slash == address.length() - 1) {
            log.warn("skip deleting file outside bucket {}: {}", bucket, url);
            return;
        }
        delete(address.substring(slash + 1));
    }

    @Override
    public String getDir() {
        return null;
    }
}
