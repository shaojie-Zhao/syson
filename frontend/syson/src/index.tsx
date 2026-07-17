/*******************************************************************************
 * Copyright (c) 2023, 2025 Obeo.
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

import { loadDevMessages, loadErrorMessages } from '@apollo/client/dev';
import { representationFactoryExtensionPoint } from '@eclipse-sirius/sirius-components-core';
import {
  DiagramRepresentationConfiguration,
  footerExtensionPoint,
  navigationBarIconExtensionPoint,
  navigationBarMenuHelpURLExtensionPoint,
  navigationBarRightContributionExtensionPoint,
  SiriusWebApplication,
} from '@eclipse-sirius/sirius-web-application';
import {
  sysONExtensionRegistry,
  SysONExtensionRegistryMergeStrategy,
  sysONNodeTypeRegistry,
} from '@eclipse-syson/syson-components';
import { createRoot } from 'react-dom/client';

import { HomepageBackground } from './background/HomepageBackground';
import { httpOrigin, wsOrigin } from './core/URL';
import { SysONFooter } from './extensions/SysONFooter';
import { SysONNavigationBarIcon } from './extensions/SysONNavigationBarIcon';
import { sysonTheme } from './theme/sysonTheme';

import './fonts.css';
import './ReactFlow.css';
import './reset.css';
import './transparency.css';
import './variables.css';
import './dodaf-views.css';
import ReactDOM from 'react-dom/client';
import React from 'react';
import DoDAFGanttTimeline from './views/DoDAFGanttTimeline';
import DoDAFMatrixView from './views/DoDAFMatrixView';
import DoDAFRulesView from './views/DoDAFRulesView';
import DoDAFPredictionView from './views/DoDAFPredictionView';
import { Ov1BlankView } from './views/Ov1BlankView';
import { ViewFlowBlankView } from './views/ViewFlowBlankView';
import { ViewFlowNavigator } from './extensions/ViewFlowNavigator';

(window as any).renderDoDAFGantt = (container: HTMLElement) => {
  const root = ReactDOM.createRoot(container);
  root.render(React.createElement(DoDAFGanttTimeline));
};
(window as any).renderDoDAFMatrix = (container: HTMLElement) => {
  const root = ReactDOM.createRoot(container);
  (container as any).__matrixRoot = root;
  root.render(React.createElement(DoDAFMatrixView));
};
(window as any).renderDoDAFRules = (container: HTMLElement) => {
  const root = ReactDOM.createRoot(container);
  (container as any).__rulesRoot = root;
  root.render(React.createElement(DoDAFRulesView));
};
(window as any).renderDoDAFPrediction = (container: HTMLElement) => {
  const root = ReactDOM.createRoot(container);
  (container as any).__predictionRoot = root;
  root.render(React.createElement(DoDAFPredictionView));
};

if (process.env.NODE_ENV !== 'production') {
  loadDevMessages();
  loadErrorMessages();
}

sysONExtensionRegistry.addComponent(navigationBarIconExtensionPoint, {
  identifier: `syson_${navigationBarIconExtensionPoint.identifier}`,
  Component: SysONNavigationBarIcon,
});

sysONExtensionRegistry.putData(navigationBarMenuHelpURLExtensionPoint, {
  identifier: `syson_${navigationBarMenuHelpURLExtensionPoint.identifier}`,
  data: 'https://doc.mbse-syson.org',
});

sysONExtensionRegistry.addComponent(footerExtensionPoint, {
  identifier: `syson_${footerExtensionPoint.identifier}`,
  Component: SysONFooter,
});

sysONExtensionRegistry.addComponent(navigationBarRightContributionExtensionPoint, {
  identifier: `syson_${navigationBarRightContributionExtensionPoint.identifier}_viewFlowNav`,
  Component: ViewFlowNavigator,
});


sysONExtensionRegistry.putData(representationFactoryExtensionPoint, {
  identifier: `syson_${representationFactoryExtensionPoint.identifier}`,
  data: [
    (representationMetadata: any): any => {
      if (representationMetadata?.label?.includes('OV-1')) return Ov1BlankView;
      // Match by description sourceId for manually created OV-1 views
      // DoDAFOV1ViewDiagramDescriptionProvider has viewId="DoDAFOV1ViewDiagram" → UUID is deterministic
      var descId = representationMetadata?.description?.id || '';
      if (descId.indexOf('5058ff41-3a74-3fad-93f4-0854893bd3b6') >= 0) return Ov1BlankView;
      // ViewFlow
      if (representationMetadata?.label?.includes('ViewFlow')) return ViewFlowBlankView;
      // DoDAF Rules View (OV-6a etc.)
      if (representationMetadata?.label?.includes('OV-6a') || representationMetadata?.label?.includes('规则')) return DoDAFRulesView;
      if (representationMetadata?.label?.includes('技术和技能预测') || representationMetadata?.label?.includes('Prediction')) return DoDAFPredictionView;
      return null;
    },
  ],
});

// The default SysONExtensionRegistryMergeStrategy REPLACES data for representationFactoryExtensionPoint
// ("workbench#representationFactory"), which would wipe out the built-in table/diagram/form factories when
// we register the OV-1 factory above — breaking every non-OV-1 representation (incl. the DoDAF matrix table).
// Override so factories are CONCATENATED (SysON/OV-1 first, then the defaults) instead of replaced.
class SysONRepresentationSafeMergeStrategy extends SysONExtensionRegistryMergeStrategy {
  mergeDataExtensions(identifier: string, existingValues: any, newValues: any): any {
    if (identifier === representationFactoryExtensionPoint.identifier) {
      const exData = Array.isArray(existingValues?.data) ? existingValues.data : [];
      const nvData = Array.isArray(newValues?.data) ? newValues.data : [];
      const existingIsSysON = String(existingValues?.identifier || '').startsWith('syson_');
      const data = existingIsSysON ? [...exData, ...nvData] : [...nvData, ...exData];
      return { identifier: newValues?.identifier ?? existingValues?.identifier, data };
    }
    // Navigation bar: concatenate components (both syson and sirius-web)
    if (identifier === 'navigationBar#leftContribution' || identifier === 'navigationBar#rightContribution') {
      const exData = Array.isArray(existingValues?.data) ? existingValues.data : [];
      const nvData = Array.isArray(newValues?.data) ? newValues.data : [];
      return { identifier: newValues?.identifier ?? existingValues?.identifier, data: [...exData, ...nvData] };
    }
    return super.mergeDataExtensions(identifier, existingValues, newValues);
  }
}


const container = document.getElementById('root');
const root = createRoot(container!);
root.render(
  <>
    <HomepageBackground />
    <div style={{ position: 'relative', zIndex: 1 }}>
      <SiriusWebApplication
        httpOrigin={httpOrigin}
        wsOrigin={wsOrigin}
        theme={sysonTheme}
        extensionRegistryMergeStrategy={new SysONRepresentationSafeMergeStrategy()}
        extensionRegistry={sysONExtensionRegistry}>
        <DiagramRepresentationConfiguration nodeTypeRegistry={sysONNodeTypeRegistry} />
      </SiriusWebApplication>
    </div>
  </>
);