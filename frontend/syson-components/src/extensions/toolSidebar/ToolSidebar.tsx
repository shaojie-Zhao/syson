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

import {
  useDeletionConfirmationDialog,
  useMultiToast,
  useWorkbench,
  WorkbenchViewComponentProps,
} from '@eclipse-sirius/sirius-components-core';
import BuildCircleOutlinedIcon from '@mui/icons-material/BuildCircleOutlined';
import ExpandLess from '@mui/icons-material/ExpandLess';
import ExpandMore from '@mui/icons-material/ExpandMore';
import Box from '@mui/material/Box';
import CircularProgress from '@mui/material/CircularProgress';
import Divider from '@mui/material/Divider';
import List from '@mui/material/List';
import ListItemButton from '@mui/material/ListItemButton';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import Typography from '@mui/material/Typography';
import { forwardRef, useEffect, useRef, useState } from 'react';
import { makeStyles } from 'tss-react/mui';

import {
  getSelectedDiagramElementIds,
  getSelectedSemanticIds,
  requestDialog,
  subscribeToDiagramSelection,
} from './diagramSelectionStore';
import { GQLPalette } from './ToolSidebar.types';
import { useInvokeTool } from './useInvokeTool';
import { usePalette } from './usePalette';

const useToolSidebarStyles = makeStyles()((theme) => ({
  root: {
    display: 'flex',
    flexDirection: 'column',
    height: '100%',
    backgroundColor: theme.palette.background.default,
    overflow: 'hidden',
  },
  header: {
    display: 'flex',
    alignItems: 'center',
    padding: theme.spacing(1, 2),
    borderBottom: `1px solid ${theme.palette.divider}`,
    minHeight: 48,
  },
  headerTitle: {
    flex: 1,
    fontWeight: 600,
    fontSize: '0.875rem',
    color: theme.palette.text.primary,
  },
  contextChip: {
    fontSize: '0.625rem',
    color: theme.palette.primary.main,
    backgroundColor: theme.palette.action.selected,
    padding: theme.spacing(0.25, 0.75),
    borderRadius: Number(theme.shape.borderRadius) / 2,
    lineHeight: 1.5,
  },
  content: {
    flex: 1,
    overflow: 'auto',
    padding: theme.spacing(0.5, 0),
  },
  emptyState: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    padding: theme.spacing(4),
    color: theme.palette.text.disabled,
    gap: theme.spacing(1),
  },
  emptyIcon: {
    fontSize: '2rem',
    color: theme.palette.text.disabled,
  },
  emptyText: {
    fontSize: '0.75rem',
    textAlign: 'center',
  },
  loadingContainer: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    padding: theme.spacing(4),
  },
  contextInfo: {
    display: 'flex',
    alignItems: 'center',
    gap: theme.spacing(1),
    padding: theme.spacing(0.5, 2),
    borderBottom: `1px solid ${theme.palette.divider}`,
    backgroundColor: theme.palette.background.paper,
  },
  contextLabel: {
    fontSize: '0.7rem',
    color: theme.palette.text.secondary,
  },
}));

