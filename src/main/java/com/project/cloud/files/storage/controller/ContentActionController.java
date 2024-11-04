package com.project.cloud.files.storage.controller;

import com.project.cloud.files.storage.model.dto.ContentDto;
import com.project.cloud.files.storage.service.FileService;
import com.project.cloud.files.storage.util.PathUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ContentActionController {

    private final FileService fileService;
    private final PathUtil pathUtil;

    @PostMapping("/create-folder")
    public String createFolder(@RequestParam String path,
                               @RequestParam String name, HttpSession session) {
        String userRootPath = pathUtil.getUserRootPath(session);
        String innerPath = pathUtil.createInnerPath(path, userRootPath);
        String publicPath = pathUtil.createPublicPath(innerPath, userRootPath);
        publicPath = pathUtil.encodePath(publicPath);

        boolean folderNameNotUnique = fileService.folderNameNotUnique(innerPath, name);

        if (folderNameNotUnique) {
            return path.isEmpty() ? "redirect:/?error=folder-name" :
                    String.format("redirect:/?path=%s&error=folder-name", publicPath);
        }

        String folderPath = String.format("%s/%s", innerPath, name);
        fileService.createFolder(folderPath);


        return publicPath.isEmpty() ? "redirect:/" : String.format("redirect:/?path=%s", publicPath);
    }

    @PostMapping("/upload-content")
    public String uploadContent(@RequestParam("data") MultipartFile[] data,
                                @RequestParam String path, HttpSession session) throws Exception {
        String userRootPath = pathUtil.getUserRootPath(session);
        String innerPath = pathUtil.createInnerPath(path, userRootPath);

        for (MultipartFile file : data) {
            fileService.uploadContent(file, innerPath);
        }

        String publicPath = pathUtil.createPublicPath(innerPath, userRootPath);
        publicPath = pathUtil.encodePath(publicPath);
        return publicPath.isEmpty() ? "redirect:/" : String.format("redirect:/?path=%s", publicPath);
    }

    @GetMapping("/download-content")
    public void downloadContent(@RequestParam String path,
                                @RequestParam String name,
                                @RequestParam boolean isFile,
                                HttpServletResponse response, HttpSession session) throws FileUploadException {

        String userRootPath = pathUtil.getUserRootPath(session);
        String innerPath = pathUtil.createInnerPath(path, userRootPath);
        String contentPath = String.format("%s/%s", innerPath, name);

        name = isFile ? name : name + ".zip";
        String encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20");
        String contentType = isFile ? "octet-stream" : "zip";

        response.setHeader("Content-Disposition", "attachment; filename=\"" + encodedName + "\";");
        response.setContentType("application/" + contentType);

        try (InputStream inputStream = fileService.downloadContent(contentPath, isFile);
             OutputStream outputStream = response.getOutputStream()) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
        } catch (Exception e) {
            throw new FileUploadException("Error downloading content", e);
        }
    }

    @PostMapping("/move-content")
    public String moveContent(@RequestParam String path,
                              @RequestParam String name,
                              @RequestParam String destinationPath,
                              @RequestParam boolean isFile, HttpSession session) {
        String userRootPath = pathUtil.getUserRootPath(session);
        String innerPath = pathUtil.createInnerPath(path, userRootPath);

        boolean isMovingToSameFolder = (innerPath + "/").equals(destinationPath);
        if (isMovingToSameFolder) {
            return String.format("redirect:/?path=%s&error=move", pathUtil.encodePath(path));
        }

        String oldPath = innerPath + "/" + name;

        if (destinationPath.equals("Trash")) {
            destinationPath = userRootPath + "/Trash";
            fileService.moveContent(oldPath, destinationPath, isFile);
            return String.format("redirect:/?path=%s", pathUtil.encodePath(path));
        }
        fileService.moveContent(oldPath, destinationPath, isFile);

        String publicPath = pathUtil.createPublicPath(destinationPath, userRootPath);
        publicPath = pathUtil.encodePath(publicPath);
        return String.format("redirect:/?path=%s", publicPath);
    }

    @GetMapping("/get-directories")
    public String getDirectories(@RequestParam String path,
                                 @RequestParam String name,
                                 @RequestParam String filePath,
                                 Model model, HttpSession session) {

        filePath = pathUtil.getContentRootPath(filePath, name);

        String userRootPath = pathUtil.getUserRootPath(session);

        String publicPath = path.isEmpty() ? path : pathUtil.createPublicPath(path, userRootPath);
        String innerPath = pathUtil.createInnerPath(publicPath, userRootPath);
        List<ContentDto> folders = fileService.getListDirectories(innerPath, name);

        model.addAttribute("directories", folders);
        model.addAttribute("filePath", filePath);
        model.addAttribute("name", name);

        return "fragments/directory-list";
    }

    @PostMapping("/delete-content")
    public String deleteContent(@RequestParam String path,
                                @RequestParam String name,
                                @RequestParam boolean isFile,
                                HttpSession session) {
        String userRootPath = pathUtil.getUserRootPath(session);
        String innerPath = pathUtil.createInnerPath(path, userRootPath);

        boolean isInTrash = innerPath.contains("Trash");

        if (isInTrash) {
            fileService.deleteContent(innerPath, name, isFile);
        } else
            moveContent(path, name, "Trash", isFile, session);

        String publicPath = pathUtil.createPublicPath(innerPath, userRootPath);
        publicPath = pathUtil.encodePath(publicPath);
        return publicPath.isEmpty() ? "redirect:/" : String.format("redirect:/?path=%s", publicPath);
    }

}

