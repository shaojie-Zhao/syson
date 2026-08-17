/*******************************************************************************
 * Copyright (c) 2026 Obeo.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package org.eclipse.syson.standard.diagrams.view.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.syson.sysml.Dependency;
import org.eclipse.syson.sysml.PartDefinition;
import org.eclipse.syson.sysml.SysmlFactory;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DoDAFSequenceEdgeController#getEdges(String)}.
 *
 * @author oSpec
 */
class DoDAFSequenceEdgeControllerTests {

    @Test
    void testGetEdgesReturnsOnlyMessageEdgesWithMessageType() {
        DoDAFMatrixDataService dataService = mock(DoDAFMatrixDataService.class);
        ResourceSet rs = new ResourceSetImpl();
        ResourceImpl resource = new ResourceImpl();
        rs.getResources().add(resource);

        // Two lifelines A and B, a message Dependency with a "dodaf" EAnnotation and a plain Dependency without one.
        PartDefinition src = SysmlFactory.eINSTANCE.createPartDefinition();
        src.setDeclaredName("A");
        PartDefinition tgt = SysmlFactory.eINSTANCE.createPartDefinition();
        tgt.setDeclaredName("B");
        Dependency message = SysmlFactory.eINSTANCE.createDependency();
        message.setDeclaredName("Msg");
        message.getClient().add(src);
        message.getSupplier().add(tgt);
        var annotation = EcoreFactory.eINSTANCE.createEAnnotation();
        annotation.setSource("dodaf");
        annotation.getDetails().put("messageType", "SynchronousCallMessage");
        message.getEAnnotations().add(annotation);
        Dependency plain = SysmlFactory.eINSTANCE.createDependency();
        plain.setDeclaredName("Plain");

        resource.getContents().add(src);
        resource.getContents().add(tgt);
        resource.getContents().add(message);
        resource.getContents().add(plain);

        when(dataService.getRSByEcId("ctx1")).thenReturn(rs);
        DoDAFSequenceEdgeController controller = new DoDAFSequenceEdgeController(dataService);

        Map<String, Object> result = controller.getEdges("ctx1");
        List<Map<String, Object>> edges = (List<Map<String, Object>>) result.get("edges");

        // Only the Dependency carrying the "dodaf" messageType annotation is reported.
        assertThat(edges).hasSize(1);
        assertThat(edges.get(0).get("messageType")).isEqualTo("SynchronousCallMessage");
        assertThat(edges.get(0).get("label")).isEqualTo("Msg");
        assertThat(edges.get(0).get("source")).isEqualTo("A");
        assertThat(edges.get(0).get("target")).isEqualTo("B");
    }

    @Test
    void testGetEdgesWithUnknownContextReturnsEmptyList() {
        DoDAFMatrixDataService dataService = mock(DoDAFMatrixDataService.class);
        when(dataService.getRSByEcId("missing")).thenReturn(null);
        DoDAFSequenceEdgeController controller = new DoDAFSequenceEdgeController(dataService);

        Map<String, Object> result = controller.getEdges("missing");

        assertThat((List<?>) result.get("edges")).isEmpty();
    }
}
