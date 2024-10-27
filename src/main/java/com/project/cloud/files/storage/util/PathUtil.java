package com.project.cloud.files.storage.util;

import com.project.cloud.files.storage.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class PathUtil {

    private final UserService userService;

    public String createPublicPath(String path, String userRootPath) {
        return path.equals(userRootPath) ? "" : path.substring(userRootPath.length() + 1);
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


}
