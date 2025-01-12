package com.example.gcmcPlugin;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Main extends JavaPlugin implements Listener {

    private FileConfiguration donorsConfig;
    private File donorsFile;
    private FileConfiguration nicknamesConfig;
    private File nicknamesFile;

    private final Map<UUID, BukkitRunnable> teleportTasks = new HashMap<>();
    private final Map<UUID, UUID> teleportRequests = new HashMap<>();
    private Scoreboard scoreboard;

    @Override
    public void onEnable() {
        getLogger().info("gcmcPlugin has been enabled!");
        createDonorsFile();
        createNicknamesFile();
        scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        getLogger().info("gcmcPlugin has been disabled!");
    }

    private void createDonorsFile() {
        donorsFile = new File(getDataFolder(), "donors.yml");
        if (!donorsFile.exists()) {
            donorsFile.getParentFile().mkdirs();
            try {
                donorsFile.createNewFile();
            } catch (IOException e) {
                getLogger().severe("Could not create donors.yml!");
                e.printStackTrace();
            }
        }
        donorsConfig = YamlConfiguration.loadConfiguration(donorsFile);
    }

    private void createNicknamesFile() {
        nicknamesFile = new File(getDataFolder(), "nicknames.yml");
        if (!nicknamesFile.exists()) {
            nicknamesFile.getParentFile().mkdirs();
            try {
                nicknamesFile.createNewFile();
            } catch (IOException e) {
                getLogger().severe("Could not create nicknames.yml!");
                e.printStackTrace();
            }
        }
        nicknamesConfig = YamlConfiguration.loadConfiguration(nicknamesFile);
    }

    private void saveDonors() {
        try {
            donorsConfig.save(donorsFile);
        } catch (IOException e) {
            getLogger().severe("Could not save donors.yml!");
            e.printStackTrace();
        }
    }

    private void saveNicknames() {
        try {
            nicknamesConfig.save(nicknamesFile);
        } catch (IOException e) {
            getLogger().severe("Could not save nicknames.yml!");
            e.printStackTrace();
        }
    }

    private void createOrUpdateTeam(String teamName, ChatColor prefixColor, String prefix) {
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }
        team.setPrefix(prefixColor + prefix + ChatColor.RESET + " ");
    }

    private void removePlayerFromAllTeams(Player player) {
        for (Team team : scoreboard.getTeams()) {
            if (team.hasEntry(player.getUniqueId().toString())) {
                team.removeEntry(player.getUniqueId().toString());
            }
        }
    }

    private void updatePlayerTeam(Player player) {
        UUID playerId = player.getUniqueId();
        int level = donorsConfig.getInt("donors." + playerId + ".level", -1);
        removePlayerFromAllTeams(player);

        if (level > 0) {
            String teamName = "donor_" + level;
            String prefix;
            ChatColor prefixColor;

            switch (level) {
                case 5:
                    prefix = "[Netherite]";
                    prefixColor = ChatColor.DARK_RED;
                    break;
                case 4:
                    prefix = "[Diamond]";
                    prefixColor = ChatColor.AQUA;
                    break;
                case 3:
                    prefix = "[Gold]";
                    prefixColor = ChatColor.GOLD;
                    break;
                case 2:
                    prefix = "[Silver]";
                    prefixColor = ChatColor.GRAY;
                    break;
                case 1:
                default:
                    prefix = "[Bronze]";
                    prefixColor = ChatColor.DARK_GRAY;
                    break;
            }

            createOrUpdateTeam(teamName, prefixColor, prefix);
            Team team = scoreboard.getTeam(teamName);
            if (team != null) {
                team.addEntry(player.getUniqueId().toString());
            }
        }
    }

    private void updateDisplayName(Player player) {
        UUID playerId = player.getUniqueId();
        int level = donorsConfig.getInt("donors." + playerId + ".level", -1);
        String nickname = nicknamesConfig.getString(playerId.toString());
        String nameToUse = nickname != null ? nickname : player.getName();

        ChatColor color = ChatColor.WHITE;
        String tag = "";

        switch (level) {
            case 5:
                color = ChatColor.DARK_RED;
                tag = "[Netherite]";
                break;
            case 4:
                color = ChatColor.AQUA;
                tag = "[Diamond]";
                break;
            case 3:
                color = ChatColor.GOLD;
                tag = "[Gold]";
                break;
            case 2:
                color = ChatColor.GRAY;
                tag = "[Silver]";
                break;
            case 1:
                color = ChatColor.DARK_GRAY;
                tag = "[Bronze]";
                break;
        }

        String displayName = (tag.isEmpty() ? "" : tag + " ") + color + nameToUse;

        player.setDisplayName(displayName);
        player.setPlayerListName(displayName);
        player.setCustomName(displayName);
        player.setCustomNameVisible(true);
        updatePlayerTeam(player);
    }

    private void resetPlayer(Player player) {
        player.setDisplayName(player.getName());
        player.setPlayerListName(player.getName());
        player.setCustomName(player.getName());
        player.setCustomNameVisible(false);
        removePlayerFromAllTeams(player);
    }

    private void validatePlayerState(Player player) {
        UUID playerId = player.getUniqueId();

        if (!donorsConfig.contains("donors." + playerId)) {
            resetPlayer(player);
        } else {
            updateDisplayName(player);
        }

        if (!nicknamesConfig.contains(playerId.toString())) {
            resetPlayer(player);
        }
    }

    private void sendTpaRequest(Player requester, Player target) {
        if (teleportRequests.containsKey(requester.getUniqueId())) {
            requester.sendMessage(ChatColor.RED + "You already have a pending teleport request.");
            return;
        }

        teleportRequests.put(requester.getUniqueId(), target.getUniqueId());
        requester.sendMessage(ChatColor.GREEN + "Teleportation request sent to " + target.getName() + ".");

        TextComponent accept = new TextComponent(ChatColor.GREEN + "[ACCEPT]");
        accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpaccept"));

        TextComponent deny = new TextComponent(ChatColor.RED + "[DENY]");
        deny.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpdeny"));

        target.spigot().sendMessage(new ComponentBuilder()
                .append(ChatColor.YELLOW + requester.getName() + " has requested to teleport to you. ")
                .append(accept)
                .append(" ")
                .append(deny)
                .create());

        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (teleportRequests.containsKey(requester.getUniqueId())) {
                teleportRequests.remove(requester.getUniqueId());
                requester.sendMessage(ChatColor.RED + "Teleport request to " + target.getName() + " expired.");
                target.sendMessage(ChatColor.RED + "Teleport request from " + requester.getName() + " expired.");
            }
        }, 1200);
    }

    private void startTeleportCountdown(Player requester, Location targetLocation) {
        BukkitRunnable task = new BukkitRunnable() {
            int countdown = 5;

            @Override
            public void run() {
                if (countdown > 0) {
                    requester.sendTitle(ChatColor.YELLOW + "Teleporting in " + countdown, "", 0, 20, 0);
                    countdown--;
                } else {
                    requester.teleport(targetLocation);
                    requester.sendTitle(ChatColor.GREEN + "Teleported!", "", 10, 70, 20);
                    teleportTasks.remove(requester.getUniqueId());
                    cancel();
                }
            }
        };

        teleportTasks.put(requester.getUniqueId(), task);
        task.runTaskTimer(this, 0, 20);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        validatePlayerState(player);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (teleportTasks.containsKey(player.getUniqueId())) {
            if (event.getFrom().getX() != event.getTo().getX()
                    || event.getFrom().getY() != event.getTo().getY()
                    || event.getFrom().getZ() != event.getTo().getZ()) {

                teleportTasks.get(player.getUniqueId()).cancel();
                teleportTasks.remove(player.getUniqueId());
                player.sendMessage(ChatColor.RED + "Teleportation canceled because you moved.");
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("add_to_donators")) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Usage: /add_to_donators <player> <level>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                player.sendMessage(ChatColor.RED + "Player not found.");
                return true;
            }

            int level;
            try {
                level = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Level must be a number between 1 and 5.");
                return true;
            }

            donorsConfig.set("donors." + target.getUniqueId() + ".level", level);
            saveDonors();

            updateDisplayName(target);
            player.sendMessage(ChatColor.GREEN + "Added " + target.getName() + " to donors with level " + level + ".");
            return true;

        } else if (command.getName().equalsIgnoreCase("remove_from_donators")) {
            if (args.length < 1) {
                player.sendMessage(ChatColor.RED + "Usage: /remove_from_donators <player>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                player.sendMessage(ChatColor.RED + "Player not found.");
                return true;
            }

            donorsConfig.set("donors." + target.getUniqueId(), null);
            saveDonors();

            resetPlayer(target);
            player.sendMessage(ChatColor.GREEN + "Removed " + target.getName() + " from donors.");
            return true;

        } else if (command.getName().equalsIgnoreCase("setnick")) {
            if (args.length < 1) {
                player.sendMessage(ChatColor.RED + "Usage: /setnick <nickname>");
                return true;
            }

            String nickname = args[0];
            if (nickname.length() > 16) {
                player.sendMessage(ChatColor.RED + "Nickname is too long! Maximum 16 characters.");
                return true;
            }

            nicknamesConfig.set(player.getUniqueId().toString(), nickname);
            saveNicknames();

            updateDisplayName(player);
            player.sendMessage(ChatColor.GREEN + "Your nickname has been set to " + nickname + ".");
            return true;

        } else if (command.getName().equalsIgnoreCase("removenick")) {
            if (!nicknamesConfig.contains(player.getUniqueId().toString())) {
                player.sendMessage(ChatColor.RED + "You don't have a nickname set.");
                return true;
            }

            nicknamesConfig.set(player.getUniqueId().toString(), null);
            saveNicknames();

            resetPlayer(player);
            player.sendMessage(ChatColor.GREEN + "Your nickname has been removed.");
            return true;

        } else if (command.getName().equalsIgnoreCase("tpa")) {
            if (args.length < 1) {
                player.sendMessage(ChatColor.RED + "Usage: /tpa <player>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                player.sendMessage(ChatColor.RED + "Player not found.");
                return true;
            }

            sendTpaRequest(player, target);
            return true;

        } else if (command.getName().equalsIgnoreCase("tpaccept")) {
            UUID requesterId = teleportRequests.entrySet().stream()
                    .filter(entry -> entry.getValue().equals(player.getUniqueId()))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(null);

            if (requesterId == null) {
                player.sendMessage(ChatColor.RED + "You have no pending teleport requests.");
                return true;
            }

            Player requester = Bukkit.getPlayer(requesterId);
            if (requester == null) {
                player.sendMessage(ChatColor.RED + "The requester is no longer online.");
                teleportRequests.remove(requesterId);
                return true;
            }

            teleportRequests.remove(requesterId);
            startTeleportCountdown(requester, player.getLocation());
            return true;

        } else if (command.getName().equalsIgnoreCase("tpdeny")) {
            UUID requesterId = teleportRequests.entrySet().stream()
                    .filter(entry -> entry.getValue().equals(player.getUniqueId()))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(null);

            if (requesterId == null) {
                player.sendMessage(ChatColor.RED + "You have no pending teleport requests.");
                return true;
            }

            Player requester = Bukkit.getPlayer(requesterId);
            if (requester != null) {
                requester.sendMessage(ChatColor.RED + "Your teleport request to " + player.getName() + " was denied.");
            }

            teleportRequests.remove(requesterId);
            player.sendMessage(ChatColor.RED + "You denied the teleport request from " + (requester != null ? requester.getName() : "unknown player") + ".");
            return true;
        }

        return false;
    }
}