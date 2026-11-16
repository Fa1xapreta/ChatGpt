/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.util;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.network.L2GameClient;

/**
 * Collection of flood protectors for single player.
 * @author fordfrog
 */
public final class FloodProtectors
{
	private final FloodProtectorAction _rollDice;
	private final FloodProtectorAction _globalChat;
	private final FloodProtectorAction _tradeChat;
	private final FloodProtectorAction _heroVoice;
	private final FloodProtectorAction _subclass;
	private final FloodProtectorAction _dropItem;
	private final FloodProtectorAction _serverBypass;
    private final FloodProtectorAction _npcHeaderQuery;
    private final FloodProtectorAction _autoFarmStatusQuery;
    private final FloodProtectorAction _playerModeStatusQuery;
	private final FloodProtectorAction _multiSell;
	private final FloodProtectorAction _manufacture;
	private final FloodProtectorAction _manor;
	private final FloodProtectorAction _sendMail;
	private final FloodProtectorAction _characterSelect;
	private final FloodProtectorAction _useEnchant;
	
	/**
	 * Creates new instance of FloodProtectors.
	 * @param client client for which the collection of flood protectors is being created.
	 */
	public FloodProtectors(final L2GameClient client)
	{
		super();
		_rollDice = new FloodProtectorAction(client, Config.FLOOD_PROTECTOR_ROLL_DICE);
		_globalChat = new FloodProtectorAction(client, Config.FLOOD_PROTECTOR_SHOUT_VOICE);
		_tradeChat = new FloodProtectorAction(client, Config.FLOOD_PROTECTOR_TRADE_CHAT);
		_heroVoice = new FloodProtectorAction(client, Config.FLOOD_PROTECTOR_HERO_VOICE);
		_subclass = new FloodProtectorAction(client, Config.FLOOD_PROTECTOR_SUBCLASS);
		_dropItem = new FloodProtectorAction(client, Config.FLOOD_PROTECTOR_DROP_ITEM);
		_serverBypass = new FloodProtectorAction(client, Config.FLOOD_PROTECTOR_SERVER_BYPASS);
        // Read-only status requests must not consume the manual-action bucket.
        _npcHeaderQuery = new FloodProtectorAction(client,
            queryConfig("NpcHeaderQueryFloodProtector"));
        _autoFarmStatusQuery = new FloodProtectorAction(client,
            queryConfig("AutoFarmStatusFloodProtector"));
        _playerModeStatusQuery = new FloodProtectorAction(client,
            queryConfig("PlayerModeStatusFloodProtector"));
		_multiSell = new FloodProtectorAction(client, Config.FLOOD_PROTECTOR_MULTISELL);
		_manufacture = new FloodProtectorAction(client, Config.FLOOD_PROTECTOR_MANUFACTURE);
		_manor = new FloodProtectorAction(client, Config.FLOOD_PROTECTOR_MANOR);
		_sendMail = new FloodProtectorAction(client, Config.FLOOD_PROTECTOR_SENDMAIL);
		_characterSelect = new FloodProtectorAction(client, Config.FLOOD_PROTECTOR_CHARACTER_SELECT);
		_useEnchant = new FloodProtectorAction(client, Config.FLOOD_PROTECTOR_USE_ENCHANT);
	}
	
	public FloodProtectorAction getRollDice()
	{
		return _rollDice;
	}
	
	public FloodProtectorAction getGlobalChat()
	{
		return _globalChat;
	}
	
	public FloodProtectorAction getTradeChat()
	{
		return _tradeChat;
	}
	
	public FloodProtectorAction getHeroVoice()
	{
		return _heroVoice;
	}
	
	public FloodProtectorAction getSubclass()
	{
		return _subclass;
	}
	
	public FloodProtectorAction getDropItem()
	{
		return _dropItem;
	}
	
	public FloodProtectorAction getServerBypass()
	{
		return _serverBypass;
	}
	
	public FloodProtectorAction getMultiSell()
	{
		return _multiSell;
	}
	
	public FloodProtectorAction getManufacture()
	{
		return _manufacture;
	}
	
	public FloodProtectorAction getManor()
	{
		return _manor;
	}
	
	public FloodProtectorAction getSendMail()
	{
		return _sendMail;
	}
	
	public FloodProtectorAction getCharacterSelect()
	{
		return _characterSelect;
	}
	
	public FloodProtectorAction getUseEnchant()
	{
		return _useEnchant;
	}

    private static FloodProtectorConfig queryConfig(String name)
    {
        FloodProtectorConfig source = Config.FLOOD_PROTECTOR_SERVER_BYPASS;
        FloodProtectorConfig copy = new FloodProtectorConfig(name);
        copy.FLOOD_PROTECTION_INTERVAL = source.FLOOD_PROTECTION_INTERVAL;
        copy.LOG_FLOODING = source.LOG_FLOODING;
        copy.PUNISHMENT_LIMIT = source.PUNISHMENT_LIMIT;
        copy.PUNISHMENT_TYPE = source.PUNISHMENT_TYPE;
        copy.PUNISHMENT_TIME = source.PUNISHMENT_TIME;
        return copy;
    }

    private static int interfaceQueryKind(String command)
    {
        if (command == null)
            return 0;
        if (command.startsWith("NpcHeaderInfo "))
            return 1;
        if ("voiced_autoFarmUiStatus2".equals(command))
            return 2;
        if ("voiced_playerModeStatus".equals(command))
            return 3;
        return 0;
    }

    public static boolean isInterfaceStateQuery(String command)
    {
        return interfaceQueryKind(command) != 0;
    }

    public FloodProtectorAction getBypassProtector(String command)
    {
        switch (interfaceQueryKind(command))
        {
            case 1: return _npcHeaderQuery;
            case 2: return _autoFarmStatusQuery;
            case 3: return _playerModeStatusQuery;
            default: return _serverBypass;
        }
    }
}