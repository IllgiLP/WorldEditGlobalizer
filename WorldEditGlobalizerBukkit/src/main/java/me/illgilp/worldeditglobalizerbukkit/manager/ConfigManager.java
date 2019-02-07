package me.illgilp.worldeditglobalizerbukkit.manager;

import me.illgilp.worldeditglobalizerbukkit.network.PacketSender;
import me.illgilp.worldeditglobalizercommon.network.packets.PluginConfigRequestPacket;
import me.illgilp.worldeditglobalizercommon.network.packets.PluginConfigResponsePacket;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ConfigManager {

    private static ConfigManager instance;

    private Map<UUID, String> tmpName = new HashMap<>();
    private Map<UUID, PluginConfigResponsePacket> tmpResponse = new HashMap<>();


    public PluginConfigResponsePacket getPluginConfig(Player player) {
        if (player == null) {
            PluginConfigResponsePacket res = new PluginConfigResponsePacket();
            res.setKeepClipboard(false);
            res.setLanguage("en");
            res.setPrefix("§3WEG §7§l>> §r");
            res.setMaxClipboardSize(1024 * 1024 * 30);
            return res;
        }
        PluginConfigRequestPacket req = new PluginConfigRequestPacket();
        tmpName.put(req.getIdentifier(), player.getName());
        PacketSender.sendPacket(player, req);
        synchronized (tmpName.get(req.getIdentifier())) {
            try {
                tmpName.get(req.getIdentifier()).wait(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (tmpResponse.containsKey(req.getIdentifier())) {
            PluginConfigResponsePacket res = tmpResponse.get(req.getIdentifier());
            tmpName.remove(req.getIdentifier());
            tmpResponse.remove(req.getIdentifier());
            return res;
        } else {
            PluginConfigResponsePacket res = new PluginConfigResponsePacket();
            res.setKeepClipboard(false);
            res.setLanguage("en");
            res.setPrefix("§3WEG §7§l>> §r");
            res.setMaxClipboardSize(1024 * 1024 * 30);
            tmpName.remove(req.getIdentifier());
            tmpResponse.remove(req.getIdentifier());
            return res;
        }

    }

    public void callPLuginConfigResponse(PluginConfigResponsePacket packet) {
        if (tmpName.containsKey(packet.getIdentifier())) {
            tmpResponse.put(packet.getIdentifier(), packet);
            synchronized (tmpName.get(packet.getIdentifier())) {
                tmpName.get(packet.getIdentifier()).notify();
            }
        }
    }

    public static ConfigManager getInstance() {
        if (instance == null) instance = new ConfigManager();
        return instance;
    }
}
