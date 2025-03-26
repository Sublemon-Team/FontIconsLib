package fonticolib;

import mindustry.mod.Mod;

public class IcoLib extends Mod{
    @Override
    public void init() {
        IconPropsParser.start();
        IconPropsParser.loadTeams();
    }
}
