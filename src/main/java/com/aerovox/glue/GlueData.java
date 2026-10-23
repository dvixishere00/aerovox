package com.aerovox.glue;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Persistent world data: which block positions are superglued together.
 * Block states are NOT stored here; they remain in the world. This only tracks membership,
 * so a block can never belong to two groups at once (prevents duplication).
 */
public final class GlueData extends SavedData {
    private static final String DATA_NAME = "aerovox_glue";
    private static final SavedData.Factory<GlueData> FACTORY =
            new SavedData.Factory<>(GlueData::new, GlueData::load, DataFixTypes.LEVEL);

    private final Map<UUID, Set<Long>> groups = new HashMap<>();
    private final Map<Long, UUID> index = new HashMap<>();

    public GlueData() {}

    public static GlueData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public Optional<UUID> groupAt(BlockPos pos) {
        return Optional.ofNullable(index.get(pos.asLong()));
    }

    public Set<BlockPos> positionsOf(UUID id) {
        Set<Long> packed = groups.get(id);
        if (packed == null) return Set.of();
        Set<BlockPos> out = new HashSet<>(packed.size());
        for (long p : packed) out.add(BlockPos.of(p));
        return out;
    }

    public UUID createGroup(Collection<BlockPos> positions) {
        UUID id = UUID.randomUUID();
        Set<Long> set = new HashSet<>(positions.size());
        for (BlockPos p : positions) {
            long key = p.asLong();
            set.add(key);
            index.put(key, id);
        }
        groups.put(id, set);
        setDirty();
        return id;
    }

    public void removeGroup(UUID id) {
        Set<Long> packed = groups.remove(id);
        if (packed != null) {
            for (long key : packed) index.remove(key);
            setDirty();
        }
    }

    /** Called when a glued block is broken or destroyed. */
    public void forgetBlock(BlockPos pos) {
        long key = pos.asLong();
        UUID id = index.remove(key);
        if (id == null) return;
        Set<Long> packed = groups.get(id);
        if (packed != null) {
            packed.remove(key);
            if (packed.isEmpty()) groups.remove(id);
        }
        setDirty();
    }

    private static GlueData load(CompoundTag tag, HolderLookup.Provider registries) {
        GlueData data = new GlueData();
        ListTag list = tag.getList("groups", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag g = list.getCompound(i);
            UUID id = g.getUUID("id");
            Set<Long> packed = new HashSet<>();
            for (long p : g.getLongArray("positions")) {
                packed.add(p);
                data.index.put(p, id);
            }
            data.groups.put(id, packed);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, Set<Long>> e : groups.entrySet()) {
            CompoundTag g = new CompoundTag();
            g.putUUID("id", e.getKey());
            g.putLongArray("positions", e.getValue().stream().mapToLong(Long::longValue).toArray());
            list.add(g);
        }
        tag.put("groups", list);
        return tag;
    }
}
