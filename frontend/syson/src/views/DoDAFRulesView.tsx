import React, { useCallback, useEffect, useRef, useState } from 'react';

const API = '/api/rules/default-rules';

function getEditingContextId(): string {
  const m = window.location.pathname.match(/\/projects\/([^/]+)\/edit/);
  return m ? m[1] : 'default-rules';
}
function getRepresentationId(): string {
  const q = window.location.search.match(/[?&]representation=([^&]+)/);
  if (q) return decodeURIComponent(q[1]);
  const p = window.location.pathname.match(/\/edit\/([^/?#]+)/);
  if (p) return decodeURIComponent(p[1]);
  return 'default-rules';
}

interface Rule {
  id: string; name: string; applied: string; description: string;
  ruleType: string; owner: string; children?: Rule[]; parentId?: string;
}

const thStyle: React.CSSProperties = { background: '#1e293b', color: '#94a3b8', fontSize: 12, fontWeight: 600, padding: '8px 12px', textAlign: 'left', borderBottom: '2px solid rgba(255,255,255,0.1)', whiteSpace: 'nowrap' };
const cellStyle: React.CSSProperties = { padding: '6px 10px', fontSize: 13, color: '#e2e8f0', borderBottom: '1px solid rgba(255,255,255,0.05)' };
const inputStyle: React.CSSProperties = { background: '#0f172a', border: '1px solid rgba(255,255,255,0.12)', borderRadius: 4, color: '#e2e8f0', padding: '6px 10px', fontSize: 13, width: '100%', boxSizing: 'border-box' };
const btnStyle: React.CSSProperties = { background: '#3b82f6', color: '#fff', border: 'none', borderRadius: 6, padding: '8px 16px', cursor: 'pointer', fontSize: 13, fontWeight: 500, display: 'flex', alignItems: 'center', gap: 6 };
const btnDanger: React.CSSProperties = { ...btnStyle, background: '#ef4444' };
const btnSmall: React.CSSProperties = { ...btnStyle, padding: '4px 10px', fontSize: 11, borderRadius: 3 };

export default function DoDAFRulesView() {
  const ctxId = getEditingContextId();
  const repId = getRepresentationId();
  const [rules, setRules] = useState<Rule[]>([]);
  const [filtered, setFiltered] = useState<Rule[]>([]);
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [editing, setEditing] = useState<{ id: string; field: string } | null>(null);
  const [editVal, setEditVal] = useState('');
  const [syncLoading, setSyncLoading] = useState(false);
  const [deleteDialog, setDeleteDialog] = useState<{ open: boolean; count: number }>({ open: false, count: 0 });
  const [toast, setToast] = useState<string | null>(null);
  const showToast = (msg: string) => { setToast(msg); setTimeout(() => setToast(null), 2500); };

  // Filters
  const [scopeFilter, setScopeFilter] = useState('');
  const [typeFilter, setTypeFilter] = useState('');
  const [appliedFilter, setAppliedFilter] = useState('');
  const [searchQuery, setSearchQuery] = useState('');

  // Tree state
  const [expanded, setExpanded] = useState<Set<string>>(new Set());

  const load = useCallback(async () => {
    try {
      const r = await fetch(API + '/list?ctxId=' + ctxId).then(r => r.json());
      setRules(r);
      setFiltered(r);
    } catch(e) { console.error(e); }
  }, [ctxId]);

  useEffect(() => { load(); }, [load]);

  // Apply filters
  useEffect(() => {
    let result = [...rules];
    if (scopeFilter) result = result.filter(r => (r.applied || '').includes(scopeFilter));
    if (typeFilter) result = result.filter(r => (r.ruleType || '').includes(typeFilter));
    if (appliedFilter) result = result.filter(r => (r.applied || '') === appliedFilter);
    if (searchQuery) {
      const q = searchQuery.toLowerCase();
      result = result.filter(r =>
        (r.name || '').toLowerCase().includes(q) || (r.applied || '').toLowerCase().includes(q) ||
        (r.description || '').toLowerCase().includes(q) || (r.ruleType || '').toLowerCase().includes(q) ||
        (r.owner || '').toLowerCase().includes(q));
    }
    setFiltered(result);
  }, [rules, scopeFilter, typeFilter, appliedFilter, searchQuery]);

  const handleAdd = async (parentId?: string) => {
    try {
      await fetch(API + '/add', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ ctxId, parentId: parentId || null })
      });
      load();
    } catch(e) {}
  };

  const handleDelete = async () => {
    if (selected.size === 0) { showToast('请先选择要删除的数据行'); return; }
    setDeleteDialog({ open: true, count: selected.size });
  };

  const handleSync = async () => {
    setSyncLoading(true);
    try { await fetch(API + '/sync?ctxId=' + ctxId, { method: 'POST' }); load(); } catch(e) {}
    setSyncLoading(false);
  };

  const handleExport = () => {
    const csv = ['序号,应用于,名称,规则说明,规则种类,所有者',
      ...filtered.map((r, i) => `${i + 1},"${r.applied || ''}","${r.name || ''}","${r.description || ''}","${r.ruleType || ''}","${r.owner || ''}"`)
    ].join('\n');
    const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a'); a.href = url; a.download = 'rules.csv'; a.click();
  };

  const handleEdit = async (id: string, field: string, value: string) => {
    try {
      await fetch(API + '/' + id, {
        method: 'PUT', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ ctxId, field, value })
      });
      load();
    } catch(e) {}
    setEditing(null);
  };

  const toggleExpand = (id: string) => {
    setExpanded(prev => { const next = new Set(prev); if (next.has(id)) next.delete(id); else next.add(id); return next; });
  };

  const toggleSelect = (id: string) => {
    setSelected(prev => { const next = new Set(prev); if (next.has(id)) next.delete(id); else next.add(id); return next; });
  };

  var _rowIdx = 0;
  const renderRows = (items: Rule[], depth: number = 0): React.ReactNode[] => {
    if (depth === 0) _rowIdx = 0;
    const result: React.ReactNode[] = [];
    for (const r of items) {
      _rowIdx++;
      var idx = _rowIdx;
      const hasChildren = r.children && r.children.length > 0;
      const isExpanded = expanded.has(r.id);
      const isSelected = selected.has(r.id);
      result.push(
        <tr key={r.id} style={{ background: isSelected ? 'rgba(59,130,246,0.12)' : (depth % 2 ? 'rgba(255,255,255,0.01)' : 'transparent') }}
          onMouseEnter={e => { if (!isSelected) (e.currentTarget as HTMLElement).style.background = 'rgba(255,255,255,0.04)'; }}
          onMouseLeave={e => { if (!isSelected) (e.currentTarget as HTMLElement).style.background = depth % 2 ? 'rgba(255,255,255,0.01)' : 'transparent'; }}>
          <td style={cellStyle}><input type="checkbox" checked={isSelected} onChange={() => toggleSelect(r.id)} /></td>
          <td style={{ ...cellStyle, width: 50, textAlign: 'center', color: '#94a3b8' }}>{idx}</td>
          <td style={{ ...cellStyle, paddingLeft: 10 + depth * 20 }}>
            {hasChildren && <span onClick={() => toggleExpand(r.id)} style={{ cursor: 'pointer', marginRight: 4, color: '#3b82f6' }}>{isExpanded ? '▼' : '▶'}</span>}
            {editing?.id === r.id && editing?.field === 'applied' ?
              <input autoFocus value={editVal} onChange={e => setEditVal(e.target.value)} onBlur={() => handleEdit(r.id, 'applied', editVal)} onKeyDown={e => { if (e.key === 'Enter') handleEdit(r.id, 'applied', editVal); if (e.key === 'Escape') setEditing(null); }} style={{ ...inputStyle, width: '80%' }} />
              : <span onClick={() => { setEditing({ id: r.id, field: 'applied' }); setEditVal(r.applied || ''); }} style={{ color: r.applied ? '#e2e8f0' : '#64748b', cursor: 'pointer' }}>{r.applied || '双击编辑...'}</span>}
            <span style={{ marginLeft: 6 }}>{hasChildren && <><button onClick={() => handleAdd(r.id)} style={{ ...btnSmall, background: 'rgba(59,130,246,0.15)', color: '#60a5fa' }} title="添加子行">+</button></>}</span>
          </td>
          {(['name', 'description', 'owner'] as const).map(f => (
            <td key={f} style={cellStyle} onDoubleClick={() => { setEditing({ id: r.id, field: f }); setEditVal(r[f] || ''); }}>
              {editing?.id === r.id && editing?.field === f ?
                <input autoFocus value={editVal} onChange={e => setEditVal(e.target.value)} onBlur={() => handleEdit(r.id, f, editVal)} onKeyDown={e => { if (e.key === 'Enter') handleEdit(r.id, f, editVal); if (e.key === 'Escape') setEditing(null); }} style={inputStyle} />
                : <span style={{ color: r[f] ? '#e2e8f0' : '#64748b' }}>{r[f] || '双击编辑...'}</span>}
            </td>
          ))}
          <td style={cellStyle} onDoubleClick={() => { setEditing({ id: r.id, field: 'ruleType' }); setEditVal(r.ruleType || ''); }}>
            {editing?.id === r.id && editing?.field === 'ruleType' ?
              <input autoFocus value={editVal} onChange={e => setEditVal(e.target.value)} onBlur={() => handleEdit(r.id, 'ruleType', editVal)} onKeyDown={e => { if (e.key === 'Enter') handleEdit(r.id, 'ruleType', editVal); if (e.key === 'Escape') setEditing(null); }} style={inputStyle} />
              : <span style={{ color: r.ruleType ? '#e2e8f0' : '#64748b' }}>{r.ruleType || '双击编辑...'}</span>}
          </td>
        </tr>
      );
      if (hasChildren && isExpanded) result.push(...renderRows(r.children!, depth + 1));
    }
    return result;
  };

  const appliedOptions = [...new Set(rules.map(r => r.applied).filter(Boolean))];

  return (
    <div style={{ height: '100%', display: 'flex', flexDirection: 'column', background: '#111827', color: '#e0e0e0', fontFamily: 'system-ui, sans-serif', fontSize: 13 }}>
      {toast && <div style={{ position: 'fixed', top: 16, left: '50%', transform: 'translateX(-50%)', zIndex: 9999, background: '#ef4444', color: '#fff', padding: '10px 24px', borderRadius: 8, fontSize: 14, boxShadow: '0 4px 12px rgba(0,0,0,0.3)' }}>{toast}</div>}
      {/* Toolbar */}
      <div style={{ display: 'flex', gap: 8, padding: '10px 16px', borderBottom: '1px solid rgba(255,255,255,0.08)' }}>
        <button onClick={() => handleAdd()} style={btnStyle}>＋ 添加行</button>
        <button onClick={handleDelete} style={btnDanger}>🗑 删除行</button>
        <button onClick={handleSync} disabled={syncLoading} style={{ ...btnStyle, background: '#6366f1' }}>{syncLoading ? '同步中...' : '🔄 同步'}</button>
        <button onClick={handleExport} style={{ ...btnStyle, background: '#10b981' }}>📥 导出</button>
      </div>
      {/* Filters */}
      <div style={{ display: 'flex', gap: 12, padding: '8px 16px', borderBottom: '1px solid rgba(255,255,255,0.06)', alignItems: 'center', flexWrap: 'wrap' }}>
        <span style={{ color: '#94a3b8', fontSize: 12 }}>范围:</span>
        <input value={scopeFilter} onChange={e => setScopeFilter(e.target.value)} placeholder="模糊搜索..." style={{ ...inputStyle, width: 120 }} />
        <span style={{ color: '#94a3b8', fontSize: 12 }}>类型:</span>
        <input value={typeFilter} onChange={e => setTypeFilter(e.target.value)} placeholder="模糊搜索..." style={{ ...inputStyle, width: 120 }} />
        <span style={{ color: '#94a3b8', fontSize: 12, marginLeft: 16 }}>过滤:</span>
        <select value={appliedFilter} onChange={e => setAppliedFilter(e.target.value)} style={{ ...inputStyle, width: 140, cursor: 'pointer' }}>
          <option value="">应用于</option>
          {appliedOptions.map(o => <option key={o} value={o}>{o}</option>)}
        </select>
        <input value={searchQuery} onChange={e => setSearchQuery(e.target.value)} placeholder="全局搜索..." style={{ ...inputStyle, width: 200 }} />
        <button onClick={() => {}} style={{ ...btnSmall, padding: '6px 10px' }}>🔍</button>
      </div>
      {/* Delete Confirmation Dialog */}
      {deleteDialog.open && (
        <div style={{ position: 'fixed', inset: 0, zIndex: 9999, display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'rgba(0,0,0,0.5)' }} onClick={() => setDeleteDialog({ open: false, count: 0 })}>
          <div style={{ background: '#1e293b', borderRadius: 12, padding: '24px 32px', minWidth: 360, boxShadow: '0 8px 32px rgba(0,0,0,0.5)', border: '1px solid rgba(255,255,255,0.1)' }} onClick={e => e.stopPropagation()}>
            <h3 style={{ margin: '0 0 12px', color: '#f1f5f9', fontSize: 16 }}>确认删除</h3>
            <p style={{ margin: '0 0 20px', color: '#94a3b8', fontSize: 14 }}>确定删除选中的 {deleteDialog.count} 行数据？此操作不可撤销。</p>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10 }}>
              <button onClick={() => setDeleteDialog({ open: false, count: 0 })} style={{ background: '#334155', color: '#cbd5e1', border: 'none', borderRadius: 6, padding: '8px 20px', cursor: 'pointer', fontSize: 13 }}>取消</button>
              <button onClick={async () => {
                setDeleteDialog({ open: false, count: 0 });
                for (const id of selected) { await fetch(API + '/' + id, { method: 'DELETE', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ ctxId }) }).catch(() => {}); }
                setSelected(new Set());
                load();
              }} style={{ background: '#ef4444', color: '#fff', border: 'none', borderRadius: 6, padding: '8px 20px', cursor: 'pointer', fontSize: 13 }}>确认删除</button>
            </div>
          </div>
        </div>
      )}
      {/* Table */}
      <div style={{ flex: 1, overflow: 'auto', position: 'relative' }}>
        <button onClick={() => handleAdd()} style={{ ...btnStyle, position: 'absolute', top: 8, left: 8, zIndex: 10, padding: '6px 14px', fontSize: 12 }}>＋ 添加行</button>
        {filtered.length === 0 ? (
          <div style={{ textAlign: 'center', padding: 80, color: '#64748b' }}>暂无规则数据<br/><br/><button onClick={() => handleAdd()} style={btnStyle}>＋ 添加行</button></div>
        ) : (
          <table style={{ width: '100%', borderCollapse: 'collapse', tableLayout: 'auto' }}>
            <thead>
              <tr>
                <th style={{ ...thStyle, width: 40 }}></th>
                <th style={{ ...thStyle, width: 50 }}>序号</th>
                <th style={{ ...thStyle, minWidth: 180 }}>应用于</th>
                <th style={{ ...thStyle, minWidth: 160 }}>名称</th>
                <th style={{ ...thStyle, minWidth: 240 }}>规则说明</th>
                <th style={{ ...thStyle, minWidth: 120 }}>规则种类</th>
                <th style={{ ...thStyle, minWidth: 120 }}>所有者</th>
              </tr>
            </thead>
            <tbody>{renderRows(filtered)}</tbody>
          </table>
        )}
      </div>
    </div>
  );
}
