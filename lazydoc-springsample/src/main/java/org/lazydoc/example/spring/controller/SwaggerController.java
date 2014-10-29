package org.lazydoc.example.spring.controller;

import org.lazydoc.annotation.IgnoreForDocumentation;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@IgnoreForDocumentation
public class SwaggerController {

    @RequestMapping("/swagger")
    public String swagger() {
        return "swagger";
    }
}
