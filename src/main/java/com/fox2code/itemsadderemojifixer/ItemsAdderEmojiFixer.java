package com.fox2code.itemsadderemojifixer;

import dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;

public final class ItemsAdderEmojiFixer extends JavaPlugin implements Listener {
    private static final boolean DEBUG = false;
    private static ItemsAdderEmojiFixer instance;
    private final HashMap<String, String> emojiMap = new HashMap<>(256);
    private PlaceholderExpansion emojiPlaceholderInterface = null;
    private Runnable disableTask = null;
    private Permission iaImageUseAll;
    private File itemsAdderData;
    private boolean needEmojiReload;
    private final Function<String, String> emojiResolver = emojiId -> {
        PlaceholderExpansion emojiPlaceholderInterface = this.emojiPlaceholderInterface;
        if (emojiPlaceholderInterface == null) {
            this.emojiPlaceholderInterface = emojiPlaceholderInterface =
                    PlaceholderAPIPlugin.getInstance().getLocalExpansionManager().getExpansion("img");
            if (emojiPlaceholderInterface == null) return null;
        }
        String placeholderText = emojiPlaceholderInterface.onPlaceholderRequest(null, emojiId);
        return placeholderText == null || placeholderText.startsWith(emojiId) ? null : placeholderText;
    };

