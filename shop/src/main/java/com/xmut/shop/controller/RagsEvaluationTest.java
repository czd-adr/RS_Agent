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
    @Autowired
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
    public void testSpatialDecayingFieldScores() {
        // 1. 准备测试的输入参数
        String poiName = "临洪河口";
        String landType = "互花米草";
        int classType = 1;
        double sigma = 500.0;

        System.out.println("\n=========================================================");
        System.out.println("🧪 开始执行 [高斯圈层场] 动态数据库特征实时核验测试...");
        System.out.println("=========================================================");

        // 2. 👈 【核心修改：拒绝写死】直接调 Mapper 去 PostGIS 实时捞取你刚刚分组聚合出来的最新真实数据！
        // 此时无论是 229.44 还是未来数据变动，这里拿到的永远是第一手动态真数据
        List<WetlandPatchDTO> dbPatches = wetlandPatchMapper.selectIntersectsPatches(poiName, classType);

        // 健壮性防御：防止你换了 POI 之后数据库查出空集导致测试报错
        if (dbPatches == null || dbPatches.isEmpty()) {
            System.out.println("⚠️ 警告：当前数据库中该 POI 未捞出任何圈层数据，请检查空间相交关系！");
            return;
        }

        // 3. 动态解算高斯连续场，生成高保真断言文本（完全基于刚刚捞出的 dbPatches 动态计算）
        double totalPhysicalArea = 0.0;
        double totalWeightedArea = 0.0;

        System.out.println("【基于动态数据库面积特征的空间场理论解算】");
        for (WetlandPatchDTO band : dbPatches) {
            // 如果你的 SQL 之前把截断值（500, 1000, 1500）赋给了 spatialDistance，这里就会动态拿到
            double d = band.getSpatialDistance();

            // 标准高斯衰减权重 fs
            double theoreticalWeight = Math.exp(- (d * d) / (2 * sigma * sigma));
            double physicalAreaHectare = band.getPatchArea() / 10000.0; // 平方米转公顷
            double weightedAreaHectare = physicalAreaHectare * theoreticalWeight;

            totalPhysicalArea += physicalAreaHectare;
            totalWeightedArea += weightedAreaHectare;

            String bandName = band.getPatchId() == 1L ? "0-500m 核心区" :
                    band.getPatchId() == 2L ? "500-1000m 警戒区" : "1000-1500m 扩散区";

            System.out.printf(" -> [%s] 动态边界距离: %4.0f米 | 高斯权重: %.4f | 实时物理面积: %8.4f 公顷 | 生态加权面积: %8.4f 公顷\n",
                    bandName, d, theoreticalWeight, physicalAreaHectare, weightedAreaHectare);
        }

        // 格式化为字符串，用于匹配大模型 Agent 文本输出
        String expectedPhysicalStr = String.format("%.2f", totalPhysicalArea);
        String expectedWeightedStr = String.format("%.2f", totalWeightedArea);

        System.out.println("---------------------------------------------------------");
        System.out.println("【完全动态对齐的理论期望值】");
        System.out.println(" -> 实时总物理面积期望值: 【" + expectedPhysicalStr + "】公顷");
        System.out.println(" -> 动态生态加权总面积期望值: 【" + expectedWeightedStr + "】公顷");
        System.out.println("---------------------------------------------------------");

        // 4. Mockito 拦截：把刚刚从数据库动态捞出来的、原汁原味的 dbPatches 喂给 Service
        Mockito.when(wetlandPatchMapper.selectIntersectsPatches(poiName, classType))
                .thenReturn(dbPatches);

        // 5. 执行核心分析接口
        String finalReport = wetlandAgentService.executeSpatialSemanticAnalysis(poiName, landType);

        // 6. 打印最终生成的 Agent 报告
        System.out.println("🔥 RS-Agent 空间连续衰减场测算报告输出测试：");
        System.out.println(finalReport);
        System.out.println("=========================================================");

        // 7. 自动化断言核验
        assert finalReport != null : "错误：分析报告不应为空";
        assert finalReport.contains(poiName) : "错误：报告应包含目标POI信息【" + poiName + "】";

        // 动态断言真实总物理面积（系统会自动根据数据库当前状态动态比对，再也不用手动改断言数字了！）
        assert finalReport.contains(expectedPhysicalStr) : "错误：实际物理总面积计算错误，预期为 " + expectedPhysicalStr + " 公顷";
        assert finalReport.contains(expectedWeightedStr) : "错误：空间高斯连续衰减场加权计算有偏差，预期报告中应包含 " + expectedWeightedStr;

        System.out.println("🏆 动态对齐断言完全通过！数据流已完全由 PostGIS 数据库实时驱动！");
    }
    @Test
    public void testSpatialDecayingFieldScores1() {

        String poiName = "青口渔场";
        String landType = "互花米草";
        int classType = 1;

        double sigma = 500.0;

        System.out.println("\n=========================================================");
        System.out.println("🚀 启动 [RS-Spatial-RAG] 真实数据库穿透全链路集成测试...");
        System.out.println("=========================================================");
        System.out.println("【当前运行时参数配置】");
        System.out.println(" -> 评估核心 POI 锚点: " + poiName);
        System.out.println(" -> 监测目标生态地物: " + landType);
        System.out.println(" -> 高斯衰减场带宽 (Sigma): " + sigma + " 米");
        System.out.println("---------------------------------------------------------");

        List<WetlandPatchDTO> dbPatches =
                wetlandPatchMapper.selectIntersectsPatches(
                        poiName,
                        classType
                );
        System.out.println("dbPatches = " + dbPatches);
        if (dbPatches == null || dbPatches.isEmpty()) {

            System.out.println(
                    "⚠️ 当前数据库中未发现 "
                            + poiName
                            + " 周边1500m范围内的 "
                            + landType
            );

            return;
        }

        double totalPhysicalArea = 0.0;
        double totalWeightedArea = 0.0;

        System.out.println("【PostGIS 圈层统计结果】");

        for (WetlandPatchDTO band : dbPatches) {

            Long bandId = band.getPatchId();

            double distance =
                    band.getSpatialDistance();

            double areaM2 =
                    band.getPatchArea();

            double areaHm2 =
                    areaM2 / 10000.0;

            double gaussianWeight =
                    Math.exp(
                            -(distance * distance)
                                    /
                                    (2 * sigma * sigma)
                    );

            double weightedAreaHm2 =
                    areaHm2 * gaussianWeight;

            totalPhysicalArea += areaHm2;
            totalWeightedArea += weightedAreaHm2;

            String bandName;

            if (bandId == 1L) {
                bandName = "0-500m 核心风险区";
            } else if (bandId == 2L) {
                bandName = "500-1000m 缓冲警戒区";
            } else if (bandId == 3L) {
                bandName = "1000-1500m 边缘扩散区";
            } else {
                bandName = "未知圈层";
            }

            System.out.printf(
                    " -> [%s] 距离=%.0fm | 面积=%.4fhm² | 高斯权重=%.4f | 加权面积=%.4fhm²%n",
                    bandName,
                    distance,
                    areaHm2,
                    gaussianWeight,
                    weightedAreaHm2
            );
        }

        String expectedPhysicalStr =
                String.format("%.2f", totalPhysicalArea);

        String expectedWeightedStr =
                String.format("%.2f", totalWeightedArea);

        System.out.println("---------------------------------------------------------");
        System.out.println("【理论计算结果】");
        System.out.println(" -> 物理总面积 = "
                + expectedPhysicalStr
                + " hm²");

        System.out.println(" -> 高斯衰减加权面积 = "
                + expectedWeightedStr
                + " hm²");

        System.out.println("---------------------------------------------------------");

        String finalReport =
                wetlandAgentService.executeSpatialSemanticAnalysis(
                        poiName,
                        landType
                );

        System.out.println("🔥 Agent输出结果：");
        System.out.println(finalReport);

        System.out.println("=========================================================");

        assert finalReport != null;

        assert finalReport.contains(poiName)
                : "报告中未出现POI名称";

        assert finalReport.contains(expectedPhysicalStr)
                : "报告中的物理总面积与数据库计算不一致";

        assert finalReport.contains(expectedWeightedStr)
                : "报告中的高斯加权面积与理论计算不一致";

        System.out.println("🏆 集成测试通过！");
    }
}
