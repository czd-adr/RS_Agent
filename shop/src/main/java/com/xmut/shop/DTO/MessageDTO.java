package com.xmut.shop.DTO;

public class MessageDTO {
    private String role;
    private String content;

    public MessageDTO(String role, String content) {
        this.role = role;
        this.content = content;
    }

    // Getter 和 Setter (必须有，Jackson 才能序列化)
    public String getRole() { return role; }
    public String getContent() { return content; }
}