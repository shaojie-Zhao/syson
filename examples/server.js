const express = require('express');
const path = require('path');
const fs = require('fs');
const { Pool } = require('pg');

const app = express();
const PORT = process.env.PORT || 3100;

// PostgreSQL connection (same as Spring Boot dev config)
const pool = new Pool({
  host: 'localhost',
  port: 5433,
  database: 'syson-db',
  user: 'dbuser',
  password: 'dbpwd',
});

// CORS
app.use((req, res, next) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET,PUT,POST,DELETE,OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');
  if (req.method === 'OPTIONS') return res.sendStatus(200);
  next();
});

// Request logging
app.use((req, res, next) => {
  console.log(`${new Date().toISOString()} ${req.method} ${req.url}`);
  next();
});

// Parse JSON request bodies
app.use(express.json({ limit: '10mb' }));

// Ensure layers storage directory exists
const layersDir = path.join(__dirname, 'layers');
if (!fs.existsSync(layersDir)) {
  fs.mkdirSync(layersDir, { recursive: true });
}

// --- Layer JSON persistence API ---

// GET  /api/layer/:representationId  →  load saved layer JSON
app.get('/api/layer/:representationId', (req, res) => {
  const filePath = path.join(layersDir, `${req.params.representationId}.json`);
  if (fs.existsSync(filePath)) {
    try {
      const data = fs.readFileSync(filePath, 'utf8');
      res.json(JSON.parse(data));
    } catch (e) {
      res.status(500).json({ error: 'Failed to read layer data' });
    }
  } else {
    res.status(404).json({ error: 'No saved layer' });
  }
});

// PUT  /api/layer/:representationId  →  save layer JSON
app.put('/api/layer/:representationId', (req, res) => {
  const filePath = path.join(layersDir, `${req.params.representationId}.json`);
  try {
    fs.writeFileSync(filePath, JSON.stringify(req.body, null, 2), 'utf8');
    res.json({ ok: true });
  } catch (e) {
    res.status(500).json({ error: 'Failed to save layer data' });
  }
});

// GET /api/targetObjectId/:representationId → lookup targetObjectId from PostgreSQL
app.get('/api/targetObjectId/:representationId', async (req, res) => {
  try {
    const result = await pool.query(
      'SELECT target_object_id FROM representation_metadata WHERE id LIKE $1',
      ['%' + req.params.representationId]
    );
    if (result.rows.length > 0 && result.rows[0].target_object_id) {
      res.json({ targetObjectId: result.rows[0].target_object_id });
    } else {
      res.status(404).json({ error: 'Not found' });
    }
  } catch (e) {
    console.error('DB query failed', e);
    res.status(500).json({ error: 'Database error' });
  }
});

// GET /api/elementId/:editingContextId/:objectId → map Sirius object ID to EMF elementId
app.get('/api/elementId/:editingContextId/:objectId', async (req, res) => {
  try {
    const result = await pool.query(
      'SELECT content FROM document WHERE semantic_data_id = $1 AND name LIKE $2',
      [req.params.editingContextId, '%.sysml']
    );
    for (const row of result.rows) {
      const text = typeof row.content === 'string' ? row.content : JSON.stringify(row.content);
      // Find the element with the given object ID and extract its elementId
      const idMatch = text.match(new RegExp('"id":"' + req.params.objectId + '"[^}]*'));
      if (idMatch) {
        const elemMatch = idMatch[0].match(/"elementId":"([^"]+)"/);
        if (elemMatch) {
          return res.json({ elementId: elemMatch[1] });
        }
      }
    }
    res.status(404).json({ error: 'Not found' });
  } catch (e) {
    console.error('elementId lookup failed', e);
    res.status(500).json({ error: 'DB error' });
  }
});

