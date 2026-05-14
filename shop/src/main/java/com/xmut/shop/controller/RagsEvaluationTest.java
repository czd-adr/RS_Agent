package com.xmut.shop.controller;

import com.xmut.shop.DTO.RagTestCase;
import com.xmut.shop.Utils.EvaluationUtils;
import com.xmut.shop.agent.WebGisAgent;
import com.xmut.shop.agent.WebGisAgentNoRAG;
import com.xmut.shop.common.CommonConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SpringBootTest // 关键：启动完整的 Spring 上下文，这样 @Autowired 才会生效
public class RagsEvaluationTest {

    @Autowired
    private WebGisAgent webGisAgent;
    @Autowired
    private WebGisAgentNoRAG webGisAgentNoRAG;
    @Test
    public void evaluateRecall() {
        // 建议：测试前先清空或使用特定的 memoryId，避免历史干扰
        String memoryId = "test-session-1" + System.currentTimeMillis();
        String message = "这个研究区域的具体地理范围和坐标是多少？";

        // 这是你在 md 文档里定义的核心术语
        List<String> expectedTerms = Arrays.asList("119.162", "34.834", "119.313", "34.727", "矩形研究窗口");

        // 1. 获取完整回答（阻塞式获取，方便计算指标）
        String fullResponse = webGisAgent.chat(memoryId, message)
                .collectList()
                .map(list -> String.join("", list))
                .block();

        System.out.println("--- Agent 回答内容 ---");
        System.out.println(fullResponse);
        System.out.println("---------------------");

        // 2. 计算 Recall
        double recall = EvaluationUtils.calculateTermRecall(fullResponse, expectedTerms);

        System.out.println("📊 实验组专业术语提取率 (Recall): " + (recall * 100) + "%");
    }
    @Test
    public void evaluateContrast() {
        String memoryId = "test-session-NoRAG" + System.currentTimeMillis();
        String message = "这个研究区域的具体地理范围和坐标是多少？";

        // 这是你在 md 文档里定义的核心术语
        List<String> expectedTerms = Arrays.asList("119.162", "34.834", "119.313", "34.727", "矩形研究窗口");

        // 1. 获取完整回答（阻塞式获取，方便计算指标）
        String fullResponse = webGisAgentNoRAG.chat(memoryId, message)
                .collectList()
                .map(list -> String.join("", list))
                .block();

        System.out.println("--- Agent 回答内容 ---");
        System.out.println(fullResponse);
        System.out.println("---------------------");

        // 2. 计算 Recall
        double recall = EvaluationUtils.calculateTermRecall(fullResponse, expectedTerms);

        System.out.println("📊 对照组专业术语提取率 (Recall): " + (recall * 100) + "%");
    }
    @Test
    public void runFullExperiment() {
        String message = "这个研究区域的具体地理范围和坐标是多少？";
        List<String> expectedTerms = Arrays.asList("119.162", "34.834", "119.313", "34.727", "矩形研究窗口");

        // --- 实验组 ---
        CommonConfig.enableRAG = true;
        String resExp = webGisAgent.chat("exp-4" + System.currentTimeMillis(), message)
                .collectList().map(l -> String.join("", l)).block();
        double recallExp = EvaluationUtils.calculateTermRecall(resExp, expectedTerms);
        System.out.println("实验组 (RAG ON) Recall: " + (recallExp * 100) + "%");

        // --- 对照组 ---
        CommonConfig.enableRAG = false; // 一键关掉检索逻辑
        String resCtrl = webGisAgent.chat("ctrl-4" + System.currentTimeMillis(), message)
                .collectList().map(l -> String.join("", l)).block();
        double recallCtrl = EvaluationUtils.calculateTermRecall(resCtrl, expectedTerms);
        System.out.println("对照组 (RAG OFF) Recall: " + (recallCtrl * 100) + "%");
    }
    @Test
    public void runFullComprehensiveExperiment() {
        // 1. 初始化四个维度的实验数据
        List<RagTestCase> testSuite = new ArrayList<>();

        testSuite.add(new RagTestCase("地理概况",
                "介绍一下青口河口研究区的基本地理概况、面积以及潮汐特征。",
                Arrays.asList("青口河口", "100.39 hm2", "正规半日潮", "118°24′—119°48′E")));

        testSuite.add(new RagTestCase("环境问题",
                "目前赣榆区青口河口湿地面临的主要环境挑战有哪些？互花米草分布情况如何？",
                Arrays.asList("互花米草入侵", "占比94%", "生境均质化", "人为干扰")));

        testSuite.add(new RagTestCase("水鸟生境",
                "该研究区有哪些重点保护鸟类？针对不同水鸟规划了哪几种生境类型？",
                Arrays.asList("半蹼鹬", "97.5%", "鸻鹬类", "光滩", "稀疏草洲", "浅水")));

        testSuite.add(new RagTestCase("修复参数",
                "针对互花米草治理有哪些具体工程手段？相关深度和盖度参数是多少？",
                Arrays.asList("刈割", "深翻", "翻耕深度80 cm", "水深0～25 cm", "25%～30%")));

        // 2. 开始迭代实验
        System.out.println("==================== 连云港湿地 RAG 消融实验报表 ====================");
        System.out.printf("%-10s | %-15s | %-15s | %-10s%n", "维度", "实验组(RAG ON)", "对照组(RAG OFF)", "提升幅度");
        System.out.println("------------------------------------------------------------------");

        for (RagTestCase testCase : testSuite) {
            // --- 实验组测试 ---
            CommonConfig.enableRAG = true;
            String resExp = getFullResponse(testCase.getQuestion(), "exp-" + testCase.getDimension());
            double recallExp = EvaluationUtils.calculateTermRecall(resExp, testCase.getExpected());

            // --- 对照组测试 ---
            CommonConfig.enableRAG = false;
            String resCtrl = getFullResponse(testCase.getQuestion(), "ctrl-" + testCase.getDimension());
            double recallCtrl = EvaluationUtils.calculateTermRecall(resCtrl, testCase.getExpected());

            // 打印结果行
            double improvement = (recallExp - recallCtrl) * 100;
            System.out.printf("%-10s | %-18.2f%% | %-18.2f%% | %+.2f%%%n",
                    testCase.getDimension(), recallExp * 100, recallCtrl * 100, improvement);
        }
        System.out.println("==================================================================");
    }

    /**
     * 辅助方法：汇聚流式响应为完整字符串
     */
    private String getFullResponse(String message, String sessionSuffix) {
        String memoryId = "test-" + sessionSuffix + "-" + System.currentTimeMillis();
        return webGisAgent.chat(memoryId, message)
                .collectList()
                .map(list -> String.join("", list))
                .block();
    }
}
