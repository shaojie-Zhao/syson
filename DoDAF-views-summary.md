# DoDAF 视图与元素类型汇总

## AV-1_概述和摘要信息
- 架构描述(ArchitectureDescription)
- 架构元数据描述(ArchitectureMetadataDescription)
- 执行者(Performer)
- 任务阶段(MissionPhase)
- 能力(Capability)
- 作战能力(OperationalCapability)
- 装备能力(EquipCapability)
- 顶层作战概念(TopLevelOperationalConcept)
- 任务意图(MissionIntent)
- 任务(Task)
- 任务背景(MissionBackground)
- 目标(Objective)
- 环境(Environment)
- 作战问题(OperationalProblem)
- 通用：包(Package)、注释(Comment)、约束(Constraint)

## CV-1_能力构想
- 能力(Capability)、作战能力(OperationalCapability)、装备能力(EquipCapability)
- 任务阶段(TaskStage)、任务意图(TaskIntent)、实施阶段(ImplementationPhase)
- 愿景(Vision)、愿景描述(VisionDescription)、目标(Target)、时间标尺(TimeScale)
- 通用：包(Package)、注释(Comment)、约束(Constraint)

## CV-2_能力分类
- 能力(Capability)、作战能力(OperationalCapability)、装备能力(EquipCapability)
- 任务阶段(TaskStage)、时间标尺(TimeScale)
- 通用：包(Package)、注释(Comment)、约束(Constraint)

## CV-4_能力依赖
- 能力(Capability)、作战能力(OperationalCapability)、装备能力(EquipCapability)
- 没有通用元素

## DIV-1_概念数据模型
- 概念实体(ConceptEntity)、概念可观测量(ConceptObservable)
- 交换元素(ExchangeElement)、信息(Information)
- 信息要素(InformationElement)、信息传输模式(InformationTransmissionPattern)
- 效能指标(EffectivenessIndicator)、效能指标参数(EffectivenessIndicatorParameter)
- 命令(Command)
- 通用：包(Package)、注释(Comment)、约束(Constraint)

## DIV-2_逻辑数据模型
- 逻辑实体(LogicalEntity)、逻辑测量量(LogicalMeasure)
- 逻辑测量系统(LogicalMeasurementSystem)、逻辑数据模型(LogicalDataModel)
- 交换元素(ExchangeElement)、实体(Entity)
- 通用：包(Package)、注释(Comment)、约束(Constraint)

## DIV-3_物理数据模型
- 平台实体(PlatformEntity)、物理数据模型(PhysicalDataModel)
- 交换元素(ExchangeElement)
- 通用：包(Package)、注释(Comment)、约束(Constraint)

## OV-2_作战资源流描述
- 执行者(Performer)、节点端口(NodePort)、位置(Location)
- 条件(Condition)、任务(Task)、部队(Force)、角色(Role)
- 装备(Equipment)、能力(Capability)
- 作战能力(OperationalCapability)、装备能力(EquipCapability)
- 作战行动(OperationalAction)
- 通用：包(Package)、注释(Comment)、约束(Constraint)

## OV-4_组织结构图
- 实际组织(ActualOrganization)、个人(Person)、实际个人(ActualPerson)
- 职责(Duty)、部队(Force)、角色(Role)、装备(Equipment)
- 作战行动(OperationalAction)
- 通用：包(Package)、注释(Comment)、约束(Constraint)

## OV-5a_作战活动分解树
- 作战行动(OperationalAction)、执行者(Performer)
- 能力(Capability)、作战能力(OperationalCapability)、装备能力(EquipCapability)
- 任务(Task)、条件(Condition)、装备(Equipment)
- 通用：包(Package)、注释(Comment)、约束(Constraint)

## OV-5b_作战活动模型
- 任务阶段(MissionPhase)、作战问题(OperationalProblem)
- 作战活动(OperationalActivity)、作战任务(OperationalTask)
- 作战模型(OperationalModel)、通信活动(CommunicationActivity)
- 发信活动(TransmitActivity)、收信活动(ReceiveActivity)
- 机动活动(ManeuverActivity)、悬浮机动(HoverManeuver)
- 转移机动(TransferManeuver)、筹划机动(PlanManeuver)
- 研判活动(AnalysisActivity)、指控活动(CommandControlActivity)
- 探测活动(DetectionActivity)、打击活动(StrikeActivity)
- 防御活动(DefenseActivity)、保障活动(SupportActivity)
- 装备(Equipment)、时间标尺(TimeScale)
- 活动参数节点(ActivityParameterNode)、开始节点(InitialNode)
- 决定节点(DecisionNode)、合并节点(MergeNode)
- 水平分支节点(HorizontalForkNode)、垂直分支节点(VerticalForkNode)
- 水平集合节点(HorizontalJoinNode)、垂直集合节点(VerticalJoinNode)
- 活动最终节点(ActivityFinalNode)、流最终节点(FlowFinalNode)
- 不透明动作(OpaqueAction)、信号(Signal)
- 垂直活动分区(VerticalPartition)、水平活动分区(HorizontalPartition)
- 巡逻活动(PatrolActivity)
- 通用：包(Package)、注释(Comment)、约束(Constraint)

