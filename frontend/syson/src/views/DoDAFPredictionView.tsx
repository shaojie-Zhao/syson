/// <reference types="react" />
import React, { useState, useEffect, useCallback, useRef } from 'react';
import ReactDOM from 'react-dom/client';

interface PredictionRow { id: string; domain: string; skill: string; shortTerm: string; midTerm: string; longTerm: string; }

const API = '/api/prediction/default-prediction';

function getStorageKey() { var s = new URLSearchParams(window.location.search); return 'dodaf_pred_' + (s.get('selection') || s.get('representation') || ''); }
function loadData(): PredictionRow[] { try { var k = getStorageKey(); return k ? JSON.parse(localStorage.getItem(k) || '[]') : []; } catch(e) { return []; } }
function saveData(data: PredictionRow[]) { var k = getStorageKey(); if (k) localStorage.setItem(k, JSON.stringify(data)); }

const thStyle: React.CSSProperties = { padding: '8px 12px', background: '#19284f', fontWeight: 600, fontSize: 13, color: '#e2e8f0', borderBottom: '2px solid rgba(255,255,255,0.1)', textAlign: 'center', position: 'sticky', top: 0, zIndex: 1 };
const tdStyle: React.CSSProperties = { padding: '6px 10px', borderBottom: '1px solid rgba(255,255,255,0.04)', fontSize: 13, color: '#e2e8f0', textAlign: 'center', minWidth: 100 };
const inputStyle: React.CSSProperties = { background: '#162242', border: '1px solid rgba(255,255,255,0.12)', borderRadius: 4, color: '#e2e8f0', padding: '6px 10px', fontSize: 13, width: '100%', boxSizing: 'border-box', textAlign: 'center' };

const DoDAFPredictionView: React.FC = () => {
  const [data, setData] = useState<PredictionRow[]>([]);
  const [selected, setSelected] = useState<Set<string>>(new Set());

  useEffect(() => { setData(loadData()); }, []);

  const handleAdd = () => { var d = [...data, { id: crypto.randomUUID(), domain: '', skill: '', shortTerm: '', midTerm: '', longTerm: '' }]; setData(d); saveData(d); };
  const handleDelete = () => {
    if (selected.size === 0) return;
    var d = data.filter(r => !selected.has(r.id)); setData(d); saveData(d); setSelected(new Set());
    if (!window.confirm('确定删除 ' + selected.size + ' 行？')) return;
    d = data.filter(r => !selected.has(r.id)); setData(d); saveData(d); setSelected(new Set());
  };
  const handleChange = (id: string, field: keyof PredictionRow, value: string) => {
    var d = data.map(r => r.id === id ? { ...r, [field]: value } : r); setData(d); saveData(d);
  };
  const toggleRow = (id: string) => { var s = new Set(selected); if (s.has(id)) s.delete(id); else s.add(id); setSelected(s); };
  const toggleAll = () => { if (selected.size === data.length) setSelected(new Set()); else setSelected(new Set(data.map(r => r.id))); };

  const toolbar: React.CSSProperties = { display: 'flex', gap: 8, padding: '10px 16px', borderBottom: '1px solid rgba(255,255,255,0.08)' };
  const btn: React.CSSProperties = { background: '#3b82f6', color: '#fff', border: 'none', borderRadius: 6, padding: '6px 14px', cursor: 'pointer', fontSize: 12 };
  const btnDanger: React.CSSProperties = { ...btn, background: '#ef4444' };

  return React.createElement('div', { style: { height: '100%', display: 'flex', flexDirection: 'column', background: '#19284f', color: '#e0e0e0', fontFamily: 'system-ui, sans-serif', fontSize: 13 } },
    // Toolbar
    React.createElement('div', { style: toolbar },
      React.createElement('button', { onClick: handleAdd, style: btn }, '＋ 新增行'),
      React.createElement('button', { onClick: handleDelete, style: btnDanger }, '🗑 删除行'),
      React.createElement('span', { style: { marginLeft: 'auto', color: '#64748b', fontSize: 12 } }, '共 ' + data.length + ' 行')
    ),
    // Table
    React.createElement('div', { style: { flex: 1, overflow: 'auto' } },
      React.createElement('table', { style: { width: '100%', borderCollapse: 'collapse' } },
        React.createElement('thead', null,
          React.createElement('tr', null,
            React.createElement('th', { rowSpan: 2, style: { ...thStyle, width: 40, verticalAlign: 'middle' } },
              React.createElement('input', { type: 'checkbox', checked: selected.size === data.length && data.length > 0, onChange: toggleAll, style: { accentColor: '#3b82f6', cursor: 'pointer' } })
            ),
            React.createElement('th', { rowSpan: 2, style: { ...thStyle, verticalAlign: 'middle' } }, '技术和技能领域'),
            React.createElement('th', { rowSpan: 2, style: { ...thStyle, verticalAlign: 'middle' } }, '技术和技能'),
            React.createElement('th', { colSpan: 3, style: { ...thStyle, width: 300 } }, '技术和技能预测')
          ),
          React.createElement('tr', null,
            React.createElement('th', { style: { ...thStyle, width: 100 } }, '短期'),
            React.createElement('th', { style: { ...thStyle, width: 100 } }, '中期'),
            React.createElement('th', { style: { ...thStyle, width: 100 } }, '长期')
          )
        ),
        React.createElement('tbody', null,
          data.map(function(row) {
            var sel = selected.has(row.id);
            return React.createElement('tr', { key: row.id, style: { background: sel ? 'rgba(59,130,246,0.12)' : 'transparent' } },
              React.createElement('td', { style: { ...tdStyle, minWidth: 40, cursor: 'pointer' }, onClick: function() { toggleRow(row.id); } },
                React.createElement('input', { type: 'checkbox', checked: sel, readOnly: true, style: { accentColor: '#3b82f6', pointerEvents: 'none' } })
              ),
              React.createElement('td', { style: tdStyle },
                React.createElement('input', { value: row.domain, onChange: function(e: any) { handleChange(row.id, 'domain', e.target.value); }, style: inputStyle, placeholder: '领域' })
              ),
              React.createElement('td', { style: tdStyle },
                React.createElement('input', { value: row.skill, onChange: function(e: any) { handleChange(row.id, 'skill', e.target.value); }, style: inputStyle, placeholder: '技能' })
              ),
              React.createElement('td', { style: tdStyle },
                React.createElement('input', { value: row.shortTerm, onChange: function(e: any) { handleChange(row.id, 'shortTerm', e.target.value); }, style: inputStyle, placeholder: '短期' })
              ),
              React.createElement('td', { style: tdStyle },
                React.createElement('input', { value: row.midTerm, onChange: function(e: any) { handleChange(row.id, 'midTerm', e.target.value); }, style: inputStyle, placeholder: '中期' })
              ),
              React.createElement('td', { style: tdStyle },
                React.createElement('input', { value: row.longTerm, onChange: function(e: any) { handleChange(row.id, 'longTerm', e.target.value); }, style: inputStyle, placeholder: '长期' })
              )
            );
          })
        )
      )
    )
  );
};

export default DoDAFPredictionView;
