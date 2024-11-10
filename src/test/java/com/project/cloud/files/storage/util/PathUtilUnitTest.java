package com.project.cloud.files.storage.util;

import com.project.cloud.files.storage.service.user.UserService;
import com.project.cloud.files.storage.util.validator.PathUtil;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PathUtilUnitTest {

    @Mock
    private UserService userService;

    @Mock
    private HttpSession httpSession;

    @InjectMocks
    private PathUtil pathUtil;

    @Test
    @DisplayName("Should create public path when path equals user root path")
    void createPublicPath_WhenPathEqualsUserRootPath_ShouldReturnEmptyString() {
        String path = "user-1-files";
        String userRootPath = "user-1-files";

        String result = pathUtil.createPublicPath(path, userRootPath, false);

        assertEquals("", result);
    }

    @Test
    @DisplayName("Should create public path with encoding")
    void createPublicPath_WithEncoding_ShouldReturnEncodedPath() {
        String path = "user-1-files/my documents/test file.txt";
        String userRootPath = "user-1-files";

        String result = pathUtil.createPublicPath(path, userRootPath, true);

        assertEquals("my+documents%2Ftest+file.txt", result);
    }

    @Test
    @DisplayName("Should create public path without encoding")
    void createPublicPath_WithoutEncoding_ShouldReturnPlainPath() {
        String path = "user-1-files/my documents/test.txt";
        String userRootPath = "user-1-files";

        String result = pathUtil.createPublicPath(path, userRootPath, false);

        assertEquals("my documents/test.txt", result);
    }

    @Test
    @DisplayName("Should return empty string when path is null")
    void createPublicPath_WithNullPath_ShouldReturnEmptyString() {
        String result = pathUtil.createPublicPath(null, "user-1-files", false);

        assertEquals("", result);
    }

    @Test
    @DisplayName("Should return empty string when path is empty")
    void createPublicPath_WithEmptyPath_ShouldReturnEmptyString() {
        String result = pathUtil.createPublicPath("", "user-1-files", false);

        assertEquals("", result);
    }

    @Test
    @DisplayName("Should create inner path when path is not empty")
    void createInnerPath_WithValidPath_ShouldCombinePaths() {
        String path = "documents";
        String userRootPath = "user-1-files";

        String result = pathUtil.createInnerPath(path, userRootPath);

        assertEquals("user-1-files/documents", result);
    }

    @Test
    @DisplayName("Should return user root path when path is null")
    void createInnerPath_WithNullPath_ShouldReturnUserRootPath() {
        String userRootPath = "user-1-files";

        String result = pathUtil.createInnerPath(null, userRootPath);

        assertEquals(userRootPath, result);
    }

    @Test
    @DisplayName("Should return user root path when path is empty")
    void createInnerPath_WithEmptyPath_ShouldReturnUserRootPath() {
        String userRootPath = "user-1-files";

        String result = pathUtil.createInnerPath("", userRootPath);

        assertEquals(userRootPath, result);
    }

    @Test
    @DisplayName("Should get user root path from session")
    void getUserRootPath_ShouldCreatePathWithUserId() {
        String username = "testUser";
        Long userId = 1L;

        when(httpSession.getAttribute("username")).thenReturn(username);
        when(userService.getIdByUsername(username)).thenReturn(userId);

        String result = pathUtil.getUserRootPath(httpSession);

        assertEquals("user-1-files", result);
    }

    @Test
    @DisplayName("Should normalize storage path by adding trailing slash")
    void normalizeStoragePath_ShouldAddTrailingSlash() {
        String path = "user-1-files/documents";

        String result = pathUtil.normalizeStoragePath(path);

        assertEquals("user-1-files/documents/", result);
    }

    @Test
    @DisplayName("Should not modify path that already has trailing slash")
    void normalizeStoragePath_WithExistingTrailingSlash_ShouldNotModify() {
        String path = "user-1-files/documents/";

        String result = pathUtil.normalizeStoragePath(path);

        assertEquals("user-1-files/documents/", result);
    }

    @Test
    @DisplayName("Should get content root path when object name contains file name")
    void getContentRootPath_WhenContainsFileName_ShouldReturnParentPath() {
        String objectName = "user-1-files/documents/test.txt";
        String fileName = "test.txt";

        String result = pathUtil.getContentRootPath(objectName, fileName);

        assertEquals("user-1-files/documents/", result);
    }

    @Test
    @DisplayName("Should return object name when it doesn't contain file name")
    void getContentRootPath_WhenDoesNotContainFileName_ShouldReturnObjectName() {
        String objectName = "user-1-files/documents/";
        String fileName = "test.txt";

        String result = pathUtil.getContentRootPath(objectName, fileName);

        assertEquals(objectName, result);
    }

    @Test
    @DisplayName("Should create full path by combining path and filename")
    void createFullPath_ShouldCombinePathAndFilename() {
        String path = "user-1-files/documents/";
        String fileName = "test.txt";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                fileName,
                "text/plain",
                "test content".getBytes()
        );

        String result = pathUtil.createFullPath(file, path);

        assertEquals("user-1-files/documents/test.txt", result);
    }
    @Test
    @DisplayName("Should detect special characters in names")
    void nameContainsSpecialCharactersTest() {
        PathUtil pathUtil = new PathUtil(userService);

        assertFalse(pathUtil.isContainsSpecialCharacters("normal-file"));
        assertFalse(pathUtil.isContainsSpecialCharacters("file_name"));
        assertFalse(pathUtil.isContainsSpecialCharacters("My File.txt"));
        assertFalse(pathUtil.isContainsSpecialCharacters("document123"));

        assertFalse(pathUtil.isContainsSpecialCharacters("имя-файла"));
        assertFalse(pathUtil.isContainsSpecialCharacters("имя_файла"));
        assertFalse(pathUtil.isContainsSpecialCharacters("имя файла .pdf"));
        assertFalse(pathUtil.isContainsSpecialCharacters("имя.Файла123_And English"));

        assertTrue(pathUtil.isContainsSpecialCharacters("file/name"));
        assertTrue(pathUtil.isContainsSpecialCharacters("file\\name"));
        assertTrue(pathUtil.isContainsSpecialCharacters("file:name"));
        assertTrue(pathUtil.isContainsSpecialCharacters("file*name"));
        assertTrue(pathUtil.isContainsSpecialCharacters("file?name"));
        assertTrue(pathUtil.isContainsSpecialCharacters("file<name"));
        assertTrue(pathUtil.isContainsSpecialCharacters("file>name"));
        assertTrue(pathUtil.isContainsSpecialCharacters("file|name"));
        assertTrue(pathUtil.isContainsSpecialCharacters("file\"name"));
        assertTrue(pathUtil.isContainsSpecialCharacters(null));
        assertTrue(pathUtil.isContainsSpecialCharacters(""));
    }
}