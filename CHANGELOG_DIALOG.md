# 对话修改记录 (Changelog)

> 用于记录每次对话中涉及的所有代码改动，防止代码丢失/回滚后无法恢复。
> 格式：日期 + 条目编号 + 改动描述 + 涉及文件列表。
> 后续每次对话结束时，请将本对话的改动**追加**到本文件末尾。

---

## 2025-07-08 — 对话主要改动

### 1. Explorer模型树与DoDAF Matrix View 一致性（⼤三项）
**说明**：打通矩阵视图与Explorer模型树之间的双向同步。

**1a. 矩阵创建/重命名关系 → 树节点实时同步**
- 新增后端：`RenameMatrixElementInput.java`（IInput DTO：ecId + elementId + newName）
- 新增后端：`RenameMatrixElementEventHandler.java`（IEditingContextEventHandler，在 live EC 上 setDeclaredName 并 emit SEMANTIC_CHANGE → 自动刷新树 + persist）
- 修改后端：`DoDAFMatrixDataService.renameBySiriusId()` 改为通过 `eventProcessorRegistry.dispatchEvent()` 派发（而非 `findById` 孤立副本）
- 修改后端：`DoDAFGanttController` — `POST /rename-by-sirius` 端点不变
- 修改前端：`DoDAFMatrixView.tsx` — 创建流程重排为 createChild → rename-by-sirius → createRelation(带真实siriusId)
- 修改后端：`getElementIdByObjectId()` SQL 查询修复（document 表 content 为 text 类型；JSON 解析改用正则兼容空格）
- 删除：`MatrixRenameCause.java`（早期 persist 方案残留）

**1b. DoDAF Matrix双击删除 → Explorer树删除对应节点**
- 新增后端：`DeleteMatrixElementInput.java`（IInput DTO：ecId + elementId）
- 新增后端：`DeleteMatrixElementEventHandler.java`（IEditingContextEventHandler，在 live EC 上 DeleteService.deleteFromModel，emit SEMANTIC_CHANGE）
- 修改后端：`DoDAFMatrixDataService` — `createRelation` 新增 siriusId 存储到关系表；新增 `deleteRelationAndElement(pid,id)` + `deleteBySiriusId(pid,siriusId)`，通过 dispatchEvent 删除 SysML 元素
- 修改后端：`DoDAFGanttController` — `DELETE /relations/{relId}` 支持 `?ctxId=`，触发 deleteRelationAndElement
- 修改前端：`DoDAFMatrixView.tsx` — 删除分支调 `DELETE /relations/{id}?ctxId=`，移除失效的 `deleteFromModel` GraphQL；面板"删除"按钮同样补回 ctxId

**1c. Explorer树删除节点 → DoDAF Matrix刷新（反向同步）**
- 修改后端：`getRelations(pid)` 新增剪除逻辑——校验每条关系的 siriusId 是否仍存在于持久化文档中（元素被删则自动移除关系行），宽限期 10s→3s，直接读文档（移除 docTextCache）
- 修改后端：Controller `GET /relations` 接受 `?ctxId=` 参数触发剪除
- 修改前端：`DoDAFMatrixView.tsx` — `load()` 调 `/relations?ctxId=`；新增 `syson-model-mutation` 事件监听 (resolved)；新增防抖 400ms 刷新；`System` 监听器的接口及重载与提示
- 修改前端：`index.tsx` — 应用入口包裹 `window.fetch`，拦截 `/api/graphql` mutation 派发 `syson-model-mutation` 事件（react）

### 2. 行/列范围增加 sysml::ViewUsage 拖动支持
- 修改后端：`DoDAFMatrixDataService.getPackageName()` 扩展为 `Package || ViewUsage`，复用 `en()` 取 declaredName
- 修改前端：`DoDAFMatrixView.tsx` — scope 拖放区占位符/提示文案由"Package"变为"Package/ViewUsage"

### 3. 连续拖动同一个元素显示为ID的bug
- 修改前端：`DoDAFMatrixView.tsx` — `handleDrop` 改为 async，拖放后**主动调用**后端 `/scope-name` 解析正确名称（不依赖 load 是否重新触发）

### 4. 关系持久化到数据库（方案A）
- 新建数据库表 `dodaf_matrix_relation`（JdbcTemplate + `CREATE TABLE IF NOT EXISTS` 懒建表）
  - 字段：id, editing_context_id, source_id, target_id, relation_type, sirius_id, created_at
