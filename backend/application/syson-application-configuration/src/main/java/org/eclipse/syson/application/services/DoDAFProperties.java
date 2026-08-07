/*******************************************************************************
 * DoDAF property definitions extracted from the DoDAF 2.0 ontology (OWL).
 * Each DoDAF element type maps to an ordered list of properties. Values are
 * stored in the "dodaf" EAnnotation of the SysML element (XMI persisted).
 *******************************************************************************/
package org.eclipse.syson.application.services;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Configuration-driven DoDAF property definitions for the details view.
 */
public final class DoDAFProperties {

    /** Widget type of a DoDAF property. */
    public enum PropType {
        STRING, BOOLEAN, INTEGER, FLOAT, ENUM
    }

    /** A single DoDAF property definition. */
    public record DodafProp(String key, String label, PropType type, List<String> options) {
    }

    private static final List<DodafProp> GENERIC_PROPS = List.of(
            new DodafProp("isLeaf", "是否叶属性", PropType.BOOLEAN, List.of()),
            new DodafProp("isActive", "是否为活动对象", PropType.BOOLEAN, List.of()),
            new DodafProp("isFinalSpecialization", "是否为final类", PropType.BOOLEAN, List.of()),
            new DodafProp("visibility", "可见性", PropType.ENUM, List.of("公有", "私有", "受保护的", "包")));

