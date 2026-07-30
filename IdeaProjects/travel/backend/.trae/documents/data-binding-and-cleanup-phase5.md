# 前后端数据流绑定 - 第五轮：冗余代码清理 + 剩余 Mock 替换

## Context

经过前四轮工作，前后端数据流大部分已连通。本轮聚焦：删除前端死代码、替换剩余 Mock 数据、删除后端零引用 Mapper。

## 需要修复的问题

| # | 问题 | 位置 | 处理方式 |
|---|---|---|---|
| 1 | `handleApplyOptimization` 死代码 | App.vue L1400-1409 | 删除，模板直接调用 `applyOptimization` |
| 2 | `RECOMMENDATIONS` 3条硬编码 | App.vue L576-604 | 改为 ref([])，onMounted 调 `attractionApi.getAttractions()` |
| 3 | `DESTINATIONS` 4条硬编码 | App.vue L606-640 | 改为 ref([])，同上 |
| 4 | `isCollected: false` 硬编码 | App.vue onMounted | 加载后调 `collectionApi.checkCollected` |
| 5 | `RouteAttractionMapMapper` 零引用 | backend mapper/ | 删除 |
| 6 | `FileCommentMapper` 零引用 | backend mapper/ | 删除 |

## 实施步骤

### 步骤1：删除 handleApplyOptimization 死代码
文件：frontend/src/App.vue，删除 L1400-1409。模板 L3910 直接调用 applyOptimization，此函数从未被模板调用。

### 步骤2：替换 RECOMMENDATIONS 和 DESTINATIONS 真实数据
两个常量改为 ref([])，在 onMounted 中调用 attractionApi.getAttractions() 加载，map 到模板期望字段。

### 步骤3：修复 isCollected 硬编码
onMounted 中 PLANNED_ITINERARIES 加载后，对每条路线异步调用 collectionApi.checkCollected 获取真实收藏状态。

### 步骤4：删除后端零引用 Mapper
删除 RouteAttractionMapMapper.java 和 FileCommentMapper.java（grep 确认零引用），运行 mvn compile 验证。

## 验证
1. npm run lint 通过
2. mvn compile 通过
3. 联调验证景点数据、收藏状态