    public static ItemsAdderEmojiFixer getInstance() {
        return ItemsAdderEmojiFixer.instance;
    }

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        instance = this;
        this.itemsAdderData = new File(this.getDataFolder().getParentFile(),
                "ItemsAdder" + File.separator + "data" + File.separator + "items_packs");
        if ((this.iaImageUseAll = this.getServer().getPluginManager()
                .getPermission("ia.user.image.use.*")) == null) {
            this.getServer().getPluginManager().addPermission(
                    this.iaImageUseAll = new Permission("ia.user.image.use.*"));
        }
        this.iaImageUseAll.getChildren()
                .put("ia.user.image.use.heart", true);
        this.iaImageUseAll.recalculatePermissibles();
        this.needEmojiReload = true;
        this.getServer().getPluginManager().registerEvents(this, this);
        this.resetEmojis();
        this.postInit();
        this.getLogger().info("Do not report chat emojis issues to ItemAdder creator if you use this plugin!");
        this.getLogger().info("Please test if the issue exists without this plugin before reporting.");
    }

    public void postInit() {
        if (this.disableTask != null || !this.isEnabled()) return;
        final PluginCommand pluginCommand = this.getServer().getPluginCommand("iaimage");
        final TabCompleter tabCompleter;
        if (pluginCommand == null || (tabCompleter = pluginCommand.getTabCompleter()) == null) {
            this.getServer().getScheduler().runTaskLater(this, this::postInit, 10L);
            return;
        }
        pluginCommand.setTabCompleter((sender, command, alias, args) -> {
            if (!sender.hasPermission("ia.user.image.hints")) return Collections.emptyList();
            if (args[args.length - 1].startsWith(":")) {
                String sub = args[args.length - 1].substring(1);
                ArrayList<String> completion = new ArrayList<>();
                if (sender.hasPermission("ia.user.image.use.*")) {
                    for (String emojiId : this.emojiMap.keySet()) {
                        if (emojiId.startsWith(sub)) {
                            completion.add(":" + emojiId + ":");
                        }
                    }
                } else {
                    for (String emojiId : this.emojiMap.keySet()) {
                        if (sender.hasPermission("ia.user.image.use." + emojiId)
                                && emojiId.startsWith(sub)) {
                            completion.add(":" + emojiId + ":");
                        }
                    }
                }
                return completion;
            }
            return tabCompleter.onTabComplete(sender.hasPermission("ia.user.image.use.*") ?
                    Bukkit.getConsoleSender() : sender, command, alias, args);
        });
        this.disableTask = () -> pluginCommand.setTabCompleter(tabCompleter);
        if (this.needEmojiReload) this.reloadEmojis();
    }

    @Override
    public void onDisable() {
        instance = null;
        this.emojiMap.clear();
        this.emojiPlaceholderInterface = null;
        this.needEmojiReload = false;
        Runnable disableTask = this.disableTask;
        this.disableTask = null;
        if (disableTask != null) disableTask.run();
    }

    // Note: Either "onPlayerChatMonitor" or "onPlayerChatLowest" is needed depending on chat plugin
    // But I keep both because it's really tricky to make proper detection code for each chat plugin

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerChatMonitor(AsyncPlayerChatEvent playerChatEvent) {
        Player player = playerChatEvent.getPlayer();
        if (!player.hasPermission("ia.user.image.chat")) return;
        if (player.hasPermission("ia.user.image.use.*")) player = null;
        String oldMessage = playerChatEvent.getMessage();
        String newMessage = this.insertEmojis(oldMessage, player);
        if (DEBUG) {
            this.getLogger().info("Monitor: \"" + oldMessage + "\" -> \"" + newMessage + "\"");
        }
        playerChatEvent.setMessage(newMessage);
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerChatLowest(AsyncPlayerChatEvent playerChatEvent) {
        Player player = playerChatEvent.getPlayer();
        if (!player.hasPermission("ia.user.image.chat")) return;
        if (player.hasPermission("ia.user.image.use.*")) player = null;
        String oldMessage = playerChatEvent.getMessage();
        String newMessage = this.insertEmojis(oldMessage, player);
        if (DEBUG) {
            this.getLogger().info("Lowest: \"" + oldMessage + "\" -> \"" + newMessage + "\"");
        }
        playerChatEvent.setMessage(newMessage);
    }

    @EventHandler
    public void onItemsAdderReload(ItemsAdderLoadDataEvent event) {
        this.reloadEmojis();
    }

    private void resetEmojis() {
        this.emojiMap.clear();
        this.emojiPlaceholderInterface = PlaceholderAPIPlugin.getInstance()
                .getLocalExpansionManager().getExpansion("img");
    }

    private void reloadEmojis() {
        this.needEmojiReload = false;
        if (!this.itemsAdderData.exists()) {
            this.getLogger().warning("Failed to locate ItemsAdder addon data.");
            this.resetEmojis();
            return;
        }
        this.getLogger().info("Reloading emojis...");
        this.resetEmojis();
        // ItemsAdder doesn't contain any APIs to list all emojis
        for (File addon : Objects.requireNonNull(this.itemsAdderData.listFiles())) {
            File emojis = new File(addon, "emoji_images.yml");
            if (emojis.exists()) {
                YamlConfiguration yamlConfiguration = new YamlConfiguration();
                try {
                    yamlConfiguration.load(emojis);
                } catch (Exception e) {
                    this.getLogger().log(Level.WARNING,
                            "Failed to load " + addon.getName() + " emojis", e);
                }
                ConfigurationSection configurationSection =
                        yamlConfiguration.getConfigurationSection("font_images");
                if (configurationSection != null) {
                    for (String emojiId : configurationSection.getKeys(false)) {
                        if (this.getEmoji(emojiId) != null) {
                            this.iaImageUseAll.getChildren().put(
                                    "ia.user.image.use." + emojiId, true);
                        }
                    }
                }
            }
        }
        this.iaImageUseAll.recalculatePermissibles();
        this.getLogger().info("Loaded " + this.emojiMap.size() + " emojis!");
    }

    public String insertEmojis(String text) {
        return this.insertEmojis(text, null);
    }

    public String insertEmojis(String text, Player player) {
        StringBuilder stringBuilder = new StringBuilder();
        int i = text.indexOf(':');
        int i2;
        int index = 0;
        while (i != -1) {
            i2 = text.indexOf(':', i + 1);
            if (i2 == -1) break;
            String emojiId = text.substring(i + 1, i2);
            String emojiReplacement;
            if ((player == null || player.hasPermission("ia.user.image.use." + emojiId))
                    && (emojiReplacement = this.getEmoji(emojiId)) != null) {
                stringBuilder.append(text, index, i).append(emojiReplacement);
                i2++; index = i2;
            }
            i = text.indexOf(':', i2);
        }
        return index == 0 ? text : stringBuilder.append(
                text, index, text.length()).toString();
    }

    public String getEmoji(String emojiId) {
        return this.emojiMap.computeIfAbsent(emojiId, this.emojiResolver);
    }

    public Set<String> getEmojiIds() {
        return Collections.unmodifiableSet(this.emojiMap.keySet());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0 || args[0].isEmpty() || args[0].equals(":")) {
            sender.sendMessage(ChatColor.GOLD + "Please define an emoji to print.");
            return true;
        }
        System.out.println(args[0]);
        if (args[0].startsWith(":") && args[0].endsWith(":")) {
            args[0] = args[0].substring(1, args[0].length() - 1);
            System.out.println(args[0]);
        }
        String emoji = this.getEmoji(args[0]);
        if (emoji == null || emoji.isEmpty()) {
            sender.sendMessage(ChatColor.GOLD + "Emoji not found!");
        } else {
            boolean permission = sender.hasPermission("ia.user.image.use.*") ||
                    sender.hasPermission("ia.user.image.use." + args[0]);
            sender.sendMessage(ChatColor.GOLD + "Emoji: " + ChatColor.WHITE + emoji +
                    (permission ? "" : ChatColor.RED + " (Restricted)"));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length != 1 || this.emojiMap.isEmpty() ||
                !sender.hasPermission("ia.user.image.hints")) return Collections.emptyList();
        final String arg0 = args[0];
        ArrayList<String> completion = new ArrayList<>();
        if (sender.hasPermission("ia.user.image.use.*")) {
            for (String emojiId : this.emojiMap.keySet()) {
                if (emojiId.startsWith(arg0)) {
                    completion.add(emojiId);
                }
            }
        } else {
            for (String emojiId : this.emojiMap.keySet()) {
                if (sender.hasPermission("ia.user.image.use." + emojiId)
                        && emojiId.startsWith(arg0)) {
                    completion.add(emojiId);
                }
            }
        }
        return completion;
    }
}
