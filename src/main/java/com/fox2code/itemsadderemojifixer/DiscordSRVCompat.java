package com.fox2code.itemsadderemojifixer;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.ListenerPriority;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordGuildMessagePostProcessEvent;
import github.scarsz.discordsrv.api.events.GameChatMessagePreProcessEvent;
import github.scarsz.discordsrv.api.events.VentureChatMessagePreProcessEvent;
import github.scarsz.discordsrv.dependencies.kyori.adventure.text.*;
import github.scarsz.discordsrv.dependencies.kyori.adventure.text.renderer.ComponentRenderer;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;

final class DiscordSRVCompat extends DiscordEmojisTranslations implements
        Listener, ComponentRenderer<Function<String, String>> {
    private static final DiscordSRVCompat listener = new DiscordSRVCompat();

    static void enable() {
        DiscordSRV.api.subscribe(listener);
    }

    static void disable() {
        DiscordSRV.api.unsubscribe(listener);
    }

    @Subscribe(priority = ListenerPriority.MONITOR)
    public void onGameToDiscordMonitor(GameChatMessagePreProcessEvent event) {
        event.setMessageComponent(render(event.getMessageComponent(), unmapToDiscord));
    }

    @Subscribe(priority = ListenerPriority.MONITOR)
    public void onGameToDiscordMonitor(VentureChatMessagePreProcessEvent event) {
        event.setMessageComponent(render(event.getMessageComponent(), unmapToDiscord));
    }

    @Subscribe(priority = ListenerPriority.MONITOR)
    public void onDiscordToGameMonitor(DiscordGuildMessagePostProcessEvent event) {
        event.setMinecraftMessage(render(event.getMinecraftMessage(), unmapToMinecraft));
    }

    @NotNull
    public Component render(@NotNull final Component component, @NotNull final Function<String, String> state) {
        Component modified = component;
        if (component instanceof TextComponent) {
            String content = ((TextComponent) component).content();
            String unmapped = state.apply(content);
            if (unmapped.length() != content.length()) {
                modified = ((TextComponent) component)
                        .content(unmapped);
            }
        }  else if (component instanceof TranslatableComponent) {
            List<Component> args = ((TranslatableComponent) component).args();
            List<Component> newArgs = null;
            int i = 0;

            for(int size = args.size(); i < size; ++i) {
                Component original = args.get(i);
                Component replaced = this.render(original, state);
                if (replaced != component && newArgs == null) {
                    newArgs = new ArrayList<>(size);
                    if (i > 0) {
                        newArgs.addAll(args.subList(0, i));
                    }
                }

                if (newArgs != null) {
                    newArgs.add(replaced);
                }
            }

            if (newArgs != null) {
                modified = ((TranslatableComponent)component).args(newArgs);
            }
        }

        List<Component> children = component.children();
        List<Component> newChildren = null;
        int i = 0;

        for(int size = children.size(); i < size; ++i) {
            Component original = children.get(i);
            Component replaced = this.render(original, state);
            if (replaced != component && newChildren == null) {
                newChildren = new ArrayList<>(size);
                if (i > 0) {
                    newChildren.addAll(children.subList(0, i));
                }
            }

            if (newChildren != null) {
                newChildren.add(replaced);
            }
        }

        return newChildren != null ? modified.children(newChildren) : modified;
    }
}
