package com.dp.dplanner.service.upload;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class UploadServiceImpl implements UploadService {
    String DIR = "src/main/resources/test/save/";

    @Override
    public void init() {
        try {
            Path path = Paths.get(DIR);
            if (Files.exists(path) && Files.isDirectory(path)) {
                return;
            }
            Files.createDirectory(path);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize folder for upload!");
        }
    }

    @Override
    public String uploadFile(MultipartFile multipartFile) {
        String pathName = DIR + multipartFile.getOriginalFilename();
        try {
            multipartFile.transferTo(new File(pathName));
        } catch (IOException e) {
            throw new RuntimeException("Could not upload file");
        }
        return pathName;
    }

    @Override
    public String getDir() {
        return DIR;
    }
}
