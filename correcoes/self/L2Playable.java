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
package net.sf.l2j.gameserver.model.actor;

import java.util.HashSet;
import java.util.Set;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ai.CtrlEvent;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2ClanMember;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2WorldRegion;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.knownlist.PlayableKnownList;
import net.sf.l2j.gameserver.model.actor.stat.PlayableStat;
import net.sf.l2j.gameserver.model.actor.status.PlayableStatus;
import net.sf.l2j.gameserver.model.actor.template.CharTemplate;
import net.sf.l2j.gameserver.model.entity.events.capturetheflag.CTFEvent;
import net.sf.l2j.gameserver.model.entity.events.deathmatch.DMEvent;
import net.sf.l2j.gameserver.model.entity.events.fortress.FOSEvent;
import net.sf.l2j.gameserver.model.entity.events.lastman.LMEvent;
import net.sf.l2j.gameserver.model.entity.events.teamvsteam.TvTEvent;
import net.sf.l2j.gameserver.model.entity.events.tournaments.properties.ArenaConfig;
import net.sf.l2j.gameserver.model.quest.QuestState;
import net.sf.l2j.gameserver.model.zone.ZoneId;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.templates.skills.L2EffectFlag;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;

import phantom.FakePlayer;

/**
 * This class represents all Playable characters in the world.<BR>
 * <BR>
 * L2Playable :<BR>
 * <BR>
 * <li>L2PcInstance</li> <li>L2Summon</li><BR>
 * <BR>
 */