export const ToolSidebar = forwardRef<{ id: string } | null, WorkbenchViewComponentProps>(
  ({ editingContextId }, _ref) => {
    const { classes } = useToolSidebarStyles();
    const [invokeTool, { error: invokeError }] = useInvokeTool();
    const { addErrorMessage } = useMultiToast();
    const { showDeletionConfirmation } = useDeletionConfirmationDialog();

    useEffect(() => {
      if (invokeError) {
        addErrorMessage(invokeError);
      }
    }, [invokeError, addErrorMessage]);
    const { displayedRepresentationMetadata } = useWorkbench();
    const representationId = displayedRepresentationMetadata?.id ?? null;

    const [diagramElementIds, setDiagramElementIds] = useState<string[]>(
      () => getSelectedDiagramElementIds()
    );

    useEffect(() => {
      const unsub = subscribeToDiagramSelection(() => {
        setDiagramElementIds(getSelectedDiagramElementIds());
      });
      return unsub;
    }, []);

    const hasSelection = diagramElementIds.length > 0;
    const primaryElementIds: string[] = hasSelection ? [diagramElementIds[0]!] : [];

    // Canvas palette: use the diagram's own representationId (matches canvas right-click)
    const {
      palette: canvasPalette,
      loading: canvasLoading,
    } = usePalette(editingContextId, representationId, representationId ? [representationId] : null);

    const {
      palette: elementPalette,
      loading: elementLoading,
    } = usePalette(editingContextId, representationId, primaryElementIds);

    const elementHasTools =
      elementPalette &&
      ((elementPalette.paletteEntries?.length ?? 0) > 0 ||
        (elementPalette.quickAccessTools?.length ?? 0) > 0);
    const activePalette = hasSelection && elementHasTools ? elementPalette : canvasPalette;
    const isLoading = hasSelection && !elementHasTools ? canvasLoading : hasSelection ? elementLoading : canvasLoading;

    // Keep last known good palette to prevent flash during re-fetch
    const lastGoodPalette = useRef<GQLPalette | null>(null);
    if (activePalette) {
      lastGoodPalette.current = activePalette;
    }
    const displayPalette = activePalette || lastGoodPalette.current;

    const handleToolClick = (toolId: string, toolLabel: string, dialogDescriptionId?: string) => {
      if (editingContextId && representationId) {
        const invokeIds = hasSelection && elementHasTools
          ? primaryElementIds
          : [representationId];
        const doInvoke = () => invokeTool(editingContextId, representationId, toolId, invokeIds, toolLabel);
        if (/delete/i.test(toolLabel)) {
          showDeletionConfirmation(doInvoke);
        } else if (dialogDescriptionId) {
          // Selection dialog tools: delegate to Sirius dialog pipeline via requestDialog
          const semanticIds = getSelectedSemanticIds();
          const initialVars = semanticIds.length > 0
            ? [{ name: 'targetObjectId', value: semanticIds[0] }]
            : [];
          requestDialog(
            dialogDescriptionId,
            (dialogVars) => {
              // dialogVars: ToolVariable[] from Sirius dialog, includes {name, value, type}
              const toolVars = dialogVars.map((v: any) => ({
                name: v.name,
                value: String(v.value ?? ''),
                type: v.type ?? 'STRING',
              }));
              // invoke the tool with collected dialog variables
              invokeTool(editingContextId, representationId, toolId, invokeIds, toolLabel, toolVars);
            },
            initialVars
          );
        } else {
          doInvoke();
        }
      }
    };

    const renderContent = () => {
      if (isLoading) {
        return (
          <div className={classes.loadingContainer}>
            <CircularProgress size={24} />
          </div>
        );
      }

      if (!displayPalette) {
        return (
          <div className={classes.emptyState}>
            <BuildCircleOutlinedIcon className={classes.emptyIcon} />
            <Typography className={classes.emptyText}>{'没有可用的工具'}</Typography>
          </div>
        );
      }

      const entries = displayPalette.paletteEntries ?? [];
      const quickAccess = displayPalette.quickAccessTools ?? [];

      const sectionEntries = entries.filter((e) => e.tools && e.tools.length > 0);
      const individualTools = entries.filter((e) => !e.tools && e.id && e.label);

      // Global dedup across all tool slots — prevents React duplicate-key warnings
      const seen = new Set<string>();
      const uniqueQuickAccess: typeof quickAccess = [];
      for (const t of quickAccess) {
        if (!seen.has(t.id)) { seen.add(t.id); uniqueQuickAccess.push(t); }
      }
      for (const t of individualTools) {
        if (!seen.has(t.id)) { seen.add(t.id); uniqueQuickAccess.push(t); }
      }
      // Dedup tools within each section as well
      const dedupedSectionEntries = sectionEntries.map((section) => ({
        ...section,
        tools: (section.tools || []).filter((t: { id: string }) => {
          if (seen.has(t.id)) return false;
          seen.add(t.id);
          return true;
        }),
      })).filter((section) => section.tools.length > 0);

      if (dedupedSectionEntries.length === 0 && uniqueQuickAccess.length === 0) {
        return (
          <div className={classes.emptyState}>
            <BuildCircleOutlinedIcon className={classes.emptyIcon} />
            <Typography className={classes.emptyText}>{'没有可用的工具'}</Typography>
          </div>
        );
      }

      return (
        <>
          {uniqueQuickAccess.length > 0 && (
            <ToolSectionList
              title={'快速访问'}
              tools={uniqueQuickAccess}
              defaultExpanded={true}
              onToolClick={handleToolClick}
            />
          )}
          {uniqueQuickAccess.length > 0 && dedupedSectionEntries.length > 0 && <Divider />}
          {dedupedSectionEntries.map((entry, idx) => (
            <ToolSectionList
              key={entry.id || `${entry.label}-${idx}`}
              title={entry.label}
              tools={entry.tools}
              defaultExpanded={true}
              onToolClick={handleToolClick}
            />
          ))}
        </>
      );
    };

    return (
      <Box className={classes.root}>

        <Box className={classes.contextInfo}>
          <Typography className={classes.contextLabel}>
            {hasSelection ? '选中元素的上下文工具' : '画布级工具'}
          </Typography>
        </Box>

        <Box className={classes.content}>{renderContent()}</Box>
      </Box>
    );
  }
);

