import React, { useCallback, useEffect, useRef, useState } from 'react';

const API = '/api/matrix/default-matrix';

// Extract editing context ID from URL: /projects/{projectId}/edit/...
function getEditingContextId(): string {
    const m = window.location.pathname.match(/\/projects\/([^/]+)\/edit/);
    return m ? m[1] : 'default-matrix';
}

function getRepresentationId(): string {
    // Sirius Web may carry the representation id either as a query param (?representation=)
    // or as the last path segment (/projects/{id}/edit/{representationId}). Try both so we can
    // send the REAL matrix representation id to the backend (avoids the fragile fallback that
    // could target another representation such as OV-1).
    const q = window.location.search.match(/[?&]representation=([^&]+)/);
    if (q) return decodeURIComponent(q[1]);
    const p = window.location.pathname.match(/\/edit\/([^/?#]+)/);
    if (p) return decodeURIComponent(p[1]);
    return 'default-matrix';
}

function storageKey(repId: string): string {
    return 'matrix-cfg-' + repId;
}

interface Elem {
    id: string;
    name: string;
    type: string;
    parentPath: string;
    dodafType?: string;
    displayType?: string;
}

interface Rel {
    id: string;
    sourceId: string;
    targetId: string;
    relationType: string;
    createdAt: number;
}

const C: Record<string, string> = {
    Satisfy: '#22c55e',
    Allocate: '#3b82f6',
    Trace: '#f59e0b',
    Dependency: '#ef4444',
    Derive: '#8b5cf6',
};
const S: Record<string, string> = {Satisfy: '✓', Allocate: '→', Trace: '↗', Dependency: '→', Derive: '⇒'};
// Chinese display labels for relation types (internal keys stay English for the backend typeMap).
const RELATION_LABELS: Record<string, string> = {Satisfy: '满足', Allocate: '分配', Trace: '追溯', Dependency: '依赖', Derive: '派生'};
const relLabel = (t: string): string => (RELATION_LABELS[t] ? RELATION_LABELS[t] + '（' + t + '）' : t);
const TYPES = ['Capability', 'OperationalNode', 'SystemNode', 'Organization', 'InformationExchange', 'ActionUsage', 'PartUsage', 'InterfaceUsage', 'RequirementUsage', 'Function'];
// Chinese display labels for element (meta) types (internal keys stay English for filtering/backend).
const TYPE_LABELS: Record<string, string> = {
    Capability: '能力', OperationalNode: '作战节点', SystemNode: '系统节点', Organization: '组织',
    InformationExchange: '信息交换', ActionUsage: '动作', PartUsage: '部件', InterfaceUsage: '接口',
    RequirementUsage: '需求', Function: '功能',
};
const typeLabel = (t: string): string => (TYPE_LABELS[t] ? TYPE_LABELS[t] + '（' + t + '）' : t);
// DoDAF type icons live under /images/dodaf/{DodafType}.svg; fall back to the SysML class icon
// under /icons/full/obj16/{SysmlType}.svg for elements without a DoDAF type. (Both are served by the
// backend and proxied by Vite.)
const DODAF_ICON_TYPES = new Set(['Capability', 'OperationalNode', 'SystemNode', 'Organization', 'InformationExchange']);
const typeIconUrl = (dodafType?: string, sysmlType?: string): string =>
    (dodafType && DODAF_ICON_TYPES.has(dodafType))
        ? '/images/dodaf/' + dodafType + '.svg'
        : '/icons/full/obj16/' + (sysmlType || 'Element') + '.svg';

export default function DoDAFMatrixView() {
    const repId = getRepresentationId();
    const savedKey = storageKey(repId);
    const [saved] = useState(() => {
        try {
            const raw = localStorage.getItem(savedKey);
            return raw ? JSON.parse(raw) : null;
        } catch {
            return null;
        }
    });
    const [rowEls, setRowEls] = useState<Elem[]>([]);
    const [rowParentNames, setRowParentNames] = useState([]);
    const [colEls, setColEls] = useState<Elem[]>([]);
    const [colParentNames, setColParentNames] = useState([]);
    const [rels, setRels] = useState<Rel[]>([]);
    const siriusIdMap = useRef<Record<string, string>>({}); // relId → siriusId for delete sync
    const [rowT, setRowT] = useState<Set<string>>(() =>
        saved?.rowT ? new Set(saved.rowT) : new Set(['Capability', 'OperationalNode', 'SystemNode', 'Organization', 'InformationExchange']));
    const [colT, setColT] = useState<Set<string>>(() =>
        saved?.colT ? new Set(saved.colT) : new Set(['Capability', 'OperationalNode', 'SystemNode', 'Organization', 'InformationExchange']));
    const [rowScope, setRowScope] = useState<string>(saved?.rowScope || '');
    const [rowScopeName, setRowScopeName] = useState<string>(saved?.rowScopeName || '');
    const [colScope, setColScope] = useState<string>(saved?.colScope || '');
    const [colScopeName, setColScopeName] = useState<string>(saved?.colScopeName || '');
    const [typePicker, setTypePicker] = useState<'row' | 'col' | null>(null);
    const [rt, setRt] = useState('Satisfy');
    const [tr, setTr] = useState(false);
    const [cfg, setCfg] = useState(true);
    const [sel, setSel] = useState<{ row: string; col: string } | null>(null);
    const [ctm, setCtm] = useState<{ x: number; y: number; type: string; id: string } | null>(null);
    const [hR, setHR] = useState<Set<string>>(new Set());
    const [hC, setHC] = useState<Set<string>>(new Set());
    const [sm, setSm] = useState('none');
    const [vm, setVm] = useState('all');
    const [undo, setUndo] = useState<Rel[][]>([]);
    const [redo, setRedo] = useState<Rel[][]>([]);

    const load = useCallback(async () => {
        try {
            const rt = [...rowT].join(',') || 'ALL';
            const ct = [...colT].join(',') || 'ALL';
            const ctxId = getEditingContextId();
            const buildParams = (types: string, scope: string) => {
                const p = new URLSearchParams({types});
                if (scope) p.set('scope', scope);
                p.set('ctxId', ctxId);
                return p;
            };
            const [rowRes, colRes, rel] = await Promise.all([
                fetch(API + '/elements?' + buildParams(rt, rowScope)).then(r => r.json()),
                fetch(API + '/elements?' + buildParams(ct, colScope)).then(r => r.json()),
                fetch(API + '/relations?ctxId=' + encodeURIComponent(ctxId)).then(r => r.json()),
            ]);
            // Extract scope meta entries (first element with type=__meta__)
            const extractMeta = (data: Elem[]) => {
                const meta = data.find((e) => e.type === '__meta__');
                let parent = [];
                if (meta) {
                    if (meta?.parentChain?.length) parent = meta.parentChain;
                    parent = parent.concat([meta.name])
                }
                const elements = data.filter((e) => e.type !== '__meta__');
                return {meta, elements, parent};
            };
            const rowData = extractMeta(rowRes);
            const colData = extractMeta(colRes);
            if (rowData.meta) setRowScopeName(rowData.meta.name);
            if (colData.meta) setColScopeName(colData.meta.name);
            setRowEls(rowData.elements);
            setRowParentNames(rowData.parent);
            setColEls(colData.elements);
            setColParentNames(colData.parent);
            setRels(rel);
        } catch (e) {
            console.error('[Matrix] load error:', e);
        }
    }, [rowT, colT, rowScope, colScope]);

    useEffect(() => {
        load();
    }, [load]);

    // Immediate reverse sync: when a GraphQL mutation changes the model (e.g. a node deleted from the
    // Explorer tree), refresh so any relation whose backing element was removed is pruned right away.
    useEffect(() => {
        let t: ReturnType<typeof setTimeout> | null = null;
        const onMutation = () => {
            if (t) clearTimeout(t);
            // small delay so the backend has persisted the change before we re-read/prune
            t = setTimeout(() => { load(); }, 400);
        };
        window.addEventListener('syson-model-mutation', onMutation);
        return () => {
            window.removeEventListener('syson-model-mutation', onMutation);
            if (t) clearTimeout(t);
        };
    }, [load]);

    // Persist config to localStorage
    useEffect(() => {
        try {
            localStorage.setItem(savedKey, JSON.stringify({
                rowScope, rowScopeName, colScope, colScopeName,
                rowT: [...rowT], colT: [...colT],
            }));
        } catch {
        }
    }, [rowScope, rowScopeName, colScope, colScopeName, rowT, colT, savedKey]);

    const matchesType = (elType: string, typeSet: Set<string>) => {
        if (typeSet.has(elType)) return true;
        // Map UI type labels to SysML class names
        const map: Record<string, string[]> = {
            'Requirement': ['RequirementUsage', 'RequirementDefinition'],
            'Block': ['PartUsage', 'PartDefinition', 'Block'],
            'Action': ['ActionUsage', 'ActionDefinition'],
            'Interface': ['InterfaceUsage', 'InterfaceDefinition'],
            'Function': ['Function'],
            'PartUsage': ['PartUsage'],
            'ActionUsage': ['ActionUsage'],
            'InterfaceUsage': ['InterfaceUsage'],
            'RequirementUsage': ['RequirementUsage'],
        };
        const candidates = map[elType] || [elType];
        for (const t of typeSet) {
            if (candidates.some(c => c === t || c.toLowerCase().includes(t.toLowerCase()))) return true;
        }
        // Check if any typeSet entry is a substring of the element type
        for (const t of typeSet) {
            if (elType.toLowerCase().includes(t.toLowerCase())) return true;
        }
        return false;
    };
    const rows = rowEls.filter(e => matchesType(e.dodafType || e.type, rowT));
    const cols = colEls.filter(e => matchesType(e.dodafType || e.type, colT));
    if (tr) { /* swap handled below */
    }

    const hr = (id: string) => rels.some(r => r.sourceId === id || r.targetId === id);
    const br = (vm === 'all') ? rows : rows.filter(e => vm === 'related' ? hr(e.id) : !hr(e.id));
    const bc = (vm === 'all') ? cols : cols.filter(e => vm === 'related' ? hr(e.id) : !hr(e.id));
    const sf = (a: Elem, b: Elem) => sm === 'id' ? a.id.localeCompare(b.id) : sm === 'name' ? a.name.localeCompare(b.name) : 0;
    const sR = (tr ? bc : br).sort(sf).filter(e => !hR.has(e.id));
    const sC = (tr ? br : bc).sort(sf).filter(e => !hC.has(e.id));
    const sRN = tr ? colParentNames : rowParentNames;
    const sCN = tr ? rowParentNames : colParentNames;

    const gcr = (ri: string, ci: string) => rels.filter(r => (r.sourceId === ri && r.targetId === ci) || (r.sourceId === ci && r.targetId === ri));

    const clickTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

    const handleCellClick = (rId: string, cId: string) => {
        if (clickTimer.current) {
            clearTimeout(clickTimer.current);
            clickTimer.current = null;
            return;
        }
        clickTimer.current = setTimeout(() => {
            setSel({row: rId, col: cId});
            clickTimer.current = null;
        }, 250);
    };
    const handleCellDblClick = (rId: string, cId: string) => {
        if (clickTimer.current) {
            clearTimeout(clickTimer.current);
            clickTimer.current = null;
        }
        addOrRemoveRel(rId, cId);
    };

    const addOrRemoveRel = async (ri: string, ci: string) => {
        console.log('[Matrix] addOrRemoveRel rt=' + rt, 'exLen=' + gcr(ri, ci).length);
        setUndo(p => [...p, [...rels]]);
        setRedo([]);
        const ex = gcr(ri, ci);
        if (ex.length > 0) {
            // Resolve the real editing context id once
            const ctxId = getEditingContextId();
            let realCtxId = ctxId;
            try {
                const toRes = await fetch(API + '/target-object-id?ctxId=' + ctxId);
                realCtxId = (await toRes.json()).editingContextId || ctxId;
            } catch {
            }
            for (const r of ex) {
                // Backend deletes the relation AND its backing SysML element (syncing the Explorer tree)
                await fetch(API + '/relations/' + r.id + '?ctxId=' + encodeURIComponent(realCtxId), {method: 'DELETE'});
                delete siriusIdMap.current[r.id];
            }
        } else if (rt) {
            // Create model element in Explorer tree via GraphQL createChild, then store the relation WITH its siriusId
            try {
                const ctxId = getEditingContextId();
                const toRes = await fetch(API + '/target-object-id?ctxId=' + ctxId + '&matrixRepId=' + encodeURIComponent(getRepresentationId()));
                const {targetObjectId, editingContextId: realCtxId} = await toRes.json();
                let siriusId = '';
                if (targetObjectId && realCtxId) {
                    const typeMap: Record<string, string> = {
                        Satisfy: 'SysMLv2EditService-SatisfyRequirementUsage',
                        Allocate: 'SysMLv2EditService-AllocationUsage',
                        Trace: 'SysMLv2EditService-Subclassification',
                        Dependency: 'SysMLv2EditService-Dependency',
                        Derive: 'SysMLv2EditService-Subclassification',
                    };
                    const childType = typeMap[rt] || 'SysMLv2EditService-PartUsage';
                    const srcName = rows.find(r => r.id === ri)?.name || ri.substring(0, 8);
                    const tgtName = cols.find(c => c.id === ci)?.name || ci.substring(0, 8);
                    const elName = srcName + '-' + tgtName;
                    console.log('[Matrix] Calling createChild:', {realCtxId, targetObjectId, childType, elName});
                    const uuid = crypto.randomUUID?.() || 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
                        const r = Math.random() * 16 | 0;
                        return (c === 'x' ? r : (r & 0x3 | 0x8)).toString(16);
                    });
                    const gqlRes = await fetch('/api/graphql', {
                        method: 'POST', headers: {'Content-Type': 'application/json'},
                        body: JSON.stringify({
                            query: 'mutation createChild($input: CreateChildInput!) { createChild(input: $input) { __typename ... on CreateChildSuccessPayload { object { id } } ... on ErrorPayload { message } } }',
                            variables: {
                                input: {
                                    id: uuid,
                                    editingContextId: realCtxId,
                                    objectId: targetObjectId,
                                    childCreationDescriptionId: childType
                                }
                            }
                        })
                    });
                    const gqlData = await gqlRes.json();
                    console.log('[Matrix] createChild result:', gqlData);
                    siriusId = gqlData?.data?.createChild?.object?.id || '';
                    if (siriusId) {
                        await fetch(API + '/rename-by-sirius', {
                            method: 'POST', headers: {'Content-Type': 'application/json'},
                            body: JSON.stringify({ctxId: realCtxId, siriusId, newName: elName})
                        });
                        console.log('[Matrix] renamed to:', elName);
                    }
                } else {
                    console.warn('[Matrix] No targetObjectId, skipping createChild');
                }
                // Store the relation with its backing siriusId so delete can sync the tree (survives refresh)
                const relRes = await fetch(API + '/relations', {
                    method: 'POST', headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify({sourceId: ri, targetId: ci, relationType: rt, siriusId, ctxId: realCtxId || ctxId})
                });
                const newRel = await relRes.json().catch(() => null);
                if (newRel?.id && siriusId) siriusIdMap.current[newRel.id] = siriusId;
            } catch (e) {
                console.warn('[Matrix] create relation failed:', e);
            }
        }
        load();
    };

    const handleDrop = (setScope: (v: string) => void, setName: (v: string) => void) => async (e: React.DragEvent) => {
        e.preventDefault();
        let pkgId = '';
        let pkgName = '';
        for (const t of e.dataTransfer.types) {
            try {
                const raw = e.dataTransfer.getData(t);
                // Try parsing as JSON first
                let parsed: any = raw;
                try {
                    parsed = JSON.parse(raw);
                } catch {
                }
                if (Array.isArray(parsed) && parsed.length > 0) {
                    const first = parsed[0];
                    pkgId = typeof first === 'string' ? first : first?.id || first?.elementId || first?.semanticElementId || '';
                    pkgName = first?.label || first?.name || first?.kind || '';
                } else if (parsed && typeof parsed === 'object') {
                    pkgId = parsed.id || parsed.elementId || parsed.semanticElementId || '';
                    pkgName = parsed.label || parsed.name || '';
                } else if (typeof raw === 'string' && raw.length < 500 && raw.length > 10) {
                    pkgId = raw;
                }
                if (pkgId) break;
            } catch {
            }
        }
        if (pkgId) {
            setScope(pkgId);
            setName(pkgName || pkgId.substring(0, 8) + '...');
            // Always resolve the display name from the backend (supports Package/ViewUsage) so that
            // re-dragging the SAME element (scope unchanged, load() not re-triggered) still shows the
            // element name instead of a truncated id.
            try {
                const ctxId = getEditingContextId();
                const res = await fetch(API + '/scope-name?ctxId=' + encodeURIComponent(ctxId) + '&id=' + encodeURIComponent(pkgId));
                const data = await res.json();
                if (data && data.name) setName(data.name);
            } catch {
            }
        }
    };

    const csv = () => {
        const h = ['行ID', '行名称', '行类型', ...sC.map(c => c.name)].join(',');
        const rs = sR.map(r => [r.id, r.name, r.type, ...sC.map(c => gcr(r.id, c.id).map(x => x.relationType).join(';'))].map(v => '"' + v + '"').join(','));
        const b = new Blob(['\uFEFF' + [h, ...rs].join('\n')], {type: 'text/csv;charset=utf-8'});
        const u = URL.createObjectURL(b);
        const a = document.createElement('a');
        a.href = u;
        a.download = 'matrix.csv';
        a.click();
        URL.revokeObjectURL(u);
    };

    const RW = 220,
        CW = 40,
        RH = 38;

    return (
        <div
            style={{
                display: 'flex',
                flexDirection: 'column',
                height: '100%',
                background: '#19284f',
                color: '#e0e0e0',
                fontFamily: 'system-ui',
            }}>
            {/* Toolbar */}
            <div
                style={{
                    display: 'flex',
                    gap: 6,
                    padding: '8px 12px',
                    background: '#19284f',
                    borderBottom: '1px solid rgba(255,255,255,0.08)',
                    alignItems: 'center',
                    flexShrink: 0,
                }}>
                <button onClick={load} style={bs} title={'刷新'}>
                    🔄
                </button>
                <button onClick={() => setTr(!tr)} style={bs} title={'行列互换'}>
                    🔀
                </button>
                <button onClick={csv} style={bs} title={'导出'}>
                    📥
                </button>
                <button
                    onClick={() => {
                        if (undo.length) {
                            setRedo((p) => [...p, [...rels]]);
                            setRels(undo[undo.length - 1]);
                            setUndo((p) => p.slice(0, -1));
                        }
                    }}
                    disabled={!undo.length}
                    style={{...bs, opacity: undo.length ? 1 : 0.4}}
                    title={'撤销'}>
                    ↩
                </button>
                <button
                    onClick={() => {
                        if (redo.length) {
                            setUndo((p) => [...p, [...rels]]);
                            setRels(redo[redo.length - 1]);
                            setRedo((p) => p.slice(0, -1));
                        }
                    }}
                    disabled={!redo.length}
                    style={{...bs, opacity: redo.length ? 1 : 0.4}}
                    title={'前进'}>
                    ↪
                </button>
                <button
                    onClick={() => setSm((s) => (s === 'none' ? 'id' : s === 'id' ? 'name' : 'none'))}
                    style={bs}
                    title={'排序'}>
                    {sm === 'none' ? '↕' : sm === 'id' ? 'ID↓' : '名↓'}
                </button>
                <button
                    onClick={() => {
                        setHR(new Set());
                        setHC(new Set());
                    }}
                    style={bs}>
                    重置
                </button>
                <span style={{flex: 1}}/>
                <select value={vm} onChange={(e) => setVm(e.target.value)}
                        style={{...ss, background: 'rgb(22, 34, 66)'}}>
                    <option value="all">全部</option>
                    <option value="related">有关</option>
                    <option value="unrelated">缺口</option>
                </select>
                <button onClick={() => setCfg(!cfg)} style={{...bs, background: '#475569'}}
                        title={cfg ? '收起' : '展开'}>
                    {cfg ? '◀' : '▶'}
                </button>
                <span style={{fontSize: 11, color: '#64748b'}}>
          行:{sR.length} 列:{sC.length} 关系:{rels.length}
        </span>
            </div>
            <div style={{flex: 1, display: 'flex', overflow: 'hidden'}}>
                {/* Config Panel */}
                {cfg && (
                    <div
                        style={{
                            width: 220,
                            background: '#19284f',
                            borderRight: '1px solid rgba(255,255,255,0.08)',
                            padding: 12,
                            overflowY: 'auto',
                            flexShrink: 0,
                            fontSize: 13,
                        }}>
                        <h4 style={{margin: '0 0 4px', color: '#94a3b8', fontSize: 12}}>行范围</h4>
                        <div style={{display: 'flex', gap: 4}}>
                            <input
                                value={rowScopeName || rowScope}
                                readOnly
                                style={{
                                    ...ss,
                                    flex: 1,
                                    cursor: 'pointer',
                                    background: 'rgb(22, 34, 66)',
                                    color: rowScope ? '#e2e8f0' : '#64748b',
                                }}
                                placeholder="拖动Package/ViewUsage至此处"
                                onDragOver={(e) => e.preventDefault()}
                                onDrop={handleDrop(setRowScope, setRowScopeName)}
                                title="从模型树拖拽Package或ViewUsage限定行范围"
                            />
                            {rowScope && (
                                <button
                                    onClick={() => {
                                        setRowScope('');
                                        setRowScopeName('');
                                    }}
                                    style={{...bs, padding: '4px 6px', fontSize: 11, color: '#fca5a5'}}>
                                    ✕
                                </button>
                            )}
                        </div>
                        <h4 style={{margin: '8px 0 4px', color: '#94a3b8', fontSize: 12}}>行类型</h4>
                        <div style={{display: 'flex', gap: 4}}>
                            <input
                                readOnly
                                value={[...rowT].map(typeLabel).join(', ') || '全部'}
                                style={{...ss, flex: 1, cursor: 'pointer', background: 'rgb(22, 34, 66)'}}
                                onClick={() => setTypePicker('row')}
                                title="点击选择元类型"
                            />
                            <button onClick={() => setTypePicker('row')} style={{...bs, padding: '4px 8px'}}>
                                ...
                            </button>
                        </div>
                        <h4 style={{margin: '12px 0 4px', color: '#94a3b8', fontSize: 12}}>列范围</h4>
                        <div style={{display: 'flex', gap: 4}}>
                            <input
                                value={colScopeName || colScope}
                                readOnly
                                style={{
                                    ...ss,
                                    flex: 1,
                                    cursor: 'pointer',
                                    background: 'rgb(22, 34, 66)',
                                    color: colScope ? '#e2e8f0' : '#64748b',
                                }}
                                placeholder="拖动Package/ViewUsage至此处"
                                onDragOver={(e) => e.preventDefault()}
                                onDrop={handleDrop(setColScope, setColScopeName)}
                                title="从模型树拖拽Package或ViewUsage限定列范围"
                            />
                            {colScope && (
                                <button
                                    onClick={() => {
                                        setColScope('');
                                        setColScopeName('');
                                    }}
                                    style={{...bs, padding: '4px 6px', fontSize: 11, color: '#fca5a5'}}>
                                    ✕
                                </button>
                            )}
                        </div>
                        <h4 style={{margin: '8px 0 4px', color: '#94a3b8', fontSize: 12}}>列类型</h4>
                        <div style={{display: 'flex', gap: 4}}>
                            <input
                                readOnly
                                value={[...colT].map(typeLabel).join(', ') || '全部'}
                                style={{...ss, flex: 1, cursor: 'pointer', background: 'rgb(22, 34, 66)'}}
                                onClick={() => setTypePicker('col')}
                                title="点击选择元类型"
                            />
                            <button onClick={() => setTypePicker('col')} style={{...bs, padding: '4px 8px'}}>
                                ...
                            </button>
                        </div>
                        <h4 style={{margin: '12px 0 4px', color: '#94a3b8', fontSize: 12}}>关联</h4>
                        {['Satisfy', 'Allocate', 'Dependency'].map((t) => (
                            <label
                                key={t}
                                style={{
                                    display: 'flex',
                                    alignItems: 'center',
                                    gap: 6,
                                    marginBottom: 3,
                                    cursor: 'pointer',
                                    fontSize: 12,
                                }}>
                                <input type="radio" name="relType" checked={rt === t} onChange={() => setRt(t)}/>
                                <span style={{width: 10, height: 10, background: C[t], borderRadius: 2}}/>
                                <span style={{color: '#cbd5e1'}}>{relLabel(t)}</span>
                            </label>
                        ))}
                        <h4 style={{margin: '12px 0 4px', color: '#94a3b8', fontSize: 12}}>图例</h4>
                        {['Satisfy', 'Allocate', 'Dependency'].map((t) => (
                            <div
                                key={t}
                                style={{
                                    display: 'flex',
                                    alignItems: 'center',
                                    gap: 6,
                                    fontSize: 11,
                                    marginBottom: 2,
                                    opacity: rt === t ? 1 : 0.4,
                                }}>
                                <span style={{color: C[t], fontWeight: 700, display: 'inline-block', width: 14, textAlign: 'center'}}>{S[t]}</span>
                                <span style={{width: 10, height: 10, background: C[t], borderRadius: 2}}/>
                                <span style={{color: '#94a3b8'}}>{relLabel(t)}</span>
                            </div>
                        ))}
                    </div>
                )}
                {/* Matrix: single table with sticky row + col headers */}
                <div style={{flex: 1, overflow: 'auto'}}>
                    <table style={{borderCollapse: 'separate', borderSpacing: 0, minWidth: RW * 3 + sC.length * CW}}>
                        <thead>
                        {/*列表的第一行表头*/}
                        <tr>
                            <th style={{...th, width: RW, left: 0, top: 0, zIndex: 3, ...bgs}} colSpan={2} rowSpan={2}>
                                {tr ? '列 \\ 行' : '行 \\ 列'}
                            </th>
                            {/*<th style={{ ...th, width: CW, position: 'sticky', top: 0, zIndex: 2, background: '#19284f' }}></th>*/}
                            <th
                                style={{...th, width: RW, position: 'sticky', left: 0, top: 0, zIndex: 3, ...bgs}}
                                colSpan={sC.length + 1}>
                                {sCN.map((c, ci) => (
                                    <div style={{paddingLeft: ci * 5}}>
                                        {ci != 0 && <span>└─</span>}
                                        {c}
                                    </div>
                                ))}
                            </th>
                            {/*{sC.map((c, ci) => (*/}
                            {/*  <th*/}
                            {/*    key={'th1-' + ci}*/}
                            {/*    style={{ ...th, width: CW, position: 'sticky', top: 0, zIndex: 2, background: '#19284f' }}></th>*/}
                            {/*))}*/}
                        </tr>
                        {/*列表的第二行表头*/}
                        <tr>
                            <th style={{...th, width: RW, position: 'sticky', top: 0, zIndex: 2, ...bgs}}></th>
                            {sC.map((c) => (
                                <th
                                    key={c.id}
                                    style={{
                                        ...th,
                                        width: CW,
                                        position: 'sticky',
                                        top: 0,
                                        zIndex: 2,
                                        background: '#19284f',
                                        writingMode: 'vertical-lr',
                                    }}
                                    onContextMenu={(e) => {
                                        e.preventDefault();
                                        setCtm({x: e.clientX, y: e.clientY, type: 'col', id: c.id});
                                    }}
                                    title={c.name + (c.dodafType ? ' (' + c.dodafType + ')' : '')}>
                                    <img
                                        src={typeIconUrl(c.dodafType, c.type)}
                                        alt={c.type}
                                        title={c.dodafType || c.type}
                                        width={16}
                                        height={16}
                                        style={{marginBottom: 6}}
                                        onError={(e) => {
                                            (e.currentTarget as HTMLImageElement).style.display = 'none';
                                        }}
                                    />
                                    {c.name}
                                </th>
                            ))}
                        </tr>
                        </thead>
                        <tbody>
                        {/*列表body的第一行内容*/}
                        <tr style={{height: RH}}>
                            <td
                                style={{...th, width: RW, position: 'sticky', left: 0, zIndex: 1, ...bgs}}
                                rowSpan={sR.length + 1}>
                                {sRN.map((c, ci) => (
                                    <div style={{paddingLeft: ci * 5}}>
                                        {ci != 0 && <span>└─</span>}
                                        {c}
                                    </div>
                                ))}
                            </td>
                            <td style={{...th, width: RW, position: 'sticky', left: 0, zIndex: 1, ...bgs}}></td>
                            <td style={{...td, width: CW, background: '#273B6CFF'}}></td>
                            {sC.map((c, ci) => (
                                <td key={'--' + ci} style={{...td, width: CW, background: '#273B6CFF'}}></td>
                            ))}
                        </tr>
                        {/*列表body的其他内容*/}
                        {sR.map((r, ri) => (
                            <tr key={r.id} style={{height: RH}}>
                                {/*<td style={{ ...th, width: RW, position: 'sticky', left: 0, zIndex: 1, background: '#19284f' }}></td>*/}
                                <td
                                    style={{
                                        ...th,
                                        width: RW,
                                        position: 'sticky',
                                        left: 0,
                                        zIndex: 1,
                                        background: '#19284f'
                                    }}
                                    onContextMenu={(e) => {
                                        e.preventDefault();
                                        setCtm({x: e.clientX, y: e.clientY, type: 'row', id: r.id});
                                    }}
                                    title={r.name + ' (' + r.type + ')'}>
                    <img
                        src={typeIconUrl(r.dodafType, r.type)}
                        alt={r.type}
                        title={r.dodafType || r.type}
                        width={16}
                        height={16}
                        style={{verticalAlign: 'middle', marginRight: 6}}
                        onError={(e) => {
                            (e.currentTarget as HTMLImageElement).style.display = 'none';
                        }}
                    />
                                    {r.name}
                                </td>
                                <td style={{...td, width: CW, background: '#273B6CFF'}}></td>
                                {sC.map((c, ci) => {
                                    const crs = gcr(r.id, c.id);
                                    return (
                                        <td
                                            key={ri + '-' + ci}
                                            style={{
                                                ...td,
                                                width: CW,
                                                background: crs.length ? 'rgba(59,130,246,0.08)' : 'transparent'
                                            }}
                                            onClick={() => handleCellClick(r.id, c.id)}
                                            onDoubleClick={() => handleCellDblClick(r.id, c.id)}
                                            title={crs.map((x) => x.relationType).join(', ') || '双击新建'}>
                                            {crs.map((x) => (
                                                <span
                                                    key={x.id}
                                                    style={{
                                                        color: C[x.relationType],
                                                        fontWeight: 700,
                                                        fontSize: 14,
                                                        margin: '0 1px'
                                                    }}>
                            {S[x.relationType]}
                          </span>
                                            ))}
                                        </td>
                                    );
                                })}
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>
            </div>
            {/* Context Menu */}
            {ctm && (
                <div
                    style={{
                        position: 'fixed',
                        left: ctm.x,
                        top: ctm.y,
                        zIndex: 2000,
                        background: '#19284f',
                        border: '1px solid rgba(255,255,255,0.1)',
                        borderRadius: 6,
                        minWidth: 140,
                        boxShadow: '0 8px 24px rgba(0,0,0,0.5)',
                    }}
                    onClick={() => setCtm(null)}>
                    <div
                        style={{
                            padding: '8px 12px',
                            cursor: 'pointer',
                            fontSize: 12,
                            color: '#cbd5e1',
                            borderBottom: '1px solid rgba(255,255,255,0.05)',
                        }}
                        onClick={() => {
                            ctm.type === 'row' ? setHR((p) => new Set([...p, ctm.id])) : setHC((p) => new Set([...p, ctm.id]));
                            setCtm(null);
                        }}>
                        👁️ 隐藏
                    </div>
                    <div
                        style={{padding: '8px 12px', cursor: 'pointer', fontSize: 12, color: '#cbd5e1'}}
                        onClick={() => {
                            setHR(new Set());
                            setHC(new Set());
                            setCtm(null);
                        }}>
                        🔄 恢复
                    </div>
                </div>
            )}
            {/* Detail Modal */}
            {sel && (
                <div
                    style={{
                        position: 'fixed',
                        inset: 0,
                        background: 'rgba(0,0,0,0.6)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        zIndex: 1000,
                    }}
                    onClick={() => setSel(null)}>
                    <div
                        style={{background: '#19284f', padding: 20, borderRadius: 10, minWidth: 350, color: '#e0e0e0'}}
                        onClick={(e) => e.stopPropagation()}>
                        <h3 style={{margin: '0 0 12px'}}>单元格关系</h3>
                        {gcr(sel.row, sel.col).length === 0 ? (
                            <p style={{color: '#64748b'}}>无关系</p>
                        ) : (
                            gcr(sel.row, sel.col).map((r) => (
                                <div
                                    key={r.id}
                                    style={{
                                        display: 'flex',
                                        alignItems: 'center',
                                        gap: 8,
                                        padding: '6px 0',
                                        borderBottom: '1px solid rgba(255,255,255,0.05)',
                                    }}>
                  <span style={{color: C[r.relationType], fontWeight: 700}}>
                    {S[r.relationType]} {relLabel(r.relationType)}
                  </span>
                                    <span style={{
                                        fontSize: 11,
                                        color: '#64748b'
                                    }}>{new Date(r.createdAt).toLocaleString()}</span>
                                    <button
                                        onClick={async () => {
                                            let rc = getEditingContextId();
                                            try {
                                                const tr = await fetch(API + '/target-object-id?ctxId=' + rc);
                                                rc = (await tr.json()).editingContextId || rc;
                                            } catch {
                                            }
                                            await fetch(API + '/relations/' + r.id + '?ctxId=' + encodeURIComponent(rc), {method: 'DELETE'});
                                            delete siriusIdMap.current[r.id];
                                            setSel(null);
                                            load();
                                        }}
                                        style={{
                                            marginLeft: 'auto',
                                            background: '#ef4444',
                                            color: '#fff',
                                            border: 'none',
                                            borderRadius: 3,
                                            padding: '2px 8px',
                                            cursor: 'pointer',
                                            fontSize: 11,
                                        }}>
                                        删除
                                    </button>
                                </div>
                            ))
                        )}
                        <button
                            onClick={() => setSel(null)}
                            style={{
                                marginTop: 12,
                                background: '#475569',
                                color: '#fff',
                                border: 'none',
                                borderRadius: 4,
                                padding: '6px 16px',
                                cursor: 'pointer',
                            }}>
                            关闭
                        </button>
                    </div>
                </div>
            )}
            {/* Type Picker Modal */}
            {typePicker && (
                <div
                    style={{
                        position: 'fixed',
                        inset: 0,
                        background: 'rgba(0,0,0,0.6)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        zIndex: 2000,
                        backdropFilter: 'blur(4px)',
                    }}
                    onClick={() => setTypePicker(null)}>
                    <div
                        style={{
                            background: '#19284f',
                            padding: 24,
                            borderRadius: 12,
                            minWidth: 280,
                            color: '#e0e0e0',
                            border: '1px solid rgba(255,255,255,0.1)',
                            boxShadow: '0 20px 60px rgba(0,0,0,0.5)',
                        }}
                        onClick={(e) => e.stopPropagation()}>
                        <h3 style={{margin: '0 0 16px', fontSize: 15, color: '#f1f5f9'}}>
                            选择{typePicker === 'row' ? '行' : '列'}元类型
                        </h3>
                        {TYPES.filter((t) => t !== 'ALL').map((t) => {
                            const sel = typePicker === 'row' ? rowT : colT;
                            const setFn = typePicker === 'row' ? setRowT : setColT;
                            return (
                                <label
                                    key={t}
                                    style={{
                                        display: 'flex',
                                        alignItems: 'center',
                                        gap: 8,
                                        padding: '6px 0',
                                        cursor: 'pointer',
                                        fontSize: 13,
                                    }}>
                                    <input
                                        type="checkbox"
                                        checked={sel.has(t)}
                                        onChange={() =>
                                            setFn((p) => {
                                                const s = new Set(p);
                                                s.has(t) ? s.delete(t) : s.add(t);
                                                return s;
                                            })
                                        }
                                    />
                                    <span style={{color: '#cbd5e1'}}>{typeLabel(t)}</span>
                                </label>
                            );
                        })}
                        <div style={{display: 'flex', gap: 8, marginTop: 16, justifyContent: 'space-between'}}>
                            <div style={{display: 'flex', gap: 8}}>
                                <button onClick={() => {
                                    const setFn = typePicker === 'row' ? setRowT : setColT;
                                    setFn(new Set(TYPES.filter(t => t !== 'ALL')));
                                }}
                                style={{background: '#475569', color: '#cbd5e1', border: 'none', padding: '6px 16px', borderRadius: 4, cursor: 'pointer'}}>
                                    全选
                                </button>
                                <button onClick={() => {
                                    const setFn = typePicker === 'row' ? setRowT : setColT;
                                    setFn(new Set());
                                }}
                                style={{background: '#475569', color: '#cbd5e1', border: 'none', padding: '6px 16px', borderRadius: 4, cursor: 'pointer'}}>
                                    全不选
                                </button>
                            </div>
                            <button
                                onClick={() => setTypePicker(null)}
                                style={{background: '#475569', color: '#cbd5e1', border: 'none', padding: '6px 16px', borderRadius: 4, cursor: 'pointer'}}>
                                确定
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}

const bs: React.CSSProperties = {
    background: '#334155',
    color: '#cbd5e1',
    border: '1px solid rgba(255,255,255,0.1)',
    padding: '5px 10px',
    borderRadius: 4,
    cursor: 'pointer',
    fontSize: 12,
    whiteSpace: 'nowrap',
};
const ss: React.CSSProperties = {
    width: '100%',
    padding: '6px 8px',
    borderRadius: 4,
    border: '1px solid rgba(255,255,255,0.1)',
    background: '#111827',
    color: '#cbd5e1',
    fontSize: 12,
};
const th: React.CSSProperties = {
    padding: '6px 10px',
    background: '#19284f',
    color: '#cbd5e1',
    fontSize: 12,
    fontWeight: 500,
    textAlign: 'left',
    borderBottom: '2px solid rgba(255,255,255,0.1)',
    borderRight: '1px solid rgba(255,255,255,0.06)',
    whiteSpace: 'nowrap',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
};
const td: React.CSSProperties = {
    padding: 0,
    textAlign: 'center',
    borderBottom: '1px solid rgba(255,255,255,0.04)',
    borderRight: '1px solid rgba(255,255,255,0.04)',
    cursor: 'pointer',
    fontSize: 14,
    verticalAlign: 'middle',
};
const bgs: React.CSSProperties = {
    backgroundColor: '#524a73',
}
