package com.xmut.shop.controller;

import com.xmut.shop.DTO.RagTestCase;
import com.xmut.shop.DTO.WetlandPatchDTO;
import com.xmut.shop.Utils.EvaluationUtils;
import com.xmut.shop.Utils.SpatialSemanticEvaluationUtils;
import com.xmut.shop.agent.WebGisAgent;
import com.xmut.shop.agent.WebGisAgentNoRAG;
import com.xmut.shop.common.CommonConfig;
import com.xmut.shop.mapper.WetlandPatchMapper;
import com.xmut.shop.service.RfAreaService;
import com.xmut.shop.service.WetlandAgentService;
import dev.langchain4j.model.embedding.EmbeddingModel;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SpringBootTest // 关键：启动完整的 Spring 上下文，这样 @Autowired 才会生效
public class RagsEvaluationTest {

    @Autowired
    @Qualifier("webGisAgent")
    private WebGisAgent webGisAgent;
    @Autowired
    @Qualifier("webGisAgentNoRAG")
    private WebGisAgentNoRAG webGisAgentNoRAG;
    @Autowired
    private RfAreaService rfAreaService;
    @Autowired
    private EmbeddingModel embeddingModel;
    @Resource
    private WetlandAgentService wetlandAgentService;

    // 使用 MockBean 模拟数据库查询，这样不需要依赖真实的数据库和 PostGIS 环境就能进行纯纯的数学机理测试
    @MockBean
    private WetlandPatchMapper wetlandPatchMapper;
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


