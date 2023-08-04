package com.fox2code.itemsadderemojifixer;

import com.fox2code.itemsadderemojifixer.intmap.ItemsAdderEmojiFixerIntMap;
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

public final class ItemsAdderEmojiFixer extends JavaPlugin implements Listener, Iterable<Map.Entry<String, String>> {
    private static final boolean DEBUG = false;
    private static ItemsAdderEmojiFixer instance;
    private final HashMap<String, String> emojiMap = new HashMap<>(256);
    private final ItemsAdderEmojiFixerIntMap invertedMap =
            ItemsAdderEmojiFixerIntMap.newIntMap();
    private PlaceholderExpansion emojiPlaceholderInterface = null;
    private Runnable disableTask = null;
    private Permission iaImageUseAll;
    private boolean needEmojiReload;
    private int emojiExtCodePoint;
    private String emojiExtCodePointStr = "";
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
    // Compatibility
    private boolean discordSrvCompat;

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
        if ((this.iaImageUseAll = this.getServer().getPluginManager()
                .getPermission("ia.user.image.use.*")) == null) {
            this.getServer().getPluginManager().addPermission(
                    this.iaImageUseAll = new Permission("ia.user.image.use.*"));
        }
        this.iaImageUseAll.getChildren()
                .put("ia.user.image.use.heart", true);
        this.iaImageUseAll.recalculatePermissibles();
        this.needEmojiReload = true;
        this.discordSrvCompat = this.getServer()
                .getPluginManager().isPluginEnabled("DiscordSRV");
        if (this.discordSrvCompat) {
            this.getLogger().info("Using DiscordSRV compat");
            try {
                DiscordSRVCompat.enable();
            } catch (Throwable t) {
                this.getLogger().log(Level.WARNING,
                        "Unable to initialize DiscordSRV compat", t);
                this.discordSrvCompat = false;
            }
        }
        this.getServer().getPluginManager().registerEvents(this, this);
        this.resetEmojis();
        this.postInit();
        this.getLogger().info("Do not report chat emojis issues to ItemAdder creator if you use this plugin!");
        this.getLogger().info("Please test if the issue exists without this plugin before reporting.");
        this.getLogger().info("Emojis to emoji-id map impl: " + this.invertedMap.getProvider());
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
        if (this.discordSrvCompat) {
            this.discordSrvCompat = false;
            try {
                DiscordSRVCompat.disable();
            } catch (Throwable ignored) {}
        }
        this.emojiMap.clear();
        this.invertedMap.clear();
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
        playerChatEvent.setMessage(newMessage + this.emojiExtCodePointStr);
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
        this.invertedMap.clear();
        this.emojiPlaceholderInterface = PlaceholderAPIPlugin.getInstance()
                .getLocalExpansionManager().getExpansion("img");
    }

    private void reloadEmojis() {
        this.needEmojiReload = false;
        File itemsAdderData = new File(this.getDataFolder().getParentFile(),
                "ItemsAdder" + File.separator + "contents");
        boolean oldPath = false;
        if (!itemsAdderData.exists()) {
            itemsAdderData = new File(this.getDataFolder().getParentFile(),
                    "ItemsAdder" + File.separator + "data" + File.separator + "items_packs");
            oldPath = true;
        }
        if (!itemsAdderData.exists()) {
            this.getLogger().warning("Failed to locate ItemsAdder addon data.");
            this.resetEmojis();
            return;
        }
        this.getLogger().info("Reloading emojis...");
        this.resetEmojis();
        // ItemsAdder doesn't contain any APIs to list all emojis
        int codePointExt = 0;
        for (File addon : Objects.requireNonNull(itemsAdderData.listFiles())) {
            if (!addon.isDirectory()) continue;
            File emojis = oldPath ? new File(addon, "emoji_images.yml") :
                    new File(addon, "configs" + File.separator + "emoji_images.yml");
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
                        String emoji;
                        if ((emoji = this.getEmoji(emojiId)) != null) {
                            this.iaImageUseAll.getChildren().put(
                                    "ia.user.image.use." + emojiId, true);
                            int codePointCount = emoji.codePointCount(0, emoji.length());

                            if (codePointCount == 1) {
                                this.invertedMap.putIfAbsent(emoji.codePointAt(0), emojiId);
                            } else if (codePointCount == 2) {
                                int codePoint1 = emoji.codePointAt(0);
                                int codePoint2 = emoji.codePointBefore(emoji.length());
                                if (codePointExt == 0) {
                                    codePointExt = codePoint2;
                                } else if (codePoint2 != codePointExt) {
                                    this.getLogger().warning("Emoji " + emojiId +
                                            " has an unusual formatting. -> " +
                                            Arrays.toString(emoji.codePoints().toArray()));
                                    continue;
                                }
                                this.invertedMap.putIfAbsent(codePoint1, emojiId);
                            } else {
                                this.getLogger().warning("Emoji " + emojiId +
                                        " has an unusual formatting. -> " +
                                        Arrays.toString(emoji.codePoints().toArray()));
                            }
                        }
                    }
                }
            }
        }
        this.emojiExtCodePoint = codePointExt;
        this.emojiExtCodePointStr = new StringBuilder()
                .appendCodePoint(codePointExt).toString();
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

    public String unmapEmojis(String text) {
        return this.unmapEmojisEx(text, true, null);
    }

    public String unmapEmojisEx(String text, boolean clean, Function<String, String> emojiIdRemap) {
        final StringBuilder stringBuilder = new StringBuilder(text.length() + 32);
        final boolean[] tracker = clean ? null : new boolean[]{false};
        text.codePoints().sequential().forEach(i -> {
            if (tracker == null) {
                if (i == this.emojiExtCodePoint)
                    return;
            } else if (tracker[0]) {
                if (i == this.emojiExtCodePoint)
                    return;
                tracker[0] = false;
            }
            String emojiId = this.invertedMap.get(i);
            if (emojiIdRemap != null) {
                emojiId = emojiIdRemap.apply(emojiId);
            }
            if (emojiId == null) {
                stringBuilder.appendCodePoint(i);
            } else {
                stringBuilder.append(':')
                        .append(emojiId)
                        .append(':');
                if (tracker != null) {
                    tracker[0] = true;
                }
            }
        });
        if (stringBuilder.length() == text.length()) {
            return text;
        }
        return stringBuilder.toString();
    }

    public String getEmoji(String emojiId) {
        return this.emojiMap.computeIfAbsent(emojiId, this.emojiResolver);
    }

    public Set<String> getEmojiIds() {
        return Collections.unmodifiableSet(this.emojiMap.keySet());
    }

    public boolean hasEmoji(String emojiId) {
        return this.emojiMap.containsKey(emojiId);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if ("print_all_emojis".equals(command.getName())) {
            StringBuilder stringBuilder = new StringBuilder(100);
            stringBuilder.append(ChatColor.GOLD).append("All emojis (")
                    .append(ChatColor.GREEN).append(this.emojiMap.size())
                    .append(ChatColor.GOLD).append("): ").append(ChatColor.RESET);
            for (String emoji : this.emojiMap.values()) {
                stringBuilder.append(emoji);
            }
            sender.sendMessage(stringBuilder.toString());
            return true;
        }
        boolean wantIds = "print_emoji_id".equals(command.getName());
        if (args.length == 0 || args[0].isEmpty() || args[0].equals(":")) {
            sender.sendMessage(ChatColor.GOLD + "Please define an emoji to print.");
            return true;
        }
        if (args[0].startsWith(":") && args[0].endsWith(":")) {
            args[0] = args[0].substring(1, args[0].length() - 1);
        }
        if (wantIds) {
            StringJoiner stringJoiner = new StringJoiner(" ");
            for (String arg : args) {
                stringJoiner.add(arg);
            }
            String arg = stringJoiner.toString();
            String emojiId = this.unmapEmojisEx(arg, false, null);
            if (emojiId.isEmpty() || emojiId.length() == arg.length()) {
                sender.sendMessage(ChatColor.GOLD + "No emojis found!");
            } else {
                sender.sendMessage(ChatColor.GOLD + "Emoji-ID: " + ChatColor.WHITE + emojiId);
            }
        } else {
            String emoji = this.getEmoji(args[0]);
            if (emoji == null || emoji.isEmpty()) {
                sender.sendMessage(ChatColor.GOLD + "Emoji not found!");
            } else {
                boolean permission = sender.hasPermission("ia.user.image.use.*") ||
                        sender.hasPermission("ia.user.image.use." + args[0]);
                sender.sendMessage(ChatColor.GOLD + "Emoji: " + ChatColor.WHITE + emoji +
                        (permission ? "" : ChatColor.RED + " (Restricted)"));
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if ("print_all_emojis".equals(command.getName())) {
            return Collections.emptyList();
        }
        boolean wantIds = "print_emoji_id".equals(command.getName());
        if (args.length != 1 || this.emojiMap.isEmpty() ||
                !sender.hasPermission("ia.user.image.hints")) return Collections.emptyList();
        final String arg0 = args[0];
        ArrayList<String> completion = new ArrayList<>();
        if (sender.hasPermission("ia.user.image.use.*")) {
            if (wantIds) {
                for (String emoji : this.emojiMap.values()) {
                    if (emoji.startsWith(arg0)) {
                        completion.add(emoji);
                    }
                }
            } else {
                for (String emojiId : this.emojiMap.keySet()) {
                    if (emojiId.startsWith(arg0)) {
                        completion.add(emojiId);
                    }
                }
            }
        } else {
            if (wantIds) {
                for (Map.Entry<String, String> emoji : this.emojiMap.entrySet()) {
                    if (sender.hasPermission("ia.user.image.use." + emoji.getKey())
                            && emoji.getValue().startsWith(arg0)) {
                        completion.add(emoji.getValue());
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
        }
        return completion;
    }

    @NotNull
    @Override
    public Iterator<Map.Entry<String, String>> iterator() {
        return Collections.unmodifiableSet(this.emojiMap.entrySet()).iterator();
    }
}
