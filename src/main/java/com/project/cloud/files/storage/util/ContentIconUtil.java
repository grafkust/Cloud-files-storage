package com.project.cloud.files.storage.util;

public class ContentIconUtil {
    public static String getFileIcon(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "/icon/file.png";
        }

        String lowerCaseFileName = fileName.toLowerCase();

        if (lowerCaseFileName.endsWith(".pdf")) {
            return "/icon/pdf.png";
        } else if (lowerCaseFileName.endsWith(".jpg") || lowerCaseFileName.endsWith(".jpeg")) {
            return "/icon/jpg.png";
        } else if (lowerCaseFileName.endsWith(".png")) {
            return "/icon/png.png";
        } else if (lowerCaseFileName.endsWith(".doc") || lowerCaseFileName.endsWith(".docx")) {
            return "/icon/word.png";
        } else if (lowerCaseFileName.endsWith(".mp3")) {
            return "/icon/mp3.png";
        } else if (lowerCaseFileName.endsWith(".mp4")) {
            return "/icon/mp4.png";
        } else if (lowerCaseFileName.endsWith(".xlsx") || lowerCaseFileName.endsWith(".xls")) {
            return "/icon/xl.png";
        } else if (lowerCaseFileName.endsWith(".java")) {
            return "/icon/java.png";
        } else {
            int lastDotIndex = lowerCaseFileName.lastIndexOf('.');
            if (lastDotIndex == -1 || lastDotIndex == lowerCaseFileName.length() - 1) {
                return "/icon/folder.png";
            }
            String extension = lowerCaseFileName.substring(lastDotIndex + 1);
            return extension.length() <= 4 ? "/icon/file.png" : "/icon/folder.png";
        }
    }
}
