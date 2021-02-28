package me.towdium.jecalculation.network;

import me.towdium.jecalculation.event.PlayerEventHandler;
import net.minecraftforge.common.MinecraftForge;

/**
 * @author Towdium
 */
public class ProxyServer implements IProxy {
    public static PlayerHandlerServer playerHandler = new PlayerHandlerServer();

    @Override
    public IPlayerHandler getPlayerHandler() {
        return playerHandler;
    }

    @Override
    public void init() {
        MinecraftForge.EVENT_BUS.register(new PlayerEventHandler());
    }

}
