package net.havencore.pokehaven.capabilities;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public interface IPlayerData {
    /**
     * Retrieves a value stored for the supplied key.
     */
    <T> Optional<T> get(PlayerDataKey<T> key);

    /**
     * Stores a value under the supplied key. Passing {@code null} removes the value from the segment.
     */
    <T> void set(PlayerDataKey<T> key, @Nullable T value);

    /**
     * Removes every value stored in the supplied segment.
     */
    void clearSegment(ResourceLocation segmentId);

}
