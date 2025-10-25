package com.example.Kadr.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.stream.Collectors;


@ControllerAdvice
public class GlobalExceptionHandlers {

    private String normalizeBack(String referer) {
        if (referer == null || referer.isBlank()) return "/";
        try {
            URI uri = new URI(referer);
            String path = uri.getPath();
            String query = uri.getQuery();
            String target = (path == null || path.isBlank()) ? "/" : path;
            if (!target.startsWith("/")) target = "/" + target;
            if (query != null && !query.isBlank()) target += "?" + query;
            return target;
        } catch (URISyntaxException e) {
            return referer.startsWith("/") ? referer : "/" + referer;
        }
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public String handleConstraintViolation(ConstraintViolationException ex,
                                            HttpServletRequest req,
                                            RedirectAttributes ra) {
        String message = ex.getConstraintViolations().stream()
                .map(v -> v.getMessage())
                .collect(Collectors.joining("; "));
        ra.addFlashAttribute("error", message);

        return "redirect:" + normalizeBack(req.getHeader("Referer"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgument(IllegalArgumentException ex,
                                        HttpServletRequest req,
                                        RedirectAttributes ra) {
        ra.addFlashAttribute("error", ex.getMessage());
        return "redirect:" + normalizeBack(req.getHeader("Referer"));
    }
}
