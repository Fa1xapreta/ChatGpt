/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.network.clientpackets;

import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;

import TeleporterInterface.TeleLocation2;
import TeleporterInterface.TeleportLocationData2;
import TeleporterInterface.TeleportRaidStatus;
import net.sf.l2j.Base64;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.communitybbs.CommunityBoard;
import net.sf.l2j.gameserver.datatables.AdminCommandAccessRights;
import net.sf.l2j.gameserver.datatables.CharNameTable;
import net.sf.l2j.gameserver.datatables.CharTemplateTable;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.datatables.TeleportLocationTable;
import net.sf.l2j.gameserver.datatables.custom.AuctionTable;
import net.sf.l2j.gameserver.datatables.custom.IconTable;
import net.sf.l2j.gameserver.datatables.custom.SkinTable;
import net.sf.l2j.gameserver.datatables.custom.WeaponSkinTable;
import net.sf.l2j.gameserver.handler.AdminCommandHandler;
import net.sf.l2j.gameserver.handler.CBBypassHandler;
import net.sf.l2j.gameserver.handler.CustomBypassHandler;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.handler.ICBBypassHandler;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.handler.VoicedCommandHandler;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.instancemanager.BuffWndManager;
import net.sf.l2j.gameserver.instancemanager.HuntPassManager;
import net.sf.l2j.gameserver.instancemanager.autofarmsecondary.AutofarmSecondaryManager;
import net.sf.l2j.gameserver.instancemanager.balance.BalanceManager;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2TeleportLocation;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2ClassMasterInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2GrandBossInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2MonsterInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2OlympiadManagerInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2RaidBossInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2TeleporterInstance;
import net.sf.l2j.gameserver.model.actor.instance.NewDailyRewardManager;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.base.ClassId;
import net.sf.l2j.gameserver.model.entity.Hero;
import net.sf.l2j.gameserver.model.entity.events.capturetheflag.CTFEvent;
import net.sf.l2j.gameserver.model.entity.events.deathmatch.DMEvent;
import net.sf.l2j.gameserver.model.entity.events.killtheboss.KTBEvent;
import net.sf.l2j.gameserver.model.entity.events.lastman.LMEvent;
import net.sf.l2j.gameserver.model.entity.events.teamvsteam.TvTEvent;
import net.sf.l2j.gameserver.model.entity.events.tournaments.properties.ArenaConfig;
import net.sf.l2j.gameserver.model.entity.events.treasurehunt.MiningInteractionManager;
import net.sf.l2j.gameserver.model.holder.AuctionHolder;
import net.sf.l2j.gameserver.model.item.DropCategory;
import net.sf.l2j.gameserver.model.item.DropData;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.model.olympiad.OlympiadManager;
import net.sf.l2j.gameserver.model.zone.ZoneId;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ExPCCafePointInfo;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.templates.StatsSet;
import net.sf.l2j.gameserver.util.CheatLog;
import net.sf.l2j.gameserver.util.GMAudit;
import net.sf.l2j.util.Rnd;

public final class RequestBypassToServer extends L2GameClientPacket
{
	private String _command;
	//private final Map<Integer, Integer> _lastPageRaid = new ConcurrentHashMap<>();
	//private final Map<Integer, Integer> _lastPageGrand = new ConcurrentHashMap<>();
	/** Tracks the active skin-preview revert task per player (objectId  task). */
	private static final Map<Integer, ScheduledFuture<?>> _skinPreviewTasks = new ConcurrentHashMap<>();
	/** Tracks the active weapon-skin-preview revert task per player (objectId  task). */
	private static final Map<Integer, ScheduledFuture<?>> _weaponPreviewTasks = new ConcurrentHashMap<>();
	
	@Override
	protected void readImpl()
	{
		_command = readS();
	}
	
