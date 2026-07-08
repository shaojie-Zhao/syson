package org.eclipse.syson.standard.diagrams.view.services;

import java.util.List;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.syson.sysml.Element;
import org.eclipse.syson.sysml.SysmlPackage;

public class DoDAFOV1ViewCreateService {
    public boolean canCreateDiagram(Element element) {
        List<EClass> accepted = List.of(SysmlPackage.eINSTANCE.getPackage(), SysmlPackage.eINSTANCE.getViewUsage());
        return accepted.stream().anyMatch(t -> t.isSuperTypeOf(element.eClass()));
    }
}