    @Test
    public void evaluateSpatialSemanticFusion2() {
        // 1. 模拟用户输入
        String baseMessage = "连云港附近入侵的互花米草对水鸟栖息地有什么影响，有什么具体的生态修复策略和政策行动计划？";

        // 联动配置开关：如果是对照组（NoRAG），追加严苛提示词，逼大模型老实说不知道
        String finalMessage = CommonConfig.enableRAG
                ? baseMessage
                : baseMessage + "（警告：如果你无法通过知识库检索到确切的地理观测参数、具体生态修复行动计划或地方特定工程工艺，请直接老实回答无法获取，绝对不能凭空编造数字与政策。）";

        // 2. 强制先让大模型响应，切断当前线程在计算真实面积时的“数据先验泄露”
        String response = webGisAgent.chat("session-" + System.currentTimeMillis(), finalMessage)
                .collectList()
                .map(list -> String.join("", list))
                .block();

        // 3. 设立基于 PostGIS 的物理世界 Ground Truth (面积计算)
        Double trueAreaM2 = rfAreaService.calculatePolygonIntersectsArea("临洪河口", 1);
        double trueAreaHm2 = (trueAreaM2 != null) ? trueAreaM2 / 10000.0 : 145.22;

        // 4. 💡 重新提炼的【国家级政策依凭、物种功能群与工艺微地形】的结构化语义 Ground Truth
        String groundTruthPolicyText = "依据国家林草局等联合发布的《候鸟迁飞通道保护修复中国行动计划（2024—2030）》，"
                + "连云港赣榆区互花米草入侵面积占比高达94%，严重威胁半蹼鹬、黑脸琵鹭等IUCN全球受胁水鸟适宜生境。"
                + "修复策略必须以保护水鸟栖息地为目标，采用物理技术（刈割、深翻、旋耕）清除互花米草，"
                + "并通过水系塑造（控制宽深比>8）冲刷土壤盐分、构建‘光滩-浅水-稀疏草洲’（控制植被盖度25%～30%）三类微地形生境，"
                + "开展碱蓬、柽柳群落的生物替代与修复。";

        // 5. 指标定量测算
        double area = SpatialSemanticEvaluationUtils.extractAreaFromText(response);

        // 空间得分（空间拓扑衰减模型）
        double sSpatial = SpatialSemanticEvaluationUtils.calculateSpatialScore(area, trueAreaHm2);

        // 语义得分（高维向量空间余弦相似度）
        double sSemantic = SpatialSemanticEvaluationUtils.calculateSemanticScore(response, groundTruthPolicyText, embeddingModel);

        // 联合协同平衡函数 F(x,y)
        double f = 0.5 * sSemantic + 0.5 * sSpatial;

        // 6. 打印符合论文发表标准的规范化表格
        String currentGroupName = CommonConfig.enableRAG ? "实验组 (RAG ON)" : "对照组 (NoRAG)";
        System.out.println("\n=======================================================================");
        System.out.println("   基于深度 Embedding 向量空间的多目标平衡函数 F(x,y) 定量消融实验结果     ");
        System.out.println("=======================================================================");
        System.out.println("【当前测算组别】: " + currentGroupName);
        System.out.println("【真实物理面积 (PostGIS GT)】: " + trueAreaHm2 + " 公顷");
        System.out.println("【模型响应面积 (Extracted)】  : " + area + " 公顷");
        System.out.println("-----------------------------------------------------------------------");
        System.out.printf("%-12s | %-16s | %-16s | %-16s\n", "评估组别", "模型语义得分(S_sem)", "空间拓扑(S_spa)", "联合协同函数 F(x,y)");
        System.out.println("-----------------------------------------------------------------------");
        System.out.printf("%-12s | %-18.4f | %-18.4f | %-18.4f\n", currentGroupName, sSemantic, sSpatial, f);
        System.out.println("=======================================================================");
        System.out.println("【模型响应原始文本】：\n" + response);
        System.out.println("=======================================================================\n");
    }
    @Test
    public void runControlGroupTest() {//语义分析

        // =========================================================
        // 1. 对照组问题（空知识库）
        // =========================================================
        String baseQuery =
                "连云港赣榆青口河口水鸟栖息地修复中，"
                        + "浅水区的目标水深范围是多少？"
                        + "光滩面积是多少？"
                        + "稀疏草洲植被盖度控制在什么范围？"
                        + "潮沟宽深比要求是多少？"
                        + "并说明对应的水鸟生态意义。";

        // 防止 NoRAG 胡编
        String controlQuery =
                baseQuery
                        + "（如果无法通过外部知识库或工具获得精确参数，"
                        + "请直接说明无法获取，不允许主观猜测。）";

        String sessionId =
                "SESSION_CONTROL_" + System.currentTimeMillis();

        System.out.println("\n==============================================================");
        System.out.println("==============================================================");

        // =========================================================
        // 2. 获取模型响应
        // =========================================================
        String response = webGisAgent.chat(sessionId, controlQuery)
                .collectList()
                .map(list -> String.join("", list))
                .block();

        // =========================================================
        // 3. Ground Truth
        // =========================================================
        String groundTruth =
                "浅水生境总面积约15.46hm2，"
                        + "浅水区水深范围控制在0到25cm之间，"
                        + "光滩面积约7.61hm2，"
                        + "稀疏草洲植被盖度控制在25%到30%，"
                        + "潮沟宽深比需大于8，"
                        + "并通过构建光滩、浅水和稀疏草洲三类生境，"
                        + "满足鸻鹬类、鹭类和雁鸭类水鸟的觅食与栖息需求。";

        // =========================================================
        // 4. Embedding 语义得分
        // =========================================================
        double semanticScore =
                SpatialSemanticEvaluationUtils.calculateSemanticScore(
                        response,
                        groundTruth,
                        embeddingModel
                );

        // =========================================================
        // 5. 专属关键词命中率
        // =========================================================
        List<String> keywords = Arrays.asList(
                "15.46",
                "7.61",
                "25%到30%",
                "宽深比",
                "大于8",
                "0到25",
                "光滩",
                "浅水",
                "稀疏草洲",
                "鸻鹬类",
                "鹭类",
                "雁鸭类"
        );

        double keywordScore =
                SpatialSemanticEvaluationUtils.calculateKeywordHitScore(
                        response,
                        keywords
                );

        // =========================================================
        // 6. 联合语义函数
        // =========================================================
        double finalScore =
                0.5 * semanticScore
                        + 0.5 * keywordScore;

        // =========================================================
        // 7. 输出实验结果
        // =========================================================
        System.out.println("\n【Ground Truth】");
        System.out.println(groundTruth);

        System.out.println("\n--------------------------------------------------------------");
        System.out.println("【模型响应】");
        System.out.println(response);

        System.out.println("\n--------------------------------------------------------------");
        System.out.println("【Embedding语义得分 S_sem】");
        System.out.println(semanticScore);

        System.out.println("\n--------------------------------------------------------------");
        System.out.println("【关键词命中率 S_key】");
        System.out.println(keywordScore);

        System.out.println("\n--------------------------------------------------------------");
        System.out.println("【联合语义函数 F_sem】");
        System.out.println(finalScore);

        System.out.println("==============================================================");
    }
    @Test
    public void runExperimentGroupTest() {

        // =========================================================
        // 1. 实验组问题（真实 RAG 知识库）
        // =========================================================
        String query =
                "连云港赣榆青口河口水鸟栖息地修复中，"
                        + "浅水区的目标水深范围是多少？"
                        + "光滩面积是多少？"
                        + "稀疏草洲植被盖度控制在什么范围？"
                        + "潮沟宽深比要求是多少？"
                        + "并说明对应的水鸟生态意义。";

        String sessionId =
                "SESSION_EXPERIMENT_" + System.currentTimeMillis();

        System.out.println("\n==============================================================");
        System.out.println("              RAG 消融实验 —— 实验组（真实知识库）");
        System.out.println("==============================================================");

        // =========================================================
        // 2. 获取模型响应
        // =========================================================
        String response = webGisAgentNoRAG.chat(sessionId, query)
                .collectList()
                .map(list -> String.join("", list))
                .block();

        // =========================================================
        // 3. Ground Truth（高区分度专属知识）
        // =========================================================
        String groundTruth =
                "浅水生境总面积约15.46hm2，"
                        + "浅水区水深范围控制在0到25cm之间，"
                        + "光滩面积约7.61hm2，"
                        + "稀疏草洲植被盖度控制在25%到30%，"
                        + "潮沟宽深比需大于8，"
                        + "并通过构建光滩、浅水和稀疏草洲三类生境，"
                        + "满足鸻鹬类、鹭类和雁鸭类水鸟的觅食与栖息需求。";

        // =========================================================
        // 4. Embedding 语义相似度
        // =========================================================
        double semanticScore =
                SpatialSemanticEvaluationUtils.calculateSemanticScore(
                        response,
                        groundTruth,
                        embeddingModel
                );

        // =========================================================
        // 5. RAG 专属关键词命中率
        // =========================================================
        List<String> keywords = Arrays.asList(
                "15.46",
                "7.61",
                "25%到30%",
                "宽深比",
                "大于8",
                "0到25",
                "光滩",
                "浅水",
                "稀疏草洲",
                "鸻鹬类",
                "鹭类",
                "雁鸭类"
        );

        double keywordScore =
                SpatialSemanticEvaluationUtils.calculateKeywordHitScore(
                        response,
                        keywords
                );

        // =========================================================
        // 6. 联合语义函数
        // =========================================================
        double finalScore =
                0.5 * semanticScore
                        + 0.5 * keywordScore;

        // =========================================================
        // 7. 输出实验结果
        // =========================================================
        System.out.println("\n【Ground Truth】");
        System.out.println(groundTruth);

        System.out.println("\n--------------------------------------------------------------");
        System.out.println("【模型响应】");
        System.out.println(response);

        System.out.println("\n--------------------------------------------------------------");
        System.out.println("【Embedding语义得分 S_sem】");
        System.out.println(semanticScore);

        System.out.println("\n--------------------------------------------------------------");
        System.out.println("【关键词命中率 S_key】");
        System.out.println(keywordScore);

        System.out.println("\n--------------------------------------------------------------");
        System.out.println("【联合语义函数 F_sem】");
        System.out.println(finalScore);

        System.out.println("==============================================================");
    }
    @Test
    public void testSpatialDecayingFieldScores() {//高斯空间得分
        // 1. 准备测试的输入参数
        String poiName = "青口渔场";
        String landType = "互花米草";
        int classType = 1; // 对应互花米草的数据库 class

        // 2. 构造 3 个处于不同距离场的虚拟遥感图斑（假设面积均为 10000 平方米，即 1 公顷）
        List<WetlandPatchDTO> mockPatches = new ArrayList<>();

        // 图斑 A：刚好在边界上（距离 0 米） -> 理论得分应该接近 1.0
        WetlandPatchDTO patchA = new WetlandPatchDTO();
        patchA.setPatchId(101L);
        patchA.setPatchArea(10000.0);
        patchA.setSpatialDistance(0.0);
        mockPatches.add(patchA);

        // 图斑 B：处于警戒带宽上（距离 500 米 = 1 sigma） -> 理论得分应该等于 exp(-0.5) ≈ 0.606
        WetlandPatchDTO patchB = new WetlandPatchDTO();
        patchB.setPatchId(102L);
        patchB.setPatchArea(10000.0);
        patchB.setSpatialDistance(500.0);
        mockPatches.add(patchB);

        // 图斑 C：远离边界（距离 1500 米 = 3 sigma） -> 理论得分应该极低，接近 0
        WetlandPatchDTO patchC = new WetlandPatchDTO();
        patchC.setPatchId(103L);
        patchC.setPatchArea(10000.0);
        patchC.setSpatialDistance(1500.0);
        mockPatches.add(patchC);

        // 3. 拦截 Mapper 的底层数据库请求，使其返回我们精心设计的这 3 个不同距离的图斑
        Mockito.when(wetlandPatchMapper.selectIntersectsPatches(poiName, classType))
                .thenReturn(mockPatches);

        // 4. 执行你刚刚编写的工具集核心分析接口
        String finalReport = wetlandAgentService.executeSpatialSemanticAnalysis(poiName, landType);

        // 5. 打印测试报告到控制台，以便人工观察高斯场的输出是否漂亮
        System.out.println("=========================================================");
        System.out.println("🔥 RS-Agent 空间连续衰减场测算报告输出测试：");
        System.out.println(finalReport);
        System.out.println("=========================================================");

        // 6. 自动化断言：通过严密的数学逻辑验证连续场运行是否正确
        // 替代 assertNotNull
        assert finalReport != null : "分析报告不应为空";

// 替代 assertTrue
        assert finalReport.contains("青口渔场") : "报告应包含目标POI信息";

// 物理面积总和校验
        assert finalReport.contains("3.00") : "实际物理总面积计算错误";

// 核心数学机理校验
        assert finalReport.contains("1.62") : "空间高斯连续衰减场加权计算有偏差，请检查数学公式";
    }
}