## OV-6c_事件追踪描述
- 生命线(Lifeline)、组合片段(CombinedFragment)
- 交互(Interaction)、状态不变量(StateInvariant)
- 时间约束(TimeConstraint)、持续约束(DurationConstraint)
- 执行规约(ExecutionSpecification)
- **关系**：作战消息(OperationalMessage)、同步调用消息(SynchronousCallMessage)
- 异步调用消息(AsynchronousCallMessage)、发送信号消息(SendSignalMessage)
- 创建消息(CreateMessage)、交互消息(InteractionMessage)
- 外部触发消息(ExternalTriggerMessage)、删除消息(DeleteMessage)
- 恢复消息(ResumeMessage)
- 通用：包(Package)、注释(Comment)、约束(Constraint)

## PV-1a_项目组合关系
- 项目(Project)、项目类型(ProjectType)、项目活动(ProjectActivity)
- 状态指示器(StateIndicator)、项目里程碑(ProjectMilestone)
- 实际项目里程碑(ActualProjectMilestone)
- 通用：包(Package)、注释(Comment)、约束(Constraint)

## PV-2_项目时间线
- 包(Package)、系统(System)、功能(Function)、作战行动(OperationalAction)
- 注释(Comment)、约束(Constraint)

## SvcV-1_服务背景描述
- 服务访问(ServiceAccess)、服务接口(ServiceInterface)
- 系统(System)、软件(Software)、组织类型(OrganizationType)
- 资源端口(ResourcePort)、能力配置(CapabilityConfiguration)
- 能力(Capability)、作战能力(OperationalCapability)
- 装备能力(EquipCapability)、位置类型(LocationType)
- 位置(Location)、功能(Function)
- 通用：包(Package)、注释(Comment)、约束(Constraint)

## SvcV-2_服务资源流描述
- 服务访问(ServiceAccess)、接口(Interface)
- 系统(System)、能力(Capability)
- 作战能力(OperationalCapability)、装备能力(EquipCapability)
- 位置(Location)、功能(Function)、条件(Condition)
- 协议(Protocol)、资源端口(ResourcePort)
- 功能标准(FunctionStandard)、标准配置(StandardConfiguration)
- 通用：包(Package)、注释(Comment)、约束(Constraint)

## SvcV-4a_服务功能描述
- 服务访问(ServiceAccess)、系统(System)、软件(Software)
- 条件(Condition)、能力(Capability)
- 作战能力(OperationalCapability)、装备能力(EquipCapability)
- 能力配置(CapabilityConfiguration)
- 个人类型(PersonType)、组织类型(OrganizationType)
- 通用：包(Package)、注释(Comment)、约束(Constraint)

## SV-1_系统接口描述
- 系统(System)、软件(Software)
- 个人类型(PersonType)、组织类型(OrganizationType)
- 资源端口(ResourcePort)、能力(Capability)
- 作战能力(OperationalCapability)、装备能力(EquipCapability)
- 能力配置(CapabilityConfiguration)
- 通用：包(Package)、注释(Comment)、约束(Constraint)

## SV-2_系统资源流描述
- 系统(System)、软件(Software)
- 能力配置(CapabilityConfiguration)
- 组织类型(OrganizationType)、个人类型(PersonType)
- 资源端口(ResourcePort)
- 技术标准(TechnicalStandard)、功能标准(FunctionStandard)
- 协议(Protocol)、标准配置(StandardConfiguration)
- 通用：包(Package)、注释(Comment)、约束(Constraint)

## SV-4a_系统功能描述
- 功能(Function)、系统(System)、软件(Software)
- 条件(Condition)、能力配置(CapabilityConfiguration)
- 组织类型(OrganizationType)、个人类型(PersonType)
- 服务访问(ServiceAccess)、能力(Capability)
- 作战能力(OperationalCapability)、装备能力(EquipCapability)
- 通用：包(Package)、注释(Comment)、约束(Constraint)

## SV-4b_系统功能流描述
- 同 SV-4a（功能、系统、软件、条件、能力配置等）

## General View（通用视图·DoDAF右键菜单）
- 能力(Capability)、作战节点(OperationalNode)
- 系统节点(SystemNode)、组织(Organization)
- 作战活动(Action)
- 通用：包(Package)、注释(Comment)、约束(Constraint)
