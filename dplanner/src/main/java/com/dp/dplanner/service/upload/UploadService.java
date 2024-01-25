package com.dp.dplanner.service.upload;

import com.dp.dplanner.domain.FileType;
import org.springframework.web.multipart.MultipartFile;


import static com.dp.dplanner.domain.FileType.*;

public interface UploadService {

    void init();
    String uploadFile(MultipartFile multipartFile);

    String getDir();

    default FileType getFileType(String contentType){
        FileType fileType;

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
