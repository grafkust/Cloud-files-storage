package com.project.cloud.files.storage.service.file;

import com.project.cloud.files.storage.model.dto.StorageItemDto;
import com.project.cloud.files.storage.service.file.impl.FileOperationServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FileOperationServiceImplUnitTest {

    @Mock
    private FileSearchService searchService;

    @Mock
    private FileManagementService managementService;

    @InjectMocks
    private FileOperationServiceImpl fileOperationService;

    @Test
    @DisplayName("Should return content of folder when query is null or empty")
    void getPageContentFolderTest() {

        String rootPath = "user-1-files/";
        String path = "user-1-files/documents/";

        StorageItemDto item1 = new StorageItemDto("doc1.txt", "txt-icon", "user-1-files/documents/doc1.txt");
        StorageItemDto item2 = new StorageItemDto("folder", "dir-icon", "user-1-files/documents/folder/");
        List<StorageItemDto> expected = List.of(item1, item2);

        when(searchService.getContentOfFolder(path)).thenReturn(expected);

        List<StorageItemDto> result = fileOperationService.getPageContent(rootPath, path, null);

        assertEquals(expected, result);
        verify(searchService).getContentOfFolder(path);
        verifyNoMoreInteractions(searchService);

        String emptyQuery = " ";

        result = fileOperationService.getPageContent(rootPath, path, emptyQuery);

        assertEquals(expected, result);
        verify(searchService, times(2)).getContentOfFolder(path);
        verifyNoMoreInteractions(searchService);
    }

    @Test
    @DisplayName("Should return search results when query is provided")
    void getPageContentSearchTest() {

        String rootPath = "user-1-files/";
        String path = "user-1-files/documents/";
        String query = "doc";

        StorageItemDto item1 = new StorageItemDto("doc1.txt", "txt-icon", "user-1-files/documents/doc1.txt");
        StorageItemDto item2 = new StorageItemDto("doc2.txt", "txt-icon", "user-1-files/documents/doc2.txt");
        List<StorageItemDto> expected = List.of(item1, item2);

        when(searchService.searchFileOrDirectory(rootPath, query)).thenReturn(expected);

        List<StorageItemDto> result = fileOperationService.getPageContent(rootPath, path, query);

        assertEquals(expected, result);
        verify(searchService).searchFileOrDirectory(rootPath, query);
        verifyNoMoreInteractions(searchService);
    }

    @Test
    @DisplayName("Should delete file")
    void deleteFileTest() {

        String path = "user-1-files/documents";
        String name = "doc1.txt";

        fileOperationService.deleteFileOrDirectory(path, name, true);

        verify(managementService).deleteFile(path, name);
        verifyNoMoreInteractions(managementService);
    }

    @Test
    @DisplayName("Should delete directory")
    void deleteDirectoryTest() {

        String path = "user-1-files/documents";
        String name = "folder";

        fileOperationService.deleteFileOrDirectory(path, name, false);

        verify(managementService).deleteDirectory(path, name);
        verifyNoMoreInteractions(managementService);
    }

}
