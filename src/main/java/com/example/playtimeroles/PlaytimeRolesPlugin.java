package com.example.playtimeroles;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class PlaytimeRolesPlugin extends JavaPlugin implements Listener {

    private LuckPerms luckPerms;
    private Map<UUID, Long> playerJoinTimes = new HashMap<>();
    private File playtimeFile;
    private FileConfiguration playtimeConfig;

    @Override
    public void onEnable() {
        // Register events
        getServer().getPluginManager().registerEvents(this, this);

        // Get LuckPerms API
        try {
            luckPerms = LuckPermsProvider.get();
        } catch (IllegalStateException e) {
            getLogger().severe("LuckPerms not found! Plugin disabled.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Load config
        saveDefaultConfig();

        // File for storing play time
        playtimeFile = new File(getDataFolder(), "playtime.yml");
        if (!playtimeFile.exists()) {
            try {
                playtimeFile.createNewFile();
            } catch (IOException e) {
                getLogger().severe("Failed to create playtime.yml file: " + e.getMessage());
            }
        }
        playtimeConfig = YamlConfiguration.loadConfiguration(playtimeFile);

        // Load playtime data
        loadPlaytimeData();

        // Register command
        getCommand("playtime").setExecutor(new PlaytimeCommand(this));

        // Scheduler to check roles every 5 minutes
        new BukkitRunnable() {
            @Override
            public void run() {
                checkAndGrantRoles();
            }
        }.runTaskTimer(this, 0L, 6000L); // 6000 ticks = 5 minutes

        getLogger().info("PlaytimeRolesPlugin enabled!");
    }

    @Override
    public void onDisable() {
        // Save playtime for online players before disabling
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            if (playerJoinTimes.containsKey(uuid)) {
                long joinTime = playerJoinTimes.get(uuid);
                long playtime = System.currentTimeMillis() - joinTime;
                
                long totalPlaytime = getTotalPlaytime(uuid) + playtime;
                setTotalPlaytime(uuid, totalPlaytime);
            }
        }

        // Save data
        savePlaytimeData();
        getLogger().info("PlaytimeRolesPlugin disabled!");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        playerJoinTimes.put(player.getUniqueId(), System.currentTimeMillis());

        // Check roles on join
        checkAndGrantRolesForPlayer(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (playerJoinTimes.containsKey(uuid)) {
            long joinTime = playerJoinTimes.get(uuid);
            long playtime = System.currentTimeMillis() - joinTime;

            long totalPlaytime = getTotalPlaytime(uuid) + playtime;
            setTotalPlaytime(uuid, totalPlaytime);

            playerJoinTimes.remove(uuid);
        }
    }

    public long getTotalPlaytime(UUID uuid) {
        long total = playtimeConfig.getLong(uuid.toString(), 0L);
        if (playerJoinTimes.containsKey(uuid)) {
            total += System.currentTimeMillis() - playerJoinTimes.get(uuid);
        }
        return total;
    }

    public void setTotalPlaytime(UUID uuid, long time) {
        playtimeConfig.set(uuid.toString(), time);
    }

    private void loadPlaytimeData() {
        // Data is loaded when config loads
    }

    private void savePlaytimeData() {
        try {
            playtimeConfig.save(playtimeFile);
        } catch (IOException e) {
            getLogger().severe("Failed to save playtime.yml: " + e.getMessage());
        }
    }

    private void checkAndGrantRoles() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            checkAndGrantRolesForPlayer(player);
        }
    }

    private void checkAndGrantRolesForPlayer(Player player) {
        FileConfiguration config = getConfig();
        List<Map<?, ?>> roles = config.getMapList("roles");

        UUID uuid = player.getUniqueId();
        long totalPlaytime = getTotalPlaytime(uuid);

        getLogger().info("Player " + player.getName() + " total time: " + formatPlaytime(totalPlaytime));

        for (Map<?, ?> roleMap : roles) {
            String role = (String) roleMap.get("role");
            long requiredTime = parseTime((String) roleMap.get("time"));
            getLogger().info("Checking role " + role + " required: " + formatPlaytime(requiredTime));

            if (totalPlaytime >= requiredTime) {
                grantRole(uuid, role);
            }
        }
    }

    private long parseTime(String timeStr) {
        // Parse time, e.g. "1h" -> 3600000 ms
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
        User user = luckPerms.getUserManager().getUser(uuid);
        if (user != null) {
            Node node = Node.builder("group." + role).build();
            if (!user.getNodes().contains(node)) {
                user.data().add(node);
                luckPerms.getUserManager().saveUser(user);

                getLogger().info("Granted role " + role + " to player " + uuid);
            }
        }
    }

    public String formatPlaytime(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;

        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        return String.format("%dh %dm %ds", hours, minutes, seconds);
    }
}
