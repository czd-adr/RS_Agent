-- ----------------------------
-- 1. 创建 RS-AGENT 工具集注册表 (Tool Registry)
-- ----------------------------
DROP TABLE IF EXISTS "sys_agent_tool" CASCADE;
CREATE TABLE "sys_agent_tool" (
  "id" BIGSERIAL PRIMARY KEY,
  "tool_code" VARCHAR(64) NOT NULL UNIQUE,          -- 工具唯一标识，例如：toggle_layer, trigger_inspector
  "tool_name" VARCHAR(100) NOT NULL,         -- 工具显示名称，例如：图层显隐切换
  "category" VARCHAR(50) DEFAULT 'GIS_CLIENT', -- 工具分类：GIS_CLIENT(前端工具), ALGORITHM(后端算法), RAG(知识库)
  "description" TEXT,                        -- 工具描述（给大模型 Prompt 读取的意图说明）
  "parameters_schema" JSONB DEFAULT '{}'::jsonb, -- 核心：用 JSONB 存储异构参数的 Schema 定义
  "is_enabled" SMALLINT DEFAULT 1,           -- 状态：1-启用，0-禁用
  "created_at" TIMESTAMP(6) WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
  "updated_at" TIMESTAMP(6) WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 添加表与字段注释
COMMENT ON TABLE "sys_agent_tool" IS 'RS-AGENT 智能体动态工具注册表';
COMMENT ON COLUMN "sys_agent_tool"."tool_code" IS '工具唯一编码 (对应前端/后端执行函数名)';
COMMENT ON COLUMN "sys_agent_tool"."tool_name" IS '工具中文名称';
COMMENT ON COLUMN "sys_agent_tool"."category" IS '工具大类';
COMMENT ON COLUMN "sys_agent_tool"."description" IS '大模型意图匹配描述';
COMMENT ON COLUMN "sys_agent_tool"."parameters_schema" IS '异构参数的 JSON Schema 结构定义';

-- 创建索引以加速大模型匹配检索
CREATE INDEX "idx_tool_code" ON "sys_agent_tool" ("tool_code");
CREATE INDEX "idx_tool_enabled" ON "sys_agent_tool" ("is_enabled");


-- ----------------------------
-- 2. 预置初始化数据：将你的 WebGIS 前端工具注册进数据库
-- ----------------------------

-- 工具 A：图层显隐切换（入参：图层名字符串 + 可见性布尔值）
INSERT INTO "sys_agent_tool" ("tool_code", "tool_name", "category", "description", "parameters_schema")
VALUES (
  'toggle_layer',
  '图层显隐切换',
  'GIS_CLIENT',
  '控制地图中 WMS 图层（如 628yangshi 或分类图层）的显示或隐藏状态。',
  '{
    "type": "object",
    "properties": {
      "layerName": { "type": "string", "description": "图层唯一标识名" },
      "visible": { "type": "boolean", "description": "true表示显示，false表示隐藏" }
    },
    "required": ["layerName", "visible"]
  }'::jsonb
);

-- 工具 B：地图像元点查（无复杂入参，仅触发前端交互状态）
INSERT INTO "sys_agent_tool" ("tool_code", "tool_name", "category", "description", "parameters_schema")
VALUES (
  'trigger_inspector',
  '像元多波段点查',
  'GIS_CLIENT',
  '当用户想要点击地图查询某一点的 NDVI、DEM、VV、VH 等多波段数值时调用此工具。',
  '{
    "type": "object",
    "properties": {},
    "required": []
  }'::jsonb
);

-- 工具 C：区域 ROI 绘制分析（入参：绘制模式）
INSERT INTO "sys_agent_tool" ("tool_code", "tool_name", "category", "description", "parameters_schema")
VALUES (
  'trigger_draw_roi',
  'ROI 区域框选绘制',
  'GIS_CLIENT',
  '当用户希望在地图上画框或绘制多边形来圈定特定监测区域时调用。',
  '{
    "type": "object",
    "properties": {
      "mode": { "type": "string", "enum": ["polygon", "rectangle"], "description": "绘制几何类型" }
    },
    "required": []
  }'::jsonb
);

-- 工具 D：地图视角平移聚焦 (Fit Bounds)（入参：空间边界数组）
INSERT INTO "sys_agent_tool" ("tool_code", "tool_name", "category", "description", "parameters_schema")
VALUES (
  'fly_to_extent',
  '地图视角聚焦',
  'GIS_CLIENT',
  '当大模型需要将地图视窗平移并缩放到某个特定的地理坐标范围时调用。',
  '{
    "type": "object",
    "properties": {
      "extent": {
        "type": "array",
        "items": { "type": "number" },
        "description": "地理范围边界数组 [minLon, minLat, maxLon, maxLat]"
      }
    },
    "required": ["extent"]
  }'::jsonb
);
