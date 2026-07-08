package org.eclipse.syson.application.ov1;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for OV-1 icon library.
 */
@RestController
public class Ov1IconController {

    private final Ov1IconLibraryService iconLibraryService;

    public Ov1IconController(Ov1IconLibraryService iconLibraryService) {
        this.iconLibraryService = iconLibraryService;
    }

    @GetMapping("/api/ov1/icons")
    public ResponseEntity<Ov1IconLibraryService.Ov1IconLibrary> getIcons() {
        return ResponseEntity.ok(this.iconLibraryService.getIconLibrary());
    }

    @GetMapping("/api/ov1/icons/{faction}/{category}/{name}")
    public ResponseEntity<Resource> getIcon(
            @PathVariable String faction,
            @PathVariable String category,
            @PathVariable String name) {
        String path = "images/dodaf/ov1-icons/" + faction + "/" + category + "/" + name + ".svg";
        Resource resource = new ClassPathResource(path);
        if (resource.exists()) {
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("image/svg+xml"))
                    .body(resource);
        }
        return ResponseEntity.notFound().build();
    }
}
