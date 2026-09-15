package com.yuecai.fraud.web;

import com.yuecai.fraud.user.RegistrationForm;
import com.yuecai.fraud.user.UserAlreadyExistsException;
import com.yuecai.fraud.user.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Login is rendered here but processed by Spring Security's form-login filter;
 * registration is handled by this controller.
 */
@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("form", new RegistrationForm());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegistrationForm form,
                           BindingResult binding,
                           RedirectAttributes redirect) {
        if (!form.passwordsMatch()) {
            binding.rejectValue("password2", "mismatch", "Password and confirm password do not match.");
        }
        if (binding.hasErrors()) {
            return "register";
        }
        try {
            userService.register(form);
        } catch (UserAlreadyExistsException e) {
            binding.rejectValue("username", "duplicate", "Username already exists. Please choose a different username.");
            return "register";
        }
        redirect.addFlashAttribute("msg", "Registration successful. You can now log in.");
        return "redirect:/login";
    }
}
