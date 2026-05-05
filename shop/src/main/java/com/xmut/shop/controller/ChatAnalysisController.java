package com.xmut.shop.controller;

import com.xmut.shop.DTO.AiNDVIResponse;
import com.xmut.shop.DTO.ChatRequest;
import com.xmut.shop.agent.WebGisAgent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/WebGISAgent")
public class ChatAnalysisController {
    @Autowired
    private WebGisAgent webGisAgent; // 注入你定义的 AiService 接口

    @Autowired
    private SamplePointsController samplePointsController;

    @RequestMapping(value = "/chat",produces = "text/html;charset=utf-8")
    public Flux<String> chat(String message) {
        Flux<String> result = webGisAgent.chat(message);
        return result;
    }

}