	@SuppressWarnings("unused")
	@Override
	protected void runImpl()
	{
	    final L2PcInstance activeChar = getClient().getActiveChar();

	    if (activeChar == null)
	    {
	        return;
	    }
	    
	    if (MiningInteractionManager.getInstance().handle(
	            activeChar,
	            _command))
	        {
	            return;
	        }
	    

	    if (!getClient().getFloodProtectors().getServerBypass().tryPerformAction(_command))
	    {
	        if (_command != null && _command.startsWith("GkGo"))
	        {
	            activeChar.sendMessage(
	                "Aguarde alguns instantes antes de tentar novamente."
	            );
	        }

	        if (_command != null
	            && _command.startsWith("ClassMasterTutorial_"))
	        {
	            activeChar.sendMessage(
	                "Aguarde alguns instantes antes de tentar trocar de classe novamente."
	            );
	        }

	        return;
	    }
        // Milenium Monster Data: preserve the flood check and existing routes.
        if (net.sf.l2j.gameserver.custom.MileniumMonsterData.handles(_command))
        {
            net.sf.l2j.gameserver.custom.MileniumMonsterData.handle(activeChar, _command);
            return;
        }

	 // Consulta somente visual, validada contra o alvo atual.
	    if (_command != null&& _command.startsWith("NpcHeaderInfo "))
	    {
	        final int requestedId;

	        try
	        {
	            requestedId = Integer.parseInt(_command.substring("NpcHeaderInfo ".length()).trim());
	        }
	        catch (NumberFormatException e)
	        {
	            return;
	        }

	        if (!(activeChar.getTarget() instanceof L2Npc))
	            return;

	        final L2Npc npc = (L2Npc) activeChar.getTarget();
	        if (npc.getObjectId() != requestedId)
	            return;
	        final boolean hideHeader =npc instanceof L2NpcInstance&& !(npc instanceof L2MonsterInstance)&& !(npc instanceof L2RaidBossInstance)&& !(npc instanceof L2GrandBossInstance)&& npc.isInsideZone(ZoneId.TOWN);
	        activeChar.sendPacket(new ExShowScreenMessage("CustomEvent=NpcHeaderV1|"+ requestedId + "|"+ (hideHeader ? "1" : "0"),1));
	        return;
	    }
	    
	    // Este bloco fica DEPOIS da verificacao de flood.
	    if (_command != null
	        && _command.startsWith("ClassMasterTutorial_"))
	    {
	        L2ClassMasterInstance.onInterfaceTutorialLink(
	            activeChar,
	            _command.substring("ClassMasterTutorial_".length())
	        );

	        return;
	    }

	    if (_command.isEmpty())
	    {
	        _log.info(activeChar.getName() + " sent an empty requestBypass packet.");
	        activeChar.logout();
	        return;
	    }

	    // METODO DA INTERFACE BUFFS
	    if (BuffWndManager.getInstance().handle(activeChar, _command))
	    {
	        return;
	    }
	    // FIM

	    try
	    {
	        if (_command.startsWith("admin_"))
	        {
	            if (!activeChar.isGM())
	            {
	                _log.warning("Player: " + activeChar.getName() + " is trying to execute a GM command!");
	                CheatLog.auditGMAction(activeChar.getName());
	                activeChar.logout();
	                return;
	            }

	            String command = _command.split(" ")[0];

	            IAdminCommandHandler ach = AdminCommandHandler.getInstance().getAdminCommandHandler(command);
	            if (ach == null)
	            {
	                if (activeChar.isGM())
	                    activeChar.sendMessage("The command " + command.substring(6) + " doesn't exist.");

	                _log.warning("No handler registered for admin command '" + command + "'");
	                return;
	            }

	            if (!AdminCommandAccessRights.getInstance().hasAccess(command, activeChar.getAccessLevel()))
	            {
	                activeChar.sendMessage("You don't have the access rights to use this command.");
	                _log.warning(activeChar.getName() + " tried to use admin command " + command + " without proper Access Level.");
	                return;
	            }

	            if (Config.GMAUDIT)
	                GMAudit.auditGMAction(activeChar.getName() + " [" + activeChar.getObjectId() + "]", _command, (activeChar.getTarget() != null ? activeChar.getTarget().getName() : "no-target"));

	            ach.useAdminCommand(_command, activeChar);
	        }
	        else if (_command.startsWith("player_help "))
	        {
	            playerHelp(activeChar, _command.substring(12));
	        }
	        else if (_command.startsWith("player_book "))
	        {
	            playerTeleporter(activeChar, _command.substring(12));
	        }
	        
	        //METODO DA INFORMACOES  NO NOME DO CHAR 
	        else if (_command.startsWith("TargetPlayerInfo "))
	        {
	            sendTargetPlayerInfo(activeChar, _command);
	        }
	        
	        // METODO DO TELEPORT DA INTERFACE
	        else if (_command.equals("GkOpenTeleport"))
	        {
	            if (!checkTeleportVip(activeChar))
	                return;

	            activeChar.sendPacket(
	                new ExShowScreenMessage(
	                    "CustomEvent=TeleportAccessV1|OPEN",
	                    1
	                )
	            );

	            // Envia os estados iniciais sem depender de outro bypass.
	            TeleportRaidStatus.sendTo(activeChar);
	        }
	        else if (_command.equals("GkRaidStatus"))
	        {
	            if (!checkTeleportVip(activeChar))
	                return;

	            TeleportRaidStatus.sendTo(activeChar);
	        }
	        else if (_command.startsWith("GkGo "))
	        {
	            if (!checkTeleportVip(activeChar))
	                return;

	            // Formato esperado: GkGo _<ID>
	            if (!_command.startsWith("GkGo _") || _command.length() <= 6)
	            {
	                activeChar.sendMessage(
	                    "Comando de teleporte invalido."
	                );
	                return;
	            }

	            String targetLocation = _command.substring(6).trim();

	            TeleLocation2 teleLocation = TeleportLocationData2
	                .getInstance()
	                .getTeleportLocation(targetLocation);

	            if (teleLocation == null)
	            {
	                activeChar.sendMessage(
	                    "A localizacao de teletransporte nao existe."
	                );
	                return;
	            }

	            if (!checkallowed(activeChar))
	            {
	                activeChar.sendMessage(
	                    "Teleport: bloqueado pela verificacao checkallowed."
	                );
	                return;
	            }

	            if (teleLocation.isNoble() && !activeChar.isNoble())
	            {
	                activeChar.sendMessage(
	                    "Apenas os nobres podem ir a esta zona."
	                );
	                return;
	            }

	            int price = activeChar.getLevel() >= 40
	                ? teleLocation.getPrice()
	                : 0;

	            if (
	                price != 0 &&
	                !activeChar.destroyItemByItemId(
	                    "", 57, price, activeChar, true
	                )
	            )
	            {
	                return;
	            }

	            MagicSkillUse MSU = new MagicSkillUse(
	                activeChar,
	                activeChar,
	                teleLocation.getSkillEffectId(),
	                1,
	                1,
	                0
	            );

	            activeChar.broadcastPacket(MSU);

	            activeChar.teleToLocation(teleLocation, 50);

	            activeChar.sendPacket(
	                new ExPCCafePointInfo(
	                    activeChar,
	                    0,
	                    false,
	                    99981,
	                    false
	                )
	            );
	        }
			// INICIO DO NOVO REWARD DIARIO
			if (_command.startsWith("daily_claim"))
			{
				 activeChar.sendMessage("Daily Reward: clique recebido.");
				String[] args = _command.split(" ");
				int day = args.length > 1 ? Integer.parseInt(args[1]) : 0;
				NewDailyRewardManager.getInstance().claimDailyReward(activeChar, day);
			}
			else if (_command.startsWith("daily_page"))
			{
				String[] args = _command.split(" ");
				int page = args.length > 1 ? Integer.parseInt(args[1]) : 1;
				NewDailyRewardManager.getInstance().openDailyRewardHtml(activeChar, page);
			}
			else if (_command.equalsIgnoreCase(".dailyreward") || _command.equalsIgnoreCase("dailyreward"))
			{
				NewDailyRewardManager.getInstance().initialAllEngines(activeChar);
			}
			
			//METODOS DA INTERFACE
			else if (_command.startsWith("lucdevbp"))
			{
				HuntPassManager.getInstance().handle(activeChar, _command.substring(8));
			}
			
			else if (_command.startsWith("voiced_"))
			{
				String command = _command.split(" ")[0];
				
				IVoicedCommandHandler ach = VoicedCommandHandler.getInstance().getHandler(_command.substring(7));
				
				if (ach == null)
				{
					activeChar.sendMessage("The command " + command.substring(7) + " does not exist!");
					_log.warning(activeChar.getName() + " No handler registered for command '" + _command + "'");
					return;
				}
				
				ach.useVoicedCommand(_command.substring(7), activeChar, null);
			} 
			else if(_command.startsWith("custom_"))
			{
				L2PcInstance player = getClient().getActiveChar();
				CustomBypassHandler.getInstance().handleBypass(player, _command);
			}
			else if (_command.startsWith("bp_"))
			{
				String command = _command.split(" ")[0];
				
				ICBBypassHandler bh = CBBypassHandler.getInstance().getBypassHandler(command);
				if (bh == null)
				{
					_log.warning("No handler registered for bypass '" + command + "'");
					return;
				}
				
				bh.handleBypass(_command, activeChar);
			}
			else if (_command.startsWith("npc_"))
			{
				if (!activeChar.validateBypass(_command))
					return;
				
				activeChar.setIsUsingCMultisell(false);
				
				int endOfId = _command.indexOf('_', 5);
				String id;
				if (endOfId > 0)
					id = _command.substring(4, endOfId);
				else
					id = _command.substring(4);
				
				try
				{
					final L2Object object = L2World.getInstance().findObject(Integer.parseInt(id));
					
					if (object != null && object instanceof L2Npc && endOfId > 0 && ((L2Npc) object).canInteract(activeChar))
						((L2Npc) object).onBypassFeedback(activeChar, _command.substring(endOfId + 1));
					
					activeChar.sendPacket(ActionFailed.STATIC_PACKET);
				}
				catch (NumberFormatException nfe)
				{
				}
			}
			else if (_command.startsWith("classe_change"))
			{
				StringTokenizer st = new StringTokenizer(_command);
				st.nextToken();
				String type = null;
				type = st.nextToken();
				try
				{
					if (activeChar.getBaseClass() != activeChar.getClassId().getId())
					{
						activeChar.sendMessage("You need to be in your base class to be able to use this item.");
						return ;
					}
					
					/*
					if (activeChar.getLevel() < 79)
					{
						activeChar.sendMessage("You need to be at least 80 level in order to use class card.");
						return;			
					}
					
					if (activeChar.getClassId() == ClassId.duelist || activeChar.getClassId() == ClassId.dreadnought || activeChar.getClassId() == ClassId.phoenixKnight || activeChar.getClassId() == ClassId.hellKnight || activeChar.getClassId() == ClassId.sagittarius || activeChar.getClassId() == ClassId.adventurer || activeChar.getClassId() == ClassId.archmage || activeChar.getClassId() == ClassId.soultaker || activeChar.getClassId() == ClassId.arcanaLord || activeChar.getClassId() == ClassId.cardinal || activeChar.getClassId() == ClassId.hierophant || activeChar.getClassId() == ClassId.evaTemplar || activeChar.getClassId() == ClassId.swordMuse || activeChar.getClassId() == ClassId.windRider || activeChar.getClassId() == ClassId.moonlightSentinel || activeChar.getClassId() == ClassId.mysticMuse || activeChar.getClassId() == ClassId.elementalMaster || activeChar.getClassId() == ClassId.evaSaint || activeChar.getClassId() == ClassId.shillienTemplar || activeChar.getClassId() == ClassId.spectralDancer || activeChar.getClassId() == ClassId.ghostHunter || activeChar.getClassId() == ClassId.ghostSentinel || activeChar.getClassId() == ClassId.stormScreamer || activeChar.getClassId() == ClassId.spectralMaster || activeChar.getClassId() == ClassId.shillienSaint || activeChar.getClassId() == ClassId.titan || activeChar.getClassId() == ClassId.grandKhauatari || activeChar.getClassId() == ClassId.dominator || activeChar.getClassId() == ClassId.doomcryer || activeChar.getClassId() == ClassId.fortuneSeeker || activeChar.getClassId() == ClassId.maestro)
					{
						activeChar.sendMessage("You need to be at least third class in order to use class card.");
						return;			
					}
					 */
					
					if (activeChar.isInOlympiadMode())
					{
						activeChar.sendMessage("This item cannot be used on olympiad games.");
						return;			
					}
					ClassChangeCoin(activeChar, type); 
				}
				catch (StringIndexOutOfBoundsException e)
				{
				}
			}
			
			
			else if (_command.startsWith("class_index"))
			{
				NpcHtmlMessage html = new NpcHtmlMessage(0);
				html.setFile("data/html/mods/class_changer/Class.htm");
				activeChar.sendPacket(html);
				activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			}
			// Navigate throught Manor windows
			else if (_command.startsWith("manor_menu_select?"))
			{
				L2Object object = activeChar.getTarget();
				if (object instanceof L2Npc)
					((L2Npc) object).onBypassFeedback(activeChar, _command);
			}
			else if (_command.startsWith("bbs_") || _command.startsWith("_bbs") || _command.startsWith("_friend") || _command.startsWith("_mail") || _command.startsWith("_block"))
			{
				CommunityBoard.getInstance().handleCommands(getClient(), _command);
			}
			else if (_command.startsWith("Quest "))
			{
				if (!activeChar.validateBypass(_command))
					return;
				
				String[] str = _command.substring(6).trim().split(" ", 2);
				if (str.length == 1)
					activeChar.processQuestEvent(str[0], "");
				else
					activeChar.processQuestEvent(str[0], str[1]);
			}
			else if (_command.startsWith("_match"))
			{
				String params = _command.substring(_command.indexOf("?") + 1);
				StringTokenizer st = new StringTokenizer(params, "&");
				int heroclass = Integer.parseInt(st.nextToken().split("=")[1]);
				int heropage = Integer.parseInt(st.nextToken().split("=")[1]);
				int heroid = Hero.getInstance().getHeroByClass(heroclass);
				if (heroid > 0)
					Hero.getInstance().showHeroFights(activeChar, heroclass, heroid, heropage);
			}
			else if (_command.startsWith("_diary"))
			{
				String params = _command.substring(_command.indexOf("?") + 1);
				StringTokenizer st = new StringTokenizer(params, "&");
				int heroclass = Integer.parseInt(st.nextToken().split("=")[1]);
				int heropage = Integer.parseInt(st.nextToken().split("=")[1]);
				int heroid = Hero.getInstance().getHeroByClass(heroclass);
				if (heroid > 0)
					Hero.getInstance().showHeroDiary(activeChar, heroclass, heroid, heropage);
			}
			else if (_command.startsWith("arenachange")) // change
			{
				final boolean isManager = activeChar.getCurrentFolkNPC() instanceof L2OlympiadManagerInstance;
				if (!isManager)
				{
					// Without npc, command can be used only in observer mode on arena
					if (!activeChar.inObserverMode() || activeChar.isInOlympiadMode() || activeChar.getOlympiadGameId() < 0)
						return;
				}
				
				if (OlympiadManager.getInstance().isRegisteredInComp(activeChar))
				{
					activeChar.sendPacket(SystemMessageId.WHILE_YOU_ARE_ON_THE_WAITING_LIST_YOU_ARE_NOT_ALLOWED_TO_WATCH_THE_GAME);
					return;
				}
				
				final int arenaId = Integer.parseInt(_command.substring(12).trim());
				activeChar.enterOlympiadObserverMode(arenaId);
			}
			else if (_command.startsWith("balance"))
			{
				int classId = Integer.parseInt(_command.substring(8));
				NpcHtmlMessage htm = new NpcHtmlMessage(0);
				htm.setFile("data/html/admin/balance/show_list.htm");
				htm.replace("%classid%", classId);
				htm.replace("%classname%", CharTemplateTable.getInstance().getClassNameById(classId));
				activeChar.sendPacket(htm);
			}
			else if (_command.startsWith("makeBalance"))
			{
				String[] args = _command.substring(12).split(" ");
				int targetClassId = args[0].equals("ALL") ? -1 : Integer.parseInt(args[0]);
				int classId = Integer.parseInt(args[1]);
				NpcHtmlMessage htm = new NpcHtmlMessage(0);
				htm.setFile("data/html/admin/balance/show_change.htm");
				htm.replace("%classid%", classId);
				htm.replace("%targetclassid%", targetClassId);
				htm.replace("%dmgmod%", BalanceManager.getInstance().getMod(classId, targetClassId));
				for (ClassId ci : ClassId.values())
				{
					if (ci.getId() == classId)
					{
						htm.replace("%classname%", ci.name());
						break;
					}
				}
				if (targetClassId == -1)
					htm.replace("%targetclassname%", "ALL CLASSES");
				else
				{
					for (ClassId ci : ClassId.values())
					{
						if (ci.getId() == targetClassId)
						{
							htm.replace("%targetclassname%", ci.name());
							break;
						}
					}
				}
				activeChar.sendPacket(htm);
			}
			else if (_command.startsWith("modDamage"))
			{
				String[] args = _command.substring(10).split(" ");
				if (args.length < 3)
					return;
				int targetClassId = args[0].equals("ALL") ? -1 : Integer.parseInt(args[0]);
				int classId = Integer.parseInt(args[1]);
				int mod = Integer.parseInt(args[2]);
				
				BalanceManager.getInstance().addMod(classId, targetClassId, mod);
				
				NpcHtmlMessage htm = new NpcHtmlMessage(0);
				htm.setFile("data/html/admin/balance/show_change.htm");
				htm.replace("%classid%", classId);
				htm.replace("%targetclassid%", targetClassId);
				htm.replace("%dmgmod%", BalanceManager.getInstance().getMod(classId, targetClassId));
				for (ClassId ci : ClassId.values())
				{
					if (ci.getId() == classId)
					{
						htm.replace("%classname%", ci.name());
						break;
					}
				}
				if (targetClassId == -1)
					htm.replace("%targetclassname%", "ALL CLASSES");
				else
				{
					for (ClassId ci : ClassId.values())
					{
						if (ci.getId() == targetClassId)
						{
							htm.replace("%targetclassname%", ci.name());
							break;
						}
					}
				}
				activeChar.sendPacket(htm);
			}
			else if (_command.equals("main"))
			{
				NpcHtmlMessage htm = new NpcHtmlMessage(0);
				htm.setFile("data/html/admin/balance/main.htm");
				activeChar.sendPacket(htm);
			}
			
			else if (_command.equalsIgnoreCase("pkrecovery"))
			{
				String htmContent = HtmCache.getInstance().getHtm("data/html/passkey/recovery.htm");
				NpcHtmlMessage html = new NpcHtmlMessage(1);
				html.setHtml(htmContent);
				html.replace("%question%", getPassKeyQuestion(activeChar));
				activeChar.sendPacket(html);
				html = null;
			}
			else if (_command.startsWith("pkset"))
			{
				StringTokenizer st = new StringTokenizer(_command, "]");
				
				if (st.countTokens() != 5)
				{
					activeChar.sendMessage("SVR: You have not entered all the data!");
					String htmContent = HtmCache.getInstance().getHtm("data/html/passkey/setup.htm");
					NpcHtmlMessage html = new NpcHtmlMessage(1);
					html.setHtml(htmContent);
					activeChar.sendPacket(html);
					html = null;
					return;
				}
				
				String newCommand = st.nextToken();
				String pass1 = st.nextToken();
				pass1 = pass1.substring(1, pass1.length() - 1);
				String pass2 = st.nextToken();
				pass2 = pass2.substring(1, pass2.length() - 1);
				String question = st.nextToken();
				question = question.substring(1, question.length() - 1);
				String answer = st.nextToken();
				answer = answer.substring(1, answer.length());
				
				if (pass1 == null || pass2 == null || question == null || answer == null)
				{
					activeChar.sendMessage("SVR: Input error");
					String htmContent = HtmCache.getInstance().getHtm("data/html/passkey/setup.htm");
					NpcHtmlMessage html = new NpcHtmlMessage(1);
					html.setHtml(htmContent);
					activeChar.sendPacket(html);
					html = null;
					return;
				}
				
				if (!pass1.equals(pass2))
				{
					activeChar.sendMessage("SVR: You entered different passwords");
					activeChar.sendMessage("pass1 = " + pass1);
					activeChar.sendMessage("pass2 = " + pass2);
					activeChar.sendMessage("Question = " + question);
					activeChar.sendMessage("answer = " + answer);
					
					String htmContent = HtmCache.getInstance().getHtm("data/html/passkey/setup.htm");
					NpcHtmlMessage html = new NpcHtmlMessage(1);
					html.setHtml(htmContent);
					activeChar.sendPacket(html);
					html = null;
					return;
				}
				insertPassKeyInformation(activeChar, pass1, question, answer);
				
				activeChar.sendMessage("SVR: password successfully, you can Pass.");
				String htmContent = HtmCache.getInstance().getHtm("data/html/passkey/login.htm");
				NpcHtmlMessage html = new NpcHtmlMessage(1);
				html.setHtml(htmContent);
				activeChar.sendPacket(html);
				html = null;
			}
			else if (_command.startsWith("pklogin"))
			{
				StringTokenizer st = new StringTokenizer(_command, " ");
				if (st.countTokens() != 2)
				{
					activeChar.sendMessage("SVR: You make a mistake when entering the password!");
					String htmContent = HtmCache.getInstance().getHtm("data/html/passkey/login.htm");
					NpcHtmlMessage html = new NpcHtmlMessage(1);
					html.setHtml(htmContent);
					activeChar.sendPacket(html);
					html = null;
					return;
				}
				String newCommand = st.nextToken();
				String pass = st.nextToken();
				
				Connection con = null;
				String query = "SELECT passkey FROM passkey WHERE obj_Id = ?";
				String pwdindb = "error";
				try
				{
					con = L2DatabaseFactory.getInstance().getConnection();
					PreparedStatement ps = con.prepareStatement(query);
					ps.setInt(1, activeChar.getObjectId());
					ResultSet rs = ps.executeQuery();
					
					while (rs.next())
						pwdindb = rs.getString(1);
					
					rs.close();
					ps.close();
					ps = null;
					rs = null;
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				finally
				{
					try
					{
						con.close();
					}
					catch (SQLException e)
					{
						e.printStackTrace();
					}
					con = null;
				}
				
				if (pwdindb.equals(encodePass(pass)))
				{
					activeChar.setIsParalyzed(false);
				}
				else
				{
					activeChar.sendMessage("You have not entered the correct password");
					String htmContent = HtmCache.getInstance().getHtm("data/html/passkey/login.htm");
					NpcHtmlMessage html = new NpcHtmlMessage(1);
					html.setHtml(htmContent);
					activeChar.sendPacket(html);
					html = null;
					return;
				}
			}
			else if (_command.startsWith("pkrec"))
			{
				StringTokenizer st = new StringTokenizer(_command, " ");
				if (st.countTokens() != 4)
				{
					activeChar.sendMessage("SVR: You make a mistake when entering data!");
					String htmContent = HtmCache.getInstance().getHtm("data/html/passkey/recovery.htm");
					NpcHtmlMessage html = new NpcHtmlMessage(1);
					html.setHtml(htmContent);
					html.replace("%question%", getPassKeyQuestion(activeChar));
					activeChar.sendPacket(html);
					html = null;
					return;
				}
				
				String newCommand = st.nextToken();
				String answer = st.nextToken();
				String pass1 = st.nextToken();
				String pass2 = st.nextToken();
				
				if (!pass1.equals(pass2))
				{
					activeChar.sendMessage("SVR: Вы ввели разные пароли");
					String htmContent = HtmCache.getInstance().getHtm("data/html/passkey/recovery.htm");
					NpcHtmlMessage html = new NpcHtmlMessage(1);
					html.setHtml(htmContent);
					html.replace("%question%", getPassKeyQuestion(activeChar));
					activeChar.sendPacket(html);
					html = null;
					return;
				}
				
				Connection con = null;
				String query = "SELECT answer FROM passkey WHERE obj_Id = ?";
				String anwindb = "error";
				try
				{
					con = L2DatabaseFactory.getInstance().getConnection();
					PreparedStatement ps = con.prepareStatement(query);
					ps.setInt(1, activeChar.getObjectId());
					ResultSet rs = ps.executeQuery();
					
					while (rs.next())
						anwindb = rs.getString(1);
					
					rs.close();
					ps.close();
					ps = null;
					rs = null;
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				finally
				{
					try
					{
						con.close();
					}
					catch (SQLException e)
					{
						e.printStackTrace();
					}
					con = null;
				}
				
				if (anwindb.equals(answer))
				{
					updPassKey(activeChar, pass1);
					activeChar.sendMessage("You have successfully changed your password.");
					String htmContent = HtmCache.getInstance().getHtm("data/html/passkey/login.htm");
					NpcHtmlMessage html = new NpcHtmlMessage(1);
					html.setHtml(htmContent);
					activeChar.sendPacket(html);
					html = null;
				}
				else
				{
					activeChar.sendMessage("You entered the wrong answer to your question");
					String htmContent = HtmCache.getInstance().getHtm("data/html/passkey/recovery.htm");
					NpcHtmlMessage html = new NpcHtmlMessage(1);
					html.setHtml(htmContent);
					html.replace("%question%", getPassKeyQuestion(activeChar));
					activeChar.sendPacket(html);
					html = null;
					return;
				}
				
			}
			else if (_command.startsWith("name_change"))
			{
				try
				{
					String name = _command.substring(12);
					
					if (name.length() < 3)
					{
						activeChar.sendMessage("Your name needs a minimum of 3 letters. Please try again.");
						return;
					}
					
					if (name.length() > 16)
					{
						activeChar.sendMessage("Your name cannot exceed 16 characters in length. Please try again.");
						return;
					}
					
					if (name.equals(activeChar.getName()))
					{
						activeChar.sendMessage("Please, choose a different name.");
						return;
					}
					
					if (activeChar.isClanLeader())
					{
						activeChar.sendMessage("Clan leaders can't change name!");
						return;
					}
					
					if (activeChar.getClan() != null)
					{
						activeChar.sendMessage("Clan members can't change name!");
						return;
					}
					
					if (!name.matches("^[a-zA-Z0-9]+$"))
					{
						activeChar.sendMessage("Incorrect name. Please try again.");
						return;
					}
					
					synchronized (CharNameTable.getInstance())
					{
						if (CharNameTable.doesCharNameExist(name))
						{
							activeChar.sendMessage("The chosen name already exists.");
							return;
						}
					}
					
					if (activeChar.destroyItemByItemId("Name Change", activeChar.getNameChangeItemId(), 1, null, true))
					{
						activeChar.setName(name);
						activeChar.sendMessage("Your new character name is " + name);
						activeChar.broadcastUserInfo();
						activeChar.store();
					}
				}
				catch (Exception e)
				{
					activeChar.sendMessage("Fill out the field correctly.");
				}
			}
			else if (_command.startsWith("special_name_change"))
			{
				try
				{
					final String name = _command.substring(20).trim();

					if (name.length() < 3)
					{
						activeChar.sendMessage("Your name needs a minimum of 3 characters. Please try again.");
						return;
					}

					if (name.length() > 16)
					{
						activeChar.sendMessage("Your name cannot exceed 16 characters in length. Please try again.");
						return;
					}

					if (name.equals(activeChar.getName()))
					{
						activeChar.sendMessage("Please, choose a different name.");
						return;
					}

					// Block characters that could cause database or client issues
					if (name.contains("'") || name.contains("\"") || name.contains("\\") || name.contains(";") || name.contains("%"))
					{
						activeChar.sendMessage("Name contains forbidden characters. Avoid: ' \" \\ ; %");
						return;
					}

					synchronized (CharNameTable.getInstance())
					{
						if (CharNameTable.doesCharNameExist(name))
						{
							activeChar.sendMessage("The chosen name already exists.");
							return;
						}
					}

					if (activeChar.destroyItemByItemId("Special Name Change", activeChar.getNameChangeItemId(), 1, null, true))
					{
						activeChar.setName(name);
						activeChar.sendMessage("Your new character name is: " + name);
						activeChar.broadcastUserInfo();
						activeChar.store();
					}
				}
				catch (Exception e)
				{
					activeChar.sendMessage("Fill out the field correctly.");
				}
			}
			else if (_command.startsWith("skin_try_page"))
			{
				try
				{
					final int page = Integer.parseInt(_command.substring(14).trim());
					net.sf.l2j.gameserver.handler.voicedcommandhandlers.VoicedSkinTry.showSkinMenu(activeChar, page);
				}
				catch (NumberFormatException e)
				{
					net.sf.l2j.gameserver.handler.voicedcommandhandlers.VoicedSkinTry.showSkinMenu(activeChar, 1);
				}
			}
			else if (_command.startsWith("skin_try "))
			{
				try
				{
					final int skinId = Integer.parseInt(_command.substring(9).trim());

					if (!SkinTable.getInstance().getSkinId(skinId))
					{
						activeChar.sendMessage("Invalid skin ID.");
						return;
					}

					// Cancel any pending revert from a previous preview click
					final ScheduledFuture<?> existing = _skinPreviewTasks.remove(activeChar.getObjectId());
					if (existing != null)
						existing.cancel(false);

					// Save the skin that was active BEFORE any preview started.
					// If a previous task was cancelled, the player's skin is already
					// the preview skin, so we must restore to 0 (no skin) on revert.
					final int restoreSkinId = (existing != null) ? 0 : activeChar.getActiveSkinId();

					// Remove current appearance to bypass client visual caching
					if (activeChar.getActiveSkinId() != 0)
					{
						activeChar.setActiveSkinId(0);
						activeChar.broadcastUserInfo();
					}

					// Apply the preview skin visually (no stat changes, no DB write)
					activeChar.setActiveSkinId(skinId);
					activeChar.broadcastUserInfo();
					activeChar.sendMessage("Previewing skin for 5 seconds...");

					// Schedule revert after 5 seconds and store the task
					final ScheduledFuture<?> task = ThreadPoolManager.getInstance().scheduleGeneral(() ->
					{
						_skinPreviewTasks.remove(activeChar.getObjectId());
						if (activeChar.isOnline())
						{
							// Remove preview appearance before restoring
							if (activeChar.getActiveSkinId() != 0)
							{
								activeChar.setActiveSkinId(0);
								activeChar.broadcastUserInfo();
							}

							activeChar.setActiveSkinId(restoreSkinId);
							activeChar.broadcastUserInfo();
							activeChar.sendMessage("Skin preview ended.");
						}
					}, 5000);
					_skinPreviewTasks.put(activeChar.getObjectId(), task);
				}
				catch (NumberFormatException e)
				{
					activeChar.sendMessage("Invalid skin ID format.");
				}
			}
			else if (_command.startsWith("weapon_try "))
			{
				try
				{
					final int skinId = Integer.parseInt(_command.substring(11).trim());

					if (!WeaponSkinTable.getInstance().hasSkin(skinId))
					{
						activeChar.sendMessage("Invalid weapon skin ID.");
						return;
					}

					final net.sf.l2j.gameserver.model.holder.WeaponSkinHolder weaponSkin = WeaponSkinTable.getInstance().getSkin(skinId);
					final int previewWeaponItemId = weaponSkin.getWeaponItemId();

					// Cancel any pending revert from a previous preview click
					final ScheduledFuture<?> existing = _weaponPreviewTasks.remove(activeChar.getObjectId());
					if (existing != null)
						existing.cancel(false);

					// Save the weapon that was active BEFORE any preview started.
					// If a previous task was cancelled, we restore to 0 (no fake weapon).
					final int restoreWeaponItemId   = (existing != null) ? 0 : activeChar.getFakeWeaponItemId();
					final int restoreWeaponObjectId = (existing != null) ? 0 : activeChar.getFakeWeaponObjectId();

					// Remove current fake weapon to bypass client visual caching
					if (activeChar.getFakeWeaponObjectId() != 0)
					{
						activeChar.setFakeWeaponObjectId(0);
						activeChar.setFakeWeaponItemId(0);
						activeChar.broadcastUserInfo();
					}

					// Apply the preview weapon visually (no stat changes, no DB write).
					// We ALWAYS use a synthetic objectId so the client sees a "new" item
					// in the slot and forces a visual update, even if a weapon is equipped.
					final int fakeObjId = activeChar.getObjectId() + skinId;

					activeChar.setFakeWeaponObjectId(fakeObjId);
					activeChar.setFakeWeaponItemId(previewWeaponItemId);
					activeChar.broadcastUserInfo();
					activeChar.sendMessage("Previewing weapon skin for 5 seconds...");

					// Schedule revert after 5 seconds and store the task
					final ScheduledFuture<?> task = ThreadPoolManager.getInstance().scheduleGeneral(() ->
					{
						_weaponPreviewTasks.remove(activeChar.getObjectId());
						if (activeChar.isOnline())
						{
							// Remove preview weapon before restoring
							if (activeChar.getFakeWeaponObjectId() != 0)
							{
								activeChar.setFakeWeaponObjectId(0);
								activeChar.setFakeWeaponItemId(0);
								activeChar.broadcastUserInfo();
							}

							activeChar.setFakeWeaponObjectId(restoreWeaponObjectId);
							activeChar.setFakeWeaponItemId(restoreWeaponItemId);
							activeChar.broadcastUserInfo();
							activeChar.sendMessage("Weapon skin preview ended.");
						}
					}, 5000);
					_weaponPreviewTasks.put(activeChar.getObjectId(), task);
				}
				catch (NumberFormatException e)
				{
					activeChar.sendMessage("Invalid weapon skin ID format.");
				}
			}
//			else if (_command.startsWith("skin_try "))
//			{
//				try
//				{
//					final int skinId = Integer.parseInt(_command.substring(9).trim());
//
//					if (!SkinTable.getInstance().getSkinId(skinId))
//					{
//						activeChar.sendMessage("Invalid skin ID.");
//						return;
//					}
//
//					// Cancel any pending revert from a previous preview click
//					final ScheduledFuture<?> existing = _skinPreviewTasks.remove(activeChar.getObjectId());
//					if (existing != null)
//						existing.cancel(false);
//
//					// Save the skin that was active BEFORE any preview started.
//					// If a previous task was cancelled, the player's skin is already
//					// the preview skin, so we must restore to 0 (no skin) on revert.
//					final int restoreSkinId = (existing != null) ? 0 : activeChar.getActiveSkinId();
//
//					// Apply the preview skin visually (no stat changes, no DB write)
//					activeChar.setActiveSkinId(skinId);
//					activeChar.broadcastUserInfo();
//					activeChar.sendMessage("Previewing skin for 5 seconds...");
//
//					// Schedule revert after 5 seconds and store the task
//					final ScheduledFuture<?> task = ThreadPoolManager.getInstance().scheduleGeneral(() ->
//					{
//						_skinPreviewTasks.remove(activeChar.getObjectId());
//						if (activeChar.isOnline())
//						{
//							activeChar.setActiveSkinId(restoreSkinId);
//							activeChar.broadcastUserInfo();
//							activeChar.sendMessage("Skin preview ended.");
//						}
//					}, 5000);
//					_skinPreviewTasks.put(activeChar.getObjectId(), task);
//				}
//				catch (NumberFormatException e)
//				{
//					activeChar.sendMessage("Invalid skin ID format.");
//				}
//			}
			else if (_command.startsWith("clan_name_change"))
			{
				try
				{
					final String newName = _command.substring(17).trim();

					if (newName.length() < 3)
					{
						activeChar.sendMessage("The clan name must have at least 3 characters.");
						return;
					}

					if (newName.length() > 16)
					{
						activeChar.sendMessage("The clan name cannot exceed 16 characters.");
						return;
					}

					if (!newName.matches("^[a-zA-Z0-9]+$"))
					{
						activeChar.sendMessage("The clan name can only contain letters and numbers.");
						return;
					}

					if (activeChar.getClan() == null)
					{
						activeChar.sendMessage("You are not in a clan.");
						return;
					}

					if (!activeChar.isClanLeader())
					{
						activeChar.sendMessage("Only the clan leader can rename the clan.");
						return;
					}

					// Check if name is already taken by another clan
					if (ClanTable.getInstance().getClanByName(newName) != null)
					{
						activeChar.sendMessage("A clan with that name already exists. Please choose another.");
						return;
					}

					// Consume the item before applying the change
					if (activeChar.destroyItemByItemId("Clan Name Change", activeChar.getNameChangeItemId(), 1, null, true))
					{
						final String oldName = activeChar.getClan().getName();
						activeChar.getClan().setName(newName);
						
						// The clan name is not updated by updateClanInDB(), so we must do it manually here.
						try (java.sql.Connection con = net.sf.l2j.L2DatabaseFactory.getInstance().getConnection())
						{
							java.sql.PreparedStatement statement = con.prepareStatement("UPDATE clan_data SET clan_name=? WHERE clan_id=?");
							statement.setString(1, newName);
							statement.setInt(2, activeChar.getClan().getClanId());
							statement.execute();
							statement.close();
						}
						catch (Exception e)
						{
							_log.warning("Exception while updating clan name in db for clanId " + activeChar.getClan().getClanId() + ": " + e);
						}

						activeChar.getClan().updateClanInDB();
						activeChar.getClan().broadcastClanStatus();
						activeChar.sendMessage("Your clan has been renamed from '" + oldName + "' to '" + newName + "'.");
					}
				}
				catch (Exception e)
				{
					activeChar.sendMessage("Fill out the field correctly.");
				}
			}
			if (_command.startsWith("auction"))
			{
				try
				{
					String[] data = _command.substring(8).split(" - ");
					int page = Integer.parseInt(data[0]);
					String search = data[1];
					showAuction(activeChar, page, search);
				}
				catch (Exception e)
				{
					showAuctionHtml(activeChar);
					activeChar.sendMessage("Invalid input. Please try again.");
					return;
				}
			}
			else if (_command.startsWith("buy"))
			{
				int auctionId = Integer.parseInt(_command.substring(4));
				AuctionHolder item = AuctionTable.getInstance().getItem(auctionId);
				
				if (item == null)
				{
					showAuctionHtml(activeChar);
					activeChar.sendMessage("Invalid choice. Please try again.");
					return;
				}
				
				if (activeChar.getInventory().getItemByItemId(item.getCostId()) == null || activeChar.getInventory().getItemByItemId(item.getCostId()).getCount() < item.getCostCount())
				{
					showAuctionHtml(activeChar);
					activeChar.sendMessage("Incorrect item count.");
					return;
				}
				
				activeChar.getInventory().destroyItemByItemId("Auction", item.getCostId(), item.getCostCount(), activeChar, null);
				
				L2PcInstance owner = L2World.getInstance().getPlayer(item.getOwnerId());
				if (owner != null && owner.isOnline())
				{
					owner.addItem("Auction", item.getCostId(), item.getCostCount(), null, true);
					owner.sendMessage("You have sold an item in the Auction Shop.");
				}
				else
				{
					addItemToOffline(item.getOwnerId(), item.getCostId(), item.getCostCount());
				}
				
				ItemInstance i = activeChar.addItem("auction", item.getItemId(), item.getCount(), activeChar, true);
				i.setEnchantLevel(item.getEnchant());
				activeChar.sendPacket(new InventoryUpdate());
				activeChar.sendMessage("You have purchased an item from the Auction Shop.");
				
				AuctionTable.getInstance().deleteItem(item);
				
				showAuctionHtml(activeChar);
			}
			else if (_command.startsWith("addpanel"))
			{
				int page = Integer.parseInt(_command.substring(9));
				
				showAddPanel(activeChar, page);
			}
			else if (_command.startsWith("additem"))
			{
				int itemId = Integer.parseInt(_command.substring(8));
				
				if (activeChar.getInventory().getItemByObjectId(itemId) == null)
				{
					showAuctionHtml(activeChar);
					activeChar.sendMessage("Invalid item. Please try again.");
					return;
				}
				
				showAddPanel2(activeChar, itemId);
			}
			else if (_command.startsWith("addit2"))
			{
				try
				{
					String[] data = _command.substring(7).split(" ");
					int itemId = Integer.parseInt(data[0]);
					String costitemtype = data[1];
					int costCount = Integer.parseInt(data[2]);
					int itemAmount = Integer.parseInt(data[3]);
					
					if (activeChar.getInventory().getItemByObjectId(itemId) == null)
					{
						showAuctionHtml(activeChar);
						activeChar.sendMessage("Invalid item. Please try again.");
						return;
					}
					if (activeChar.getInventory().getItemByObjectId(itemId).getCount() < itemAmount)
					{
						showAuctionHtml(activeChar);
						activeChar.sendMessage("Invalid item. Please try again.");
						return;
					}
					if (!activeChar.getInventory().getItemByObjectId(itemId).isTradable())
					{
						showAuctionHtml(activeChar);
						activeChar.sendMessage("Invalid item. Please try again.");
						return;
					}
					
					int costId = 0;
					if (costitemtype.equals("Gold_Coin"))
						costId = 9500;
					else if (costitemtype.equals("Event_Coin"))
						costId = 9501;
					else if (costitemtype.equals("Ticket_Donate"))
						costId = 9511;
					
					AuctionTable.getInstance().addItem(new AuctionHolder(AuctionTable.getInstance().getNextAuctionId(), activeChar.getObjectId(), activeChar.getInventory().getItemByObjectId(itemId).getItemId(), itemAmount, activeChar.getInventory().getItemByObjectId(itemId).getEnchantLevel(), costId, costCount));
					
					activeChar.destroyItem("auction", itemId, itemAmount, activeChar, true);
					activeChar.sendPacket(new InventoryUpdate());
					activeChar.sendMessage("You have added an item for sale in the Auction Shop.");
					showAuctionHtml(activeChar);
				}
				catch (Exception e)
				{
					showAuctionHtml(activeChar);
					activeChar.sendMessage("Invalid input. Please try again.");
					return;
				}
			}
			else if (_command.startsWith("myitems"))
			{
				int page = Integer.parseInt(_command.substring(8));
				showMyItems(activeChar, page);
			}
			else if (_command.startsWith("remove"))
			{
				int auctionId = Integer.parseInt(_command.substring(7));
				AuctionHolder item = AuctionTable.getInstance().getItem(auctionId);
				
				if (item == null)
				{
					showAuctionHtml(activeChar);
					activeChar.sendMessage("Invalid choice. Please try again.");
					return;
				}
				
				AuctionTable.getInstance().deleteItem(item);
				
				ItemInstance i = activeChar.addItem("auction", item.getItemId(), item.getCount(), activeChar, true);
				i.setEnchantLevel(item.getEnchant());
				activeChar.sendPacket(new InventoryUpdate());
				activeChar.sendMessage("You have removed an item from the Auction Shop.");
				showAuctionHtml(activeChar);
			} 
			else if (_command.startsWith("tele_tournament"))
			{
				if (activeChar.isOlympiadProtection())
				{
					activeChar.sendMessage("Are you participating in the Olympiad.");
					return;
				}	
				
				if(Config.TELEPORT_EFFECT)
					activeChar.broadcastPacket(new MagicSkillUse(activeChar, activeChar, Config.ID_ESCAPE_SKILL, 1, 0, 0));
				
				for (L2TeleporterInstance knownChar : activeChar.getKnownList().getKnownTypeInRadius(L2TeleporterInstance.class, 300))
				{
					if (knownChar != null)
					{
						activeChar.teleToLocation(ArenaConfig.Tournament_locx + Rnd.get(-100, 100), ArenaConfig.Tournament_locy + Rnd.get(-100, 100), ArenaConfig.Tournament_locz, 0);
						activeChar.setTournamentTeleport(true);
					}
				}
			}
			
			StringTokenizer st = new StringTokenizer(_command, " ");
			String actualCommand = st.nextToken(); // Get actual command
			
//			if (actualCommand.startsWith("RaidBossInfo"))
//			{
//				int pageId = Integer.parseInt(st.nextToken());
//				_lastPageRaid.put(activeChar.getObjectId(), pageId);
//				showRaidBossInfo(activeChar, pageId);
//			}
//			if (actualCommand.startsWith("GrandBossInfo"))
//			{
//				if (!Config.RETAIL_EVENTS_STARTED)
//				{
//					activeChar.sendMessage("Event currently unavailable, please wait!");
//					return;
//				}
//				
//				int pageId = Integer.parseInt(st.nextToken());
//				_lastPageGrand.put(activeChar.getObjectId(), pageId);
//				showGrandBossInfo(activeChar, pageId);
//			}
			if (actualCommand.startsWith("RaidBossDrop"))
			{
				int bossId = Integer.parseInt(st.nextToken());
				int pageId = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 1;
				showRaidBossDrop(activeChar, bossId, pageId);
			}
			
			if (actualCommand.equalsIgnoreCase("st_goto")) 
			{
				if (st.countTokens() <= 0)
					return;
				
				doTeleport(activeChar, Integer.parseInt(st.nextToken()));
				return;
			}
		}
		
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Bad RequestBypassToServer: ", e);
		}
	}
	
	private boolean checkallowed(L2PcInstance activeChar)
	{
	    if (activeChar == null)
	        return false;

	    if (activeChar.isAlikeDead())
	    {
	        activeChar.sendMessage(
	            "Nao e possivel teleportar enquanto estiver morto."
	        );
	        return false;
	    }

	    if (
	        activeChar.isCursedWeaponEquipped()
	        || activeChar.isInArenaEvent()
	        || OlympiadManager.getInstance().isRegistered(activeChar)
	        || activeChar.isInCombat()
	        || activeChar.getKarma() > 0
	        || activeChar.inObserverMode()
	        || CTFEvent.isPlayerParticipant(activeChar.getObjectId())
	        || DMEvent.isPlayerParticipant(activeChar.getObjectId())
	        || LMEvent.isPlayerParticipant(activeChar.getObjectId())
	        || TvTEvent.isPlayerParticipant(activeChar.getObjectId())
	        || KTBEvent.isPlayerParticipant(activeChar.getObjectId())
	    )
	    {
	        activeChar.sendMessage(
	            "Nao e possivel teleportar em combate, com karma, "
	            + "arma amaldicoada ou durante eventos/modo observador."
	        );
	        return false;
	    }

	    return true;
	}

	private static void showMyItems(L2PcInstance player, int page)
	{
		HashMap<Integer, ArrayList<AuctionHolder>> items = new HashMap<>();
		int curr = 1;
		int counter = 0;
		
		ArrayList<AuctionHolder> temp = new ArrayList<>();
		for (Map.Entry<Integer, AuctionHolder> entry : AuctionTable.getInstance().getItems().entrySet())
		{
			if (entry.getValue().getOwnerId() == player.getObjectId())
			{
				temp.add(entry.getValue());
				
				counter++;
				
				if (counter == 7)
				{
					items.put(curr, temp);
					temp = new ArrayList<>();
					curr++;
					counter = 0;
				}
			}
		}
		items.put(curr, temp);
		
		if (!items.containsKey(page))
		{
			showAuctionHtml(player);
			player.sendMessage("Invalid page. Please try again.");
			return;
		}
		
		String html = "";
		html += "<html><title>Auction Manager</title><body><center><br1>";
		html += "<table width=100%>";
		html += "<tr><td></td></tr>";
		for (AuctionHolder item : items.get(page))
		{
			String name = ItemTable.getInstance().getTemplate(item.getItemId()).getName();
			
			if (name.length() >= 19)
				name = name.substring(0, 19) + "...";
			
			html += "<tr>";
			html += "<td><img src=\"" + IconTable.getIcon(item.getItemId()) + "\" width=32 height=32 align=center></td>";
			html += "<td>Item: <font color=\"LEVEL\">" + (item.getEnchant() > 0 ? "+" + item.getEnchant() + " " + name + "</font> - <font color=\"CC2900\">" + item.getCount() : name + "</font> - <font color=\"CC2900\">" + item.getCount() +"</font>");
			html += "</font><br1>Cost: <font color=\"00FF00\">" + item.getCostCount() + " " + name + "</font>";
			html += "</td>";
			html += "<td fixwidth=71><button value=\"Remove\" action=\"bypass -h remove "+item.getAuctionId()+"\" width=65 height=19 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\">";
			html += "</td></tr>";
		}
		html += "</table><br1>";
		
		html += "<table width=265><tr>";
		
		if (items.keySet().size() > 1)
		{
			if (page > 1)
				html += "<td align=center><a action=\"bypass -h myitems " + (page-1) + "\"><- Prev</a></td>";
			
			html += "<td align=center>Page: " + page + "</td>";
			
			if (items.keySet().size() > page)
				html += "<td align=center><a action=\"bypass -h myitems " + (page+1) + "\">Next -></a></td>";
		}
		
		html += "</tr></table></center></body></html>";
		
		NpcHtmlMessage htm = new NpcHtmlMessage(0);
		htm.setHtml(html);
		player.sendPacket(htm);
	}
	
	private static void showAddPanel2(L2PcInstance player, int itemId)
	{
		ItemInstance item = player.getInventory().getItemByObjectId(itemId);
		
		String html = "";
		html += "<html><title>Auction Manager</title><body><center><br1>";
		html += "<img src=\"" + IconTable.getIcon(item.getItemId()) + "\" width=32 height=32 align=center>";
		html += "Item: " + (item.getEnchantLevel() > 0 ? " + " + item.getEnchantLevel() + " " + item.getName() : item.getName());
		
		if (item.isStackable())
		{
			html += "<br>Set amount of items to sell:";
			html += "<edit var=amm type=number width=120 height=17>";
		}
		
		html += "<br>Select price:";
		html += "<br><combobox width=120 height=17 var=ebox list=Gold_Coin;Event_Coin;Ticket_Donate;>";
		html += "<br><edit var=count type=number width=120 height=17>";
		html += "<br><button value=\"Add item\" action=\"bypass -h addit2 " + itemId + " $ebox $count " + (item.isStackable() ? "$amm" : "1") + "\" width=65 height=19 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\">";
		html += "</center></body></html>";
		
		NpcHtmlMessage htm = new NpcHtmlMessage(0);
		htm.setHtml(html);
		player.sendPacket(htm);
	}
	
	private static void showAddPanel(L2PcInstance player, int page)
	{
		HashMap<Integer, ArrayList<ItemInstance>> items = new HashMap<>();
		int curr = 1;
		int counter = 0;
		
		ArrayList<ItemInstance> temp = new ArrayList<>();
		for (ItemInstance item : player.getInventory().getItems())
		{
			if (item.getItemId() != 57 && item.isTradable() && !item.isEquipped())
			{
				temp.add(item);
				
				counter++;
				
				if (counter == 7)
				{
					items.put(curr, temp);
					temp = new ArrayList<>();
					curr++;
					counter = 0;
				}
			}
		}
		items.put(curr, temp);
		
		if (!items.containsKey(page))
		{
			showAuctionHtml(player);
			player.sendMessage("Invalid page. Please try again.");
				return;
		}
		
		String html = "";
				html += "<html><title>Auction Manager</title><body><center><br>";
				html += "Select the item you want to sell:";
				html += "<table width=100%>";
				
				for (ItemInstance item : items.get(page))
				{
					String name = item.getName();
					
					if (name.length() >= 26)
						name = name.substring(0, 26) + "...";
					
					html += "<tr>";
					html += "<td>";
					html += "<img src=\"" + IconTable.getIcon(item.getItemId()) + "\" width=32 height=32 align=center></td>";
					html += "<td><font color=\"00FF00\">" + (item.getEnchantLevel() > 0 ? " + " + item.getEnchantLevel() + " " + name : name);
					html += "</font></td>";
					html += "<td><button value=\"Select\" action=\"bypass -h additem " + item.getObjectId() + "\" width=65 height=19 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\">";
					html += "</td>";
					html += "</tr>";
				}
				html += "</table><br1>";
				
				html += "<br1>";
				
				html += "<table width=265><tr>";
				
				if (items.keySet().size() > 1)
				{
					if (page > 1)
						html += "<td align=center><a action=\"bypass -h addpanel " + (page-1) + "\"><- Prev</a></td>";
					
					html += "<td align=center>Page: " + page + "</td>";
					
					if (items.keySet().size() > page)
						html += "<td align=center><a action=\"bypass -h addpanel " + (page+1) + "\">Next -></a></td>";
				}
				
				html += "</tr></table></center></body></html>";
				
				NpcHtmlMessage htm = new NpcHtmlMessage(0);
				htm.setHtml(html);
				player.sendPacket(htm);
	}
	
	private static void addItemToOffline(int playerId, int itemId, int count)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement stm = con.prepareStatement("SELECT count FROM items WHERE owner_id=? AND item_id=?");
			stm.setInt(1, playerId);
			stm.setInt(2, itemId);
			ResultSet rset = stm.executeQuery();
			
			if (rset.next())
			{
				stm = con.prepareStatement("UPDATE items SET count=? WHERE owner_id=? AND item_id=?");
				stm.setInt(1, rset.getInt("count") + count);
				stm.setInt(2, playerId);
				stm.setInt(3, itemId);
				
				stm.execute();
			}
			else
			{
				con.prepareStatement("INSERT INTO items VALUES (?,?,?,?,?,?,?,?,?,?,?,?)");
				stm.setInt(1, playerId);
				stm.setInt(2, IdFactory.getInstance().getNextId());
				stm.setInt(3, itemId);
				stm.setInt(4, count);
				stm.setInt(5, 0);
				stm.setString(6, "INVENTORY");
				stm.setInt(7, 0);
				stm.setInt(8, 0);
				stm.setInt(9, 0);
				stm.setInt(10, 0);
				stm.setInt(11, -1);
				stm.setInt(12, 0);
				
				stm.execute();
			}
			
			rset.close();
			stm.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private static void showAuction(L2PcInstance player, int page, String search)
	{
		boolean src = !search.equals("*null*");
		
		HashMap<Integer, ArrayList<AuctionHolder>> items = new HashMap<>();
		int curr = 1;
		int counter = 0;
		
		ArrayList<AuctionHolder> temp = new ArrayList<>();
		for (Map.Entry<Integer, AuctionHolder> entry : AuctionTable.getInstance().getItems().entrySet())
		{
			if (entry.getValue().getOwnerId() != player.getObjectId() && (!src || (src && ItemTable.getInstance().getTemplate(entry.getValue().getItemId()).getName().contains(search))))
			{
				temp.add(entry.getValue());
				
				counter++;
				
				if (counter == 4)
				{
					items.put(curr, temp);
					temp = new ArrayList<>();
					curr++;
					counter = 0;
				}
			}
		}
		items.put(curr, temp);
		
		if (!items.containsKey(page))
		{
			showAuctionHtml(player);
			player.sendMessage("Invalid page. Please try again.");
				return;
		}
		
		String html = "<html><title>Auction Manager</title><body><center><br>";
				html += "<font color=\"LEVEL\">Looking for a specific item?<br1></font>";
				html += "<img src=\"L2UI.SquareGray\" width=230 height=1><br>";
				html += "<edit var=srch width=150 height=13><br>";
				html += "<img src=\"L2UI.SquareGray\" width=230 height=1><br>";
				html += "<button value=\"Search\" action=\"bypass -h auction 1 - $srch\" width=80 height=13 back=\"buttons_bs.bs_64x15_1\" fore=\"buttons_bs.bs_64x15_2\">";

				html += "<br1><table width=100%>";
				
				html += "<tr><td></td></tr>";
				for (AuctionHolder item : items.get(page))
				{
					String name = ItemTable.getInstance().getTemplate(item.getCostId()).getName();
					
					if (name.length() >= 19)
						name = name.substring(0, 19) + "...";
					
					html += "<tr>";
					html += "<td><img src=\"" + IconTable.getIcon(item.getItemId()) + "\" width=32 height=32 align=center></td>";
					//html += "<td>Item: " + (item.getEnchant() > 0 ? " + " + item.getEnchant() + " " + name + " - " + item.getCount() : name + " - " + item.getCount());
					//html += "<br1>Cost: " + item.getCostCount() + " " + ItemTable.getInstance().getTemplate(item.getCostId()).getName();
					
					html += "<td>Item: <font color=\"LEVEL\">" + (item.getEnchant() > 0 ? "+" + item.getEnchant() + " " + name + "</font> - <font color=\"CC2900\">" + item.getCount() : name + "</font> - <font color=\"CC2900\">" + item.getCount() +"</font>");
					html += "</font><br1>Cost: <font color=\"00FF00\">" + item.getCostCount() + " " + name + "</font>";
					
					html += "</td>";
					html += "<td fixwidth=71><button value=\"Buy\" action=\"bypass -h buy " + item.getAuctionId() + "\" width=65 height=19 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\">";
					html += "</td></tr>";
				}
				html += "</table><br1>";
				
				
				html += "<table width=265><tr>";
				
				if (items.keySet().size() > 1)
				{
					if (page > 1)
						html += "<td align=center><a action=\"bypass -h auction " + (page-1) + " - " + search + "\"><- Prev</a></td>";
					
					html += "<td align=center>Page: " + page + "</td>";
					
					if (items.keySet().size() > page)
						html += "<td align=center><a action=\"bypass -h auction " + (page+1) + " - " + search + "\">Next -></a></td>";
				}
				
				html += "</tr></table></center></body></html>";
				/*
        html += "Page: "+page;
        html += "<br1>";
       
        if (items.keySet().size() > 1)
        {
            if (page > 1)
                html += "<a action=\"bypass -h auction " + (page-1) + " - " + search + "\"><- Prev</a>";
           
            if (items.keySet().size() > page)
                html += "<a action=\"bypass -h auction " + (page+1) + " - " + search + "\">Next -></a>";
        }
       
        html += "</center></body></html>";
				 */
				
				NpcHtmlMessage htm = new NpcHtmlMessage(0);
				htm.setHtml(html);
				player.sendPacket(htm);
	}
	public static void showAuctionHtml(L2PcInstance activeChar)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile("data/html/mods/AuctionerManager.htm");
		activeChar.sendPacket(html);
		activeChar.sendPacket(ActionFailed.STATIC_PACKET);
	}

