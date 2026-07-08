import React, { useEffect, useState, useCallback, useMemo } from 'react';
import { Gantt as GanttOriginal, ViewMode, Task, DateStartColumn, DateEndColumn } from '@ObeoNetwork/gantt-task-react';
import '@ObeoNetwork/gantt-task-react/dist/style.css';
import { zhCN } from 'date-fns/locale';

const Gantt = GanttOriginal as React.FC<any>;

const API = '/api/gantt/default-gantt/tasks';
const DARK = '#19284F';
const DARK2 = '#1e3366';
const ACCENT = '#3b82f6';
const BORDER = 'rgba(148,163,184,0.15)';

interface GT { id: string; parentId: string | null; name: string; description: string; startDate: string; endDate: string; progress: number; }

const DoDAFGanttTimeline: React.FC = () => {
  const [tasks, setTasks] = useState<GT[]>([]);
  const [viewMode, setViewMode] = useState<ViewMode>(ViewMode.Month);
  const [editing, setEditing] = useState<GT | null>(null);
  const [expanded, setExpanded] = useState<Set<string>>(new Set());
  const [adding, setAdding] = useState(false);

  const load = useCallback(async () => {
    try { const r = await fetch(API); setTasks(await r.json()); } catch {}
  }, []);

  useEffect(() => { load(); }, [load]);

  const ganttTasks = useMemo(() => {
    const hasChildren = new Set(tasks.filter(t => t.parentId).map(t => t.parentId!));
    const ordered: GT[] = [];
    const walk = (parentId: string | null) => {
      tasks.filter(t => t.parentId === parentId).forEach(t => {
        ordered.push(t);
        if (expanded.has(t.id)) walk(t.id);
      });
    };
    walk(null);
    return ordered.map(t => ({
      id: t.id,
      type: hasChildren.has(t.id) ? 'project' as const : 'task' as const,
      name: t.name,
      start: new Date(t.startDate + 'T00:00:00'),
      end: new Date(t.endDate + 'T00:00:00'),
      progress: t.progress,
      project: t.parentId || undefined,
      hideChildren: !expanded.has(t.id),
      styles: { progressColor: t.progress >= 100 ? '#22c55e' : '#3b82f6', backgroundColor: t.progress >= 100 ? '#22c55e' : '#3b82f6' } as any,
      isDisabled: false, dependencies: [],
    }));
  }, [tasks, expanded]);

  const handleAdd = async (parentId: string | null) => {
    if (adding) return;
    setAdding(true);
    const url = API + (parentId ? `?parentId=${parentId}` : '');
    try {
      const r = await fetch(url, { method: 'POST' });
      const newTask = await r.json();
      if (newTask && parentId) {
        const parent = tasks.find(t => t.id === parentId);
        let start = newTask.startDate, end = newTask.endDate;
        if (parent) {
          if (start < parent.startDate) start = parent.startDate;
          if (end > parent.endDate) end = parent.endDate;
          if (start > end) { start = parent.startDate; end = parent.endDate; }
        }
        if (start !== newTask.startDate || end !== newTask.endDate) {
          await fetch(API + '/' + newTask.id, {
            method: 'PUT', headers: {'Content-Type':'application/json'},
            body: JSON.stringify({ ...newTask, startDate: start, endDate: end }),
          });
        }
        setExpanded(p => { const s = new Set(p); s.add(parentId); return s; });
      }
    } catch {} finally {
      await load();
      setAdding(false);
    }
  };

  const save = async (t: GT) => {
    await fetch(API + '/' + t.id, { method: 'PUT', headers: {'Content-Type':'application/json'}, body: JSON.stringify(t) }).catch(()=>{});
    load();
  };

  const toggle = (id: string) => setExpanded(p => { const s = new Set(p); s.has(id) ? s.delete(id) : s.add(id); return s; });

  const colorFor = (progress: number) => progress >= 100 ? '#22c55e' : '#3b82f6';

  const getDepth = (t: GT | null): number => {
    let depth = 0;
    let current = t;
    while (current?.parentId) {
      depth++;
      current = tasks.find(x => x.id === current?.parentId) || null;
    }
    return depth;
  };

  const seqMap = useMemo(() => {
    const map: Record<string, string> = {};
    const walk = (parentId: string | null, prefix: string) => {
      const siblings = tasks.filter(t => t.parentId === parentId);
      siblings.forEach((t, i) => {
        const num = prefix ? `${prefix}.${i + 1}` : `${i + 1}`;
        map[t.id] = num;
        if (expanded.has(t.id)) walk(t.id, num);
      });
    };
    walk(null, '');
    return map;
  }, [tasks, expanded]);

  const SeqCell: React.FC<any> = ({ data: { task } }) => {
    if (!task || task.type === 'empty') return <div style={{ padding: '8px 12px', color: '#64748b' }}></div>;
    const num = seqMap[task.id] || '';
    return <div style={{ padding: '6px 12px', fontSize: 12, color: '#94a3b8', whiteSpace: 'nowrap' }}>{num}</div>;
  };

  const NameCell: React.FC<any> = ({ data: { task, handleDeleteTasks } }) => {
    if (!task || task.type === 'empty') return <div style={{ padding: 8, color: '#64748b' }}>—</div>;
    const t = tasks.find(x => x.id === task.id);
    const name = t ? t.name : task.name;
    const hasChildTasks = tasks.some(c => c.parentId === task.id);
    const depth = t ? getDepth(t) : 0;
    const isClosed = !expanded.has(task.id);
    return (
      <div style={{ display: 'flex', alignItems: 'center', gap: 4, padding: '6px 8px', height: '100%', minWidth: 0, color: '#e2e8f0' }}>
        <span style={{ width: depth * 20, flexShrink: 0 }} />
        <span onClick={(e) => { e.stopPropagation(); if (hasChildTasks) { toggle(task.id); } }}
          style={{ cursor: hasChildTasks ? 'pointer' : 'default', color: '#cbd5e1', fontSize: 10, width: 16, flexShrink: 0, textAlign: 'center', userSelect: 'none' }}>
          {hasChildTasks ? (isClosed ? '▶' : '▼') : ''}
        </span>
        <span onClick={() => t && setEditing(t)}
          style={{ flex: 1, cursor: 'pointer', fontSize: 13, color: '#e2e8f0', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', minWidth: 0 }}>
          {name}
        </span>
        <button onClick={() => handleAdd(task.id)}
          disabled={adding}
          style={{ border: 'none', borderRadius: 3, padding: '2px 8px', cursor: adding ? 'wait' : 'pointer', fontSize: 11, color: adding ? '#64748b' : '#a5b4fc', background: adding ? 'rgba(255,255,255,0.05)' : 'rgba(99,102,241,0.15)', flexShrink: 0, opacity: adding ? 0.5 : 1 }}
          title="添加子任务">+</button>
        <button onClick={(e) => { e.stopPropagation(); handleDeleteTasks([task]); }}
          style={{ border: 'none', borderRadius: 3, padding: '2px 8px', cursor: 'pointer', fontSize: 11, color: '#fca5a5', background: 'rgba(239,68,68,0.15)', flexShrink: 0 }}
          title="删除">×</button>
      </div>
    );
  };

  const StartCell: React.FC<any> = ({ data: { task } }) => {
    if (!task || task.type === 'empty') return <div />;
    const d = task.start ? (typeof task.start === 'string' ? task.start : task.start.toISOString().slice(0, 10)) : '';
    return <div style={{ padding: '6px 8px', fontSize: 12, color: '#e2e8f0' }}>{d}</div>;
  };
  const EndCell: React.FC<any> = ({ data: { task } }) => {
    if (!task || task.type === 'empty') return <div />;
    const d = task.end ? (typeof task.end === 'string' ? task.end : task.end.toISOString().slice(0, 10)) : '';
    return <div style={{ padding: '6px 8px', fontSize: 12, color: '#e2e8f0' }}>{d}</div>;
  };
  const ProgressCell: React.FC<any> = ({ data: { task } }) => {
    const t = tasks.find(x => x.id === task.id);
    const p = t ? t.progress : (task.progress || 0);
    const c = colorFor(p);
    return (
      <div style={{ display: 'flex', alignItems: 'center', gap: 4, padding: '6px 8px', height: '100%' }}>
        <div style={{ flex: 1, height: 5, background: 'rgba(255,255,255,0.08)', borderRadius: 3, overflow: 'hidden' }}>
          <div style={{ height: '100%', width: p + '%', background: c, borderRadius: 3 }} />
        </div>
        <span style={{ fontSize: 11, color: '#cbd5e1', minWidth: 28, textAlign: 'right' }}>{p}%</span>
      </div>
    );
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%', background: DARK, fontFamily: 'system-ui, sans-serif' }}>
      <style>{`
        :root { --gantt-bg: #19284F; --gantt-surface: #1e3366; --gantt-text: #e2e8f0; --gantt-muted: #94a3b8; --gantt-border: rgba(148,163,184,0.15); }
        .gantt-task-react-wrapper, .gantt-task-react-wrapper * { font-family: system-ui, sans-serif !important; }
        .gantt-task-react-wrapper { background: var(--gantt-bg) !important; }
        .gantt-task-react-header { background: var(--gantt-surface) !important; }
        .gantt-task-react-task-list { background: var(--gantt-bg) !important; }
        .gantt-task-react-task-list__table { background: var(--gantt-bg) !important; }
        .gantt-task-react-task-list__table-row { border-color: var(--gantt-border) !important; background: var(--gantt-bg) !important; }
        .gantt-task-react-task-list__table-row:hover { background: rgba(255,255,255,0.04) !important; }
        .gantt-task-react-task-list__cell { border-color: var(--gantt-border) !important; }
        .gantt-task-react-task-list__header-cell { background: var(--gantt-surface) !important; color: #cbd5e1 !important; border-color: var(--gantt-border) !important; font-weight: 600 !important; font-size: 12px !important; text-transform: uppercase !important; letter-spacing: 0.3px !important; }
        .gantt-task-react-task-list__header-cell * { color: #cbd5e1 !important; fill: #cbd5e1 !important; }
        .gantt-task-react-weekend { fill: rgba(255,255,255,0.015) !important; }
        .gantt-task-react-calendar__header-cell { fill: var(--gantt-surface) !important; color: #cbd5e1 !important; stroke: var(--gantt-border) !important; }
        .gantt-task-react-calendar__grid-row-cell { stroke: rgba(148,163,184,0.06) !important; }
        .gantt-task-react-calendar__grid-cell { stroke: rgba(148,163,184,0.06) !important; }
        .gantt-task-react-calendar__grid-row-cell__weekend { fill: rgba(255,255,255,0.015) !important; }
        .gantt-task-react-calendar__ticks { stroke: rgba(148,163,184,0.1) !important; }
        .gantt-task-react-calendar__table-body { background: var(--gantt-bg) !important; }
        .gantt-task-react-task-list__empty { background: var(--gantt-bg) !important; color: var(--gantt-muted) !important; }
        .gantt-task-react-task-list__table-row__selected { background: #1e3366 !important; }
        svg text, svg tspan, .gantt-task-react-calendar text { fill: #cbd5e1 !important; color: #cbd5e1 !important; }
        .gantt-task-react-task-list__cell, .gantt-task-react-task-list__cell * { color: #e2e8f0 !important; }
        .gantt-task-react-calendar__table-body text, .gantt-task-react-calendar__header-cell text { fill: #cbd5e1 !important; }
        input, textarea, select { background: var(--gantt-surface) !important; color: #e2e8f0 !important; border-color: var(--gantt-border) !important; }
        .gantt-task-react-task-list__table-row:nth-child(even) { background: var(--gantt-bg) !important; }
        ._ganttTaskContent_1sr1d_41 > div,
        [class*="_ganttTaskContent"] > div {
          background-image: linear-gradient(transparent 50px, #19284F 50px) !important;
        }
        [class*="_ganttTaskContent"] > div > div {
          background-image: linear-gradient(to right, rgba(148,163,184,0.06) 1px, transparent 1px), linear-gradient(transparent 50px, #19284F 50px) !important;
        }
        ._projectBackground_19i2s_6, [class*="_projectBackground"] { fill: #1e40af !important; }
        ._projectTop_19i2s_11, [class*="_projectTop"] { fill: #1e40af !important; }
        ._wrapper_1sr1d_54, [class*="_wrapper_"], [data-testid="gantt-main"] { height: 100% !important; }
        ._ganttTaskContent_1sr1d_41, [class*="_ganttTaskContent"] { height: 100% !important; }
        ._taskListContent_v4xjz_64, [class*="_taskListContent"] { height: 100% !important; }
        ._taskListContent_v4xjz_64 > div, [class*="_taskListContent"] > div {
          background-image: linear-gradient(transparent 50px, #19284F 50px) !important;
        }
      `}</style>
      {/* Toolbar */}
      <div style={{ display: 'flex', gap: 8, padding: '10px 16px', background: DARK2, borderBottom: '1px solid ' + BORDER, alignItems: 'center', flexShrink: 0 }}>
        <button onClick={() => handleAdd(null)}
          style={{ background: ACCENT, color: '#fff', border: 'none', padding: '7px 18px', borderRadius: 6, cursor: 'pointer', fontSize: 13, fontWeight: 500, whiteSpace: 'nowrap' }}>
          + 新建任务
        </button>
        <span style={{ flex: 1, fontSize: 12, color: '#94a3b8' }}>
          点击名称编辑 | [+] 子任务 | [×] 删除 | 拖拽调整日期
        </span>
        <select value={viewMode} onChange={e => setViewMode(e.target.value as ViewMode)}
          style={{ background: 'rgba(255,255,255,0.08)', color: '#94a3b8', border: '1px solid ' + BORDER, borderRadius: 6, padding: '4px 10px', fontSize: 12, cursor: 'pointer' }}>
          {[ViewMode.Day, ViewMode.Week, ViewMode.Month, ViewMode.Year].map(m => (
            <option key={m} value={m} style={{ background: DARK, color: '#e0e0e0' }}>{m}</option>
          ))}
        </select>
      </div>
      {/* Gantt chart */}
      <div style={{ flex: 1, overflow: 'hidden' }}>
        {ganttTasks.length > 0 ? (
          <Gantt tasks={ganttTasks as any} viewMode={viewMode}
            columnWidth={viewMode === ViewMode.Year ? 350 : viewMode === ViewMode.Month ? 260 : viewMode === ViewMode.Week ? 200 : 120}
            columns={[
              { id: 'seq', Cell: SeqCell as any, width: 80, title: '序号' },
              { id: 'name', Cell: NameCell as any, width: 300, title: '名称' },
              { id: 'start', Cell: StartCell as any, width: 100, title: '开始' },
              { id: 'end', Cell: EndCell as any, width: 100, title: '结束' },
              { id: 'progress', Cell: ProgressCell as any, width: 100, title: '进度' },
            ]}
            onDateChange={async (task: any) => {
              const t = tasks.find(x => x.id === task.id);
              if (!t) return;
              const newStart = (task as any).start?.toISOString?.()?.slice(0, 10) || '';
              const newEnd = (task as any).end?.toISOString?.()?.slice(0, 10) || '';
              if (!newStart || !newEnd) return;
              if (t.parentId) {
                const parent = tasks.find(x => x.id === t.parentId);
                if (parent) {
                  if (newStart < parent.startDate || newStart > parent.endDate) return;
                  if (newEnd < parent.startDate || newEnd > parent.endDate) return;
                }
              }
              save({ ...t, startDate: newStart, endDate: newEnd });
            }}
            onProgressChange={async (task: any) => {
              const t = tasks.find(x => x.id === task.id);
              if (t) save({ ...t, progress: task.progress });
            }}
            onDelete={async (ts) => { for (const t of ts) await fetch(API + '/' + t.id, { method: 'DELETE' }).catch(() => {}); load(); }}
            onExpandChange={(task) => { toggle(task.id); }}
            headerHeight={48}
            rowHeight={42}
            dateFormats={{
              dateColumnFormat: 'yyyy-MM-dd',
              monthBottomHeaderFormat: 'M月',
              monthTopHeaderFormat: 'yyyy',
              dayBottomHeaderFormat: 'MM-dd',
              dayTopHeaderFormat: 'M月 yyyy',
            }}
            dateLocale={zhCN}
            fontFamily="system-ui, sans-serif"
            TooltipContent={({ task }: any) => {
              if (!task) return null;
              return (
                <div style={{ background: DARK2, padding: '8px 12px', borderRadius: 8, color: '#e0e0e0', fontSize: 12, border: '1px solid ' + BORDER, boxShadow: '0 4px 16px rgba(0,0,0,0.4)' }}>
                  <b style={{ color: '#f1f5f9' }}>{task.name}</b>
                  <div style={{ color: '#64748b', marginTop: 4 }}>
                    {task.start.toLocaleDateString('zh-CN')} — {task.end.toLocaleDateString('zh-CN')}
                  </div>
                  <div style={{ marginTop: 2 }}>进度: {task.progress}%</div>
                </div>
              );
            }}
          />
        ) : (
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%', color: '#64748b', fontSize: 14 }}>
            暂无任务，点击 <b style={{ color: ACCENT, cursor: 'pointer', margin: '0 4px' }} onClick={() => handleAdd(null)}>+ 新建任务</b> 开始
          </div>
        )}
      </div>
      {/* Edit modal */}
      {editing && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.7)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000, backdropFilter: 'blur(4px)' }}
          onClick={() => setEditing(null)}>
          <div style={{ background: DARK2, padding: 28, borderRadius: 12, width: 440, maxWidth: '90vw', color: '#e0e0e0', border: '1px solid ' + BORDER, boxShadow: '0 20px 60px rgba(0,0,0,0.5)' }}
            onClick={e => e.stopPropagation()}>
            <h3 style={{ margin: '0 0 20px', fontSize: 16, fontWeight: 600, color: '#f1f5f9' }}>编辑任务</h3>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
              <div>
                <div style={{ fontSize: 12, color: '#94a3b8', marginBottom: 4, fontWeight: 500 }}>名称</div>
                <input value={editing.name} onChange={e => setEditing({ ...editing, name: e.target.value })} style={inputStyle({})} />
              </div>
              <div>
                <div style={{ fontSize: 12, color: '#94a3b8', marginBottom: 4, fontWeight: 500 }}>描述</div>
                <textarea value={editing.description} onChange={e => setEditing({ ...editing, description: e.target.value })} rows={3} style={inputStyle({ resize: 'vertical' })} />
              </div>
              <div style={{ display: 'flex', gap: 12 }}>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontSize: 12, color: '#94a3b8', marginBottom: 4, fontWeight: 500, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                    开始
                  </div>
                  <input type="date" value={editing.startDate}
                    min={editing.parentId ? (tasks.find(t => t.id === editing.parentId)?.startDate) : undefined}
                    max={editing.parentId ? (tasks.find(t => t.id === editing.parentId)?.endDate) || editing.endDate : editing.endDate}
                    onChange={e => setEditing({ ...editing, startDate: e.target.value })} style={inputStyle({})} />
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontSize: 12, color: '#94a3b8', marginBottom: 4, fontWeight: 500 }}>结束</div>
                  <input type="date" value={editing.endDate}
                    min={editing.parentId ? (tasks.find(t => t.id === editing.parentId)?.startDate) || editing.startDate : editing.startDate}
                    max={editing.parentId ? (tasks.find(t => t.id === editing.parentId)?.endDate) : undefined}
                    onChange={e => setEditing({ ...editing, endDate: e.target.value })} style={inputStyle({})} />
                </div>
              </div>
              <div>
                <div style={{ fontSize: 12, color: '#94a3b8', marginBottom: 4, fontWeight: 500 }}>进度: <span style={{ color: ACCENT }}>{editing.progress}%</span></div>
                <input type="range" min={0} max={100} value={editing.progress}
                  onChange={e => setEditing({ ...editing, progress: parseInt(e.target.value) })}
                  style={{ width: '100%', accentColor: ACCENT }} />
              </div>
            </div>
            <div style={{ display: 'flex', gap: 10, marginTop: 24, justifyContent: 'flex-end' }}>
              <button onClick={() => setEditing(null)}
                style={{ background: 'rgba(255,255,255,0.08)', color: '#94a3b8', border: '1px solid ' + BORDER, padding: '8px 20px', borderRadius: 6, cursor: 'pointer', fontSize: 13 }}>取消</button>
              <button onClick={async () => { await save(editing); setEditing(null); }}
                style={{ background: ACCENT, color: '#fff', border: 'none', padding: '8px 24px', borderRadius: 6, cursor: 'pointer', fontSize: 13, fontWeight: 500 }}>保存</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

const inputStyle = (extra: React.CSSProperties): React.CSSProperties => ({
  width: '100%', padding: '8px 12px', borderRadius: 6,
  border: '1px solid rgba(255,255,255,0.12)', background: 'rgba(255,255,255,0.05)',
  color: '#f1f5f9', fontSize: 14, boxSizing: 'border-box', outline: 'none',
  ...extra,
});

export default DoDAFGanttTimeline;
