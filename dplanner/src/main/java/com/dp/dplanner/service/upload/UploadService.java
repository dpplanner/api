package com.dp.dplanner.service.upload;

import com.dp.dplanner.domain.FileType;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.dp.dplanner.domain.FileType.*;

public interface UploadService {

    void init();
    String uploadFile(MultipartFile multipartFile) throws IOException;

    String getDir();

    default FileType getFileType(String url) throws IOException {
        FileType fileType;
        String contentType = Files.probeContentType(Path.of(url));

        if (contentType == null) {
            return NONE;
        }
        String type = contentType.split("/")[0];

        if(type.equals("image"))
            fileType = IMAGE;
        else if(type.equals("video"))
            fileType = VIDEO;
        else
            fileType = NONE;

        return fileType;
    }

}
