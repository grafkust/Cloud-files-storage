package com.project.cloud.files.storage.util.validator;

import com.project.cloud.files.storage.service.user.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class PathUtil {

    private final UserService userService;

    public String createPublicPath(String path, String userRootPath, boolean encode) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        String publicPath = path.equals(userRootPath) ? ""
                : path.substring(userRootPath.length() + 1).replaceAll("/$", "");
        return encode ? encodePath(publicPath) : publicPath;
    }

    public String createInnerPath(String path, String userRootPath) {
        return (path == null || path.isEmpty()) ? userRootPath : String.format("%s/%s", userRootPath, path);
    }

    public String getUserRootPath(HttpSession session) {
        String username = (String) session.getAttribute("username");
        Long id = userService.getIdByUsername(username);
        return String.format("user-%d-files", id);
    }

    public String encodePath(String path) {
        return URLEncoder.encode(path, StandardCharsets.UTF_8);
    }

    public String normalizeStoragePath(String path) {
        return path.endsWith("/") ? path : path + "/";
    }

    public String getContentRootPath(String objectName, String fileName) {
        return objectName.contains(fileName)
                ? objectName.substring(0, objectName.lastIndexOf(fileName))
                : objectName;
    }

    public String createFullPath(MultipartFile file, String path) {
        String fileName = file.getOriginalFilename();
        return path + fileName;
    }

    public boolean isContainsSpecialCharacters(String name) {

        if (name == null || name.isEmpty())
            return true;

        String allowedCharactersPattern = "^[a-zA-Zа-яА-ЯёЁ0-9\\s\\-_.]+$";
        return !name.matches(allowedCharactersPattern);
    }


}
