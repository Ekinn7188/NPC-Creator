package jeeper.nmstest.commands;

import com.google.common.base.Charsets;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.datafixers.util.Pair;
import jeeper.nmstest.Main;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.io.IOUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class SpawnNPC implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            return true;
        }

        CraftPlayer craftPlayer = (CraftPlayer) player; //craftbukkit
        ServerPlayer serverPlayer = craftPlayer.getHandle(); // nms
        var connection = serverPlayer.connection;

        MinecraftServer server = serverPlayer.server;
        ServerLevel level = serverPlayer.getLevel();


        GameProfile profile;

        //https://api.mojang.com/users/profiles/minecraft/<name> - Get UUID
        //https://sessionserver.mojang.com/session/minecraft/profile/UUID?unsigned=false - Get Skin
        if (args.length >= 1){
            profile = new GameProfile(UUID.randomUUID(), args[0]);
            try {
                //get uuid
                String uuidURL = "https://api.mojang.com/users/profiles/minecraft/" + args[0];
                String uuidJSON = IOUtils.toString(new URL(uuidURL), Charsets.UTF_8);
                if (uuidJSON.isEmpty()) throw new IOException("Empty UUID JSON");
                JSONObject uuidObject = (JSONObject) JSONValue.parseWithException(uuidJSON);
                String uuid = uuidObject.get("id").toString();

                //get skin texture and signature
                String skinURL = "https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false";
                String skinJSON = IOUtils.toString(new URL(skinURL), Charsets.UTF_8);
                if (skinJSON.isEmpty()) throw new IOException("Empty Skin JSON");
                JSONObject skinObject = (JSONObject) JSONValue.parseWithException(skinJSON);
                JSONObject properties = (JSONObject) ((JSONArray)skinObject.get("properties")).get(0);
                String texture = properties.get("value").toString();
                String signature = properties.get("signature").toString();

                profile.getProperties().put("textures", new Property("textures", texture, signature));



            } catch (IOException | ParseException e) {
                e.printStackTrace();
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Can't Connect Name to A Real Player"));
            }


        } else {
            profile = new GameProfile(UUID.randomUUID(), player.getName());

            serverPlayer.getGameProfile().getProperties().get("textures").forEach(property -> {
                profile.getProperties().put("textures", property);
            });
        }

        ServerPlayer npc = new ServerPlayer(server, level, profile);

        npc.setPos(player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ());

        npc.getEntityData().set(new EntityDataAccessor<>(17, EntityDataSerializers.BYTE), (byte) 127);

        ClientboundPlayerInfoPacket playerInfoPacket =
                new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.ADD_PLAYER, npc);

        ClientboundAddPlayerPacket addPlayerPacket =
                new ClientboundAddPlayerPacket(npc);

        List<Pair<EquipmentSlot, ItemStack>> equipment = new ArrayList<>();
        org.bukkit.inventory.ItemStack[] armor = player.getEquipment().getArmorContents();

        for (int i = 0; i < armor.length; i++) {
            if (armor[i] == null) {
                continue;
            }
            equipment.add(Pair.of(EquipmentSlot.values()[i+2], CraftItemStack.asNMSCopy(armor[i])));
        }

       /* equipment.add(Pair.of(EquipmentSlot.HEAD, CraftItemStack.asNMSCopy(new org.bukkit.inventory.ItemStack(Material.DIAMOND_HELMET))));
        equipment.add(Pair.of(EquipmentSlot.CHEST, CraftItemStack.asNMSCopy(new org.bukkit.inventory.ItemStack(Material.DIAMOND_CHESTPLATE))));
        equipment.add(Pair.of(EquipmentSlot.LEGS, CraftItemStack.asNMSCopy(new org.bukkit.inventory.ItemStack(Material.DIAMOND_LEGGINGS))));
        equipment.add(Pair.of(EquipmentSlot.FEET, CraftItemStack.asNMSCopy(new org.bukkit.inventory.ItemStack(Material.DIAMOND_BOOTS))));
        equipment.add(Pair.of(EquipmentSlot.MAINHAND, CraftItemStack.asNMSCopy(new org.bukkit.inventory.ItemStack(Material.DIAMOND_SWORD))));
        equipment.add(Pair.of(EquipmentSlot.OFFHAND, CraftItemStack.asNMSCopy(new org.bukkit.inventory.ItemStack(Material.SHIELD))));*/

        final ClientboundSetEquipmentPacket setEquipmentPacket;
        if (equipment.size() > 0) {
             setEquipmentPacket= new ClientboundSetEquipmentPacket(npc.getBukkitEntity().getEntityId(),
                            equipment);
        } else {
            setEquipmentPacket = null;
        }

        //send packet to everyone
        Bukkit.getOnlinePlayers().forEach(onlinePlayer -> {
            CraftPlayer onlineCraftPlayer = (CraftPlayer) player; //craftbukkit
            ServerPlayer onlineServerPlayer = craftPlayer.getHandle(); // nms
            var onlineConnection = serverPlayer.connection;

            onlineConnection.send(playerInfoPacket);
            onlineConnection.send(addPlayerPacket);
            if (setEquipmentPacket != null) {
                onlineConnection.send(setEquipmentPacket);
            }
        });

        Main.getNPCs().add(npc);

        return true;

    }

    public static class MovementListener implements Listener {

        @EventHandler
        public void onMove(PlayerMoveEvent e) {

            Main.getNPCs().forEach(
                    npc -> {
                        var connection = ((CraftPlayer)e.getPlayer()).getHandle().connection;
                        Location location = npc.getBukkitEntity().getLocation();
                        location.setDirection(e.getPlayer().getLocation().subtract(location).toVector());
                        float yaw = location.getYaw();
                        float pitch = location.getPitch();

                        //Rotate head = horizontal
                        connection.send(new ClientboundRotateHeadPacket(npc,
                                (byte) ((yaw%360)*256/360)));

                        //Move Entity = vertical
                        connection.send(new ClientboundMoveEntityPacket.Rot(npc.getBukkitEntity().getEntityId(),
                                (byte) ((yaw%360)*256/360), (byte) ((pitch%360)*256/360), true));
                    }
            );

        }
    }
}


