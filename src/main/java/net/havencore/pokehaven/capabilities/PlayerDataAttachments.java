package net.havencore.pokehaven.capabilities;

import net.havencore.pokehaven.PokeHaven;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Collection of attachment registrations backing the player persistent data capability.
 */
public final class PlayerDataAttachments {
    private PlayerDataAttachments() {
    }

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, PokeHaven.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerDataAttachment>> PLAYER_DATA =
            ATTACHMENT_TYPES.register("player_data",
                    () -> AttachmentType.builder(PlayerDataAttachment::empty)
                            .serialize(PlayerDataAttachment.CODEC)
                            .copyOnDeath()
                            .build());
}
