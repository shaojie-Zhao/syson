package org.eclipse.syson.application.sysmlv2;

import java.util.UUID;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.syson.sysml.SysmlFactory;
import org.eclipse.syson.sysml.SysmlPackage;
import org.eclipse.syson.sysml.metamodel.util.ElementUtil;

public class Dodafv2TemplateBuilder {

    public Resource buildLibraryResource(Resource r) {
        var ns=SysmlFactory.eINSTANCE.createNamespace();r.getContents().add(ns);
        var lp=SysmlFactory.eINSTANCE.createLibraryPackage();lp.setDeclaredName("DoDAFv2_Library");lp.setElementId(uuid(lp));add(ns,lp);
        for(var n:new String[]{"DoDAF_ViewpointKind","DoDAF_ModelType","DoDAF_OperationalNode","DoDAF_SystemNode","DoDAF_Capability","DoDAF_Organization","DoDAF_InformationFlow"})md(lp,n);
        // DoDAF-specific ViewDefinitions for specialized diagram types
        vd(lp,"MatrixView");
        vd(lp,"GanttView");
        vd(lp,"SequenceView");
        vd(lp,"TableView");
        vd(lp,"OV1View");
        return r;
    }

    public Resource buildProjectResource(Resource r) {
        var ns=SysmlFactory.eINSTANCE.createNamespace();r.getContents().add(ns);
        var root=pkg(null,"有人无人协同反潜系统");add(ns,root);

        // AV (2)
        var av=pkg(root,"AV_全视角");
        cmt(av,"AV-1_概述和摘要信息","本体系架构描述有人/无人协同反潜作战系统");
        cmt(av,"AV-2_综合词典","UUV:无人潜航器 TAS:拖曳阵列声呐 VDS:变深声呐 SOSUS:水下声波监听系统");

        // CV (7): Capability — sub() for hierarchy, suc() for phases, dep() for mappings
        var cv=pkg(root,"CV_能力视角");
        {var v=vu(cv,"CV-1_能力构想");var a=pd("反潜作战能力","dodaf:capability");add(v,a);var b=pu("水下目标探测能力","dodaf:capability");add(v,b);dep(v,b,a);}
        {var v=vu(cv,"CV-2_能力分类");var a=pd("探测感知能力","dodaf:capability");add(v,a);var b=pd("指挥控制能力","dodaf:capability");add(v,b);var c=pd("打击能力","dodaf:capability");add(v,c);var d=pd("保障能力","dodaf:capability");add(v,d);sub(v,a,b);sub(v,b,c);sub(v,c,d);}
        {var v=vu(cv,"CV-3_能力阶段");var a=pu("初始作战能力","dodaf:capability");add(v,a);var b=pu("全面作战能力","dodaf:capability");add(v,b);dep(v,a,b);}
        {var v=vu(cv,"CV-4_能力依赖");var a=pu("探测→指控依赖","dodaf:capability");add(v,a);var b=pu("指控→打击依赖","dodaf:capability");add(v,b);dep(v,a,b);}
        {var v=vu(cv,"CV-5_能力-组织映射");var a=pu("探测能力→水面作战群","dodaf:capability");add(v,a);var b=pu("打击能力→航空反潜大队","dodaf:capability");add(v,b);dep(v,a,b);}
        {var v=vu(cv,"CV-6_能力-作战活动映射");var a=pu("探测能力→搜索探测","dodaf:capability");add(v,a);var b=pu("打击能力→武器投放","dodaf:capability");add(v,b);dep(v,a,b);}
        {var v=vu(cv,"CV-7_能力-服务映射");var a=pu("探测能力→声学处理服务","dodaf:capability");add(v,a);var b=pu("指挥能力→态势融合服务","dodaf:capability");add(v,b);dep(v,a,b);}

        // DIV (3): Data — sub() for data hierarchy, typ() for logical, dep() for physical
        var div=pkg(root,"DIV_数据和信息视角");
        {var v=vu(div,"DIV-1_概念数据模型");var a=pd("目标数据");add(v,a);var b=pd("环境数据");add(v,b);var c=pd("平台数据");add(v,c);sub(v,a,c);dep(v,b,a);}
        {var v=vu(div,"DIV-2_逻辑数据模型");var a=pu("目标实体");add(v,a);var b=pu("传感器实体");add(v,b);var c=pu("武器实体");add(v,c);dep(v,b,a);dep(v,a,c);}
        {var v=vu(div,"DIV-3_物理数据模型");var a=pu("声呐数据表");add(v,a);var b=pu("雷达数据表");add(v,b);var c=pu("武器状态表");add(v,c);dep(v,a,b);dep(v,b,c);}

        // OV (8): flw() for resource flows, suc() for action/event flows, sub() for org, dep() for others
        var ov=pkg(root,"OV_作战视角");
        {var v=vu(ov,"OV-1_高层作战概念图");/* PartUsages created dynamically from plotting tool via auto-save */}
        {var v=vu(ov,"OV-2_作战资源流描述");var a=pu("指挥节点","dodaf:operational");add(v,a);var b=pu("探测节点","dodaf:operational");add(v,b);var c=pu("攻击节点","dodaf:operational");add(v,c);dep(v,b,a);dep(v,a,c);}
        {var v=vu(ov,"OV-3_作战资源流矩阵");var a=pu("探测→指挥数据流","dodaf:exchange");add(v,a);var b=pu("指挥→攻击指令流","dodaf:exchange");add(v,b);dep(v,a,b);}
        {var v=vu(ov,"OV-4_组织结构图");var a=pu("联合反潜指挥部","dodaf:organization");add(v,a);var b=pu("水面作战群","dodaf:organization");add(v,b);var c=pu("航空反潜大队","dodaf:organization");add(v,c);var d=pu("水下无人系统分队","dodaf:organization");add(v,d);dep(v,b,a);dep(v,c,a);dep(v,d,a);}
        {var v=vu(ov,"OV-5a_作战活动分解树");var a=ac("反潜作战");add(v,a);var b=ac("搜索探测");add(v,b);var c=ac("识别跟踪");add(v,c);var d=ac("攻击决策");add(v,d);var e=ac("效果评估");add(v,e);var f=ac("战场保障");add(v,f);dep(v,a,b);dep(v,a,c);dep(v,a,d);dep(v,a,e);dep(v,a,f);dep(v,b,c);dep(v,c,d);dep(v,d,e);}
        {var v=vu(ov,"OV-5b_作战活动模型");
          var flow=ac("反潜作战指挥流程");add(v,flow);
          var start=ac("开始");add(flow,start);
          var sa=ac("接收声呐数据");add(flow,sa);
          var ta=ac("目标识别确认");add(flow,ta);
          var pa=ac("制定攻击方案");add(flow,pa);
          var wa=ac("武器准备");add(flow,wa);
          var ca=ac("下达攻击指令");add(flow,ca);
          var ea=ac("评估攻击效果");add(flow,ea);
          var done=ac("结束");add(flow,done);
          dep(v,start,sa);dep(v,sa,ta);dep(v,ta,pa);
          dep(v,pa,wa);dep(v,wa,ca);dep(v,ca,ea);dep(v,ea,done);}
        {var v=vu(ov,"OV-6a_作战规则模型");var a=pu("交战规则","dodaf:operational");add(v,a);var b=pu("识别规则","dodaf:operational");add(v,b);var c=pu("武器投放授权","dodaf:operational");add(v,c);dep(v,b,a);dep(v,b,c);}
        {var v=vu(ov,"OV-6c_事件追踪描述");var a=ac("声呐接触");add(v,a);var b=ac("目标识别");add(v,b);var c=ac("武器投放");add(v,c);var d=ac("战果评估");add(v,d);dep(v,a,b);dep(v,b,c);dep(v,c,d);}

        // PV (3): suc() for timeline, dep() for others
        var pv=pkg(root,"PV_项目视角");
        {var v=vu(pv,"PV-1_项目组合关系");var a=pu("反潜能力建设项目");add(v,a);var b=pu("UUV研发项目");add(v,b);var c=pu("声呐升级项目");add(v,c);dep(v,a,b);dep(v,a,c);}
        {var v=vu(pv,"PV-2_项目时间线");var a=pu("第一阶段:需求分析");add(v,a);var b=pu("第二阶段:系统设计");add(v,b);var c=pu("第三阶段:集成测试");add(v,c);dep(v,a,b);dep(v,b,c);}
        {var v=vu(pv,"PV-3_项目-能力映射");var a=pu("UUV研发→水下探测");add(v,a);var b=pu("声呐升级→搜索感知");add(v,b);var c=pu("指控升级→指挥控制");add(v,c);dep(v,a,b);dep(v,b,c);}

        // SvcV (10): flw() for service flows, suc() for functions/evolution, dep() for matrices
        var vc=pkg(root,"SvcV_服务视角");
        {var v=vu(vc,"SvcV-1_服务背景描述");var a=pu("声学处理服务");add(v,a);var b=pu("数据融合服务");add(v,b);var c=pu("态势显示服务");add(v,c);dep(v,a,b);dep(v,b,c);}
        {var v=vu(vc,"SvcV-2_服务资源流描述");var a=pu("声学服务→数据融合");add(v,a);var b=pu("数据融合→态势显示");add(v,b);dep(v,a,b);}
        {var v=vu(vc,"SvcV-3a_服务-系统矩阵");var a=pu("声学处理→声呐系统");add(v,a);var b=pu("数据融合→指控系统");add(v,b);var c=pu("态势显示→指控系统");add(v,c);dep(v,a,b);dep(v,b,c);}
        {var v=vu(vc,"SvcV-3b_服务-服务矩阵");var a=pu("声学处理↔数据融合");add(v,a);var b=pu("数据融合↔态势显示");add(v,b);dep(v,a,b);}
        {var v=vu(vc,"SvcV-4_服务功能描述");var a=ac("信号滤波");add(v,a);var b=ac("波束形成");add(v,b);var c=ac("目标检测");add(v,c);dep(v,a,b);dep(v,b,c);}
        {var v=vu(vc,"SvcV-5_服务-活动追溯");var a=pu("信号滤波→搜索探测");add(v,a);var b=pu("目标检测→识别跟踪");add(v,b);dep(v,a,b);}
        {var v=vu(vc,"SvcV-6_服务资源流矩阵");var a=pu("输入:原始声呐数据");add(v,a);var b=pu("输出:目标航迹");add(v,b);dep(v,a,b);}
        {var v=vu(vc,"SvcV-7_服务度量");var a=pu("检测概率");add(v,a);var b=pu("虚警率");add(v,b);var c=pu("处理延迟");add(v,c);dep(v,a,b);dep(v,b,c);}
        {var v=vu(vc,"SvcV-8_服务演进");var a=pu("当前:基础声学处理");add(v,a);var b=pu("演进:AI辅助识别");add(v,b);var c=pu("远期:自主决策");add(v,c);dep(v,a,b);dep(v,b,c);}
        {var v=vu(vc,"SvcV-9_服务技术预测");var a=pu("量子声呐");add(v,a);var b=pu("分布式处理");add(v,b);var c=pu("边缘计算");add(v,c);dep(v,a,b);dep(v,b,c);}

        // StdV (2): dep() for standard references, suc() for evolution
        var st=pkg(root,"StdV_标准视角");
        {var v=vu(st,"StdV-1_标准概要");var a=pu("MIL-STD-882E:系统安全");add(v,a);var b=pu("STANAG 4164:声呐数据格式");add(v,b);var c=pu("IEEE 1471:体系架构描述");add(v,c);var d=pu("DoDAF v2.0:体系架构框架");add(v,d);dep(v,d,a);dep(v,d,b);dep(v,d,c);}
        {var v=vu(st,"StdV-2_标准预测");var a=pu("当前标准");add(v,a);var b=pu("演进标准");add(v,b);var c=pu("未来标准");add(v,c);dep(v,a,b);dep(v,b,c);}

        // SV (8): flw() for resource flows, suc() for functions/evolution, dep() for interfaces/matrices
        var sv=pkg(root,"SV_系统视角");
        {var v=vu(sv,"SV-1_系统接口描述");var c2=pd("舰载指控系统","dodaf:system");add(v,c2);var so=pd("声呐系统","dodaf:system");add(v,so);var wp=pd("武器系统","dodaf:system");add(v,wp);var cm=pd("通信系统","dodaf:system");add(v,cm);var nv=pd("导航系统","dodaf:system");add(v,nv);var um=pd("无人系统","dodaf:system");add(v,um);dep(v,so,c2);dep(v,c2,wp);dep(v,cm,c2);dep(v,nv,c2);dep(v,um,c2);}
        {var v=vu(sv,"SV-2_系统资源流描述");var a=pu("声呐→指控数据流","dodaf:system");add(v,a);var b=pu("指控→武器指令流","dodaf:system");add(v,b);var c=pu("通信→指控消息流","dodaf:system");add(v,c);dep(v,a,b);dep(v,b,c);}
        {var v=vu(sv,"SV-3_系统-系统矩阵");var a=pu("声呐↔指控","dodaf:system");add(v,a);var b=pu("指控↔武器","dodaf:system");add(v,b);var c=pu("通信↔指控","dodaf:system");add(v,c);var d=pu("导航↔指控","dodaf:system");add(v,d);dep(v,a,b);dep(v,b,c);dep(v,c,d);}
        {var v=vu(sv,"SV-4_系统功能描述");var a=ac("声学信号处理");add(v,a);var b=ac("目标运动分析");add(v,b);var c=ac("火控解算");add(v,c);var d=ac("数据融合");add(v,d);dep(v,a,b);dep(v,b,c);dep(v,c,d);}
        {var v=vu(sv,"SV-5a_作战活动-系统功能追溯");var a=pu("搜索探测→声学信号处理","dodaf:system");add(v,a);var b=pu("识别跟踪→目标运动分析","dodaf:system");add(v,b);var c=pu("攻击决策→火控解算","dodaf:system");add(v,c);dep(v,a,b);dep(v,b,c);}
        {var v=vu(sv,"SV-5b_作战活动-系统追溯");var a=pu("搜索探测→声呐系统","dodaf:system");add(v,a);var b=pu("攻击决策→武器系统","dodaf:system");add(v,b);var c=pu("战场保障→通信系统","dodaf:system");add(v,c);dep(v,a,b);dep(v,b,c);}
        {var v=vu(sv,"SV-6_系统资源流矩阵");var a=pu("声呐→指控:目标数据","dodaf:system");add(v,a);var b=pu("指控→武器:攻击指令","dodaf:system");add(v,b);var c=pu("通信→指控:态势更新","dodaf:system");add(v,c);dep(v,a,b);dep(v,b,c);}
        {var v=vu(sv,"SV-8_系统演进描述");var a=pu("当前:独立声呐系统","dodaf:system");add(v,a);var b=pu("演进:多基声呐组网","dodaf:system");add(v,b);var c=pu("远期:AI驱动自适应声呐","dodaf:system");add(v,c);dep(v,a,b);dep(v,b,c);}

        return r;
    }

