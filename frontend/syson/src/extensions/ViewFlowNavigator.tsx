import { useEffect } from 'react';

export const ViewFlowNavigator = () => {
  useEffect(() => {
    const handler = (event: MessageEvent) => {
      const msg = event.data;
      if (!msg || msg.type !== 'viewFlowNavigate') return;
      const viewCode = msg.viewLabel;
      if (!viewCode) return;
      openView(viewCode);
    };
    window.addEventListener('message', handler);
    return () => window.removeEventListener('message', handler);
  }, []);

  return null;
};

/* ── Find workbench setState via React fiber ─────────────────────────── */

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
        if (val && typeof val === 'object'
            && val.representationsMetadata !== undefined
            && val.displayedRepresentationMetadata !== undefined) {
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

/* ── Open view ───────────────────────────────────────────────────────── */

function openView(code: string) {
  fetch('http://localhost:3100/api/findRepresentation/' + encodeURIComponent(code))
    .then(r => { if (!r.ok) throw new Error('not found'); return r.json(); })
    .then(data => {
      const repId = data.representationId;
      if (!repId) return;

      const wb = findWorkbenchSetState();
      if (!wb) return;
      const state = wb.getState();
      if (!state) return;

      // Already open?
      const existing = state.representationsMetadata.find((r: any) => r.id === repId);
      if (existing) {
        wb.setState((prev: any) => ({ ...prev, displayedRepresentationMetadata: existing }));
        return;
      }

      const newRep = {
        id: repId,
        label: data.label || code,
        kind: data.kind || 'siriusComponents://representation?type=Diagram',
        iconURLs: [],
        description: { id: data.descriptionId || '', __typename: 'DiagramDescription' },
        __typename: 'RepresentationMetadata',
      };

      wb.setState((prev: any) => ({
        ...prev,
        representationsMetadata: [...prev.representationsMetadata, newRep],
        displayedRepresentationMetadata: newRep,
      }));
    })
    .catch(() => {});
}