- 修改后端：`DoDAFMatrixDataService` — 全部关系操作改为 DB CRUD：
  - `createRelation(…, ctxId)` → INSERT；`getRelations()` → SELECT *；`getRelations(pid)` → WHERE editing_context_id；`deleteRelation(id)` → DELETE；`deleteRelationAndElement(pid,id)` → SELECT/deleteBySiriusId/DELETE
  - 新增 `resolveEc(pid)` 缓存 helper、`ensureTable()` 懒建表、`rowToRelation()` 行映射
- 修改后端：`DoDAFGanttController` — `createRelation` 增加 ctxId 参数
- 修改前端：`DoDAFMatrixView.tsx` — createRelation POST body 增加 `ctxId`
- 移除后端：static `relations` ConcurrentHashMap 字段

### 5. DoDAF Matrix View 打开时闪现上一个视图（已修复）
- 修改前端：`index.html` — 注入/扫描逻辑重构：
  - 新增 `currentMatrixRepId()`（从 URL 解析 representation id）
  - 新增 `teardownMatrix()`（React root.unmount + 恢复被隐藏的 Sirius table + remove wrapper）
  - wrapper 打标记 `data-repid` 和 `__hiddenTable`
  - `scanForMatrix` 新增 repid 变化守卫（仅当新旧id都非空且不同时才拆除）
  - 扫描间隔 500ms → 200ms；表格检测放宽为 `.MuiTableContainer-root table` **或** 任意 `table`
- 修改前端：`index.tsx` — `renderDoDAFMatrix` 把 React root 存到 `container.__matrixRoot`

### 6. 合并引入的问题修复
| 问题 | 影响范围 | 修复 |
|------|----------|------|
| Git冲突标记残留（`SysMLv2EditService.java:216-223`） | 后端编译失败 | 保留两边逻辑：DoDAF alias 打标记 + setDeclaredName(initName)；tagDoDAFAlias 传 resolvedId |
| Git冲突标记残留（`index.tsx:42-59`） | 前端入口语法错误 | 保留 Gantt/Matrix 渲染 + Ov1BlankView import |
| Git冲突标记残留（`DoDAFGanttTimeline.tsx:2,250,286`） | 前端编译错误 | 保留 GanttOriginal alias + Task 类型 + 丢弃未使用的 DateStartColumn/DateEndColumn |
| **关键**：OV-1 的 `representationFactoryExtensionPoint` 注册通过 `SysONExtensionRegistryMergeStrategy.mergeDataExtensions` **覆盖**了 Sirius 默认表示工厂（table/diagram/form） | 矩阵及所有非OV-1表示不渲染→空白 | 新增 `SysONRepresentationSafeMergeStrategy`（继承原策略），对 `representationFactoryExtensionPoint` 改为**拼接**（SysON OV-1 工厂在前，默认在后），其余扩展点仍走原替换逻辑 |
- 修改：`frontend/syson/src/index.tsx`
- 注意：`package-lock.json` 仍有真实冲突标记（不影响运行但会破坏 npm install/ci）

### 7. 移除前端自动轮询（改为手动刷新）
- 修改前端：`DoDAFMatrixView.tsx` — 移除定时 `setInterval` 轮询 relations；移除 focus/visibilitychange 自动刷新
- 刷新由 🔄 按钮点 `load()` 触发（`load()` 含 `?ctxId=`）

### 8. 文件变更清单（新建/修改）
**新建**：
- `backend/views/syson-standard-diagrams-view/.../RenameMatrixElementInput.java`
- `backend/views/syson-standard-diagrams-view/.../RenameMatrixElementEventHandler.java`
- `backend/views/syson-standard-diagrams-view/.../DeleteMatrixElementInput.java`
- `backend/views/syson-standard-diagrams-view/.../DeleteMatrixElementEventHandler.java`

**修改**：
- `backend/views/syson-standard-diagrams-view/.../DoDAFMatrixDataService.java`
- `backend/views/syson-standard-diagrams-view/.../DoDAFGanttController.java`
- `backend/application/syson-application-configuration/.../SysMLv2EditService.java`
- `frontend/syson/src/index.tsx`
- `frontend/syson/src/views/DoDAFMatrixView.tsx`
- `frontend/syson/src/views/DoDAFGanttTimeline.tsx`
- `frontend/syson/index.html`

**数据库新增**：`public.dodaf_matrix_relation` 表

### 9. 注意事项
- 右键菜单/palette 改动因"回滚代码和对话"丢失（未提交git，未stash），无法从git恢复——需从**环境检查点**恢复或编辑器本地历史找，或手动重做。
- 当前工作区仍有 4 个未提交文件：`DoDAFGanttController.java`, `DoDAFMatrixDataService.java`, `dodaf-views.css`, `DoDAFMatrixView.tsx`——为矩阵/关系持久化相关改动。
