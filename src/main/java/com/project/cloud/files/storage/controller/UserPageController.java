package com.project.cloud.files.storage.controller;

import com.project.cloud.files.storage.model.dto.ContentDto;
import com.project.cloud.files.storage.service.FileService;
import com.project.cloud.files.storage.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;


@Controller
@RequiredArgsConstructor
public class UserPageController {

    private final UserService userService;
    private final FileService fileService;

    @GetMapping("/prepare-main-directories")
    public String index(HttpSession session) {
        String username = (String) session.getAttribute("username");
        Long id = userService.getIdByUsername(username);

        String defaultPath = "user-" + id + "-files";
        String trashPath = defaultPath + "/Trash/";

        if (!fileService.isDirectoryExists(trashPath))
            fileService.createFolder(trashPath);

        if (!fileService.isDirectoryExists(defaultPath))
            fileService.createFolder(defaultPath);

        return "redirect:/?path=" + defaultPath;
    }

    private String getUserRootPath(HttpSession session) {
        String username = (String) session.getAttribute("username");
        Long id = userService.getIdByUsername(username);
        return String.format("user-%d-files", id);
    }

    //TODO: Eliminate this method
    @GetMapping("/trash")
    public String trash(HttpSession session) {
        String userRootPath = getUserRootPath(session);
        return String.format("redirect:/?path=%s/Trash",userRootPath);
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
        String rootFilesDirectory = "user-" + id + "-files";


        if (path == null || !fileService.isDirectoryExists(path)) {
            return "redirect:/?path=" + rootFilesDirectory;
        }

        if (path.endsWith("/"))
            path = path.substring(0, path.length() - 1);

        List<ContentDto> pageContent;
        String request = "";

        if (query != null) {
            pageContent = fileService.searchFiles(id, query);
            model.addAttribute("content", pageContent);
            request = query;
        } else if ( path.equals("Trash")) {
            pageContent = fileService.getListFilesInFolder("/user-" + id + "-files/Trash/");
        } else {
            pageContent = fileService.getListFilesInFolder(path);
        }

        model.addAttribute("username", username);
        model.addAttribute("content", pageContent);
        model.addAttribute("path", path);
        model.addAttribute("query", request);
        return "user/user-claud";
    }

    // TODO: Add functionality to navigate to file's parent directory from search results
    @GetMapping("/search-content")
    public String search(@RequestParam String query, HttpSession session) {
        String username = (String) session.getAttribute("username");
        Long id = userService.getIdByUsername(username);
        String defaultPath = "user-" + id + "-files";
        return String.format("redirect:/?path=%s&query=%s", defaultPath, query);
    }


    @PostMapping("/upload-content")
    public String uploadContent(@RequestParam("data") MultipartFile[] data,
                                @RequestParam String path) throws IOException {

        for (MultipartFile file : data) {
            fileService.upload(file, path);
        }

        return "redirect:/?path=" + path;
    }

    // TODO: Check folder name uniqueness in parent path
    @PostMapping("/create-folder")
    public String createFolder(@RequestParam String path, @RequestParam String name) {
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        fileService.createFolder(path + "/" + name);

        return "redirect:/?path=" + path;
    }

    @GetMapping("/download-content")
    public void downloadContent(@RequestParam String path, @RequestParam String name,
                                @RequestParam boolean isFile, HttpServletResponse response) {

        if (isFile) {
            response.setHeader("Content-Disposition", "attachment; filename=\"" + name + "\"");
            response.setContentType("application/octet-stream");

        } else {
            response.setHeader("Content-Disposition", "attachment; filename=\"" + name + ".zip" + "\"");
            response.setContentType("application/zip");
        }

        try (InputStream inputStream = fileService.downloadContent(path + "/" + name, isFile);
             OutputStream outputStream = response.getOutputStream()) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
        } catch (IOException e) {
            throw new RuntimeException("Error downloading folder: " + e.getMessage(), e);
        }
    }

    //TODO: Add ability to move files/folders between directories
    @PostMapping("/move-content")
    public String moveContent(@RequestParam String path, @RequestParam String name,
                              @RequestParam String destinationPath, @RequestParam boolean isFile, HttpSession session)  {

        String oldPath = isFile ? path + "/" + name : path + "/" + name + "/";
        if (destinationPath.equals("Trash")) {
            String username = (String) session.getAttribute("username");
            Long userId = userService.getIdByUsername(username);
            destinationPath = "user-" + userId + "-files/Trash/";
            fileService.moveContent(oldPath, destinationPath, isFile);

            return "redirect:/?path=" + path;
        }

        fileService.moveContent(oldPath, destinationPath, isFile);
        return "redirect:/?path=" + destinationPath;
    }

    @PostMapping("/delete-content")
    public String deleteContent(@RequestParam String path, @RequestParam String name,
                                @RequestParam boolean isFile, HttpSession session) {

        if (!path.contains("Trash")) {
            moveContent(path, name, "Trash", isFile, session);
        } else
            fileService.deleteContent(path, name, isFile);

        return "redirect:/?path=" + path;
    }

    //TODO: Add UTF-8 encoding to correctly handle Russian symbols
    @GetMapping("/back")
    public String back(@RequestParam String path) {
        int lastSlashIndex = path.lastIndexOf('/');

        if (lastSlashIndex > 0)
            path = path.substring(0, lastSlashIndex);

        return "redirect:/?path=" + path;
    }

}