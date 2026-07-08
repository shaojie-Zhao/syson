/*******************************************************************************
 * Copyright (c) 2025, 2026 Obeo.
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

import { ApolloLink, Observable } from '@apollo/client';
import { ExtensionRegistry, WorkbenchViewContribution, workbenchViewContributionExtensionPoint } from '@eclipse-sirius/sirius-components-core';
import {
  diagramToolbarActionExtensionPoint,
  EdgeAppearanceSection,
  EdgeData,
  ImageNodeAppearanceSection,
  NodeData,
  PaletteAppearanceSectionContributionProps,
  paletteAppearanceSectionExtensionPoint,
  RectangularNodeAppearanceSection,
} from '@eclipse-sirius/sirius-components-diagrams';
import {
  GQLWidget,
  PropertySectionComponent,
  widgetContributionExtensionPoint,
} from '@eclipse-sirius/sirius-components-forms';
import {
  OmniboxCommand,
  OmniboxCommandOverrideContribution,
  omniboxCommandOverrideContributionExtensionPoint,
} from '@eclipse-sirius/sirius-components-omnibox';
import {
  GQLTreeItemContextMenuEntry,
  treeItemContextMenuEntryOverrideExtensionPoint,
  TreeItemContextMenuOverrideContribution,
} from '@eclipse-sirius/sirius-components-trees';
import {
  ApolloClientOptionsConfigurer,
  apolloClientOptionsConfigurersExtensionPoint,
  ImportLibraryCommand,
  navigationBarMenuIconExtensionPoint,
} from '@eclipse-sirius/sirius-web-application';
import BuildCircleOutlinedIcon from '@mui/icons-material/BuildCircleOutlined';
import QuestionMarkOutlinedIcon from '@mui/icons-material/QuestionMarkOutlined';
import { Edge, Node, useStoreApi } from '@xyflow/react';
import { SysMLImportedPackageNodePaletteAppearanceSection } from '../../nodes/imported_package/SysMLImportedPackageNodePaletteAppearanceSection';
import { SysMLNoteNodePaletteAppearanceSection } from '../../nodes/note/SysMLNoteNodePaletteAppearanceSection';
import { SysMLPackageNodePaletteAppearanceSection } from '../../nodes/package/SysMLPackageNodePaletteAppearanceSection';
import { sysMLNodesStyleDocumentTransform } from '../../nodes/SysMLNodesDocumentTransform';
import { SysMLViewFrameNodePaletteAppearanceSection } from '../../nodes/view_frame/SysMLViewFrameNodePaletteAppearanceSection';
import { DeleteSysMLExpressionMenuContribution } from '../expressions/DeleteSysMLExpressionMenuContribution';
import { EditSysMLExpressionMenuContribution } from '../expressions/EditSysMLExpressionMenuContribution';
import { ExpressionPropertySection } from '../expressions/ExpressionPropertySection';
import { NewSysMLExpressionMenuContribution } from '../expressions/NewSysMLExpressionMenuContribution';
import { InsertTextualSysMLMenuContribution } from '../InsertTextualSysMLv2MenuContribution';
import { SysONNavigationBarMenuIcon } from '../navigationBarMenu/SysONNavigationBarMenuIcon';
import { PublishProjectSysMLContentsAsLibraryCommand } from '../omnibox/PublishProjectSysMLContentsAsLibraryCommand';
import { ToolSidebar } from '../toolSidebar/ToolSidebar';
import { DiagramSelectionSync } from '../toolSidebar/diagramSelectionStore';
import { SysONDiagramPanelMenu } from '../SysONDiagramPanelMenu';

const sysONExtensionRegistry: ExtensionRegistry = new ExtensionRegistry();

const omniboxCommandOverrides: OmniboxCommandOverrideContribution[] = [
  {
    canHandle: (action: OmniboxCommand) => {
      return action.id === 'publishProjectSysMLContentsAsLibrary';
    },
    component: PublishProjectSysMLContentsAsLibraryCommand,
  },
  {
    canHandle: (action: OmniboxCommand) => {
      return action.id === 'importPublishedLibrary';
    },
    component: ImportLibraryCommand,
  },
];

sysONExtensionRegistry.putData<OmniboxCommandOverrideContribution[]>(omniboxCommandOverrideContributionExtensionPoint, {
  identifier: `syson_${omniboxCommandOverrideContributionExtensionPoint.identifier}`,
  data: omniboxCommandOverrides,
});

const apolloClientOptionsConfigurer: ApolloClientOptionsConfigurer = (currentOptions) => {
  const { documentTransform } = currentOptions;

  const newDocumentTransform = documentTransform
    ? documentTransform.concat(sysMLNodesStyleDocumentTransform)
    : sysMLNodesStyleDocumentTransform;
  return {
    ...currentOptions,
    documentTransform: newDocumentTransform,
  };
};

const zhLocaleConfigurer: ApolloClientOptionsConfigurer = (currentOptions) => {
  // Apollo Link that intercepts the getLocale GraphQL response and forces language to "zh"
  const zhLocaleLink = new ApolloLink((operation, forward) => {
    return new Observable((observer) => {
      const subscription = forward(operation).subscribe({
        next: (response: any) => {
          if (operation.operationName === 'getLocale' && response?.data?.viewer) {
            observer.next({
              ...response,
              data: {
                ...response.data,
                viewer: {
                  ...response.data.viewer,
                  language: 'zh',
                },
              },
            });
          } else {
            observer.next(response);
          }
        },
        error: (err: any) => observer.error(err),
        complete: () => observer.complete(),
      });
      return () => subscription.unsubscribe();
    });
  });

  return {
    ...currentOptions,
    link: currentOptions.link ? zhLocaleLink.concat(currentOptions.link) : zhLocaleLink,
  };
};

const toolInLeftSidebarConfigurer: ApolloClientOptionsConfigurer = (currentOptions) => {
  // Apollo Link that intercepts getWorkbenchConfiguration and adds the Tool sidebar to the left panel.
  // Uses JSON round-trip to reliably clone the Apollo response object (which may be frozen).
  const toolSidebarLink = new ApolloLink((operation, forward) => {
    return forward(operation).map((response: any) => {
      if (operation.operationName === 'getWorkbenchConfiguration') {
        const config = response?.data?.viewer?.editingContext?.workbenchConfiguration;
        if (config?.workbenchPanels) {
          const leftPanel = config.workbenchPanels.find((p: any) => p.id === 'left');
          if (leftPanel?.views && Array.isArray(leftPanel.views) && !leftPanel.views.some((v: any) => v.id === 'syson-tool-sidebar')) {
            // Deep-clone via JSON to avoid Apollo frozen-object issues
            const cloned = JSON.parse(JSON.stringify(response));
            const clonedPanels = cloned.data.viewer.editingContext.workbenchConfiguration.workbenchPanels;
            const clonedLeft = clonedPanels.find((p: any) => p.id === 'left');
            clonedLeft.views.push({
              id: 'syson-tool-sidebar',
              isActive: false,
              __typename: 'DefaultViewConfiguration',
            });
            return cloned;
          }
        }
      }
      return response;
    });
  });

  return {
    ...currentOptions,
    link: currentOptions.link ? toolSidebarLink.concat(currentOptions.link) : toolSidebarLink,
  };
};

sysONExtensionRegistry.putData(apolloClientOptionsConfigurersExtensionPoint, {
  identifier: `syson_${apolloClientOptionsConfigurersExtensionPoint.identifier}`,
  data: [zhLocaleConfigurer, apolloClientOptionsConfigurer, toolInLeftSidebarConfigurer],
});

// Register the Tool sidebar as a workbench view contribution
const toolViewContribution: WorkbenchViewContribution = {
  id: 'syson-tool-sidebar',
  title: '工具',
  icon: <BuildCircleOutlinedIcon />,
  component: ToolSidebar as any,
};

sysONExtensionRegistry.putData<WorkbenchViewContribution[]>(workbenchViewContributionExtensionPoint, {
  identifier: `syson_${workbenchViewContributionExtensionPoint.identifier}`,
  data: [toolViewContribution],
});

sysONExtensionRegistry.addComponent(diagramToolbarActionExtensionPoint, {
  identifier: `syson_${diagramToolbarActionExtensionPoint.identifier}_CustomToolbarEntriesMenu`,
  Component: SysONDiagramPanelMenu,
});

sysONExtensionRegistry.addComponent(diagramToolbarActionExtensionPoint, {
  identifier: `syson_${diagramToolbarActionExtensionPoint.identifier}_DiagramSelectionSync`,
  Component: DiagramSelectionSync,
});

// Apollo Link: intercept deleteTreeItem to sync RM deletions → OV-1 iframe
const deleteTreeItemInterceptor: ApolloClientOptionsConfigurer = (currentOptions) => {
  const deleteLink = new ApolloLink((operation, forward) => {
    return new Observable((observer: any) => {
      const subscription = forward(operation).subscribe({
        next: (response: any) => {
          if (operation.operationName === 'deleteTreeItem') {
            try {
              const treeItemId = operation.variables?.input?.treeItemId;
              const ecId = operation.variables?.input?.editingContextId;
              console.info('[OV-1 Link] deleteTreeItem treeItemId=' + treeItemId + ' ecId=' + ecId + ' cbSet=' + !!((window as any).__ov1OnDeleteItem));
              if (treeItemId) {
                if ((window as any).__ov1OnDeleteItem) {
                  (window as any).__ov1OnDeleteItem(treeItemId);
                } else if (ecId) {
                  fetch('http://localhost:3100/api/cleanupByPartUsage/' + encodeURIComponent(ecId) + '/' + encodeURIComponent(treeItemId), { method: 'POST' })
                    .then(function(r: any) { return r.json(); })
                    .then(function(d: any) { console.info('[OV-1 Link] cleanup result:', d); })
                    .catch(function(e: any) { console.warn('[OV-1 Link] cleanup failed:', e); });
                }
              }
            } catch(e) { console.warn('[OV-1 Link] error', e); }
          }
          observer.next(response);
        },
        error: (err: any) => observer.error(err),
        complete: () => observer.complete(),
      });
      return () => subscription.unsubscribe();
    });
  });
  return { ...currentOptions, link: currentOptions.link ? deleteLink.concat(currentOptions.link) : deleteLink };
};
sysONExtensionRegistry.putData(apolloClientOptionsConfigurersExtensionPoint, {
  identifier: `syson_${apolloClientOptionsConfigurersExtensionPoint.identifier}_deleteTreeItemInterceptor`,
  data: [deleteTreeItemInterceptor],
});

sysONExtensionRegistry.addComponent(navigationBarMenuIconExtensionPoint, {
  identifier: `syson_${navigationBarMenuIconExtensionPoint.identifier}`,
  Component: SysONNavigationBarMenuIcon,
});

const treeItemContextMenuOverrideContributions: TreeItemContextMenuOverrideContribution[] = [
  {
    canHandle: (entry: GQLTreeItemContextMenuEntry) => {
      return entry.id === 'newObjectsFromText';
    },
    component: InsertTextualSysMLMenuContribution,
  },
  {
    canHandle: (entry: GQLTreeItemContextMenuEntry) => {
      return entry.id === 'createExpression';
    },
    component: NewSysMLExpressionMenuContribution,
  },
  {
    canHandle: (entry: GQLTreeItemContextMenuEntry) => {
      return entry.id === 'editExpression';
    },
    component: EditSysMLExpressionMenuContribution,
  },
  {
    canHandle: (entry: GQLTreeItemContextMenuEntry) => {
      return entry.id === 'deleteExpression';
    },
    component: DeleteSysMLExpressionMenuContribution,
  },
];

sysONExtensionRegistry.putData<TreeItemContextMenuOverrideContribution[]>(
  treeItemContextMenuEntryOverrideExtensionPoint,
  {
    identifier: `syson_${treeItemContextMenuEntryOverrideExtensionPoint.identifier}`,
    data: treeItemContextMenuOverrideContributions,
  }
);

/*******************************************************************************
 *
 * Custom nodes appearance contributions
 *
 *******************************************************************************/
