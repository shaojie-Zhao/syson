import {
  ConnectionTargetHandle, DecoratorContainer, Label,
  useConnectionLineNodeStyle, useConnectorNodeStyle, useDrop, useDropNodeStyle,
  useRefreshConnectionHandles,
} from '@eclipse-sirius/sirius-components-diagrams';
import { Theme, useTheme } from '@mui/material/styles';
import { Handle, Node, NodeProps, Position } from '@xyflow/react';
import React, { Fragment, memo, useEffect, useRef, useState } from 'react';
import { LifelineNodeData, NodeComponentsMap } from './SysMLLifelineNode.types';

const rootStyle = (theme: Theme, style: React.CSSProperties, selected: boolean, hovered: boolean, faded: boolean): React.CSSProperties => ({
  display: 'flex', flexDirection: 'column', alignItems: 'center', padding: '0px',
  width: '100%', height: '100%', position: 'relative', overflow: 'visible',
  opacity: faded ? '0.4' : '', ...style, border: 'none', backgroundColor: 'transparent',
  ...(selected || hovered ? { outline: `${theme.palette.selected} solid 1px` } : {}),
});

export const SysMLLifelineNode: NodeComponentsMap['dodafLifelineNode'] = memo(
  ({ data, id, selected, dragging }: NodeProps<Node<LifelineNodeData>>) => {
    const theme: Theme = useTheme();
    const { onDrop, onDragOver } = useDrop();
    const { style: connectionFeedbackStyle } = useConnectorNodeStyle(id, data.nodeDescription.id);
    const { style: dropFeedbackStyle } = useDropNodeStyle(data.isDropNodeTarget, data.isDragNodeSource, data.isDropNodeCandidate, dragging);
    const { style: connectionLineActiveNodeStyle } = useConnectionLineNodeStyle(data.connectionLinePositionOnNode);
    useRefreshConnectionHandles(id, data.connectionHandles);

    // ---- OV-6c Activation bars (visual + connection handles) ----
    // Driven by the 'seq-activations' CustomEvent dispatched from index.html:
    // the event carries this node's activation bars {key, top, height} in
    // coordinates RELATIVE TO the dashed lifeline container. A source + target
    // handle is placed on the bar top so message edges prefer to snap to the
    // activation bar; when no bar exists the regular lls-/llt- dashed-line
    // handles remain as the fallback connection points.
    type Activation = { top: number; height: number };
    const [activations, setActivations] = useState<Record<string, Activation>>({});
    const dragRef = useRef<{ key: string; y: number; h: number } | null>(null);

    useEffect(() => {
      function onSeqActivations(ev: Event) {
        const d = (ev as CustomEvent).detail;
        if (!d || d.nodeId !== id) return;
        setActivations((prev) => {
          const next = { ...prev };
          (d.acts || []).forEach((a: { key: string; top: number; height: number }) => {
            next[a.key] = { top: a.top, height: a.height };
          });
          return next;
        });
      }
      window.addEventListener('seq-activations', onSeqActivations);
      return () => window.removeEventListener('seq-activations', onSeqActivations);
    }, [id]);

    // Height drag: pointermove/up on window so the drag survives outside the bar.
    useEffect(() => {
      function onMove(ev: PointerEvent) {
        const dr = dragRef.current;
        if (!dr) return;
        const nh = Math.max(20, dr.h + (ev.clientY - dr.y));
        setActivations((prev) => {
          const cur = prev[dr.key];
          if (!cur) return prev;
          const next = { ...prev, [dr.key]: { ...cur, height: nh } };
          try {
            localStorage.setItem('syson_seq_act_' + dr.key, String(nh));
            window.dispatchEvent(new CustomEvent('seq-act-height', { detail: { key: dr.key, height: nh } }));
          } catch (e) {}
          return next;
        });
      }
      function onUp() {
        const dr = dragRef.current;
        if (dr) {
          const h = activations[dr.key]?.height ?? 60;
          try {
            localStorage.setItem('syson_seq_act_' + dr.key, String(h));
            window.dispatchEvent(new CustomEvent('seq-act-height', { detail: { key: dr.key, height: h } }));
          } catch (e) {}
        }
        dragRef.current = null;
      }
      window.addEventListener('pointermove', onMove);
      window.addEventListener('pointerup', onUp);
      return () => {
        window.removeEventListener('pointermove', onMove);
        window.removeEventListener('pointerup', onUp);
      };
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    function startActDrag(e: React.PointerEvent<HTMLDivElement>, key: string) {
      e.preventDefault();
      e.stopPropagation();
      const cur = activations[key];
      dragRef.current = { key, y: e.clientY, h: cur ? cur.height : 60 };
    }

    // Dense hidden handles distributed along the whole dashed lifeline (percentage
    // based so they cover the entire node height regardless of size). These are the
    // interactive connection points a user drags a message from.
    const handlePercent: number[] = [10, 22, 34, 46, 58, 70, 82, 94];

    return (
      <>
        <div style={{ ...rootStyle(theme, data.style, !!selected, data.isHovered, data.faded) }}
             onDragOver={onDragOver} onDrop={(e) => onDrop(e, id)}
             data-testid={`LifelineNode - ${data?.insideLabel?.text ?? ''}`}>
          <DecoratorContainer decorators={data.decorators}></DecoratorContainer>
          <ConnectionTargetHandle nodeId={id} nodeDescription={data.nodeDescription} isHovered={data.isHovered} />
          {/* Head rectangle: contains stereotype + name, sized to its content (UML style) */}
          <div style={{
            display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
            width: 'fit-content', maxWidth: '100%', minHeight: '42px', padding: '4px 10px', boxSizing: 'border-box',
            background: data.style.background ?? '#e2e8f0', border: '1px solid #64748b', borderRadius: '3px',
            ...connectionFeedbackStyle, ...dropFeedbackStyle, ...connectionLineActiveNodeStyle,
          }} data-svg="rect">
            {data.insideLabel ? <Label diagramElementId={id} label={data.insideLabel} faded={data.faded} /> : null}
          </div>
          {/* Vertical dashed lifeline filling remaining node height */}
          <div style={{
            position: 'relative', width: '2px', margin: '0 auto', flex: 1, alignSelf: 'stretch',
            minHeight: '90px',
            background: 'repeating-linear-gradient(to bottom, #334155 0 4px, transparent 4px 8px)',
          }} data-lifeline-dashed="1">
            {handlePercent.map((p) => (
              <Fragment key={p}>
                <Handle
                  id={`lls-${p}`}
                  type="source"
                  position={Position.Left}
                  isConnectable={true}
                  style={{ position: 'absolute', top: `${p}%`, left: -4, transform: 'none', width: 8, height: 8, opacity: 0, pointerEvents: 'all' }}
                />
                <Handle
                  id={`llt-${p}`}
                  type="target"
                  position={Position.Right}
                  isConnectable={true}
                  style={{ position: 'absolute', top: `${p}%`, left: -2, transform: 'none', width: 8, height: 8, opacity: 0, pointerEvents: 'all' }}
                />
              </Fragment>
            ))}
            {/* Re-created Sirius connection handles, placed ON the dashed lifeline so the
                message edges (whose sourceHandle/targetHandle reference these ids) snap to
                the dashed line instead of the node border. Legacy handleLayoutData
                coordinates (XYPosition, relative to the node) are ignored on purpose:
                they were recorded at the old border position and would desync the edge
                endpoints from the dashed line. */}
            {data.connectionHandles.map((ch, i) => {
              const isSource = ch.type === 'source';
              const count = data.connectionHandles.length;
              // Spread handles evenly along the dashed lifeline (12%..84%); no overlap
              // regardless of how many message edges the lifeline carries.
              const topPct = count > 1 ? 12 + (i / (count - 1)) * 72 : 40;
              return (
                <Handle
                  key={ch.id}
                  id={ch.id}
                  type={isSource ? 'source' : 'target'}
                  position={isSource ? Position.Right : Position.Left}
                  isConnectable={true}
                  style={{ position: 'absolute', top: `${topPct}%`, left: -4, transform: 'none', width: 8, height: 8, opacity: 0, pointerEvents: 'all' }}
                />
              );
            })}
            {/* Activation bars: visual rect + top source/target handles. Message
                edges snap to the bar top (preferred); the lls-/llt- handles above
                remain the fallback when no activation bar exists. */}
            {Object.entries(activations).map(([key, a]) => (
              <Fragment key={key}>
                <div
                  data-act-node={key}
                  style={{
                    position: 'absolute', top: a.top, left: -4, width: 8,
                    height: Math.max(12, a.height), background: '#cbd5e1',
                    border: '1px solid #94a3b8', borderRadius: 1, zIndex: 3,
                    cursor: 'ns-resize', boxSizing: 'border-box',
                  }}
                  onPointerDown={(e) => startActDrag(e, key)}
                />
                <Handle
                  id={`act-s-${key}`}
                  type="source"
                  position={Position.Right}
                  isConnectable={true}
                  style={{ position: 'absolute', top: a.top - 4, left: -4, transform: 'none', width: 8, height: 8, opacity: 0, pointerEvents: 'all' }}
                />
                <Handle
                  id={`act-t-${key}`}
                  type="target"
                  position={Position.Left}
                  isConnectable={true}
                  style={{ position: 'absolute', top: a.top - 4, left: -4, transform: 'none', width: 8, height: 8, opacity: 0, pointerEvents: 'all' }}
                />
              </Fragment>
            ))}
          </div>
        </div>
      </>
    );
  }
);
