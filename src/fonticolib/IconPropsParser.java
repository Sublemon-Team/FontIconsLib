package fonticolib;

import arc.Core;
import arc.files.Fi;
import arc.graphics.Texture;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.TextureRegion;
import arc.math.geom.Vec2;
import arc.struct.ObjectIntMap;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Reflect;
import arc.util.Scaling;
import mindustry.game.Team;
import mindustry.ui.Fonts;

import java.util.Scanner;
import java.util.concurrent.atomic.AtomicReference;

import static mindustry.Vars.mods;

/*
Example file:
icon FF00 subvoyage-ceramic-burner
icon FF01 subvoyage-laser-icon laser
team-icon FF02 melius
 */
public class IconPropsParser {

    public static ObjectMap<String,Integer> modifiers =
            ObjectMap.of(
                    "icon",0,
                    "team-icon",1
            );
    public static ObjectMap<String,Integer> icons = new ObjectMap<>();
    public static ObjectMap<Team, Character> teamIcons = new ObjectMap<>();

    public static int lastAvailable = 128;

    public static void start() {
        teamIcons = new ObjectMap<>();
        icons = new ObjectMap<>();
        lastAvailable = 128; // it can get into reserved characters such as \n, so we use an offset

        Seq<Font> fonts = Seq.with(Fonts.def, Fonts.outline);
        Texture uitex = Core.atlas.find("logo").texture;
        int size = (int)(Fonts.def.getData().lineHeight/Fonts.def.getData().scaleY);

        Seq<Team> namedTeams = Seq.with();

        for (Team team : Team.all) {
            if(team.name == null || team.name.startsWith("team#")) continue;
            namedTeams.add(team);
        }

        Seq<Fi> found = sortedConfigs();

        for (Fi fi : found) {
            if(!fi.exists()) {
                Log.debug("[FontIconLib:@] Skipping, doesn't have icons",fi.nameWithoutExtension());
                continue;
            }
            if(fi.nameWithoutExtension().equals("yourmodid")) {
                Log.debug("[FontIconLib:@] Skipping, example file",fi.nameWithoutExtension());
                continue;
            }
            Log.debug("[FontIconLib:@] Processing...",fi.nameWithoutExtension());
            IconInfo[] info = read(fi);
            int line = 0;
            for (IconInfo iconInfo : info) {
                line++;
                TextureRegion region = Core.atlas.find(iconInfo.texture);
                if(!region.found()) {
                    Log.warn("[FontIconLib:@] Texture not found: @ | line @",fi.nameWithoutExtension(),iconInfo.texture,line);
                }
                if(region.texture != uitex && region.found()) {
                    Log.warn("[FontIconLib:@] Texture is not in UI atlas: @ | line @",fi.nameWithoutExtension(),iconInfo.id,line);
                    continue;
                }
                if(iconInfo.id == -1) iconInfo.autoId();
                int wsize = (int) (size * iconInfo.widthMultiplier);
                int hsize = (int) (size * iconInfo.heightMultiplier);
                Vec2 out = Scaling.fit.apply(region.width, region.height, wsize, hsize);
                Font.Glyph glyph = new Font.Glyph();
                glyph.id = iconInfo.id;
                glyph.srcX = 0;
                glyph.srcY = 0;
                glyph.width = (int) out.x;
                glyph.height = (int) out.y;
                glyph.u = region.u;
                glyph.v = region.v2;
                glyph.u2 = region.u2;
                glyph.v2 = region.v;
                glyph.xoffset = 0;
                glyph.yoffset = -hsize;
                glyph.xadvance = (int) (wsize + iconInfo.advance);
                glyph.kerning = null;
                glyph.fixedWidth = true;
                glyph.page = 0;

                fonts.each(f -> f.getData().setGlyph(iconInfo.id, glyph));
                icons.put(iconInfo.name,iconInfo.id);

                if(iconInfo.modifier == 1) {
                    Team team = namedTeams.find(t -> t.name.equals(iconInfo.team));
                    if(team == null)
                        Log.warn("[FontIconLib:@] Team not found: @ | line @",fi.nameWithoutExtension(),iconInfo.team,line);
                    if(team != null)
                        teamIcons.put(team,(char) iconInfo.id);
                }

                Log.debug("[FontIconLib:@] Loaded glyph: @ - @ - #@",fi.nameWithoutExtension(),iconInfo.id,iconInfo.name,Integer.toHexString(iconInfo.id));
            }
            Log.debug("[FontIconLib:@] Processed",fi.nameWithoutExtension());
        }
        updateVanilla();
        updateBundles();
    }

