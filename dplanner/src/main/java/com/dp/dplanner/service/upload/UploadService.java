package com.dp.dplanner.service.upload;

import com.dp.dplanner.domain.FileType;
import org.springframework.web.multipart.MultipartFile;


import static com.dp.dplanner.domain.FileType.*;

public interface UploadService {

    void init();
    String uploadFile(MultipartFile multipartFile);

    /** uploadFile 이 돌려준 값(url)으로 파일을 지운다. */
    void deleteByUrl(String url);

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
