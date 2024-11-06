package com.project.cloud.files.storage.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class StorageItemDto {

    private String name;
    private String lastModified;
    private String iconPath;
    private boolean isFile;
    private String path;
    private boolean isSearchResult;


    public StorageItemDto(String name, String iconPath, String path) {
        this.name = name;
        this.iconPath = iconPath;
        this.path = path;
    }


}
