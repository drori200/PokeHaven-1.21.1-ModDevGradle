package net.havencore.pokehaven.capabilities;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.DataResult;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

final class PlayerDataSlot {
    private final ResourceLocation typeId;
    private final Tag value;

    private PlayerDataSlot(ResourceLocation typeId, Tag value) {
        this.typeId = typeId;
        this.value = value;
    }

    static <T> PlayerDataSlot from(PlayerDataKey<T> key, T value) {
        DataResult<Tag> encoded = key.type().codec().encodeStart(NbtOps.INSTANCE, value);
        Tag tag = encoded.getOrThrow(error -> new IllegalStateException("Failed to encode player data for key " + key + ": " + error));
        return new PlayerDataSlot(key.type().id(), tag);
    }

    static MapCodec<PlayerDataSlot> codec(Codec<Tag> tagCodec) {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("type").forGetter(PlayerDataSlot::typeId),
                tagCodec.fieldOf("value").forGetter(PlayerDataSlot::value)
        ).apply(instance, PlayerDataSlot::new));
    }

    ResourceLocation typeId() {
        return typeId;
    }

    Tag value() {
        return value;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof PlayerDataSlot other) {
            return typeId.equals(other.typeId) && value.equals(other.value);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeId, value);
    }
}
