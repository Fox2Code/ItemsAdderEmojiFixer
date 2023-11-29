package com.fox2code.itemsadderemojifixer.shield;

import com.fox2code.itemsadderemojifixer.ItemsAdderEmojiFixer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;

public final class CommandShieldBukkit extends CommandShield implements Listener {
    private final HashMap<String, Command> variantBlocklist = new HashMap<>(dangerousVanillaCommands.size() * 2);
    private CommandMap commandMap;

    private volatile boolean reloadLater;

    public CommandShieldBukkit() {
        super(null);
    }

    @Override
    public void register(JavaPlugin javaPlugin) {
        super.register(javaPlugin);
        javaPlugin.getServer().getPluginManager().registerEvents(this, javaPlugin);
        this.commandMap = javaPlugin.getServer().getCommandMap();
        this.reloadLater = false;
        this.reloadLater();
    }

    public void reloadLater() {
        if (this.reloadLater) return;
        this.reloadLater = true;
        Bukkit.getServer().getScheduler().runTaskLaterAsynchronously(
                ItemsAdderEmojiFixer.getInstance(), this::reload, 1L);
    }

    public void reload() {
        final HashMap<String, Command> variantBlocklist = this.variantBlocklist;
        synchronized (this.variantBlocklist) {
            variantBlocklist.clear();
            CommandMap commandMap = this.commandMap;
            for (String commandName : dangerousVanillaCommands) {
                String commandNameExt = "minecraft:" + commandName;
                Command commandExt;
                variantBlocklist.put(commandNameExt, commandExt =
                        commandMap.getCommand(commandNameExt));
                Command command = commandMap.getCommand(commandName);
                // I hope this check is good enough...
                if (commandExt == command ||
                        !(command instanceof PluginCommand)) {
                    variantBlocklist.put(commandName, command);
                }
            }
        }

        this.reloadLater = false;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommandSend(PlayerCommandPreprocessEvent playerCommandPreprocessEvent) {
        if (!isEnabled()) return;
        final String message = playerCommandPreprocessEvent.getMessage();
        final int spaceIndex = message.indexOf(' ');
        final int start = message.startsWith("/") ? 1 : 0;
        final String commandName = spaceIndex == -1 ?
                message.substring(start) :
                message.substring(start, spaceIndex);
        boolean cancel;
        Command command = null;
        synchronized (this.variantBlocklist) {
            cancel = this.variantBlocklist.containsKey(commandName);
            if (cancel) command = this.variantBlocklist.get(commandName);
        }
        if (command != null && !command.testPermissionSilent(
                playerCommandPreprocessEvent.getPlayer())) {
            // If player isn't allowed to execute command, assume it's already blocked.
            cancel = false;
        }
        if (cancel) {
            playerCommandPreprocessEvent.setCancelled(true);
            sendCommandBlockedMessage(playerCommandPreprocessEvent.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommandSend(PlayerCommandSendEvent playerCommandSendEvent) {
        if (!isEnabled()) return;
        synchronized (this.variantBlocklist) {
            playerCommandSendEvent.getCommands().removeAll(this.variantBlocklist.keySet());
        }
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent pluginDisableEvent) {
        this.reloadLater();
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent pluginEnableEvent) {
        this.reloadLater();
    }
}
