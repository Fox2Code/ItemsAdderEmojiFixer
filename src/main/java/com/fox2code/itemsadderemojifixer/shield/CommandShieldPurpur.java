package com.fox2code.itemsadderemojifixer.shield;

import com.fox2code.itemsadderemojifixer.ItemsAdderEmojiFixer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.purpurmc.purpur.event.ExecuteCommandEvent;

public final class CommandShieldPurpur extends CommandShield implements Listener {
    private boolean message;

    static CommandShield makePurpur() {
        return new CommandShieldPurpur(new CommandShieldBukkit());
    }

    private CommandShieldPurpur(CommandShieldBukkit commandShieldBukkit) {
        super(commandShieldBukkit);
    }

    @Override
    public void register(JavaPlugin javaPlugin) {
        super.register(javaPlugin);
        javaPlugin.getServer().getPluginManager().registerEvents(this, javaPlugin);
    }

    @EventHandler
    public void onCommand(ExecuteCommandEvent executeCommandEvent) {
        if (!isEnabled()) return;
        Command command = executeCommandEvent.getCommand();
        if (!(command instanceof PluginCommand) &&
                dangerousVanillaCommands.contains(command.getName())) {
            CommandSender commandSender = executeCommandEvent.getSender();
            if (commandSender instanceof Player &&
                    command.testPermissionSilent(commandSender)) {
                // The CommandShieldBukkit should prevent us from reaching here.
                ItemsAdderEmojiFixer.getInstance().getLogger().warning("The player " +
                        commandSender.getName() + " found a way around CommandShieldBukkit " +
                        "but CommandShieldPurpur caught it.");
                executeCommandEvent.setCancelled(true);
                sendCommandBlockedMessage((Player) commandSender);
            }
        }
    }
}