	private static void doTeleport(L2PcInstance player, int val) 
	{
		L2TeleportLocation list = TeleportLocationTable.getInstance().getTemplate(val);
		if (list != null)
		{
			if (player.isCursedWeaponEquipped() || player.isInArenaEvent() || OlympiadManager.getInstance().isRegistered(player) || player.isInCombat() || player.getKarma() > 0 || player.inObserverMode() || CTFEvent.isPlayerParticipant(player.getObjectId()) || DMEvent.isPlayerParticipant(player.getObjectId()) || LMEvent.isPlayerParticipant(player.getObjectId()) || TvTEvent.isPlayerParticipant(player.getObjectId()) || KTBEvent.isPlayerParticipant(player.getObjectId()))
			{
				player.sendMessage("You can not teleport right now.");
				return;
			}
			
			player.teleToLocation(list.getLocX(), list.getLocY(), list.getLocZ(), 20);
		} 
		else
			_log.warning("No teleport destination with id:" + val);
		
		if(Config.TELEPORT_EFFECT)
			player.broadcastPacket(new MagicSkillUse(player, player, Config.ID_ESCAPE_SKILL, 1, 0, 0));
		
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	public static void UpdateLastIP(L2PcInstance player ,String user)
	{
		String address = player.getClient().getConnection().getInetAddress().getHostAddress();
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("UPDATE accounts SET lastIP=? WHERE login=?");
			statement.setString(1, address);
			statement.setString(2, user);
			statement.execute();
			statement.close();
			con.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "UpdateMyIP: Error while saving new IP: ", e);
		}
	}
	
	private static void playerHelp(L2PcInstance activeChar, String path)
	{
		if (path.indexOf("..") != -1)
			return;
		
		final StringTokenizer st = new StringTokenizer(path);
		final String[] cmd = st.nextToken().split("#");
		
		NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile("data/html/help/" + cmd[0]);
		if (cmd.length > 1)
			html.setItemId(Integer.parseInt(cmd[1]));
		html.disableValidation();
		activeChar.sendPacket(html);
	}
	
	private static void playerTeleporter(L2PcInstance activeChar, String path)
	{
		if (path.indexOf("..") != -1)
			return;
		
		final StringTokenizer st = new StringTokenizer(path);
		final String[] cmd = st.nextToken().split("#");
		
		NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile("data/html/mods/" + cmd[0]);
		if (cmd.length > 1)
			html.setItemId(Integer.parseInt(cmd[1]));
		html.disableValidation();
		activeChar.sendPacket(html);
	}
	
	private static void ClassChangeCoin(L2PcInstance player, String command)
	{
		String nameclasse = player.getTemplate().getClassName();
		
		String type = command;
		if (type.equals("---SELECT---"))
		{
			NpcHtmlMessage html = new NpcHtmlMessage(0);
			html.setFile("data/html/mods/class_changer/Class.htm");
			player.sendPacket(html);
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
		if (type.equals("Duelist"))
		{		
			if (player.getInventory().getInventoryItemCount(9590, 0) >= 1)
			{
				if (player.getClassId().getId() == 88)
				{
					player.sendMessage("Your class is already " + nameclasse + ".");				
					return;
				}
				
				RemoverSkills(player);
				
				player.setClassId(88);
				if (!player.isSubClassActive())
					player.setBaseClass(88);
				
				Finish(player);
			}
			else
			{				
				player.sendMessage("You dont have class card item!");
				return;
			}
		}
		if (type.equals("DreadNought"))
		{
			if (player.getInventory().getInventoryItemCount(9590, 0) >= 1)
			{
				if (player.getClassId().getId() == 89)
				{
					player.sendMessage("Your class is already "+nameclasse+".");				
					return;
				}
				
				RemoverSkills(player);
				
				player.setClassId(89);
				if (!player.isSubClassActive())
					player.setBaseClass(89);
				Finish(player);
			}
			else
			{				
				player.sendMessage("You dont have class card item!");
				return;
			}
		}
		if (type.equals("Phoenix_Knight"))
		{
			if (player.getInventory().getInventoryItemCount(9590, 0) >= 1)
			{
				if (player.getClassId().getId() == 90)
				{
					player.sendMessage("Your class is already "+nameclasse+".");				
					return;
				}
				
				RemoverSkills(player);
				
				player.setClassId(90);
				if (!player.isSubClassActive())
					player.setBaseClass(90);
				Finish(player);
			}
			else
			{				
				player.sendMessage("You dont have class card item!");
				return;
			}
		}
		if (type.equals("Hell_Knight"))
		{
			if (player.getInventory().getInventoryItemCount(9590, 0) >= 1)
			{
				if (player.getClassId().getId() == 91)
				{
					player.sendMessage("Your class is already "+nameclasse+".");				
					return;
				}
				
				RemoverSkills(player);
				
				player.setClassId(91);
				if (!player.isSubClassActive())
					player.setBaseClass(91);
				Finish(player);
			}
			else
			{				
				player.sendMessage("You dont have class card item!");
				return;
			}
		}
		if (type.equals("Sagittarius"))
		{
			if (player.getInventory().getInventoryItemCount(9590, 0) >= 1)
			{
				if (player.getClassId().getId() == 92)
				{
					player.sendMessage("Your class is already "+nameclasse+".");				
					return;
				}
				
				RemoverSkills(player);
				
				player.setClassId(92);
				if (!player.isSubClassActive())
					player.setBaseClass(92);
				Finish(player);
			}
			else
			{				
				player.sendMessage("You dont have class card item!");
				return;
			}
		}
		if (type.equals("Adventurer"))
		{
			if (player.getInventory().getInventoryItemCount(9590, 0) >= 1)
			{
				if (player.getClassId().getId() == 93)
				{
					player.sendMessage("Your class is already "+nameclasse+".");				
					return;
				}
				
				RemoverSkills(player);
				
				player.setClassId(93);
				if (!player.isSubClassActive())
					player.setBaseClass(93);
				Finish(player);
			}
			else
			{				
				player.sendMessage("You dont have class card item!");
				return;
			}
		}
		if (type.equals("Archmage"))
		{
			if (player.getInventory().getInventoryItemCount(9590, 0) >= 1)
			{
				if (player.getClassId().getId() == 94)
				{
					player.sendMessage("Your class is already "+nameclasse+".");				
					return;
				}
				
				RemoverSkills(player);
				
				player.setClassId(94);
				if (!player.isSubClassActive())
					player.setBaseClass(94);
				Finish(player);
			}
			else
			{				
				player.sendMessage("You dont have class card item!");
				return;
			}
		}
		if (type.equals("Soultaker"))
		{
			if (player.getInventory().getInventoryItemCount(9590, 0) >= 1)
			{
				if (player.getClassId().getId() == 95)
				{
					player.sendMessage("Your class is already "+nameclasse+".");				
					return;
				}
				
				RemoverSkills(player);
				
				player.setClassId(95);
				if (!player.isSubClassActive())
					player.setBaseClass(95);
				Finish(player);
			}
			else
			{				
				player.sendMessage("You dont have class card item!");
				return;
			}
		}
		if (type.equals("Arcana_Lord"))
		{
			if (player.getInventory().getInventoryItemCount(9590, 0) >= 1)
			{
				if (player.getClassId().getId() == 96)
				{
					player.sendMessage("Your class is already "+nameclasse+".");				
					return;
				}
				
				RemoverSkills(player);
				
				player.setClassId(96);
				if (!player.isSubClassActive())
					player.setBaseClass(96);
				Finish(player);
			}
			else
			{				
				player.sendMessage("You dont have class card item!");
				return;
			}
		}
		if (type.equals("Cardinal"))
		{
			if (player.getInventory().getInventoryItemCount(9590, 0) >= 1)
			{
				if (player.getClassId().getId() == 97)
				{
					player.sendMessage("Your class is already "+nameclasse+".");				
					return;
				}
				
				RemoverSkills(player);
				
				player.setClassId(97);
				if (!player.isSubClassActive())
					player.setBaseClass(97);
				Finish(player);
			}
			else
			{				
				player.sendMessage("You dont have class card item!");
				return;
			}
		}
		if (type.equals("Hierophant"))
		{
			if (player.getInventory().getInventoryItemCount(9590, 0) >= 1)
			{
				if (player.getClassId().getId() == 98)
				{
					player.sendMessage("Your class is already "+nameclasse+".");				
					return;
				}
				
				RemoverSkills(player);
				
				player.setClassId(98);
				if (!player.isSubClassActive())
					player.setBaseClass(98);
				Finish(player);
			}
			else
			{				
				player.sendMessage("You dont have class card item!");
				return;
			}
		}
		if (type.equals("Eva_Templar"))
		{
			if (player.getInventory().getInventoryItemCount(9590, 0) >= 1)
			{
				if (player.getClassId().getId() == 99)
				{
					player.sendMessage("Your class is already "+nameclasse+".");				
					return;
				}
				
				RemoverSkills(player);
				
				player.setClassId(99);
				if (!player.isSubClassActive())
					player.setBaseClass(99);
				Finish(player);
			}
			else
			{				
				player.sendMessage("You dont have class card item!");
				return;
			}
		}
		if (type.equals("Sword_Muse"))
		{
			if (player.getInventory().getInventoryItemCount(9590, 0) >= 1)
			{
				if (player.getClassId().getId() == 100)
				{
					player.sendMessage("Your class is already "+nameclasse+".");				
					return;
				}
				
				RemoverSkills(player);
				
				player.setClassId(100);
				if (!player.isSubClassActive())
					player.setBaseClass(100);
				Finish(player);
			}
			else
			{				
				player.sendMessage("You dont have class card item!");
				return;
			}
		}
		if (type.equals("Wind_Rider"))
		{
			if (player.getInventory().getInventoryItemCount(9590, 0) >= 1)
			{
				if (player.getClassId().getId() == 101)
				{
					player.sendMessage("Your class is already "+nameclasse+".");				
					return;
				}
				
				RemoverSkills(player);
				
				player.setClassId(101);
				if (!player.isSubClassActive())
					player.setBaseClass(101);
				Finish(player);
			}
			else
			{				
				player.sendMessage("You dont have class card item!");
				return;
			}
		}
		if (type.equals("Moonli_Sentinel"))
		{
			if (player.getInventory().getInventoryItemCount(9590, 0) >= 1)
			{
				if (player.getClassId().getId() == 102)
				{
					player.sendMessage("Your class is already "+nameclasse+".");				
					return;
				}
				
				RemoverSkills(player);
				
				player.setClassId(102);
				if (!player.isSubClassActive())
					player.setBaseClass(102);
				Finish(player);
			}
			else
			{				
				player.sendMessage("You dont have class card item!");
				return;
			}
		}
		if (type.equals("Mystic_Muse"))
		{
			if (player.getInventory().getInventoryItemCount(9590, 0) >= 1)
			{
				if (player.getClassId().getId() == 103)
				{
					player.sendMessage("Your class is already "+nameclasse+".");				
					return;
				}
				
				RemoverSkills(player);
				
				player.setClassId(103);
				if (!player.isSubClassActive())
					player.setBaseClass(103);
				Finish(player);
			}
			else
			{				
				player.sendMessage("You dont have class card item!");
				return;
			}
		}
		if (type.equals("Elemental_Master"))
		{
			if (player.getInventory().getInventoryItemCount(9590, 0) >= 1)
			{
				if (player.getClassId().getId() == 104)
				{
					player.sendMessage("Your class is already "+nameclasse+".");				
					return;
				}
				
				RemoverSkills(player);
				
				player.setClassId(104);
				if (!player.isSubClassActive())
					player.setBaseClass(104);
				Finish(player);
			}
			else
			{				
				player.sendMessage("You dont have class card item!");
				return;
			}
		}
		if (type.equals("Eva_Saint"))
		{
			if (player.getInventory().getInventoryItemCount(9590, 0) >= 1)
			{
				if (player.getClassId().getId() == 105)
				{
					player.sendMessage("Your class is already "+nameclasse+".");				
					return;
				}
				
				RemoverSkills(player);
				
				player.setClassId(105);
				if (!player.isSubClassActive())
					player.setBaseClass(105);
				Finish(player);
			}
			else
			{				
				player.sendMessage("You dont have class card item!");
				return;
			}
		}
		if (type.equals("Shillien_Templar"))
		{
			if (player.getInventory().getInventoryItemCount(9590, 0) >= 1)
			{
				if (player.getClassId().getId() == 106)
				{
					player.sendMessage("Your class is already "+nameclasse+".");				
					return;
				}
				
				RemoverSkills(player);
				
				player.setClassId(106);
				if (!player.isSubClassActive())
					player.setBaseClass(106);
				Finish(player);
			}
			else
			{				
				player.sendMessage("You dont have class card item!");
				return;
			}
		}
		if (type.equals("Spectral_Dancer"))
		{
			if (player.getInventory().getInventoryItemCount(9590, 0) >= 1)
			{
				if (player.getClassId().getId() == 107)
				{
					player.sendMessage("Your class is already "+nameclasse+".");				
					return;
				}
				
				RemoverSkills(player);
				
				player.setClassId(107);
				if (!player.isSubClassActive())
					player.setBaseClass(107);
				Finish(player);
			}
			else
			{				
				player.sendMessage("You dont have class card item!");
				return;
			}
		}
		if (type.equals("Ghost_Hunter"))
		{
			if (player.getInventory().getInventoryItemCount(9590, 0) >= 1)
			{
				if (player.getClassId().getId() == 108)
				{
					player.sendMessage("Your class is already "+nameclasse+".");				
					return;
				}
				
				RemoverSkills(player);
				
				player.setClassId(108);
				if (!player.isSubClassActive())
					player.setBaseClass(108);
				Finish(player);
			}
			else
			{				
				player.sendMessage("You dont have class card item!");
				return;
			}
		}
		if (type.equals("Ghost_Sentinel"))
		{
			if (player.getInventory().getInventoryItemCount(9590, 0) >= 1)
			{
				if (player.getClassId().getId() == 109)
				{
					player.sendMessage("Your class is already "+nameclasse+".");				
					return;
				}
				
				RemoverSkills(player);
				
				player.setClassId(109);
				if (!player.isSubClassActive())
					player.setBaseClass(109);
				Finish(player);
			}
			else
			{				
				player.sendMessage("You dont have class card item!");
				return;
			}
		}
		if (type.equals("Storm_Screamer"))
		{
			if (player.getInventory().getInventoryItemCount(9590, 0) >= 1)
			{
				if (player.getClassId().getId() == 110)
				{
					player.sendMessage("Your class is already "+nameclasse+".");				
					return;
				}
				
				RemoverSkills(player);
				
				player.setClassId(110);
				if (!player.isSubClassActive())
					player.setBaseClass(110);
				Finish(player);
			}
			else
			{				
				player.sendMessage("You dont have class card item!");
				return;
			}
		}
		if (type.equals("Spectral_Master"))
		{
			if (player.getInventory().getInventoryItemCount(9590, 0) >= 1)
			{
				if (player.getClassId().getId() == 111)
				{
					player.sendMessage("Your class is already "+nameclasse+".");				
					return;
				}
				
				RemoverSkills(player);
				
				player.setClassId(111);
				if (!player.isSubClassActive())
					player.setBaseClass(111);
				Finish(player);
			}
			else
			{				
				player.sendMessage("You dont have class card item!");
				return;
			}
		}
		if (type.equals("Shillen_Saint"))
		{
			if (player.getInventory().getInventoryItemCount(9590, 0) >= 1)
			{
				if (player.getClassId().getId() == 112)
				{
					player.sendMessage("Your class is already "+nameclasse+".");				
					return;
				}
				
				RemoverSkills(player);
				
				player.setClassId(112);
				if (!player.isSubClassActive())
					player.setBaseClass(112);
				Finish(player);
			}
			else
			{				
				player.sendMessage("You dont have class card item!");
				return;
			}
		}
		if (type.equals("Titan"))
		{
			if (player.getInventory().getInventoryItemCount(9590, 0) >= 1)
			{
				if (player.getClassId().getId() == 113)
				{
					player.sendMessage("Your class is already "+nameclasse+".");				
					return;
				}
				
				RemoverSkills(player);
				
				player.setClassId(113);
				if (!player.isSubClassActive())
					player.setBaseClass(113);
				Finish(player);
			}
			else
			{				
				player.sendMessage("You dont have class card item!");
				return;
			}
		}
		if (type.equals("Grand_Khauatari"))
		{
			if (player.getInventory().getInventoryItemCount(9590, 0) >= 1)
			{
				if (player.getClassId().getId() == 114)
				{
					player.sendMessage("Your class is already "+nameclasse+".");				
					return;
				}
				
				RemoverSkills(player);
				
				player.setClassId(114);
				if (!player.isSubClassActive())
					player.setBaseClass(114);
				Finish(player);
			}
			else
			{				
				player.sendMessage("You dont have class card item!");
				return;
			}
		}
		if (type.equals("Dominator"))
		{
			if (player.getInventory().getInventoryItemCount(9590, 0) >= 1)
			{
				if (player.getClassId().getId() == 115)
				{
					player.sendMessage("Your class is already "+nameclasse+".");				
					return;
				}
				
				RemoverSkills(player);
				
				player.setClassId(115);
				if (!player.isSubClassActive())
					player.setBaseClass(115);
				Finish(player);
			}
			else
			{				
				player.sendMessage("You dont have class card item!");
				return;
			}
		}
		if (type.equals("Doomcryer"))
		{
			if (player.getInventory().getInventoryItemCount(9590, 0) >= 1)
			{
				if (player.getClassId().getId() == 116)
				{
					player.sendMessage("Your class is already "+nameclasse+".");				
					return;
				}
				
				RemoverSkills(player);
				
				player.setClassId(116);
				if (!player.isSubClassActive())
					player.setBaseClass(116);
				Finish(player);
			}
			else
			{				
				player.sendMessage("You dont have class card item!");
				return;
			}
		}
		if (type.equals("Fortune_Seeker"))
		{
			if (player.getInventory().getInventoryItemCount(9590, 0) >= 1)
			{
				if (player.getClassId().getId() == 117)
				{
					player.sendMessage("Your class is already "+nameclasse+".");				
					return;
				}
				
				RemoverSkills(player);
				
				player.setClassId(117);
				if (!player.isSubClassActive())
					player.setBaseClass(117);
				Finish(player);
			}
			else
			{				
				player.sendMessage("You dont have class card item!");
				return;
			}
		}
		if (type.equals("Maestro"))
		{
			if (player.getInventory().getInventoryItemCount(9590, 0) >= 1)
			{
				if (player.getClassId().getId() == 118)
				{
					player.sendMessage("Your class is already "+nameclasse+".");				
					return;
				}
				
				RemoverSkills(player);
				
				player.setClassId(118);
				if (!player.isSubClassActive())
					player.setBaseClass(118);
				Finish(player);
			}
			else
			{				
				player.sendMessage("You dont have class card item!");
				return;
			}
		}
	}
	
	private static void RemoverSkills(L2PcInstance activeChar)
	{
		if (activeChar.isSecondaryAutoFarm() || AutofarmSecondaryManager.INSTANCE.isAutofarming(activeChar))
			AutofarmSecondaryManager.INSTANCE.stopFarm(activeChar);
		activeChar.clearSecondaryAutoFarmSkills();
		activeChar.abortCast();
		activeChar.abortAttack();
		activeChar.setTarget(null);
		
		L2Skill[] skills = activeChar.getAllSkills();
		
		for (L2Skill skill : skills)
			activeChar.removeSkill(skill);
		
		activeChar.destroyItemByItemId("Classe Change", activeChar.getClassChangeItemId(), 1, null, true);
	}
	
	private static void Finish(L2PcInstance activeChar)
	{
		String newclass = activeChar.getTemplate().getClassName();
		
		DeleteHenna(activeChar, 0);
		DeleteHero(activeChar);
		
		activeChar.sendMessage(activeChar.getName() + " is now a " + newclass + ".");
		activeChar.refreshOverloaded();
		activeChar.store();
		activeChar.broadcastUserInfo();
		activeChar.sendSkillList();
		activeChar.sendPacket(new PlaySound("ItemSound.quest_finish"));
		
		if (activeChar.isNoble())
		{
			StatsSet playerStat = Olympiad.getNobleStats(activeChar.getObjectId());
			if (!(playerStat == null))
			{
				updateClasse(activeChar);
				DeleteHero(activeChar);
				activeChar.sendMessage("You now has " + Olympiad.getInstance().getNoblePoints(activeChar.getObjectId()) + " Olympiad points.");
			}
		}
		activeChar.sendMessage("You will be disconnected for security reasons.");
		waitSecs(5);
		
		activeChar.getClient().closeNow();
	}
	
	public static void updateClasse(L2PcInstance player)
	{
		if (player == null)
			return;
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement stmt = con.prepareStatement(INSERT_DATA);
			
			stmt.setInt(1, player.getObjectId());
			stmt.setInt(2, player.getClassId().getId());
			stmt.setInt(3, 18);
			stmt.execute();
			stmt.close();
			con.close();
		}
		catch (Exception e)
		{
			_log.warning("Class Card: Could not clear char Olympiad Points: " + e);
		}
	}
	
	public static void DeleteHero(L2PcInstance player)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement(DELETE_CHAR_HERO);
			statement.setInt(1, player.getObjectId());
			statement.execute();
			statement.close();
			con.close();
		}
		catch (Exception e)
		{
			_log.warning("could not clear char Hero: " + e);
		}
	}
	
	public static void DeleteHenna(L2PcInstance player, int classIndex)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			// Remove all henna info stored for this sub-class.
			PreparedStatement statement = con.prepareStatement(DELETE_CHAR_HENNAS);
			statement.setInt(1, player.getObjectId());
			statement.setInt(2, classIndex);
			statement.execute();
			statement.close();
			con.close();
		}
		catch (Exception e)
		{
			_log.warning("could not clear char Hero: " + e);
		}
	}
	
	// Updates That Will be Executed by MySQL
	// ----------------------------------------
	static String INSERT_DATA = "REPLACE INTO olympiad_nobles (char_id, class_id, olympiad_points) VALUES (?,?,?)";
	static String OLYMPIAD_UPDATE = "UPDATE olympiad_nobles SET class_id=?, olympiad_points=?, competitions_done=?, competitions_won=?, competitions_lost=?, competitions_drawn=? WHERE char_Id=?";
	static String DELETE_CHAR_HERO = "DELETE FROM heroes WHERE char_id=?";	
	static String DELETE_CHAR_HENNAS = "DELETE FROM character_hennas WHERE char_obj_id=? AND class_index=?";
	
	public static void waitSecs(int i)
	{
		try
		{
			Thread.sleep(i * 1000);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
	}
	
//	private static void showRaidBossInfo(L2PcInstance player, int pageId)
//	{
//		List<Integer> infos = new ArrayList<>();
//		infos.addAll(Config.LIST_RAID_BOSS_IDS);
//		
//		final int limit = Config.RAID_BOSS_INFO_PAGE_LIMIT;
//		final int max = infos.size() / limit + (infos.size() % limit == 0 ? 0 : 1);
//		infos = infos.subList((pageId - 1) * limit, Math.min(pageId * limit, infos.size()));
//		
//		final StringBuilder sb = new StringBuilder();
//		sb.append("<html>");
//		sb.append("<title>Raid Manager</title>");
//		sb.append("<body>");
//		sb.append("<center>");
//		sb.append("<br1>");
//		sb.append("<img src=\"l2ui_ch3.herotower_deco\" width=\"256\" height=\"32\">");
//		sb.append("<br>");
//		sb.append("Check the status and drops of the main bosses.");
//		sb.append("<br1>");
//		sb.append("<img src=\"L2UI.SquareWhite\" width=\"230\" height=\"1\">");
//		sb.append("<br>");
//		sb.append("<table width=\"275\">");
//		
//		for (int bossId : infos)
//		{
//			final NpcTemplate template = NpcTable.getInstance().getTemplate(bossId);
//			if (template == null)
//				continue;
//			
//			String bossName = template.getName();
//			if (bossName.length() > 23)
//				bossName = bossName.substring(0, 23) + "...";
//			
//			final long respawnTime = RaidBossInfoManager.getInstance().getRaidBossRespawnTime(bossId);
//			if (respawnTime <= System.currentTimeMillis())
//			{
//				sb.append("<tr>");
//				sb.append("<td width=\"150\" align=\"left\"><font color=\"0FE4EE\"><a action=\"bypass -h RaidBossDrop " + bossId + "\">" + bossName + "</a></font></td>");
//				sb.append("<td width=\"110\" align=\"right\"><font color=\"00FF00\">Alive</font></td>");
//				sb.append("<td width=\"5\" align=\"right\"><img src=\"panel.online\" width=\"16\" height=\"16\"></td>");
//				sb.append("</tr>");
//			}
//			else
//			{
//				sb.append("<tr>");
//				sb.append("<td width=\"150\" align=\"left\"><font color=\"84857E\"><a action=\"bypass -h RaidBossDrop " + bossId + "\">" + bossName + "</a></font></td>");
//				sb.append("<td width=\"110\" align=\"right\"><font color=\"FF0000\">Dead</font> " + new SimpleDateFormat(Config.RAID_BOSS_DATE_FORMAT).format(new Date(respawnTime)) + "</td>");
//				sb.append("<td width=\"5\" align=\"right\"><img src=\"panel.offline\" width=\"16\" height=\"16\"></td>");
//				sb.append("</tr>");
//			}
//		}
//		
//		sb.append("</table>");
//		sb.append("<br>");
//		sb.append("<img src=\"L2UI.SquareWhite\" width=\"230\" height=\"1\">");
//		sb.append("<br>");
//		sb.append("<table width=\"224\" cellspacing=\"2\">");
//		sb.append("<tr>");
//		
//		for (int x = 0; x < max; x++)
//		{
//			final int pageNr = x + 1;
//			if (pageId == pageNr)
//				sb.append("<td align=\"center\">" + pageNr + "</td>");
//			else
//				sb.append("<td align=\"center\"><a action=\"bypass -h RaidBossInfo " + pageNr + "\">" + pageNr + "</a></td>");
//		}
//		
//		sb.append("</tr>");
//		sb.append("</table>");
//		sb.append("<br>");
//		sb.append("<table width=\"160\" cellspacing=\"2\">");
//		sb.append("<tr>");
//		sb.append("<td width=\"160\" align=\"center\"><a action=\"bypass voiced_menu\">Back</a></td>");
//		sb.append("</tr>");
//		sb.append("</table>");
//		sb.append("</center>");
//		sb.append("</body>");
//		sb.append("</html>");
//		
//		NpcHtmlMessage html = new NpcHtmlMessage(0);
//		html.setHtml(sb.toString());
//		player.sendPacket(html);
//		player.sendPacket(ActionFailed.STATIC_PACKET);
//	}
//	
//	private static void showGrandBossInfo(L2PcInstance player, int pageId)
//	{
//		List<Integer> infos = new ArrayList<>();
//		infos.addAll(Config.LIST_GRAND_BOSS_IDS);
//		
//		final int limit = Config.RAID_BOSS_INFO_PAGE_LIMIT;
//		final int max = infos.size() / limit + (infos.size() % limit == 0 ? 0 : 1);
//		infos = infos.subList((pageId - 1) * limit, Math.min(pageId * limit, infos.size()));
//		
//		final StringBuilder sb = new StringBuilder();
//		sb.append("<html>");
//		sb.append("<title>Raid Manager</title>");
//		sb.append("<body>");
//		sb.append("<center>");
//		sb.append("<br><br>");
//		sb.append("<img src=\"l2ui_ch3.herotower_deco\" width=\"256\" height=\"32\">");
//		sb.append("<br>");
//		sb.append("Check the status and drops of the main bosses.");
//		sb.append("<br1>");
//		sb.append("<img src=\"L2UI.SquareWhite\" width=\"230\" height=\"1\">");
//		sb.append("<br>");
//		sb.append("<table width=\"275\">");
//		
//		for (int bossId : infos)
//		{
//			final NpcTemplate template = NpcTable.getInstance().getTemplate(bossId);
//			if (template == null)
//				continue;
//			
//			String bossName = template.getName();
//			if (bossName.length() > 23)
//				bossName = bossName.substring(0, 23) + "...";
//			
//			final long respawnTime = RaidBossInfoManager.getInstance().getRaidBossRespawnTime(bossId);
//			if (respawnTime <= System.currentTimeMillis())
//			{
//				sb.append("<tr>");
//				sb.append("<td width=\"150\" align=\"left\"><font color=\"0FE4EE\"><a action=\"bypass -h RaidBossDrop " + bossId + "\">" + bossName + "</a></font></td>");
//				sb.append("<td width=\"110\" align=\"right\"><font color=\"9CC300\">Alive</font></td>");
//				sb.append("<td width=\"5\" align=\"right\"><img src=\"panel.online\" width=\"16\" height=\"16\"></td>");
//				sb.append("</tr>");
//			}
//			else
//			{
//				sb.append("<tr>");
//				sb.append("<td width=\"150\" align=\"left\"><font color=\"84857E\"><a action=\"bypass -h RaidBossDrop " + bossId + "\">" + bossName + "</a></font></td>");
//				sb.append("<td width=\"110\" align=\"right\"><font color=\"FB5858\">Dead</font> " + new SimpleDateFormat(Config.RAID_BOSS_DATE_FORMAT).format(new Date(respawnTime)) + "</td>");
//				sb.append("<td width=\"5\" align=\"right\"><img src=\"panel.offline\" width=\"16\" height=\"16\"></td>");
//				sb.append("</tr>");
//			}
//		}
//		
//		sb.append("</table>");
//		sb.append("<br>");
//		sb.append("<img src=\"L2UI.SquareWhite\" width=\"230\" height=\"1\">");
//		sb.append("<br>");
//		sb.append("<table width=\"224\" cellspacing=\"2\">");
//		sb.append("<tr>");
//		
//		for (int x = 0; x < max; x++)
//		{
//			final int pageNr = x + 1;
//			if (pageId == pageNr)
//				sb.append("<td align=\"center\">" + pageNr + "</td>");
//			else
//				sb.append("<td align=\"center\"><a action=\"bypass -h GrandBossInfo " + pageNr + "\">" + pageNr + "</a></td>");
//		}
//		
//		sb.append("</tr>");
//		sb.append("</table>");
//		sb.append("<br>");
//		sb.append("<table width=\"160\" cellspacing=\"2\">");
//		sb.append("<tr>");
//		sb.append("<td width=\"160\" align=\"center\"><a action=\"bypass voiced_menu\">Back</a></td>");
//		sb.append("</tr>");
//		sb.append("</table>");
//		sb.append("<br>");
//		sb.append("</center>");
//		sb.append("</body>");
//		sb.append("</html>");
//		
//		NpcHtmlMessage html = new NpcHtmlMessage(0);
//		html.setHtml(sb.toString());
//		player.sendPacket(html);
//		player.sendPacket(ActionFailed.STATIC_PACKET);
//	}
//	
	private static void showRaidBossDrop(L2PcInstance player, int bossId, int pageId)
	{
		final NpcTemplate template = NpcTable.getInstance().getTemplate(bossId);
		if (template == null)
			return;
		
		final StringBuilder sb = new StringBuilder(2000);
		sb.append("<html><title>Drop List </title><body><center>");
		sb.append("<img src=l2ui.squaregray width=260 height=1><img height=1><img src=l2ui.squaregray width=294 height=1>");
		sb.append("<table width=300 bgcolor=000000>");
		sb.append("<tr><td align=center width=300>" + template.getName() + "</td></tr></table>");
		sb.append("<img src=l2ui.squaregray width=294 height=1><img height=1><img src=l2ui.squaregray width=260 height=1>");
		sb.append("<img height=5>");
		sb.append("<img src=l2ui.squaregray width=260 height=1><img height=1><img src=l2ui.squaregray width=294 height=1>");
		sb.append("<table cellspacing=0 cellpadding=0><tr>");
		sb.append("<td><button width=16 height=20 back=L2UI_ch3.FrameBackLeft fore=L2UI_ch3.FrameBackLeft></td>");
		sb.append("<td><button width=6 height=20 back=L2UI_ch3.FrameBackMid fore=L2UI_ch3.FrameBackMid></td>");
		sb.append("<td><button width=16 height=20 back=L2UI_CH3.FrameBackRight fore=L2UI_CH3.FrameBackRight></td>");
		sb.append("<td><button width=16 height=20 back=L2UI_ch3.FrameBackLeft fore=L2UI_ch3.FrameBackLeft></td>");
		sb.append("<td><button value=\"[ Drop List ]\" width=188 height=20 back=L2UI_ch3.FrameBackMid fore=L2UI_ch3.FrameBackMid></td>");
		sb.append("<td><button width=16 height=20 back=L2UI_ch3.FrameBackMid fore=L2UI_ch3.FrameBackMid></td>");
		sb.append("<td><button width=16 height=20 back=L2UI_ch3.FrameBackMid fore=L2UI_ch3.FrameBackMid></td>");
		sb.append("<td><button width=6 height=20 back=L2UI_ch3.FrameBackMid fore=L2UI_ch3.FrameBackMid></td>");
		sb.append("<td><button width=16 height=20 back=L2UI_CH3.FrameBackRight fore=L2UI_CH3.FrameBackRight></td>");
		sb.append("</tr></table>");
		
		if (!template.getDropData().isEmpty())
		{
			int myPage = 1;
			int i = 0;
			int shown = 0;
			boolean hasMore = false;
			
			for (DropCategory cat : template.getDropData())
			{
				if (shown == 6)
				{
					hasMore = true;
					break;
				}
				
				for (DropData drop : cat.getAllDrops())
				{
					int mind = 0, maxd = 0, enchmax = 0, enchmin = 0, enchsucess = 0;
					double chance = (double)drop.getChance() / 10000;
					
					mind = (int) (Config.RATE_DROP_ITEMS * drop.getMinDrop());
					maxd = (int) (Config.RATE_DROP_ITEMS * drop.getMaxDrop());
					
					enchmin = drop.getMinEnchant();
					enchmax = drop.getMaxEnchant();
					enchsucess = drop.getEnchantSucess();
					
					String smind = null, smaxd = null, drops = null;
					if (mind > 999999)
					{
						DecimalFormat df = new DecimalFormat("###.#");
						smind = df.format(((double)(mind))/1000000) + "KK";
						smaxd = df.format(((double)(maxd))/1000000) + "KK";
					}
					else if (mind > 999)
					{
						smind = (mind/1000) + "K";
						smaxd = (maxd/1000) + "K";
					}                                              
					else
					{
						smind = Integer.toString(mind);
						smaxd = Integer.toString(maxd);
					}
					
					if (chance <= 0.001)
					{
						DecimalFormat df = new DecimalFormat("#.####");
						drops = df.format(chance);
					}
					else if (chance <= 0.01)
					{
						DecimalFormat df = new DecimalFormat("#.###");
						drops = df.format(chance);
					}
					else
					{
						DecimalFormat df = new DecimalFormat("##.##");
						drops = df.format(chance);
					}
					
					Item item = ItemTable.getInstance().getTemplate(drop.getItemId());
					String name = item.getName();
					
					if (name.length() >= 17)
						name = name.substring(0, 14) + "...";
					
					if (myPage != pageId)
					{
						i++;
						if (i == 6)
						{
							myPage++;
							i = 0;
						}
						continue;
					}
					
					if (shown == 6)
					{
						hasMore = true;
						break;
					}
					
					sb.append("<table width=295 bgcolor=000000>");
					sb.append("<tr>");
					sb.append("<td align=left width=32><button width=32 height=32 back=" + IconTable.getIcon(item.getItemId()) + " fore=" + IconTable.getIcon(item.getItemId()) + "></td>");
					sb.append("<td align=left width=263>");
					sb.append("<table>");
					
					if (enchsucess == 0)
						sb.append("<tr><td align=left width=263>" + (cat.isSweep() ? "<font color=FF0099>Sweep Chance</font>" : "<font color=00FF00>Drop Chance</font>") + " : (" + drops + "%)</td></tr>");
					else
						sb.append("<tr><td align=left width=263>" + (cat.isSweep() ? "<font color=FF0099>Sweep Chance</font>" : "<font color=00FF00>Drop Chance</font>") + " : (" + drops + "%)<font color=FF0099> Enchant Chance</font> : (" + enchsucess + "%)</td></tr>");
					
					if (Config.NOT_SHOW_DROP_INFO.contains(Integer.valueOf(item.getItemId())))
					{
						if (enchmax == 0)
							sb.append("<tr><td align=left width=263><font color=F01E23>" + name + "</font></td></tr>");
						else
							sb.append("<tr><td align=left width=263><font color=F01E23>" + name + "</font> - Enchant Min: <font color=0CFFF9>+" + enchmin + "</font> Max: <font color=FF0C0C>+" + enchmax + "</font></td></tr>");
					}
					else
						sb.append("<tr><td align=left width=263><font color=F9FF00>" + name + "</font> - Min Drop: <font color=00ECFF>" + smind + "</font> Max Drop: <font color=FF0C0C>" + smaxd + "</font></td></tr>");
					
					sb.append("</table>");
					sb.append("</td>");
					sb.append("</tr>");
					sb.append("</table>");
					sb.append("<img src=l2ui.squaregray width=294 height=1>");
					shown++;
				}
			}
			
			if (shown == 1)
				sb.append("<img height=200>");
			
			if (shown == 2)
				sb.append("<img height=160>");
			
			if (shown == 3)
				sb.append("<img height=120>");
			
			if (shown == 4)
				sb.append("<img height=80>");
			
			if (shown == 5)
				sb.append("<img height=40>");
			
			sb.append("<table cellspacing=0 cellpadding=0><tr>");
			
			if (pageId > 1)
			{
				sb.append("<td><button width=16 height=20 back=L2UI_ch3.FrameBackLeft fore=L2UI_ch3.FrameBackLeft></td>");
				sb.append("<td><button width=15 height=20 back=L2UI_ch3.FrameBackMid fore=L2UI_ch3.FrameBackMid></td>");
				sb.append("<td><button value=\"<Prev\" action=\"bypass -h RaidBossDrop ");
				sb.append(bossId);
				sb.append(" ");
				sb.append(pageId - 1);
				sb.append("\" width=75 height=22 back=L2UI_CH3.Btn1_normalOn fore=L2UI_CH3.Btn1_normal></td>");
				if (!hasMore)
				{
					sb.append("<td><button value=\"Page ");
					sb.append(pageId);
					sb.append("\" width=82 height=20 back=L2UI_ch3.FrameBackMid fore=L2UI_ch3.FrameBackMid></td>");
					sb.append("<td><button width=75 height=22 back=L2UI_ch3.FrameBackMid fore=L2UI_ch3.FrameBackMid></td>");
					sb.append("<td><button width=15 height=20 back=L2UI_ch3.FrameBackMid fore=L2UI_ch3.FrameBackMid></td>");
					sb.append("<td><button width=16 height=20 back=L2UI_CH3.FrameBackRight fore=L2UI_CH3.FrameBackRight></td>");
				}
			}
			
			if (pageId == 1 && !hasMore)
			{
				sb.append("<td><button width=16 height=20 back=L2UI_ch3.FrameBackLeft fore=L2UI_ch3.FrameBackLeft></td>");
				sb.append("<td><button width=15 height=20 back=L2UI_ch3.FrameBackMid fore=L2UI_ch3.FrameBackMid></td>");
				sb.append("<td><button width=75 height=22 back=L2UI_ch3.FrameBackMid fore=L2UI_ch3.FrameBackMid></td>");
				sb.append("<td><button value=\"Page " + pageId + "\" width=82 height=20 back=L2UI_ch3.FrameBackMid fore=L2UI_ch3.FrameBackMid></td>");
				sb.append("<td><button width=75 height=22 back=L2UI_ch3.FrameBackMid fore=L2UI_ch3.FrameBackMid></td>");
				sb.append("<td><button width=15 height=20 back=L2UI_ch3.FrameBackMid fore=L2UI_ch3.FrameBackMid></td>");
				sb.append("<td><button width=16 height=20 back=L2UI_CH3.FrameBackRight fore=L2UI_CH3.FrameBackRight></td>");
			}
			
			if (hasMore)
			{
				if (pageId <= 1)
				{
					sb.append("<td><button width=16 height=20 back=L2UI_ch3.FrameBackLeft fore=L2UI_ch3.FrameBackLeft></td>");
					sb.append("<td><button width=15 height=20 back=L2UI_ch3.FrameBackMid fore=L2UI_ch3.FrameBackMid></td>");
					sb.append("<td><button width=75 height=22 back=L2UI_ch3.FrameBackMid fore=L2UI_ch3.FrameBackMid></td>");
				}
				sb.append("<td><button value=\"Page ");
				sb.append(pageId);
				sb.append("\" width=82 height=20 back=L2UI_ch3.FrameBackMid fore=L2UI_ch3.FrameBackMid></td>");
				sb.append("<td><button value=\"Next>\" action=\"bypass -h RaidBossDrop ");
				sb.append(bossId);
				sb.append(" ");
				sb.append(pageId + 1);
				sb.append("\" width=75 height=22 back=L2UI_CH3.Btn1_normalOn fore=L2UI_CH3.Btn1_normal></td>");
				sb.append("<td><button width=15 height=20 back=L2UI_ch3.FrameBackMid fore=L2UI_ch3.FrameBackMid></td>");
				sb.append("<td><button width=16 height=20 back=L2UI_CH3.FrameBackRight fore=L2UI_CH3.FrameBackRight></td>");
			}
			sb.append("</tr></table>");
		}
		else
			sb.append("This NPC has no drops.");
		
		sb.append("<br>");
		sb.append("<table>");
		sb.append("<tr>");
		sb.append("<td><button value=\"Return\" action=\"bypass voiced_raidinfo\" width=75 height=22 back=L2UI_CH3.Btn1_normalOn fore=L2UI_CH3.Btn1_normal></td>");
		sb.append("</tr>");
		sb.append("</table>");
		sb.append("<br>");
		sb.append("</center></body></html>");
		
		NpcHtmlMessage htm = new NpcHtmlMessage(0);
		htm.setHtml(sb.toString());
		player.sendPacket(htm);
	}
	
	private static void updPassKey(L2PcInstance player, String pass)
	{
		Connection con = null;
		String query = "UPDATE passkey SET passkey = ? WHERE obj_Id = ?";
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement st = con.prepareStatement(query);
			st.setString(1, encodePass(pass));
			st.setInt(2, player.getObjectId());
			st.executeUpdate();
			st.close();
			st = null;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
			con = null;
		}
	}
	
	private static String encodePass(String password)
	{
		String pass = "error";
		try
		{
			MessageDigest md = MessageDigest.getInstance("SHA");
			byte[] raw = password.getBytes("UTF-8");
			byte[] hash = md.digest(raw);
			pass = Base64.encodeBytes(hash);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return pass;
	}
	
	private static void insertPassKeyInformation(L2PcInstance player, String pass, String question, String answer)
	{
		Connection con = null;
		String query = "INSERT INTO passkey (obj_Id, passkey, question, answer) VALUES (?,?,?,?)";
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement st = con.prepareStatement(query);
			st.setInt(1, player.getObjectId());
			st.setString(2, encodePass(pass));
			st.setString(3, question);
			st.setString(4, answer);
			st.execute();
			st.close();
			st = null;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
			con = null;
		}
	}
	
	private static String getPassKeyQuestion(L2PcInstance player)
	{
		Connection con = null;
		String query = "SELECT question FROM passkey WHERE obj_Id = ?";
		String question = "error";
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement st = con.prepareStatement(query);
			st.setInt(1, player.getObjectId());
			ResultSet rs = st.executeQuery();
			
			while (rs.next())
				question = rs.getString(1);
			
			rs.close();
			st.close();
			st = null;
			rs = null;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
			con = null;
		}
		
		return question;
	}
	//METODO DE VERIFICAÇAO DE VIP NA INTERFACE
	private static boolean checkTeleportVip(L2PcInstance player)
	{
	    final long now = System.currentTimeMillis();
	    final long vipEnd = player.getVipEndTime();

	    // Preserva VIP sem prazo cadastrado.
	    if (player.isVip() && (vipEnd == 0L || vipEnd > now))
	        return true;

	    final String message = vipEnd > 0L && vipEnd <= now
	        ? "VIP expirado. Renove para usar."
	        : "Teleport exclusivo para VIP.";

	    player.sendPacket(
	        new ExShowScreenMessage(
	            "CustomEvent=TeleportAccessV1|DENIED",
	            1
	        )
	    );

	    player.sendMessage(message);

	    player.sendPacket(
	        new ExShowScreenMessage(message, 4000)
	    );

	    return false;
	}
	//METODO DE INFORMAÇOES NO NOME DO JOGADOR 
	private static void sendTargetPlayerInfo(L2PcInstance player,String command)
		{
		    final int requestedId;

		    try
		    {
		        requestedId = Integer.parseInt(
		            command.substring("TargetPlayerInfo ".length()).trim()
		        );
		    }
		    catch (NumberFormatException e)
		    {
		        return;
		    }

		    final L2Object selected = player.getTarget();

		    if (!(selected instanceof L2PcInstance))
		        return;

		    final L2PcInstance target = (L2PcInstance) selected;

		    if (target.getObjectId() != requestedId)
		        return;

		    final String clanName = target.getClan() == null
		        ? "No_Clan"
		        : target.getClan().getName().replace('|', ' ');

		    final String payload = "CustomEvent=TargetPlayerV1|"+ target.getObjectId() + "|"+ target.getPvpKills() + "|"+ target.getPkKills() + "|"+ target.getDeaths() + "|"+ clanName + "|"+ (target.isVip() ? "1" : "0");
		    player.sendPacket(
		        new ExShowScreenMessage(payload, 1)
		    );
		}
	
	public static boolean getPassKeyEnable(L2PcInstance player)
	{
		Connection con = null;
		String query = "SELECT COUNT(*) FROM passkey WHERE obj_Id = ?";
		int count = 0;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement st = con.prepareStatement(query);
			st.setInt(1, player.getObjectId());
			ResultSet rs = st.executeQuery();
			
			while (rs.next())
				count = rs.getInt(1);
			
			rs.close();
			st.close();
			st = null;
			rs = null;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
			con = null;
		}
		
		if (count == 1)
			return true;
		return false;
	}
	
}
