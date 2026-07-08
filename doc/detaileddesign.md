# SysON DoDAF v2.0 详细设计文档

> 版本: 2026.5.0 | 日期: 2026-06-22 | 分支: feature/combine (基于 feature/chinese-localization + ToolSidebar 合并)

---

## 目录

1. [项目概述](#1-项目概述)
2. [系统架构](#2-系统架构)
3. [DoDAF 模板设计](#3-dodaf-模板设计)
4. [自定义节点样式系统](#4-自定义节点样式系统)
5. [衍型标签系统](#5-衍型标签系统)
6. [资源管理器图标](#6-资源管理器图标)
7. [ViewDefinition 与图表类型](#7-viewdefinition-与图表类型)
8. [渲染引擎设计](#8-渲染引擎设计)
9. [前端组件](#9-前端组件)
10. [数据持久化](#10-数据持久化)
11. [待办事项与未实现项](#11-待办事项与未实现项)
12. [表格/矩阵视图样式系统](#12-表格矩阵视图样式系统)
13. [文件清单](#13-文件清单)

---

## 1. 项目概述

### 1.1 背景

基于 SysON（Eclipse SysML v2 建模工具）源码，实现 DoDAF v2.0（Department of Defense Architecture Framework）体系架构模板的完整功能扩展。

### 1.2 需求范围

- DoDAF v2.0 完整 8 个视角、52 个模型的模板
- 5 种 DoDAF 元素类型的差异化节点样式（参考 UPDM 符号标准）
- 中文界面汉化（SysML 模型元素名称除外）
- 深色科技风格前端主题
- 7 种 ViewDefinition 图表类型（含 4 种自定义）
- 4 种渲染引擎（图表/表格/甘特图）

### 1.3 技术栈

| 层次 | 技术 |
|------|------|
| 后端框架 | Java 21 + Spring Boot 4.0.6 |
| 建模框架 | EMF (Eclipse Modeling Framework) |
| 图表引擎 | Sirius Web 2026.5.0 |
| 前端 | React 18 + TypeScript + Vite |
| UI 框架 | MUI (Material-UI) + React Flow |
| 数据库 | PostgreSQL 15 (Docker) |
| 国际化 | react-i18next + i18next-http-backend |
| 图表 DSL | Sirius View DSL (diagram/table/gantt) |

---

## 2. 系统架构

### 2.1 模块结构

```
syson/
├── backend/
│   ├── application/syson-application-configuration/  ← DoDAF 模板 & 初始化器
│   ├── metamodel/
│   │   ├── syson-sysml-metamodel/                    ← SysML 元模型
│   │   ├── syson-sysml-metamodel-edit/               ← EMF ItemProvider (资源管理器图标)
│   │   └── syson-siriusweb-customnodes-metamodel/    ← 自定义节点样式元模型
│   ├── services/
│   │   ├── syson-services/                           ← UtilService (DoDAF 检测)
│   │   ├── syson-diagram-services/                   ← 图表服务 (MultiLineLabelSwitch)
│   │   └── syson-model-services/                     ← 模型服务 (featureTypeViewUsage)
│   └── views/
│       ├── syson-diagram-common-view/                ← 通用节点 Provider (条件样式)
│       ├── syson-standard-diagrams-view/             ← SDV + DoDAF 图表/表格/甘特图 Provider
│       └── syson-table-requirements-view/            ← RTV 参考实现
├── frontend/
│   ├── syson/                                        ← 主应用 (主题、布局)
│   └── syson-components/                             ← 组件库 (节点组件、注册表)
└── doc/
    └── detaileddesign.md                             ← 本文档
```

### 2.2 核心数据流

```
Dodafv2TemplateBuilder (模板数据)
    → SysMLv2SemanticDataTemplatesInitializer (资源加载)
    → SysMLv2TemplatesRepresentationInitializer (图表创建)
        ├→ DiagramMutationDiagramService.createDiagram() (图表)
        ├→ TableCreationService.create() (表格)
        └→ GanttCreationService.create() (甘特图)
    → 前端 React 组件渲染
```

### 2.3 DoDAF 元素标记机制

每个模板元素携带 `aliasId` 标记，贯穿整个系统用于识别 DoDAF 类型：

| 标记 | 含义 | 用于 |
|------|------|------|
| `dodaf:capability` | 能力元素 | CV 视角 |
| `dodaf:operational` | 作战执行者 | OV 视角 (pu) |
| `dodaf:system` | 系统元素 | SV 视角 |
| `dodaf:organization` | 组织元素 | OV-4 |
| `dodaf:exchange` | 信息交换 | OV-3 |
| `dodaf:node` | 通用 DoDAF 节点 | 默认标记 |

---

## 3. DoDAF 模板设计

### 3.1 模板结构

文件: `Dodafv2TemplateBuilder.java`

#### 3.1.1 视角与视图

| 视角 | 中文名 | 视图数 | 父 Package |
|------|--------|--------|------------|
| AV | 全视角 | 2 | `AV_全视角` |
| CV | 能力视角 | 7 | `CV_能力视角` |
| DIV | 数据和信息视角 | 3 | `DIV_数据和信息视角` |
| OV | 作战视角 | 8 | `OV_作战视角` |
| PV | 项目视角 | 3 | `PV_项目视角` |
| SvcV | 服务视角 | 10 | `SvcV_服务视角` |
| StdV | 标准视角 | 2 | `StdV_标准视角` |
| SV | 系统视角 | 8 | `SV_系统视角` |

#### 3.1.2 元素创建方法

| 方法 | SysML 类型 | DoDAF 用途 |
|------|-----------|-----------|
| `pd(n, marker)` | PartDefinition | 静态定义（系统、能力分类） |
| `pu(n, marker)` | PartUsage | 实例元素（执行者、节点、矩阵元素） |
| `ac(n)` | ActionUsage | 活动/功能（作战活动、系统功能） |
| `vd(p, n)` | ViewDefinition | DoDAF 自定义视图定义 |
| `md(p, n)` | MetadataDefinition | 元数据定义 |

**注意**: `ac()` 元素不再添加 DoDAF 标记，使用默认 SysML `«action»` 衍型（圆角矩形样式，符合 style.md 的 Activity 符号）。

#### 3.1.3 关系创建方法

| 方法 | SysML 关系 | 用途 |
|------|-----------|------|
| `dep(p, client, supplier)` | Dependency | 通用依赖关系（模板主要使用） |
| `sub(p, sub, sup)` | Subclassification | 分类层次（仅用于 PartDefinition） |
| `add(p, child)` | OwningMembership | 元素归属 |

**受限关系**（因 EMF 只读列表，模板中不可用）:
- `SuccessionAsUsage.getSource()` / `getTarget()` — 只读
- `FeatureTyping` 某些引用 — 只读

### 3.2 元素-关系矩阵

所有视图中的元素通过 `dep()` (Dependency) 建立关联。部分分类视图使用 `sub()` (Subclassification)。

---

## 4. 自定义节点样式系统

### 4.1 设计依据

基于 `style.md` 中的 UPDM (Unified Profile for DoDAF and MODAF) 符号标准和 DoDAF 2.0 规范。

### 4.2 5 种节点样式

| 类型 | 边框颜色 | 背景 | 圆角 | 对应标记 | 衍型 |
|------|---------|------|------|---------|------|
| Capability | `#F59E0B` (琥珀) | `#F1F5F9` | 8px | `dodaf:capability` | `«Capability»` |
| Operational | `#3B82F6` (蓝) | `#F1F5F9` | 8px | `dodaf:operational` | `«Performer»` |
| System | `#00D4FF` (青) | `#F1F5F9` | 8px | `dodaf:system` | `«System»` |
| Organization | `#10B981` (绿) | `#F1F5F9` | 8px | `dodaf:organization` | `«Organization»` |
| Exchange | `#EF4444` (红) | `#F1F5F9` | 8px | `dodaf:exchange` | `«ExchangeElement»` |

### 4.3 实现架构

```
AbstractUsageNodeDescriptionProvider / AbstractDefinitionNodeDescriptionProvider
└── .conditionalStyles(
        ConditionalNodeStyle(condition: isDodafCapability → createDodafCapabilityStyle)
        ConditionalNodeStyle(condition: isDodafOperational → createDodafOperationalStyle)
        ConditionalNodeStyle(condition: isDodafSystem → createDodafSystemStyle)
        ConditionalNodeStyle(condition: isDodafOrganization → createDodafOrganizationStyle)
        ConditionalNodeStyle(condition: isDodafExchange → createDodafExchangeStyle)
    )
```

每个样式方法使用标准 Sirius `RectangularNodeStyleDescription` 构建器创建（修复了 ClassCastException）。

### 4.4 检测方法

文件: `UtilService.java`

```java
isDodafCapability(Element) → aliasIds.contains("dodaf:capability")
isDodafOperational(Element) → aliasIds.contains("dodaf:operational")
isDodafSystem(Element)     → aliasIds.contains("dodaf:system")
isDodafOrganization(Element) → aliasIds.contains("dodaf:organization")
isDodafExchange(Element)   → aliasIds.contains("dodaf:exchange")
isDodafNode(Element)       → aliasIds.contains("dodaf:node")
```

---

## 5. 衍型标签系统

### 5.1 实现

文件: `MultiLineLabelSwitch.java`

新增 `getDodafStereotype(Element)` 方法，在 `casePartDefinition`、`casePartUsage`、`caseActionUsage` 中优先返回 DoDAF 衍型。

未标记 DoDAF 的元素保持 SysML 默认衍型（如 `«part def»`、`«part»`、`«action»`）。

### 5.2 衍型映射

| 标记 | PartDefinition 衍型 | PartUsage 衍型 | ActionUsage 衍型 |
|------|-------------------|---------------|-----------------|
| `dodaf:capability` | `«Capability»` | `«Capability»` | `«Capability»` |
| `dodaf:operational` | `«Performer»` | `«Performer»` | `«Performer»` |
| `dodaf:system` | `«System»` | `«System»` | `«System»` |
| `dodaf:organization` | `«Organization»` | `«Organization»` | `«Organization»` |
| `dodaf:exchange` | `«ExchangeElement»` | `«ExchangeElement»` | `«ExchangeElement»` |
| 无标记 `ac()` | — | — | `«action»` |

---

## 6. 资源管理器图标

### 6.1 图标文件

位置: `syson-sysml-metamodel-edit/resources/icons/full/obj16/`

| 文件 | 图标设计 |
|------|---------|
| `DodafCapability.svg` | 琥珀色菱形 + 中心圆点 |
| `DodafOperationalNode.svg` | 蓝色六边形 + 中心圆 |
| `DodafSystemNode.svg` | 青色圆角矩形 + 紫色雷达符号 |
| `DodafOrganization.svg` | 绿色矩形 + 人物符号 |
| `DodafInformationExchange.svg` | 红色箭头 + 虚线 |

### 6.2 ItemProvider 修改

| 文件 | 修改内容 |
|------|---------|
| `PartUsageItemProvider.java` | `getImage()` 增加 DoDAF 图标检测 |
| `PartDefinitionItemProvider.java` | `getImage()` 增加 DoDAF 图标检测 |
| `ActionUsageItemProvider.java` | `getImage()` 增加 DoDAF 图标检测 |

每个 Provider 新增 `getDodafIconPath(Object)` 方法，遍历 5 种 aliasId 返回对应图标路径。

---

## 7. ViewDefinition 与图表类型

### 7.1 类型定义

| # | ViewDefinition | QN | 来源 |
|---|---------------|-----|------|
| 1 | GeneralView | `StandardViewDefinitions::GeneralView` | 标准库 |
| 2 | InterconnectionView | `StandardViewDefinitions::InterconnectionView` | 标准库 |
| 3 | ActionFlowView | `StandardViewDefinitions::ActionFlowView` | 标准库 |
| 4 | StateTransitionView | `StandardViewDefinitions::StateTransitionView` | 标准库 |
| 5 | **MatrixView** | `DoDAFv2_Library::MatrixView` | **新建** |
| 6 | **GanttView** | `DoDAFv2_Library::GanttView` | **新建** |
| 7 | **SequenceView** | `DoDAFv2_Library::SequenceView` | **新建** |
| 8 | **TableView** | `DoDAFv2_Library::TableView` | **新建** |

### 7.2 视图映射

完整的 52 个视图到 ViewDefinition 映射（详见 `SysMLv2TemplatesRepresentationInitializer.getViewDefinitionForDoDAFView()`）:

#### GeneralView (15 个)
CV-1, CV-2, CV-4; DIV-1, DIV-2; OV-1, OV-2, OV-4, OV-5a, OV-6a; PV-1; SvcV-8, SvcV-9; AV-1

#### InterconnectionView (4 个)
SV-1, SV-2; SvcV-1, SvcV-2

#### ActionFlowView (3 个)
OV-5b, SV-4, SvcV-4

#### MatrixView (17 个)
OV-3; SV-3, SV-5a, SV-5b, SV-6; CV-5, CV-6, CV-7; DIV-3; PV-3; SvcV-3a, SvcV-3b, SvcV-5, SvcV-6, SvcV-7

#### GanttView (3 个)
CV-3, PV-2, SV-8

#### SequenceView (1 个)
OV-6c

#### TableView (3 个)
StdV-1, StdV-2; AV-2

### 7.3 ViewDefinitionKind 枚举

文件: `ViewDefinitionKind.java`

新增枚举值: `DODAF_MATRIX_VIEW`, `DODAF_GANTT_VIEW`, `DODAF_SEQUENCE_VIEW`, `DODAF_TABLE_VIEW`

新增方法: `isDoDAFView(ViewDefinitionKind)` 用于判断自定义类型。

### 7.4 图表自动创建

文件: `SysMLv2TemplatesRepresentationInitializer.java`

`createRepresentationForView()` 方法根据 ViewDefinition 类型分发到:
- `createTableRepresentation()` — MatrixView / TableView
- `createGanttRepresentation()` — GanttView
- `createDiagram()` — 其余类型

---

## 8. 渲染引擎设计

### 8.1 MatrixView 表格

文件: `DoDAFMatrixTableDescriptionProvider.java`

| 属性 | 值 |
|------|-----|
| 框架 | Sirius `TableBuilders` |
| 列定义 | 名称 (可编辑) / 类型 (只读) / 描述 (可编辑, 持久化) |
| 行数据源 | `aql:self.ownedElement` (ViewUsage 的子元素) |
| 分页 | 10/20/50 每页 |
| 右键菜单 | "从模型中删除" |
| Java 服务 | `DoDAFMatrixMutationServices` (文档读写) |

### 8.2 TableView 表格

文件: `DoDAFTableDescriptionProvider.java`

| 属性 | 值 |
|------|-----|
| 框架 | Sirius `TableBuilders` |
| 列定义 | 分类 / 名称 (可编辑) / 描述 |
| 行数据源 | `aql:self.getExposedElements()` |

### 8.3 GanttView 甘特图

文件: `DoDAFGanttDescriptionProvider.java`

| 属性 | 值 |
|------|-----|
| 框架 | Sirius `GanttBuilders` |
| 布局 | 左表 (任务名称) + 右甘特图 (时间条) |
| 任务数据源 | `aql:self.getExposedElements(PartUsage)` |
| 时间计算 | `computeStartEndDynamicallyExpression: true` |
| 工具 | 创建任务 / 编辑 / 删除 |

### 8.4 SequenceView 时序图

文件: `DoDAFSequenceViewDiagramDescriptionProvider.java`

| 属性 | 值 |
|------|-----|
| 框架 | Sirius `DiagramBuilders` (SDV) |
| 布局方向 | `ArrangeLayoutDirection.DOWN` (垂直) |

### 8.5 Java 服务注册

| Provider | 服务类 |
|----------|-------|
| `DoDAFMatrixViewJavaServiceProvider` | `DeleteService`, `UtilService`, `DoDAFMatrixMutationServices` |

---

## 9. 前端组件

### 9.1 节点组件注册

文件: `SysONNodeTypeRegistry.tsx`

注册 5 种 DoDAF 自定义节点类型:

| 类型 | React 组件 | 布局处理器 |
|------|-----------|-----------|
| `dodafOperationalNode` | `DodafOperationalNode` | `DodafOperationalNodeLayoutHandler` |
| `dodafSystemNode` | `DodafSystemNode` | `DodafSystemNodeLayoutHandler` |
| `dodafCapability` | `DodafCapability` | `DodafCapabilityLayoutHandler` |
| `dodafOrganization` | `DodafOrganization` | `DodafOrganizationLayoutHandler` |
| `dodafInformationExchange` | `DodafInformationExchange` | `DodafInformationExchangeLayoutHandler` |

### 9.2 布局处理器

所有处理器硬编码 `borderWidth = 2`，使用 `2 * borderWidth` 补偿边框宽度防止边框塌陷。

### 9.3 前端修复

| 问题 | 文件 | 修复 |
|------|------|------|
| 节点类型名不匹配 | `Dodaf*Node.tsx` | 对齐 `NodeComponentsMap` 键名 |
| `nodeIndex` 未使用 | `Dodaf*NodeLayoutHandler.ts` | 移除冗余的 `findNodeIndex` 调用 |
| `ApolloLink undefined` | `SysONExtensionRegistry.tsx` | 条件判断 `currentOptions.link` |
| `MuiAppBar` 重复 | `sysonTheme.ts` | 合并两个 `MuiAppBar` 定义 |
| `MuiTreeItem` 类型错误 | `sysonTheme.ts` | 使用 `as any` 类型断言 |
| Handle 无节点 ID | Template | 移除 `DecisionNode/ForkNode/JoinNode` |

---

## 10. 数据持久化

### 10.1 描述列持久化

通过 `Documentation` 元素实现:

```
PartUsage
  └── OwningMembership
       └── Documentation
            └── body: String
```

- **读取**: `DoDAFMatrixMutationServices.getDocumentationBody(Element)` → 返回 `Documentation.body`
- **写入**: `DoDAFMatrixMutationServices.editDocumentation(Element, String)` → 创建或更新 `Documentation`

### 10.2 元素名称持久化

使用标准 Sirius `SetValue` 操作写入 `declaredName` 特征:

```java
newSetValue()
    .featureName("declaredName")
    .valueExpression("aql:newValue")
```

---

## 11. 待办事项与未实现项

### 11.1 高优先级

| # | 项目 | 状态 | 描述 |
|---|------|------|------|
| 1 | **MatrixView 表格拖放** | ❌ 已跳过 | Sirius 表格组件不支持拖放，用户确认不需要 |
| 2 | **MatrixView 表格新建行** | 🔶 部分完成 | `createMatrixElement()` 已实现，右键菜单因操作列问题暂时移除 |
| 3 | **GanttView 数据渲染** | ✅ 完成 | `getGanttTasks()` Java 服务已实现 |
| 4 | **TableView 数据渲染** | ✅ 完成 | `getTableElements()` Java 服务已实现 |
| 5 | **表格数据为空** | ✅ 完成 | 统一使用 Java 服务获取 `ViewUsage.getOwnedElement()` |
| 6 | **表格行高自适应** | ✅ 完成 | CSS `white-space: normal` + `word-wrap: break-word` |
| 7 | **描述列自动换行** | ✅ 完成 | TextareaWidget 替代 TextfieldWidget |
| 8 | **操作列汉化** | ✅ 完成 | `useTableTranslation.actions` → "操作" |
| 9 | **表格背景色统一** | ✅ 完成 | `rgb(25,40,79)` + `[data-testid="table-representation"]` 精准定位 |
| 10 | **空图表图片更新** | ✅ 完成 | BASE64 → 运行时文件加载，替换为 v2 版本 |
| 11 | **甘特图表头汉化** | ✅ 完成 | Vite transform 插件替换 date-fns 英文字符串 |
| 12 | **甘特图日历日期汉化** | ✅ 完成 | Vite 插件 + DOM 扫描器 |
| 13 | **表示可删除** | ✅ 完成 | `isDeletable()` 允许删除所有表示类型 |
| 14 | **DoDAF 模板图片** | ✅ 完成 | 替换为 `dodafv2.png` |
| 15 | **重复表示节点** | ✅ 完成 | DoDAF 模板跳过 `view1` ViewUsage 创建 |
| 16 | **图标素材替换** | ✅ 完成 | ~218 个文件替换为 icon-v2 版本 |

### 11.2 中优先级

| # | 项目 | 描述 | 阻塞因素 |
|---|------|------|---------|
| 6 | **SequenceView 生命线渲染** | 实现完整的 SysML 1.6 序列图（生命线 + 消息箭头） | 需要自定义 React 前端组件，工作量约 2-3 周 |
| 7 | **GanttView 左表右甘特图联动** | 实现左侧里程碑表格与右侧 Gantt 时间条的联动交互 | Sirius Gantt 组件原生支持，需完善数据映射 |
| 8 | **Table 渲染引擎（独立）** | 为 TableView / MatrixView 创建真正独立的表格渲染（替代 SDV 图表回退） | 已完成基础框架，需完善 AQL 表达式和交互 |
| 9 | **Gantt 渲染引擎（独立）** | 为 GanttView 创建真正独立的甘特图渲染 | 已完成基础框架，需完善 TaskDescription 数据映射 |
| 10 | **矩阵表格Source/Target 拆分** | 将元素名称按 `→` / `↔` 分隔符拆分为源和目标列 | AQL `split()` 在表格中可能不可用，需自定义 Java 服务 |

### 11.3 低优先级

| # | 项目 | 描述 | 阻塞因素 |
|---|------|------|---------|
| 11 | **表格列排序/筛选** | MatrixView 表格的排序和筛选功能 | 需要注册类似 RTV 的 `sortAndFilter` 服务 |
| 12 | **表格全局搜索** | 跨所有列的全文搜索 | Sirius 框架表格组件支持，需配置 `RowFilterDescription` |
| 13 | **DIV 数据视角** | DIV-1/2/3 的数据模型可视化（UML 类图风格） | 需要专用的数据建模节点样式 |
| 14 | **PV 项目视角甘特图** | PV-2 项目时间线的甘特图完善 | 模板中的阶段元素需要时间属性 |
| 15 | **SvcV 服务视角** | SvcV 视图的服务编排/交互可视化 | 可以使用现有图表类型 |

### 11.4 已知限制

| # | 限制 | 影响范围 | 说明 |
|---|------|---------|------|
| 1 | `SuccessionAsUsage` 只读目标 | 模板 | 无法在模板中预设 Action Flow 的连接关系 |
| 2 | `DecisionNode/ForkNode/JoinNode` 无前端组件 | OV-5b | 已从模板移除，可通过 Action Flow 图表面板手动创建 |
| 3 | 甘特图交互（新建/编辑/删除按钮）不可用 | GanttView | Sirius Gantt 前端组件ReadOnly，callbacks未实现。后端工具已配置，前端交互需修改Sirius源码 |
| 4 | 所有自定义 ViewDefinition 复用 SDV 图表描述 | Matrix/Gantt/Table/Sequence | 架构上已注册独立描述，后续可替换专属实现 |
| 5 | Java 字符串常量上限 65535 字节 | AddYourFirstElement | 已改为运行时从 classpath 加载 SVG 文件 |
| 6 | 表格操作列（Row Actions） | MatrixView | 移除 contextMenuEntries 导致操作列消失，需恢复 |
| 7 | EMF EClass 图标 URL 解析异常 | ToolSidebar / 右键菜单 | Sirius 框架将 EClass 传入图标解析器时调用 `toString()` 而非 ItemProvider，导致 URL 包含 `EClassImpl@xxx`。文件本身存在于 `/api/images/icons/full/obj16/xxx.svg`，需向 Sirius 上游提 PR 修复 |
| 8 | ToolSidebar 工具侧边栏 | 全部图表 | 已合并到 feature/combine 分支，通过 Apollo Link 动态注入左侧面板（默认隐藏），需在面板菜单手动激活 |

### 11.5 后续迭代建议

1. **Phase 2**: 完善表格渲染（数据展示 + 增删改查 + 拖放）
2. **Phase 3**: 完善甘特图渲染（时间条 + 依赖线 + 里程碑）
3. **Phase 4**: 实现 SequenceView 完整生命线渲染
4. **Phase 5**: DIV/PV/SvcV 视角的专属可视化
5. **Phase 6**: 前端性能优化（大图渲染、虚拟滚动）

---

## 12. 表格/矩阵视图样式系统

### 12.1 深色主题

表格表示统一使用深色背景 `rgb(25, 40, 79)` + 白色字体，通过 `[data-testid="table-representation"]` 精准定位：

- 结构元素（Table/Row/Cell/Toolbar）：深色背景 + 白色文字
- 首列（空列）：`display: none` 隐藏
- 行高：`auto` + `white-space: normal` + `word-wrap: break-word`
- 描述列：`TextareaWidget` 多行编辑 + `Documentation` 持久化

### 12.2 表格汉化

80+ Sirius 表格 UI key 已汉化（`sirius-components-tables.json`），覆盖：
- `useTableTranslation.*`（操作、取消、保存、编辑、复制、搜索等）
- 工具栏（导出、筛选、搜索、列可见性）
- 分页（每页行数、首页、末页）
- 筛选运算符（包含、等于、开头是、之间等）
- 排序（升序、降序）

### 12.3 甘特图汉化

甘特图表头（名称、开始日期、结束日期、进度）和日历日期通过 Vite transform 插件替换 date-fns 英文字符串实现。DOM 级别翻译观察器补充动态文本。

### 12.4 图标素材

~218 个 SVG/PNG 图标替换为 icon-v2 版本，覆盖：
- sysml-types/obj16（187 个 SysML 类型图标）
- diagram-tools（11 个图表工具图标）
- table-tools（4 个表格工具图标）
- tree-explorer（2 个树资源管理器图标）
- brand（2 个品牌图片）
- impact-analysis（4 个影响分析图标）
- omnibox（2 个命令框图标）

空图表提示图片 `add_your_first_element.svg` 由硬编码 BASE64 改为运行时从 classpath 加载，便于后续替换。

---

## 13. ToolSidebar 工具侧边栏

### 13.1 概述

feature/combine 分支新增了工具侧边栏功能，位于左侧面板，默认隐藏。用户需在左侧面板菜单中手动激活"工具"面板。

### 13.2 架构

- `ToolSidebar.tsx` — 工具侧边栏 React 组件，显示 SysML 元素类型分类树
- `usePalette.ts` — 调色板工具加载 Hook
- `useInvokeTool.ts` — 工具调用 Hook（通过 GraphQL mutation 创建元素）
- `diagramSelectionStore.tsx` — 图表选中同步 Store
- `ToolSidebar.types.ts` — 类型定义

### 13.3 注册方式

通过 Apollo Link（`toolInLeftSidebarConfigurer`）拦截 `getWorkbenchConfiguration` 响应，将 `syson-tool-sidebar` 注入左侧面板 views 列表。同时注册为 `WorkbenchViewContribution`（id: `syson-tool-sidebar`, title: `工具`）。

### 13.4 已知问题

- EMF 图标渲染异常（见 11.4-7）
- `currentOptions.link` 可能为 undefined 导致 Link 拼接失败（已修复）

---

## 14. 文件清单

### 13.1 修改文件（部分）

| 文件 | 模块 | 修改内容 |
|------|------|---------|
| `Dodafv2TemplateBuilder.java` | config | DoDAF 模板 + 5 种标记 + ViewDefinition |
| `SysMLv2TemplatesRepresentationInitializer.java` | config | 自动创建图表/表格/甘特图 |
| `pom.xml` (config) | config | 添加 table/gantt Maven 依赖 |
| `AbstractUsageNodeDescriptionProvider.java` | common-view | 5 种 ConditionalNodeStyles |
| `AbstractDefinitionNodeDescriptionProvider.java` | common-view | 5 种 ConditionalNodeStyles |
| `UtilService.java` | services | 5 种 DoDAF 检测方法 |
| `MultiLineLabelSwitch.java` | diagram-services | DoDAF 衍型标签 |
| `ViewDefinitionKind.java` | services | 4 种新枚举值 |
| `StandardDiagramsConstants.java` | services | 4 种 DoDAF QN 常量 |
| `PartUsageItemProvider.java` | metamodel-edit | DoDAF 图标 |
| `PartDefinitionItemProvider.java` | metamodel-edit | DoDAF 图标 |
| `ActionUsageItemProvider.java` | metamodel-edit | DoDAF 图标 |
| `DodafOperationalNode.tsx` | frontend | 节点组件命名修正 |
| `DodafOperationalNodeLayoutHandler.ts` | frontend | 移除未使用变量 |
| `DodafSystemNode.tsx` | frontend | 节点组件命名修正 |
| `DodafSystemNodeLayoutHandler.ts` | frontend | 移除未使用变量 |
| `DodafCapability.tsx` | frontend | 节点组件命名修正 |
| `DodafCapabilityLayoutHandler.ts` | frontend | 移除未使用变量 |
| `DodafOrganization.tsx` | frontend | 节点组件命名修正 |
| `DodafOrganizationLayoutHandler.ts` | frontend | 移除未使用变量 |
| `DodafInformationExchange.tsx` | frontend | 节点组件命名修正 |
| `DodafInformationExchangeLayoutHandler.ts` | frontend | 移除未使用变量 |
| `SysONExtensionRegistry.tsx` | frontend | ApolloLink 修复 |
| `sysonTheme.ts` + `SysONFooter.tsx` | frontend | 预存 TS 错误修复 |

### 13.2 新增文件（部分）

| 文件 | 模块 | 说明 |
|------|------|------|
| `DoDAFMatrixViewDiagramDescriptionProvider.java` | standard-view | MatrixView `@Service` |
| `DoDAFMatrixTableDescriptionProvider.java` | standard-view | MatrixView 表格描述 |
| `DoDAFMatrixViewJavaServiceProvider.java` | standard-view | MatrixView Java 服务 |
| `DoDAFGanttViewDiagramDescriptionProvider.java` | standard-view | GanttView `@Service` |
| `DoDAFGanttDescriptionProvider.java` | standard-view | GanttView 甘特图描述 |
| `DoDAFSequenceViewDiagramDescriptionProvider.java` | standard-view | SequenceView `@Service` |
| `DoDAFTableViewDiagramDescriptionProvider.java` | standard-view | TableView `@Service` |
| `DoDAFTableDescriptionProvider.java` | standard-view | TableView 表格描述 |
| `DoDAFMatrixViewCreateService.java` | standard-view | MatrixView 创建条件 |
| `DoDAFGanttViewCreateService.java` | standard-view | GanttView 创建条件 |
| `DoDAFSequenceViewCreateService.java` | standard-view | SequenceView 创建条件 |
| `DoDAFTableViewCreateService.java` | standard-view | TableView 创建条件 |
| `DoDAFMatrixMutationServices.java` | standard-view | 文档读写服务 |
| `DodafCapability.svg` | metamodel-edit | Capability 图标 |
| `DodafOperationalNode.svg` | metamodel-edit | Operational 图标 |
| `DodafSystemNode.svg` | metamodel-edit | System 图标 |
| `DodafOrganization.svg` | metamodel-edit | Organization 图标 |
| `DodafInformationExchange.svg` | metamodel-edit | Exchange 图标 |
| `Capability.svg` | frontend | 前端预览用 |
| `OperationalNode.svg` | frontend | 前端预览用 |
| `SystemNode.svg` | frontend | 前端预览用 |
| `Organization.svg` | frontend | 前端预览用 |
| `InformationExchange.svg` | frontend | 前端预览用 |

---

> **文档版本**: 1.0 | **最后更新**: 2026-06-14