    public static void updateVanilla() {
        try {
            ObjectIntMap<String> unicode = Reflect.get(Fonts.class, "unicodeIcons");
            icons.each(unicode::put);
        } catch (Exception e) {

        }
        try {
            ObjectMap<String, String> string = Reflect.get(Fonts.class, "stringIcons");
            icons.each((k,v) -> string.put(k,((char) (int) v)+""));
        } catch (Exception e) {

        }
    }

    public static void updateBundles() {
        Log.info("[FontIconLib] Updating bundles");
        var bundle = Core.bundle;
        do {
            var props = bundle.getProperties();
            props.each((k,v) -> {
                String[] str = new String[] {v};
                icons.each((name,ch) ->
                        str[0] = str[0].replace("[fico-"+name+"]",((char) (int) ch)+""));
                props.put(k,str[0]);
            });
            bundle = bundle.getParent();
        }
        while(bundle != null);
    }

    public static Seq<Fi> sortedConfigs() {
        Seq<Fi> found = Seq.with();
        ObjectMap<Fi, Integer> priorities = new ObjectMap<>();


        mods.listFiles("icons",(m,fi) -> {
            if(!fi.extEquals("icons")) return;
            int index = found.size;
            int priority = 0;
            found.add(fi);
            try(Scanner scan = new Scanner(fi.read(512))) {
                if(scan.hasNextLine()) {
                    String line = scan.nextLine();
                    if(line.startsWith("priority ")) {
                        String str = line.replace("priority ","");
                        priority = Integer.parseInt(str);
                    }
                }
            }
            priorities.put(fi,priority);
        });

        found.sort((fi) -> -priorities.get(fi,0));

        return found;
    }

    public static IconInfo[] read(Fi fi) {
        String modId = fi.nameWithoutExtension();
        Seq<IconInfo> icos = Seq.with();

        try(Scanner scan = new Scanner(fi.read(512))) {
            int line = 0;
            while (scan.hasNextLine()) {
                line++;
                String lineStr = scan.nextLine();
                if(lineStr.isEmpty()) continue;
                if(lineStr.startsWith("#")) continue;
                if(lineStr.startsWith("priority ")) continue;
                String[] split = lineStr.split(" ");
                if(split.length < 3 || split.length > 7) {
                    Log.warn("[FontIconLib:@] Incorrect format | line @, @\n    @",modId,line,lineStr,"Expected format: <type> <hexId> <texture> [name] [width] [height] [advance]");
                    continue;
                }
                byte modifier = modifiers.get(split[0],
                        0).byteValue();
                String idHex = split[1];
                boolean autoId = idHex.equals("auto");
                int id = autoId ? -1 : parseCode(idHex);
                String texture = split[2].replace("@",modId);
                String name = texture;
                float widthMult = 1f;
                float heightMult = 1f;
                float advance = 0f;
                if(split.length > 3) name = split[3];
                if(split.length > 4) widthMult = Float.parseFloat(split[4]);
                if(split.length > 5) heightMult = Float.parseFloat(split[5]);
                if(split.length > 6) advance = Float.parseFloat(split[6]);
                var ico = new IconInfo(modifier, id, texture, name, modifier == 1 ? name : null, widthMult, heightMult, advance);
                icos.add(ico);
            }
        }
        return icos.toArray();
    }

    public static int parseCode(String code) {
        return Integer.parseInt(code.replace("#",""), code.startsWith("#") ? 16 : 10);
    }

    public static int lastAvailable() {
        for (int i = lastAvailable; i < 65536; i++) {
            if(isAvailable(i)) {
                lastAvailable = i;
                break;
            }
        }
        return lastAvailable;
    }

    public static boolean isAvailable(int glyph) {
        return font().getGlyph((char) glyph) == null || font().getGlyph((char) glyph) == font().missingGlyph || !font().hasGlyph((char) glyph);
    }

    public static void loadTeams() {
        teamIcons.each((team,ch) ->
                team.emoji = "[#"+team.color.toString()+"]"+ch+"[]");
    }

    public static Font.FontData font() {
        return Fonts.def.getData();
    }

    public static class IconInfo {

        public byte modifier;
        public int id;
        public String texture;
        public String name;
        public String team;

        public float widthMultiplier = 1f;
        public float heightMultiplier = 1f;
        public float advance = 0f;

        public IconInfo(byte modifier, int id, String texture, String name, String team, float width, float height, float advance) {
            this.modifier = modifier;
            this.id = id;
            this.texture = texture;
            this.name = name;
            this.team = team;
            this.widthMultiplier = width;
            this.heightMultiplier = height;
            this.advance = advance;
        }

        public void autoId() {
            if(isAvailable(lastAvailable)) id = lastAvailable;
            else id = lastAvailable();
        }
    }
}
