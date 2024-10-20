package com.project.cloud.files.storage.controller;

import com.project.cloud.files.storage.model.entity.file.ContentDto;
import com.project.cloud.files.storage.service.FileService;
import com.project.cloud.files.storage.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;


@Controller
@RequiredArgsConstructor
public class UserController {

    //step 1 - get userId
    //step 2 - get UserMainFolder
    //step 3 - get Content from MainFolder
    //step 4 - get Chosen folder and internal content
    //step 5 - loop this cycle

    //btn - create folder
    //field - upload file
    //action - rename folder/file
    //action - remove folder/file
    //action - transport folder/file


    private final UserService userService;
    private final FileService fileService;

    @GetMapping("/user")
    public String index(HttpSession session) {
        String username = (String) session.getAttribute("username");
        Long id = userService.getIdByUsername(username);
        String defaultPath = "user-" + id + "-files";

        return "redirect:/?path=" + defaultPath;
    }


    @GetMapping
    public String generateUserPage(HttpSession session, Model model,
                                   @RequestParam(required = false) String path) throws Exception {

        if (path == null)
            return "redirect:/user";

        String username = (String) session.getAttribute("username");
        model.addAttribute("username", username);

        List<String> listFilesInFolder = fileService.getListFilesInFolder(path);
        model.addAttribute("data", listFilesInFolder);
        model.addAttribute("path", path);

        return "user/user-claud";
    }

    @PostMapping("/upload-file")
    public String uploadFile(@RequestParam("file") MultipartFile file,
                             @RequestParam String path) throws IOException {

        fileService.upload(file, path + "/");
        return "redirect:/?path=" + path;
    }

    @PostMapping("/upload-folder")
    public String uploadFolder(@RequestParam("files") MultipartFile[] files,
                               @RequestParam String path) throws IOException {

        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                fileService.upload(file, path + "/");
            }
        }

        return "redirect:/?path=" + path;
    }

    @PostMapping("/create-folder")
    public String createFolder(@RequestParam String path, @RequestParam String name) {
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        fileService.createFolder(path + "/" + name);
        return "redirect:/?path=" + path;
    }

    @PostMapping("/delete-file")
    public String deleteFile(@RequestParam String path, @RequestParam String name) {
        fileService.deleteFile(path + "/" + name);
        return "redirect:/?path=" + path;
    }

    @PostMapping("/delete-folder")
    public String deleteFolder(@RequestParam String path, @RequestParam String name) {
        fileService.deleteFolder(path + "/" + name + "/");
        return "redirect:/?path=" + path;
    }

    @GetMapping("/download-file")
    public void download(@RequestParam String path, @RequestParam String name,
                         HttpServletResponse response) throws IOException {
        InputStream inputStream = fileService.download(path + "/" + name);
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + name + "\"");

        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1)
            response.getOutputStream().write(buffer, 0, bytesRead);

        inputStream.close();
        response.getOutputStream().flush();
    }

    @GetMapping("/back")
    public String back(@RequestParam String path) {
        int lastSlashIndex = path.lastIndexOf('/');

        if (lastSlashIndex > 0)
            path = path.substring(0, lastSlashIndex);

        return "redirect:/?path=" + path;
    }






}
