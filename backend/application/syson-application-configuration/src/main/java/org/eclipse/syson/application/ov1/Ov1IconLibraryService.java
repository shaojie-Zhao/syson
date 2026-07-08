package org.eclipse.syson.application.ov1;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

/**
 * OV-1 icon library service providing categorized military concept icons for operational views.
 */
@Service
public class Ov1IconLibraryService {

    public static final String ICONS_BASE = "/images/dodaf/ov1-icons";

    public Ov1IconLibrary getIconLibrary() {
        List<Ov1IconFaction> factions = new ArrayList<>();

        factions.add(createFaction("red", "红方", new Object[][]{
            {"c2", "指挥控制", "hq", "command-post", "tac-ops"},
            {"sensor", "探测感知", "radar", "sonar", "satellite", "uav"},
            {"strike", "打击", "missile", "artillery", "aircraft"},
            {"comm", "通信", "radio", "satcom"},
            {"platform", "平台", "surface-ship", "submarine", "ground-unit"},
        }));

        factions.add(createFaction("blue", "蓝方", new Object[][]{
            {"c2", "指挥控制", "hq", "command-post", "tac-ops"},
            {"sensor", "探测感知", "radar", "sonar", "satellite", "uav"},
            {"strike", "打击", "missile", "artillery", "aircraft"},
            {"comm", "通信", "radio", "satcom"},
            {"platform", "平台", "surface-ship", "submarine", "ground-unit"},
        }));

        factions.add(createFaction("neutral", "中立/未知", new Object[][]{
            {"other", "其他", "merchant", "unknown"},
        }));

        return new Ov1IconLibrary(factions);
    }

    private Ov1IconFaction createFaction(String id, String label, Object[][] categories) {
        List<Ov1IconCategory> cats = new ArrayList<>();
        for (Object[] cat : categories) {
            String catId = (String) cat[0];
            String catLabel = (String) cat[1];
            List<Ov1Icon> icons = new ArrayList<>();
            for (int i = 2; i < cat.length; i++) {
                String iconName = (String) cat[i];
                icons.add(new Ov1Icon(
                    id + "/" + catId + "/" + iconName,
                    iconName,
                    ICONS_BASE + "/" + id + "/" + catId + "/" + iconName + ".svg"
                ));
            }
            cats.add(new Ov1IconCategory(catId, catLabel, icons));
        }
        return new Ov1IconFaction(id, label, cats);
    }

    // DTOs
    public record Ov1IconLibrary(List<Ov1IconFaction> factions) {}
    public record Ov1IconFaction(String id, String label, List<Ov1IconCategory> categories) {}
    public record Ov1IconCategory(String id, String label, List<Ov1Icon> icons) {}
    public record Ov1Icon(String id, String name, String url) {}
}
