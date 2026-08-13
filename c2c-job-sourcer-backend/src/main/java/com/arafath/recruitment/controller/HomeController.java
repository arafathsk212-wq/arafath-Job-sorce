package com.arafath.recruitment.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import java.util.HashMap;
import java.util.Map;

@RestController
public class HomeController {

    @GetMapping("/")
    public RedirectView redirectRoot() {
        return new RedirectView("/api/jobs");
    }

    @RequestMapping("/error")
    public ResponseEntity<Map<String, Object>> handleError(HttpServletRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", "error");
        body.put("message", "Requested resource not found");
        body.put("path", request.getRequestURI());
        return ResponseEntity.status(404).body(body);
    }
}
