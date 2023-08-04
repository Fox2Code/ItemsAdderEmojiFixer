package com.fox2code.itemsadderemojifixer.intmap;

public abstract class ItemsAdderEmojiFixerIntMap {
    private final String provider;

    ItemsAdderEmojiFixerIntMap(String provider) {
        this.provider = provider;
    }

    public String getProvider() {
        return provider;
    }

    public abstract void clear();

    public abstract void put(int val, String arg);

    public abstract void putIfAbsent(int val, String arg);

    public abstract String get(int val);

    public static ItemsAdderEmojiFixerIntMap newIntMap() {
        try {
            return ItemsAdderEmojiFixerIntMapFastUtils.create();
        } catch (Throwable ignored) {}
        try {
            return ItemsAdderEmojiFixerIntMapDiscordSRV.create();
        } catch (Throwable ignored) {}
        return new ItemsAdderEmojiFixerIntMapJVM();
    }
}
