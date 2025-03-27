package fonticolib;

import arc.Events;
import mindustry.game.EventType;
import mindustry.mod.Mod;

public class IcoLib extends Mod{
    public IcoLib() {
        Events.on(EventType.ClientLoadEvent.class, event -> {
            IconPropsParser.start();
            IconPropsParser.loadTeams();
        });
    }
}
