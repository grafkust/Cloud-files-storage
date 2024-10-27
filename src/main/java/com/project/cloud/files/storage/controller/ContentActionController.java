package com.project.cloud.files.storage.controller;

import com.project.cloud.files.storage.service.FileService;
import com.project.cloud.files.storage.util.PathUtil;
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

    private final FileService fileService;
    private final PathUtil pathUtil;


    // TODO: Check folder name uniqueness in parent path
    @PostMapping("/create-folder")
    public String createFolder(@RequestParam String path,
                               @RequestParam String name, HttpSession session) {
        String userRootPath = pathUtil.getUserRootPath(session);
        String innerPath = pathUtil.createInnerPath(path, userRootPath);
        String publicPath = pathUtil.createPublicPath(innerPath, userRootPath);

        if (publicPath.endsWith("/")) {
            publicPath = publicPath.substring(0, path.length() - 1);
        }
        String folderPath = String.format("%s/%s", innerPath, name);
        fileService.createFolder(folderPath);
        publicPath = pathUtil.encodePath(publicPath);
        return publicPath.isEmpty() ? "redirect:/" : String.format("redirect:/?path=%s", publicPath);
    }


    @PostMapping("/upload-content")
    public String uploadContent(@RequestParam("data") MultipartFile[] data,
                                @RequestParam String path, HttpSession session) throws IOException {

        String userRootPath = pathUtil.getUserRootPath(session);
        String innerPath = pathUtil.createInnerPath(path, userRootPath);
        String publicPath = pathUtil.createPublicPath(innerPath, userRootPath);

        for (MultipartFile file : data) {
            fileService.upload(file, innerPath);
        }
        publicPath = pathUtil.encodePath(publicPath);
        return publicPath.isEmpty() ? "redirect:/" : String.format("redirect:/?path=%s", publicPath);

    }

    @GetMapping("/download-content")
    public void downloadContent(@RequestParam String path,
                                @RequestParam String name,
                                @RequestParam boolean isFile,
                                HttpServletResponse response, HttpSession session) {

        String userRootPath = pathUtil.getUserRootPath(session);
        String innerPath = pathUtil.createInnerPath(path, userRootPath);
        String contentPath = String.format("%s/%s", innerPath, name);

        if (isFile) {
            response.setHeader("Content-Disposition", "attachment; filename=\"" + name + "\";");
            response.setContentType("application/octet-stream");

        } else {
            response.setHeader("Content-Disposition", "attachment; filename=\"" + name + ".zip" + "\"");
            response.setContentType("application/zip");

        }
        response.setCharacterEncoding("UTF-8");
        try (InputStream inputStream = fileService.downloadContent(contentPath, isFile);
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
    public String moveContent(@RequestParam String path,
                              @RequestParam String name,
                              @RequestParam String destinationPath,
                              @RequestParam boolean isFile, HttpSession session) {

        String userRootPath = pathUtil.getUserRootPath(session);
        String innerPath = pathUtil.createInnerPath(path, userRootPath);
        String oldPath = isFile ? innerPath + "/" + name : innerPath + "/" + name + "/";
        if (destinationPath.equals("Trash")) {
            destinationPath = userRootPath + "/Trash/";
            fileService.moveContent(oldPath, destinationPath, isFile);
            return String.format("redirect:/?path=%s", path);
        }

        fileService.moveContent(oldPath, destinationPath, isFile);
        destinationPath = pathUtil.encodePath(destinationPath);
        return String.format("redirect:/?path=%s", destinationPath);
    }


    // TODO: Implement daily automatic cleanup of expired files in trash
    @PostMapping("/delete-content")
    public String deleteContent(@RequestParam String path,
                                @RequestParam String name,
                                @RequestParam boolean isFile,
                                HttpSession session) {

        String userRootPath = pathUtil.getUserRootPath(session);
        String innerPath = pathUtil.createInnerPath(path, userRootPath);
        String publicPath = pathUtil.createPublicPath(innerPath, userRootPath);

        if (!innerPath.contains("Trash")) {
            moveContent(path, name, "Trash", isFile, session);
        } else
            fileService.deleteContent(innerPath, name, isFile);

        publicPath = pathUtil.encodePath(publicPath);
        return publicPath.isEmpty() ? "redirect:/" : String.format("redirect:/?path=%s", publicPath);
    }









}
