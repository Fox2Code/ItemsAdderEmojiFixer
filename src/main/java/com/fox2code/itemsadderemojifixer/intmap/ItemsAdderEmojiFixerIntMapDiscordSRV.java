package com.fox2code.itemsadderemojifixer.intmap;

import github.scarsz.discordsrv.dependencies.trove.map.hash.TIntObjectHashMap;

public final class ItemsAdderEmojiFixerIntMapDiscordSRV extends ItemsAdderEmojiFixerIntMap {
    private final TIntObjectHashMap<String> intObjectHashMap = new TIntObjectHashMap<>();

    ItemsAdderEmojiFixerIntMapDiscordSRV() {
        super("GNU Trove TIntObjectHashMap bundled in DiscordSRV");
    }

    @Override
    public void clear() {
        intObjectHashMap.clear();
    }

    @Override
    public void put(int val, String arg) {
        intObjectHashMap.put(val, arg);
    }

    @Override
    public void putIfAbsent(int val, String arg) {
        intObjectHashMap.putIfAbsent(val, arg);
    }

    @Override
    public String get(int val) {
        return intObjectHashMap.get(val);
    }

    static ItemsAdderEmojiFixerIntMap create() {
        return new ItemsAdderEmojiFixerIntMapDiscordSRV();
    }
}
