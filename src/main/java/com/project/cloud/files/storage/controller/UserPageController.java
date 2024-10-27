package com.project.cloud.files.storage.controller;

import com.project.cloud.files.storage.model.dto.ContentDto;
import com.project.cloud.files.storage.service.FileService;
import com.project.cloud.files.storage.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;


@Controller
@RequiredArgsConstructor
public class UserPageController {

    private final UserService userService;
    private final FileService fileService;

    @GetMapping("/prepare-main-directories")
    public String createMainDirectories(HttpSession session) {

        String userRootPath = getUserRootPath(session);
        String trashPath = userRootPath + "/Trash/";

        if (!fileService.isDirectoryExists(trashPath))
            fileService.createFolder(trashPath);

        if (!fileService.isDirectoryExists(userRootPath))
            fileService.createFolder(userRootPath);

        return "redirect:/";
    }


    //TODO:
    // 2. Add UTF-8 encoding to correctly handle Russian symbols
    @GetMapping("/")
    public String generateUserPage(HttpSession session, Model model,
                                   @RequestParam(required = false) String path,
                                   @RequestParam(required = false) String query) throws Exception {

        String username = (String) session.getAttribute("username");
        String userRootPath = getUserRootPath(session);

        String innerPath = createInnerPath(path, userRootPath);
        String publicPath = createPublicPath(innerPath, userRootPath);

        if (!fileService.isDirectoryExists(innerPath)) {
            return "redirect:/";
        }

        List<ContentDto> pageContent;

        if (query != null) {
            pageContent = fileService.searchFiles(userRootPath, query);
        }
        else {
            pageContent = fileService.getListFilesInFolder(innerPath);
        }

        model.addAttribute("username", username);
        model.addAttribute("content", pageContent);
        model.addAttribute("path", publicPath);
        model.addAttribute("query", query != null ? query : "");
        return "user/user-claud";
    }

    //TODO: Add UTF-8 encoding to correctly handle Russian symbols
    @GetMapping("/back")
    public String back(@RequestParam String path) {

        if (!path.contains("/"))
            return "redirect:/";

        return String.format("redirect:/?path=%s", path.substring(0, path.lastIndexOf('/')));
    }


    private String createPublicPath(String path, String userRootPath) {
        return path.equals(userRootPath) ? "" : path.substring(userRootPath.length() + 1);
    }

    private String createInnerPath(String path, String userRootPath) {
        return (path == null || path.isEmpty()) ? userRootPath : String.format("%s/%s", userRootPath, path);
    }

    private String getUserRootPath(HttpSession session) {
        String username = (String) session.getAttribute("username");
        Long id = userService.getIdByUsername(username);
        return String.format("user-%d-files", id);
    }

}