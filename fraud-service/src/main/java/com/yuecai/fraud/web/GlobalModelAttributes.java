package com.yuecai.fraud.web;

import java.security.Principal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Exposes the signed-in username to every JSP (the sidebar used to read it from the HttpSession). */
@ControllerAdvice(basePackageClasses = GlobalModelAttributes.class)
public class GlobalModelAttributes {

    @ModelAttribute("username")
    public String username(Principal principal) {
        return principal == null ? "" : principal.getName();
    }
}
