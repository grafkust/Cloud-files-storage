package com.project.cloud.files.storage.controller;

import com.project.cloud.files.storage.model.dto.StorageItemDto;
import com.project.cloud.files.storage.service.file.FileOperationService;
import com.project.cloud.files.storage.util.validator.PathUtil;
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
public class ContentOperationController {

    private final FileOperationService fileOperationService;
    private final PathUtil pathUtil;

    @PostMapping("/create-folder")
    public String createDirectory(@RequestParam String path,
                                  @RequestParam String name, HttpSession session) {


        String userRootPath = pathUtil.getUserRootPath(session);
        String innerPath = pathUtil.createInnerPath(path, userRootPath);
        String publicPath = pathUtil.createPublicPath(innerPath, userRootPath, true);

        boolean folderNameNotUnique = fileOperationService.isDirectoryNameTaken(innerPath, name);
        boolean invalidName = pathUtil.isContainsSpecialCharacters(name);

        if (folderNameNotUnique || invalidName) {
            String errorType = folderNameNotUnique ? "duplicate" : "invalid";
            return path.isEmpty()
                    ? String.format("redirect:/?error=%s-folder-name", errorType)
                    : String.format("redirect:/?path=%s&error=%s-name", publicPath, errorType);
        }

        String folderPath = String.format("%s/%s", innerPath, name);
        fileOperationService.createDirectory(folderPath);


        return publicPath.isEmpty() ? "redirect:/" : String.format("redirect:/?path=%s", publicPath);
    }

    @PostMapping("/upload-content")
    public String uploadFileOrDirectory(@RequestParam("data") MultipartFile[] data,
                                        @RequestParam String path, HttpSession session) {
        String userRootPath = pathUtil.getUserRootPath(session);
        String innerPath = pathUtil.createInnerPath(path, userRootPath);

        for (MultipartFile file : data) {
            fileOperationService.uploadFileOrDirectory(file, innerPath);
        }

        String publicPath = pathUtil.createPublicPath(innerPath, userRootPath, true);

        return publicPath.isEmpty() ? "redirect:/" : String.format("redirect:/?path=%s", publicPath);
    }

    @GetMapping("/download-content")
    public void downloadFileOrDirectory(@RequestParam String path,
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

        try (InputStream inputStream = fileOperationService.downloadFileOrDirectory(contentPath, isFile);
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
    public String moveFileOrDirectory(@RequestParam String path,
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
            destinationPath = userRootPath + "/Trash/";
            fileOperationService.moveFileOrDirectory(oldPath, destinationPath, isFile);
            return String.format("redirect:/?path=%s", pathUtil.encodePath(path));
        }
        fileOperationService.moveFileOrDirectory(oldPath, destinationPath, isFile);

        String publicPath = pathUtil.createPublicPath(destinationPath, userRootPath, true);

        return publicPath.isEmpty() ? "redirect:/": String.format("redirect:/?path=%s", publicPath);
    }

    @GetMapping("/get-directories")
    public String listDirectories(@RequestParam String path,
                                  @RequestParam String name,
                                  @RequestParam String filePath,
                                  Model model, HttpSession session) {

        filePath = pathUtil.getContentRootPath(filePath, name);

        String userRootPath = pathUtil.getUserRootPath(session);

        String publicPath = pathUtil.createPublicPath(path, userRootPath, false);
        String innerPath = pathUtil.createInnerPath(publicPath, userRootPath);
        List<StorageItemDto> folders = fileOperationService.listDirectory(innerPath, name);

        model.addAttribute("directories", folders);
        model.addAttribute("filePath", filePath);
        model.addAttribute("name", name);

        return "fragments/directory-list";
    }

    @PostMapping("/delete-content")
    public String deleteFileOrDirectory(@RequestParam String path,
                                        @RequestParam String name,
                                        @RequestParam boolean isFile,
                                        HttpSession session) {
        String userRootPath = pathUtil.getUserRootPath(session);
        String innerPath = pathUtil.createInnerPath(path, userRootPath);

        boolean isInTrash = innerPath.contains("Trash");

        if (isInTrash) {
            fileOperationService.deleteFileOrDirectory(innerPath, name, isFile);
        } else
            moveFileOrDirectory(path, name, "Trash", isFile, session);

        String publicPath = pathUtil.createPublicPath(innerPath, userRootPath, true);
        return publicPath.isEmpty() ? "redirect:/" : String.format("redirect:/?path=%s", publicPath);
    }

}

