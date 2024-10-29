package com.project.cloud.files.storage.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class ContentDto {

    private String name;
    private String lastModified;
    private String iconPath;
    private boolean isFile;
    private String path;

    public ContentDto(String name, String lastModified, String iconPath, boolean isFile) {
        this.name = name;
        this.lastModified = lastModified;
        this.iconPath = iconPath;
        this.isFile = isFile;
    }

    public ContentDto(String name, String iconPath, String path) {
        this.name = name;
        this.iconPath = iconPath;
        this.path = path;
    }
}
