import { useEffect } from 'react';

export const ViewFlowNavigator = () => {
  useEffect(() => {
    const handler = (event: MessageEvent) => {
      const msg = event.data;
      if (!msg || msg.type !== 'viewFlowNavigate') return;
      const viewCode = msg.viewLabel;
      if (!viewCode) return;

      fetch('http://localhost:3100/api/findRepresentation/' + encodeURIComponent(viewCode))
        .then(r => { if (!r.ok) throw new Error('not found'); return r.json(); })
        .then(data => {
          const m = window.location.href.match(/projects\/([^/]+)/);
          const projectId = m ? m[1] : '';
          if (projectId) {
            const newUrl = window.location.href.replace(/\/edit\/[^/]*$/, '/edit/' + data.representationId);
            window.open(newUrl, '_blank');
          }
        })
        .catch(() => {});
    };
    window.addEventListener('message', handler);
    return () => window.removeEventListener('message', handler);
  }, []);

  return null;
};