// GET /api/partUsageSiriusIds/:editingContextId/:targetObjectId → list PartUsage Sirius object IDs
app.get('/api/partUsageSiriusIds/:editingContextId/:targetObjectId', async (req, res) => {
  try {
    const docResult = await pool.query(
      'SELECT content FROM document WHERE semantic_data_id = $1 AND name LIKE $2',
      [req.params.editingContextId, '%.sysml']
    );
    var ids = [];
    function walk(obj, inVu) {
      if (!obj || typeof obj !== 'object') return;
      if (Array.isArray(obj)) { obj.forEach(function(v) { walk(v, inVu); }); return; }
      if (obj.eClass === 'sysml:ViewUsage' && obj.data && obj.data.elementId === req.params.targetObjectId) inVu = true;
      if (inVu && obj.eClass === 'sysml:PartUsage') ids.push(obj.id);
      for (var k of Object.keys(obj)) walk(obj[k], inVu);
    }
    for (const row of docResult.rows) {
      let doc = row.content;
      if (typeof doc === 'string') doc = JSON.parse(doc);
      walk(doc, false);
      if (ids.length > 0) break;
    }
    res.json(ids);
  } catch (e) { res.status(500).json({ error: 'DB error' }); }
});

// GET /api/partUsages/:editingContextId/:targetObjectId → list PartUsage children IDs (EMF elementId)
app.get('/api/partUsages/:editingContextId/:targetObjectId', async (req, res) => {
  try {
    const docResult = await pool.query(
      'SELECT content FROM document WHERE semantic_data_id = $1 AND name LIKE $2',
      [req.params.editingContextId, '%.sysml']
    );
    const ids = [];
    for (const row of docResult.rows) {
      let doc = row.content;
      if (typeof doc === 'string') doc = JSON.parse(doc);
      // Recursively walk JSON to find PartUsage elements under the ViewUsage
      function walk(obj, inVu) {
        if (!obj || typeof obj !== 'object') return;
        if (Array.isArray(obj)) { obj.forEach(function(v) { walk(v, inVu); }); return; }
        if (obj.eClass === 'sysml:ViewUsage' && obj.data && obj.data.elementId === req.params.targetObjectId) {
          inVu = true;
        }
        if (inVu && obj.eClass === 'sysml:PartUsage' && obj.data && obj.data.elementId) {
          ids.push(obj.data.elementId);  // EMF elementId
        }
        for (var k of Object.keys(obj)) walk(obj[k], inVu);
      }
      walk(doc, false);
      if (ids.length > 0) break;
    }
    res.json(ids);
  } catch (e) {
    console.error('partUsages query failed', e);
    res.status(500).json({ error: 'DB error' });
  }
});

// POST /api/partUsage/rename → Directly update declaredName in document.content JSON
app.post('/api/partUsage/rename', async (req, res) => {
  try {
    const { editingContextId, elementId, newName } = req.body;
    if (!editingContextId || !elementId || !newName) {
      return res.status(400).json({ error: 'Missing fields' });
    }
    const docResult = await pool.query(
      'SELECT id, content FROM document WHERE semantic_data_id = $1 AND name LIKE $2',
      [editingContextId, '%.sysml']
    );
    let updated = false;
    for (const row of docResult.rows) {
      try {
        let doc = row.content;
        if (typeof doc === 'string') doc = JSON.parse(doc);
        const text = JSON.stringify(doc);
        // Find the element with matching id and update its declaredName in the JSON
        if (text.indexOf('"id":"' + elementId + '"') >= 0) {
          // Use regex to find and replace declaredName in the element's data block
          const regex = new RegExp('("id":"' + elementId + '"[^}]*"declaredName":")([^"]*)(")', 'g');
          if (regex.test(text)) {
            const newText = text.replace(regex, '$1' + newName + '$3');
            const newDoc = JSON.parse(newText);
            await pool.query('UPDATE document SET content = $1 WHERE id = $2', [JSON.stringify(newDoc), row.id]);
            updated = true;
            break;
          }
        }
      } catch (e) { /* skip */ }
    }
    if (updated) {
      res.json({ ok: true });
    } else {
      res.status(404).json({ error: 'Element not found' });
    }
  } catch (e) {
    console.error('Rename failed', e);
    res.status(500).json({ error: 'DB error' });
  }
});
app.post('/api/partUsage/rename', async (req, res) => {
  try {
    const { editingContextId, elementId, newName } = req.body;
    if (!editingContextId || !elementId || !newName) {
      return res.status(400).json({ error: 'Missing fields' });
    }
    // Find the document containing this element and update declaredName in JSON
    const docResult = await pool.query(
      'SELECT id, content FROM document WHERE semantic_data_id = $1 AND name LIKE $2',
      [editingContextId, '%.sysml']
    );
    let updated = false;
    for (const row of docResult.rows) {
      try {
        var doc = row.content;
        if (typeof doc === 'string') doc = JSON.parse(doc);
        // Walk the JSON tree to find and rename the element
        function walk(obj) {
          if (!obj || typeof obj !== 'object') return false;
          if (obj.id === elementId && obj.data && typeof obj.data.declaredName === 'string') {
            obj.data.declaredName = newName;
            return true;
          }
          for (var key of Object.keys(obj)) {
            var val = obj[key];
            if (Array.isArray(val)) {
              for (var item of val) { if (walk(item)) return true; }
            } else if (typeof val === 'object') {
              if (walk(val)) return true;
            }
          }
          return false;
        }
        if (walk(doc)) {
          await pool.query('UPDATE document SET content = $1 WHERE id = $2', [JSON.stringify(doc), row.id]);
          updated = true;
          break;
        }
      } catch (e) { /* skip unparseable docs */ }
    }
    if (updated) {
      res.json({ ok: true });
    } else {
      res.status(404).json({ error: 'Element not found or no declaredName' });
    }
  } catch (e) {
    console.error('Rename failed', e);
    res.status(500).json({ error: 'Rename error' });
  }
});

