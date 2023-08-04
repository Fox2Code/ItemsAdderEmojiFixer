package com.fox2code.itemsadderemojifixer.intmap;

import java.util.HashMap;

final class ItemsAdderEmojiFixerIntMapJVM extends ItemsAdderEmojiFixerIntMap {
    private final HashMap<Integer, String> hashMap = new HashMap<>();

    ItemsAdderEmojiFixerIntMapJVM() {
        super("JVM HashMap (Slow)");
    }

    @Override
    public void clear() {
        this.hashMap.clear();
    }

    @Override
    public void put(int val, String arg) {
        this.hashMap.put(val, arg);
    }

    @Override
    public void putIfAbsent(int val, String arg) {
        this.hashMap.putIfAbsent(val, arg);
    }

    @Override
    public String get(int val) {
        return this.hashMap.get(val);
    }
}
