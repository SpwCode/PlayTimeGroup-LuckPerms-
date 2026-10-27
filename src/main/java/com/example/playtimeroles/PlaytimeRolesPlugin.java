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
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class PlaytimeRolesPlugin extends JavaPlugin implements Listener {

    private LuckPerms luckPerms;

    @Override
    public void onEnable() {
        // Регистрируем только вход игрока (выход больше не нужен)
        getServer().getPluginManager().registerEvents(this, this);

        // Подключаем LuckPerms API
        try {
            luckPerms = LuckPermsProvider.get();
        } catch (IllegalStateException e) {
            getLogger().severe("LuckPerms not found! Plugin disabled.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Загружаем конфиг
        saveDefaultConfig();

        // Регистрируем команду
        getCommand("playtime").setExecutor(new PlaytimeCommand(this));

        // Проверка ролей каждые 5 минут
        new BukkitRunnable() {
            @Override
            public void run() {
                checkAndGrantRoles();
            }
        }.runTaskTimer(this, 0L, 6000L); // 6000 тиков = 5 минут

        getLogger().info("PlaytimeRolesPlugin enabled using server statistics!");
    }

    @Override
    public void onDisable() {
        // Больше ничего сохранять не нужно, сервер сам пишет статистику в файлы игроков
        getLogger().info("PlaytimeRolesPlugin disabled!");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Проверяем роль сразу при входе на основе его старой статистики
        checkAndGrantRolesForPlayer(event.getPlayer());
    }

    /**
     * Получает общее время игры онлайн-игрока напрямую из статистики сервера.
     * Возвращает значение в МИЛЛИСЕКУНДАХ для совместимости с вашим старым кодом парсинга.
     */
    public long getTotalPlaytime(Player player) {
        if (player == null) return 0L;

        // В зависимости от версии Minecraft статистика называется по-разному:
        // 1.13+ использует PLAY_ONE_MINUTE (хотя на самом деле измеряет в тиках)
        // Если у вас древняя версия сервера (1.12.2 и ниже), замените на Statistic.PLAY_ONE_TICK
        long ticks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);

        // 1 секунда = 20 тиков. Переводим тики в миллисекунды.
        return (ticks * 50L);
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
        long totalPlaytime = getTotalPlaytime(player);

        getLogger().info("Player " + player.getName() + " total server time: " + formatPlaytime(totalPlaytime));

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
        User user = luckPerms.getUserManager().getUser(uuid);
        if (user != null) {
            Node node = Node.builder("group." + role).build();
            if (!user.getNodes().contains(node)) {
                user.data().add(node);
                // Сохраняем асинхронно, чтобы не фризить главный поток сервера
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