// POST /api/partUsage/delete → directly remove PartUsage from document.content JSON
app.post('/api/partUsage/delete', async (req, res) => {
  try {
    const { editingContextId, elementId } = req.body;
    if (!editingContextId || !elementId) return res.status(400).json({ error: 'Missing fields' });
    const docResult = await pool.query(
      'SELECT id, content FROM document WHERE semantic_data_id = $1 AND name LIKE $2',
      [editingContextId, '%.sysml']
    );
    let deleted = false;
    for (const row of docResult.rows) {
      let doc = row.content;
      if (typeof doc === 'string') doc = JSON.parse(doc);
      const text = JSON.stringify(doc);
      // Find the element by id or elementId
      const searchId = elementId.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
      if (text.indexOf('"id":"' + elementId + '"') < 0 && text.indexOf('"elementId":"' + elementId + '"') < 0) continue;
      // Recursively walk and remove the element
      function removeById(arr, targetId) {
        if (!Array.isArray(arr)) return false;
        for (var i = arr.length - 1; i >= 0; i--) {
          var item = arr[i];
          if (item && typeof item === 'object') {
            if ((item.id === targetId || (item.data && item.data.elementId === targetId)) && item.eClass === 'sysml:PartUsage') {
              arr.splice(i, 1);
              return true;
            }
            for (var k of Object.keys(item)) {
              var v = item[k];
              if (Array.isArray(v) && removeById(v, targetId)) return true;
              if (v && typeof v === 'object' && removeById([v], targetId)) return true;
            }
          }
        }
      }
      function walkAndRemove(obj) {
        if (!obj || typeof obj !== 'object') return false;
        for (var k of Object.keys(obj)) {
          var v = obj[k];
          if (Array.isArray(v)) {
            if (removeById(v, elementId)) return true;
            for (var item of v) { if (item && typeof item === 'object' && walkAndRemove(item)) return true; }
          } else if (v && typeof v === 'object') {
            if (walkAndRemove(v)) return true;
          }
        }
        return false;
      }
      if (walkAndRemove(doc)) {
        await pool.query('UPDATE document SET content = $1 WHERE id = $2', [JSON.stringify(doc), row.id]);
        deleted = true;
        break;
      }
    }
    res.json({ ok: deleted });
  } catch (e) { console.error('Delete failed', e); res.status(500).json({ error: 'DB error' }); }
});

// PUT /api/mapping/:representationId → save symbol→PartUsage mapping for cleanup
app.put('/api/mapping/:representationId', async (req, res) => {
  try {
    const filePath = path.join(layersDir, `${req.params.representationId}.mapping.json`);
    fs.writeFileSync(filePath, JSON.stringify(req.body || {}, null, 2), 'utf8');
    res.json({ ok: true });
  } catch(e) { res.status(500).json({ error: 'Failed' }); }
});

// GET /api/mapping/:representationId → load mapping
app.get('/api/mapping/:representationId', async (req, res) => {
  try {
    const filePath = path.join(layersDir, `${req.params.representationId}.mapping.json`);
    if (fs.existsSync(filePath)) res.json(JSON.parse(fs.readFileSync(filePath, 'utf8')));
    else res.json({});
  } catch(e) { res.status(500).json({ error: 'Failed' }); }
});

