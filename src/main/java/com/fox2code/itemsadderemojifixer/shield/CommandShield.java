package com.fox2code.itemsadderemojifixer.shield;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.LinkedHashSet;

public abstract class CommandShield {
    static final LinkedHashSet<String> dangerousVanillaCommands = new LinkedHashSet<>(Arrays.asList(
            "me", "msg", "say", "teammsg", "tell"
    ));
    private final CommandShield controller;
    private boolean enabled = false;

    CommandShield(CommandShield controller) {
        this.controller = controller;
    }

    public final boolean isEnabled() {
        if (this.controller != null)
            return this.controller.isEnabled();
        return this.enabled;
    }

    public final void setEnabled(boolean enabled) {
        if (this.controller != null)
            this.controller.setEnabled(enabled);
        else this.enabled = enabled;
    }

    public void register(JavaPlugin javaPlugin) {
        if (this.controller != null) {
            this.controller.register(javaPlugin);
        }
    }

    public static CommandShield make(boolean onPurpurMC) {
        if (onPurpurMC) {
            return CommandShieldPurpur.makePurpur();
        }
        return new CommandShieldBukkit();
    }

    public void sendCommandBlockedMessage(Player player) {
        player.sendMessage(ChatColor.GOLD + "[ItemsAdderEmojiFixer]" +
                ChatColor.RESET + " Command blocked for safety reasons");
    }
}