const customNodePaletteAppearanceSectionContribution: PaletteAppearanceSectionContributionProps[] = [
  // standard nodes and edges from Sirius Web
  {
    canHandle: (diagramElementIds) => {
      const store = useStoreApi<Node<NodeData>, Edge<EdgeData>>();
      const canHandle = diagramElementIds.every(
        (elementId) =>
          store.getState().nodeLookup.get(elementId)?.data.nodeAppearanceData?.gqlStyle.__typename ===
          'RectangularNodeStyle'
      );

      return canHandle;
    },
    component: RectangularNodeAppearanceSection,
  },
  {
    canHandle: (diagramElementIds) => {
      const store = useStoreApi<Node<NodeData>, Edge<EdgeData>>();
      return diagramElementIds.every(
        (elementId) =>
          store.getState().nodeLookup.get(elementId)?.data.nodeAppearanceData?.gqlStyle.__typename === 'ImageNodeStyle'
      );
    },
    component: ImageNodeAppearanceSection,
  },
  {
    canHandle: (diagramElementIds) => {
      const store = useStoreApi<Node<NodeData>, Edge<EdgeData>>();
      return diagramElementIds.every((elementId) => !!store.getState().edgeLookup.get(elementId));
    },
    component: EdgeAppearanceSection,
  },
  // custom nodes from SysON
  {
    canHandle: (diagramElementIds) => {
      const store = useStoreApi<Node<NodeData>, Edge<EdgeData>>();
      return diagramElementIds.every(
        (elementId) => store.getState().nodeLookup.get(elementId)?.type === 'sysMLPackageNode'
      );
    },
    component: SysMLPackageNodePaletteAppearanceSection,
  },
  {
    canHandle: (diagramElementIds) => {
      const store = useStoreApi<Node<NodeData>, Edge<EdgeData>>();
      return diagramElementIds.every(
        (elementId) => store.getState().nodeLookup.get(elementId)?.type === 'sysMLImportedPackageNode'
      );
    },
    component: SysMLImportedPackageNodePaletteAppearanceSection,
  },
  {
    canHandle: (diagramElementIds) => {
      const store = useStoreApi<Node<NodeData>, Edge<EdgeData>>();
      return diagramElementIds.every(
        (elementId) => store.getState().nodeLookup.get(elementId)?.type === 'sysMLNoteNode'
      );
    },
    component: SysMLNoteNodePaletteAppearanceSection,
  },
  {
    canHandle: (diagramElementIds) => {
      const store = useStoreApi<Node<NodeData>, Edge<EdgeData>>();
      return diagramElementIds.every(
        (elementId) => store.getState().nodeLookup.get(elementId)?.type === 'sysMLNoteNode'
      );
    },
    component: SysMLViewFrameNodePaletteAppearanceSection,
  },
];

sysONExtensionRegistry.putData<PaletteAppearanceSectionContributionProps[]>(paletteAppearanceSectionExtensionPoint, {
  identifier: `syson_${paletteAppearanceSectionExtensionPoint.identifier}`,
  data: customNodePaletteAppearanceSectionContribution,
});

sysONExtensionRegistry.putData(widgetContributionExtensionPoint, {
  identifier: `syson_${widgetContributionExtensionPoint.identifier}`,
  data: [
    {
      name: 'ExpressionValuePropertySectionOverride',
      icon: <QuestionMarkOutlinedIcon />,
      previewComponent: () => null,
      component: (widget: GQLWidget): PropertySectionComponent<GQLWidget> | null => {
        let propertySectionComponent: PropertySectionComponent<GQLWidget> | null = null;
        if (widget.__typename == 'Textarea' && widget.label.startsWith('syson:expression-value-widget')) {
          propertySectionComponent = ExpressionPropertySection as PropertySectionComponent<GQLWidget>;
        }
        return propertySectionComponent;
      },
    },
  ],
});

export { sysONExtensionRegistry };