/* ------------------------------------------------------------------ */
/*  Tool Section sub-component                                        */
/* ------------------------------------------------------------------ */

interface ToolSectionListProps {
  title: string;
  tools: Array<{ id: string; label: string; iconURL?: string; dialogDescriptionId?: string }>;
  defaultExpanded: boolean;
  onToolClick: (toolId: string, toolLabel: string, dialogDescriptionId?: string) => void;
}

const useSectionStyles = makeStyles()((theme) => ({
  sectionHeader: {
    display: 'flex',
    alignItems: 'center',
    padding: theme.spacing(0.75, 2),
    cursor: 'pointer',
    '&:hover': {
      backgroundColor: theme.palette.action.hover,
    },
    userSelect: 'none',
  },
  sectionTitle: {
    flex: 1,
    fontWeight: 600,
    fontSize: '0.75rem',
    textTransform: 'uppercase',
    letterSpacing: '0.05em',
    color: theme.palette.text.secondary,
  },
  expandIcon: {
    fontSize: '1rem',
    color: theme.palette.text.disabled,
  },
  toolItem: {
    paddingLeft: theme.spacing(4),
    paddingTop: 2,
    paddingBottom: 2,
    borderRadius: 4,
    margin: '1px 6px',
    '&:hover': {
      backgroundColor: theme.palette.action.hover,
    },
  },
  toolItemIcon: {
    minWidth: 28,
    '& .MuiSvgIcon-root': {
      fontSize: '0.875rem',
    },
  },
}));

const ToolSectionList = ({ title, tools, defaultExpanded, onToolClick }: ToolSectionListProps) => {
  const { classes } = useSectionStyles();
  const [expanded, setExpanded] = useState<boolean>(defaultExpanded);

  if (!tools || tools.length === 0) return null;

  return (
    <List dense disablePadding>
      <ListItemButton className={classes.sectionHeader} onClick={() => setExpanded(!expanded)} dense>
        <ListItemText
          primary={title}
          primaryTypographyProps={{ className: classes.sectionTitle }}
        />
        {expanded ? (
          <ExpandLess className={classes.expandIcon} />
        ) : (
          <ExpandMore className={classes.expandIcon} />
        )}
      </ListItemButton>
      {expanded &&
        tools.map((tool) => (
          <ListItemButton
            key={tool.id}
            className={classes.toolItem}
            dense
            onClick={() => onToolClick(tool.id, tool.label, tool.dialogDescriptionId)}
          >
            <ListItemIcon className={classes.toolItemIcon}>
              {tool.iconURL ? (
                <img src={Array.isArray(tool.iconURL) ? tool.iconURL[0] : tool.iconURL} alt="" style={{ width: 18, height: 18 }} />
              ) : (
                <BuildCircleOutlinedIcon sx={{ fontSize: '0.875rem', color: 'action.disabled' }} />
              )}
            </ListItemIcon>
            <ListItemText
              primary={tool.label}
              primaryTypographyProps={{ fontSize: '0.8125rem' }}
            />
          </ListItemButton>
        ))}
    </List>
  );
};
