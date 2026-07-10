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
  const queue = [fiber];
  let depth = 0;
  while (queue.length && depth < 200) {
    const node = queue.shift(); depth++;
    try {
      let h = node?.memoizedState;
      if (!h) { pushChildren(node, queue); continue; }
      while (h) {
        const val = h.memoizedState;
        if (val && typeof val === 'object' && val.representationsMetadata !== undefined) {
          return { setState: h.queue?.dispatch, getState: () => h.memoizedState };
        }
        h = h.next;
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

      const newRep = {
        id: data.representationId,
        label: data.label || code,
        kind: data.kind || 'siriusComponents://representation?type=Diagram',
        iconURLs: [],
        targetObjectId: data.targetObjectId || '',
        description: { id: data.descriptionId || '', __typename: 'DiagramDescription' },
        __typename: 'RepresentationMetadata',
      };

      const alreadyOpen = state.representationsMetadata.find((r: any) => r.id === newRep.id);
      if (alreadyOpen) {
        wb.setState((prev: any) => ({ ...prev, displayedRepresentationMetadata: alreadyOpen }));
        return;
      }

      // Add to tabs AND switch in single update
      wb.setState((prev: any) => ({
        ...prev,
        representationsMetadata: [...prev.representationsMetadata, newRep],
        displayedRepresentationMetadata: newRep,
      }));
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
