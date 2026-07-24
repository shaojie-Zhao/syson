# DoDAF 时序图元素样式修改指南

## 一、修改内容概览

| 功能 | 文件 | 状态 |
|------|------|------|
| 《Lifeline》标签 | `MultiLineLabelSwitch.java` | ✅ |
| 《CombinedFragment》标签 | `MultiLineLabelSwitch.java` | ✅ |
| 《StateInvariant》标签 | `MultiLineLabelSwitch.java` | ✅ |
| Lifeline aliasIds 自动设置 | `SysMLv2EditService.java` | ✅ |
| Lifeline 虚线尾（UML 样式） | `index.html` (JS注入) | ✅ |
| Lifeline 自定义图标 | `lifeline.svg` | ⚠️ 已创建但未启用 |
| Lifeline 自定义节点描述 | `LifelineNodeDescriptionProvider.java` | ⚠️ 已创建但未生效 |

---

## 二、标签映射

### 文件：`backend/services/syson-diagram-services/src/main/java/org/eclipse/syson/diagram/services/utils/MultiLineLabelSwitch.java`

**方法**：`getDodafStereotype(Element element)`

新增三个映射：

```java
} else if (element.getAliasIds().contains("dodaf:Lifeline")) {
    return "Lifeline";
} else if (element.getAliasIds().contains("dodaf:CombinedFragment")) {
    return "CombinedFragment";
} else if (element.getAliasIds().contains("dodaf:StateInvariant")) {
    return "StateInvariant";
```

> **二次开发**：添加新元素标签时，在此方法中仿照添加 `dodaf:XXX` → `"XXX"` 映射。

---

## 三、别名自动设置

### 文件：`backend/application/syson-application-configuration/src/main/java/org/eclipse/syson/application/services/SysMLv2EditService.java`

**方法**：`createChild()` 中约第218行

通过 `childCreationDescriptionId.contains("XXX")` 判断创建的元素类型，自动设置 `aliasIds`。

当前支持的序列图元素：
```
Lifeline        → "dodaf:Lifeline"
CombinedFragment → "dodaf:CombinedFragment"
StateInvariant   → "dodaf:StateInvariant"
```

> **二次开发**：在 `if` 条件中添加新元素判断即可。

### 别名匹配前置条件

`isLifeline()` 等方法：

- **AQL层**：`backend/services/syson-model-services/src/main/java/org/eclipse/syson/model/services/aql/ModelQueryAQLService.java`
- **元模型层**：`backend/services/syson-sysml-metamodel-services/src/main/java/org/eclipse/syson/sysml/metamodel/services/MetamodelQueryElementService.java`

---

## 四、UML 生命线虚线尾

### 文件：`frontend/syson/index.html`

**JS 函数**：`styleLifelines()`（约第920行）

实现原理：
1. 轮询查找 `data-svg="rect"` 元素
2. 检测 label 包含 `"Lifeline"` 的节点
3. 动态注入 `<div>` 作为虚线尾（`border-left: 2px dashed #666`）
4. 高度 = `paneRect.bottom - nodeRect.bottom`（自动延伸至视图底部）
5. 每300ms刷新以适应缩放/滚动

> **二次开发**：可直接修改此函数调整虚线样式、颜色、间隔等。

---

## 五、自定义图标

### 文件：`backend/views/syson-standard-diagrams-view/src/main/resources/images/lifeline.svg`

一个简单的 UML 生命线 SVG 图标（矩形头+虚线尾）。

> **注意**：由于 Sirius Web 的 `ViewDiagramDescriptionConverter` 节点匹配算法优先选择标准 `PartDefinition` 节点，自定义 `LifelineNodeDescriptionProvider` 目前**未生效**。

---

## 六、已知问题

### 自定义节点描述未生效

**文件**：`backend/views/syson-standard-diagrams-view/src/main/java/org/eclipse/syson/standard/diagrams/view/nodes/LifelineNodeDescriptionProvider.java`

**根因**：Sirius Web 的 `ViewDiagramDescriptionConverter` 在 `preconditionExpression` 匹配时优先选择列表中首个匹配节点。标准 `PartDefinition` 节点在列表中位置靠前且无条件，自定义节点被跳过。

**可能的解决方案**（待验证）：
1. 修改 `SDVDiagramDescriptionProvider` 将自定义节点插入列表**前端**而非尾部
2. 使用 `FakeNodeDescriptionProvider` 拦截 Lifeline 元素
3. 修改标准节点 `semanticCandidatesExpression` 排除 `dodaf:Lifeline` 别名元素

---

## 七、完整文件清单

```
# 标签映射
backend/services/syson-diagram-services/src/main/java/org/eclipse/syson/diagram/services/utils/MultiLineLabelSwitch.java

# 别名自动设置
backend/application/syson-application-configuration/src/main/java/org/eclipse/syson/application/services/SysMLv2EditService.java

# AQL 方法
backend/services/syson-model-services/src/main/java/org/eclipse/syson/model/services/aql/ModelQueryAQLService.java
backend/services/syson-sysml-metamodel-services/src/main/java/org/eclipse/syson/sysml/metamodel/services/MetamodelQueryElementService.java

# 节点描述（未生效）
backend/views/syson-standard-diagrams-view/src/main/java/org/eclipse/syson/standard/diagrams/view/nodes/LifelineNodeDescriptionProvider.java
backend/views/syson-standard-diagrams-view/src/main/java/org/eclipse/syson/standard/diagrams/view/SDVDiagramDescriptionProvider.java

# 前端
frontend/syson/index.html (styleLifelines 函数)
frontend/syson/src/views/DoDAFPredictionView.tsx
frontend/syson/src/dodaf-views.css

# 图标资源
backend/views/syson-standard-diagrams-view/src/main/resources/images/lifeline.svg
```
