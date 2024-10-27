package com.project.cloud.files.storage.controller;

import com.project.cloud.files.storage.service.FileService;
import com.project.cloud.files.storage.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Controller
@RequiredArgsConstructor
public class ContentActionController {

    private final UserService userService;
    private final FileService fileService;


    // TODO: Check folder name uniqueness in parent path
    @PostMapping("/create-folder")
    public String createFolder(@RequestParam String path, @RequestParam String name) {
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        String folderPath = String.format("%s/%s", path, name);
        fileService.createFolder(folderPath);

        return String.format("redirect:/?path=%s", path);
    }


    // TODO: Add functionality to navigate to file's parent directory from search results
    @GetMapping("/search-content")
    public String searchContent(@RequestParam String query, HttpSession session) {
        String username = (String) session.getAttribute("username");
        Long id = userService.getIdByUsername(username);
        String userRootPath = String.format("user-%d-files", id);
        return String.format("redirect:/?path=%s&query=%s", userRootPath, query);
    }


    @PostMapping("/upload-content")
    public String uploadContent(@RequestParam("data") MultipartFile[] data,
                                @RequestParam String path) throws IOException {

        for (MultipartFile file : data) {
            fileService.upload(file, path);
        }
        return String.format("redirect:/?path=%s", path);

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


    // TODO: Add ability to move files/folders between directories
    @PostMapping("/move-content")
    public String moveContent(@RequestParam String path, @RequestParam String name,
                              @RequestParam String destinationPath, @RequestParam boolean isFile, HttpSession session) {

        String oldPath = isFile ? path + "/" + name : path + "/" + name + "/";
        if (destinationPath.equals("Trash")) {
            String username = (String) session.getAttribute("username");
            Long userId = userService.getIdByUsername(username);
            destinationPath = "user-" + userId + "-files/Trash/";
            fileService.moveContent(oldPath, destinationPath, isFile);

            return String.format("redirect:/?path=%s", path);
        }

        fileService.moveContent(oldPath, destinationPath, isFile);
        return String.format("redirect:/?path=%s", destinationPath);
    }


    // TODO: Implement daily automatic cleanup of expired files in trash
    @PostMapping("/delete-content")
    public String deleteContent(@RequestParam String path, @RequestParam String name,
                                @RequestParam boolean isFile, HttpSession session) {

        if (!path.contains("Trash")) {
            moveContent(path, name, "Trash", isFile, session);
        } else
            fileService.deleteContent(path, name, isFile);

        return String.format("redirect:/?path=%s", path);
    }

    // TODO: Add method to restore files from trash


}
