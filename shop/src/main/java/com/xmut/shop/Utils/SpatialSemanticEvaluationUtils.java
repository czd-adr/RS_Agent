package com.xmut.shop.Utils;


import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpatialSemanticEvaluationUtils {

    /**
     * 1. 真正的深度语义相似度计算 S_semantic（基于大模型 Embedding 向量空间）
     * 原理：利用 LangChain4j 的 EmbeddingModel 将两段文本转化为深度稠密向量，
     * 再通过标准余弦相似度算法计算它们在多维语义空间中的夹角。
     *
     * @param response        Agent 实际生成的回答内容
     * @param groundTruthText 知识库里预设的标准参考文本
     * @param embeddingModel  从 Spring 容器中注入的 Embedding 模型
     * @return S_semantic 语义得分 [0, 1]
     */
    public static double calculateSemanticScore(String response, String groundTruthText, EmbeddingModel embeddingModel) {
        if (response == null || groundTruthText == null || response.trim().isEmpty() || groundTruthText.trim().isEmpty()) {
            return 0.0;
        }

        // 利用你系统配置的嵌入模型，将自然语言文本转化为标准的 float[] 高维向量
        Embedding embedding1 = embeddingModel.embed(response).content();
        Embedding embedding2 = embeddingModel.embed(groundTruthText).content();

        float[] vector1 = embedding1.vector();
        float[] vector2 = embedding2.vector();

        // 调用你提供的标准高维向量余弦相似度算法
        return cosineSimilarity(vector1, vector2);
    }

    /**
     * 用户提供的标准高维向量余弦相似度计算核心算法（保持高保真数值流）
     */
    public static double cosineSimilarity(float[] v1, float[] v2) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < v1.length; i++) {
            dotProduct += v1[i] * v2[i];
            normA += Math.pow(v1[i], 2);
            normB += Math.pow(v2[i], 2);
        }
        if (normA == 0.0 || normB == 0.0) return 0.0; // 避免除以 0
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * 2. 计算空间拓扑得分 S_spatial
     */
    public static double calculateSpatialScore(double responseArea, double postGisTrueArea) {
        if (postGisTrueArea <= 0) return 0.0;
        double resa = Math.abs(responseArea - postGisTrueArea) / postGisTrueArea;
        return 1.0 / (1.0 + resa);
    }

    /**
     * 3. 辅助正则算子：提取文本中的“公顷”数字
     */
    public static double extractAreaFromText(String text) {
        try {
            Pattern pattern = Pattern.compile("(\\d+(\\.\\d+)?)\\s*公顷");
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return Double.parseDouble(matcher.group(1));
            }
        } catch (Exception e) {
            System.err.println("提取文本面积失败: " + e.getMessage());
        }
        return 0.0;
    }
    // 新增：关键词命中率
    public static double calculateKeywordHitScore(
            String response,
            List<String> keywords
    ) {

        if (response == null || response.trim().isEmpty()) {
            return 0.0;
        }

        int hit = 0;

        for (String keyword : keywords) {

            if (response.contains(keyword)) {
                hit++;
            }
        }

        return (double) hit / keywords.size();
    }
}
