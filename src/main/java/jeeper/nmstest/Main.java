package jeeper.nmstest;
import jeeper.nmstest.commands.SpawnNPC;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Main extends JavaPlugin {

    private static List<ServerPlayer> npcs = new ArrayList<>();
    private static Main plugin;

    @Override
    public void onEnable() {
        plugin = this;
        //register command
        Objects.requireNonNull(getCommand("spawnnpc")).setExecutor(new SpawnNPC());
        getServer().getPluginManager().registerEvents(new SpawnNPC.MovementListener(), this);
    }

    public static List<ServerPlayer> getNPCs() {
        return npcs;
    }

    public static Main getPlugin() {
        return plugin;
    }
}