// GET /api/partUsageNames/:editingContextId/:targetObjectId → list PartUsage declaredNames
app.get('/api/partUsageNames/:editingContextId/:targetObjectId', async (req, res) => {
  try {
    const docResult = await pool.query(
      'SELECT content FROM document WHERE semantic_data_id = $1 AND name LIKE $2',
      [req.params.editingContextId, '%.sysml']
    );
    const names = [];
    function walk(obj, inVu) {
      if (!obj || typeof obj !== 'object') return;
      if (Array.isArray(obj)) { obj.forEach(function(v) { walk(v, inVu); }); return; }
      if (obj.eClass === 'sysml:ViewUsage' && obj.data && obj.data.elementId === req.params.targetObjectId) inVu = true;
      if (inVu && obj.eClass === 'sysml:PartUsage' && obj.data) {
        names.push(obj.data.declaredName || '');
      }
      for (var k of Object.keys(obj)) walk(obj[k], inVu);
    }
    for (const row of docResult.rows) {
      let doc = row.content;
      if (typeof doc === 'string') doc = JSON.parse(doc);
      walk(doc, false);
      if (names.length > 0) break;
    }
    res.json(names);
  } catch (e) { res.status(500).json({ error: 'DB error' }); }
});

// POST /api/cleanupByPartUsage/:ecId/:partUsageId → remove symbol by PartUsage Sirius ID from all layer files
app.post('/api/cleanupByPartUsage/:editingContextId/:partUsageId', async (req, res) => {
  try {
    var files = fs.readdirSync(layersDir).filter(function(f) { return f.endsWith('.mapping.json'); });
    var cleaned = 0;
    for (var i = 0; i < files.length; i++) {
      var mappingPath = path.join(layersDir, files[i]);
      var mapping = JSON.parse(fs.readFileSync(mappingPath, 'utf8'));
      var repId = files[i].replace('.mapping.json', '');
      // Find symbolId for this PartUsage
      var targetSid = null;
      for (var sid in mapping) {
        if (mapping[sid] === req.params.partUsageId) { targetSid = sid; break; }
      }
      if (!targetSid) continue;
      // Remove from mapping
      delete mapping[targetSid];
      fs.writeFileSync(mappingPath, JSON.stringify(mapping, null, 2), 'utf8');
      // Remove from layer JSON
      var layerPath = path.join(layersDir, repId + '.json');
      if (fs.existsSync(layerPath)) {
        var layer = JSON.parse(fs.readFileSync(layerPath, 'utf8'));
        layer.symbols = (layer.symbols || []).filter(function(s) { return s._symbolId !== targetSid; });
        fs.writeFileSync(layerPath, JSON.stringify(layer, null, 2), 'utf8');
      }
      cleaned++;
    }
    res.json({ ok: true, cleaned: cleaned });
  } catch(e) { res.status(500).json({ error: 'Failed' }); }
});

// DELETE /api/layer/:representationId → delete saved layer
app.delete('/api/layer/:representationId', (req, res) => {
  const filePath = path.join(layersDir, `${req.params.representationId}.json`);
  try {
    if (fs.existsSync(filePath)) {
      fs.unlinkSync(filePath);
      res.json({ ok: true });
    } else {
      res.status(404).json({ error: 'No saved layer' });
    }
  } catch (e) {
    res.status(500).json({ error: 'Failed to delete layer data' });
  }
});

// No-cache middleware for development
app.use((req, res, next) => {
  res.setHeader('Cache-Control', 'no-store, no-cache, must-revalidate, max-age=0');
  res.setHeader('Pragma', 'no-cache');
  res.setHeader('Expires', '0');
  next();
});

// Serve static files from the current directory
app.use(express.static(__dirname, {
  etag: false,
  lastModified: false,
  setHeaders(res, filePath) {
    if (filePath.endsWith('.mjs')) {
      res.setHeader('Content-Type', 'application/javascript');
    }
  },
}));

// Default route
app.get('/', (req, res) => {
  res.sendFile(path.join(__dirname, 'plotting.html'));
});

app.listen(PORT, () => {
  console.log(`OV-1 Plotting Service running at http://localhost:${PORT}`);
});
