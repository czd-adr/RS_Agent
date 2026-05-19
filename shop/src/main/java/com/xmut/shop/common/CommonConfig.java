package com.xmut.shop.common;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.ClassPathDocumentLoader;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.List;

@Configuration
public class CommonConfig {
    public static boolean enableRAG = true;
    @Autowired
    private ChatMemoryStore redisChatMemoryStore;
    @Autowired
    private EmbeddingModel embeddingModel;
    //构建会话记忆对象
    @Bean
    public ChatMemory chatMemory(){
        MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
                .maxMessages(20)
                .build();
        return memory;
    }
    @Bean
    public ChatMemoryProvider chatMemoryProvider(){
        ChatMemoryProvider chatMemoryProvider = new ChatMemoryProvider() {
            @Override
            public ChatMemory get(Object memoryId) {
                return MessageWindowChatMemory.builder()
                        .id(memoryId)
                        .maxMessages(20)
                        .chatMemoryStore(redisChatMemoryStore)
                        .build();
            }
        };
        return chatMemoryProvider;
    }
    //构建向量数据库操作对象
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {

        // 加载 content 目录
        List<Document> documents =
                ClassPathDocumentLoader.loadDocuments("content");

        // 内存向量库
        InMemoryEmbeddingStore<TextSegment> store =
                new InMemoryEmbeddingStore<>();

        // 创建 ingest 对象
        EmbeddingStoreIngestor ingestor =
                EmbeddingStoreIngestor.builder()
                        .embeddingModel(embeddingModel)
                        .embeddingStore(store)
                        .build();

        // 使用当前 ingestor
        ingestor.ingest(documents);

        return store;
    }

    // 3. 构建向量数据库检索对象，供 AiService 使用
//    @Bean("contentRetriever")
//    public ContentRetriever contentRetriever(EmbeddingStore<TextSegment> store) {
//        return EmbeddingStoreContentRetriever.builder()
//                .embeddingStore(store)
//                .minScore(0.6) // 稍微提高匹配阈值，确保遥感分析的准确性
//                .maxResults(3) // 每次检索前3条相关背景
//                .build();
//    }
    @Bean("contentRetriever")
    public ContentRetriever contentRetriever(EmbeddingStore<TextSegment> store) {
        // 创建真实的检索器
        ContentRetriever realRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(store)
                .minScore(0.5)
                .maxResults(3)
                .embeddingModel(embeddingModel)
                .build();

        // 返回一个包装后的检索器：根据开关决定返回内容还是空集合
        return query -> {
            if (enableRAG) {
                return realRetriever.retrieve(query);
            } else {
                return Collections.emptyList(); // 关掉 RAG 时，返回空
            }
        };
    }
}
