import { useEffect } from 'react';

export const ViewFlowNavigator = () => {
  useEffect(() => {
    const handler = (event: MessageEvent) => {
      const msg = event.data;
      if (!msg || msg.type !== 'viewFlowNavigate') return;
      const viewCode = msg.viewLabel;
      if (!viewCode) return;
      openInWorkbench(viewCode);
    };
    window.addEventListener('message', handler);
    return () => window.removeEventListener('message', handler);
  }, []);

  return null;
};

function findWorkbenchSetState(): any {
  const rootEl = document.getElementById('root');
  if (!rootEl) return null;
  const fiberKey = Object.keys(rootEl).find(k => k.startsWith('__reactContainer$'));
  if (!fiberKey) return null;
  let fiber: any = (rootEl as any)[fiberKey];

  // BFS to find Workbench component: has representationsMetadata AND displayedRepresentationMetadata in state
  const queue = [fiber];
  let depth = 0;
  while (queue.length && depth < 200) {
    const node = queue.shift();
    depth++;
    try {
      // Walk the hooks linked list (useState stores in memoizedState chain)
      let hook = node?.memoizedState;
      if (!hook) { pushChildren(node, queue); continue; }

      let repsMeta: any = null;
      let displayedMeta: any = null;
      let setStateFn: any = null;

      // useState in React 18: hook is { memoizedState, queue: { dispatch, ... }, next }
      let h = hook;
      let hookIdx = 0;
      while (h && hookIdx < 10) {
        const val = h.memoizedState;
        const dispatch = h.queue?.dispatch;

        if (val && typeof val === 'object' && val.representationsMetadata !== undefined && val.displayedRepresentationMetadata !== undefined) {
          // This is the Workbench's useState hook!
          return {
            setState: dispatch,
            getState: () => h.memoizedState,
          };
        }

        h = h.next;
        hookIdx++;
      }
    } catch(e) {}
    pushChildren(node, queue);
  }
  return null;

  function pushChildren(node: any, queue: any[]) {
    if (node?.child) queue.push(node.child);
    let sib = node?.child;
    while (sib?.sibling) { queue.push(sib.sibling); sib = sib.sibling; }
  }
}

function openInWorkbench(code: string) {
  fetch('http://localhost:3100/api/findRepresentation/' + encodeURIComponent(code))
    .then(r => { if (!r.ok) throw new Error('not found'); return r.json(); })
    .then(data => {
      const wb = findWorkbenchSetState();
      if (!wb) { fallbackNavigate(data.representationId); return; }

      const state = wb.getState();
      if (!state) { fallbackNavigate(data.representationId); return; }

      // Build RepresentationMetadata with real kind from DB
      const newRep = {
        id: data.representationId,
        label: data.label || code,
        kind: data.kind || 'siriusComponents://representation?type=Diagram',
        iconURLs: [],
        description: { id: data.descriptionId || '', __typename: 'DiagramDescription' },
        __typename: 'RepresentationMetadata',
      };

      // Check if already open
      const alreadyOpen = state.representationsMetadata.find((r: any) => r.id === newRep.id);
      if (alreadyOpen) {
        // Just switch to it
        wb.setState((prev: any) => ({ ...prev, displayedRepresentationMetadata: alreadyOpen }));
        return;
      }

      // Add to open tabs WITHOUT changing displayed representation
      wb.setState((prev: any) => ({
        ...prev,
        representationsMetadata: [...prev.representationsMetadata, newRep],
        // Don't change displayedRepresentationMetadata — keep current view
      }));

      // Then select it
      setTimeout(() => {
        wb.setState((prev: any) => ({
          ...prev,
          displayedRepresentationMetadata: newRep,
        }));
      }, 200);
    })
    .catch(() => {});
}

function fallbackNavigate(repId: string) {
  const m = window.location.href.match(/projects\/([^/]+)/);
  const projectId = m ? m[1] : '';
  if (projectId) {
    window.open(window.location.href.replace(/\/edit\/[^/]*$/, '/edit/' + repId), '_blank');
  }
}
