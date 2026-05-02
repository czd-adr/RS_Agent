package com.xmut.shop.controller;

import com.xmut.shop.agent.WebGisAgent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/WebGISAgent")
public class ChatAnalysisController {
    @Autowired
    private WebGisAgent webGisAgent; // 注入你定义的 AiService 接口

    @GetMapping("/chat")
    public String testAgent(@RequestParam String query) {
        // 执行对话
        return webGisAgent.chat(query);
    }
}
