package net.havencore.pokehaven.capabilities;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;

/**
 * Describes the codec used to store a value for a specific data slot.
 */
public record PlayerDataType<T>(ResourceLocation id, Codec<T> codec) {
}
