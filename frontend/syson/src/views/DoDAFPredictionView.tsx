import React, { useCallback, useEffect, useState } from 'react';

const API = '/api/prediction/default-prediction';

interface Row { id: string; domain: string; skill: string; shortTerm: string; midTerm: string; longTerm: string; }

export default function DoDAFPredictionView() {
  const [rows, setRows] = useState<Row[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [editing, setEditing] = useState<{ id: string; field: string } | null>(null);
  const [editVal, setEditVal] = useState('');
  const [toast, setToast] = useState<string | null>(null);
  const [deleteDialog, setDeleteDialog] = useState<{ open: boolean; count: number }>({ open: false, count: 0 });
  const showToast = (msg: string) => { setToast(msg); setTimeout(() => setToast(null), 2500); };

  const load = useCallback(async () => {
    try { const r = await fetch(API + '/list').then(r => r.json()); setRows(r); } catch(e) {}
  }, []);
  useEffect(() => { load(); }, [load]);

  const filtered = rows.filter(r => {
    if (!searchQuery) return true;
    const q = searchQuery.toLowerCase();
    return ['domain','skill','shortTerm','midTerm','longTerm'].some(f => (r[f as keyof Row]||'').toLowerCase().includes(q));
  });

  const handleAdd = async () => {
    try { await fetch(API + '/add', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: '{"domain":"","skill":"","shortTerm":"","midTerm":"","longTerm":""}' }); load(); } catch(e) {}
  };
  const handleDelete = async () => {
    for (const id of selected) { await fetch(API + '/' + id, { method: 'DELETE' }).catch(() => {}); }
    setSelected(new Set()); load(); setDeleteDialog({ open: false, count: 0 });
  };
  const handleEdit = async (id: string, field: string, value: string) => {
    try { await fetch(API + '/' + id, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ field, value }) }); load(); setEditing(null); } catch(e) {}
  };
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const toggleSelect = (id: string) => { const s = new Set(selected); if (s.has(id)) s.delete(id); else s.add(id); setSelected(s); };

  const btnStyle: React.CSSProperties = { background: '#3b82f6', color: '#fff', border: 'none', borderRadius: 6, padding: '6px 14px', cursor: 'pointer', fontSize: 13 };
  const inputStyle: React.CSSProperties = { background: '#162242', border: '1px solid rgba(255,255,255,0.12)', borderRadius: 4, color: '#e2e8f0', padding: '6px 10px', fontSize: 13, width: '100%', boxSizing: 'border-box' };
  const cellStyle: React.CSSProperties = { padding: '6px 10px', borderBottom: '1px solid rgba(255,255,255,0.04)', fontSize: 13, borderRight: '1px solid rgba(255,255,255,0.04)' };
  const thStyle: React.CSSProperties = { ...cellStyle, background: '#19284f', fontWeight: 600, whiteSpace: 'nowrap', color: '#fff', textAlign: 'center', verticalAlign: 'middle' };

  return (
    <div id="prediction-wrapper" style={{ height: '100%', display: 'flex', flexDirection: 'column', background: '#19284f', color: '#e0e0e0', fontFamily: 'system-ui, sans-serif', fontSize: 13 }}>
      {toast && <div style={{ position: 'fixed', top: 16, left: '50%', transform: 'translateX(-50%)', zIndex: 9999, background: '#ef4444', color: '#fff', padding: '10px 24px', borderRadius: 8, fontSize: 14, boxShadow: '0 4px 12px rgba(0,0,0,0.3)' }}>{toast}</div>}
      {/* Toolbar */}
      <div style={{ display: 'flex', gap: 8, padding: '10px 16px', borderBottom: '1px solid rgba(255,255,255,0.08)' }}>
        <button onClick={handleAdd} style={btnStyle}>＋ 添加行</button>
        <button onClick={() => { if (selected.size === 0) { showToast('请先选择要删除的数据行'); return; } setDeleteDialog({ open: true, count: selected.size }); }} style={{ ...btnStyle, background: '#ef4444' }}>🗑 删除行</button>
        <span style={{ flex: 1 }} />
        <input value={searchQuery} onChange={e => setSearchQuery(e.target.value)} placeholder="全局搜索..." style={{ ...inputStyle, width: 200 }} />
      </div>
      {/* Delete Dialog */}
      {deleteDialog.open && (
        <div style={{ position: 'fixed', inset: 0, zIndex: 9999, display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'rgba(0,0,0,0.5)' }} onClick={() => setDeleteDialog({ open: false, count: 0 })}>
          <div style={{ background: '#19284f', borderRadius: 12, padding: '24px 32px', minWidth: 360, boxShadow: '0 8px 32px rgba(0,0,0,0.5)', border: '1px solid rgba(255,255,255,0.1)' }} onClick={e => e.stopPropagation()}>
            <h3 style={{ margin: '0 0 12px', color: '#f1f5f9', fontSize: 16 }}>确认删除</h3>
            <p style={{ margin: '0 0 20px', color: '#94a3b8', fontSize: 14 }}>确定删除选中的 {deleteDialog.count} 行数据？</p>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10 }}>
              <button onClick={() => setDeleteDialog({ open: false, count: 0 })} style={{ background: '#334155', color: '#cbd5e1', border: 'none', borderRadius: 6, padding: '8px 20px', cursor: 'pointer', fontSize: 13 }}>取消</button>
              <button onClick={handleDelete} style={{ background: '#ef4444', color: '#fff', border: 'none', borderRadius: 6, padding: '8px 20px', cursor: 'pointer', fontSize: 13 }}>确认删除</button>
            </div>
          </div>
        </div>
      )}
      {/* Table */}
      <div style={{ flex: 1, overflow: 'auto' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
          <thead>
            <tr>
              <th rowSpan={2} style={{ ...thStyle, width: 40 }}></th>
              <th rowSpan={2} style={{ ...thStyle, width: 50 }}>序号</th>
              <th rowSpan={2} style={thStyle}>技术和技能领域</th>
              <th rowSpan={2} style={thStyle}>技术和技能</th>
              <th colSpan={3} style={thStyle}>技术和技能预测</th>
            </tr>
            <tr>
              <th style={thStyle}>短期</th>
              <th style={thStyle}>中期</th>
              <th style={thStyle}>长期</th>
            </tr>
          </thead>
          <tbody>
            {filtered.map((r, i) => (
              <tr key={r.id} style={{ background: i % 2 === 0 ? '#19284f' : '#162242' }}>
                <td style={cellStyle}><input type="checkbox" checked={selected.has(r.id)} onChange={() => toggleSelect(r.id)} /></td>
                <td style={{ ...cellStyle, textAlign: 'center', color: '#94a3b8' }}>{i + 1}</td>
                {(['domain','skill','shortTerm','midTerm','longTerm'] as const).map(f => (
                  <td key={f} style={cellStyle} onDoubleClick={() => { setEditing({ id: r.id, field: f }); setEditVal(r[f] || ''); }}>
                    {editing?.id === r.id && editing?.field === f ?
                      <input autoFocus value={editVal} onChange={e => setEditVal(e.target.value)} onBlur={() => handleEdit(r.id, f, editVal)} onKeyDown={e => { if (e.key === 'Enter') handleEdit(r.id, f, editVal); if (e.key === 'Escape') setEditing(null); }} style={inputStyle} />
                      : <span style={{ color: r[f] ? '#e2e8f0' : '#64748b' }}>{r[f] || '双击编辑...'}</span>}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
