import React, { forwardRef, useEffect, useRef } from 'react';

const VIEWFLOW_ORIGIN = 'http://localhost:3100';

export var ViewFlowBlankView = forwardRef<any, any>(function ViewFlowBlankView(props, _ref) {
  var iframeRef = useRef<HTMLIFrameElement>(null);

  useEffect(function() {
    var style = document.createElement('style');
    style.id = 'viewflow-hide-panel';
    style.textContent = '[data-testid="sidebar-right"],[data-testid="site-right"],[data-testid="right-resizer"]{display:none!important}[data-panel-id]:has([data-testid="site-right"]){flex:0 0 0px!important;max-width:0!important;min-width:0!important;overflow:hidden!important}';
    document.head.appendChild(style);

    // Notify iframe of theme changes
    var observer = new MutationObserver(function() {
      var iframe = iframeRef.current;
      if (iframe && iframe.contentWindow) {
        var isLight = document.body.classList.contains('theme-light');
        iframe.contentWindow.postMessage({ type: 'themeChange', theme: isLight ? 'light' : 'dark' }, '*');
      }
    });
    observer.observe(document.body, { attributes: true, attributeFilter: ['class'] });

    return function() {
      var el = document.getElementById('viewflow-hide-panel');
      if (el) el.remove();
      observer.disconnect();
    };
  }, []);

  // Send initial theme when iframe loads
  var handleLoad = function() {
    var iframe = iframeRef.current;
    if (iframe && iframe.contentWindow) {
      var isLight = document.body.classList.contains('theme-light');
      iframe.contentWindow.postMessage({ type: 'themeChange', theme: isLight ? 'light' : 'dark' }, '*');
    }
  };

  var isLight = document.body.classList.contains('theme-light');
  return React.createElement('iframe', {
    ref: iframeRef,
    src: VIEWFLOW_ORIGIN + '/viewFlow/index.html?theme=' + (isLight ? 'light' : 'dark'),
    style: { width: '100%', height: '100%', border: 'none', display: 'block', background: '#19284F' },
    title: 'DoDAF View Flow',
    onLoad: handleLoad,
  });
});

export default ViewFlowBlankView;
