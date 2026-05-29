package com.evbooking.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Replaces the default Whitelabel error page.
 * Browser requests (Accept: text/html) are redirected to the main page.
 * API requests keep the standard Spring error JSON response.
 */
@Controller
public class AppErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request) {
        Object statusAttr = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (statusAttr != null) {
            int status = Integer.parseInt(statusAttr.toString());
            if (status == HttpStatus.FORBIDDEN.value()) {
                return "redirect:/index.html";
            }
        }
        return "redirect:/index.html";
    }
}
