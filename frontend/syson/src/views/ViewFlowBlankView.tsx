import React, { forwardRef, useEffect } from 'react';

const VIEWFLOW_ORIGIN = 'http://localhost:3100';

export var ViewFlowBlankView = forwardRef<any, any>(function ViewFlowBlankView(props, _ref) {
  useEffect(function() {
    var style = document.createElement('style');
    style.id = 'viewflow-hide-panel';
    style.textContent = '[data-testid="sidebar-right"],[data-testid="site-right"],[data-testid="right-resizer"]{display:none!important}[data-panel-id]:has([data-testid="site-right"]){flex:0 0 0px!important;max-width:0!important;min-width:0!important;overflow:hidden!important}';
    document.head.appendChild(style);
    return function() { var el = document.getElementById('viewflow-hide-panel'); if (el) el.remove(); };
  }, []);

  return React.createElement('iframe', {
    src: VIEWFLOW_ORIGIN + '/viewFlow/index.html',
    style: { width: '100%', height: '100%', border: 'none', display: 'block', background: '#19284F' },
    title: 'DoDAF View Flow',
  });
});

export default ViewFlowBlankView;
