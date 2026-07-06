/*******************************************************************************
 * Copyright (c) 2023, 2024 Obeo.
 * ...
 *******************************************************************************/
package org.eclipse.syson;

import java.util.Locale;
import java.util.Map;

import org.eclipse.syson.standard.diagrams.view.services.DoDAFGanttDataService;
import org.eclipse.syson.standard.diagrams.view.services.DoDAFMatrixDataService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@SpringBootApplication
@ComponentScan(basePackages = { "org.eclipse.syson", "org.eclipse.sirius.web", "org.eclipse.sirius.components" })
public class SysONApplication {

    public static void main(String[] args) {
        Locale.setDefault(Locale.SIMPLIFIED_CHINESE);
        SpringApplication.run(SysONApplication.class, args);
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/gantt/**")
                        .allowedOrigins("*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
            }
        };
    }

    @Bean
    public Filter corsFilter() {
        return (ServletRequest req, ServletResponse res, FilterChain chain) -> {
            var request = (HttpServletRequest) req;
            var response = (HttpServletResponse) res;
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "*");
            if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
                response.setStatus(200);
            } else {
                chain.doFilter(req, res);
            }
        };
    }

    @Bean
    public RouterFunction<ServerResponse> ganttRoutes() {
        return RouterFunctions.route()
                .GET("/api/gantt/{repId}/tasks", request -> {
                    var tasks = DoDAFGanttDataService.getTasks(request.pathVariable("repId"));
                    return ServerResponse.ok().body(tasks);
                })
                .POST("/api/gantt/{repId}/tasks", request -> {
                    var parentId = request.param("parentId").orElse(null);
                    var task = DoDAFGanttDataService.createTask(request.pathVariable("repId"), parentId);
                    return ServerResponse.ok().body(task);
                })
                .PUT("/api/gantt/{repId}/tasks/{taskId}", request -> {
                    var body = request.body(Map.class);
                    var task = new DoDAFGanttDataService.GanttTaskData(
                            request.pathVariable("taskId"),
                            (String) body.getOrDefault("parentId", null),
                            (String) body.getOrDefault("name", ""),
                            (String) body.getOrDefault("description", ""),
                            (String) body.getOrDefault("startDate", "2026-01-01"),
                            (String) body.getOrDefault("endDate", "2026-01-31"),
                            body.containsKey("progress") ? ((Number) body.get("progress")).intValue() : 0
                    );
                    var updated = DoDAFGanttDataService.updateTask(request.pathVariable("repId"), task);
                    return ServerResponse.ok().body(updated);
                })
                .DELETE("/api/gantt/{repId}/tasks/{taskId}", request -> {
                    DoDAFGanttDataService.deleteTask(request.pathVariable("repId"), request.pathVariable("taskId"));
                    return ServerResponse.ok().build();
                })
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> matrixRoutes(DoDAFMatrixDataService matrixDataService) {
        return RouterFunctions.route()
                .GET("/api/matrix/{repId}/element-id", request -> {
                    var ctxId = request.param("ctxId").orElse("");
                    var objectId = request.param("objectId").orElse("");
                    var elementId = matrixDataService.getElementIdByObjectId(ctxId, objectId);
                    return ServerResponse.ok().body(Map.of("elementId", elementId != null ? elementId : ""));
                })
                .POST("/api/matrix/{repId}/rename-by-sirius", request -> {
                    @SuppressWarnings("unchecked")
                    var body = (Map<String,String>) request.body(Map.class);
                    var ctxId = body.getOrDefault("ctxId", "");
                    var siriusId = body.getOrDefault("siriusId", "");
                    var newName = body.getOrDefault("newName", "");
                    boolean ok = matrixDataService.renameBySiriusId(ctxId, siriusId, newName);
                    return ServerResponse.ok().body(Map.of("result", ok ? "ok" : "failed"));
                })
                .build();
    }
}
