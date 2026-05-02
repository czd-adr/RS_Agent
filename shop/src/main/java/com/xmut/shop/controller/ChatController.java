package com.xmut.shop.controller;

import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class ChatController {
    @Autowired
    private OpenAiChatModel openAiChatModel;

    @RequestMapping("/chat")
    public String chat(String msg) {
        return openAiChatModel.chat(msg);
    }
}
