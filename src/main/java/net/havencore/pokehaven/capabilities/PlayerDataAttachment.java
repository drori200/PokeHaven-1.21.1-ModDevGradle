package net.havencore.pokehaven.capabilities;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Immutable payload stored on a player that contains all persistent capability data grouped by segment.
 */
public record PlayerDataAttachment(Map<ResourceLocation, Map<String, PlayerDataSlot>> segments) {
    private static final Codec<Tag> TAG_CODEC = Codec.PASSTHROUGH.xmap(
            dynamic -> dynamic.convert(NbtOps.INSTANCE).getValue(),
            tag -> new Dynamic<>(NbtOps.INSTANCE, tag)
    );

    public static final Codec<PlayerDataAttachment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.unboundedMap(Codec.STRING, PlayerDataSlot.codec(TAG_CODEC).codec()))
                    .fieldOf("segments")
                    .forGetter(PlayerDataAttachment::segments)
    ).apply(instance, PlayerDataAttachment::new));

    public PlayerDataAttachment {
        segments = segments.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> Map.copyOf(entry.getValue())));
    }

    public static PlayerDataAttachment empty() {
        return new PlayerDataAttachment(Map.of());
    }

    public <T> Optional<T> get(PlayerDataKey<T> key) {
        Map<String, PlayerDataSlot> segment = segments.get(key.segment());
        if (segment == null) {
            return Optional.empty();
        }
        PlayerDataSlot slot = segment.get(key.path());
        if (slot == null || !slot.typeId().equals(key.type().id())) {
            return Optional.empty();
        }
        return key.type().codec().parse(new Dynamic<>(NbtOps.INSTANCE, slot.value())).result();
    }

    public <T> PlayerDataAttachment with(PlayerDataKey<T> key, @Nullable T value) {
        Map<ResourceLocation, Map<String, PlayerDataSlot>> mutableSegments = deepCopy();
        Map<String, PlayerDataSlot> segment = mutableSegments.computeIfAbsent(key.segment(), unused -> new HashMap<>());
        if (value == null) {
            segment.remove(key.path());
            if (segment.isEmpty()) {
                mutableSegments.remove(key.segment());
            }
        } else {
            segment.put(key.path(), PlayerDataSlot.from(key, value));
        }
        return new PlayerDataAttachment(toImmutable(mutableSegments));
    }

    public PlayerDataAttachment clear(ResourceLocation segmentId) {
        if (!segments.containsKey(segmentId)) {
            return this;
        }
        Map<ResourceLocation, Map<String, PlayerDataSlot>> mutableSegments = deepCopy();
        mutableSegments.remove(segmentId);
        return new PlayerDataAttachment(toImmutable(mutableSegments));
    }

    private Map<ResourceLocation, Map<String, PlayerDataSlot>> deepCopy() {
        Map<ResourceLocation, Map<String, PlayerDataSlot>> copy = new HashMap<>();
        segments.forEach((segmentId, entries) -> copy.put(segmentId, new HashMap<>(entries)));
        return copy;
    }

    private static Map<ResourceLocation, Map<String, PlayerDataSlot>> toImmutable(Map<ResourceLocation, Map<String, PlayerDataSlot>> mutableSegments) {
        return mutableSegments.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> Map.copyOf(entry.getValue())));
    }
}