    /** Element type (matched on the alias, case-insensitive) -> its properties. */
    private static final Map<String, List<DodafProp>> DODAF_PROPS = Map.ofEntries(
            // === AV-1 概述和摘要信息 ===
            entry("ArchitectureDescription",
                    new DodafProp("purpose", "目的", PropType.STRING, List.of()),
                    new DodafProp("viewpoint", "视点", PropType.STRING, List.of()),
                    new DodafProp("views", "视角", PropType.STRING, List.of()),
                    new DodafProp("analysis", "分析", PropType.STRING, List.of()),
                    new DodafProp("toolsUsed", "使用的工具", PropType.STRING, List.of()),
                    new DodafProp("summaryOfFindings", "结果摘要", PropType.STRING, List.of()),
                    new DodafProp("recommendations", "推荐", PropType.STRING, List.of()),
                    new DodafProp("approvalAuthority", "审批权限", PropType.STRING, List.of()),
                    new DodafProp("creatingOrganization", "创建组织", PropType.STRING, List.of()),
                    new DodafProp("dateCompleted", "完成日期", PropType.STRING, List.of()),
                    new DodafProp("assumptionAndConstraint", "假想和约束", PropType.STRING, List.of()),
                    new DodafProp("architecturalElements", "架构元素", PropType.STRING, List.of()),
                    new DodafProp("architectureFramework", "架构框架", PropType.STRING, List.of()),
                    new DodafProp("architect", "架构", PropType.STRING, List.of()),
                    new DodafProp("concerns", "关注者", PropType.STRING, List.of()),
                    new DodafProp("methods", "方法", PropType.STRING, List.of()),
                    new DodafProp("date", "日期", PropType.STRING, List.of())),
            entry("ArchitectureMetadata",
                    new DodafProp("metaData", "元数据", PropType.STRING, List.of()),
                    new DodafProp("dublinCoreElement", "都柏林核心元素", PropType.STRING, List.of()),
                    new DodafProp("modMetaDataElement", "mod元数据元素", PropType.STRING, List.of()),
                    new DodafProp("ontologyReference", "本体引用", PropType.STRING, List.of())),
            // === CV-1 能力构想 ===
            entry("EnterpriseGoal",
                    new DodafProp("goal", "目的", PropType.STRING, List.of()),
                    new DodafProp("benefits", "利益", PropType.STRING, List.of())),
            entry("EnterprisePhase",
                    new DodafProp("startDate", "开始时间", PropType.STRING, List.of()),
                    new DodafProp("endDate", "结束时间", PropType.STRING, List.of())),
            entry("Vision",
                    new DodafProp("vision", "愿景", PropType.STRING, List.of()),
                    new DodafProp("statement", "声明", PropType.STRING, List.of())),
            entry("TimeScale",
                    new DodafProp("timeValue", "时间值", PropType.STRING, List.of())),
            // === OV 特有属性 ===
            entry("ActualOrganization",
                    new DodafProp("codeOrSymbol", "代码/符号", PropType.STRING, List.of()),
                    new DodafProp("serviceType", "服务类型", PropType.STRING, List.of()),
                    new DodafProp("ratifiedStandards", "技术标准", PropType.STRING, List.of())),
            entry("LocationType",
                    new DodafProp("locationTypeKind", "位置类型种类", PropType.STRING, List.of()),
                    new DodafProp("customKind", "定制种类", PropType.STRING, List.of())),
            entry("ActualLocation",
                    new DodafProp("address", "地址", PropType.STRING, List.of()),
                    new DodafProp("locationKind", "位置种类", PropType.STRING, List.of()),
                    new DodafProp("locationNamedByAddress", "按地址命名的位置", PropType.BOOLEAN, List.of()),
                    new DodafProp("longitude", "经度", PropType.FLOAT, List.of()),
                    new DodafProp("latitude", "纬度", PropType.FLOAT, List.of()),
                    new DodafProp("altitude", "高度", PropType.FLOAT, List.of())),
            entry("Condition",
                    new DodafProp("conditionKind", "条件种类", PropType.STRING, List.of())),
            // === SV 特有属性 ===
            entry("Function",
                    new DodafProp("aFunctionDescription", "功能描述", PropType.STRING, List.of())),
            entry("System",
                    new DodafProp("aSystemDescription", "系统描述", PropType.STRING, List.of()),
                    new DodafProp("externalInterface", "外部接口", PropType.STRING, List.of()),
                    new DodafProp("internalInterface", "内部接口", PropType.STRING, List.of())),
            entry("ResourceRole",
                    new DodafProp("performsInContext", "在上下文中执行", PropType.BOOLEAN, List.of())),
            // === StdV 特有属性 ===
            entry("Standard",
                    new DodafProp("ITStandardCategory", "IT标准类别", PropType.STRING, List.of()),
                    new DodafProp("ratifiedBy", "批准", PropType.STRING, List.of()),
                    new DodafProp("mandatedDate", "法定日期", PropType.STRING, List.of()),
                    new DodafProp("retiredDate", "退役时间", PropType.STRING, List.of()),
                    new DodafProp("shortName", "简称", PropType.STRING, List.of()),
                    new DodafProp("versionNum", "版本", PropType.STRING, List.of()),
                    new DodafProp("currentStatus", "当前状态", PropType.STRING, List.of())),
            entry("CapabilityConfiguration",
                    new DodafProp("doctrine", "原则", PropType.STRING, List.of())),
            entry("DesignRule",
                    new DodafProp("identifier", "标识符", PropType.STRING, List.of())),
            entry("ResourceConnector",
                    new DodafProp("realizedInterface", "接口实现", PropType.STRING, List.of())),
            // === OV-5b 作战活动模型 ===
            entry("OperationalActivityAction",
                    new DodafProp("bActionInput", "进入条件", PropType.STRING, List.of()),
                    new DodafProp("bActionOutput", "退出条件", PropType.STRING, List.of()),
                    new DodafProp("influencfactor", "影响因素", PropType.STRING, List.of()),
                    new DodafProp("constraintCondition", "约束条件", PropType.STRING, List.of()),
                    new DodafProp("operationalPosition", "作战执行位置", PropType.STRING, List.of()),
                    new DodafProp("intelligenceSupport", "外部情报支援状态", PropType.STRING, List.of()),
                    new DodafProp("communicationSupport", "外部通信支援状态", PropType.STRING, List.of()),
                    new DodafProp("invokeDLL", "执行脚本", PropType.STRING, List.of())),
            entry("OperationalActivityEdge",
                    new DodafProp("k_sendMessageInformation", "发送信息", PropType.STRING, List.of())),
            entry("OperationalProblem",
                    new DodafProp("a_problemType", "问题类型", PropType.STRING, List.of()),
                    new DodafProp("b_problemDescription", "问题描述", PropType.STRING, List.of())),
            // === DIV 特有属性 ===
            entry("MessageInformation",
                    new DodafProp("msgID", "报文ID", PropType.STRING, List.of()),
                    new DodafProp("packageHeadLength", "包头长度(bit)", PropType.INTEGER, List.of()),
                    new DodafProp("packageLoadLength", "包载荷长度(bit)", PropType.INTEGER, List.of()),
                    new DodafProp("aInformationUsage", "信息用途", PropType.STRING, List.of()),
                    new DodafProp("bInformationSource", "信源信宿", PropType.STRING, List.of()),
                    new DodafProp("cInformationTransmissionMethod", "信息发送方式", PropType.STRING, List.of()),
                    new DodafProp("dInformationDescription", "信息描述", PropType.STRING, List.of())),
            entry("InformationAssuranceProperties",
                    new DodafProp("accessControl", "使用权限", PropType.STRING, List.of()),
                    new DodafProp("availability", "可用性", PropType.STRING, List.of()),
                    new DodafProp("confidentiality", "机密", PropType.STRING, List.of()),
                    new DodafProp("disseminationControl", "传染控制", PropType.STRING, List.of()),
                    new DodafProp("integrityProp", "完整", PropType.STRING, List.of()),
                    new DodafProp("nonRepudiationConsumer", "不可抵赖消费者", PropType.STRING, List.of()),
                    new DodafProp("nonRepudiationProducer", "不可抵赖生产者", PropType.STRING, List.of())),
            entry("SecurityAttributes",
                    new DodafProp("classification", "分类", PropType.STRING, List.of()),
                    new DodafProp("classificationReason", "分类依据", PropType.STRING, List.of()),
                    new DodafProp("classifiedBy", "通过分类", PropType.STRING, List.of()),
                    new DodafProp("releasableTo", "与之相关", PropType.STRING, List.of()),
                    new DodafProp("ownerProducer", "所有者生产者", PropType.STRING, List.of()),
                    new DodafProp("dateOfExemptedSource", "豁免来源日期", PropType.STRING, List.of()),
                    new DodafProp("typeOfExemptedSource", "豁免来源类型", PropType.STRING, List.of()),
                    new DodafProp("nonICmarkings", "非IC标记", PropType.STRING, List.of()),
                    new DodafProp("declassException", "解密异常", PropType.STRING, List.of()),
                    new DodafProp("declassEvent", "解密事件", PropType.STRING, List.of()),
                    new DodafProp("declassDate", "解密日期", PropType.STRING, List.of()),
                    new DodafProp("derivedFrom", "派生自", PropType.STRING, List.of())),
            entry("InformationElementProperties",
                    new DodafProp("scope", "范围", PropType.STRING, List.of()),
                    new DodafProp("content", "内容", PropType.STRING, List.of()),
                    new DodafProp("accuracy", "精度", PropType.STRING, List.of()),
                    new DodafProp("language", "语言", PropType.STRING, List.of())),
            entry("DataElementProperties",
                    new DodafProp("formatType", "格式类型", PropType.STRING, List.of()),
                    new DodafProp("mediaType", "媒介类型", PropType.STRING, List.of()),
                    new DodafProp("unitOfMeasurement", "测量单位", PropType.STRING, List.of())),
            // === 效能指标属性 ===
            entry("PerformanceIndicator",
                    new DodafProp("aIndicatorValue", "指标值", PropType.FLOAT, List.of()),
                    new DodafProp("bExpression", "表达式", PropType.STRING, List.of())),
            entry("PerformanceIndicatorItem",
                    new DodafProp("defaultValue", "默认值", PropType.FLOAT, List.of()),
                    new DodafProp("upperValue", "上限值", PropType.FLOAT, List.of()),
                    new DodafProp("lowerValue", "下限值", PropType.FLOAT, List.of()),
                    new DodafProp("unit", "单位", PropType.STRING, List.of()),
                    new DodafProp("actualValue", "实际值", PropType.FLOAT, List.of())));

    private DoDAFProperties() {
        // Utility class
    }

    private static Entry<String, List<DodafProp>> entry(String type, DodafProp... props) {
        return Map.entry(type, List.of(props));
    }

    /**
     * Returns the property list for the given DoDAF element, matched on its alias
     * (case-insensitive contains). Falls back to the generic property set when the
     * element type has no dedicated configuration.
     *
     * @param aliasIds the element alias ids
     * @return the ordered property list
     */
    public static List<DodafProp> propsFor(List<String> aliasIds) {
        for (var e : DODAF_PROPS.entrySet()) {
            for (String alias : aliasIds) {
                if (alias != null && alias.toLowerCase().contains(e.getKey().toLowerCase())) {
                    var props = new java.util.ArrayList<DodafProp>(e.getValue());
                    props.addAll(GENERIC_PROPS);
                    return List.copyOf(props);
                }
            }
        }
        // Generic Classifier property set for any other DoDAF element
        return GENERIC_PROPS;
    }
}
