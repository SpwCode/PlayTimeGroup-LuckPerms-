package com.example.playtimeroles;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PlaytimeCommand implements CommandExecutor {

    private final PlaytimeRolesPlugin plugin;

    public PlaytimeCommand(PlaytimeRolesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command is only for players!");
            return true;
        }

        Player player = (Player) sender;
        UUID uuid;

        if (args.length > 0) {
            // Check permission to view someone else's time
            if (!player.hasPermission("playtimeroles.admin")) {
                player.sendMessage("You don't have permission to view other players' time!");
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                player.sendMessage("Player not found!");
                return true;
            }

            uuid = target.getUniqueId();
            player.sendMessage("Play time " + target.getName() + ": " + plugin.formatPlaytime(plugin.getTotalPlaytime(uuid)));
            
        } else {
            uuid = player.getUniqueId();
            player.sendMessage("Your play time: " + plugin.formatPlaytime(plugin.getTotalPlaytime(uuid)));
        }

        return true;
    }
}
