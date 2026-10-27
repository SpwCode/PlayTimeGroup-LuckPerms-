package com.example.playtimeroles;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PlaytimeCommand implements CommandExecutor {

    private final PlaytimeRolesPlugin plugin;

    public PlaytimeCommand(PlaytimeRolesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Если команду вводит консоль
        if (!(sender instanceof Player)) {
            if (args.length == 0) {
                sender.sendMessage("§cПожалуйста, укажите имя игрока! Использование: /playtime <игрок>");
                return true;
            }

            // Консоль может смотреть любого игрока
            checkAndSendPlaytime(sender, args[0]);
            return true;
        }

        Player player = (Player) sender;

        // Если игрок проверяет чужое время
        if (args.length > 0) {
            if (!player.hasPermission("playtimeroles.admin")) {
                player.sendMessage("§cУ вас нет прав для просмотра времени других игроков!");
                return true;
            }
            checkAndSendPlaytime(player, args[0]);
        } else {
            // Если игрок проверяет своё собственное время
            long totalPlaytime = plugin.getTotalPlaytime(player);
            player.sendMessage("§aВаше время в игре: §e" + plugin.formatPlaytime(totalPlaytime));
        }

        return true;
    }

    /**
     * Вспомогательный метод для проверки времени другого игрока (работает и с онлайн, и с оффлайн игроками)
     */
    @SuppressWarnings("deprecation")
    private void checkAndSendPlaytime(CommandSender sender, String targetName) {
        // Сначала ищем среди тех, кто в сети
        Player targetOnline = Bukkit.getPlayer(targetName);

        if (targetOnline != null) {
            long totalPlaytime = plugin.getTotalPlaytime(targetOnline);
            sender.sendMessage("§aВремя в игре игрока §e" + targetOnline.getName() + "§a: §e" + plugin.formatPlaytime(totalPlaytime));
        } else {
            // Если игрок оффлайн, Spigot всё равно позволяет прочитать его файлы статистики
            OfflinePlayer targetOffline = Bukkit.getOfflinePlayer(targetName);

            if (targetOffline.hasPlayedBefore()) {
                long ticks = targetOffline.getStatistic(Statistic.PLAY_ONE_MINUTE);
                long totalPlaytime = ticks * 50L; // Переводим тики оффлайн игрока в миллисекунды

                sender.sendMessage("§aВремя в игре игрока §e" + targetOffline.getName() + " §7(Оффлайн)§a: §e" + plugin.formatPlaytime(totalPlaytime));
            } else {
                sender.sendMessage("§cИгрок не найден или никогда не играл на этом сервере!");
            }
        }
    }
}
