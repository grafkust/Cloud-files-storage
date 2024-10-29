package com.project.cloud.files.storage.controller;

import com.project.cloud.files.storage.model.dto.ContentDto;
import com.project.cloud.files.storage.service.FileService;
import com.project.cloud.files.storage.util.PathUtil;
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

    private final FileService fileService;
    private final PathUtil pathUtil;

    @GetMapping("/prepare-main-directories")
    public String createMainDirectories(HttpSession session) {

        String userRootPath = pathUtil.getUserRootPath(session);
        String trashPath = userRootPath + "/Trash";

        if (fileService.directoryDoesNotExist(trashPath))
            fileService.createFolder(trashPath);

        if (fileService.directoryDoesNotExist(userRootPath))
            fileService.createFolder(userRootPath);

        return "redirect:/";
    }

    @GetMapping("/")
    public String generateUserPage(HttpSession session, Model model,
                                   @RequestParam(required = false) String path,
                                   @RequestParam(required = false) String query,
                                   @RequestParam(required = false) String error) throws Exception {

        String username = (String) session.getAttribute("username");
        String userRootPath = pathUtil.getUserRootPath(session);

        String innerPath = pathUtil.createInnerPath(path, userRootPath);
        String publicPath = pathUtil.createPublicPath(innerPath, userRootPath);

        if (!innerPath.contains("Trash") && fileService.directoryDoesNotExist(innerPath)) {
            return "redirect:/";
        }

        List<ContentDto> pageContent;

        if (query != null && !query.isEmpty()) {
            pageContent = fileService.searchFiles(userRootPath, query);
        } else {
            pageContent = fileService.getListFilesInFolder(innerPath);
        }

        model.addAttribute("username", username);
        model.addAttribute("content", pageContent);
        model.addAttribute("path", publicPath);
        model.addAttribute("query", query != null ? query : "");
        model.addAttribute("error", error != null ? error : "");
        return "user/user-page";
    }

    @GetMapping("/back")
    public String back(@RequestParam String path) {

        if (!path.contains("/"))
            return "redirect:/";

        path = path.substring(0, path.lastIndexOf('/'));

        return String.format("redirect:/?path=%s", pathUtil.encodePath(path));
    }


}