    private String uuid(org.eclipse.syson.sysml.Element e){return ElementUtil.generateUUID(e).toString();}
    private org.eclipse.syson.sysml.Element add(org.eclipse.syson.sysml.Namespace p,org.eclipse.syson.sysml.Element c){var m=SysmlFactory.eINSTANCE.createOwningMembership();p.getOwnedRelationship().add(m);m.getOwnedRelatedElement().add(c);return c;}
    private org.eclipse.syson.sysml.Package pkg(org.eclipse.syson.sysml.Package parent,String n){var p=SysmlFactory.eINSTANCE.createPackage();p.setDeclaredName(n);p.setElementId(uuid(p));if(parent!=null)add(parent,p);return p;}
    private org.eclipse.syson.sysml.ViewUsage vu(org.eclipse.syson.sysml.Namespace p,String n){var v=SysmlFactory.eINSTANCE.createViewUsage();v.setDeclaredName(n);v.setElementId(uuid(v));add(p,v);return v;}
    private org.eclipse.syson.sysml.PartDefinition pd(String n){return pd(n,"dodaf:node");}
    private org.eclipse.syson.sysml.PartDefinition pd(String n,String m){var e=SysmlFactory.eINSTANCE.createPartDefinition();e.setDeclaredName(n);e.setElementId(uuid(e));e.getAliasIds().add(m);return e;}
    private org.eclipse.syson.sysml.PartUsage pu(String n){return pu(n,"dodaf:node");}
    private org.eclipse.syson.sysml.PartUsage pu(String n,String m){var e=SysmlFactory.eINSTANCE.createPartUsage();e.setDeclaredName(n);e.setElementId(uuid(e));e.getAliasIds().add(m);return e;}
    private org.eclipse.syson.sysml.ActionUsage ac(String n){return ac(n,"dodaf:node");}
    private org.eclipse.syson.sysml.ActionUsage ac(String n,String m){var e=SysmlFactory.eINSTANCE.createActionUsage();e.setDeclaredName(n);e.setElementId(uuid(e));e.getAliasIds().add(m);return e;}
    private void md(org.eclipse.syson.sysml.Namespace p,String n){var d=SysmlFactory.eINSTANCE.createMetadataDefinition();d.setDeclaredName(n);d.setElementId(uuid(d));add(p,d);}
    private void vd(org.eclipse.syson.sysml.Namespace p,String n){var d=SysmlFactory.eINSTANCE.createViewDefinition();d.setDeclaredName(n);d.setElementId(uuid(d));add(p,d);}
    private void cmt(org.eclipse.syson.sysml.Namespace p,String n,String b){var c=SysmlFactory.eINSTANCE.createComment();c.setDeclaredName(n);c.setBody(b);c.setElementId(uuid(c));add(p,c);}
    // Dependency: client depends on supplier
    private void dep(org.eclipse.syson.sysml.Namespace p,org.eclipse.syson.sysml.Element client,org.eclipse.syson.sysml.Element supplier){var d=SysmlFactory.eINSTANCE.createDependency();d.setElementId(uuid(d));d.getClient().add(client);d.getSupplier().add(supplier);add(p,d);}
    // Subclassification
    private void sub(org.eclipse.syson.sysml.Namespace p,org.eclipse.syson.sysml.Classifier sub,org.eclipse.syson.sysml.Classifier sup){
        var s=SysmlFactory.eINSTANCE.createSubclassification();s.setElementId(uuid(s));s.setSubclassifier(sub);s.setSuperclassifier(sup);add(p,s);}
    // FeatureTyping
    private void typ(org.eclipse.syson.sysml.Namespace p,org.eclipse.syson.sysml.Feature typed,org.eclipse.syson.sysml.Type type){
        var t=SysmlFactory.eINSTANCE.createFeatureTyping();t.setElementId(uuid(t));t.setTypedFeature(typed);t.setType(type);add(p,t);}
    // Subsetting
    private void sst(org.eclipse.syson.sysml.Namespace p,org.eclipse.syson.sysml.Feature ing,org.eclipse.syson.sysml.Feature ed){
        var s=SysmlFactory.eINSTANCE.createSubsetting();s.setElementId(uuid(s));s.setSubsettingFeature(ing);s.setSubsettedFeature(ed);add(p,s);}
    // Redefinition
    private void rdf(org.eclipse.syson.sysml.Namespace p,org.eclipse.syson.sysml.Feature ing,org.eclipse.syson.sysml.Feature ed){
        var r=SysmlFactory.eINSTANCE.createRedefinition();r.setElementId(uuid(r));r.setRedefiningFeature(ing);r.setRedefinedFeature(ed);add(p,r);}
}
