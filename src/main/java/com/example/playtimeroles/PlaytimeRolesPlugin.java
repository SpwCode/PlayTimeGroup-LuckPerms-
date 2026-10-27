package com.example.playtimeroles;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class PlaytimeRolesPlugin extends JavaPlugin implements Listener {

    private LuckPerms luckPerms;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);

        try {
            luckPerms = LuckPermsProvider.get();
        } catch (IllegalStateException e) {
            getLogger().severe("LuckPerms not found! Plugin disabled.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();
        getCommand("playtime").setExecutor(new PlaytimeCommand(this));

        getLogger().info("PlaytimeRolesPlugin enabled! (Join-check only mode)");
    }

    @Override
    public void onDisable() {
        getLogger().info("PlaytimeRolesPlugin disabled!");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Проверка запускается асинхронно исключительно при входе игрока на сервер
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> checkAndGrantRolesForPlayer(player));
    }

    public long getTotalPlaytime(Player player) {
        if (player == null) return 0L;

        // Безопасное чтение статистики сервера из асинхронного потока
        try {
            return Bukkit.getScheduler().callSyncMethod(this, () -> {
                long ticks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
                return ticks * 50L;
            }).get(1, TimeUnit.SECONDS);
        } catch (Exception e) {
            return 0L;
        }
    }

    private void checkAndGrantRolesForPlayer(Player player) {
        if (!player.isOnline()) return;

        FileConfiguration config = getConfig();
        List<Map<?, ?>> roles = config.getMapList("roles");

        UUID uuid = player.getUniqueId();
        long totalPlaytime = getTotalPlaytime(player);

        for (Map<?, ?> roleMap : roles) {
            String role = (String) roleMap.get("role");
            long requiredTime = parseTime((String) roleMap.get("time"));

            if (totalPlaytime >= requiredTime) {
                grantRole(uuid, role);
            }
        }
    }

    private long parseTime(String timeStr) {
        if (timeStr.endsWith("h")) {
            return Long.parseLong(timeStr.substring(0, timeStr.length() - 1)) * 3600000L;
        } else if (timeStr.endsWith("m")) {
            return Long.parseLong(timeStr.substring(0, timeStr.length() - 1)) * 60000L;
        } else if (timeStr.endsWith("s")) {
            return Long.parseLong(timeStr.substring(0, timeStr.length() - 1)) * 1000L;
        }
        return 0L;
    }

    private void grantRole(UUID uuid, String role) {
        // Потокобезопасное добавление роли через LuckPerms API
        luckPerms.getUserManager().modifyUser(uuid, user -> {
            Node node = Node.builder("group." + role).build();
            if (!user.getNodes().contains(node)) {
                user.data().add(node);
                getLogger().info("Granted role " + role + " to player " + uuid + " on join.");
            }
        });
    }

    public String formatPlaytime(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        return String.format("%dh %dm %ds", hours, minutes, seconds);
    }
}
