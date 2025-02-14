package dev.felnull.imp.client.renderer.item;

import dev.felnull.imp.block.IMPBlocks;
import dev.felnull.imp.integration.PatchouliIntegration;
import dev.felnull.imp.item.IMPItems;
import dev.felnull.otyacraftengine.client.renderer.item.ItemRendererRegister;

public class IMPItemRenderers {
    public static ManualItemRenderer manualItemRenderer;

    public static void init() {
        ItemRendererRegister.register(IMPBlocks.MUSIC_MANAGER, new MusicManagerItemRenderer());
        ItemRendererRegister.register(IMPBlocks.CASSETTE_DECK, new CassetteDeckItemRenderer());
        ItemRendererRegister.register(IMPBlocks.BOOMBOX, new BoomboxItemRenderer());
        ItemRendererRegister.register(IMPItems.PARABOLIC_ANTENNA, new ParabolicAntennaItemRenderer());
        var cr = new CassetteTapeItemRenderer();
        ItemRendererRegister.register(IMPItems.CASSETTE_TAPE, cr);
        ItemRendererRegister.register(IMPItems.CASSETTE_TAPE_GLASS, cr);
        ItemRendererRegister.register(IMPItems.ANTENNA, new AntennaItemRenderer());
        if (PatchouliIntegration.isEnableIntegration() && IMPItems.MANUAL != null) {
            manualItemRenderer = new ManualItemRenderer();
            ItemRendererRegister.register(IMPItems.MANUAL, manualItemRenderer);
        }
    }
}
