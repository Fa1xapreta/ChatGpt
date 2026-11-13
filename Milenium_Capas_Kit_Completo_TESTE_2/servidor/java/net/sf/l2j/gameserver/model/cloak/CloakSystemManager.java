package net.sf.l2j.gameserver.model.cloak;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/** Compatibility bridge for legacy callers after migrating cloak rendering.
 * The reference native engine uses its own configuration. No old 0x81 D0-D3
 * packets may be sent when its old receiver has been removed.
 */
public final class CloakSystemManager
{
    private CloakSystemManager() { }
    public synchronized void reload() { }
    public int getRecordCount() { return 0; }
    public void sendConfig(L2PcInstance player) { }
    public static CloakSystemManager getInstance() { return Holder.INSTANCE; }
    private static final class Holder
    {
        private static final CloakSystemManager INSTANCE = new CloakSystemManager();
    }
}
