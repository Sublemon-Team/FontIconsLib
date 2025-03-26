package fonticolib;

import arc.Core;
import arc.files.Fi;
import arc.graphics.Texture;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.TextureRegion;
import arc.math.geom.Vec2;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Scaling;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.ui.Fonts;

import java.util.Scanner;

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
    public static ObjectMap<String,Character> icons = new ObjectMap<>();
    public static ObjectMap<Team, Character> teamIcons = new ObjectMap<>();

    public static void start() {
        teamIcons = new ObjectMap<>();
        icons = new ObjectMap<>();
        Seq<Fi> found = Seq.with(Vars.tree.get("assets/icons").list()).select(f -> f.extEquals("icons"));
        Seq<Font> fonts = Seq.with(Fonts.def, Fonts.outline);
        Texture uitex = Core.atlas.find("logo").texture;
        int size = (int)(Fonts.def.getData().lineHeight/Fonts.def.getData().scaleY);

        Seq<Team> namedTeams = Seq.with();

        for (Team team : Team.all) {
            if(team.name == null || team.name.startsWith("team#")) continue;
            namedTeams.add(team);
        }

        for (Fi fi : found) {
            if(fi.nameWithoutExtension().equals("yourmodid")) continue;
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
                Vec2 out = Scaling.fit.apply(region.width, region.height, size, size);
                Font.Glyph glyph = new Font.Glyph();
                glyph.id = iconInfo.id;
                glyph.srcX = 0;
                glyph.srcY = 0;
                glyph.width = (int)out.x;
                glyph.height = (int)out.y;
                glyph.u = region.u;
                glyph.v = region.v2;
                glyph.u2 = region.u2;
                glyph.v2 = region.v;
                glyph.xoffset = 0;
                glyph.yoffset = -size;
                glyph.xadvance = size;
                glyph.kerning = null;
                glyph.fixedWidth = true;
                glyph.page = 0;

                fonts.each(f -> f.getData().setGlyph(iconInfo.id, glyph));
                icons.put(iconInfo.name,(char) iconInfo.id);

                if(iconInfo.modifier == 1) {
                    Team team = namedTeams.find(t -> t.name.equals(iconInfo.team));
                    if(team == null)
                        Log.warn("[FontIconLib:@] Team not found: @ | line @",fi.nameWithoutExtension(),iconInfo.team,line);
                    if(team != null)
                        teamIcons.put(team,(char) iconInfo.id);
                }

                Log.debug("[FontIconLib:@] Loaded glyph: @ - @ - #@",fi.nameWithoutExtension(),iconInfo.id,iconInfo.name,Integer.toHexString(iconInfo.id));
            }
        }
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
                String[] split = lineStr.split(" ");
                if(split.length < 3 || split.length > 4) {
                    Log.warn("[FontIconLib:@] Incorrect format | line @, @\n    @",modId,line,lineStr,"Expected format: <type> <hexId> <texture> [name]");
                    continue;
                }
                byte modifier = modifiers.get(split[0],
                        0).byteValue();
                String idHex = split[1];
                int id = Integer.parseInt(idHex, idHex.startsWith("#") ? 16 : 10);
                String texture = split[2].replace("@",modId);
                String name = texture;
                if(split.length > 3) name = split[3];
                icos.add(new IconInfo(modifier, id, texture, name, modifier == 1 ? name : null));
            }
        }
        return icos.toArray();
    }

    public static void loadTeams() {
        teamIcons.each((team,ch) ->
                team.emoji = "[#"+team.color.toString()+"]"+ch+"[]");
    }

    public static class IconInfo {

        public byte modifier;
        public int id;
        public String texture;
        public String name;
        public String team;

        public IconInfo(byte modifier, int id, String texture, String name, String team) {
            this.modifier = modifier;
            this.id = id;
            this.texture = texture;
            this.name = name;
            this.team = team;
        }
    }
}
