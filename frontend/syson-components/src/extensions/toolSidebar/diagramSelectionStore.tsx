/*******************************************************************************
 * Copyright (c) 2026 Obeo.
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

import { useSelection } from '@eclipse-sirius/sirius-components-core';
import { useDialog } from '@eclipse-sirius/sirius-components-diagrams';
import { useReactFlow, useStore } from '@xyflow/react';
import React, { useEffect, useState } from 'react';

// ── Singleton store ─────────────────────────────────────────────────
let _selectedDiagramElementIds: string[] = [];
let _selectedSemanticIds: string[] = [];
const listeners = new Set<() => void>();

export function getSelectedDiagramElementIds(): string[] {
  return _selectedDiagramElementIds;
}

export function getSelectedSemanticIds(): string[] {
  return _selectedSemanticIds;
}

function notifyListeners() {
  listeners.forEach((fn) => fn());
}

export function subscribeToDiagramSelection(onChange: () => void): () => void {
  listeners.add(onChange);
  return () => listeners.delete(onChange);
}

// ── Dialog delegation (ToolSidebar → in-diagram component → useDialog) ──
type DialogRequest = {
  dialogDescriptionId: string;
  onConfirm: (variables: any[]) => void;
  onClose: () => void;
  variables: any[];
};
let _pendingDialog: DialogRequest | null = null;
let _dialogRequestId = 0;

export function requestDialog(
  dialogDescriptionId: string,
  onConfirm: (variables: any[]) => void,
  selectionVariables: any[]
) {
  _pendingDialog = {
    dialogDescriptionId,
    onConfirm: (variables) => {
      _pendingDialog = null;  // clear BEFORE calling onConfirm to prevent re-trigger
      onConfirm(variables);
    },
    onClose: () => {
      _pendingDialog = null;
    },
    variables: selectionVariables,
  };
  _dialogRequestId++;
  notifyListeners();  // trigger re-render via subscriber state update
}

// ── inner sync + dialog component ───────────────────────────────────
const DiagramSelectionSyncInner = () => {
  const { getNodes } = useReactFlow();
  // Subscribe to ReactFlow node store so we re-render when nodes change (add/remove)
  const rfNodes = useStore((store) => store.nodes);
  const { selection } = useSelection();
  const { showDialog } = useDialog();
  const [dialogTrigger, setDialogTrigger] = useState(0);

  // Listen for dialog requests via the shared store
  useEffect(() => {
    const unsub = subscribeToDiagramSelection(() => {
      if (_pendingDialog) {
        setDialogTrigger((prev) => prev + 1);
      }
    });
    return unsub;
  }, []);

  // Handle pending dialog requests from ToolSidebar
  useEffect(() => {
    if (_pendingDialog && dialogTrigger > 0) {
      const req = _pendingDialog;
      showDialog(req.dialogDescriptionId, req.variables, req.onConfirm, req.onClose);
    }
  }, [dialogTrigger, showDialog]);

  // Sync ReactFlow node IDs — use rfNodes as dep so deletion/creation triggers re-sync
  useEffect(() => {
    const nodes = getNodes();
    const entries = selection.entries;
    const semanticIds = entries.map((e) => e.id);

    let changed = false;

    if (semanticIds.length === 0) {
      if (_selectedDiagramElementIds.length > 0) {
        _selectedDiagramElementIds = [];
        changed = true;
      }
      if (_selectedSemanticIds.length > 0) {
        _selectedSemanticIds = [];
        changed = true;
      }
    } else {
      const nodeIds = nodes
        .filter((n) => semanticIds.includes((n.data as any)?.targetObjectId))
        .map((n) => n.id);
      if (
        nodeIds.length !== _selectedDiagramElementIds.length ||
        !nodeIds.every((id, i) => id === _selectedDiagramElementIds[i])
      ) {
        _selectedDiagramElementIds = nodeIds;
        changed = true;
      }
      if (
        semanticIds.length !== _selectedSemanticIds.length ||
        !semanticIds.every((id, i) => id === _selectedSemanticIds[i])
      ) {
        _selectedSemanticIds = semanticIds;
        changed = true;
      }
    }

    if (changed) notifyListeners();
  }, [selection.entries, rfNodes, getNodes]);

  return null;
};

// ── error boundary ──────────────────────────────────────────────────
class ErrorBoundary extends React.Component<
  { children: React.ReactNode },
  { hasError: boolean }
> {
  constructor(props: { children: React.ReactNode }) {
    super(props);
    this.state = { hasError: false };
  }
  static getDerivedStateFromError() {
    return { hasError: true };
  }
  override render() {
    if (this.state.hasError) return null;
    return this.props.children;
  }
}

export const DiagramSelectionSync = () => (
  <ErrorBoundary>
    <DiagramSelectionSyncInner />
  </ErrorBoundary>
);
