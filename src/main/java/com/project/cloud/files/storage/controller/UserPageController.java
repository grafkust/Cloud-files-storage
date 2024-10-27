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

        return String.format("redirect:/?path=%s", userRootPath);
    }


    //TODO:
    // 1. Remove userRootPath from url
    // 2. Add UTF-8 encoding to correctly handle Russian symbols
    @GetMapping("/")
    public String generateUserPage(HttpSession session, Model model,
                                   @RequestParam(required = false) String path,
                                   @RequestParam(required = false) String query) throws Exception {

        String username = (String) session.getAttribute("username");
        Long id = userService.getIdByUsername(username);
        String userRootPath = String.format("user-%d-files", id);

        if (path == null || !fileService.isDirectoryExists(path)) {
            return String.format("redirect:/?path=%s", userRootPath);
        }

        if (path.endsWith("/"))
            path = path.substring(0, path.length() - 1);

        List<ContentDto> pageContent;
        String request = "";

        if (query != null) {
            pageContent = fileService.searchFiles(id, query);
            model.addAttribute("content", pageContent);
            request = query;
        } else if (path.equals("Trash")) {
            String trashPath = String.format("%s/Trash/", userRootPath);
            pageContent = fileService.getListFilesInFolder(trashPath);
        } else {
            pageContent = fileService.getListFilesInFolder(path);
        }

        model.addAttribute("username", username);
        model.addAttribute("content", pageContent);
        model.addAttribute("path", path);
        model.addAttribute("query", request);
        return "user/user-claud";
    }


    //TODO: Add UTF-8 encoding to correctly handle Russian symbols
    @GetMapping("/back")
    public String back(@RequestParam String path) {
        int lastSlashIndex = path.lastIndexOf('/');

        if (lastSlashIndex > 0)
            path = path.substring(0, lastSlashIndex);

        return String.format("redirect:/?path=%s", path);
    }

    //TODO: Eliminate this method
    @GetMapping("/trash")
    public String trash(HttpSession session) {
        String userRootPath = getUserRootPath(session);
        return String.format("redirect:/?path=%s/Trash", userRootPath);
    }

    private String getUserRootPath(HttpSession session) {
        String username = (String) session.getAttribute("username");
        Long id = userService.getIdByUsername(username);
        return String.format("user-%d-files", id);
    }
}