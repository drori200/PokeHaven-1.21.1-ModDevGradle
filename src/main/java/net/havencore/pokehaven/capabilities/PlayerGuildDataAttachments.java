package net.havencore.pokehaven.capabilities;

import net.havencore.pokehaven.PokeHaven;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Collection of attachment registrations backing player guild capabilities.
 */
public final class PlayerGuildDataAttachments {
    private PlayerGuildDataAttachments() {
    }

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, PokeHaven.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerGuildAttachment>> PLAYER_GUILD =
            ATTACHMENT_TYPES.register("player_guild",
                    () -> AttachmentType.builder(PlayerGuildAttachment::empty)
                            .serialize(PlayerGuildAttachment.CODEC.codec())
                            .copyOnDeath()
                            .build());
}
