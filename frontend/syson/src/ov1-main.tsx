import React from 'react';
import { createRoot } from 'react-dom/client';
import { Ov1ConceptView } from './views/Ov1ConceptView';

const root = createRoot(document.getElementById('root')!);
root.render(React.createElement(Ov1ConceptView));
