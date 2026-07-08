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
<<<<<<< HEAD
import ReactDOM from 'react-dom/client';
import React from 'react';
import DoDAFGanttTimeline from './views/DoDAFGanttTimeline';
import DoDAFMatrixView from './views/DoDAFMatrixView';

(window as any).renderDoDAFGantt = (container: HTMLElement) => {
  const root = ReactDOM.createRoot(container);
  root.render(React.createElement(DoDAFGanttTimeline));
};
(window as any).renderDoDAFMatrix = (container: HTMLElement) => {
  const root = ReactDOM.createRoot(container);
  (container as any).__matrixRoot = root;
  root.render(React.createElement(DoDAFMatrixView));
};
=======
import { Ov1BlankView } from './views/Ov1BlankView';
>>>>>>> 2382e533cb3c0fc85062c2c03f6e26fea5f95c9a

// Notify overlay views (e.g. the DoDAF Matrix) whenever a GraphQL mutation changes the model — for
// instance after deleting a node from the Explorer tree — so they can refresh immediately. The matrix
// lives in its own isolated React root without access to Sirius' Apollo/subscriptions, so we bridge the
// signal through a window event by wrapping fetch (which Apollo's HttpLink uses for /api/graphql).
(() => {
  const w = window as any;
  if (w.__sysonFetchPatched) {
    return;
  }
  w.__sysonFetchPatched = true;
  const origFetch: typeof fetch = w.fetch?.bind(w);
  if (!origFetch) {
    return;
  }
  w.fetch = async (...args: any[]) => {
    const res = await origFetch(...(args as [RequestInfo, RequestInit?]));
    try {
      const req = args[0];
      const url = typeof req === 'string' ? req : req?.url;
      const init = args[1];
      const method = (init?.method || (typeof req === 'object' ? req?.method : '') || '').toUpperCase();
      const body = init?.body;
      if (url && url.includes('/api/graphql') && method === 'POST' && typeof body === 'string' && body.includes('mutation') && res.ok) {
        window.dispatchEvent(new CustomEvent('syson-model-mutation'));
      }
    } catch {
      /* ignore */
    }
    return res;
  };
})();

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

sysONExtensionRegistry.putData(representationFactoryExtensionPoint, {
  identifier: `syson_${representationFactoryExtensionPoint.identifier}`,
  data: [
    (representationMetadata: any): any => {
      if (representationMetadata?.label?.includes('OV-1')) return Ov1BlankView;
      // Match by description sourceId for manually created OV-1 views
      // DoDAFOV1ViewDiagramDescriptionProvider has viewId="DoDAFOV1ViewDiagram" → UUID is deterministic
      var descId = representationMetadata?.description?.id || '';
      if (descId.indexOf('5058ff41-3a74-3fad-93f4-0854893bd3b6') >= 0) return Ov1BlankView;
      return null;
    },
  ],
});

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
        extensionRegistryMergeStrategy={new SysONExtensionRegistryMergeStrategy()}
        extensionRegistry={sysONExtensionRegistry}>
        <DiagramRepresentationConfiguration nodeTypeRegistry={sysONNodeTypeRegistry} />
      </SiriusWebApplication>
    </div>
  </>
);