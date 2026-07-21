import React, { useCallback, useEffect, useState } from 'react';

const API = '/api/rules/default-rules';
function getEditingContextId(): string {
  const m = window.location.pathname.match(/\/projects\/([^/]+)\/edit/);
  return m ? m[1] : 'default-rules';
}

interface Rule { id: string; name: string; applied: string; description: string; ruleType: string; owner: string; parentId?: string; children?: Rule[]; }

export default function DoDAFRulesView() {
  const ctxId = getEditingContextId();
  const [rules, setRules] = useState<Rule[]>([]);
  const [filtered, setFiltered] = useState<Rule[]>([]);
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [editing, setEditing] = useState<{ id: string; field: string } | null>(null);
  const [editVal, setEditVal] = useState('');
  const [syncLoading, setSyncLoading] = useState(false);
  const [deleteDialog, setDeleteDialog] = useState<{ open: boolean; count: number }>({ open: false, count: 0 });
  const [toast, setToast] = useState<string | null>(null);
  const showToast = (msg: string) => { setToast(msg); setTimeout(() => setToast(null), 2500); };
  // Hide right side panel on mount, restore on unmount
  useEffect(() => {
    var rp = document.getElementById('right');
    var sr = document.querySelector('[data-testid="site-right"]');
    var rr = document.querySelector('[data-testid="right-resizer"]');
    var savedFlex = rp ? rp.style.flex : '';
    var savedDisplay = sr ? sr.style.display : '';
    if (rp) { rp.style.flex = '0 0 0px'; rp.style.maxWidth = '0px'; rp.style.minWidth = '0px'; rp.style.overflow = 'hidden'; }
    if (sr) sr.style.display = 'none';
    if (rr) rr.style.display = 'none';
    return function() {
      if (rp) { rp.style.flex = savedFlex; rp.style.maxWidth = ''; rp.style.minWidth = ''; rp.style.overflow = ''; }
      if (sr) sr.style.display = savedDisplay;
      if (rr) rr.style.display = '';
    };
  }, []);

  // Tree picker
  const [treePicker, setTreePicker] = useState<{ open: boolean; ruleId: string }>({ open: false, ruleId: '' });
  const [treeNodes, setTreeNodes] = useState<{id:string;label:string;type:string}[]>([]);
  const [treeSearch, setTreeSearch] = useState('');
  const [treeLoading, setTreeLoading] = useState(false);
  const [treeSelected, setTreeSelected] = useState<Set<string>>(new Set());
  const openTreePicker = (ruleId: string) => {
    setTreePicker({ open: true, ruleId });
    setTreeSearch('');
    setTreeSelected(new Set());
    setTreeLoading(true);
    var params = new URLSearchParams();
    params.set('ctxId', ctxId);
    if (scopeId) params.set('scope', scopeId);
    if (typeFilter) params.set('types', typeFilter);
    fetch(API + '/elements?' + params.toString())
      .then(r => r.json()).then(d => {
        var nodes = (d || []).filter(function(n: any) { return n.type !== '__meta__' && (n.name || n.label); })
          .map(function(n: any) { return { id: n.id, label: n.name || n.label, type: n.dodafType || n.type || '' }; });
        if (nodes.length === 0) {
          // Fallback: read from DOM
          var items = document.querySelectorAll('[data-treeitemid]');
          var seen2 = new Set<string>();
          items.forEach(function(el: any) {
            var id = el.getAttribute('data-treeitemid'); if (!id || seen2.has(id)) return; seen2.add(id);
            var labelEl = el.querySelector('[class*="MuiTreeItem-label"]') || el.querySelector('[class*="label"]') || el;
            var label = (labelEl.textContent || '').trim().replace(/\s+/g, ' ');
            if (id && label) nodes.push({ id: id, label: label, type: '' });
          });
        }
        setTreeNodes(nodes);
      }).catch(function() { setTreeNodes([]); }).finally(function() { setTreeLoading(false); });
  };
  const selectTreeNode = (nodeId: string) => {
    var s = new Set(treeSelected);
    if (s.has(nodeId)) s.delete(nodeId); else s.add(nodeId);
    setTreeSelected(s);
  };
  const confirmTreeSelection = async () => {
    if (treeSelected.size === 0) return;
    var items: string[] = [];
    treeSelected.forEach(function(id) { var n = treeNodes.find(function(tn: any) { return tn.id === id; }); if (n) items.push(id + '|' + n.label); });
    await fetch(API + '/' + treePicker.ruleId, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ ctxId, field: 'applied', value: items.join('\\n') }) });
    setTreePicker({ open: false, ruleId: '' }); load();
  };

  const STORAGE_KEY = 'rules_view_filters_' + ctxId;
  const [scopeId, setScopeId] = useState(() => { try { return localStorage.getItem(STORAGE_KEY + '_scopeId') || ''; } catch(e) { return ''; } });
  const [scopeName, setScopeName] = useState(() => { try { return localStorage.getItem(STORAGE_KEY + '_scopeName') || ''; } catch(e) { return ''; } });
  const [typePickerOpen, setTypePickerOpen] = useState(false);
  const dodafTypes = ['Capability','OperationalNode','SystemNode','Organization','InformationExchange','ActionUsage','PartUsage','InterfaceUsage','RequirementUsage','Function'];
  const TYPE_LABELS: Record<string,string> = {
    Capability:'能力', OperationalNode:'作战节点', SystemNode:'系统节点', Organization:'组织',
    InformationExchange:'信息交换', ActionUsage:'动作', PartUsage:'部件', InterfaceUsage:'接口',
    RequirementUsage:'需求', Function:'功能',
  };
  const typeLabel = (t: string): string => TYPE_LABELS[t] ? TYPE_LABELS[t] + '（' + t + '）' : t;
  const [typeFilter, setTypeFilter] = useState(() => { try { return localStorage.getItem(STORAGE_KEY + '_typeFilter') || ''; } catch(e) { return ''; } });
  // Persist typeFilter changes
  useEffect(() => { try { localStorage.setItem(STORAGE_KEY + '_typeFilter', typeFilter); } catch(e) {} }, [typeFilter]);
  useEffect(() => { try { localStorage.setItem(STORAGE_KEY + '_scopeId', scopeId); } catch(e) {} }, [scopeId]);
  useEffect(() => { try { localStorage.setItem(STORAGE_KEY + '_scopeName', scopeName); } catch(e) {} }, [scopeName]);

  // Scope drag-drop handler (same logic as Matrix View)
  const handleScopeDrop = async (e: React.DragEvent) => {
    e.preventDefault();
    var pkgId = '', pkgName = '';
    for (var i = 0; i < e.dataTransfer.types.length; i++) {
      try {
        var raw = e.dataTransfer.getData(e.dataTransfer.types[i]);
        var parsed: any = raw;
        try { parsed = JSON.parse(raw); } catch (_) {}
        if (Array.isArray(parsed) && parsed.length > 0) {
          var first = parsed[0];
          pkgId = typeof first === 'string' ? first : first?.id || first?.elementId || '';
          pkgName = first?.label || first?.name || '';
        } else if (parsed && typeof parsed === 'object') {
          pkgId = parsed.id || parsed.elementId || '';
          pkgName = parsed.label || parsed.name || '';
        } else if (typeof raw === 'string' && raw.length > 10 && raw.length < 200) {
          pkgId = raw; pkgName = raw;
        }
        if (pkgId) { setScopeId(pkgId); setScopeName(pkgName); fetch(API + '/scope-name?ctxId=' + ctxId + '&id=' + pkgId).then(r => r.json()).then(d => { if (d.name) setScopeName(d.name); }); break; }
      } catch (_) {}
    }
  };

  const [appliedFilter, setAppliedFilter] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [expanded, setExpanded] = useState<Set<string>>(new Set());

  const load = useCallback(async () => {
    try { const r = await fetch(API + '/list?ctxId=' + ctxId).then(r => r.json()); setRules(r); setFiltered(r); } catch(e) {}
  }, [ctxId]);
  useEffect(() => { load(); }, [load]);

  useEffect(() => {
    let result = [...rules];
    if (searchQuery) { const q = searchQuery.toLowerCase(); result = result.filter(r => (r.name||'').toLowerCase().includes(q) || (r.applied||'').toLowerCase().includes(q) || (r.description||'').toLowerCase().includes(q) || (r.ruleType||'').toLowerCase().includes(q) || (r.owner||'').toLowerCase().includes(q)); }
    if (appliedFilter) result = result.filter(r => (r.applied||'').split('\\n').map(extractLabel).includes(appliedFilter));
    setFiltered(result);
  }, [rules, appliedFilter, searchQuery]);

  const handleAdd = async (parentId?: string) => {
    try { await fetch(API + '/add', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ ctxId, parentId: parentId || null }) }); load(); } catch(e) {}
  };
  const handleDelete = async () => {
    if (selected.size === 0) { showToast('请先选择要删除的数据行'); return; }
    setDeleteDialog({ open: true, count: selected.size });
  };
  const handleSync = async () => { setSyncLoading(true); try { await fetch(API + '/sync?ctxId=' + ctxId, { method: 'POST' }); load(); } catch(e) {} setSyncLoading(false); };
  const handleExport = () => {
    const csv = ['序号,应用于,名称,规则说明,规则种类,所有者', ...filtered.map((r, i) => `${i + 1},"${r.applied||''}","${r.name||''}","${r.description||''}","${r.ruleType||''}","${r.owner||''}"`)].join('\n');
    const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8' }); const url = URL.createObjectURL(blob); const a = document.createElement('a'); a.href = url; a.download = 'rules.csv'; a.click();
  };
  const handleEdit = async (id: string, field: string, value: string) => {
    try { await fetch(API + '/' + id, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ ctxId, field, value }) }); load(); setEditing(null); } catch(e) {}
  };
  const toggleSelect = (id: string) => { const s = new Set(selected); if (s.has(id)) s.delete(id); else s.add(id); setSelected(s); };
  const toggleExpand = (id: string) => { const s = new Set(expanded); if (s.has(id)) s.delete(id); else s.add(id); setExpanded(s); };

  var _rowIdx = 0;
  var _flatIdx = 0;
  const renderRows = (items: Rule[], depth: number = 0): React.ReactNode[] => {
    if (depth === 0) { _rowIdx = 0; _flatIdx = 0; }
    const result: React.ReactNode[] = [];
    for (const r of items) {
      _rowIdx++; var idx = _rowIdx;
      const hasChildren = r.children && r.children.length > 0;
      const isExpanded = expanded.has(r.id);
      const isSelected = selected.has(r.id);
      const isEven = _flatIdx % 2 === 0; _flatIdx++;
      result.push(
        <tr key={r.id} style={{ background: isSelected ? 'rgba(59,130,246,0.15)' : (isEven ? '#19284f' : '#162242') }}
          onMouseEnter={e => { if (!isSelected) (e.currentTarget as HTMLElement).style.background = 'rgba(255,255,255,0.08)'; }}
          onMouseLeave={e => { if (!isSelected) (e.currentTarget as HTMLElement).style.background = isEven ? '#19284f' : '#162242'; }}>
          <td style={cellStyle}><input type="checkbox" checked={isSelected} onChange={() => toggleSelect(r.id)} /></td>
          <td style={{ ...cellStyle, width: 50, textAlign: 'center', color: '#94a3b8' }}>{idx}</td>
          <td style={{ ...cellStyle, paddingLeft: 10 + depth * 20 }}>
            {hasChildren && <span onClick={() => toggleExpand(r.id)} style={{ cursor: 'pointer', marginRight: 4, color: '#3b82f6' }}>{isExpanded ? '▼' : '▶'}</span>}
            <span onDoubleClick={() => openTreePicker(r.id)} style={{ color: r.applied ? '#e2e8f0' : '#64748b', cursor: 'pointer', whiteSpace: 'pre-line' }}>{(r.applied || '').split('\\n').map(function(s: any) { var p = (s||'').indexOf('|'); return p > 0 ? s.substring(p + 1) : s; }).filter(Boolean).join('\n') || '双击选择树节点...'}</span>
            {hasChildren && <button onClick={() => handleAdd(r.id)} style={{ ...btnSmall, marginLeft: 6, background: 'rgba(59,130,246,0.15)', color: '#60a5fa' }} title="添加子行">+</button>}
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

  const btnStyle: React.CSSProperties = { background: '#3b82f6', color: '#fff', border: 'none', borderRadius: 6, padding: '6px 14px', cursor: 'pointer', fontSize: 13 };
  const btnDanger: React.CSSProperties = { ...btnStyle, background: '#ef4444' };
  const btnSmall: React.CSSProperties = { ...btnStyle, padding: '2px 8px', fontSize: 11 };
  const inputStyle: React.CSSProperties = { background: '#162242', border: '1px solid rgba(255,255,255,0.12)', borderRadius: 4, color: '#e2e8f0', padding: '6px 10px', fontSize: 13, width: '100%', boxSizing: 'border-box' };
  const cellStyle: React.CSSProperties = { padding: '6px 10px', borderBottom: '1px solid rgba(255,255,255,0.04)', fontSize: 13, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' };
  const thStyle: React.CSSProperties = { ...cellStyle, background: '#19284f', fontWeight: 600, position: 'sticky', top: 0, zIndex: 1 };

  const extractLabel = (s: string) => { var p = (s || '').indexOf('|'); return p > 0 ? s.substring(p + 1) : s; };
  const appliedOptions = [...new Set(rules.flatMap(r => (r.applied || '').split('\\n').filter(Boolean).map(extractLabel)))];

  return (
    <div id="rules-wrapper" style={{ height: '100%', display: 'flex', flexDirection: 'column', background: '#19284f', color: '#e0e0e0', fontFamily: 'system-ui, sans-serif', fontSize: 13 }}>
      {toast && <div style={{ position: 'fixed', top: 16, left: '50%', transform: 'translateX(-50%)', zIndex: 9999, background: '#ef4444', color: '#fff', padding: '10px 24px', borderRadius: 8, fontSize: 14, boxShadow: '0 4px 12px rgba(0,0,0,0.3)' }}>{toast}</div>}
      {/* Tree Picker Modal */}
      {treePicker.open && <div style={{ position:'fixed',inset:0,zIndex:9998,display:'flex',alignItems:'center',justifyContent:'center',background:'rgba(0,0,0,0.5)' }} onClick={()=>setTreePicker({open:false,ruleId:''})}><div style={{ background:'#19284f',borderRadius:12,padding:'20px 24px',width:440,maxHeight:520,display:'flex',flexDirection:'column',boxShadow:'0 8px 32px rgba(0,0,0,0.5)',border:'1px solid rgba(255,255,255,0.1)' }} onClick={e=>e.stopPropagation()}><h3 style={{ margin:'0 0 12px',color:'#f1f5f9',fontSize:16 }}>选择树节点</h3><input value={treeSearch} onChange={e=>setTreeSearch(e.target.value)} placeholder="搜索节点..." style={{ ...inputStyle,marginBottom:12 }} autoFocus/><div style={{ flex:1,overflow:'auto',minHeight:200 }}>{treeLoading?<div style={{ color:'#64748b',textAlign:'center',padding:30 }}>加载中...</div>:treeNodes.length===0?<div style={{ color:'#64748b',textAlign:'center',padding:30 }}>暂无树节点数据<br/><small>请先在Explorer中展开需要的节点层级</small></div>:treeNodes.filter(n=>!treeSearch||n.label.toLowerCase().includes(treeSearch.toLowerCase())).slice(0,100).map(n=><div key={n.id} onClick={()=>selectTreeNode(n.id)} style={{ padding:'8px 12px',cursor:'pointer',fontSize:13,color:treeSelected.has(n.id)?'#60a5fa':'#e2e8f0',borderBottom:'1px solid rgba(255,255,255,0.04)',borderRadius:4,display:'flex',alignItems:'center',gap:8 }} onMouseEnter={e=>(e.currentTarget as HTMLElement).style.background='rgba(59,130,246,0.15)'} onMouseLeave={e=>(e.currentTarget as HTMLElement).style.background='transparent'}><input type="checkbox" checked={treeSelected.has(n.id)} readOnly style={{accentColor:'#3b82f6'}}/><span style={{ fontSize:11,color:'#64748b',marginRight:8 }}>[{n.type}]</span>{n.label}</div>)}</div><div style={{display:'flex',gap:8,marginTop:12,justifyContent:'flex-end'}}><button onClick={()=>setTreePicker({open:false,ruleId:''})} style={{background:'#334155',color:'#cbd5e1',border:'none',borderRadius:6,padding:'6px 14px',cursor:'pointer',fontSize:12}}>取消</button><button onClick={confirmTreeSelection} style={{background:'#3b82f6',color:'#fff',border:'none',borderRadius:6,padding:'6px 18px',cursor:'pointer',fontSize:12}}>确定({treeSelected.size})</button></div></div></div>}
      {/* Toolbar */}
      <div style={{ display: 'flex', gap: 8, padding: '10px 16px', borderBottom: '1px solid rgba(255,255,255,0.08)' }}>
        <button onClick={() => handleAdd()} style={btnStyle}>＋ 添加行</button>
        <button onClick={handleDelete} style={btnDanger}>🗑 删除行</button>
        <button onClick={handleSync} disabled={syncLoading} style={{ ...btnStyle, background: '#6366f1' }}>{syncLoading ? '同步中...' : '🔄 同步'}</button>
        <button onClick={handleExport} style={{ ...btnStyle, background: '#10b981' }}>📥 导出</button>
      </div>
      {/* Filters */}
      <div style={{ display: 'flex', gap: 12, padding: '8px 16px', borderBottom: '1px solid rgba(255,255,255,0.06)', alignItems: 'center', flexWrap: 'wrap', position: 'relative' }}>
        <span style={{ color: '#94a3b8', fontSize: 12 }}>范围:</span>
        <input value={scopeName || scopeId} readOnly placeholder="拖动Package/ViewUsage至此处" title="从模型树拖拽Package限定范围"
          style={{ ...inputStyle, width: 220, cursor: 'pointer', color: scopeId ? '#e2e8f0' : '#64748b' }}
          onDragOver={e => { e.preventDefault(); e.dataTransfer!.dropEffect = 'link'; }}
          onDrop={handleScopeDrop}
        />
        <span style={{ color: '#94a3b8', fontSize: 12 }}>类型:</span>
        <input value={typeFilter} readOnly placeholder="点击选择元类型" style={{ ...inputStyle, width: 180, cursor: 'pointer' }} onClick={() => setTypePickerOpen(true)} />
        {typePickerOpen && (
          <div style={{ position: 'fixed', inset: 0, zIndex: 9997, display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'rgba(0,0,0,0.5)' }} onClick={() => setTypePickerOpen(false)}>
            <div style={{ background: '#19284f', borderRadius: 12, padding: 24, minWidth: 300, maxHeight: 500, overflow: 'auto', color: '#e0e0e0', border: '1px solid rgba(255,255,255,0.1)', boxShadow: '0 20px 60px rgba(0,0,0,0.5)' }} onClick={e => e.stopPropagation()}>
              <h3 style={{ margin: '0 0 16px', fontSize: 15, color: '#f1f5f9' }}>选择元类型</h3>
              {dodafTypes.map(t => {
                var checked = typeFilter.split(',').indexOf(t) >= 0;
                return <label key={t} style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '6px 0', cursor: 'pointer', fontSize: 13 }}>
                  <input type="checkbox" checked={checked} onChange={() => {
                    var arr = typeFilter ? typeFilter.split(',').filter(Boolean) : [];
                    if (checked) arr = arr.filter(x => x !== t); else arr.push(t);
                    setTypeFilter(arr.join(','));
                  }} />{typeLabel(t)}
                </label>;
              })}
              <div style={{ display: 'flex', gap: 8, marginTop: 16, justifyContent: 'flex-end' }}>
                <button onClick={() => setTypeFilter(dodafTypes.join(','))} style={{ background: '#334155', color: '#cbd5e1', border: 'none', borderRadius: 6, padding: '6px 14px', cursor: 'pointer', fontSize: 12 }}>全选</button>
                <button onClick={() => setTypeFilter('')} style={{ background: '#334155', color: '#cbd5e1', border: 'none', borderRadius: 6, padding: '6px 14px', cursor: 'pointer', fontSize: 12 }}>全不选</button>
                <button onClick={() => setTypePickerOpen(false)} style={{ background: '#3b82f6', color: '#fff', border: 'none', borderRadius: 6, padding: '6px 18px', cursor: 'pointer', fontSize: 12 }}>确定</button>
              </div>
            </div>
          </div>
        )}
        <span style={{ color: '#94a3b8', fontSize: 12, marginLeft: 16 }}>过滤:</span>
        <select value={appliedFilter} onChange={e => setAppliedFilter(e.target.value)} style={{ ...inputStyle, width: 140, cursor: 'pointer' }}>
          <option value="">应用于</option>
          {appliedOptions.map(o => <option key={o} value={o}>{o}</option>)}
        </select>
        <input value={searchQuery} onChange={e => setSearchQuery(e.target.value)} placeholder="全局搜索..." style={{ ...inputStyle, width: 200 }} />
      </div>
      {/* Delete Confirmation Dialog */}
      {deleteDialog.open && (
        <div style={{ position: 'fixed', inset: 0, zIndex: 9999, display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'rgba(0,0,0,0.5)' }} onClick={() => setDeleteDialog({ open: false, count: 0 })}>
          <div style={{ background: '#19284f', borderRadius: 12, padding: '24px 32px', minWidth: 360, boxShadow: '0 8px 32px rgba(0,0,0,0.5)', border: '1px solid rgba(255,255,255,0.1)' }} onClick={e => e.stopPropagation()}>
            <h3 style={{ margin: '0 0 12px', color: '#f1f5f9', fontSize: 16 }}>确认删除</h3>
            <p style={{ margin: '0 0 20px', color: '#94a3b8', fontSize: 14 }}>确定删除选中的 {deleteDialog.count} 行数据？此操作不可撤销。</p>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10 }}>
              <button onClick={() => setDeleteDialog({ open: false, count: 0 })} style={{ background: '#334155', color: '#cbd5e1', border: 'none', borderRadius: 6, padding: '8px 20px', cursor: 'pointer', fontSize: 13 }}>取消</button>
              <button onClick={async () => { setDeleteDialog({ open: false, count: 0 }); for (const id of selected) { await fetch(API + '/' + id, { method: 'DELETE', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ ctxId }) }).catch(() => {}); } setSelected(new Set()); load(); }} style={{ background: '#ef4444', color: '#fff', border: 'none', borderRadius: 6, padding: '8px 20px', cursor: 'pointer', fontSize: 13 }}>确认删除</button>
            </div>
          </div>
        </div>
      )}
      {/* Table */}
      <div style={{ flex: 1, overflow: 'auto' }}>
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
