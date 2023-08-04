package com.fox2code.itemsadderemojifixer.intmap;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

final class ItemsAdderEmojiFixerIntMapFastUtils extends ItemsAdderEmojiFixerIntMap {
    private final Int2ObjectOpenHashMap<String> openHashMap = new Int2ObjectOpenHashMap<>();


    ItemsAdderEmojiFixerIntMapFastUtils() {
        super("FastUtils OpenHashMap");
    }

    @Override
    public void clear() {
        this.openHashMap.clear();
    }

    @Override
    public void put(int val, String arg) {
        this.openHashMap.put(val, arg);
    }

    @Override
    public void putIfAbsent(int val, String arg) {
        this.openHashMap.putIfAbsent(val, arg);
    }

    @Override
    public String get(int val) {
        return this.openHashMap.get(val);
    }

    static ItemsAdderEmojiFixerIntMap create() {
        return new ItemsAdderEmojiFixerIntMapFastUtils();
    }
}
