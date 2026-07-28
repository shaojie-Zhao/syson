package org.eclipse.syson.application.sysmlv2;

import java.util.UUID;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.syson.sysml.SysmlFactory;
import org.eclipse.syson.sysml.metamodel.util.ElementUtil;

/**
 * Builds a blank DoDAFv2 project template with only Package + ViewUsage nodes.
 * No PartUsage / PartDefinition / ActionUsage / Dependency elements are created.
 */
public class Dodafv2BlankTemplateBuilder {

    public Resource buildProjectResource(Resource r) {
        var ns = SysmlFactory.eINSTANCE.createNamespace();
        r.getContents().add(ns);
        var root = pkg(null, "DoDAFv2-Blank");
        add(ns, root);

        // AV (2)
        var navPkg = pkg(root, "全视图导航");
        { var v = vu(navPkg, "ViewFlow_流程视图"); }
        var av = pkg(root, "AV_全视角");
        { var v = vu(av, "AV-1_概述和摘要信息"); }

        // CV (7)
        var cv = pkg(root, "CV_能力视角");
        { vu(cv, "CV-1_能力构想"); }
        { vu(cv, "CV-2_能力分类"); }
        { vu(cv, "CV-3_能力阶段"); }
        { vu(cv, "CV-4_能力依赖"); }
        { vu(cv, "CV-5_能力-组织映射"); }
        { vu(cv, "CV-6_能力-作战活动映射"); }
        { vu(cv, "CV-7_能力-服务映射"); }

        // DIV (3)
        var div = pkg(root, "DIV_数据和信息视角");
        { vu(div, "DIV-1_概念数据模型"); }
        { vu(div, "DIV-2_逻辑数据模型"); }
        { vu(div, "DIV-3_物理数据模型"); }

        // OV (8)
        var ov = pkg(root, "OV_作战视角");
        { vu(ov, "OV-1_高层作战概念图"); }
        { vu(ov, "OV-2_作战资源流描述"); }
        { vu(ov, "OV-3_作战资源流矩阵"); }
        { vu(ov, "OV-4_组织结构图"); }
        { vu(ov, "OV-5a_作战活动分解树"); }
        { vu(ov, "OV-5b_作战活动模型"); }
        { vu(ov, "OV-6a_作战规则模型"); }
        { vu(ov, "OV-6c_事件追踪描述"); }

        // PV (4)
        var pv = pkg(root, "PV_项目视角");
        { vu(pv, "PV-1a_项目组合关系"); }
        { vu(pv, "PV-1b_责任矩阵"); }
        { vu(pv, "PV-2_项目时间线"); }
        { vu(pv, "PV-3_项目-能力映射"); }

        // SvcV (10)
        var vc = pkg(root, "SvcV_服务视角");
        { vu(vc, "SvcV-1_服务背景描述"); }
        { vu(vc, "SvcV-2_服务资源流描述"); }
        { vu(vc, "SvcV-3a_服务-系统矩阵"); }
        { vu(vc, "SvcV-3b_服务-服务矩阵"); }
        { vu(vc, "SvcV-4a_服务功能描述"); }
        { vu(vc, "SvcV-4b_服务功能流描述"); }
        { vu(vc, "SvcV-5_服务-活动追溯"); }
        { vu(vc, "SvcV-6_服务资源流矩阵"); }
        { vu(vc, "SvcV-7_服务度量"); }
        { vu(vc, "SvcV-8_服务演进"); }
        { vu(vc, "SvcV-9_服务技术预测"); }

        // StdV (2)
        var st = pkg(root, "StdV_标准视角");
        { vu(st, "StdV-1_标准概要"); }
        { vu(st, "StdV-2_标准预测"); }

        // SV (9)
        var sv = pkg(root, "SV_系统视角");
        { vu(sv, "SV-1_系统接口描述"); }
        { vu(sv, "SV-2_系统资源流描述"); }
        { vu(sv, "SV-3_系统-系统矩阵"); }
        { vu(sv, "SV-4a_系统功能描述"); }
        { vu(sv, "SV-4b_系统功能流描述"); }
        { vu(sv, "SV-5a_作战活动-系统功能追溯"); }
        { vu(sv, "SV-5b_作战活动-系统追溯"); }
        { vu(sv, "SV-6_系统资源流矩阵"); }
        { vu(sv, "SV-8_系统演进描述"); }

        return r;
    }

    private String uuid(org.eclipse.syson.sysml.Element e) {
        return ElementUtil.generateUUID(e).toString();
    }

    private org.eclipse.syson.sysml.Element add(org.eclipse.syson.sysml.Namespace p, org.eclipse.syson.sysml.Element c) {
        var m = SysmlFactory.eINSTANCE.createOwningMembership();
        p.getOwnedRelationship().add(m);
        m.getOwnedRelatedElement().add(c);
        return c;
    }

    private org.eclipse.syson.sysml.Package pkg(org.eclipse.syson.sysml.Package parent, String n) {
        var p = SysmlFactory.eINSTANCE.createPackage();
        p.setDeclaredName(n);
        p.setElementId(uuid(p));
        if (parent != null) add(parent, p);
        return p;
    }

    private org.eclipse.syson.sysml.ViewUsage vu(org.eclipse.syson.sysml.Namespace p, String n) {
        var v = SysmlFactory.eINSTANCE.createViewUsage();
        v.setDeclaredName(n);
        v.setElementId(uuid(v));
        add(p, v);
        return v;
    }
}
