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
package org.eclipse.syson.standard.diagrams.view.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * REST controller for DoDAF Prediction View CRUD operations.
 */
@RestController
@RequestMapping("/api/prediction/default-prediction")
public class DoDAFPredictionController {
    private static final Logger log = LoggerFactory.getLogger(DoDAFPredictionController.class);
    private final Map<String, PredictionRow> rows = new ConcurrentHashMap<>();
    private final AtomicInteger seq = new AtomicInteger(0);

    public static class PredictionRow {
        public String id;
        public String domain;    // 技术和技能领域
        public String skill;     // 技术和技能
        public String shortTerm; // 短期
        public String midTerm;   // 中期
        public String longTerm;  // 长期
    }

    public DoDAFPredictionController() {
        // Demo data
        addDemoRow("水面作战", "雷达探测技术", "相控阵雷达升级", "量子雷达", "AI驱动探测");
        addDemoRow("水下作战", "声纳技术", "低频声纳", "光纤传感", "生物声纳");
        addDemoRow("航空反潜", "磁异常探测", "高灵敏度MAD", "超导磁探", "分布式磁探网");
        addDemoRow("指挥控制", "数据融合技术", "多源融合平台", "认知决策辅助", "自主指挥系统");
    }

    private void addDemoRow(String domain, String skill, String shortTerm, String midTerm, String longTerm) {
        int id = seq.incrementAndGet();
        PredictionRow r = new PredictionRow();
        r.id = String.valueOf(id);
        r.domain = domain; r.skill = skill;
        r.shortTerm = shortTerm; r.midTerm = midTerm; r.longTerm = longTerm;
        rows.put(r.id, r);
    }

    @GetMapping("/list")
    public List<PredictionRow> list(@RequestParam(defaultValue = "") String ctxId) {
        log.info("GET /list ctxId={}", ctxId);
        return new ArrayList<>(rows.values());
    }

    @PostMapping("/add")
    public PredictionRow add(@RequestBody Map<String, String> body) {
        int id = seq.incrementAndGet();
        PredictionRow r = new PredictionRow();
        r.id = String.valueOf(id);
        r.domain = body.getOrDefault("domain", "");
        r.skill = body.getOrDefault("skill", "");
        r.shortTerm = body.getOrDefault("shortTerm", "");
        r.midTerm = body.getOrDefault("midTerm", "");
        r.longTerm = body.getOrDefault("longTerm", "");
        rows.put(r.id, r);
        log.info("POST /add id={}", r.id);
        return r;
    }

    @PutMapping("/{id}")
    public PredictionRow update(@PathVariable String id, @RequestBody Map<String, String> body) {
        PredictionRow r = rows.get(id);
        if (r == null) return null;
        String field = body.get("field");
        String value = body.getOrDefault("value", "");
        if (field != null) {
            switch (field) {
                case "domain": r.domain = value; break;
                case "skill": r.skill = value; break;
                case "shortTerm": r.shortTerm = value; break;
                case "midTerm": r.midTerm = value; break;
                case "longTerm": r.longTerm = value; break;
            }
        }
        return r;
    }

    @DeleteMapping("/{id}")
    public Map<String, String> delete(@PathVariable String id) {
        rows.remove(id);
        log.info("DELETE /" + id);
        return Map.of("result", "ok");
    }
}
