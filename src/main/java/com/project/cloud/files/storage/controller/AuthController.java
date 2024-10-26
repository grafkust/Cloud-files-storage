package com.project.cloud.files.storage.controller;

import com.project.cloud.files.storage.mapper.UserMapper;
import com.project.cloud.files.storage.model.dto.UserDto;
import com.project.cloud.files.storage.model.entity.user.User;
import com.project.cloud.files.storage.service.MailService;
import com.project.cloud.files.storage.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/auth")
@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserMapper userMapper;
    private final UserService userService;
    private final MailService mailService;

    @GetMapping("/login")
    private String login(Model model, @RequestParam(required = false) String reg) {
        model.addAttribute("illegalArgument", "no exception");
        model.addAttribute("username");
        model.addAttribute("reg", reg);
        return "auth/login";
    }

    @PostMapping("/login")
    public String loginError() {
        return "auth/login";
    }


    @GetMapping("/registration")
    private String registration(Model model) {
        model.addAttribute("userDto", new UserDto());
        model.addAttribute("fieldNotUnique", "no exception");
        return "auth/registration";
    }

    @PostMapping("/registration")
    public String regUser(@Validated @ModelAttribute("userDto") UserDto userDto,
                          BindingResult bindingResult, Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("fieldNotUnique", "no exception");
            return "auth/registration";
        }
        User user = userMapper.toEntity(userDto);
        userService.create(user);
        mailService.sendEmail(user);
        return "redirect:/auth/login?reg=success";
    }


}
