package com.xmut.shop.controller;

import com.xmut.shop.DTO.RagTestCase;
import com.xmut.shop.Utils.EvaluationUtils;
import com.xmut.shop.Utils.SpatialSemanticEvaluationUtils;
import com.xmut.shop.agent.WebGisAgent;
import com.xmut.shop.agent.WebGisAgentNoRAG;
import com.xmut.shop.common.CommonConfig;
import com.xmut.shop.service.RfAreaService;
import dev.langchain4j.model.embedding.EmbeddingModel;
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
    @Autowired
    private RfAreaService rfAreaService;
    @Autowired
    private EmbeddingModel embeddingModel;
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

    @Test
    public void evaluateSpatialSemanticFusion() {
        // 1. 模拟用户输入
        String message = "临洪河口附近入侵的互花米草面积有多大，有什么政策影响？";

        // 2. 设立物理世界与非结构化知识的 Ground Truth
        Double trueAreaM2 = rfAreaService.calculatePolygonIntersectsArea("临洪河口", 1);
        double trueAreaHm2 = (trueAreaM2 != null) ? trueAreaM2 / 10000.0 : 145.22;

        // 期待知识库的核心表达（模型会用 Embedding 自动做语义泛化对齐）
        String groundTruthPolicyText = "互花米草属于外来入侵物种，会威胁本地盐沼植被芦苇和碱蓬，需要物理防除和生态修复";

        // 3. 运行【实验组 (Spatial-RAG 开启)】
        String experimentalResponse = webGisAgent.chat("exp-session-" + System.currentTimeMillis(), message)
                .collectList().map(list -> String.join("", list)).block();

        double expArea = SpatialSemanticEvaluationUtils.extractAreaFromText(experimentalResponse);
        double sSpatialExp = SpatialSemanticEvaluationUtils.calculateSpatialScore(expArea, trueAreaHm2);

        // 💡 传入 embeddingModel，利用大模型做真正的深度向量余弦相似度计算
        double sSemanticExp = SpatialSemanticEvaluationUtils.calculateSemanticScore(experimentalResponse, groundTruthPolicyText, embeddingModel);
        double fExp = 0.5 * sSemanticExp + 0.5 * sSpatialExp;

        // 4. 运行【对照组 (No-RAG 纯大模型)】
        String contrastResponse = webGisAgentNoRAG.chat("norag-session-" + System.currentTimeMillis(), message)
                .collectList().map(list -> String.join("", list)).block();

        double conArea = SpatialSemanticEvaluationUtils.extractAreaFromText(contrastResponse);
        double sSpatialCon = SpatialSemanticEvaluationUtils.calculateSpatialScore(conArea, trueAreaHm2);

        // 💡 对照组同样使用深度模型向量化计算
        double sSemanticCon = SpatialSemanticEvaluationUtils.calculateSemanticScore(contrastResponse, groundTruthPolicyText, embeddingModel);
        double fCon = 0.5 * sSemanticCon + 0.5 * sSpatialCon;

        // 5. 打印对比实验结果表格
        System.out.println("\n=======================================================================");
        System.out.println("   基于深度 Embedding 向量空间的多目标平衡函数 F(x,y) 定量消融实验结果     ");
        System.out.println("=======================================================================");
        System.out.println("【真实物理标准 (Ground Truth)】: 临洪河口区域互花米草真实面积 = " + trueAreaHm2 + " 公顷");
        System.out.println("-----------------------------------------------------------------------");
        System.out.printf("%-10s | %-16s | %-16s | %-16s\n", "评估组别", "模型语义得分(S_sem)", "空间拓扑(S_spa)", "联合协同函数 F(x,y)");
        System.out.println("-----------------------------------------------------------------------");
        System.out.printf("%-10s | %-18.4f | %-18.4f | %-18.4f\n", "实验组(RAG ON)", sSemanticExp, sSpatialExp, fExp);
        System.out.printf("%-10s | %-18.4f | %-18.4f | %-18.4f\n", "对照组(NoRAG)", sSemanticCon, sSpatialCon, fCon);
        System.out.println("=======================================================================");
    }
}
