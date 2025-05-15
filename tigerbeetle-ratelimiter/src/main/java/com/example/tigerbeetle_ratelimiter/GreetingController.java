package com.example.tigerbeetle_ratelimiter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequiredArgsConstructor
public class GreetingController {

    @GetMapping("/greeting")
    public @ResponseBody String greeting() {
        return "hello";
    }
}
