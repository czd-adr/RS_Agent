package com.xmut.shop.Utils;

import java.util.List;

public class EvaluationUtils {
    /**
     * 计算专业术语提取率
     * @param output 模型生成的回答全文
     * @param groundTruthTerms 预设的标准术语列表
     * @return Recall 分值 (0-1)
     */
    public static double calculateTermRecall(String output, List<String> groundTruthTerms) {
        if (groundTruthTerms == null || groundTruthTerms.isEmpty()) return 0.0;

        long matchedCount = groundTruthTerms.stream()
                .filter(term -> output.contains(term)) // 检查术语是否出现在回答中
                .count();

        return (double) matchedCount / groundTruthTerms.size();
    }
}