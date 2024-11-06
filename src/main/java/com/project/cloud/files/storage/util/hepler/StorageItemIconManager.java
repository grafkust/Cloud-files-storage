package com.project.cloud.files.storage.util.hepler;

import org.springframework.stereotype.Component;

@Component
public class StorageItemIconManager {

    public String resolveItemTypeIcon(String fileName, boolean isFile) {

        if (fileName == null || fileName.isEmpty())
            return "/icon/file.png";

        return isFile ? resolveFileTypeIcon(fileName) : getDirectoryIcon();
    }

    private String resolveFileTypeIcon(String fileName) {

        if (fileName.endsWith(".pdf")) {
            return "/icon/pdf.png";
        } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "/icon/jpg.png";
        } else if (fileName.endsWith(".png")) {
            return "/icon/png.png";
        } else if (fileName.endsWith(".doc") || fileName.endsWith(".docx")) {
            return "/icon/word.png";
        } else if (fileName.endsWith(".mp3")) {
            return "/icon/mp3.png";
        } else if (fileName.endsWith(".mp4")) {
            return "/icon/mp4.png";
        } else if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
            return "/icon/xl.png";
        } else if (fileName.endsWith(".java")) {
            return "/icon/java.png";

        } else return "/icon/file.png";
    }

    private String getDirectoryIcon() {
        return "/icon/folder.png";
    }

}