public abstract class L2Playable extends L2Character
{
	/**
	 * Constructor of L2Playable (use L2Character constructor).<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Call the L2Character constructor to create an empty _skills slot and link copy basic Calculator set to this L2Playable</li><BR>
	 * <BR>
	 * @param objectId Identifier of the object to initialized
	 * @param template The L2CharTemplate to apply to the L2Playable
	 */
	public L2Playable(int objectId, CharTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void initKnownList()
	{
		setKnownList(new PlayableKnownList(this));
	}
	
	@Override
	public PlayableKnownList getKnownList()
	{
		return (PlayableKnownList) super.getKnownList();
	}
	
	@Override
	public void initCharStat()
	{
		setStat(new PlayableStat(this));
	}
	
	@Override
	public PlayableStat getStat()
	{
		return (PlayableStat) super.getStat();
	}
	
	@Override
	public void initCharStatus()
	{
		setStatus(new PlayableStatus(this));
	}
	
	@Override
	public PlayableStatus getStatus()
	{
		return (PlayableStatus) super.getStatus();
	}
	
	@Override
	public void onActionShift(L2PcInstance player)
	{
		if (!CTFEvent.onAction(player, getObjectId()) || !FOSEvent.onAction(player, getObjectId()) || !DMEvent.onAction(player, getObjectId()) || !LMEvent.onAction(player, getObjectId()) || !TvTEvent.onAction(player, getObjectId()))
			return;
		
		if (player.getTarget() != this)
			player.setTarget(this);
		else
			player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	@Override
	public boolean doDie(L2Character killer)
	{
		// killing is only possible one time
		synchronized (this)
		{
			if (isDead())
				return false;
			
			// now reset currentHp to zero
			setCurrentHp(0);
			
			setIsDead(true);
		}
		
		// Set target to null and cancel Attack or Cast
		setTarget(null);
		
		// Stop movement
		stopMove(null);
		
		// Stop HP/MP/CP Regeneration task
		getStatus().stopHpMpRegeneration();

		boolean selfNotification = false;

		// Stop all active skills effects in progress
		if (isPhoenixBlessed())
		{
			// remove Lucky Charm if player has SoulOfThePhoenix/Salvation buff
			if (getCharmOfLuck())
				stopCharmOfLuck(null);
			if (isNoblesseBlessed())
				stopNoblesseBlessing(null);
		}
		// Same thing if the Character isn't a Noblesse Blessed L2Playable
		else if (isNoblesseBlessed())
		{
			stopNoblesseBlessing(null);
			
			// remove Lucky Charm if player have Nobless blessing buff
			if (getCharmOfLuck())
				stopCharmOfLuck(null);
		}
		else if (isArena5x5())
		{
			if (ArenaConfig.LOST_BUFFS_ONDIE_5X5)
				stopAllEffectsExceptThoseThatLastThroughDeath();

			selfNotification = true;
		}
		else if (isArena9x9())
		{
			if (ArenaConfig.LOST_BUFFS_ONDIE_9X9)
				stopAllEffectsExceptThoseThatLastThroughDeath();

			selfNotification = true;
		}
		else if (isInsideZone(ZoneId.FLAG_AREA_SELF))
		{
				stopAllEffectsExceptThoseThatLastThroughDeath();

			selfNotification = true;
		}
		
		else if (isInsideZone(ZoneId.LSZONE))
		{
				stopAllEffectsExceptThoseThatLastThroughDeath();

			selfNotification = true;
		}
		
		else if (isInsideZone(ZoneId.PARTY_FARM))
		{
				stopAllEffectsExceptThoseThatLastThroughDeath();

			selfNotification = true;
		}
		
		else if (Config.LEAVE_BUFFS_ON_DIE)
		{
			stopAllEffectsExceptThoseThatLastThroughDeath();
			
			selfNotification = true;
		}
		
		// Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform
		broadcastStatusUpdate();
		
		// Notify L2Character AI
		getAI().notifyEvent(CtrlEvent.EVT_DEAD);
		
		final L2WorldRegion region = getWorldRegion();
		if (region != null)
			region.onDeath(this);
		
		// Notify Quest of L2Playable's death
		final L2PcInstance actingPlayer = getActingPlayer();
		for (QuestState qs : actingPlayer.getNotifyQuestOfDeath())
			qs.getQuest().notifyDeath((killer == null ? this : killer), actingPlayer);
		
		if (killer != null)
		{
			final L2PcInstance player = killer.getActingPlayer();
			if (player != null)
				player.onKillUpdatePvPKarma(this);
		}
		
		if (selfNotification)
			announceSelf(killer);

		return true;
	}
	
	/** Recipient-specific screen notices only; no global SELF chat announcement. */
	private void announceSelf(L2Character killer)
	{
		if (!Config.GET_SELF_ANNOUNCE)
			return;

		final L2PcInstance target = getActingPlayer();
		if (target == null)
			return;

		final L2Clan targetClan = target.getClan();

		// A pet's death must not tell its owner that the owner was SELF.
		if (!(this instanceof L2PcInstance) || target instanceof FakePlayer)
			return;

		final L2PcInstance attacker = killer == null ? null : killer.getActingPlayer();
		final Set<Integer> notified = new HashSet<>();
		final ExShowScreenMessage victimMessage = new ExShowScreenMessage("You was SELF !", 6000);
		final ExShowScreenMessage killedMessage = new ExShowScreenMessage(target.getName() + " was SELF !", 6000);
		final ExShowScreenMessage victimClanMessage = new ExShowScreenMessage("A Clan member " + target.getName() + " was SELF !", 6000);

		// Personal messages take precedence over clan messages.
		sendSelfScreen(target, victimMessage, notified);
		if (attacker != null && attacker != target)
			sendSelfScreen(attacker, killedMessage, notified);

		// If both clans are the same, use the victim-clan message only once.
		sendSelfToClan(targetClan, victimClanMessage, notified);
		if (attacker != null && attacker != target)
		{
			final ExShowScreenMessage killerClanMessage = new ExShowScreenMessage(
				"A Clan member " + attacker.getName() + " left " + target.getName() + " was SELF !", 6000);
			sendSelfToClan(attacker.getClan(), killerClanMessage, notified);
		}
	}

	private static void sendSelfToClan(L2Clan clan, ExShowScreenMessage message, Set<Integer> notified)
	{
		if (clan == null)
			return;

		for (L2ClanMember member : clan.getMembers())
		{
			if (member != null)
				sendSelfScreen(member.getPlayerInstance(), message, notified);
		}
	}

	private static void sendSelfScreen(L2PcInstance player, ExShowScreenMessage message, Set<Integer> notified)
	{
		if (player != null && !(player instanceof FakePlayer) && player.isOnline() && notified.add(player.getObjectId()))
			player.sendPacket(message);
	}

	public boolean checkIfPvP(L2Playable target)
	{
		if (target == null || target == this)
			return false;
		
		final L2PcInstance player = getActingPlayer();
		if (player == null || player.getKarma() != 0)
			return false;
		
		final L2PcInstance targetPlayer = target.getActingPlayer();
		if (targetPlayer == null || targetPlayer == this)
			return false;
		
		if (targetPlayer.getKarma() != 0 || targetPlayer.getPvpFlag() == 0)
			return false;
		
		return true;
	}
	
	/**
	 * Return True.
	 */
	@Override
	public boolean isAttackable()
	{
		return true;
	}
	
	/**
	 * <B><U> Overridden in </U> :</B>
	 * <ul>
	 * <li>L2Summon</li>
	 * <li>L2PcInstance</li>
	 * </ul>
	 * @param id The system message to send to player.
	 */
	public void sendPacket(SystemMessageId id)
	{
		// default implementation
	}
	
	// Support for Noblesse Blessing skill, where buffs are retained after resurrect
	public final boolean isNoblesseBlessed()
	{
		return _effects.isAffected(L2EffectFlag.NOBLESS_BLESSING);
	}
	
	public final void stopNoblesseBlessing(L2Effect effect)
	{
		if (effect == null)
			stopEffects(L2EffectType.NOBLESSE_BLESSING);
		else
			removeEffect(effect);
		updateAbnormalEffect();
	}
	
	// Support for Soul of the Phoenix and Salvation skills
	public final boolean isPhoenixBlessed()
	{
		return _effects.isAffected(L2EffectFlag.PHOENIX_BLESSING);
	}
	
	public final void stopPhoenixBlessing(L2Effect effect)
	{
		if (effect == null)
			stopEffects(L2EffectType.PHOENIX_BLESSING);
		else
			removeEffect(effect);
		
		updateAbnormalEffect();
	}
	
	/**
	 * @return True if the Silent Moving mode is active.
	 */
	public boolean isSilentMoving()
	{
		return _effects.isAffected(L2EffectFlag.SILENT_MOVE);
	}
	
	// for Newbie Protection Blessing skill, keeps you safe from an attack by a chaotic character >= 10 levels apart from you
	public final boolean getProtectionBlessing()
	{
		return _effects.isAffected(L2EffectFlag.PROTECTION_BLESSING);
	}
	
	public void stopProtectionBlessing(L2Effect effect)
	{
		if (effect == null)
			stopEffects(L2EffectType.PROTECTION_BLESSING);
		else
			removeEffect(effect);
		
		updateAbnormalEffect();
	}
	
	// Charm of Luck - During a Raid/Boss war, decreased chance for death penalty
	public final boolean getCharmOfLuck()
	{
		return _effects.isAffected(L2EffectFlag.CHARM_OF_LUCK);
	}
	
	public final void stopCharmOfLuck(L2Effect effect)
	{
		if (effect == null)
			stopEffects(L2EffectType.CHARM_OF_LUCK);
		else
			removeEffect(effect);
		
		updateAbnormalEffect();
	}
	
	@Override
	public void updateEffectIcons(boolean partyOnly)
	{
		_effects.updateEffectIcons(partyOnly);
	}
	
	/**
	 * This method allows to easily send relations. Overridden in L2Summon and L2PcInstance.
	 */
	public void broadcastRelationsChanges()
	{
	}
	
	@Override
	public boolean isInArena()
	{
		return ((isInsideZone(ZoneId.PVP) && !isInsideZone(ZoneId.SIEGE)));
	}
	
	public abstract int getKarma();
	
	public abstract byte getPvpFlag();
	
	public abstract boolean useMagic(L2Skill skill, boolean forceUse, boolean dontMove);
}