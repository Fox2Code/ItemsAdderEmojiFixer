package com.fox2code.itemsadderemojifixer;

import net.essentialsx.api.v2.events.discord.DiscordChatMessageEvent;
import net.essentialsx.api.v2.events.discord.DiscordMessageEvent;
import net.essentialsx.api.v2.events.discord.DiscordRelayEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

final class EssentialsXDiscordCompat extends DiscordEmojisTranslations implements Listener {
    private static final DiscordSRVCompat listener = new DiscordSRVCompat();
    private static boolean enabled = false;

    static void register(JavaPlugin javaPlugin) {
        javaPlugin.getServer().getPluginManager().registerEvents(listener, javaPlugin);
    }

    static void enable() {
        enabled = true;
    }

    static void disable() {
        enabled = false;
    }

    @EventHandler
    public void onGameToDiscord(DiscordChatMessageEvent event) {
        if (!enabled) return;
        event.setMessage(unmapToDiscord.apply(event.getMessage()));
    }

    @EventHandler
    public void onGameToDiscord(DiscordMessageEvent event) {
        if (!enabled) return;
        event.setMessage(unmapToDiscord.apply(event.getMessage()));
    }

    @EventHandler
    public void onDiscordToGame(DiscordRelayEvent event) {
        if (!enabled) return;
        event.setFormattedMessage(unmapToMinecraft.apply(event.getFormattedMessage()));
    }
}
