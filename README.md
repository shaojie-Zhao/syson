# SysON DoDAF v2.0 体系架构建模工具

> 基于 Eclipse SysON (SysML v2 建模工具) 扩展，实现 DoDAF v2.0 体系架构框架完整功能。

## 项目背景

Eclipse SysON 是由 Obeo 和 CEA 联合发起的开源项目，基于 Sirius Web 平台构建 Web 端 SysML v2 建模工具。SysML v2 是 OMG 于 2018 年启动的重大修订版本，不再基于 UML 而是基于 KerML 核心建模语言，具有更严谨的形式化语义。

本项目在 SysON 基础上扩展了 **DoDAF v2.0 (Department of Defense Architecture Framework)** 体系架构框架，支持 8 个视角、52 个模型的完整模板，并提供 OV-1 高层作战概念图的军标标绘与资源管理器同步功能。

## 技术栈

| 层次 | 技术 |
|------|------|
| 后端 | Java 21 + Spring Boot 4.0.6 |
| 建模框架 | EMF (Eclipse Modeling Framework) |
| 图表引擎 | Sirius Web 2026.5.0 |
| 前端 | React 18 + TypeScript + Vite |
| UI 框架 | MUI (Material-UI) + React Flow |
| 数据库 | PostgreSQL 15 |
| 3D 地图 | Cesium + EasyGlobe (OV-1 军标标绘) |

## 项目结构

```
syson/
├── backend/
│   ├── application/syson-application-configuration/  ← DoDAF 模板、初始化器、OV-1 控制器
│   ├── metamodel/                                     ← SysML 元模型、自定义节点样式
│   ├── services/                                      ← DoDAF 检测、图表服务、模型服务
│   ├── views/                                         ← 图表/表格/甘特图 Provider
│   └── tests/                                         ← 集成测试
├── frontend/
│   ├── syson/                                         ← 主应用 (主题、布局、OV-1 托管)
│   └── syson-components/                              ← 组件库 (节点组件、注册表、Apollo Link)
├── examples/                                          ← OV-1 军标标绘面板 (独立服务)
│   ├── server.js                                      ← Express 静态 + REST API
│   ├── plotting.html                                  ← Cesium 军标标绘页面
│   └── layers/                                        ← 图层 JSON 持久化目录
└── doc/                                               ← 设计文档
    ├── detaileddesign.md                              ← 详细设计文档
    └── nodeCreate.md                                  ← 模型对象创建机制文档
```

## DoDAF 扩展功能

### OV-1 高层作战概念图

OV-1 View 是 DoDAF 8 个视角中**作战视角 (OV)** 的第一个视图。本实现采用与众不同的方案：嵌入独立的 Cesium/EasyGlobe 军标标绘面板，通过 Express 服务提供图层持久化，并通过 postMessage 桥接 + Apollo Link 拦截实现与资源管理器 (Explorer Tree) 的 **PartUsage 双向同步**。

#### 军标标绘

- 基于 Cesium 3D 地球 + EasyGlobe 军标标绘库
- 支持点状/线状军标的绘制、编辑、删除
- 红/蓝双方阵营军标
- 通过 `examples/server.js` (Express) 独立服务运行在 `:3100` 端口

#### 作战概念同步

绘制军标时自动在资源管理器中创建对应的 **PartUsage 作战概念**对象：

| 功能 | 说明 |
|------|------|
| **创建** | 军标绘制 → postMessage 通知父窗口 → `createChild` GraphQL mutation (一步创建+命名) |
| **命名规则** | `"军标类型-顺序号"` (如 "海军航空兵-001"，序号三位补零) |
| **iframe→RM 删除** | 删除军标 → `deleteOv1PartUsage` 自定义 GraphQL mutation |
| **RM→iframe 删除 (视图开)** | Apollo Link 拦截 `deleteTreeItem` 响应 → postMessage → 移除军标 |
| **RM→iframe 删除 (视图关)** | Apollo Link → Express `/api/cleanupByPartUsage` → 更新 layer JSON |

#### 浮动标签

每个军标自动显示浮动标签，包含：
- 作战概念名称 (与 RM 中 PartUsage 名称一致)
- 经纬度坐标 (经度在前)
- 标签边框和引线颜色跟随军标线型颜色
- 标签不重叠 (碰撞避免)

#### 图层持久化

- **保存**: 每次绘制/编辑/删除后自动保存到 `layers/{representationId}.json`
- **加载**: 重新打开 OV-1 View 时自动恢复上次状态
- **ID 稳定**: 使用 `sym-{libID}-{code}-{pos.x}-{pos.y}` 算法（跨会话不变）

### 其他 DoDAF 功能

- 完整 52 个 DoDAF 模型模板 (8 视角)
- 5 种 DoDAF 元素差异化节点样式
- MatrixView / TableView / GanttView / SequenceView 四种自定义渲染引擎
- 中文界面汉化
- 深色主题

## 快速开始

### 环境要求

- Java 21+
- Node.js 22+
- Maven 3.8+
- Docker (PostgreSQL)
- Git

### 启动步骤

```bash
# 1. 启动 PostgreSQL
docker start syson-postgres

# 2. 启动后端 (Spring Boot :8080)
cd backend/application/syson-application
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 3. 启动 OV-1 标绘服务 (Express :3100)
cd examples
node server.js

# 4. 启动前端 (Vite :5173)
cd frontend/syson
npm run start
```

### 访问地址

| 服务 | 端口 | 说明 |
|------|------|------|
| 前端 | `:5173` | 主应用 |
| 后端 | `:8080` | GraphQL API + REST |
| 标绘 | `:3100` | OV-1 军标面板 + 图层 API |
| 数据库 | `:5433` | PostgreSQL |

## 开发指南

详细设计文档见 `doc/detaileddesign.md`。
OV-1 PartUsage 创建机制见 `doc/nodeCreate.md`。

## 许可证

Eclipse Public License 2.0

## 更多信息

- [SysON 官网](https://mbse-syson.org/)
- [Obeo](https://www.obeosoft.com/en/contact)
