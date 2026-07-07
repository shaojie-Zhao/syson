import React, { useState, useCallback, useRef, useEffect } from 'react';
import Draggable from 'react-draggable';

const C = { bg: '#0F172A', panel: '#1E293B', border: '#334155', text: '#E2E8F0', accent: '#3B82F6', red: '#EF4444', blue: '#3B82F6', hover: 'rgba(59,130,246,0.15)', active: 'rgba(59,130,246,0.3)' };

interface Ov1Node { id: string; x: number; y: number; iconId: string; iconUrl: string; label: string; faction: string; }
interface Ov1Edge { id: string; from: string; to: string; }
interface IconDef { id: string; name: string; url: string; }
interface IconCat { id: string; label: string; icons: IconDef[]; }
interface Faction { id: string; label: string; categories: IconCat[]; }

export const Ov1ConceptView: React.FC = () => {
  const [nodes, setNodes] = useState<Ov1Node[]>([]);
  const [edges, setEdges] = useState<Ov1Edge[]>([]);
  const [factions, setFactions] = useState<Faction[]>([]);
  const [activeFactionId, setActiveFactionId] = useState('red');
  const [selectedCatId, setSelectedCatId] = useState<string | null>(null);
  const [previewIcon, setPreviewIcon] = useState<IconDef | null>(null);
  const [edgeMode, setEdgeMode] = useState(false);
  const [label, setLabel] = useState('');
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const canvasRef = useRef<HTMLDivElement>(null);
  const nextId = useRef(1);

  useEffect(() => { fetch('/api/ov1/icons').then(r => r.json()).then(d => setFactions(d.factions || [])).catch(() => {}); }, []);

  const activeFaction = factions.find(f => f.id === activeFactionId);

  const addNode = useCallback((iconUrl: string, iconId: string, defaultLabel: string) => {
    const c = canvasRef.current;
    const cx = c ? c.clientWidth / 2 + (Math.random() - 0.5) * 200 : 200;
    const cy = c ? c.clientHeight / 2 + (Math.random() - 0.5) * 200 : 200;
    setNodes(prev => [...prev, { id: 'n' + (nextId.current++), x: cx, y: cy, iconId, iconUrl, label: label || defaultLabel, faction: activeFactionId }]);
    setLabel('');
  }, [label, activeFactionId]);

  const deleteNode = useCallback((id: string) => {
    setNodes(prev => prev.filter(n => n.id !== id));
    setEdges(prev => prev.filter(e => e.from !== id && e.to !== id));
    if (selectedId === id) setSelectedId(null);
  }, [selectedId]);

  const handleCanvasClick = (e: React.MouseEvent) => {
    if (e.target === canvasRef.current) {
      setSelectedId(null); setPreviewIcon(null); setSelectedCatId(null);
    }
  };

  const handleNodeClick = useCallback((nodeId: string, e: React.MouseEvent) => {
    e.stopPropagation();
    if (edgeMode) {
      if (e.shiftKey) { deleteNode(nodeId); return; }
      setSelectedId(nodeId);
      return;
    }
    setSelectedId(prev => {
      if (!prev || prev === nodeId) return nodeId;
      setEdges(es => es.some(e => (e.from === prev && e.to === nodeId) || (e.from === nodeId && e.to === prev))
        ? es : [...es, { id: 'e' + (nextId.current++), from: prev, to: nodeId }]);
      return nodeId;
    });
  }, [edgeMode, deleteNode]);

  const getLine = (e: Ov1Edge) => {
    const f = nodes.find(n => n.id === e.from), t = nodes.find(n => n.id === e.to);
    return f && t ? { x1: f.x + 40, y1: f.y + 30, x2: t.x + 40, y2: t.y + 30 } : null;
  };

  return (
    <div style={{ display: 'flex', height: '100%', background: C.bg, color: C.text, fontFamily: "'Segoe UI',system-ui,sans-serif", fontSize: 13 }}>
      {/* LEFT PANEL: Tree (top) + Preview (bottom) */}
      <div style={{ width: 260, minWidth: 260, background: C.panel, borderRight: '1px solid ' + C.border, display: 'flex', flexDirection: 'column' }}>
        {/* Faction tabs */}
        <div style={{ display: 'flex', padding: '8px 8px 0', gap: 4 }}>
          {factions.map(f => (
            <button key={f.id} onClick={() => { setActiveFactionId(f.id); setSelectedCatId(null); setPreviewIcon(null); }}
              style={{ flex: 1, padding: '6px 4px', border: 'none', borderRadius: '4px 4px 0 0', cursor: 'pointer', fontSize: 12, fontWeight: 600,
                background: activeFactionId === f.id ? (f.id === 'red' ? '#991B1B' : f.id === 'blue' ? '#1E3A5F' : '#334155') : '#0F172A',
                color: activeFactionId === f.id ? '#fff' : '#94A3B8' }}>{f.label}</button>
          ))}
        </div>
        {/* Category tree */}
        <div style={{ flex: 1, overflowY: 'auto', padding: '0 8px 8px', borderBottom: '1px solid ' + C.border }}>
          {activeFaction?.categories.map(cat => (
            <div key={cat.id}>
              <div onClick={() => { setSelectedCatId(selectedCatId === cat.id ? null : cat.id); setPreviewIcon(null); }}
                style={{ display: 'flex', alignItems: 'center', gap: 6, padding: '7px 8px', cursor: 'pointer', borderRadius: 4, marginTop: 2,
                  background: selectedCatId === cat.id ? C.active : 'transparent', color: selectedCatId === cat.id ? C.accent : C.text }}>
                <span style={{ fontSize: 10, transition: 'transform .2s', transform: selectedCatId === cat.id ? 'rotate(90deg)' : 'none' }}>▶</span>
                <span style={{ fontSize: 11, opacity: 0.6 }}>📁</span>
                <span style={{ fontWeight: 500 }}>{cat.label}</span>
                <span style={{ marginLeft: 'auto', fontSize: 10, color: '#64748B' }}>{cat.icons.length}</span>
              </div>
              {selectedCatId === cat.id && cat.icons.map(icon => (
                <div key={icon.id} onClick={(e) => { e.stopPropagation(); setPreviewIcon(icon); }}
                  style={{ display: 'flex', alignItems: 'center', gap: 6, padding: '6px 8px 6px 28px', cursor: 'pointer', borderRadius: 4,
                    background: previewIcon?.id === icon.id ? C.active : 'transparent', fontSize: 12 }}>
                  <span style={{ opacity: 0.5 }}>🖼</span>
                  <span>{icon.name}</span>
                </div>
              ))}
            </div>
          ))}
        </div>
        {/* Preview area */}
        <div style={{ height: 200, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', padding: 12, gap: 8 }}>
          {previewIcon ? (
            <>
              <img src={previewIcon.url} alt={previewIcon.name} style={{ width: 64, height: 64, imageRendering: 'auto' }} />
              <div style={{ fontSize: 12, fontWeight: 600 }}>{previewIcon.name}</div>
              <div style={{ fontSize: 10, color: '#94A3B8' }}>{previewIcon.id}</div>
              <button onClick={() => addNode(previewIcon.url, previewIcon.id, previewIcon.name)}
                style={{ padding: '6px 20px', border: 'none', borderRadius: 6, cursor: 'pointer', background: C.accent, color: '#fff', fontSize: 12, fontWeight: 600 }}>
                + 添加到画布
              </button>
            </>
          ) : (
            <span style={{ color: '#475569', fontSize: 12 }}>选择图标预览</span>
          )}
        </div>
        {/* Toolbar */}
        <div style={{ padding: 8, borderTop: '1px solid ' + C.border, display: 'flex', flexDirection: 'column', gap: 6 }}>
          <input value={label} onChange={e => setLabel(e.target.value)} placeholder="节点标签（可选）"
            style={{ padding: '6px 8px', fontSize: 12, borderRadius: 4, border: '1px solid ' + C.border, background: C.bg, color: C.text }} />
          <div style={{ display: 'flex', gap: 6 }}>
            <button onClick={() => setEdgeMode(!edgeMode)}
              style={{ flex: 1, padding: '6px 0', border: 'none', borderRadius: 4, cursor: 'pointer', fontSize: 11, fontWeight: 600,
                background: edgeMode ? C.accent : C.border, color: edgeMode ? '#fff' : '#94A3B8' }}>
              {edgeMode ? '连线模式 (ON)' : '连线模式'}
            </button>
            <button onClick={() => { setNodes([]); setEdges([]); setSelectedId(null); setPreviewIcon(null); }}
              style={{ padding: '6px 14px', border: 'none', borderRadius: 4, cursor: 'pointer', fontSize: 11, background: C.red, color: '#fff', fontWeight: 600 }}>清空</button>
          </div>
          {selectedId && (
            <div style={{ display: 'flex', gap: 6, alignItems: 'center', fontSize: 11 }}>
              <span style={{ color: '#94A3B8' }}>已选: {selectedId}</span>
              <button onClick={() => deleteNode(selectedId)}
                style={{ background: 'transparent', color: C.red, border: 'none', cursor: 'pointer', fontSize: 11 }}>删除</button>
              <button onClick={() => setSelectedId(null)}
                style={{ background: 'transparent', color: '#94A3B8', border: 'none', cursor: 'pointer', fontSize: 11 }}>取消</button>
            </div>
          )}
        </div>
      </div>
      {/* CANVAS */}
      <div ref={canvasRef} onClick={handleCanvasClick}
        style={{ flex: 1, position: 'relative', overflow: 'hidden', background: 'radial-gradient(circle at 50% 50%, #1a2a4a 0%, #0F172A 70%)' }}>
        <svg style={{ position: 'absolute', top: 0, left: 0, width: '100%', height: '100%', pointerEvents: 'none', zIndex: 1 }}>
          {edges.map(e => { const l = getLine(e); return l ? (
            <g key={e.id}>
              <line {...l} stroke={C.border} strokeWidth={2} markerEnd="url(#arrow)" />
            </g>
          ) : null; })}
          <defs><marker id="arrow" viewBox="0 0 10 10" refX={8} refY={5} markerWidth={6} markerHeight={6} orient="auto-start-reverse"><path d="M 0 0 L 10 5 L 0 10 z" fill={C.border} /></marker></defs>
        </svg>
        {nodes.map(node => (
          <Draggable key={node.id} position={{ x: node.x, y: node.y }}
            onDrag={(_, d) => setNodes(prev => prev.map(n => n.id === node.id ? { ...n, x: d.x, y: d.y } : n))}>
            <div onClick={(e) => handleNodeClick(node.id, e)}
              onContextMenu={e => { e.preventDefault(); deleteNode(node.id); }}
              style={{ position: 'absolute', width: 80, height: 90, cursor: edgeMode ? 'crosshair' : 'pointer', zIndex: 2,
                borderRadius: 10, padding: 4, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
                background: selectedId === node.id ? 'rgba(59,130,246,0.25)' : 'rgba(15,23,42,0.6)',
                border: selectedId === node.id ? '2px solid #3B82F6' : '1px solid rgba(148,163,184,0.2)',
                backdropFilter: 'blur(4px)' }}>
              <img src={node.iconUrl} alt="" style={{ width: 44, height: 44, filter: 'drop-shadow(0 2px 4px rgba(0,0,0,.4))' }} />
              <span style={{ fontSize: 10, color: C.text, textAlign: 'center', marginTop: 3, maxWidth: 76, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{node.label}</span>
            </div>
          </Draggable>
        ))}
        {nodes.length === 0 && (
          <div style={{ position: 'absolute', top: '50%', left: '50%', transform: 'translate(-50%,-50%)', color: '#334155', fontSize: 15, textAlign: 'center', pointerEvents: 'none' }}>
            <div style={{ fontSize: 40, marginBottom: 12 }}>🗺️</div>
            <div>选择左侧图标 → 预览 → 添加到画布</div>
            <div style={{ fontSize: 11, marginTop: 6, color: '#1E293B' }}>单击节点选中 | 连续点击两节点建立连线 | Shift+Click 删除 | 右键删除</div>
          </div>
        )}
      </div>
    </div>
  );
};
