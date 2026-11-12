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
package net.sf.l2j.gameserver.model.actor.instance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.commons.math.MathUtil;
import net.sf.l2j.gameserver.GameTimeController;
import net.sf.l2j.gameserver.LoginServerThread;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.CtrlEvent;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.ai.L2CharacterAI;
import net.sf.l2j.gameserver.ai.L2PlayerAI;
import net.sf.l2j.gameserver.ai.L2SummonAI;
import net.sf.l2j.gameserver.ai.NextAction;
import net.sf.l2j.gameserver.ai.NextAction.NextActionCallback;
import net.sf.l2j.gameserver.communitybbs.BB.Forum;
import net.sf.l2j.gameserver.communitybbs.Manager.ForumsBBSManager;
import net.sf.l2j.gameserver.datatables.AccessLevels;
import net.sf.l2j.gameserver.model.entity.events.treasurehunt.TreasureHuntSession;
import net.sf.l2j.gameserver.datatables.CharNameTable;
import net.sf.l2j.gameserver.datatables.CharTemplateTable;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.datatables.FishTable;
import net.sf.l2j.gameserver.datatables.GmListTable;
import net.sf.l2j.gameserver.datatables.HennaTable;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.datatables.PetDataTable;
import net.sf.l2j.gameserver.datatables.RecipeTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.datatables.custom.VipAuditLog;
import net.sf.l2j.gameserver.datatables.SkillTable.FrequentSkill;
import net.sf.l2j.gameserver.datatables.SkillTreeTable;
import net.sf.l2j.gameserver.geoengine.PathFinding;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.handler.ItemHandler;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminEditChar;
import net.sf.l2j.gameserver.handler.itemhandlers.custom.special.AugmentScrolls;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.CoupleManager;
import net.sf.l2j.gameserver.instancemanager.CursedWeaponsManager;
import net.sf.l2j.gameserver.instancemanager.DimensionalRiftManager;
import net.sf.l2j.gameserver.instancemanager.DuelManager;
import net.sf.l2j.gameserver.instancemanager.GrandBossManager;
import net.sf.l2j.gameserver.instancemanager.GuildMissionManager;
import net.sf.l2j.gameserver.instancemanager.ItemsOnGroundManager;
import net.sf.l2j.gameserver.instancemanager.PlayerKillsManager;
import net.sf.l2j.gameserver.instancemanager.PlayerModeManager;
import net.sf.l2j.gameserver.instancemanager.QuestManager;
import net.sf.l2j.gameserver.instancemanager.SevenSigns;
import net.sf.l2j.gameserver.instancemanager.SevenSignsFestival;
import net.sf.l2j.gameserver.instancemanager.SiegeManager;
import net.sf.l2j.gameserver.instancemanager.autofarm.AutofarmManager;
import net.sf.l2j.gameserver.instancemanager.autofarm.AutofarmPlayerRoutine;
import net.sf.l2j.gameserver.instancemanager.autofarmsecondary.AutofarmSecondaryManager;
import net.sf.l2j.gameserver.model.Announcement;
import net.sf.l2j.gameserver.model.BlockList;
import net.sf.l2j.gameserver.model.FishData;
import net.sf.l2j.gameserver.model.L2AccessLevel;
import net.sf.l2j.gameserver.model.L2CharPosition;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2ClanMember;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Fishing;
import net.sf.l2j.gameserver.model.L2Macro;
import net.sf.l2j.gameserver.model.L2ManufactureList;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Party;
import net.sf.l2j.gameserver.model.L2Party.MessageType;
import net.sf.l2j.gameserver.model.L2PetData;
import net.sf.l2j.gameserver.model.L2PetData.L2PetLevelData;
import net.sf.l2j.gameserver.model.L2Radar;
import net.sf.l2j.gameserver.model.L2Request;
import net.sf.l2j.gameserver.model.L2ShortCut;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Skill.SkillTargetType;
import net.sf.l2j.gameserver.model.L2SkillLearn;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.L2WorldRegion;
import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.model.MacroList;
import net.sf.l2j.gameserver.model.ShortCuts;
import net.sf.l2j.gameserver.model.ShotType;
import net.sf.l2j.gameserver.model.TradeList;
import net.sf.l2j.gameserver.model.Certification.CertificationManager;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.L2Vehicle;
import net.sf.l2j.gameserver.model.actor.appearance.PcAppearance;
import net.sf.l2j.gameserver.model.actor.knownlist.PcKnownList;
import net.sf.l2j.gameserver.model.actor.position.PcPosition;
import net.sf.l2j.gameserver.model.actor.stat.PcStat;
import net.sf.l2j.gameserver.model.actor.status.PcStatus;
import net.sf.l2j.gameserver.model.actor.template.PcTemplate;
import net.sf.l2j.gameserver.model.antilag.AntilagController;
import net.sf.l2j.gameserver.model.base.ClassId;
import net.sf.l2j.gameserver.model.base.ClassLevel;
import net.sf.l2j.gameserver.model.base.Experience;
import net.sf.l2j.gameserver.model.base.PlayerClass;
import net.sf.l2j.gameserver.model.base.Race;
import net.sf.l2j.gameserver.model.base.SubClass;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.model.entity.Duel;
import net.sf.l2j.gameserver.model.entity.Hero;
import net.sf.l2j.gameserver.model.entity.Siege;
import net.sf.l2j.gameserver.model.entity.events.Hide;
import net.sf.l2j.gameserver.model.entity.events.PvPEnchantSystem;
import net.sf.l2j.gameserver.model.entity.events.PvpBossManager;
import net.sf.l2j.gameserver.model.entity.events.RankedSystem;
import net.sf.l2j.gameserver.model.entity.events.TopKillerRoundSystem;
import net.sf.l2j.gameserver.model.entity.events.capturetheflag.CTFEvent;
import net.sf.l2j.gameserver.model.entity.events.deathmatch.DMEvent;
import net.sf.l2j.gameserver.model.entity.events.fortress.FOSEvent;
import net.sf.l2j.gameserver.model.entity.events.killtheboss.KTBEvent;
import net.sf.l2j.gameserver.model.entity.events.lastman.LMEvent;
import net.sf.l2j.gameserver.model.entity.events.multiteamvsteam.MultiTvTEvent;
import net.sf.l2j.gameserver.model.entity.events.teamvsteam.TvTEvent;
import net.sf.l2j.gameserver.model.entity.events.toppvpevent.PvPEvent;
import net.sf.l2j.gameserver.model.entity.events.toppvpevent.PvPEventManager;
import net.sf.l2j.gameserver.model.entity.events.tournaments.properties.ArenaConfig;
import net.sf.l2j.gameserver.model.holder.ItemHolder;
import net.sf.l2j.gameserver.model.holder.RewardHolder;
import net.sf.l2j.gameserver.model.holder.SkillUseHolder;
import net.sf.l2j.gameserver.model.item.Henna;
import net.sf.l2j.gameserver.model.item.RecipeList;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Armor;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.item.kind.Weapon;
import net.sf.l2j.gameserver.model.item.type.ActionType;
import net.sf.l2j.gameserver.model.item.type.ArmorType;
import net.sf.l2j.gameserver.model.item.type.CrystalType;
import net.sf.l2j.gameserver.model.item.type.EtcItemType;
import net.sf.l2j.gameserver.model.item.type.WeaponType;
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;
import net.sf.l2j.gameserver.model.itemcontainer.ItemContainer;
import net.sf.l2j.gameserver.model.itemcontainer.PcFreight;
import net.sf.l2j.gameserver.model.itemcontainer.PcInventory;
import net.sf.l2j.gameserver.model.itemcontainer.PcWarehouse;
import net.sf.l2j.gameserver.model.itemcontainer.PetInventory;
import net.sf.l2j.gameserver.model.itemcontainer.listeners.ItemPassiveSkillsListener;
import net.sf.l2j.gameserver.model.olympiad.OlympiadGameManager;
import net.sf.l2j.gameserver.model.olympiad.OlympiadGameTask;
import net.sf.l2j.gameserver.model.olympiad.OlympiadManager;
import net.sf.l2j.gameserver.model.partymatching.PartyMatchRoom;
import net.sf.l2j.gameserver.model.partymatching.PartyMatchRoomList;
import net.sf.l2j.gameserver.model.partymatching.PartyMatchWaitingList;
import net.sf.l2j.gameserver.model.quest.Quest;
import net.sf.l2j.gameserver.model.quest.QuestEventType;
import net.sf.l2j.gameserver.model.quest.QuestState;
import net.sf.l2j.gameserver.model.zone.ZoneId;
import net.sf.l2j.gameserver.model.zone.type.L2BossZone;
import net.sf.l2j.gameserver.model.zone.type.L2FarmRandomZone;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.AbstractNpcInfo;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ChairSit;
import net.sf.l2j.gameserver.network.serverpackets.ChangeWaitType;
import net.sf.l2j.gameserver.network.serverpackets.CharInfo;
import net.sf.l2j.gameserver.network.serverpackets.ConfirmDlg;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.Earthquake;
import net.sf.l2j.gameserver.network.serverpackets.EtcStatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.ExAutoSoulShot;
import net.sf.l2j.gameserver.network.serverpackets.ExDuelUpdateUserInfo;
import net.sf.l2j.gameserver.network.serverpackets.ExFishingEnd;
import net.sf.l2j.gameserver.network.serverpackets.ExFishingStart;
import net.sf.l2j.gameserver.network.serverpackets.ExOlympiadMode;
import net.sf.l2j.gameserver.network.serverpackets.ExPCCafePointInfo;
import net.sf.l2j.gameserver.network.serverpackets.ExPkContour;
import net.sf.l2j.gameserver.network.serverpackets.ExRedSky;
import net.sf.l2j.gameserver.network.serverpackets.ExSetCompassZoneCode;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.ExStorageMaxCount;
import net.sf.l2j.gameserver.network.serverpackets.FriendList;
import net.sf.l2j.gameserver.network.serverpackets.GetOnVehicle;
import net.sf.l2j.gameserver.network.serverpackets.HennaInfo;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket;
import net.sf.l2j.gameserver.network.serverpackets.LeaveWorld;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.MoveToPawn;
import net.sf.l2j.gameserver.network.serverpackets.MyTargetSelected;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.ObservationMode;
import net.sf.l2j.gameserver.network.serverpackets.ObservationReturn;
import net.sf.l2j.gameserver.network.serverpackets.PartySmallWindowUpdate;
import net.sf.l2j.gameserver.network.serverpackets.PetInventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.network.serverpackets.PledgeShowMemberListDelete;
import net.sf.l2j.gameserver.network.serverpackets.PledgeShowMemberListUpdate;
import net.sf.l2j.gameserver.network.serverpackets.PrivateStoreListBuy;
import net.sf.l2j.gameserver.network.serverpackets.PrivateStoreListSell;
import net.sf.l2j.gameserver.network.serverpackets.PrivateStoreManageListBuy;
import net.sf.l2j.gameserver.network.serverpackets.PrivateStoreManageListSell;
import net.sf.l2j.gameserver.network.serverpackets.PrivateStoreMsgBuy;
import net.sf.l2j.gameserver.network.serverpackets.PrivateStoreMsgSell;
import net.sf.l2j.gameserver.network.serverpackets.RecipeShopManageList;
import net.sf.l2j.gameserver.network.serverpackets.RecipeShopMsg;
import net.sf.l2j.gameserver.network.serverpackets.RecipeShopSellList;
import net.sf.l2j.gameserver.network.serverpackets.RelationChanged;
import net.sf.l2j.gameserver.network.serverpackets.Ride;
import net.sf.l2j.gameserver.network.serverpackets.SendTradeDone;
import net.sf.l2j.gameserver.network.serverpackets.ServerClose;
import net.sf.l2j.gameserver.network.serverpackets.SetupGauge;
import net.sf.l2j.gameserver.network.serverpackets.ShortBuffStatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.ShortCutInit;
import net.sf.l2j.gameserver.network.serverpackets.SkillCoolTime;
import net.sf.l2j.gameserver.network.serverpackets.SkillList;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.network.serverpackets.StaticObject;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.StopMove;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.TargetSelected;
import net.sf.l2j.gameserver.network.serverpackets.TargetUnselected;
import net.sf.l2j.gameserver.network.serverpackets.TitleUpdate;
import net.sf.l2j.gameserver.network.serverpackets.TradePressOtherOk;
import net.sf.l2j.gameserver.network.serverpackets.TradePressOwnOk;
import net.sf.l2j.gameserver.network.serverpackets.TradeStart;
import net.sf.l2j.gameserver.network.serverpackets.UserInfo;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.skills.effects.EffectTemplate;
import net.sf.l2j.gameserver.skills.funcs.FuncHennaCON;
import net.sf.l2j.gameserver.skills.funcs.FuncHennaDEX;
import net.sf.l2j.gameserver.skills.funcs.FuncHennaINT;
import net.sf.l2j.gameserver.skills.funcs.FuncHennaMEN;
import net.sf.l2j.gameserver.skills.funcs.FuncHennaSTR;
import net.sf.l2j.gameserver.skills.funcs.FuncHennaWIT;
import net.sf.l2j.gameserver.skills.funcs.FuncMaxCpMul;
import net.sf.l2j.gameserver.skills.l2skills.L2SkillSiegeFlag;
import net.sf.l2j.gameserver.skills.l2skills.L2SkillSummon;
import net.sf.l2j.gameserver.taskmanager.AttackStanceTaskManager;
import net.sf.l2j.gameserver.taskmanager.FarmRandomZoneTaskManager;
import net.sf.l2j.gameserver.taskmanager.ItemsAutoDestroyTaskManager;
import net.sf.l2j.gameserver.taskmanager.PvpFlagTaskManager;
import net.sf.l2j.gameserver.taskmanager.ShadowItemTaskManager;
import net.sf.l2j.gameserver.taskmanager.TakeBreakTaskManager;
import net.sf.l2j.gameserver.taskmanager.WaterTaskManager;
import net.sf.l2j.gameserver.templates.skills.L2EffectFlag;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;
import net.sf.l2j.gameserver.util.Broadcast;
import net.sf.l2j.gameserver.util.FloodProtectors;
import net.sf.l2j.gameserver.util.Util;
import net.sf.l2j.gameserver.util.variables.sub.PlayerVar;
import net.sf.l2j.gameserver.util.variables.sub.PlayerVariables;
import net.sf.l2j.util.DropsLog;
import net.sf.l2j.util.Rnd;
import phantom.FakePlayer;
import phantom.ai.FakePlayerUtilsAI;

/**
 * This class represents all player characters in the world. There is always a client-thread connected to this (except if a player-store is activated upon logout).
 */
public class L2PcInstance extends L2Playable
{
	private final TreasureHuntSession _treasureHuntSession = new TreasureHuntSession(this);

	public TreasureHuntSession getTreasureHuntSession()
	{
		return _treasureHuntSession;
	}

	private static final String RESTORE_SKILLS_FOR_CHAR = "SELECT skill_id,skill_level FROM character_skills WHERE char_obj_id=? AND class_index=?";
	private static final String ADD_NEW_SKILL = "INSERT INTO character_skills (char_obj_id,skill_id,skill_level,class_index) VALUES (?,?,?,?)";
	private static final String UPDATE_CHARACTER_SKILL_LEVEL = "UPDATE character_skills SET skill_level=? WHERE skill_id=? AND char_obj_id=? AND class_index=?";
	private static final String DELETE_SKILL_FROM_CHAR = "DELETE FROM character_skills WHERE skill_id=? AND char_obj_id=? AND class_index=?";
	private static final String DELETE_CHAR_SKILLS = "DELETE FROM character_skills WHERE char_obj_id=? AND class_index=?";

	private static final String ADD_SKILL_SAVE = "INSERT INTO character_skills_save (char_obj_id,skill_id,skill_level,effect_count,effect_cur_time,reuse_delay,systime,restore_type,class_index,buff_index) VALUES (?,?,?,?,?,?,?,?,?,?)";
	private static final String RESTORE_SKILL_SAVE = "SELECT skill_id,skill_level,effect_count,effect_cur_time, reuse_delay, systime, restore_type FROM character_skills_save WHERE char_obj_id=? AND class_index=? ORDER BY buff_index ASC";
	private static final String DELETE_SKILL_SAVE = "DELETE FROM character_skills_save WHERE char_obj_id=? AND class_index=?";
	private static final String INSERT_CHARACTER = "INSERT INTO characters (account_name,obj_Id,char_name,level,maxHp,curHp,maxCp,curCp,maxMp,curMp,face,hairStyle,hairColor,sex,exp,sp,karma,pvpkills,pkkills,deaths,clanid,race,classid,deletetime,cancraft,title,accesslevel,online,isin7sdungeon,clan_privs,wantspeace,base_class,nobless,power_grade,last_recom_date,name_color,title_color) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
	//String original 
	//private static final String INSERT_CHARACTER = "INSERT INTO characters (account_name,obj_Id,char_name,level,maxHp,curHp,maxCp,curCp,maxMp,curMp,face,hairStyle,hairColor,sex,exp,sp,karma,pvpkills,pkkills,clanid,race,classid,deletetime,cancraft,title,accesslevel,online,isin7sdungeon,clan_privs,wantspeace,base_class,nobless,power_grade,last_recom_date,name_color,title_color) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

	private static final String UPDATE_CHARACTER = "UPDATE characters SET level=?,maxHp=?,curHp=?,maxCp=?,curCp=?,maxMp=?,curMp=?,face=?,hairStyle=?,hairColor=?,sex=?,heading=?,x=?,y=?,z=?,exp=?,expBeforeDeath=?,sp=?,karma=?,pvpkills=?,pkkills=?,deaths=?,rec_have=?,rec_left=?,clanid=?,race=?,classid=?,deletetime=?,title=?,accesslevel=?,online=?,isin7sdungeon=?,clan_privs=?,wantspeace=?,base_class=?,onlinetime=?,punish_level=?,punish_timer=?,nobless=?,power_grade=?,subpledge=?,last_recom_date=?,lvl_joined_academy=?,apprentice=?,sponsor=?,varka_ketra_ally=?,clan_join_expiry_time=?,clan_create_expiry_time=?,char_name=?,death_penalty_level=?,name_color=?,title_color=?,aio=?,aio_end=?,vip=?,vip_end=?,timedHero=?,timedHero_end=?,pc_point=?,item_time=?,1x1_wins=?,2x2_wins=?,fakeArmorObjectId=?,fakeArmorItemId=?,fakeWeaponObjectId=?,fakeWeaponItemId=?,activeSkinId=? WHERE obj_id=?";
	private static final String RESTORE_CHARACTER = "SELECT account_name, obj_Id, char_name, level, maxHp, curHp, maxCp, curCp, maxMp, curMp, face, hairStyle, hairColor, sex, heading, x, y, z, exp, expBeforeDeath, sp, karma, pvpkills, pkkills,deaths, clanid, race, classid, deletetime, cancraft, title, rec_have, rec_left, accesslevel, online, char_slot, lastAccess, clan_privs, wantspeace, base_class, onlinetime, isin7sdungeon, punish_level, punish_timer, nobless, power_grade, subpledge, last_recom_date, lvl_joined_academy, apprentice, sponsor, varka_ketra_ally,clan_join_expiry_time,clan_create_expiry_time,death_penalty_level,name_color,title_color,aio,aio_end,vip,vip_end,timedHero,timedHero_end,pc_point,item_time,1x1_wins,2x2_wins,first_log,select_armor,select_weapon,select_classe,fakeArmorObjectId,fakeWeaponObjectId,activeSkinId FROM characters WHERE obj_id=?";

	private static final String RESTORE_CHAR_SUBCLASSES = "SELECT class_id,exp,sp,level,class_index FROM character_subclasses WHERE char_obj_id=? ORDER BY class_index ASC";
	private static final String ADD_CHAR_SUBCLASS = "INSERT INTO character_subclasses (char_obj_id,class_id,exp,sp,level,class_index) VALUES (?,?,?,?,?,?)";
	private static final String UPDATE_CHAR_SUBCLASS = "UPDATE character_subclasses SET exp=?,sp=?,level=?,class_id=? WHERE char_obj_id=? AND class_index =?";
	private static final String DELETE_CHAR_SUBCLASS = "DELETE FROM character_subclasses WHERE char_obj_id=? AND class_index=?";

	private static final String RESTORE_CHAR_HENNAS = "SELECT slot,symbol_id FROM character_hennas WHERE char_obj_id=? AND class_index=?";
	private static final String ADD_CHAR_HENNA = "INSERT INTO character_hennas (char_obj_id,symbol_id,slot,class_index) VALUES (?,?,?,?)";
	private static final String DELETE_CHAR_HENNA = "DELETE FROM character_hennas WHERE char_obj_id=? AND slot=? AND class_index=?";
	private static final String DELETE_CHAR_HENNAS = "DELETE FROM character_hennas WHERE char_obj_id=? AND class_index=?";
	private static final String DELETE_CHAR_SHORTCUTS = "DELETE FROM character_shortcuts WHERE char_obj_id=? AND class_index=?";

	private static final String RESTORE_CHAR_RECOMS = "SELECT char_id,target_id FROM character_recommends WHERE char_id=?";
	private static final String ADD_CHAR_RECOM = "INSERT INTO character_recommends (char_id,target_id) VALUES (?,?)";
	private static final String DELETE_CHAR_RECOMS = "DELETE FROM character_recommends WHERE char_id=?";

	private static final String UPDATE_NOBLESS = "UPDATE characters SET nobless=? WHERE obj_Id=?";

	public static final int REQUEST_TIMEOUT = 15;

	public static final int STORE_PRIVATE_NONE = 0;
	public static final int STORE_PRIVATE_SELL = 1;
	public static final int STORE_PRIVATE_SELL_MANAGE = 2;
	public static final int STORE_PRIVATE_BUY = 3;
	public static final int STORE_PRIVATE_BUY_MANAGE = 4;
	public static final int STORE_PRIVATE_MANUFACTURE = 5;
	public static final int STORE_PRIVATE_PACKAGE_SELL = 8;

	private static final int[] EXPERTISE_LEVELS =
		{
		0, // NONE
		20, // D
		40, // C
		52, // B
		61, // A
		76, // S
		};

	private static final int[] COMMON_CRAFT_LEVELS =
		{
		5,
		20,
		28,
		36,
		43,
		49,
		55,
		62
		};

	public class AIAccessor extends L2Character.AIAccessor
	{
		protected AIAccessor()
		{

		}

		public L2PcInstance getPlayer()
		{
			return L2PcInstance.this;
		}

		public void doPickupItem(L2Object object)
		{
			L2PcInstance.this.doPickupItem(object);

			// Schedule a paralyzed task to wait for the animation to finish
			ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
			{
				@Override
				public void run()
				{
					setIsParalyzed(false);
				}
			}, 500);
			setIsParalyzed(true);

			ItemInstance item = (ItemInstance) object;

			if (!isGM() && (item.getItemId() >= 6656 && item.getItemId() <= 6662) && isInsideZone(ZoneId.PARTY_FARM) && isInsideZone(ZoneId.LSZONE) && isInsideZone(ZoneId.FLAG_AREA_SELF) && isInsideZone(ZoneId.NO_ZERG))
			{
				if (getClan() != null)
					Announcement.Announce(getName() + " of the clan " + getClan().getName() + " picked up " + item.getName());
				else
					Announcement.Announce(getName() + " picked up " + item.getName());
			}
		}

		public void doInteract(L2Character target)
		{
			L2PcInstance.this.doInteract(target);
		}

		@Override
		public void doAttack(L2Character target)
		{
			super.doAttack(target);

			getPlayer().setRecentFakeDeath(false);
		}

		@Override
		public void doCast(L2Skill skill)
		{
			super.doCast(skill);

			getPlayer().setRecentFakeDeath(false);
		}
	}

	private L2GameClient _client;

	private final Object _pkContourPacketLock = new Object();
	private volatile boolean _pkContourReady;
	
	private String _accountName;
	private long _deleteTimer;

	private boolean _isOnline = false;
	private long _onlineTime;
	private long _onlineBeginTime;
	private long _lastAccess;
	private long _uptime;

	private final ReentrantLock _subclassLock = new ReentrantLock();
	protected int _baseClass;
	protected int _activeClass;
	protected int _classIndex = 0;
	private final Map<Integer, SubClass> _subClasses = new LinkedHashMap<>();

	private PcAppearance _appearance;
	private int pcBangPoint = 0;

	private long _expBeforeDeath;
	private int _karma;
	private int _pvpKills;
	private int _pkKills;
	private int _deaths;
	private byte _pvpFlag;
	private byte _siegeState = 0;
	private int _curWeightPenalty = 0;
	
	//METODOS DO BOTAO PVP MODE INTERFACE
	private volatile int _playerModeState;
	private volatile long _hunterCombatFlagUntil;

	private String _playerModeOriginalTitle;
	private int _playerModeOriginalTitleColor;
	private boolean _playerModeAppearanceSaved;

	public int getPlayerModeState()
	{
	    return _playerModeState;
	}

	public void setPlayerModeState(int mode)
	{
	    if (mode < 0 || mode > 2)
	    {
	        throw new IllegalArgumentException("Modo invalido: " + mode);
	    }

	    _playerModeState = mode;
	}

	public boolean isPveModeActive()
	{
	    return _playerModeState == 1;
	}

	public boolean isPvpDropModeActive()
	{
	    return _playerModeState == 2;
	}

	public void setPvpDropModeActive(boolean active)
	{
	    PlayerModeManager.getInstance().setMode(this, active ? 2 : 0);
	}

	public synchronized void savePlayerModeAppearance()
	{
	    if (!_playerModeAppearanceSaved)
	    {
	        _playerModeOriginalTitle = getTitle();
	        _playerModeOriginalTitleColor = getAppearance().getTitleColor();
	        _playerModeAppearanceSaved = true;
	    }
	}

	public synchronized void restorePlayerModeAppearance()
	{
	    if (_playerModeAppearanceSaved)
	    {
	        setTitle(_playerModeOriginalTitle);
	        getAppearance().setTitleColor(_playerModeOriginalTitleColor);

	        _playerModeAppearanceSaved = false;
	    }
	}

	public synchronized String getPlayerModeStoredTitle()
	{
	    return _playerModeAppearanceSaved
	        ? _playerModeOriginalTitle
	        : getTitle();
	}

	public synchronized int getPlayerModeStoredTitleColor()
	{
	    return _playerModeAppearanceSaved
	        ? _playerModeOriginalTitleColor
	        : getAppearance().getTitleColor();
	}

	public synchronized void noteHunterPvpFlag(long durationMillis)
	{
	    if (isPvpDropModeActive())
	    {
	        _hunterCombatFlagUntil = Math.max(
	            _hunterCombatFlagUntil,
	            System.currentTimeMillis() + durationMillis
	        );
	    }
	}

	public long getHunterCombatFlagUntil()
	{
	    return _hunterCombatFlagUntil;
	}

	public void clearHunterCombatFlagTime()
	{
	    _hunterCombatFlagUntil = 0;
	}
	//FIM 
	private int _lastCompassZone; // the last compass zone update send to the client

	private boolean _isInWater;
	private boolean _isIn7sDungeon = false;

	private PunishLevel _punishLevel = PunishLevel.NONE;
	private long _punishTimer = 0;
	private ScheduledFuture<?> _punishTask;

	public enum PunishLevel
	{
		NONE(0, ""),
		CHAT(1, "chat banned"),
		JAIL(2, "jailed"),
		CHAR(3, "banned"),
		ACC(4, "banned");

		private final int punValue;
		private final String punString;

		PunishLevel(int value, String string)
		{
			punValue = value;
			punString = string;
		}

		public int value()
		{
			return punValue;
		}

		public String string()
		{
			return punString;
		}
	}

	private boolean _inOlympiadMode = false;
	private boolean _OlympiadStart = false;
	private int _olympiadGameId = -1;
	private int _olympiadSide = -1;

	private boolean _isInDuel = false;
	private int _duelState = Duel.DUELSTATE_NODUEL;
	private int _duelId = 0;
	private SystemMessageId _noDuelReason = SystemMessageId.THERE_IS_NO_OPPONENT_TO_RECEIVE_YOUR_CHALLENGE_FOR_A_DUEL;

	private L2Vehicle _vehicle = null;
	private Location _inVehiclePosition;

	public ScheduledFuture<?> _taskforfish;

	private int _mountType;
	private int _mountNpcId;
	private int _mountLevel;
	private int _mountObjectID = 0;

	public int _telemode = 0;
	private boolean _inCrystallize;
	private boolean _inCraftMode;

	private final Map<Integer, RecipeList> _dwarvenRecipeBook = new HashMap<>();
	private final Map<Integer, RecipeList> _commonRecipeBook = new HashMap<>();

	private boolean _waitTypeSitting;

	private int _lastX;
	private int _lastY;
	private int _lastZ;
	private boolean _observerMode = false;

	private final Location _lastServerPosition = new Location(0, 0, 0);

	private int _recomHave;
	private int _recomLeft;
	private long _lastRecomUpdate;
	private final List<Integer> _recomChars = new ArrayList<>();

	private final PcInventory _inventory = new PcInventory(this);
	private PcWarehouse _warehouse;
	private PcFreight _freight;
	private final List<PcFreight> _depositedFreight = new ArrayList<>();

	private int _privateStore;

	private TradeList _activeTradeList;
	private ItemContainer _activeWarehouse;
	private L2ManufactureList _createList;
	private TradeList _sellList;
	private TradeList _buyList;

	private boolean _noble = false;
	private boolean _hero = false;

	private int heroConsecutiveKillCount = 0;
	private boolean isPVPHero = false;

	private boolean _isAio = false;
	private long _aio_endTime;

	private boolean _isVip = false;
	private long _vip_endTime = 0;

	private boolean _isTimedHero = false;
	private long _timedHero_endTime = 0;

	private L2Npc _currentFolkNpc = null;

	private int _questNpcObject = 0;

	private final List<QuestState> _quests = new ArrayList<>();
	private final List<QuestState> _notifyQuestOfDeathList = new ArrayList<>();

	private final ShortCuts _shortCuts = new ShortCuts(this);

	private final MacroList _macroses = new MacroList(this);

	private ClassId _skillLearningClassId;

	private final Henna[] _henna = new Henna[3];
	private int _hennaSTR;
	private int _hennaINT;
	private int _hennaDEX;
	private int _hennaMEN;
	private int _hennaWIT;
	private int _hennaCON;


	private L2Summon _summon = null;
	private L2AgathionInstance _agathion;
	private L2TamedBeastInstance _tamedBeast = null;

	// TODO: This needs to be better integrated and saved/loaded
	private L2Radar _radar;

	private int _partyroom = 0;

	private int _clanId;
	private L2Clan _clan;
	private int _apprentice = 0;
	private int _sponsor = 0;
	private long _clanJoinExpiryTime;
	private long _clanCreateExpiryTime;
	private int _powerGrade = 0;
	private int _clanPrivileges = 0;
	private int _pledgeClass = 0;
	private int _pledgeType = 0;
	private int _lvlJoinedAcademy = 0;

	private boolean _wantsPeace;

	private int _deathPenaltyBuffLevel = 0;

	private final AtomicInteger _charges = new AtomicInteger();
	private ScheduledFuture<?> _chargeTask = null;

	private Location _currentSkillWorldPosition;

	private L2AccessLevel _accessLevel;

	private boolean _stopexp = false; // stop xp
	private boolean _messageRefusal = false; // message refusal mode
	private boolean _tradeRefusal = false; // Trade refusal
	private boolean _exchangeRefusal = false; // Exchange refusal
	private boolean _isPartyInRefuse = false; // Party Refusal Mode

	private L2Party _party;

	private L2PcInstance _activeRequester;
	private long _requestExpireTime = 0;
	private final L2Request _request = new L2Request(this);

	private ItemInstance _arrowItem;

	private ScheduledFuture<?> _protectTask = null;

	private long _recentFakeDeathEndTime = 0;
	private boolean _isFakeDeath;

	private Weapon _fistsWeaponItem;

	private final Map<Integer, String> _chars = new HashMap<>();

	private int _expertiseIndex;
	private int _expertiseArmorPenalty = 0;
	private boolean _expertiseWeaponPenalty = false;

	private ItemInstance _activeEnchantItem = null;

	protected boolean _inventoryDisable = false;

	protected Map<Integer, L2CubicInstance> _cubics = new ConcurrentHashMap<>();

	protected Set<Integer> _activeSoulShots = new CopyOnWriteArraySet<>();

	private final int _loto[] = new int[5];
	private final int _race[] = new int[2];

	private final BlockList _blockList = new BlockList(this);

	private int _team = 0;

	private int _alliedVarkaKetra = 0; // lvl of alliance with ketra orcs or varka silenos, used in quests and aggro checks [-5,-1] varka, 0 neutral, [1,5] ketra

	private Location _fishingLoc;
	private ItemInstance _lure = null;
	private L2Fishing _fishCombat;
	private FishData _fish;

	private final List<String> _validBypass = new ArrayList<>();
	private final List<String> _validBypass2 = new ArrayList<>();

	private Forum _forumMail;
	private Forum _forumMemo;

	private boolean _canFeed;
	private int _customMountVisualIndex = -1;
	private int _mountSavedRightHand;
	private int _mountSavedLeftHand;
	private L2PetData _data;
	private L2PetLevelData _leveldata;
	private int _controlItemId;
	private int _curFeed;
	protected Future<?> _mountFeedTask;
	private ScheduledFuture<?> _dismountTask;

	private int _siegeSide; // The id of castle which the L2PcInstance is registered for siege
	private boolean _isInSiege;

	private final SkillUseHolder _currentSkill = new SkillUseHolder();
	private final SkillUseHolder _currentPetSkill = new SkillUseHolder();
	private final SkillUseHolder _queuedSkill = new SkillUseHolder();

	private int _cursedWeaponEquippedId = 0;

	private int _reviveRequested = 0;
	private double _revivePower = 0;
	private boolean _revivePet = false;

	private double _cpUpdateIncCheck = .0;
	private double _cpUpdateDecCheck = .0;
	private double _cpUpdateInterval = .0;
	private double _mpUpdateIncCheck = .0;
	private double _mpUpdateDecCheck = .0;
	private double _mpUpdateInterval = .0;

	private volatile int _clientX;
	private volatile int _clientY;
	private volatile int _clientZ;
	private volatile int _clientHeading;

	private int _mailPosition;

	private static final int FALLING_VALIDATION_DELAY = 10000;
	private volatile long _fallingTimestamp = 0;

	ScheduledFuture<?> _shortBuffTask = null;
	private int _shortBuffTaskSkillId = 0;

	private boolean _married = false;
	private int _coupleId = 0;
	private boolean _marryrequest = false;
	private int _requesterId = 0;

	/**Quake System*/
	// Interlude chat channel: CRITICAL_ANNOUNCE.
	private static final int KILL_STREAK_ANNOUNCE_CHANNEL = 18;
	private int quakeSystem = 0;
	private int quakeSoundSystem = 0;

	private final SummonRequest _summonRequest = new SummonRequest();

	private final GatesRequest _gatesRequest = new GatesRequest();

	private int _countKillsInSiege;

	protected class ShortBuffTask implements Runnable
	{
		@Override
		public void run()
		{
			sendPacket(new ShortBuffStatusUpdate(0, 0, 0));
			setShortBuffTaskSkillId(0);
		}
	}

	protected static class SummonRequest
	{
		private L2PcInstance _target = null;
		private L2Skill _skill = null;

		public void setTarget(L2PcInstance destination, L2Skill skill)
		{
			_target = destination;
			_skill = skill;
		}

		public L2PcInstance getTarget()
		{
			return _target;
		}

		public L2Skill getSkill()
		{
			return _skill;
		}
	}

	protected static class GatesRequest
	{
		private L2DoorInstance _target = null;

		public void setTarget(L2DoorInstance door)
		{
			_target = door;
		}

		public L2DoorInstance getDoor()
		{
			return _target;
		}
	}

	public void gatesRequest(L2DoorInstance door)
	{
		_gatesRequest.setTarget(door);
	}

	public void gatesAnswer(int answer, int type)
	{
		if (_gatesRequest.getDoor() == null)
			return;

		if (answer == 1 && getTarget() == _gatesRequest.getDoor() && type == 1)
			_gatesRequest.getDoor().openMe();
		else if (answer == 1 && getTarget() == _gatesRequest.getDoor() && type == 0)
			_gatesRequest.getDoor().closeMe();

		_gatesRequest.setTarget(null);
	}

	public void setCountKillsInSiege(int value)
	{
		_countKillsInSiege = value;
	}

	public int getCountKillsInSiege()    
	{
		return _countKillsInSiege;
	}

	/**
	 * Create a new L2PcInstance and add it in the characters table of the database.
	 * <ul>
	 * <li>Create a new L2PcInstance with an account name</li>
	 * <li>Set the name, the Hair Style, the Hair Color and the Face type of the L2PcInstance</li>
	 * <li>Add the player in the characters table of the database</li>
	 * </ul>
	 * @param objectId Identifier of the object to initialized
	 * @param template The L2PcTemplate to apply to the L2PcInstance
	 * @param accountName The name of the L2PcInstance
	 * @param name The name of the L2PcInstance
	 * @param hairStyle The hair style Identifier of the L2PcInstance
	 * @param hairColor The hair color Identifier of the L2PcInstance
	 * @param face The face type Identifier of the L2PcInstance
	 * @param sex The sex type Identifier of the L2PcInstance
	 * @return The L2PcInstance added to the database or null
	 */
	public static L2PcInstance create(int objectId, PcTemplate template, String accountName, String name, byte hairStyle, byte hairColor, byte face, boolean sex)
	{
		// Create a new L2PcInstance with an account name
		PcAppearance app = new PcAppearance(face, hairColor, hairStyle, sex);
		L2PcInstance player = new L2PcInstance(objectId, template, accountName, app);

		// Set the name of the L2PcInstance
		player.setName(name);

		// Set the base class ID to that of the actual class ID.
		player.setBaseClass(player.getClassId());

		// Add the player in the characters table of the database
		boolean ok = player.createDb();

		if (!ok)
			return null;

		return player;
	}

	public static L2PcInstance createDummyPlayer(int objectId, String name)
	{
		// Create a new L2PcInstance with an account name
		L2PcInstance player = new L2PcInstance(objectId);
		player.setName(name);

		return player;
	}

	public String getAccountName()
	{
		if (getClient() == null)
			return getAccountNamePlayer();
		return getClient().getAccountName();
	}

	public String getAccountNamePlayer()
	{
		return _accountName;
	}

	public Map<Integer, String> getAccountChars()
	{
		return _chars;
	}

	public int getRelation(L2PcInstance target)
	{
		int result = 0;

		// karma and pvp may not be required
		if (getPvpFlag() != 0)
			result |= RelationChanged.RELATION_PVP_FLAG;
		if (getKarma() > 0)
			result |= RelationChanged.RELATION_HAS_KARMA;

		if (isClanLeader())
			result |= RelationChanged.RELATION_LEADER;

		if (getSiegeState() != 0)
		{
			result |= RelationChanged.RELATION_INSIEGE;
			if (getSiegeState() != target.getSiegeState())
				result |= RelationChanged.RELATION_ENEMY;
			else
				result |= RelationChanged.RELATION_ALLY;
			if (getSiegeState() == 1)
				result |= RelationChanged.RELATION_ATTACKER;
		}

		if (getClan() != null && target.getClan() != null)
		{
			if (target.getPledgeType() != L2Clan.SUBUNIT_ACADEMY && getPledgeType() != L2Clan.SUBUNIT_ACADEMY && target.getClan().isAtWarWith(getClan().getClanId()))
			{
				result |= RelationChanged.RELATION_1SIDED_WAR;
				if (getClan().isAtWarWith(target.getClan().getClanId()))
					result |= RelationChanged.RELATION_MUTUAL_WAR;
			}
		}
		return result;
	}

	private void initPcStatusUpdateValues()
	{
		_cpUpdateInterval = getMaxCp() / 352.0;
		_cpUpdateIncCheck = getMaxCp();
		_cpUpdateDecCheck = getMaxCp() - _cpUpdateInterval;
		_mpUpdateInterval = getMaxMp() / 352.0;
		_mpUpdateIncCheck = getMaxMp();
		_mpUpdateDecCheck = getMaxMp() - _mpUpdateInterval;
	}

	/**
	 * Constructor of L2PcInstance (use L2Character constructor).
	 * <ul>
	 * <li>Call the L2Character constructor to create an empty _skills slot and copy basic Calculator set to this L2PcInstance</li>
	 * <li>Set the name of the L2PcInstance</li>
	 * </ul>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method SET the level of the L2PcInstance to 1</B></FONT>
	 * @param objectId Identifier of the object to initialized
	 * @param template The L2PcTemplate to apply to the L2PcInstance
	 * @param accountName The name of the account including this L2PcInstance
	 * @param app The PcAppearance of the L2PcInstance
	 */
	protected L2PcInstance(int objectId, PcTemplate template, String accountName, PcAppearance app)
	{
		super(objectId, template);
		super.initCharStatusUpdateValues();
		initPcStatusUpdateValues();

		_accountName = accountName;
		_appearance = app;

		// Create an AI
		_ai = new L2PlayerAI(new AIAccessor());

		// Create a L2Radar object
		_radar = new L2Radar(this);

		antilagController = new AntilagController(this);

		// Retrieve from the database all items of this L2PcInstance and add them to _inventory
		getInventory().restore();
		getWarehouse();
		getFreight();
	}

	protected L2PcInstance(int objectId)
	{
		super(objectId, null);
		super.initCharStatusUpdateValues();
		initPcStatusUpdateValues();
	}

	@Override
	public void addFuncsToNewCharacter()
	{
		// Add L2Character functionalities.
		super.addFuncsToNewCharacter();

		addStatFunc(FuncMaxCpMul.getInstance());

		addStatFunc(FuncHennaSTR.getInstance());
		addStatFunc(FuncHennaDEX.getInstance());
		addStatFunc(FuncHennaINT.getInstance());
		addStatFunc(FuncHennaMEN.getInstance());
		addStatFunc(FuncHennaCON.getInstance());
		addStatFunc(FuncHennaWIT.getInstance());
	}

	@Override
	public void initKnownList()
	{
		setKnownList(new PcKnownList(this));
	}

	@Override
	public final PcKnownList getKnownList()
	{
		return (PcKnownList) super.getKnownList();
	}

	@Override
	public void initCharStat()
	{
		setStat(new PcStat(this));
	}

	@Override
	public final PcStat getStat()
	{
		return (PcStat) super.getStat();
	}

	@Override
	public void initCharStatus()
	{
		setStatus(new PcStatus(this));
	}

	@Override
	public final PcStatus getStatus()
	{
		return (PcStatus) super.getStatus();
	}

	@Override
	public void initPosition()
	{
		setObjectPosition(new PcPosition(this));
	}

	@Override
	public PcPosition getPosition()
	{
		return (PcPosition) super.getPosition();
	}

	public final PcAppearance getAppearance()
	{
		return _appearance;
	}

	/**
	 * @return the base L2PcTemplate link to the L2PcInstance.
	 */
	public final PcTemplate getBaseTemplate()
	{
		return CharTemplateTable.getInstance().getTemplate(_baseClass);
	}

	/** Return the L2PcTemplate link to the L2PcInstance. */
	@Override
	public final PcTemplate getTemplate()
	{
		return (PcTemplate) super.getTemplate();
	}

	public void setTemplate(ClassId newclass)
	{
		super.setTemplate(CharTemplateTable.getInstance().getTemplate(newclass));
	}

	/**
	 * Return the AI of the L2PcInstance (create it if necessary).
	 */
	@Override
	public L2CharacterAI getAI()
	{
		if (_ai == null)
		{
			synchronized (this)
			{
				if (_ai == null)
					_ai = new L2PlayerAI(new AIAccessor());
			}
		}

		return _ai;
	}

	/** Return the Level of the L2PcInstance. */
	@Override
	public final int getLevel()
	{
		return getStat().getLevel();
	}

	/**
	 * A newbie is a player reaching level 6. He isn't considered newbie at lvl 25.<br>
	 * Since IL newbie isn't anymore the first character of an account reaching that state, but any.
	 * @return True if newbie.
	 */
	public boolean isNewbie()
	{
		return getClassId().level() <= 1 && getLevel() >= 6 && getLevel() <= 25;
	}

	public void setBaseClass(int baseClass)
	{
		_baseClass = baseClass;
	}

	public void setBaseClass(ClassId classId)
	{
		_baseClass = classId.ordinal();
	}

	public boolean isInStoreMode()
	{
		return _privateStore > STORE_PRIVATE_NONE;
	}

	public boolean isInCraftMode()
	{
		return _inCraftMode;
	}

	public void isInCraftMode(boolean b)
	{
		_inCraftMode = b;
	}

	/**
	 * Manage Logout Task
	 */
	public void logout()
	{
		logout(true);
	}

	/**
	 * Manage Logout Task
	 * @param closeClient
	 */
	public void logout(boolean closeClient)
	{
		try
		{
			closeNetConnection(closeClient);
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Exception on logout(): " + e.getMessage(), e);
		}
	}

	/**
	 * @return a table containing all Common RecipeList of the L2PcInstance.
	 */
	public Collection<RecipeList> getCommonRecipeBook()
	{
		return _commonRecipeBook.values();
	}

	/**
	 * @return a table containing all Dwarf RecipeList of the L2PcInstance.
	 */
	public Collection<RecipeList> getDwarvenRecipeBook()
	{
		return _dwarvenRecipeBook.values();
	}

	/**
	 * Add a new L2RecipList to the table _commonrecipebook containing all RecipeList of the L2PcInstance.
	 * @param recipe The RecipeList to add to the _recipebook
	 */
	public void registerCommonRecipeList(RecipeList recipe)
	{
		_commonRecipeBook.put(recipe.getId(), recipe);
	}

	/**
	 * Add a new L2RecipList to the table _recipebook containing all RecipeList of the L2PcInstance.
	 * @param recipe The RecipeList to add to the _recipebook
	 */
	public void registerDwarvenRecipeList(RecipeList recipe)
	{
		_dwarvenRecipeBook.put(recipe.getId(), recipe);
	}

	/**
	 * @param recipeId The Identifier of the RecipeList to check in the player's recipe books
	 * @return <b>TRUE</b> if player has the recipe on Common or Dwarven Recipe book else returns <b>FALSE</b>
	 */
	public boolean hasRecipeList(int recipeId)
	{
		if (_dwarvenRecipeBook.containsKey(recipeId))
			return true;

		if (_commonRecipeBook.containsKey(recipeId))
			return true;

		return false;
	}

	/**
	 * Tries to remove a L2RecipList from the table _DwarvenRecipeBook or from table _CommonRecipeBook, those table contain all RecipeList of the L2PcInstance.
	 * @param recipeId The Identifier of the RecipeList to remove from the _recipebook.
	 */
	public void unregisterRecipeList(int recipeId)
	{
		if (_dwarvenRecipeBook.containsKey(recipeId))
			_dwarvenRecipeBook.remove(recipeId);
		else if (_commonRecipeBook.containsKey(recipeId))
			_commonRecipeBook.remove(recipeId);
		else
			_log.warning("Attempted to remove unknown RecipeList: " + recipeId);

		for (L2ShortCut sc : getAllShortCuts())
		{
			if (sc != null && sc.getId() == recipeId && sc.getType() == L2ShortCut.TYPE_RECIPE)
				deleteShortCut(sc.getSlot(), sc.getPage());
		}
	}

	/**
	 * @return the Id for the last talked quest NPC.
	 */
	public int getLastQuestNpcObject()
	{
		return _questNpcObject;
	}

	public void setLastQuestNpcObject(int npcId)
	{
		_questNpcObject = npcId;
	}

	/**
	 * @param name The name of the quest.
	 * @return The QuestState object corresponding to the quest name.
	 */
	public QuestState getQuestState(String name)
	{
		for (QuestState qs : _quests)
		{
			if (name.equals(qs.getQuest().getName()))
				return qs;
		}
		return null;
	}

	/**
	 * Add a QuestState to the table _quest containing all quests began by the L2PcInstance.
	 * @param qs The QuestState to add to _quest.
	 */
	public void setQuestState(QuestState qs)
	{
		_quests.add(qs);
	}

	/**
	 * Remove a QuestState from the table _quest containing all quests began by the L2PcInstance.
	 * @param qs : The QuestState to be removed from _quest.
	 */
	public void delQuestState(QuestState qs)
	{
		_quests.remove(qs);
	}

	/**
	 * @param completed : If true, include completed quests to the list.
	 * @return list of started and eventually completed quests of the player.
	 */
	public List<Quest> getAllQuests(boolean completed)
	{
		List<Quest> quests = new ArrayList<>();

		for (QuestState qs : _quests)
		{
			if (qs == null || completed && qs.isCreated() || !completed && !qs.isStarted())
				continue;

			Quest quest = qs.getQuest();
			if (quest == null || !quest.isRealQuest())
				continue;

			quests.add(quest);
		}

		return quests;
	}

	public void processQuestEvent(String questName, String event)
	{
		Quest quest = QuestManager.getInstance().getQuest(questName);
		if (quest == null)
			return;

		QuestState qs = getQuestState(questName);
		if (qs == null)
			return;

		L2Object object = L2World.getInstance().findObject(getLastQuestNpcObject());
		if (!(object instanceof L2Npc) || !isInsideRadius(object, L2Npc.INTERACTION_DISTANCE, false, false))
			return;

		L2Npc npc = (L2Npc) object;
		List<Quest> quests = npc.getTemplate().getEventQuests(QuestEventType.ON_TALK);
		if (quests != null)
		{
			for (Quest onTalk : quests)
			{
				if (onTalk == null || !onTalk.equals(quest))
					continue;

				quest.notifyEvent(event, npc, this);
				break;
			}
		}
	}

	/**
	 * Add QuestState instance that is to be notified of L2PcInstance's death.
	 * @param qs The QuestState that subscribe to this event
	 */
	public void addNotifyQuestOfDeath(QuestState qs)
	{
		if (qs == null)
			return;

		if (!_notifyQuestOfDeathList.contains(qs))
			_notifyQuestOfDeathList.add(qs);
	}

	/**
	 * Remove QuestState instance that is to be notified of L2PcInstance's death.
	 * @param qs The QuestState that subscribe to this event
	 */
	public void removeNotifyQuestOfDeath(QuestState qs)
	{
		if (qs == null)
			return;

		_notifyQuestOfDeathList.remove(qs);
	}

	/**
	 * @return A list of QuestStates which registered for notify of death of this L2PcInstance.
	 */
	public final List<QuestState> getNotifyQuestOfDeath()
	{
		return _notifyQuestOfDeathList;
	}

	/**
	 * @return A table containing all L2ShortCut of the L2PcInstance.
	 */
	public L2ShortCut[] getAllShortCuts()
	{
		return _shortCuts.getAllShortCuts();
	}

	/**
	 * @param slot The slot in wich the shortCuts is equipped
	 * @param page The page of shortCuts containing the slot
	 * @return The L2ShortCut of the L2PcInstance corresponding to the position (page-slot).
	 */
	public L2ShortCut getShortCut(int slot, int page)
	{
		return _shortCuts.getShortCut(slot, page);
	}

	/**
	 * Add a L2shortCut to the L2PcInstance _shortCuts
	 * @param shortcut The shortcut to add.
	 */
	public void registerShortCut(L2ShortCut shortcut)
	{
		_shortCuts.registerShortCut(shortcut);
	}

	/**
	 * Delete the L2ShortCut corresponding to the position (page-slot) from the L2PcInstance _shortCuts.
	 * @param slot
	 * @param page
	 */
	public void deleteShortCut(int slot, int page)
	{
		_shortCuts.deleteShortCut(slot, page);
	}

	/**
	 * Add a L2Macro to the L2PcInstance _macroses.
	 * @param macro The Macro object to add.
	 */
	public void registerMacro(L2Macro macro)
	{
		_macroses.registerMacro(macro);
	}

	/**
	 * Delete the L2Macro corresponding to the Identifier from the L2PcInstance _macroses.
	 * @param id
	 */
	public void deleteMacro(int id)
	{
		_macroses.deleteMacro(id);
	}

	/**
	 * @return all L2Macro of the L2PcInstance.
	 */
	public MacroList getMacroses()
	{
		return _macroses;
	}

	/**
	 * Set the siege state of the L2PcInstance.
	 * @param siegeState 1 = attacker, 2 = defender, 0 = not involved
	 */
	public void setSiegeState(byte siegeState)
	{
		_siegeState = siegeState;
	}

	/**
	 * @return the siege state of the L2PcInstance.
	 */
	public byte getSiegeState()
	{
		return _siegeState;
	}
	/**
	 * Informa à interface o flag PvP do próprio jogador.
	 */
	public void sendOwnStatusFlag()
	{
	    sendPacket(new ExShowScreenMessage("CustomEvent=88524 PlayerId=" + getObjectId()+ " Flag=" + getPvpFlag(),5000));
	}
	
	/**
	 * Set the PvP Flag of the L2PcInstance.
	 * @param pvpFlag 0 or 1.
	 */
	public void setPvpFlag(int pvpFlag)
	{
	    if (isPvpDropModeActive())
	    {
	        pvpFlag = 1;
	    }

	    final byte newFlag = (byte) pvpFlag;
	    final boolean changed = (_pvpFlag != newFlag);

	    _pvpFlag = newFlag;

	    if (changed)
	    {
	        sendOwnStatusFlag();
	        refreshPkContour();
	    }
	}

	@Override
	public byte getPvpFlag()
	{
		return _pvpFlag;
	}

	public void updatePvPFlag(int value)
	{
		if (isInsideZone(ZoneId.ZAKEN) || isPvpDropModeActive())
	    {
	        value = 1;
	    }

	    if (getPvpFlag() == value)
	    {
	        return;
	    }

	    setPvpFlag(value);

	    sendPacket(new UserInfo(this));

	    if (getPet() != null)
	    {
	        sendPacket(new RelationChanged(
	            getPet(),
	            getRelation(this),
	            false
	        ));
	    }

	    broadcastRelationsChanges();
	}

	@Override
	public void revalidateZone(boolean force)
	{
		// Cannot validate if not in a world region (happens during teleport)
		if (getWorldRegion() == null)
			return;

		// This function is called very often from movement code
		if (force)
			_zoneValidateCounter = 4;
		else
		{
			_zoneValidateCounter--;
			if (_zoneValidateCounter >= 0)
				return;

			_zoneValidateCounter = 4;
		}

		getWorldRegion().revalidateZones(this);
		PlayerModeManager.getInstance().enforceContext(this);

		if (Config.ALLOW_WATER)
			checkWaterState();

		if (isInsideZone(ZoneId.SIEGE))
		{
			if (_lastCompassZone == ExSetCompassZoneCode.SIEGEWARZONE2)
				return;

			_lastCompassZone = ExSetCompassZoneCode.SIEGEWARZONE2;
			sendPacket(new ExSetCompassZoneCode(ExSetCompassZoneCode.SIEGEWARZONE2));
		}
		else if (isInsideZone(ZoneId.PVP))
		{
			if (_lastCompassZone == ExSetCompassZoneCode.PVPZONE)
				return;

			_lastCompassZone = ExSetCompassZoneCode.PVPZONE;
			sendPacket(new ExSetCompassZoneCode(ExSetCompassZoneCode.PVPZONE));
		}
		else if (isIn7sDungeon())
		{
			if (_lastCompassZone == ExSetCompassZoneCode.SEVENSIGNSZONE)
				return;

			_lastCompassZone = ExSetCompassZoneCode.SEVENSIGNSZONE;
			sendPacket(new ExSetCompassZoneCode(ExSetCompassZoneCode.SEVENSIGNSZONE));
		}
		else if (isInsideZone(ZoneId.PEACE))
		{
			if (_lastCompassZone == ExSetCompassZoneCode.PEACEZONE)
				return;

			_lastCompassZone = ExSetCompassZoneCode.PEACEZONE;
			sendPacket(new ExSetCompassZoneCode(ExSetCompassZoneCode.PEACEZONE));
		}
		else
		{
			if (_lastCompassZone == ExSetCompassZoneCode.GENERALZONE)
				return;

			if (_lastCompassZone == ExSetCompassZoneCode.SIEGEWARZONE2)
				updatePvPStatus();

			_lastCompassZone = ExSetCompassZoneCode.GENERALZONE;
			sendPacket(new ExSetCompassZoneCode(ExSetCompassZoneCode.GENERALZONE));
		}
	}

	/**
	 * @return True if the L2PcInstance can Craft Dwarven Recipes.
	 */
	public boolean hasDwarvenCraft()
	{
		return getSkillLevel(L2Skill.SKILL_CREATE_DWARVEN) >= 1;
	}

	public int getDwarvenCraft()
	{
		return getSkillLevel(L2Skill.SKILL_CREATE_DWARVEN);
	}

	/**
	 * @return True if the L2PcInstance can Craft Dwarven Recipes.
	 */
	public boolean hasCommonCraft()
	{
		return getSkillLevel(L2Skill.SKILL_CREATE_COMMON) >= 1;
	}

	public int getCommonCraft()
	{
		return getSkillLevel(L2Skill.SKILL_CREATE_COMMON);
	}

	/**
	 * @return the PK counter of the L2PcInstance.
	 */
	public int getPkKills()
	{
		return _pkKills;
	}

	/**
	 * Set the PK counter of the L2PcInstance.
	 * @param pkKills A number.
	 */
	public void setPkKills(int pkKills)
	{
		_pkKills = pkKills;
	}

	/**
	 * @return The _deleteTimer of the L2PcInstance.
	 */
	public long getDeleteTimer()
	{
		return _deleteTimer;
	}

	/**
	 * Set the _deleteTimer of the L2PcInstance.
	 * @param deleteTimer Time in ms.
	 */
	public void setDeleteTimer(long deleteTimer)
	{
		_deleteTimer = deleteTimer;
	}

	/**
	 * @return The current weight of the L2PcInstance.
	 */
	public int getCurrentLoad()
	{
		return _inventory.getTotalWeight();
	}

	/**
	 * @return The date of last update of recomPoints.
	 */
	public long getLastRecomUpdate()
	{
		return _lastRecomUpdate;
	}

	public void setLastRecomUpdate(long date)
	{
		_lastRecomUpdate = date;
	}

	/**
	 * @return The number of recommandation obtained by the L2PcInstance.
	 */
	public int getRecomHave()
	{
		return _recomHave;
	}

	/**
	 * Increment the number of recommandation obtained by the L2PcInstance (Max : 255).
	 */
	protected void incRecomHave()
	{
		if (_recomHave < 255)
			_recomHave++;
	}

	/**
	 * Set the number of recommandations obtained by the L2PcInstance (Max : 255).
	 * @param value Number of recommandations obtained.
	 */
	public void setRecomHave(int value)
	{
		if (value > 255)
			_recomHave = 255;
		else if (value < 0)
			_recomHave = 0;
		else
			_recomHave = value;
	}

	/**
	 * @return The number of recommandation that the L2PcInstance can give.
	 */
	public int getRecomLeft()
	{
		return _recomLeft;
	}

	/**
	 * Increment the number of recommandation that the L2PcInstance can give.
	 */
	protected void decRecomLeft()
	{
		if (_recomLeft > 0)
			_recomLeft--;
	}

	public void giveRecom(L2PcInstance target)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement(ADD_CHAR_RECOM);
			statement.setInt(1, getObjectId());
			statement.setInt(2, target.getObjectId());
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("Could not update char recommendations: " + e);
		}

		target.incRecomHave();
		decRecomLeft();
		_recomChars.add(target.getObjectId());
	}

	public boolean canRecom(L2PcInstance target)
	{
		return !_recomChars.contains(target.getObjectId());
	}

	/**
	 * Set the exp of the L2PcInstance before a death
	 * @param exp
	 */
	public void setExpBeforeDeath(long exp)
	{
		_expBeforeDeath = exp;
	}

	public long getExpBeforeDeath()
	{
		return _expBeforeDeath;
	}

	/**
	 * Return the Karma of the L2PcInstance.
	 */
	@Override
	public int getKarma()
	{
		return _karma;
	}

	/**
	 * Set the Karma of the L2PcInstance and send StatusUpdate (broadcast).
	 * @param karma A value.
	 */
	public void setKarma(int karma)
	{
		if (karma < 0)
			karma = 0;

		if (_karma > 0 && karma == 0)
		{
			sendPacket(new UserInfo(this));
			broadcastRelationsChanges();
		}

		// send message with new karma value
		sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOUR_KARMA_HAS_BEEN_CHANGED_TO_S1).addNumber(karma));

		_karma = karma;
		broadcastKarma();
		refreshPkContour();
	}

	/**
	 * Weight Limit = (CON Modifier*69000)*Skills
	 * @return The max weight that the L2PcInstance can load.
	 */
	public int getMaxLoad()
	{
		int con = getCON();
		if (con < 1)
			return 31000;

		if (con > 59)
			return 176000;

		double baseLoad = Math.pow(1.029993928, con) * 30495.627366;
		return (int) calcStat(Stats.MAX_LOAD, baseLoad * Config.ALT_WEIGHT_LIMIT, this, null);
	}

	public int getExpertiseArmorPenalty()
	{
		return _expertiseArmorPenalty;
	}

	public boolean getExpertiseWeaponPenalty()
	{
		return _expertiseWeaponPenalty;
	}

	public int getWeightPenalty()
	{
		return _curWeightPenalty;
	}

	/**
	 * Update the overloaded status of the L2PcInstance.
	 */
	public void refreshOverloaded()
	{
		int maxLoad = getMaxLoad();
		if (maxLoad > 0)
		{
			int weightproc = getCurrentLoad() * 1000 / maxLoad;
			int newWeightPenalty;

			if (weightproc < 500)
				newWeightPenalty = 0;
			else if (weightproc < 666)
				newWeightPenalty = 1;
			else if (weightproc < 800)
				newWeightPenalty = 2;
			else if (weightproc < 1000)
				newWeightPenalty = 3;
			else
				newWeightPenalty = 4;

			if (_curWeightPenalty != newWeightPenalty)
			{
				_curWeightPenalty = newWeightPenalty;

				if (newWeightPenalty > 0)
				{
					super.addSkill(SkillTable.getInstance().getInfo(4270, newWeightPenalty));
					setIsOverloaded(getCurrentLoad() > maxLoad);
				}
				else
				{
					super.removeSkill(getKnownSkill(4270));
					setIsOverloaded(false);
				}

				sendPacket(new UserInfo(this));
				sendPacket(new EtcStatusUpdate(this));
				broadcastCharInfo();
			}
		}
	}

	/**
	 * Refresh expertise level ; weapon got one rank, when armor got 4 ranks.<br>
	 */
	public void refreshExpertisePenalty()
	{
		if (!Config.EXPERTISE_PENALTY)
			return;

		int armorPenalty = 0;
		boolean weaponPenalty = false;

		for (ItemInstance item : getInventory().getItems())
		{
			if (item != null && item.isEquipped() && item.getItemType() != EtcItemType.ARROW && item.getItem().getCrystalType().getId() > getExpertiseIndex())
			{
				if (item.isWeapon())
					weaponPenalty = true;
				else
					armorPenalty += (item.getItem().getBodyPart() == Item.SLOT_FULL_ARMOR) ? 2 : 1;
			}
		}

		armorPenalty = Math.min(armorPenalty, 4);

		// Found a different state than previous ; update it.
		if (_expertiseWeaponPenalty != weaponPenalty || _expertiseArmorPenalty != armorPenalty)
		{
			_expertiseWeaponPenalty = weaponPenalty;
			_expertiseArmorPenalty = armorPenalty;

			// Passive skill "Grade Penalty" is either granted or dropped.
			if (_expertiseWeaponPenalty || _expertiseArmorPenalty > 0)
				super.addSkill(SkillTable.getInstance().getInfo(4267, 1));
			else
				super.removeSkill(getKnownSkill(4267));

			sendSkillList();
			sendPacket(new EtcStatusUpdate(this));

			final ItemInstance weapon = getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
			if (weapon != null)
			{
				if (_expertiseWeaponPenalty)
					ItemPassiveSkillsListener.getInstance().onUnequip(0, weapon, this);
				else
					ItemPassiveSkillsListener.getInstance().onEquip(0, weapon, this);
			}
		}
	}

	private int _ArmorPenalty = 0;
	private boolean _heavy_mastery = false;
	private boolean _light_mastery = false;
	private boolean _robe_mastery = false;

	@SuppressWarnings("incomplete-switch")
	public void refreshArmorPenality()
	{
	    if (!Config.MASTERY_PENALTY || getLevel() <= Config.LEVEL_TO_GET_PENALITY)
	        return;

	    _heavy_mastery = false;
	    _light_mastery = false;
	    _robe_mastery = false;

	    L2Skill[] char_skills = getAllSkills();

	    if (char_skills == null)
	        return;

	    for (L2Skill actual_skill : char_skills)
	    {
	        if (actual_skill == null || actual_skill.getName() == null)
	            continue;

	        if (actual_skill.getName().contains("Heavy Armor Mastery"))
	            _heavy_mastery = true;

	        if (actual_skill.getName().contains("Light Armor Mastery"))
	            _light_mastery = true;

	        if (actual_skill.getName().contains("Robe Mastery"))
	            _robe_mastery = true;
	    }

	    int ArmorPenalty = 0;

	    if (_heavy_mastery || _light_mastery || _robe_mastery)
	    {
	        if (getInventory() != null && getInventory().getItems() != null)
	        {
	            for (ItemInstance item : getInventory().getItems())
	            {
	                if (item == null || !item.isEquipped() || !(item.getItem() instanceof Armor) || isInOlympiadMode())
	                    continue;

	                if (item.getItemId() == 6408)
	                    continue;

	                Armor armor_item = (Armor) item.getItem();

	                if (armor_item == null || armor_item.getItemType() == null)
	                    continue;

	                switch (armor_item.getItemType())
	                {
	                    case HEAVY:
	                        if (!_heavy_mastery)
	                            ArmorPenalty++;
	                        break;

	                    case LIGHT:
	                        if (!_light_mastery)
	                            ArmorPenalty++;
	                        break;

	                    case MAGIC:
	                        if (!_robe_mastery)
	                            ArmorPenalty++;
	                        break;
	                }
	            }
	        }
	    }

	    if (_ArmorPenalty != ArmorPenalty)
	    {
	        _ArmorPenalty = ArmorPenalty;

	        if (ArmorPenalty > 0)
	        {
	            L2Skill penaltySkill = SkillTable.getInstance().getInfo(Config.ARMOR_PENALTY_SKILL, 1);

	            if (penaltySkill != null)
	                super.addSkill(penaltySkill);
	        }
	        else
	        {
	            L2Skill penaltySkill = getKnownSkill(Config.ARMOR_PENALTY_SKILL);

	            if (penaltySkill != null)
	                super.removeSkill(penaltySkill);
	        }

	        sendSkillList();
	        sendPacket(new EtcStatusUpdate(this));
	    }
	}

	public int getMasteryPenalty()
	{
		return _ArmorPenalty;
	}

	private int _WeaponPenalty = 0;

	private boolean _blunt_mastery = false;
	private boolean _pole_mastery = false;
	private boolean _dagger_mastery = false;
	private boolean _sword_mastery = false;
	private boolean _bow_mastery = false;
	private boolean _fist_mastery = false;
	private boolean _dual_mastery = false;
	private boolean _2hands_mastery = false;

	@SuppressWarnings("incomplete-switch")
	public void refreshWeaponPenality()
	{
		if (!Config.MASTERY_WEAPON_PENALTY || this.getLevel() <= Config.LEVEL_TO_GET_WEAPON_PENALITY)
			return;

		_blunt_mastery = false;
		_bow_mastery = false;
		_dagger_mastery = false;
		_fist_mastery = false;
		_dual_mastery = false;
		_pole_mastery = false;
		_sword_mastery = false;
		_2hands_mastery = false;

		L2Skill[] char_skills = this.getAllSkills();

		for (L2Skill actual_skill : char_skills)
		{

			if (actual_skill.getName().contains("Sword Blunt Mastery"))
			{
				_sword_mastery = true;
				_blunt_mastery = true;
				continue;
			}

			if (actual_skill.getName().contains("Blunt Mastery"))
			{
				_blunt_mastery = true;
				continue;
			}

			if (actual_skill.getName().contains("Bow Mastery"))
			{
				_bow_mastery = true;
				continue;
			}

			if (actual_skill.getName().contains("Dagger Mastery"))
			{
				_dagger_mastery = true;
				continue;
			}

			if (actual_skill.getName().contains("Fist Mastery"))
			{
				_fist_mastery = true;
				continue;
			}

			if (actual_skill.getName().contains("Dual Weapon Mastery"))
			{
				_dual_mastery = true;
				continue;
			}

			if (actual_skill.getName().contains("Polearm Mastery"))
			{
				_pole_mastery = true;
				continue;
			}

			if (actual_skill.getName().contains("Two-handed Weapon Mastery"))
			{
				_2hands_mastery = true;
				continue;
			}
		}

		int WeaponPenalty = 0;

		if (!_bow_mastery && !_blunt_mastery && !_dagger_mastery && !_fist_mastery && !_dual_mastery && !_pole_mastery && !_sword_mastery && !_2hands_mastery)
		{
			WeaponPenalty = 0;
		}

		else
		{
			for (ItemInstance item : getInventory().getItems())
			{
				if (item != null && item.isEquipped() && item.getItem() instanceof Weapon && !isCursedWeaponEquipped() && !isInOlympiadMode())
				{
					// Skip Cupid Bow.
					if (item.getItemId() == 9140)
						continue;

					// Skip rsk Haste.
					if (item.getItemId() == 4761)
						continue;

					// Skip Hero Weapons.
					if (item.getItemId() >= 6611 && item.getItemId() <= 6621)
						continue;

					Weapon weap_item = (Weapon) item.getItem();

					switch (weap_item.getItemType())
					{

					case BIGBLUNT:
					case BIGSWORD:
					{
						if (!_2hands_mastery)
							WeaponPenalty++;
					}
					break;
					case BLUNT:
					{
						if (!_blunt_mastery)
							WeaponPenalty++;
					}
					break;
					case BOW:
					{
						// Bow só recebe penalidade a partir do level 76.
						if (getLevel() >= 76 && !_bow_mastery)
							WeaponPenalty++;
					}
					break;
					case DAGGER:
					{
						if (!_dagger_mastery)
							WeaponPenalty++;
					}
					break;
					case DUAL:
					{
						if (!_dual_mastery)
							WeaponPenalty++;
					}
					break;
					case DUALFIST:
					case FIST:
					{
						if (!_fist_mastery)
							WeaponPenalty++;
					}
					break;
					case POLE:
					{
						if (!_pole_mastery)
							WeaponPenalty++;
					}
					break;
					case SWORD:
					{
						if (!_sword_mastery)
							WeaponPenalty++;
					}
					break;
					}
				}
			}
		}

		if (_WeaponPenalty != WeaponPenalty)
		{
			_WeaponPenalty = WeaponPenalty;

			if (WeaponPenalty > 0)
				super.addSkill(SkillTable.getInstance().getInfo(Config.WEAPON_PENALTY_SKILL, 1));
			else
				super.removeSkill(getKnownSkill(Config.WEAPON_PENALTY_SKILL));

			sendSkillList();
			sendPacket(new EtcStatusUpdate(this));
		}
	}

	public int getMasteryWeapPenalty()
	{
		return _WeaponPenalty;
	}

	/**
	 * Equip or unequip the item.
	 * <UL>
	 * <LI>If item is equipped, shots are applied if automation is on.</LI>
	 * <LI>If item is unequipped, shots are discharged.</LI>
	 * </UL>
	 * @param item The item to charge/discharge.
	 * @param abortAttack If true, the current attack will be aborted in order to equip the item.
	 */
	public void useEquippableItem(ItemInstance item, boolean abortAttack)
	{
		ItemInstance[] items = null;
		final boolean isEquipped = item.isEquipped();
		final int oldInvLimit = getInventoryLimit();
		SystemMessage sm = null;

		if (item.getItem() instanceof Weapon)
			item.unChargeAllShots();

		if (isEquipped)
		{
			if (item.getEnchantLevel() > 0)
				sm = SystemMessage.getSystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED).addNumber(item.getEnchantLevel()).addItemName(item);
			else
				sm = SystemMessage.getSystemMessage(SystemMessageId.S1_DISARMED).addItemName(item);

			sendPacket(sm);

			int slot = getInventory().getSlotFromItem(item);
			items = getInventory().unEquipItemInBodySlotAndRecord(slot);
		}
		else
		{
			items = getInventory().equipItemAndRecord(item);

			if (item.isEquipped())
			{
				int bodyPart = item.getItem().getBodyPart();

				if (bodyPart == Item.SLOT_R_HAND || bodyPart == Item.SLOT_LR_HAND)
					removeFakeWeapon();

				if (bodyPart == Item.SLOT_L_HAND)
				{
					ItemInstance rightHandItem = getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
					if (rightHandItem != null && rightHandItem.getItem().getBodyPart() == Item.SLOT_LR_HAND)
						removeFakeWeapon();
				}

				if (item.getEnchantLevel() > 0)
					sm = SystemMessage.getSystemMessage(SystemMessageId.S1_S2_EQUIPPED).addNumber(item.getEnchantLevel()).addItemName(item);
				else
					sm = SystemMessage.getSystemMessage(SystemMessageId.S1_EQUIPPED).addItemName(item);

				sendPacket(sm);

				if ((item.getItem().getBodyPart() & Item.SLOT_ALLWEAPON) != 0)
					rechargeShots(true, true);
			}
			else
				sendPacket(SystemMessageId.CANNOT_EQUIP_ITEM_DUE_TO_BAD_CONDITION);
		}
		refreshExpertisePenalty();
		refreshArmorPenality();
		refreshWeaponPenality();
		broadcastUserInfo();

		InventoryUpdate iu = new InventoryUpdate();
		iu.addItems(Arrays.asList(items));
		sendPacket(iu);

		if (abortAttack)
			abortAttack();

		if (getInventoryLimit() != oldInvLimit)
			sendPacket(new ExStorageMaxCount(this));
	}

	/**
	 * @return PvP Kills of the L2PcInstance (number of player killed during a PvP).
	 */
	public int getPvpKills()
	{
		return _pvpKills;
	}
	/**
	 * @return O número de mortes do jogador.
	 */
	public int getDeaths()
	{
	    return _deaths;
	}

	/**
	 * Define o número de mortes do jogador.
	 * @param deaths Valor a ser definido.
	 */
	public void setDeaths(int deaths)
	{
	    _deaths = deaths;
	}

	/**
	 * Incrementa em +1 o contador de mortes.
	 */
	public void addDeath()
	{
	    _deaths++;
	}
	
	/**
	 * Set PvP Kills of the L2PcInstance (number of player killed during a PvP).
	 * @param pvpKills A value.
	 */
	public void setPvpKills(int pvpKills)
	{
		_pvpKills = pvpKills;
	}

	/**
	 * @return The ClassId object of the L2PcInstance contained in L2PcTemplate.
	 */
	public ClassId getClassId()
	{
		return getTemplate().getClassId();
	}

	/**
	 * Set the template of the L2PcInstance.
	 * @param Id The Identifier of the L2PcTemplate to set to the L2PcInstance
	 */
	public void setClassId(int Id)
	{
		if (!_subclassLock.tryLock())
			return;

		try
		{
			if (getLvlJoinedAcademy() != 0 && _clan != null && PlayerClass.values()[Id].getLevel() == ClassLevel.Third)
			{
				if (getLvlJoinedAcademy() <= 16)
					_clan.addReputationScore(400);
				else if (getLvlJoinedAcademy() >= 39)
					_clan.addReputationScore(170);
				else
					_clan.addReputationScore((400 - (getLvlJoinedAcademy() - 16) * 10));

				setLvlJoinedAcademy(0);

				// Oust pledge member from the academy, because he has finished his 2nd class transfer.
				_clan.broadcastToOnlineMembers(new PledgeShowMemberListDelete(getName()), SystemMessage.getSystemMessage(SystemMessageId.CLAN_MEMBER_S1_EXPELLED).addString(getName()));
				_clan.removeClanMember(getObjectId(), 0);
				sendPacket(SystemMessageId.ACADEMY_MEMBERSHIP_TERMINATED);

				// receive graduation gift : academy circlet
				addItem("Gift", 8181, 1, this, true);
			}

			if (isSubClassActive())
				getSubClasses().get(_classIndex).setClassId(Id);

			if (Config.SHOW_EFFECT_CLASS)
			{
				broadcastPacket(new MagicSkillUse(this, this, 5103, 1, 1000, 0));
			}

			setClassTemplate(Id);

			if (getClassId().level() == 3)
				sendPacket(SystemMessageId.THIRD_CLASS_TRANSFER);
			else
				sendPacket(SystemMessageId.CLASS_TRANSFER);

			// Update class icon in party and clan
			if (isInParty())
				getParty().broadcastToPartyMembers(new PartySmallWindowUpdate(this));

			if (getClan() != null)
				getClan().broadcastToOnlineMembers(new PledgeShowMemberListUpdate(this));

			if (Config.AUTO_LEARN_SKILLS && !isAio())
				rewardSkills();
		}
		finally
		{
			_subclassLock.unlock();
		}
	}

	/**
	 * @return the Experience of the L2PcInstance.
	 */
	public long getExp()
	{
		return getStat().getExp();
	}

	public void setActiveEnchantItem(ItemInstance scroll)
	{
		_activeEnchantItem = scroll;
	}

	public ItemInstance getActiveEnchantItem()
	{
		return _activeEnchantItem;
	}

	/**
	 * Set the fists weapon of the L2PcInstance (used when no weapon is equipped).
	 * @param weaponItem The fists Weapon to set to the L2PcInstance
	 */
	public void setFistsWeaponItem(Weapon weaponItem)
	{
		_fistsWeaponItem = weaponItem;
	}

	/**
	 * @return The fists weapon of the L2PcInstance (used when no weapon is equipped).
	 */
	public Weapon getFistsWeaponItem()
	{
		return _fistsWeaponItem;
	}

	/**
	 * @param classId The classId to test.
	 * @return The fists weapon of the L2PcInstance Class (used when no weapon is equipped).
	 */
	public static Weapon findFistsWeaponItem(int classId)
	{
		Weapon weaponItem = null;
		if ((classId >= 0x00) && (classId <= 0x09))
		{
			// human fighter fists
			Item temp = ItemTable.getInstance().getTemplate(246);
			weaponItem = (Weapon) temp;
		}
		else if ((classId >= 0x0a) && (classId <= 0x11))
		{
			// human mage fists
			Item temp = ItemTable.getInstance().getTemplate(251);
			weaponItem = (Weapon) temp;
		}
		else if ((classId >= 0x12) && (classId <= 0x18))
		{
			// elven fighter fists
			Item temp = ItemTable.getInstance().getTemplate(244);
			weaponItem = (Weapon) temp;
		}
		else if ((classId >= 0x19) && (classId <= 0x1e))
		{
			// elven mage fists
			Item temp = ItemTable.getInstance().getTemplate(249);
			weaponItem = (Weapon) temp;
		}
		else if ((classId >= 0x1f) && (classId <= 0x25))
		{
			// dark elven fighter fists
			Item temp = ItemTable.getInstance().getTemplate(245);
			weaponItem = (Weapon) temp;
		}
		else if ((classId >= 0x26) && (classId <= 0x2b))
		{
			// dark elven mage fists
			Item temp = ItemTable.getInstance().getTemplate(250);
			weaponItem = (Weapon) temp;
		}
		else if ((classId >= 0x2c) && (classId <= 0x30))
		{
			// orc fighter fists
			Item temp = ItemTable.getInstance().getTemplate(248);
			weaponItem = (Weapon) temp;
		}
		else if ((classId >= 0x31) && (classId <= 0x34))
		{
			// orc mage fists
			Item temp = ItemTable.getInstance().getTemplate(252);
			weaponItem = (Weapon) temp;
		}
		else if ((classId >= 0x35) && (classId <= 0x39))
		{
			// dwarven fists
			Item temp = ItemTable.getInstance().getTemplate(247);
			weaponItem = (Weapon) temp;
		}

		return weaponItem;
	}

	/**
	 * This method is kinda polymorph :
	 * <ul>
	 * <li>it gives proper Expertise, Dwarven && Common Craft skill level ;</li>
	 * <li>it controls the Lucky skill (remove at lvl 10) ;</li>
	 * <li>it finally sends the skill list.</li>
	 * </ul>
	 */
	public void rewardSkills()
	{
		// Get the Level of the L2PcInstance
		int lvl = getLevel();

		// Remove the Lucky skill once reached lvl 10.
		if (getSkillLevel(L2Skill.SKILL_LUCKY) > 0 && lvl >= 10)
			removeSkill(FrequentSkill.LUCKY.getSkill());

		// Calculate the current higher Expertise of the L2PcInstance
		for (int i = 0; i < EXPERTISE_LEVELS.length; i++)
		{
			if (lvl >= EXPERTISE_LEVELS[i])
				setExpertiseIndex(i);
		}

		// Add the Expertise skill corresponding to its Expertise level
		if (getExpertiseIndex() > 0)
		{
			L2Skill skill = SkillTable.getInstance().getInfo(239, getExpertiseIndex());
			addSkill(skill, true);
		}

		// Active skill dwarven craft
		if (getSkillLevel(1321) < 1 && getClassId().equalsOrChildOf(ClassId.dwarvenFighter))
		{
			L2Skill skill = FrequentSkill.DWARVEN_CRAFT.getSkill();
			addSkill(skill, true);
		}

		// Active skill common craft
		if (getSkillLevel(1322) < 1)
		{
			L2Skill skill = FrequentSkill.COMMON_CRAFT.getSkill();
			addSkill(skill, true);
		}

		for (int i = 0; i < COMMON_CRAFT_LEVELS.length; i++)
		{
			if (lvl >= COMMON_CRAFT_LEVELS[i] && getSkillLevel(1320) < (i + 1))
			{
				L2Skill skill = SkillTable.getInstance().getInfo(1320, (i + 1));
				addSkill(skill, true);
			}
		}

		// Auto-Learn skills if activated
		if (Config.AUTO_LEARN_SKILLS)
			giveAvailableSkills();

		sendSkillList();
	}

	/**
	 * Regive all skills which aren't saved to database, like Noble, Hero, Clan Skills.<br>
	 * <b>Do not call this on enterworld or char load.</b>.
	 */
	private void regiveTemporarySkills()
	{
		// Add noble skills if noble.
		if (isNoble())
			setNoble(true, false);

		// Add Hero skills if hero.
		if (isHero())
			setHero(true);

		// Add clan skills.
		if (getClan() != null)
		{
			getClan().addSkillEffects(this);

			if (getClan().getLevel() >= SiegeManager.MINIMUM_CLAN_LEVEL && isClanLeader())
				SiegeManager.addSiegeSkills(this);
		}

		// Reload passive skills from armors / jewels / weapons
		getInventory().reloadEquippedItems();

		// Add Death Penalty Buff Level
		restoreDeathPenaltyBuffLevel();
	}

	/**
	 * Give all available skills to the player.
	 * @return The number of given skills.
	 */
	public int giveAvailableSkills()
	{
		int result = 0;
		for (L2SkillLearn sl : SkillTreeTable.getInstance().getAllAvailableSkills(this, getClassId()))
		{
			addSkill(SkillTable.getInstance().getInfo(sl.getId(), sl.getLevel()), true);
			result++;
		}
		return result;
	}

	/**
	 * @return The Race object of the L2PcInstance.
	 */
	public Race getRace()
	{
		if (!isSubClassActive())
			return getTemplate().getRace();

		return CharTemplateTable.getInstance().getTemplate(_baseClass).getRace();
	}

	public L2Radar getRadar()
	{
		return _radar;
	}

	/**
	 * @return the SP amount of the L2PcInstance.
	 */
	public int getSp()
	{
		return getStat().getSp();
	}

	/**
	 * @param castleId The castle to check.
	 * @return True if this L2PcInstance is a clan leader in ownership of the passed castle.
	 */
	public boolean isCastleLord(int castleId)
	{
		L2Clan clan = getClan();

		// player has clan and is the clan leader, check the castle info
		if ((clan != null) && (clan.getLeader().getPlayerInstance() == this))
		{
			// if the clan has a castle and it is actually the queried castle, return true
			Castle castle = CastleManager.getInstance().getCastleByOwner(clan);
			if ((castle != null) && (castle == CastleManager.getInstance().getCastleById(castleId)))
				return true;
		}

		return false;
	}

	/**
	 * @return The Clan Identifier of the L2PcInstance.
	 */
	public int getClanId()
	{
		return _clanId;
	}

	/**
	 * @return The Clan Crest Identifier of the L2PcInstance or 0.
	 */
	public int getClanCrestId()
	{
		if (_clan != null)
			return _clan.getCrestId();

		return 0;
	}

	/**
	 * @return The Clan CrestLarge Identifier or 0
	 */
	public int getClanCrestLargeId()
	{
		if (_clan != null)
			return _clan.getCrestLargeId();

		return 0;
	}

	public long getClanJoinExpiryTime()
	{
		return _clanJoinExpiryTime;
	}

	public void setClanJoinExpiryTime(long time)
	{
		_clanJoinExpiryTime = time;
	}

	public long getClanCreateExpiryTime()
	{
		return _clanCreateExpiryTime;
	}

	public void setClanCreateExpiryTime(long time)
	{
		_clanCreateExpiryTime = time;
	}

	public void setOnlineTime(long time)
	{
		_onlineTime = time;
		_onlineBeginTime = System.currentTimeMillis();
	}

	/**
	 * Return the PcInventory Inventory of the L2PcInstance contained in _inventory.
	 */
	@Override
	public PcInventory getInventory()
	{
		return _inventory;
	}

	/**
	 * Delete a ShortCut of the L2PcInstance _shortCuts.
	 * @param objectId The shortcut id.
	 */
	public void removeItemFromShortCut(int objectId)
	{
		_shortCuts.deleteShortCutByObjectId(objectId);
	}

	/**
	 * @return True if the L2PcInstance is sitting.
	 */
	public boolean isSitting()
	{
		return _waitTypeSitting;
	}

	/**
	 * Set _waitTypeSitting to given value.
	 * @param state A boolean.
	 */
	public void setIsSitting(boolean state)
	{
		_waitTypeSitting = state;
	}

	/**
	 * Sit down the L2PcInstance, set the AI Intention to REST and send ChangeWaitType packet (broadcast)
	 */
	public void sitDown()
	{
		sitDown(true);
	}

	public void sitDown(boolean checkCast)
	{
		if (checkCast && isCastingNow())
			return;

		if (!_waitTypeSitting && !isAttackingDisabled() && !isOutOfControl() && !isImmobilized())
		{
			breakAttack();
			setIsSitting(true);
			broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_SITTING));

			// Schedule a sit down task to wait for the animation to finish
			getAI().setIntention(CtrlIntention.REST);
			ThreadPoolManager.getInstance().scheduleGeneral(new SitDownTask(), 2500);
			setIsParalyzed(true);
		}
	}

	protected class SitDownTask implements Runnable
	{
		@Override
		public void run()
		{
			setIsParalyzed(false);
		}
	}

	protected class StandUpTask implements Runnable
	{
		@Override
		public void run()
		{
			setIsSitting(false);
			setIsParalyzed(false);
			getAI().setIntention(CtrlIntention.IDLE);
		}
	}

	/**
	 * Stand up the L2PcInstance, set the AI Intention to IDLE and send ChangeWaitType packet (broadcast)
	 */
	public void standUp()
	{
		if (_waitTypeSitting && !isInStoreMode() && !isAlikeDead() && !isParalyzed())
		{
			if (_effects.isAffected(L2EffectFlag.RELAXING))
				stopEffects(L2EffectType.RELAXING);

			broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_STANDING));
			// Schedule a stand up task to wait for the animation to finish
			ThreadPoolManager.getInstance().scheduleGeneral(new StandUpTask(), 2500);
			setIsParalyzed(true);
		}
	}

	/**
	 * Stands up and close any opened shop window, if any.
	 */
	public void forceStandUp()
	{
		// Cancels any shop types.
		if (isInStoreMode())
		{
			setPrivateStoreType(STORE_PRIVATE_NONE);
			broadcastUserInfo();
		}

		// Stand up.
		standUp();
	}

	/**
	 * Used to sit or stand. If not possible, queue the action.
	 * @param target The target, used for thrones types.
	 * @param sittingState The sitting state, inheritated from packet or player status.
	 */
	public void tryToSitOrStand(final L2Object target, final boolean sittingState)
	{
		if (isFakeDeath())
		{
			stopFakeDeath(true);
			return;
		}

		final boolean isThrone = target instanceof L2StaticObjectInstance && ((L2StaticObjectInstance) target).getType() == 1;

		// Player wants to sit on a throne but is out of radius, move to the throne delaying the sit action.
		if (isThrone && !sittingState && !isInsideRadius(target, L2Npc.INTERACTION_DISTANCE, false, false))
		{
			getAI().setIntention(CtrlIntention.MOVE_TO, new L2CharPosition(target.getX(), target.getY(), target.getZ(), 0));

			NextAction nextAction = new NextAction(CtrlEvent.EVT_ARRIVED, CtrlIntention.MOVE_TO, new NextActionCallback()
			{
				@Override
				public void doWork()
				{
					if (getMountType() != 0)
						return;

					sitDown();
					broadcastPacket(new ChairSit(L2PcInstance.this, ((L2StaticObjectInstance) target).getStaticObjectId()));
				}
			});

			// Binding next action to AI.
			getAI().setNextAction(nextAction);
			return;
		}

		// Player isn't moving, sit directly.
		if (!isMoving())
		{
			if (getMountType() != 0)
				return;

			if (sittingState)
				standUp();
			else
			{
				sitDown();

				if (isThrone && isInsideRadius(target, L2Npc.INTERACTION_DISTANCE, false, false))
					broadcastPacket(new ChairSit(this, ((L2StaticObjectInstance) target).getStaticObjectId()));
			}
		}
		// Player is moving, wait the current action is done, then sit.
		else
		{
			NextAction nextAction = new NextAction(CtrlEvent.EVT_ARRIVED, CtrlIntention.MOVE_TO, new NextActionCallback()
			{
				@Override
				public void doWork()
				{
					if (getMountType() != 0)
						return;

					if (sittingState)
						standUp();
					else
					{
						sitDown();

						if (isThrone && isInsideRadius(target, L2Npc.INTERACTION_DISTANCE, false, false))
							broadcastPacket(new ChairSit(L2PcInstance.this, ((L2StaticObjectInstance) target).getStaticObjectId()));
					}
				}
			});

			// Binding next action to AI.
			getAI().setNextAction(nextAction);
		}
	}

	/**
	 * @return The PcWarehouse object of the L2PcInstance.
	 */
	public PcWarehouse getWarehouse()
	{
		if (_warehouse == null)
		{
			_warehouse = new PcWarehouse(this);
			_warehouse.restore();
		}
		return _warehouse;
	}

	/**
	 * Free memory used by Warehouse
	 */
	public void clearWarehouse()
	{
		if (_warehouse != null)
			_warehouse.deleteMe();

		_warehouse = null;
	}

	/**
	 * @return The PcFreight object of the L2PcInstance.
	 */
	public PcFreight getFreight()
	{
		if (_freight == null)
		{
			_freight = new PcFreight(this);
			_freight.restore();
		}
		return _freight;
	}

	/**
	 * Free memory used by Freight
	 */
	public void clearFreight()
	{
		if (_freight != null)
			_freight.deleteMe();

		_freight = null;
	}

	/**
	 * @param objectId The id of the owner.
	 * @return deposited PcFreight object for the objectId or create new if not existing.
	 */
	public PcFreight getDepositedFreight(int objectId)
	{
		for (PcFreight freight : _depositedFreight)
		{
			if (freight != null && freight.getOwnerId() == objectId)
				return freight;
		}

		PcFreight freight = new PcFreight(null);
		freight.doQuickRestore(objectId);
		_depositedFreight.add(freight);
		return freight;
	}

	/**
	 * Clear memory used by deposited freight
	 */
	public void clearDepositedFreight()
	{
		for (PcFreight freight : _depositedFreight)
		{
			if (freight != null)
				freight.deleteMe();
		}
		_depositedFreight.clear();
	}

	/**
	 * @return The Adena amount of the L2PcInstance.
	 */
	public int getAdena()
	{
		return _inventory.getAdena();
	}

	/**
	 * @return The Ancient Adena amount of the L2PcInstance.
	 */
	public int getAncientAdena()
	{
		return _inventory.getAncientAdena();
	}

	/**
	 * Add adena to Inventory of the L2PcInstance and send InventoryUpdate packet to the L2PcInstance.
	 * @param process String Identifier of process triggering this action
	 * @param count int Quantity of adena to be added
	 * @param reference L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage boolean Specifies whether to send message to Client about this action
	 */
	public void addAdena(String process, int count, L2Object reference, boolean sendMessage)
	{
		if (sendMessage)
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.EARNED_S1_ADENA).addNumber(count));


		if (count > 0)
		{
			_inventory.addAdena(process, count, this, reference);

			InventoryUpdate iu = new InventoryUpdate();

			if (_inventory.getAdenaInstance() != null)
				iu.addModifiedItem(_inventory.getAdenaInstance());
			else
				iu.addItem(_inventory.getAdenaInstance());

			sendPacket(iu);
		}
	}

	/**
	 * Reduce adena in Inventory of the L2PcInstance and send InventoryUpdate packet to the L2PcInstance.
	 * @param process String Identifier of process triggering this action
	 * @param count int Quantity of adena to be reduced
	 * @param reference L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	public boolean reduceAdena(String process, int count, L2Object reference, boolean sendMessage)
	{
		if (count > getAdena())
		{
			if (sendMessage)
				sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);

			return false;
		}

		if (count > 0)
		{
			ItemInstance adenaItem = _inventory.getAdenaInstance();
			if (!_inventory.reduceAdena(process, count, this, reference))
				return false;

			// Send update packet
			InventoryUpdate iu = new InventoryUpdate();
			iu.addItem(adenaItem);
			sendPacket(iu);

			if (sendMessage)
				sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_DISAPPEARED_ADENA).addNumber(count));
		}
		return true;
	}

	/**
	 * Add ancient adena to Inventory of the L2PcInstance and send InventoryUpdate packet to the L2PcInstance.
	 * @param process String Identifier of process triggering this action
	 * @param count int Quantity of ancient adena to be added
	 * @param reference L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage boolean Specifies whether to send message to Client about this action
	 */
	public void addAncientAdena(String process, int count, L2Object reference, boolean sendMessage)
	{
		if (sendMessage)
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.EARNED_S2_S1_S).addItemName(PcInventory.ANCIENT_ADENA_ID).addNumber(count));

		if (count > 0)
		{
			_inventory.addAncientAdena(process, count, this, reference);

			InventoryUpdate iu = new InventoryUpdate();
			iu.addItem(_inventory.getAncientAdenaInstance());
			sendPacket(iu);
		}
	}

	/**
	 * Reduce ancient adena in Inventory of the L2PcInstance and send InventoryUpdate packet to the L2PcInstance.
	 * @param process String Identifier of process triggering this action
	 * @param count int Quantity of ancient adena to be reduced
	 * @param reference L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	public boolean reduceAncientAdena(String process, int count, L2Object reference, boolean sendMessage)
	{
		if (count > getAncientAdena())
		{
			if (sendMessage)
				sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);

			return false;
		}

		if (count > 0)
		{
			ItemInstance ancientAdenaItem = _inventory.getAncientAdenaInstance();
			if (!_inventory.reduceAncientAdena(process, count, this, reference))
				return false;

			InventoryUpdate iu = new InventoryUpdate();
			iu.addItem(ancientAdenaItem);
			sendPacket(iu);

			if (sendMessage)
			{
				if (count > 1)
					sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S2_S1_DISAPPEARED).addItemName(PcInventory.ANCIENT_ADENA_ID).addItemNumber(count));
				else
					sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_DISAPPEARED).addItemName(PcInventory.ANCIENT_ADENA_ID));
			}
		}
		return true;
	}

	/**
	 * Adds item to inventory and send InventoryUpdate packet to the L2PcInstance.
	 * @param process String Identifier of process triggering this action
	 * @param item ItemInstance to be added
	 * @param reference L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage boolean Specifies whether to send message to Client about this action
	 */
	public void addItem(String process, ItemInstance item, L2Object reference, boolean sendMessage)
	{
		if (item.getCount() > 0)
		{
			// Sends message to client if requested
			if (sendMessage)
			{
				if (item.getCount() > 1)
					sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_PICKED_UP_S2_S1).addItemName(item).addNumber(item.getCount()));
				else if (item.getEnchantLevel() > 0)
					sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_PICKED_UP_A_S1_S2).addNumber(item.getEnchantLevel()).addItemName(item));
				else
					sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_PICKED_UP_S1).addItemName(item));
			}

			// Add the item to inventory
			ItemInstance newitem = _inventory.addItem(process, item, this, reference);

			// Send inventory update packet
			InventoryUpdate playerIU = new InventoryUpdate();
			playerIU.addItem(newitem);
			sendPacket(playerIU);

			// Update current load as well
			StatusUpdate su = new StatusUpdate(this);
			su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
			sendPacket(su);

			// If over capacity, drop the item
			if (!_inventory.validateCapacity(0, item.isQuestItem()) && newitem.isDropable() && (!newitem.isStackable() || newitem.getLastChange() != ItemInstance.MODIFIED))
				dropItem("InvDrop", newitem, null, true, true);
			// Cursed Weapon
			else if (CursedWeaponsManager.getInstance().isCursed(newitem.getItemId()))
				CursedWeaponsManager.getInstance().activate(this, newitem);
			// If you pickup arrows and a bow is equipped, try to equip them if no arrows is currently equipped.
			else if (item.getItem().getItemType() == EtcItemType.ARROW && getAttackType() == WeaponType.BOW && getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND) == null)
				checkAndEquipArrows();
		}
	}

	/**
	 * Adds item to Inventory and send InventoryUpdate packet to the L2PcInstance.
	 * @param process String Identifier of process triggering this action
	 * @param itemId int Item Identifier of the item to be added
	 * @param count int Quantity of items to be added
	 * @param reference L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage boolean Specifies whether to send message to Client about this action
	 * @return The created ItemInstance.
	 */
	public ItemInstance addItem(String process, int itemId, int count, L2Object reference, boolean sendMessage)
	{
		if (count > 0)
		{
			// Retrieve the template of the item.
			final Item item = ItemTable.getInstance().getTemplate(itemId);
			if (item == null)
			{
				_log.log(Level.SEVERE, "Item id " + itemId + "doesn't exist, so it can't be added.");
				return null;
			}

			// Sends message to client if requested.
			if (sendMessage && ((!isCastingNow() && item.getItemType() == EtcItemType.HERB) || item.getItemType() != EtcItemType.HERB))
			{
				if (count > 1)
				{
					if (process.equalsIgnoreCase("Sweep") || process.equalsIgnoreCase("Quest"))
						sendPacket(SystemMessage.getSystemMessage(SystemMessageId.EARNED_S2_S1_S).addItemName(itemId).addItemNumber(count));
					else
						sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_PICKED_UP_S2_S1).addItemName(itemId).addItemNumber(count));

					if (Config.LISTID_DROP.contains(item.getItemId()))
					{
						Announcement.AnnouncePikup(getName() + " obtained " + count +" " + ItemTable.getInstance().getTemplate(itemId).getName());
						DropsLog.Log(getName(), ItemTable.getInstance().getTemplate(itemId).getName());
					}
				}
				else
				{
					if (process.equalsIgnoreCase("Sweep") || process.equalsIgnoreCase("Quest"))
						sendPacket(SystemMessage.getSystemMessage(SystemMessageId.EARNED_ITEM_S1).addItemName(itemId));
					else
						sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_PICKED_UP_S1).addItemName(itemId));

					if (Config.LISTID_DROP.contains(item.getItemId()))
					{
						Announcement.AnnouncePikup(getName() + " obtained " + ItemTable.getInstance().getTemplate(itemId).getName());
						DropsLog.Log(getName(), ItemTable.getInstance().getTemplate(itemId).getName());
					}
				}
			}

			// If the item is herb type, dont add it to inventory.
			if (item.getItemType() == EtcItemType.HERB)
			{
				final ItemInstance herb = new ItemInstance(0, itemId);

				final IItemHandler handler = ItemHandler.getInstance().getItemHandler(herb.getEtcItem());
				if (handler != null)
					handler.useItem(this, herb, false);
			}
			else
			{
				// Add the item to inventory
				final ItemInstance createdItem = _inventory.addItem(process, itemId, count, this, reference);

				// If over capacity, drop the item
				if (!_inventory.validateCapacity(0, item.isQuestItem()) && createdItem.isDropable() && (!createdItem.isStackable() || createdItem.getLastChange() != ItemInstance.MODIFIED))
					dropItem("InvDrop", createdItem, null, true);
				// Cursed Weapon
				else if (CursedWeaponsManager.getInstance().isCursed(createdItem.getItemId()))
					CursedWeaponsManager.getInstance().activate(this, createdItem);
				// If you pickup arrows and a bow is equipped, try to equip them if no arrows is currently equipped.
				else if (item.getItemType() == EtcItemType.ARROW && getAttackType() == WeaponType.BOW && getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND) == null)
					checkAndEquipArrows();

				return createdItem;
			}
		}
		return null;
	}

	/**
	 * Destroy item from inventory and send InventoryUpdate packet to the L2PcInstance.
	 * @param process String Identifier of process triggering this action
	 * @param item ItemInstance to be destroyed
	 * @param reference L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	public boolean destroyItem(String process, ItemInstance item, L2Object reference, boolean sendMessage)
	{
		return this.destroyItem(process, item, item.getCount(), reference, sendMessage);
	}

	/**
	 * Destroy item from inventory and send InventoryUpdate packet to the L2PcInstance.
	 * @param process String Identifier of process triggering this action
	 * @param item ItemInstance to be destroyed
	 * @param count int Quantity of ancient adena to be reduced
	 * @param reference L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	public boolean destroyItem(String process, ItemInstance item, int count, L2Object reference, boolean sendMessage)
	{
		item = _inventory.destroyItem(process, item, count, this, reference);

		if (item == null)
		{
			if (sendMessage)
				sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);

			return false;
		}

		// Send inventory update packet
		InventoryUpdate playerIU = new InventoryUpdate();
		playerIU.addItem(item);
		sendPacket(playerIU);

		// Update current load as well
		StatusUpdate su = new StatusUpdate(this);
		su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(su);

		// Sends message to client if requested
		if (sendMessage)
		{
			if (count > 1)
				sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S2_S1_DISAPPEARED).addItemName(item).addItemNumber(count));
			else
				sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_DISAPPEARED).addItemName(item));
		}
		return true;
	}

	/**
	 * Destroys item from inventory and send InventoryUpdate packet to the L2PcInstance.
	 * @param process String Identifier of process triggering this action
	 * @param objectId int Item Instance identifier of the item to be destroyed
	 * @param count int Quantity of items to be destroyed
	 * @param reference L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	@Override
	public boolean destroyItem(String process, int objectId, int count, L2Object reference, boolean sendMessage)
	{
		ItemInstance item = _inventory.getItemByObjectId(objectId);

		if (item == null)
		{
			if (sendMessage)
				sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);

			return false;
		}
		return this.destroyItem(process, item, count, reference, sendMessage);
	}

	/**
	 * Destroys shots from inventory without logging and only occasional saving to database. Sends InventoryUpdate packet to the L2PcInstance.
	 * @param process String Identifier of process triggering this action
	 * @param objectId int Item Instance identifier of the item to be destroyed
	 * @param count int Quantity of items to be destroyed
	 * @param reference L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	public boolean destroyItemWithoutTrace(String process, int objectId, int count, L2Object reference, boolean sendMessage)
	{
		ItemInstance item = _inventory.getItemByObjectId(objectId);

		if (item == null || item.getCount() < count)
		{
			if (sendMessage)
				sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);

			return false;
		}

		return this.destroyItem(null, item, count, reference, sendMessage);
	}

	/**
	 * Destroy item from inventory by using its <B>itemId</B> and send InventoryUpdate packet to the L2PcInstance.
	 * @param process String Identifier of process triggering this action
	 * @param itemId int Item identifier of the item to be destroyed
	 * @param count int Quantity of items to be destroyed
	 * @param reference L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	@Override
	public boolean destroyItemByItemId(String process, int itemId, int count, L2Object reference, boolean sendMessage)
	{
		if (itemId == 57)
			return reduceAdena(process, count, reference, sendMessage);

		ItemInstance item = _inventory.getItemByItemId(itemId);

		if (item == null || item.getCount() < count || _inventory.destroyItemByItemId(process, itemId, count, this, reference) == null)
		{
			if (sendMessage)
				sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);

			return false;
		}

		// Send inventory update packet
		InventoryUpdate playerIU = new InventoryUpdate();
		playerIU.addItem(item);
		sendPacket(playerIU);

		// Update current load as well
		StatusUpdate su = new StatusUpdate(this);
		su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(su);

		// Sends message to client if requested
		if (sendMessage)
		{
			if (count > 1)
				sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S2_S1_DISAPPEARED).addItemName(itemId).addItemNumber(count));
			else
				sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_DISAPPEARED).addItemName(itemId));
		}
		return true;
	}

	/**
	 * Transfers item to another ItemContainer and send InventoryUpdate packet to the L2PcInstance.
	 * @param process String Identifier of process triggering this action
	 * @param objectId int Item Identifier of the item to be transfered
	 * @param count int Quantity of items to be transfered
	 * @param target Inventory the Inventory target.
	 * @param reference L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return ItemInstance corresponding to the new item or the updated item in inventory
	 */
	public ItemInstance transferItem(String process, int objectId, int count, Inventory target, L2Object reference)
	{
		final ItemInstance oldItem = checkItemManipulation(objectId, count);
		if (oldItem == null)
			return null;

		final ItemInstance newItem = getInventory().transferItem(process, objectId, count, target, this, reference);
		if (newItem == null)
			return null;

		// Send inventory update packet
		InventoryUpdate playerIU = new InventoryUpdate();

		if (oldItem.getCount() > 0 && oldItem != newItem)
			playerIU.addModifiedItem(oldItem);
		else
			playerIU.addRemovedItem(oldItem);

		sendPacket(playerIU);

		// Update current load as well
		StatusUpdate playerSU = new StatusUpdate(this);
		playerSU.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(playerSU);

		// Send target update packet
		if (target instanceof PcInventory)
		{
			final L2PcInstance targetPlayer = ((PcInventory) target).getOwner();

			InventoryUpdate playerIU2 = new InventoryUpdate();
			if (newItem.getCount() > count)
				playerIU2.addModifiedItem(newItem);
			else
				playerIU2.addNewItem(newItem);
			targetPlayer.sendPacket(playerIU2);

			// Update current load as well
			playerSU = new StatusUpdate(targetPlayer);
			playerSU.addAttribute(StatusUpdate.CUR_LOAD, targetPlayer.getCurrentLoad());
			targetPlayer.sendPacket(playerSU);
		}
		else if (target instanceof PetInventory)
		{
			PetInventoryUpdate petIU = new PetInventoryUpdate();
			if (newItem.getCount() > count)
				petIU.addModifiedItem(newItem);
			else
				petIU.addNewItem(newItem);
			((PetInventory) target).getOwner().getOwner().sendPacket(petIU);
		}
		return newItem;
	}

	/**
	 * Drop item from inventory and send InventoryUpdate packet to the L2PcInstance.
	 * @param process String Identifier of process triggering this action
	 * @param item ItemInstance to be dropped
	 * @param reference L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage boolean Specifies whether to send message to Client about this action
	 * @param protectItem whether or not dropped item must be protected temporary against other players
	 * @return boolean informing if the action was successfull
	 */
	public boolean dropItem(String process, ItemInstance item, L2Object reference, boolean sendMessage, boolean protectItem)
	{
		if (item != null && _treasureHuntSession.ownsTemporary(item.getObjectId()))
			return false;
		item = _inventory.dropItem(process, item, this, reference);

		if (item == null)
		{
			if (sendMessage)
				sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);

			return false;
		}

		item.dropMe(this, getX() + Rnd.get(50) - 25, getY() + Rnd.get(50) - 25, getZ() + 20);

		if (Config.ITEM_AUTO_DESTROY_TIME > 0 && Config.DESTROY_DROPPED_PLAYER_ITEM && !Config.LIST_PROTECTED_ITEMS.contains(item.getItemId()))
		{
			if ((item.isEquipable() && Config.DESTROY_EQUIPABLE_PLAYER_ITEM) || !item.isEquipable())
				ItemsAutoDestroyTaskManager.getInstance().addItem(item);
		}

		if (Config.DESTROY_DROPPED_PLAYER_ITEM)
		{
			if (!item.isEquipable() || (item.isEquipable() && Config.DESTROY_EQUIPABLE_PLAYER_ITEM))
				item.setProtected(false);
			else
				item.setProtected(true);
		}
		else
			item.setProtected(true);

		// retail drop protection
		if (protectItem)
			item.getDropProtection().protect(this);

		// Send inventory update packet
		InventoryUpdate playerIU = new InventoryUpdate();
		playerIU.addItem(item);
		sendPacket(playerIU);

		// Update current load as well
		StatusUpdate su = new StatusUpdate(this);
		su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(su);

		// Sends message to client if requested
		if (sendMessage)
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_DROPPED_S1).addItemName(item));

		return true;
	}

	public boolean dropItem(String process, ItemInstance item, L2Object reference, boolean sendMessage)
	{
		return dropItem(process, item, reference, sendMessage, false);
	}

	/**
	 * Drop item from inventory by using its <B>objectID</B> and send InventoryUpdate packet to the L2PcInstance.
	 * @param process String Identifier of process triggering this action
	 * @param objectId int Item Instance identifier of the item to be dropped
	 * @param count int Quantity of items to be dropped
	 * @param x int coordinate for drop X
	 * @param y int coordinate for drop Y
	 * @param z int coordinate for drop Z
	 * @param reference L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage boolean Specifies whether to send message to Client about this action
	 * @param protectItem boolean Activates drop protection on that item if true
	 * @return ItemInstance corresponding to the new item or the updated item in inventory
	 */
	public ItemInstance dropItem(String process, int objectId, int count, int x, int y, int z, L2Object reference, boolean sendMessage, boolean protectItem)
	{
		if (_treasureHuntSession.ownsTemporary(objectId))
			return null;
		ItemInstance invitem = _inventory.getItemByObjectId(objectId);
		ItemInstance item = _inventory.dropItem(process, objectId, count, this, reference);

		if (item == null)
		{
			if (sendMessage)
				sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);

			return null;
		}

		item.dropMe(this, x, y, z);

		if (Config.ITEM_AUTO_DESTROY_TIME > 0 && Config.DESTROY_DROPPED_PLAYER_ITEM && !Config.LIST_PROTECTED_ITEMS.contains(item.getItemId()))
		{
			if ((item.isEquipable() && Config.DESTROY_EQUIPABLE_PLAYER_ITEM) || !item.isEquipable())
				ItemsAutoDestroyTaskManager.getInstance().addItem(item);
		}

		if (Config.DESTROY_DROPPED_PLAYER_ITEM)
		{
			if (!item.isEquipable() || (item.isEquipable() && Config.DESTROY_EQUIPABLE_PLAYER_ITEM))
				item.setProtected(false);
			else
				item.setProtected(true);
		}
		else
			item.setProtected(true);

		// retail drop protection
		if (protectItem)
			item.getDropProtection().protect(this);

		// Send inventory update packet
		InventoryUpdate playerIU = new InventoryUpdate();
		playerIU.addItem(invitem);
		sendPacket(playerIU);

		// Update current load as well
		StatusUpdate su = new StatusUpdate(this);
		su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(su);

		// Sends message to client if requested
		if (sendMessage)
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_DROPPED_S1).addItemName(item));

		return item;
	}

	public ItemInstance checkItemManipulation(int objectId, int count)
	{
		if (L2World.getInstance().findObject(objectId) == null)
			return null;

		final ItemInstance item = getInventory().getItemByObjectId(objectId);

		if (item == null || item.getOwnerId() != getObjectId())
			return null;

		if (count < 1 || (count > 1 && !item.isStackable()))
			return null;

		if (count > item.getCount())
			return null;

		// Pet is summoned and not the item that summoned the pet AND not the buggle from strider you're mounting
		if (getPet() != null && getPet().getControlItemId() == objectId || getMountObjectID() == objectId)
			return null;

		if (getActiveEnchantItem() != null && getActiveEnchantItem().getObjectId() == objectId)
			return null;

		// We cannot put a Weapon with Augmention in WH while casting (Possible Exploit)
		if (item.isAugmented() && (isCastingNow() || isCastingSimultaneouslyNow()))
			return null;

		return item;
	}

	/**
	 * Launch a task corresponding to Config time.
	 * @param protect boolean Drop timer or activate it.
	 */
	public void setProtection(boolean protect)
	{
		if (protect)
		{
			if (_protectTask == null)
			{
				if (isInsideZone(ZoneId.PEACE) || isInsideZone(ZoneId.TOWN) || isInsideZone(ZoneId.CHANGE_PVP) || isInsideZone(ZoneId.PVP))
					return;

				_protectTask = ThreadPoolManager.getInstance().scheduleGeneral(new ProtectTask(), Config.PLAYER_SPAWN_PROTECTION * 1000);
			}
		}
		else
		{
			_protectTask.cancel(true);
			_protectTask = null;
		}
		broadcastUserInfo();
	}

	public boolean isSpawnProtected()
	{
		return _protectTask != null;
	}

	protected class ProtectTask implements Runnable
	{
		@Override
		public void run()
		{
			setProtection(false);
			sendMessage("The spawn protection has ended.");
		}
	}

	/**
	 * Set protection from agro mobs when getting up from fake death, according settings.
	 * @param protect boolean Drop timer or activate it.
	 */
	public void setRecentFakeDeath(boolean protect)
	{
		_recentFakeDeathEndTime = protect ? GameTimeController.getInstance().getGameTicks() + Config.PLAYER_FAKEDEATH_UP_PROTECTION * GameTimeController.TICKS_PER_SECOND : 0;
	}

	public boolean isRecentFakeDeath()
	{
		return _recentFakeDeathEndTime > GameTimeController.getInstance().getGameTicks();
	}

	public final boolean isFakeDeath()
	{
		return _isFakeDeath;
	}

	public final void setIsFakeDeath(boolean value)
	{
		_isFakeDeath = value;
	}

	@Override
	public final boolean isAlikeDead()
	{
		if (super.isAlikeDead())
			return true;

		return isFakeDeath();
	}

	/**
	 * @return The client owner of this char.
	 */
	public L2GameClient getClient()
	{
		return _client;
	}

	public void setClient(L2GameClient client)
	{
		synchronized (_pkContourPacketLock)
		{
			_pkContourReady = false;
			_client = client;
		}
	}

	/**
	 * Close the active connection with the client.
	 * @param closeClient
	 */
	private void closeNetConnection(boolean closeClient)
	{
		L2GameClient client = _client;
		if (client != null)
		{
			if (client.isDetached() && !(this instanceof FakePlayer))
				client.cleanMe(true);
			else
			{
				if (this instanceof FakePlayer)
					return; 

				if (!client.getConnection().isClosed())
				{
					if (closeClient)
						client.close(LeaveWorld.STATIC_PACKET);
					else
						client.close(ServerClose.STATIC_PACKET);
				}
			}
		}
	}

	public Location getCurrentSkillWorldPosition()
	{
		return _currentSkillWorldPosition;
	}

	public void setCurrentSkillWorldPosition(Location worldPosition)
	{
		_currentSkillWorldPosition = worldPosition;
	}

	/**
	 * @see net.sf.l2j.gameserver.model.actor.L2Character#enableSkill(net.sf.l2j.gameserver.model.L2Skill)
	 */
	@Override
	public void enableSkill(L2Skill skill)
	{
		super.enableSkill(skill);
		_reuseTimeStamps.remove(skill.getReuseHashCode());
	}

	/**
	 * @see net.sf.l2j.gameserver.model.actor.L2Character#checkDoCastConditions(net.sf.l2j.gameserver.model.L2Skill)
	 */
	@Override
	public boolean checkDoCastConditions(L2Skill skill)
	{
		if (!super.checkDoCastConditions(skill))
			return false;

		if (skill.getSkillType() == L2SkillType.SUMMON)
		{
			if (!((L2SkillSummon) skill).isCubic() && (getPet() != null || isMounted()))
			{
				sendPacket(SystemMessageId.SUMMON_ONLY_ONE);
				return false;
			}
		}

		if (isArenaProtection() && ((isArena5x5() && ArenaConfig.bs_COUNT_5X5 == 0) || isArena3x3()) && (skill.getSkillType() == L2SkillType.RESURRECT))
		{
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED).addSkillName(skill));
			return false;
		}

		if (isArenaProtection() && !isArena5x5() && !isArena9x9() && ArenaConfig.ARENA_SKILL_LIST.contains(skill.getId()))
		{
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED).addSkillName(skill));
			return false;
		}

		if (isArenaProtection() && ArenaConfig.ARENA_DISABLE_SKILL_LIST_PERM.contains(skill.getId()))
		{
			sendMessage("Tournament: Skill disabled in tournament.");
			return false;
		}

		if (isInArenaEvent() && !isArenaAttack() && ArenaConfig.ARENA_DISABLE_SKILL_LIST.contains(skill.getId()))
		{
			sendMessage("Tournament: Wait for the battle to begin");
			return false;
		}

		// Can't use Hero and resurrect skills during Olympiad
		if (isInOlympiadMode() && (skill.isHeroSkill() || skill.getSkillType() == L2SkillType.RESURRECT))
		{
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.THIS_SKILL_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT));
			return false;
		}

		// Check if the spell uses charges
		final int charges = getCharges();
		if (skill.getMaxCharges() == 0 && charges < skill.getNumCharges())
		{
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED).addSkillName(skill));
			return false;
		}

		return true;
	}

	/**
	 * Manage actions when a player click on this L2PcInstance.<BR>
	 * <BR>
	 * <B><U> Actions on first click on the L2PcInstance (Select it)</U> :</B>
	 * <ul>
	 * <li>Set the target of the player</li>
	 * <li>Send MyTargetSelected to the player (display the select window)</li>
	 * </ul>
	 * <B><U> Actions on second click on the L2PcInstance (Follow it/Attack it/Intercat with it)</U> :</B>
	 * <ul>
	 * <li>Send MyTargetSelected to the player (display the select window)</li>
	 * <li>If this L2PcInstance has a Private Store, notify the player AI with INTERACT</li>
	 * <li>If this L2PcInstance is autoAttackable, notify the player AI with ATTACK</li>
	 * <li>If this L2PcInstance is NOT autoAttackable, notify the player AI with FOLLOW</li>
	 * </ul>
	 * @param player The player that start an action on this L2PcInstance
	 */
	@Override
	public void onAction(L2PcInstance player)
	{
		if (!MultiTvTEvent.onAction(player, getObjectId()) || !CTFEvent.onAction(player, getObjectId()) || !FOSEvent.onAction(player, getObjectId()) || !DMEvent.onAction(player, getObjectId()) || !LMEvent.onAction(player, getObjectId()) || !TvTEvent.onAction(player, getObjectId()) || !KTBEvent.onAction(player, getObjectId()))
			return;

		// Set the target of the player
		if (player.getTarget() != this)
			player.setTarget(this);
		else
		{
			// Check if this L2PcInstance has a Private Store
			if (isInStoreMode())
			{
				player.getAI().setIntention(CtrlIntention.INTERACT, this);
				return;
			}

			// Check if this L2PcInstance is autoAttackable
			if (isAutoAttackable(player))
			{
				// Player with lvl < 21 can't attack a cursed weapon holder and a cursed weapon holder can't attack players with lvl < 21
				if ((isCursedWeaponEquipped() && player.getLevel() < 21) || (player.isCursedWeaponEquipped() && getLevel() < 21))
				{
					player.sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}

				if (PathFinding.getInstance().canSeeTarget(player, this))
				{
					player.getAI().setIntention(CtrlIntention.ATTACK, this);
					player.onActionRequest();
				}
			}
			else
			{
				// avoids to stuck when clicking two or more times
				player.sendPacket(ActionFailed.STATIC_PACKET);

				if (player != this && (PathFinding.getInstance().canSeeTarget(player, this)))
					player.getAI().setIntention(CtrlIntention.FOLLOW, this);
			}
		}
	}
	
	
	@Override
	public void onActionShift(L2PcInstance player)
	{		
		
		if (player.isGM()) {
			AdminEditChar.showCharacterInfo(player, this);
		}
		
		super.onActionShift(player);
	}

	/**
	 * @param barPixels
	 * @return true if cp update should be done, false if not
	 */
	private boolean needCpUpdate(int barPixels)
	{
		double currentCp = getCurrentCp();

		if (currentCp <= 1.0 || getMaxCp() < barPixels)
			return true;

		if (currentCp <= _cpUpdateDecCheck || currentCp >= _cpUpdateIncCheck)
		{
			if (currentCp == getMaxCp())
			{
				_cpUpdateIncCheck = currentCp + 1;
				_cpUpdateDecCheck = currentCp - _cpUpdateInterval;
			}
			else
			{
				double doubleMulti = currentCp / _cpUpdateInterval;
				int intMulti = (int) doubleMulti;

				_cpUpdateDecCheck = _cpUpdateInterval * (doubleMulti < intMulti ? intMulti-- : intMulti);
				_cpUpdateIncCheck = _cpUpdateDecCheck + _cpUpdateInterval;
			}

			return true;
		}

		return false;
	}

	/**
	 * @param barPixels
	 * @return true if mp update should be done, false if not
	 */
	private boolean needMpUpdate(int barPixels)
	{
		double currentMp = getCurrentMp();

		if (currentMp <= 1.0 || getMaxMp() < barPixels)
			return true;

		if (currentMp <= _mpUpdateDecCheck || currentMp >= _mpUpdateIncCheck)
		{
			if (currentMp == getMaxMp())
			{
				_mpUpdateIncCheck = currentMp + 1;
				_mpUpdateDecCheck = currentMp - _mpUpdateInterval;
			}
			else
			{
				double doubleMulti = currentMp / _mpUpdateInterval;
				int intMulti = (int) doubleMulti;

				_mpUpdateDecCheck = _mpUpdateInterval * (doubleMulti < intMulti ? intMulti-- : intMulti);
				_mpUpdateIncCheck = _mpUpdateDecCheck + _mpUpdateInterval;
			}

			return true;
		}

		return false;
	}

	/**
	 * Send packet StatusUpdate with current HP,MP and CP to the L2PcInstance and only current HP, MP and Level to all other L2PcInstance of the Party.
	 * <ul>
	 * <li>Send StatusUpdate with current HP, MP and CP to this L2PcInstance</li>
	 * <li>Send PartySmallWindowUpdate with current HP, MP and Level to all other L2PcInstance of the Party</li>
	 * </ul>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T SEND current HP and MP to all L2PcInstance of the _statusListener</B></FONT>
	 */
	@Override
	public void broadcastStatusUpdate()
	{
		// Send StatusUpdate with current HP, MP and CP to this L2PcInstance
		StatusUpdate su = new StatusUpdate(this);
		su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
		su.addAttribute(StatusUpdate.CUR_MP, (int) getCurrentMp());
		su.addAttribute(StatusUpdate.CUR_CP, (int) getCurrentCp());
		su.addAttribute(StatusUpdate.MAX_CP, getMaxCp());
		sendPacket(su);

		final boolean needCpUpdate = needCpUpdate(352);
		final boolean needHpUpdate = needHpUpdate(352);

		// Check if a party is in progress and party window update is needed.
		if (_party != null && (needCpUpdate || needHpUpdate || needMpUpdate(352)))
			_party.broadcastToPartyMembers(this, new PartySmallWindowUpdate(this));

		if (isInOlympiadMode() && isOlympiadStart() && (needCpUpdate || needHpUpdate))
		{
			final OlympiadGameTask game = OlympiadGameManager.getInstance().getOlympiadTask(getOlympiadGameId());
			if (game != null && game.isBattleStarted())
				game.getZone().broadcastStatusUpdate(this);
		}

		// In duel, MP updated only with CP or HP
		if (isInDuel() && (needCpUpdate || needHpUpdate))
		{
			ExDuelUpdateUserInfo update = new ExDuelUpdateUserInfo(this);
			DuelManager.getInstance().broadcastToOppositeTeam(this, update);
		}
	}

	/**
	 * Broadcast informations from a user to himself and his knownlist.<BR>
	 * If player is morphed, it sends informations from the template the player is using.
	 * <ul>
	 * <li>Send a UserInfo packet (public and private data) to this L2PcInstance.</li>
	 * <li>Send a CharInfo packet (public data only) to L2PcInstance's knownlist.</li>
	 * </ul>
	 */
	public final void broadcastUserInfo()
	{
		sendPacket(new UserInfo(this));

		if (getPoly().isMorphed())
			Broadcast.toKnownPlayers(this, new AbstractNpcInfo.PcMorphInfo(this, getPoly().getNpcTemplate()));
		else
			broadcastCharInfo();
	}

	public final void broadcastCharInfo()
	{
		for (L2PcInstance player : getKnownList().getKnownType(L2PcInstance.class))
		{
			player.sendPacket(new CharInfo(this));

			final int relation = getRelation(player);
			player.sendPacket(new RelationChanged(this, relation, isAutoAttackable(player)));
			if (getPet() != null)
				player.sendPacket(new RelationChanged(getPet(), relation, isAutoAttackable(player)));
		}
	}

	public final void broadcastUserInfoHiden() 
	{
		sendPacket(new UserInfo(this));

		if (getPoly().isMorphed()) 
			Broadcast.toKnownPlayers(this, new AbstractNpcInfo.PcMorphInfo(this, getPoly().getNpcTemplate()));
		else 
			broadcastCharInfoHiden();
	}

	public final void broadcastCharInfoHiden()
	{
		for (L2PcInstance player : getKnownList().getKnownType(L2PcInstance.class))
			sendPacket(new CharInfo(player)); 
	}



	public final void broadcastUserInfoAntiLag()
	{
		sendPacket(new UserInfo(this));
		if (getPoly().isMorphed())
		{
			Broadcast.toKnownPlayers(this, new AbstractNpcInfo.PcMorphInfo(this, getPoly().getNpcTemplate()));
		}
		else
		{
			broadcastCharInfoAntiLag();
		} 
	}

	public final void broadcastCharInfoAntiLag()
	{
		for (L2PcInstance player : getKnownList().getKnownType(L2PcInstance.class))
			sendPacket(new CharInfo(player)); 
	}

	/**
	 * Broadcast player title information.
	 */
	public final void broadcastTitleInfo()
	{
		sendPacket(new UserInfo(this));
		broadcastPacket(new TitleUpdate(this));
	}

	/**
	 * @return the Alliance Identifier of the L2PcInstance.
	 */
	public int getAllyId()
	{
		if (_clan == null)
			return 0;

		return _clan.getAllyId();
	}

	public int getAllyCrestId()
	{
		if (getClanId() == 0)
			return 0;

		if (getClan().getAllyId() == 0)
			return 0;

		return getClan().getAllyCrestId();
	}

	/**
	 * Send a packet to the L2PcInstance.
	 */
	@Override
	public void sendPacket(L2GameServerPacket packet)
	{
		if (packet instanceof CharInfo)
		{
			synchronized (_pkContourPacketLock)
			{
				final L2GameClient client = _client;

				if (client != null)
				{
					sendRemotePkContour(
						((CharInfo) packet).getPkContourTarget()
					);

					client.sendPacket(packet);
				}
			}

			return;
		}

		final L2GameClient client = _client;

		if (client != null)
			client.sendPacket(packet);
	}

	/**
	 * Send SystemMessage packet.
	 * @param id SystemMessageId
	 */
	@Override
	public void sendPacket(SystemMessageId id)
	{
		sendPacket(SystemMessage.getSystemMessage(id));
	}

	/**
	 * Manage Interact Task with another L2PcInstance.<BR>
	 * Turn the character in front of the target.<BR>
	 * In case of private stores, send the related packet.
	 * @param target The L2Character targeted
	 */
	public void doInteract(L2Character target)
	{
		if (target instanceof L2PcInstance)
		{
			L2PcInstance temp = (L2PcInstance) target;
			sendPacket(new MoveToPawn(this, temp, L2Npc.INTERACTION_DISTANCE));

			switch (temp.getPrivateStoreType())
			{
			case STORE_PRIVATE_SELL:
			case STORE_PRIVATE_PACKAGE_SELL:
				sendPacket(new PrivateStoreListSell(this, temp));
				break;

			case STORE_PRIVATE_BUY:
				sendPacket(new PrivateStoreListBuy(this, temp));
				break;

			case STORE_PRIVATE_MANUFACTURE:
				sendPacket(new RecipeShopSellList(this, temp));
				break;
			}
		}
		else
		{
			// _interactTarget=null should never happen but one never knows ^^;
			if (target != null)
				target.onAction(this);
		}
	}

	/**
	 * Manage AutoLoot Task.
	 * <ul>
	 * <li>Send a System Message to the L2PcInstance : YOU_PICKED_UP_S1_ADENA or YOU_PICKED_UP_S1_S2</li>
	 * <li>Add the Item to the L2PcInstance inventory</li>
	 * <li>Send InventoryUpdate to this L2PcInstance with NewItem (use a new slot) or ModifiedItem (increase amount)</li>
	 * <li>Send StatusUpdate to this L2PcInstance with current weight</li>
	 * </ul>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : If a Party is in progress, distribute Items between party members</B></FONT>
	 * @param target The reference Object.
	 * @param item The dropped ItemHolder.
	 */
	public void doAutoLoot(L2Attackable target, ItemHolder item)
	{
		if (isInParty())
			getParty().distributeItem(this, item, false, target);
		else if (item.getId() == 57)
			addAdena("Loot", item.getCount(), target, true);
		else
			addItem("Loot", item.getId(), item.getCount(), target, true);
	}

	/**
	 * Manage Pickup Task.
	 * <ul>
	 * <li>Send StopMove to this L2PcInstance</li>
	 * <li>Remove the ItemInstance from the world and send GetItem packets</li>
	 * <li>Send a System Message to the L2PcInstance : YOU_PICKED_UP_S1_ADENA or YOU_PICKED_UP_S1_S2</li>
	 * <li>Add the Item to the L2PcInstance inventory</li>
	 * <li>Send InventoryUpdate to this L2PcInstance with NewItem (use a new slot) or ModifiedItem (increase amount)</li>
	 * <li>Send StatusUpdate to this L2PcInstance with current weight</li>
	 * </ul>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : If a Party is in progress, distribute Items between party members</B></FONT>
	 * @param object The ItemInstance to pick up
	 */
	protected void doPickupItem(L2Object object)
	{
		if (isAlikeDead() || isFakeDeath())
			return;

		// Set the AI Intention to IDLE
		getAI().setIntention(CtrlIntention.IDLE);

		// Check if the L2Object to pick up is a ItemInstance
		if (!(object instanceof ItemInstance))
		{
			// dont try to pickup anything that is not an item :)
			_log.warning(getName() + " tried to pickup a wrong target: " + object);
			return;
		}

		ItemInstance target = (ItemInstance) object;

		// Send ActionFailed to this L2PcInstance
		sendPacket(ActionFailed.STATIC_PACKET);
		sendPacket(new StopMove(this));

		synchronized (target)
		{
			// Check if the target to pick up is visible
			if (!target.isVisible())
				return;

			if (!target.getDropProtection().tryPickUp(this))
			{
				sendPacket(SystemMessage.getSystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1).addItemName(target.getItemId()));
				return;
			}

			if (((isInParty() && getParty().getLootDistribution() == L2Party.ITEM_LOOTER) || !isInParty()) && !_inventory.validateCapacity(target))
			{
				sendPacket(SystemMessageId.SLOTS_FULL);
				return;
			}

			if (getActiveTradeList() != null)
			{
				sendPacket(SystemMessageId.CANNOT_PICKUP_OR_USE_ITEM_WHILE_TRADING);
				return;
			}

			if (target.getOwnerId() != 0 && target.getOwnerId() != getObjectId() && !isInLooterParty(target.getOwnerId()))
			{
				if (target.getItemId() == 57)
					sendPacket(SystemMessage.getSystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1_ADENA).addNumber(target.getCount()));
				else if (target.getCount() > 1)
					sendPacket(SystemMessage.getSystemMessage(SystemMessageId.FAILED_TO_PICKUP_S2_S1_S).addItemName(target).addNumber(target.getCount()));
				else
					sendPacket(SystemMessage.getSystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1).addItemName(target));

				return;
			}

			if (target.getItemLootShedule() != null && (target.getOwnerId() == getObjectId() || isInLooterParty(target.getOwnerId())))
				target.resetOwnerTimer();

			if (target.isHide())
			{
				Announcement.AnnounceBox("Congratulations! You found a gift box.");
				MagicSkillUse MSU = new MagicSkillUse(this, this, 2024, 1, 1, 0);
				broadcastPacket(MSU);

				for (RewardHolder reward : Config.HIDE_ITEM_REWARDS)
				{
					if (Rnd.get(100) <= reward.getRewardChance())
						addItem("Hide Reward", reward.getRewardId(), Rnd.get(reward.getRewardMin(), reward.getRewardMax()), this, true);
				}

				Hide.cleanEvent();
				Announcement.Announce("The player " + getName() + " found a Gift Box, Congratulations!");
				target.setHide(false);
				return;
			}
			
			if (Config.LISTID_DROP.contains(target.getItemId()))
			{
				Announcement.AnnouncePikup(getName() + ": Pickup - " + target.getCount()+" " + target.getName());
				DropsLog.Log(getName(), target.getName());
			}

			// Remove the ItemInstance from the world and send GetItem packets
			target.pickupMe(this);

			if (Config.SAVE_DROPPED_ITEM) // item must be removed from ItemsOnGroundManager if is active
				ItemsOnGroundManager.getInstance().removeObject(target);
		}

		// Auto use herbs - pick up
		if (target.getItemType() == EtcItemType.HERB)
		{
			IItemHandler handler = ItemHandler.getInstance().getItemHandler(target.getEtcItem());
			if (handler != null)
				handler.useItem(this, target, false);

			ItemTable.getInstance().destroyItem("Consume", target, this, null);
		}
		// Cursed Weapons are not distributed
		else if (CursedWeaponsManager.getInstance().isCursed(target.getItemId()))
		{
			addItem("Pickup", target, null, true);
		}
		else
		{
			// if item is instance of L2ArmorType or WeaponType broadcast an "Attention" system message
			if (target.getItemType() instanceof ArmorType || target.getItemType() instanceof WeaponType)
			{
				if (target.getEnchantLevel() > 0)
				{
					SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.ATTENTION_S1_PICKED_UP_S2_S3);
					msg.addString(getName());
					msg.addNumber(target.getEnchantLevel());
					msg.addItemName(target.getItemId());
					broadcastPacket(msg, 1400);
				}
				else
				{
					SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.ATTENTION_S1_PICKED_UP_S2);
					msg.addString(getName());
					msg.addItemName(target.getItemId());
					broadcastPacket(msg, 1400);
				}
			}

			// Check if a Party is in progress
			if (isInParty())
				getParty().distributeItem(this, target);
			// Target is adena
			else if (target.getItemId() == 57 && getInventory().getAdenaInstance() != null)
			{
				addAdena("Pickup", target.getCount(), null, true);
				ItemTable.getInstance().destroyItem("Pickup", target, this, null);
			}
			// Target is regular item
			else
				addItem("Pickup", target, null, true);
		}
	}

	public boolean canOpenPrivateStore()
	{
		return !isAlikeDead() && !isInOlympiadMode() && !isMounted() && !isInsideZone(ZoneId.NO_STORE) && !isCastingNow();
	}

	public void tryOpenPrivateBuyStore()
	{
		if (canOpenPrivateStore())
		{
			if (getPrivateStoreType() == STORE_PRIVATE_BUY || getPrivateStoreType() == STORE_PRIVATE_BUY_MANAGE)
				setPrivateStoreType(STORE_PRIVATE_NONE);

			if (getPrivateStoreType() == STORE_PRIVATE_NONE)
			{
				standUp();

				setPrivateStoreType(STORE_PRIVATE_BUY_MANAGE);
				sendPacket(new PrivateStoreManageListBuy(this));
			}
		}
		else
		{
			if (isInsideZone(ZoneId.NO_STORE))
				sendPacket(SystemMessageId.NO_PRIVATE_STORE_HERE);

			sendPacket(ActionFailed.STATIC_PACKET);
		}
	}

	public void tryOpenPrivateSellStore(boolean isPackageSale)
	{
		if (canOpenPrivateStore())
		{
			if (getPrivateStoreType() == STORE_PRIVATE_SELL || getPrivateStoreType() == STORE_PRIVATE_SELL_MANAGE || getPrivateStoreType() == STORE_PRIVATE_PACKAGE_SELL)
				setPrivateStoreType(STORE_PRIVATE_NONE);

			if (getPrivateStoreType() == STORE_PRIVATE_NONE)
			{
				standUp();

				setPrivateStoreType(STORE_PRIVATE_SELL_MANAGE);
				sendPacket(new PrivateStoreManageListSell(this, isPackageSale));
			}
		}
		else
		{
			if (isInsideZone(ZoneId.NO_STORE))
				sendPacket(SystemMessageId.NO_PRIVATE_STORE_HERE);

			sendPacket(ActionFailed.STATIC_PACKET);
		}
	}

	public void tryOpenWorkshop(boolean isDwarven)
	{
		if (canOpenPrivateStore())
		{
			if (isInStoreMode())
				setPrivateStoreType(STORE_PRIVATE_NONE);

			if (getPrivateStoreType() == STORE_PRIVATE_NONE)
			{
				standUp();

				if (getCreateList() == null)
					setCreateList(new L2ManufactureList());

				sendPacket(new RecipeShopManageList(this, isDwarven));
			}
		}
		else
		{
			if (isInsideZone(ZoneId.NO_STORE))
				sendPacket(SystemMessageId.NO_PRIVATE_WORKSHOP_HERE);

			sendPacket(ActionFailed.STATIC_PACKET);
		}
	}

	/**
	 * Set a target.
	 * <ul>
	 * <li>Remove the L2PcInstance from the _statusListener of the old target if it was a L2Character</li>
	 * <li>Add the L2PcInstance to the _statusListener of the new target if it's a L2Character</li>
	 * <li>Target the new L2Object (add the target to the L2PcInstance _target, _knownObject and L2PcInstance to _KnownObject of the L2Object)</li>
	 * </ul>
	 * @param newTarget The L2Object to target
	 */
	@Override
	public void setTarget(L2Object newTarget)
	{
		if (newTarget != null)
		{
			boolean isParty = (((newTarget instanceof L2PcInstance) && isInParty() && getParty().getPartyMembers().contains(newTarget)));

			// Check if the new target is visible
			if (!isParty && (!newTarget.isVisible() || Math.abs(newTarget.getZ() - getZ()) > 1000))
				newTarget = null;
		}

		// Can't target and attack festival monsters if not participant
		if ((newTarget instanceof L2FestivalMonsterInstance) && !isFestivalParticipant())
			newTarget = null;
		// Can't target and attack rift invaders if not in the same room
		else if (isInParty() && getParty().isInDimensionalRift())
		{
			byte riftType = getParty().getDimensionalRift().getType();
			byte riftRoom = getParty().getDimensionalRift().getCurrentRoom();

			if (newTarget != null && !DimensionalRiftManager.getInstance().getRoom(riftType, riftRoom).checkIfInZone(newTarget.getX(), newTarget.getY(), newTarget.getZ()))
				newTarget = null;
		}

		// Get the current target
		L2Object oldTarget = getTarget();

		if (oldTarget != null)
		{
			if (oldTarget.equals(newTarget))
				return; // no target change

			// Remove the L2PcInstance from the _statusListener of the old target if it was a L2Character
			if (oldTarget instanceof L2Character)
				((L2Character) oldTarget).removeStatusListener(this);
		}

		// Verify if it's a static object.
		if (newTarget instanceof L2StaticObjectInstance)
		{
			sendPacket(new MyTargetSelected(newTarget.getObjectId(), getLevel()));
			sendPacket(new StaticObject((L2StaticObjectInstance) newTarget));
		}
		// Add the L2PcInstance to the _statusListener of the new target if it's a L2Character
		else if (newTarget instanceof L2Character)
		{
			final L2Character target = (L2Character) newTarget;

			target.addStatusListener(this);

			// ==================== INÍCIO DO TRECHO ADICIONADO ====================
			if (target instanceof L2PcInstance)
			{
				L2PcInstance player = (L2PcInstance) target;
				int classId = player.getClassId().getId();
				int level = player.getLevel();
				int flag = player.getPvpFlag();
				
				// Envia o evento 88521 para a interface atualizar classe, level e flag
				ExShowScreenMessage screenMessage = new ExShowScreenMessage("CustomEvent=88521 ClassId=" + classId + " Level=" + level + " Flag=" + flag, 5000);
				sendPacket(screenMessage);
			}
			// ===================== FIM DO TRECHO ADICIONADO =====================
			
			// Show the client his new target.
			if (target.isAutoAttackable(this))
			{
				// Show the client his new target.
				sendPacket(new MyTargetSelected(target.getObjectId(), getLevel() - target.getLevel()));

				// Send max/current hp.
				final StatusUpdate su = new StatusUpdate(target);
				su.addAttribute(StatusUpdate.MAX_HP, target.getMaxHp());
				su.addAttribute(StatusUpdate.CUR_HP, (int) target.getCurrentHp());
				sendPacket(su);
			}
			else
				sendPacket(new MyTargetSelected(target.getObjectId(), 0));

			Broadcast.toKnownPlayers(this, new TargetSelected(getObjectId(), newTarget.getObjectId(), getX(), getY(), getZ()));
		}

		if (newTarget == null && getTarget() != null)
		{
			broadcastPacket(new TargetUnselected(this));
			setCurrentFolkNPC(null);
		}
		else
		{
			// Rehabilitates that useful check.
			if (newTarget instanceof L2NpcInstance)
				setCurrentFolkNPC((L2Npc) newTarget);
		}

		// Target the new L2Object (add the target to the L2PcInstance _target, _knownObject and L2PcInstance to _KnownObject of the L2Object)
		_treasureHuntSession.onTargetChanged(newTarget);
		super.setTarget(newTarget);
	}

	/**
	 * Return the active weapon instance (always equipped in the right hand).
	 */
	@Override
	public ItemInstance getActiveWeaponInstance()
	{
		return getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
	}

	/**
	 * Return the active weapon item (always equipped in the right hand).
	 */
	@Override
	public Weapon getActiveWeaponItem()
	{
		ItemInstance weapon = getActiveWeaponInstance();

		if (weapon == null)
			return getFistsWeaponItem();

		return (Weapon) weapon.getItem();
	}

	public ItemInstance getChestArmorInstance()
	{
		return getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST);
	}

	public Armor getActiveChestArmorItem()
	{
		ItemInstance armor = getChestArmorInstance();

		if (armor == null)
			return null;

		return (Armor) armor.getItem();
	}

	public boolean isWearingHeavyArmor()
	{
		ItemInstance armor = getChestArmorInstance();

		if ((ArmorType) armor.getItemType() == ArmorType.HEAVY)
			return true;

		return false;
	}

	public boolean isWearingLightArmor()
	{
		ItemInstance armor = getChestArmorInstance();

		if ((ArmorType) armor.getItemType() == ArmorType.LIGHT)
			return true;

		return false;
	}

	public boolean isWearingMagicArmor()
	{
		ItemInstance armor = getChestArmorInstance();

		if ((ArmorType) armor.getItemType() == ArmorType.MAGIC)
			return true;

		return false;
	}

	public boolean isMarried()
	{
		return _married;
	}

	public void setMarried(boolean state)
	{
		_married = state;
	}

	public void setUnderMarryRequest(boolean state)
	{
		_marryrequest = state;
	}

	public boolean isUnderMarryRequest()
	{
		return _marryrequest;
	}

	public int getCoupleId()
	{
		return _coupleId;
	}

	public void setCoupleId(int coupleId)
	{
		_coupleId = coupleId;
	}

	public void setRequesterId(int requesterId)
	{
		_requesterId = requesterId;
	}

	public void EngageAnswer(int answer)
	{
		if (!_marryrequest || _requesterId == 0)
			return;

		L2PcInstance ptarget = L2World.getInstance().getPlayer(_requesterId);
		if (ptarget != null)
		{
			if (answer == 1)
			{
				// Create the couple
				CoupleManager.getInstance().createCouple(ptarget, this);

				// Then "finish the job"
				L2WeddingManagerInstance.justMarried(ptarget, this);
			}
			else
			{
				setUnderMarryRequest(false);
				sendMessage("You declined your partner's marriage request.");

				ptarget.setUnderMarryRequest(false);
				ptarget.sendMessage("Your partner declined your marriage request.");
			}
		}
	}

	/**
	 * Return the secondary weapon instance (always equipped in the left hand).
	 */
	@Override
	public ItemInstance getSecondaryWeaponInstance()
	{
		return getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
	}

	/**
	 * Return the secondary L2Item item (always equiped in the left hand).
	 */
	@Override
	public Item getSecondaryWeaponItem()
	{
		ItemInstance item = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		if (item != null)
			return item.getItem();

		return null;
	}

	/**
	 * Kill the L2Character, Apply Death Penalty, Manage gain/loss Karma and Item Drop.
	 * <ul>
	 * <li>Reduce the Experience of the L2PcInstance in function of the calculated Death Penalty</li>
	 * <li>If necessary, unsummon the Pet of the killed L2PcInstance</li>
	 * <li>Manage Karma gain for attacker and Karam loss for the killed L2PcInstance</li>
	 * <li>If the killed L2PcInstance has Karma, manage Drop Item</li>
	 * <li>Kill the L2PcInstance</li>
	 * </ul>
	 * @param killer The L2Character who attacks
	 */
	@Override
	public boolean doDie(L2Character killer)
	{
		_treasureHuntSession.finish();
		// Kill the L2PcInstance
		if (!super.doDie(killer))
			return false;

		// Every confirmed death ends the consecutive PvP sequence.
		quakeSystem = 0;

		if (isMounted())
			stopFeed();

		synchronized (this)
		{
			if (isFakeDeath())
				stopFakeDeath(true);
		}

		if (killer != null)
		{
			L2PcInstance pk = killer.getActingPlayer();

			if (pk != null && pk != this)
			{
				// --- SOMA +1 MORTE NA VÍTIMA (SEJA MORTE POR PVP OU PK) ---
				setDeaths(getDeaths() + 1);
				
				// === KILL FEED HUD ===
				KillFeedHud.notify(pk, this);
				
				// === INÍCIO DO MOD BOUNTY HUNTER ===
				boolean isInAnyEvent = TvTEvent.isPlayerParticipant(this.getObjectId()) || CTFEvent.isPlayerParticipant(this.getObjectId()) || this.isInDuel();

				if (!isInAnyEvent)
				{
				    PlayerKillsManager.getInstance().onKill(this.getObjectId(), pk.getObjectId());
				}
				// === FIM DO MOD BOUNTY HUNTER ===
				
				// === INÍCIO DO MOD GUILD MISSION ===
				if (pk.getClan() != null)
				{
				    GuildMissionManager.getInstance().onPlayerKill(pk, this.getObjectId());
				}
				// === FIM DO MOD GUILD MISSION ===
				
				CTFEvent.onKill(killer, this);
				DMEvent.onKill(killer, this);
				FOSEvent.onKill(killer, this);
				LMEvent.onKill(killer, this);
				TvTEvent.onKill(killer, this);
				MultiTvTEvent.onKill(killer, this);
			}

			KTBEvent.onDie(this);

			// Clear resurrect xp calculation
			setExpBeforeDeath(0);

			if (isCursedWeaponEquipped())
				CursedWeaponsManager.getInstance().drop(_cursedWeaponEquippedId, killer);
			else
			{
				if (pk == null || !pk.isCursedWeaponEquipped())
				{
					onDieDropItem(killer); // Check if any item should be dropped

					// if the area isn't an arena
					if (!isInArena())
					{
						// if both victim and attacker got clans & aren't academicians
						if (pk != null && pk.getClan() != null && getClan() != null && !isAcademyMember() && !pk.isAcademyMember())
						{
							// if clans got mutual war, then use the reputation calcul
							if (_clan.isAtWarWith(pk.getClanId()) && pk.getClan().isAtWarWith(_clan.getClanId()))
							{
								// when your reputation score is 0 or below, the other clan cannot acquire any reputation points
								if (getClan().getReputationScore() > 0)
									pk.getClan().addReputationScore(1);
								// when the opposing sides reputation score is 0 or below, your clans reputation score doesn't decrease
								if (pk.getClan().getReputationScore() > 0)
									_clan.takeReputationScore(1);
							}
						}
					}

					// Reduce player's xp and karma.
					//if (Config.ALT_GAME_DELEVEL && (getSkillLevel(L2Skill.SKILL_LUCKY) < 0 || getStat().getLevel() > 9))
					//	deathPenalty(pk != null && getClan() != null && pk.getClan() != null && (getClan().isAtWarWith(pk.getClanId()) || pk.getClan().isAtWarWith(getClanId())), pk != null, killer instanceof L2SiegeGuardInstance);
					if (Config.ALT_GAME_DELEVEL)
					{
						// Reduce the Experience of the L2PcInstance in function of the calculated Death Penalty
						// NOTE: deathPenalty +- Exp will update karma
						if (getSkillLevel(L2Skill.SKILL_LUCKY) < 0 || getStat().getLevel() > 9)
							deathPenalty(pk != null && getClan() != null && pk.getClan() != null && (getClan().isAtWarWith(pk.getClanId()) || pk.getClan().isAtWarWith(getClanId())), pk != null, killer instanceof L2SiegeGuardInstance);
					}
					else
						onDieUpdateKarma(); // Update karma if delevel is not allowed
				}
			}
		}

		// Unsummon Cubics
		if (!_cubics.isEmpty())
		{
			for (L2CubicInstance cubic : _cubics.values())
			{
				cubic.stopAction();
				cubic.cancelDisappear();
			}

			_cubics.clear();
		}

		if (_fusionSkill != null)
			abortCast();

		for (L2Character character : getKnownList().getKnownType(L2Character.class))
			if (character.getFusionSkill() != null && character.getFusionSkill().getTarget() == this)
				character.abortCast();

		if (isInParty() && getParty().isInDimensionalRift())
			getParty().getDimensionalRift().getDeadMemberList().add(this);

		// calculate death penalty buff
		calculateDeathPenaltyBuffLevel(killer);

		stopWaterTask();

		if (Config.ENABLE_EFFECT_ON_DIE)
		{
			ExRedSky packet = new ExRedSky(5);
			sendPacket(packet);
			sendPacket(new Earthquake(getX(), getY(), getZ(), 80, 5));
			sendPacket(new PlaySound("skillsound7.pig_skill"));
		}

		if (isPhoenixBlessed() || (isAffected(L2EffectFlag.CHARM_OF_COURAGE) && isInSiege()))
			reviveRequest(this, null, false);

		// Stop Annonce Quake On Die
		if (isKillStreak())
			setKillStreak(false);

		// Leave war legend aura if enabled
		heroConsecutiveKillCount = 0;
		if (Config.WAR_LEGEND_AURA && !_hero && isPVPHero)
		{
			setHeroAura(false);
			sendMessage("You lost War Legend State.");
		}

		// Icons update in order to get retained buffs list
		updateEffectIcons();

		AutofarmManager.INSTANCE.onDeath(this);
		AutofarmSecondaryManager.INSTANCE.onDeath(this);

		if (this instanceof FakePlayer)
		{
			FakePlayer fakePlayer = (FakePlayer) this;
			FakePlayerUtilsAI.maybeAnnounceOnDied(fakePlayer);
		}
		
		PlayerModeManager.getInstance().reset(this, true, false);
		
		return true;
	}

	private void onDieDropItem(L2Character killer)
	{
		if (killer == null || (this instanceof FakePlayer))
			return;

		L2PcInstance pk = killer.getActingPlayer();
		if (getKarma() <= 0 && pk != null && pk.getClan() != null && getClan() != null && pk.getClan().isAtWarWith(getClanId()))
			return;

		if ((!isInsideZone(ZoneId.PVP) || pk == null) && (!isGM() || Config.KARMA_DROP_GM))
		{
			boolean isKillerNpc = (killer instanceof L2Npc);
			int pkLimit = Config.KARMA_PK_LIMIT;

			int dropEquip = 0;
			int dropEquipWeapon = 0;
			int dropItem = 0;
			int dropLimit = 0;
			int dropPercent = 0;

			if (getKarma() > 0 && getPkKills() >= pkLimit)
			{
				dropPercent = Config.KARMA_RATE_DROP;
				dropEquip = Config.KARMA_RATE_DROP_EQUIP;
				dropEquipWeapon = Config.KARMA_RATE_DROP_EQUIP_WEAPON;
				dropItem = Config.KARMA_RATE_DROP_ITEM;
				dropLimit = Config.KARMA_DROP_LIMIT;
			}
			else if (isKillerNpc && getLevel() > 4 && !isFestivalParticipant())
			{
				dropPercent = Config.PLAYER_RATE_DROP;
				dropEquip = Config.PLAYER_RATE_DROP_EQUIP;
				dropEquipWeapon = Config.PLAYER_RATE_DROP_EQUIP_WEAPON;
				dropItem = Config.PLAYER_RATE_DROP_ITEM;
				dropLimit = Config.PLAYER_DROP_LIMIT;
			}

			if (dropPercent > 0 && Rnd.get(100) < dropPercent)
			{
				int dropCount = 0;
				int itemDropPercent = 0;

				for (ItemInstance itemDrop : getInventory().getItems())
				{
					// Don't drop those following things
					if (!itemDrop.isDropable() || itemDrop.isShadowItem() || itemDrop.getItemId() == 57 || itemDrop.getItem().getType2() == Item.TYPE2_QUEST || getPet() != null && getPet().getControlItemId() == itemDrop.getItemId() || Arrays.binarySearch(Config.KARMA_LIST_NONDROPPABLE_ITEMS, itemDrop.getItemId()) >= 0 || Arrays.binarySearch(Config.KARMA_LIST_NONDROPPABLE_PET_ITEMS, itemDrop.getItemId()) >= 0 || ((this instanceof FakePlayer)))
						continue;

					if (itemDrop.isEquipped())
					{
						// Set proper chance according to Item type of equipped Item
						itemDropPercent = itemDrop.getItem().getType2() == Item.TYPE2_WEAPON ? dropEquipWeapon : dropEquip;
						getInventory().unEquipItemInSlot(itemDrop.getLocationSlot());
					}
					else
						itemDropPercent = dropItem; // Item in inventory

					// NOTE: Each time an item is dropped, the chance of another item being dropped gets lesser (dropCount * 2)
					if (Rnd.get(100) < itemDropPercent)
					{
						dropItem("DieDrop", itemDrop, killer, true);

						if (++dropCount >= dropLimit)
							break;
					}
				}
			}
		}
	}

	/**
	 * On die update karma.
	 */
	private void onDieUpdateKarma()
	{
		// Karma lose for server that does not allow delevel
		if (getKarma() > 0)
		{
			// this formula seems to work relatively well:
			// baseKarma * thisLVL * (thisLVL/100)
			// Calculate the new Karma of the attacker : newKarma = baseKarma*pkCountMulti*lvlDiffMulti
			double karmaLost = Config.KARMA_LOST_BASE;
			karmaLost *= getLevel(); // multiply by char lvl
			karmaLost *= getLevel() / 100.0; // divide by 0.charLVL
			karmaLost = Math.round(karmaLost);
			if (karmaLost < 0)
				karmaLost = 1;

			// Decrease Karma of the L2PcInstance and Send it a Server->Client StatusUpdate packet with Karma and PvP Flag if necessary
			setKarma(getKarma() - (int) karmaLost);
		}
	}

	public void updateKarmaLoss(long exp)
	{
		if (!isCursedWeaponEquipped() && getKarma() > 0)
		{
			int karmaLost = Formulas.calculateKarmaLost(getLevel(), Math.round(exp));
			if (karmaLost > 0)
				setKarma(getKarma() - karmaLost);
		}
	}

	/**
	 * This method is used to update PvP counter, or PK counter / add Karma if necessary.<br>
	 * It also updates clan kills/deaths counters on siege.
	 * @param target The L2Playable victim.
	 */
	public void onKillUpdatePvPKarma(L2Playable target)
	{
		if (target == null)
			return;

		final L2PcInstance targetPlayer = target.getActingPlayer();
		if (targetPlayer == null || targetPlayer == this)
			return;

		// Don't rank up the CW if it was a summon.
		if (isCursedWeaponEquipped() && target instanceof L2PcInstance)
		{
			CursedWeaponsManager.getInstance().increaseKills(_cursedWeaponEquippedId);
			return;
		}

		// Check anti-farm
		if(!checkAntiFarm(targetPlayer))
			return;

		if (isInsideZone(ZoneId.ZONE_PVP) && Config.TOP_KILLER_PLAYER_ROUND)
			TopKillerRoundSystem.addZonePvp(this);

		// If in duel and you kill (only can kill l2summon), do nothing
		if (isInDuel() && targetPlayer.isInDuel())
			return;

		if (Config.SHOW_HP_PVP)
		{
			targetPlayer.sendPacket(new ExShowScreenMessage("You Killed By " + getName() + " Left [HP - " + getCurrentShowHpPvp() + "] [CP - " + getCurrentShowCpPvp() + "]", 4000));
			targetPlayer.sendMessage("You Killed By " + getName() + " Left [HP - " + getCurrentShowHpPvp() + "] [CP - " + getCurrentShowCpPvp() + "]");
		}

		// If in pvp zone, do nothing.
		if (isInsideZone(ZoneId.PVP) && targetPlayer.isInsideZone(ZoneId.PVP) || isInsideZone(ZoneId.HEAVY_FARM_AREA) && targetPlayer.isInsideZone(ZoneId.HEAVY_FARM_AREA) || isInsideZone(ZoneId.SPECIAL_BOSS_AREA) && targetPlayer.isInsideZone(ZoneId.SPECIAL_BOSS_AREA))
		{
			// Until the zone was a siege zone. Check also if victim was a player. Randomers aren't counted.
			if (target instanceof L2PcInstance && getSiegeState() > 0 && targetPlayer.getSiegeState() > 0 && getSiegeState() != targetPlayer.getSiegeState())
			{
				// Now check clan relations.
				final L2Clan killerClan = getClan();
				if (killerClan != null)
				{
					killerClan.setSiegeKills(killerClan.getSiegeKills() + 1);
					setCountKillsInSiege(getCountKillsInSiege() + 1);
				}

				final L2Clan targetClan = targetPlayer.getClan();
				if (targetClan != null)
					targetClan.setSiegeDeaths(targetClan.getSiegeDeaths() + 1);
			}
			if (target instanceof L2PcInstance)
			{
				// Add PvP point to attacker.
				if (Config.ALT_GIVE_PVP_IN_ARENA)
					increasePvpKills();

				if (Config.ANNOUNCE_ZONE_PVP)
				{
					SystemMessage message = new SystemMessage(SystemMessageId.S1_S2);
					message.addString(getName() + " " + Config.LIST_OF_TYPES_PVP.get(Rnd.get(Config.LIST_OF_TYPES_PVP.size())) + " " + target.getName() + " -");
					message.addZoneName(getX(), getY(), getZ());
					Broadcast.toAllOnlinePlayersPvpAnnounce(message);
					message = null;
				}
				else
				{
					SystemMessage message2 = new SystemMessage(SystemMessageId.S1_S2);
					message2.addString(getName() + " " + Config.LIST_OF_TYPES_PVP.get(Rnd.get(Config.LIST_OF_TYPES_PVP.size())) + " " + target.getName() + "");
					Broadcast.toAllOnlinePlayersPvpAnnounce(message2);
					message2 = null;
				}

				// Pvp Reward
				if (canReceivePvpReward(targetPlayer))
					 pvpReward();

				// Send UserInfo packet to attacker with its Karma and PK Counter
				sendPacket(new UserInfo(this));
			}
			return;
		}

		// Check if it's pvp (cases : regular, wars, victim is PKer)
		if (checkIfPvP(target) || (targetPlayer.getClan() != null && getClan() != null && getClan().isAtWarWith(targetPlayer.getClanId()) && targetPlayer.getClan().isAtWarWith(getClanId()) && targetPlayer.getPledgeType() != L2Clan.SUBUNIT_ACADEMY && getPledgeType() != L2Clan.SUBUNIT_ACADEMY) || (targetPlayer.getKarma() > 0 && Config.KARMA_AWARD_PK_KILL))
		{
			if (target instanceof L2PcInstance)
			{
				// Add PvP point to attacker.
				increasePvpKills();
				
			    if (isInPvpBoss() && Config.PVP_BOSS_EVENT)
			    	PvpBossManager.registerPvpKill();

				if (Config.ANNOUNCE_ZONE_PVP)
				{
					SystemMessage message = new SystemMessage(SystemMessageId.S1_S2);
					message.addString(getName() + " " + Config.LIST_OF_TYPES_PVP.get(Rnd.get(Config.LIST_OF_TYPES_PVP.size())) + " " + target.getName() + " -");
					message.addZoneName(getX(), getY(), getZ());
					Broadcast.toAllOnlinePlayersPvpAnnounce(message);
					message = null;
				}
				else
				{
					SystemMessage message2 = new SystemMessage(SystemMessageId.S1_S2);
					message2.addString(getName() + " " + Config.LIST_OF_TYPES_PVP.get(Rnd.get(Config.LIST_OF_TYPES_PVP.size())) + " " + target.getName() + "");
					Broadcast.toAllOnlinePlayersPvpAnnounce(message2);
					message2 = null;
				}

				// Kill Streak
				//if (targetPlayer.isKillStreak())
				//Broadcast.QuakeAnnounce(getName()+ " shutdown " + targetPlayer.getName() + " kill streak.");
				if (PvPEvent.getInstance().isActive() && (isInsideZone(ZoneId.SOLO_ZONE) || isInsideZone(ZoneId.ZONE_PVP) || isInsideZone(ZoneId.AUTO_PVP_ZONE)))
				{
					if (Config.ALLOW_SPECIAL_PVP_REWARD && !(this instanceof FakePlayer))
						pvpAreaReward();
					
					PvPEventManager.getInstance().updatePvpEventPoints(this, 1);
				}
				// Pvp Reward
				if (canReceivePvpReward(targetPlayer))
					 pvpReward();

				// Quake System
				if (Config.ALLOW_QUAKE_SYSTEM)
					QuakeSystem();

				// Increase the kill count for a special hero aura
				heroConsecutiveKillCount++;

				// If heroConsecutiveKillCount == 30 give hero aura
				if (heroConsecutiveKillCount == Config.KILLS_TO_GET_WAR_LEGEND_AURA && Config.WAR_LEGEND_AURA)
				{
					setHeroAura(true);
					Broadcast.QuakeAnnounce(getName() + " becames War Legend with " + Config.KILLS_TO_GET_WAR_LEGEND_AURA + " PvP's!! ::");
				}

				if (Config.ALLOW_RANKED_SYSTEM)
				{
					RankedSystem.PvpRank(this, target);
					RankedSystem.RankReward(this);
				}

				// Send UserInfo packet to attacker with its Karma and PK Counter
				sendPacket(new UserInfo(this));
			}
		}

		// Otherwise, killer is considered as a PKer.
		else if (targetPlayer.getKarma() == 0 && targetPlayer.getPvpFlag() == 0)
		{
			// PK Points are increased only if you kill a player.
			if ((target instanceof L2PcInstance) && !(this instanceof FakePlayer))
			{
				setPkKills(getPkKills() + 1);
			}

			//announce pvp/pk
			if (Config.ANNOUNCE_ZONE_PVP)
			{
				SystemMessage message = new SystemMessage(SystemMessageId.S1_S2);
				message.addString(getName() + " " + Config.LIST_OF_TYPES_PVP.get(Rnd.get(Config.LIST_OF_TYPES_PVP.size())) + " " + target.getName() + " -");
				message.addZoneName(getX(), getY(), getZ());
				Announcement.announceToPlayers(message);
				message = null;
			}
			else
			{
				SystemMessage message2 = new SystemMessage(SystemMessageId.S1_S2);
				message2.addString(getName() + " " + Config.LIST_OF_TYPES_PVP.get(Rnd.get(Config.LIST_OF_TYPES_PVP.size())) + " " + target.getName() + "");
				Announcement.announceToPlayers(message2);
				message2 = null;
			}

			// PK Reward
			if (canReceivePvpReward(targetPlayer))
				 pvpReward();

			// Calculate new karma.
			setKarma(getKarma() + Formulas.calculateKarmaGain(getPkKills(), target instanceof L2Summon));

			// Send UserInfo packet to attacker with its Karma and PK Counter
			sendPacket(new UserInfo(this));
		}
	}

	private boolean isInPvpBoss() 
	{
	    return isInsideZone(ZoneId.SOLO_ZONE);
	}
	
	private boolean canReceivePvpReward(L2PcInstance targetPlayer)
	{
		if (!Config.ALLOW_PVP_REWARD || this instanceof FakePlayer || ThreadPoolManager.getInstance().isShutdown())
			return false;
		
		if (!Config.ALLOW_FARM_ZONE_PVP_REWARD)
			return true;
		
		L2FarmRandomZone zone = FarmRandomZoneTaskManager.getInstance().getCurrentZone();
		return zone != null && zone.isActive() && zone.isCharacterInZone(this) && targetPlayer != null && zone.isCharacterInZone(targetPlayer);
	}
	
	public void increasePvpKills()
	{
		if (this instanceof FakePlayer)
		{
			FakePlayer fakePlayer = (FakePlayer) this;
			FakePlayerUtilsAI.maybeAnnounceOnKill(fakePlayer);
		}

		// Mission
		if (Config.ACTIVE_MISSION_PVP)
		{							
			if (!checkMissions(getObjectId()))
				updateMissions();

			if (!(this instanceof FakePlayer && (isPVPCompleted() || getPVPCont() >= Config.MISSION_PVP_COUNT)))
				setPVPCont(getPVPCont() + 1);
		}

		if (!(this instanceof FakePlayer))
		{
		    boolean inEvent =
		            (CTFEvent.isStarted() && CTFEvent.isPlayerParticipant(getObjectId()))
		            || (MultiTvTEvent.isStarted() && MultiTvTEvent.isPlayerParticipant(getObjectId()))
		            || (TvTEvent.isStarted() && TvTEvent.isPlayerParticipant(getObjectId()))
		            || (LMEvent.isStarted() && LMEvent.isPlayerParticipant(getObjectId()))
		            || (DMEvent.isStarted() && DMEvent.isPlayerParticipant(getObjectId()))
		            || (KTBEvent.isStarted() && KTBEvent.isPlayerParticipant(getObjectId()))
		            || (FOSEvent.isStarted() && FOSEvent.isPlayerParticipant(getObjectId()))
		            || isInArenaEvent();

		    // Player está em evento
		    if (inEvent)
		    {
		        // Só adiciona PvP se a config permitir
		        if (Config.PVPKILL_IN_EVENTS)
		        {
		            setPvpKills(getPvpKills() + 1);
		        }
		    }
		    else
		    {
		        // Fora do evento sempre conta PvP normal
		        setPvpKills(getPvpKills() + 1);
		    }
		}

		if (Config.PVP_ITEM_ENCHANT_EVENT)
			PvPEnchantSystem.RewardEnchantChanceByPvp(this);

		// Send a Server->Client UserInfo packet to attacker with its Karma and PK Counter
		sendPacket(new UserInfo(this));
	}
	
	private void pvpAreaReward()
	{

		for (RewardHolder reward : Config.PVP_SPECIAL_ITEMS_REWARD)
		{
			if (Rnd.get(100) <= reward.getRewardChance())
				addItem("PvP Event Reward", reward.getRewardId(), Rnd.get(reward.getRewardMin(), reward.getRewardMax()), this, true);
		}
	}
	
	private void pvpReward()
	{
		
		 if(this instanceof FakePlayer)
			 return;

		for (RewardHolder reward : Config.PVP_REWARDS)
		{
			if (Rnd.get(100) <= reward.getRewardChance())
				addItem("Killer Reward", reward.getRewardId(), Rnd.get(reward.getRewardMin(), reward.getRewardMax()), this, true);
		}
	}

	/** Critical announcements for the existing consecutive-PvP counter. */
	public static void quake(int kills, L2PcInstance killer)
	{
		if (killer == null)
			return;

		final String title;
		switch (kills)
		{
			case 10:
				title = "Monster!";
				break;
			case 15:
				title = "Legendary!";
				break;
			case 20:
				title = "WTF !";
				break;
			default:
				return;
		}

		final String text = title + " " + killer.getName() + " Killed " + kills + " consecutive players";
		for (L2PcInstance player : L2World.getInstance().getL2Players())
		{
			if (player != null && player.isOnline())
				player.sendChatMessage(0, KILL_STREAK_ANNOUNCE_CHANNEL, "", text);
		}
	}

	public void QuakeSystem()
	{
		quakeSystem++;
		quake(quakeSystem, this);
		for (int i : Config.QUAKE_VALUES.keySet())
		{
			if (quakeSystem == i)
			{
				setKillStreak(true);
				// The new critical message replaces only these three legacy announcements.
				if (quakeSystem != 10 && quakeSystem != 15 && quakeSystem != 20)
					Broadcast.QuakeAnnounce(getName()+ " " + Config.QUAKE_VALUES.get(i) + ".");

				if (Config.ALLOW_QUAKE_REWARD)
				{
					if (this instanceof FakePlayer)
						return;

					addItem("PvP Badge", Config.QUAKE_REWARD_ITEM, 1, this, true);
				}
			}
		}
	}

	public void QuakeSoundSystem()
	{
		quakeSoundSystem++;

		switch (quakeSoundSystem)
		{
		case 1:
			PlaySound _snd1 = new PlaySound(1, "FirstBlood", 0, 0, 0, 0, 0);
			sendPacket(_snd1);
			break;

		case 12:
			PlaySound _snd2 = new PlaySound(1, "KillingSpree", 0, 0, 0, 0, 0);
			sendPacket(_snd2);
			break;

		case 24:
			PlaySound _snd3 = new PlaySound(1, "Unstoppable", 0, 0, 0, 0, 0);
			sendPacket(_snd3);
			break;

		case 36:
			PlaySound _snd4 = new PlaySound(1, "GodLike", 0, 0, 0, 0, 0);
			sendPacket(_snd4);
			break;

		case 55:
			PlaySound _snd5 = new PlaySound(1, "Humiliation", 0, 0, 0, 0, 0);
			sendPacket(_snd5);
			break;

		case 65:
			PlaySound _snd6 = new PlaySound(1, "HolyShit", 0, 0, 0, 0, 0);
			sendPacket(_snd6);
			break;

		case 75:
			PlaySound _snd7 = new PlaySound(1, "WickedSick", 0, 0, 0, 0, 0);
			sendPacket(_snd7);
			break;

		default:
		}
	}

	private boolean checkAntiFarm(L2PcInstance targetPlayer)
	{
		if (Config.ANTI_FARM_ENABLED && !(targetPlayer instanceof FakePlayer))
		{
			// Anti FARM Clan - Ally
			if (Config.ANTI_FARM_CLAN_ALLY_ENABLED && (getClanId() > 0 && targetPlayer.getClanId() > 0 && getClanId() == targetPlayer.getClanId()) || (getAllyId() > 0 && targetPlayer.getAllyId() > 0 && getAllyId() == targetPlayer.getAllyId()))
			{
				sendMessage("Farm Clan and Ally is punishable with Ban! Gm informed.");
				return false;
			}
			// Anti FARM level player < 40
			if (Config.ANTI_FARM_LVL_DIFF_ENABLED && targetPlayer.getLevel() < Config.ANTI_FARM_MAX_LVL_DIFF)
			{
				sendMessage("Farm is punishable with Ban! Don't kill new players! Gm informed.");
				return false;
			}
			// Anti FARM Party
			if (Config.ANTI_FARM_PARTY_ENABLED && getParty() != null && targetPlayer.getParty() != null && getParty().equals(targetPlayer.getParty()))
			{
				sendMessage("Farm in Party is punishable with Ban! Gm informed.");
				return false;
			}
			// Anti FARM same Ip
			if (Config.ANTI_FARM_IP_ENABLED && !(this instanceof FakePlayer))
			{
				if (getClient() != null && targetPlayer.getClient() != null)
				{
					String ip1 = getClient().getConnection().getInetAddress().getHostAddress();
					String ip2 = targetPlayer.getClient().getConnection().getInetAddress().getHostAddress();

					if (ip1.equals(ip2))
					{
						sendMessage("Pvp/PK Bot is punishable with Ban! Gm informed.");
						return false;
					}
				}
			}
			return true;
		}
		return true;
	}

	public void updatePvPStatus()
	{
	    if (isInsideZone(ZoneId.PVP))
	    {
	        return;
	    }

	    noteHunterPvpFlag(Config.PVP_NORMAL_TIME);

	    PvpFlagTaskManager.getInstance().add(
	        this,
	        Config.PVP_NORMAL_TIME
	    );

	    if (getPvpFlag() == 0)
	    {
	        updatePvPFlag(1);
	    }
	}

	public void updatePvPStatus(L2Character target)
	{
	    final L2PcInstance player = target.getActingPlayer();

	    if (player == null)
	    {
	        return;
	    }

	    if (isInDuel() && player.getDuelId() == getDuelId())
	    {
	        return;
	    }

	    if ((!isInsideZone(ZoneId.PVP) || !target.isInsideZone(ZoneId.PVP))
	        && player.getKarma() == 0)
	    {
	        final int duration = checkIfPvP(player)
	            ? Config.PVP_PVP_TIME
	            : Config.PVP_NORMAL_TIME;

	        noteHunterPvpFlag(duration);

	        PvpFlagTaskManager.getInstance().add(this, duration);

	        if (getPvpFlag() == 0)
	        {
	            updatePvPFlag(1);
	        }
	    }
	}

	/**
	 * Restore the experience this L2PcInstance has lost and sends StatusUpdate packet.
	 * @param restorePercent The specified % of restored experience.
	 */
	public void restoreExp(double restorePercent)
	{
		if (getExpBeforeDeath() > 0)
		{
			getStat().addExp((int) Math.round((getExpBeforeDeath() - getExp()) * restorePercent / 100));
			setExpBeforeDeath(0);
		}
	}

	/**
	 * Reduce the Experience (and level if necessary) of the L2PcInstance in function of the calculated Death Penalty.
	 * <ul>
	 * <li>Calculate the Experience loss</li>
	 * <li>Set the value of _expBeforeDeath</li>
	 * <li>Set the new Experience value of the L2PcInstance and Decrease its level if necessary</li>
	 * <li>Send StatusUpdate packet with its new Experience</li>
	 * </ul>
	 * @param atWar If true, use clan war penalty system instead of regular system.
	 * @param killedByPlayable Used to see if victim loses XP or not.
	 * @param killedBySiegeNpc Used to see if victim loses XP or not.
	 */
	public void deathPenalty(boolean atWar, boolean killedByPlayable, boolean killedBySiegeNpc)
	{
		// No xp loss inside pvp zone unless
		// - it's a siege zone and you're NOT participating
		// - you're killed by a non-pc whose not belong to the siege
		if (isInsideZone(ZoneId.PVP))
		{
			// No xp loss for siege participants inside siege zone.
			if (isInsideZone(ZoneId.SIEGE))
			{
				if (isInSiege() && (killedByPlayable || killedBySiegeNpc))
					return;
			}
			// No xp loss for arenas participants killed by playable.
			else if (killedByPlayable)
				return;
		}

		// Get the level of the L2PcInstance
		final int lvl = getLevel();

		// The death steal you some Exp
		double percentLost = 7.0;
		if (getLevel() >= 76)
			percentLost = 2.0;
		else if (getLevel() >= 40)
			percentLost = 4.0;

		if (getKarma() > 0)
			percentLost *= Config.RATE_KARMA_EXP_LOST;

		if (isFestivalParticipant() || atWar || isInsideZone(ZoneId.SIEGE))
			percentLost /= 4.0;

		// Calculate the Experience loss
		long lostExp = 0;

		if (lvl < Experience.MAX_LEVEL)
			lostExp = Math.round((getStat().getExpForLevel(lvl + 1) - getStat().getExpForLevel(lvl)) * percentLost / 100);
		else
			lostExp = Math.round((getStat().getExpForLevel(Experience.MAX_LEVEL) - getStat().getExpForLevel(Experience.MAX_LEVEL - 1)) * percentLost / 100);

		// Get the Experience before applying penalty
		setExpBeforeDeath(getExp());

		// Set new karma
		updateKarmaLoss(lostExp);

		// Set the new Experience value of the L2PcInstance
		getStat().addExp(-lostExp);
	}

	public boolean isPartyWaiting()
	{
		return PartyMatchWaitingList.getInstance().getPlayers().contains(this);
	}

	public void setPartyRoom(int id)
	{
		_partyroom = id;
	}

	public int getPartyRoom()
	{
		return _partyroom;
	}

	public boolean isInPartyMatchRoom()
	{
		return _partyroom > 0;
	}

	/**
	 * Manage the increase level task of a L2PcInstance (Max MP, Max MP, Recommandation, Expertise and beginner skills...).
	 * <ul>
	 * <li>Send System Message to the L2PcInstance : YOU_INCREASED_YOUR_LEVEL</li>
	 * <li>Send StatusUpdate to the L2PcInstance with new LEVEL, MAX_HP and MAX_MP</li>
	 * <li>Set the current HP and MP of the L2PcInstance, Launch/Stop a HP/MP/CP Regeneration Task and send StatusUpdate packet to all other L2PcInstance to inform (exclusive broadcast)</li>
	 * <li>Recalculate the party level</li>
	 * <li>Recalculate the number of Recommandation that the L2PcInstance can give</li>
	 * <li>Give Expertise skill of this level and remove beginner Lucky skill</li>
	 * </ul>
	 */
	public void increaseLevel()
	{
		// Set the current HP and MP of the L2Character, Launch/Stop a HP/MP/CP Regeneration Task and send StatusUpdate packet to all other L2PcInstance to inform (exclusive broadcast)
		setCurrentHpMp(getMaxHp(), getMaxMp());
		setCurrentCp(getMaxCp());
	}

	/**
	 * Stop all timers related to that L2PcInstance.
	 */
	public void stopAllTimers()
	{
		stopHpMpRegeneration();
		stopWaterTask();
		stopFeed();
		clearPetData();
		storePetFood(_mountNpcId);
		stopPunishTask(true);
		stopChargeTask();

		AttackStanceTaskManager.getInstance().remove(this);
		PvpFlagTaskManager.getInstance().remove(this);
		TakeBreakTaskManager.getInstance().remove(this);
		ShadowItemTaskManager.getInstance().remove(this);
	}

	/**
	 * Return the L2Summon of the L2PcInstance or null.
	 */
	@Override
	public L2Summon getPet()
	{
		return _summon;
	}

	/**
	 * @return {@code true} if the player has a pet, {@code false} otherwise
	 */
	public boolean hasPet()
	{
		return _summon instanceof L2PetInstance;
	}

	/**
	 * @return {@code true} if the player has a summon, {@code false} otherwise
	 */
	public boolean hasServitor()
	{
		return _summon instanceof L2SummonInstance;
	}

	/**
	 * Set the L2Summon of the L2PcInstance.
	 * @param summon The Object.
	 */
	public void setPet(L2Summon summon)
	{
		_summon = summon;
	}

	/**
	 * @return the L2TamedBeast of the L2PcInstance or null.
	 */
	public L2TamedBeastInstance getTrainedBeast()
	{
		return _tamedBeast;
	}

	/**
	 * Set the L2TamedBeast of the L2PcInstance.
	 * @param tamedBeast The Object.
	 */
	public void setTrainedBeast(L2TamedBeastInstance tamedBeast)
	{
		_tamedBeast = tamedBeast;
	}

	/**
	 * @return the L2PcInstance requester of a transaction (ex : FriendInvite, JoinAlly, JoinParty...).
	 */
	public L2Request getRequest()
	{
		return _request;
	}

	/**
	 * Set the L2PcInstance requester of a transaction (ex : FriendInvite, JoinAlly, JoinParty...).
	 * @param requester
	 */
	public void setActiveRequester(L2PcInstance requester)
	{
		_activeRequester = requester;
	}

	/**
	 * @return the L2PcInstance requester of a transaction (ex : FriendInvite, JoinAlly, JoinParty...).
	 */
	public L2PcInstance getActiveRequester()
	{
		if (_activeRequester != null && _activeRequester.isRequestExpired() && _activeTradeList == null)
			_activeRequester = null;

		return _activeRequester;
	}

	/**
	 * @return True if a request is in progress.
	 */
	public boolean isProcessingRequest()
	{
		return getActiveRequester() != null || _requestExpireTime > GameTimeController.getInstance().getGameTicks();
	}

	/**
	 * @return True if a transaction <B>(trade OR request)</B> is in progress.
	 */
	public boolean isProcessingTransaction()
	{
		return getActiveRequester() != null || _activeTradeList != null || _requestExpireTime > GameTimeController.getInstance().getGameTicks();
	}

	/**
	 * Set the _requestExpireTime of that L2PcInstance, and set his partner as the active requester.
	 * @param partner The partner to make checks on.
	 */
	public void onTransactionRequest(L2PcInstance partner)
	{
		_requestExpireTime = GameTimeController.getInstance().getGameTicks() + REQUEST_TIMEOUT * GameTimeController.TICKS_PER_SECOND;
		partner.setActiveRequester(this);
	}

	/**
	 * @return true if last request is expired.
	 */
	public boolean isRequestExpired()
	{
		return !(_requestExpireTime > GameTimeController.getInstance().getGameTicks());
	}

	/**
	 * Select the Warehouse to be used in next activity.
	 */
	public void onTransactionResponse()
	{
		_requestExpireTime = 0;
	}

	/**
	 * Select the Warehouse to be used in next activity.
	 * @param warehouse An active warehouse.
	 */
	public void setActiveWarehouse(ItemContainer warehouse)
	{
		_activeWarehouse = warehouse;
	}

	/**
	 * @return The active Warehouse.
	 */
	public ItemContainer getActiveWarehouse()
	{
		return _activeWarehouse;
	}

	/**
	 * Set the TradeList to be used in next activity.
	 * @param tradeList The TradeList to be used.
	 */
	public void setActiveTradeList(TradeList tradeList)
	{
		_activeTradeList = tradeList;
	}

	/**
	 * @return The active TradeList.
	 */
	public TradeList getActiveTradeList()
	{
		return _activeTradeList;
	}

	public void onTradeStart(L2PcInstance partner)
	{
		_activeTradeList = new TradeList(this);
		_activeTradeList.setPartner(partner);

		sendPacket(SystemMessage.getSystemMessage(SystemMessageId.BEGIN_TRADE_WITH_S1).addString(partner.getName()));
		sendPacket(new TradeStart(this));
	}

	public void onTradeConfirm(L2PcInstance partner)
	{
		sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_CONFIRMED_TRADE).addString(partner.getName()));

		partner.sendPacket(TradePressOwnOk.STATIC_PACKET);
		sendPacket(TradePressOtherOk.STATIC_PACKET);
	}

	public void onTradeCancel(L2PcInstance partner)
	{
		if (_activeTradeList == null)
			return;

		_activeTradeList.lock();
		_activeTradeList = null;

		sendPacket(new SendTradeDone(0));
		sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_CANCELED_TRADE).addString(partner.getName()));
	}

	public void onTradeFinish(boolean successfull)
	{
		_activeTradeList = null;
		sendPacket(new SendTradeDone(1));
		if (successfull)
			sendPacket(SystemMessageId.TRADE_SUCCESSFUL);
	}

	public void startTrade(L2PcInstance partner)
	{
		onTradeStart(partner);
		partner.onTradeStart(this);
	}

	public void cancelActiveTrade()
	{
		if (_activeTradeList == null)
			return;

		L2PcInstance partner = _activeTradeList.getPartner();
		if (partner != null)
			partner.onTradeCancel(this);

		onTradeCancel(this);
	}

	/**
	 * @return The _createList object of the L2PcInstance.
	 */
	public L2ManufactureList getCreateList()
	{
		return _createList;
	}

	/**
	 * Set the _createList object of the L2PcInstance.
	 * @param list
	 */
	public void setCreateList(L2ManufactureList list)
	{
		_createList = list;
	}

	/**
	 * @return The _sellList object of the L2PcInstance.
	 */
	public TradeList getSellList()
	{
		if (_sellList == null)
			_sellList = new TradeList(this);

		return _sellList;
	}

	/**
	 * @return the _buyList object of the L2PcInstance.
	 */
	public TradeList getBuyList()
	{
		if (_buyList == null)
			_buyList = new TradeList(this);

		return _buyList;
	}

	/**
	 * Set the Private Store type of the L2PcInstance.
	 * @param type The value : 0 = none, 1 = sell, 2 = sellmanage, 3 = buy, 4 = buymanage, 5 = manufacture.
	 */
	public void setPrivateStoreType(int type)
	{
		_privateStore = type;

		if (Config.OFFLINE_DISCONNECT_FINISHED && (_privateStore == STORE_PRIVATE_NONE) && ((getClient() == null) || getClient().isDetached()))
			deleteMe();
	}

	/**
	 * @return The Private Store type of the L2PcInstance.
	 */
	public int getPrivateStoreType()
	{
		return _privateStore;
	}

	/**
	 * Set the _skillLearningClassId object of the L2PcInstance.
	 * @param classId The parameter.
	 */
	public void setSkillLearningClassId(ClassId classId)
	{
		_skillLearningClassId = classId;
	}

	/**
	 * @return The _skillLearningClassId object of the L2PcInstance.
	 */
	public ClassId getSkillLearningClassId()
	{
		return _skillLearningClassId;
	}

	/**
	 * Set the _clan object, _clanId, _clanLeader Flag and title of the L2PcInstance.
	 * @param clan The Clan object which is used to feed L2PcInstance values.
	 */
	public void setClan(L2Clan clan)
	{
		_clan = clan;
		setTitle("");

		if (clan == null)
		{
			_clanId = 0;
			_clanPrivileges = 0;
			_pledgeType = 0;
			_powerGrade = 0;
			_lvlJoinedAcademy = 0;
			_apprentice = 0;
			_sponsor = 0;
			return;
		}

		if (!clan.isMember(getObjectId()))
		{
			// char has been kicked from clan
			setClan(null);
			return;
		}

		_clanId = clan.getClanId();
	}

	/**
	 * @return The _clan object of the L2PcInstance.
	 */
	public L2Clan getClan()
	{
		return _clan;
	}

	/**
	 * @return True if the L2PcInstance is the leader of its clan.
	 */
	public boolean isClanLeader()
	{
		if (getClan() == null)
			return false;

		return getObjectId() == getClan().getLeaderId();
	}

	/**
	 * Reduce the number of arrows owned by the L2PcInstance and send InventoryUpdate or ItemList (to unequip if the last arrow was consummed).
	 */
	@Override
	protected void reduceArrowCount()
	{
		ItemInstance arrows = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);

		if (arrows == null)
		{
			getInventory().unEquipItemInSlot(Inventory.PAPERDOLL_LHAND);
			_arrowItem = null;
			sendPacket(new ItemList(this, false));
			return;
		}

		// Adjust item quantity
		if (arrows.getCount() > 1)
		{
			synchronized (arrows)
			{
				arrows.changeCountWithoutTrace(-1, this, null);
				arrows.setLastChange(ItemInstance.MODIFIED);

				// could do also without saving, but let's save approx 1 of 10
				if (GameTimeController.getInstance().getGameTicks() % 10 == 0)
					arrows.updateDatabase();
				_inventory.refreshWeight();
			}
		}
		else
		{
			// Destroy entire item and save to database
			_inventory.destroyItem("Consume", arrows, this, null);

			getInventory().unEquipItemInSlot(Inventory.PAPERDOLL_LHAND);
			_arrowItem = null;

			sendPacket(new ItemList(this, false));
			return;
		}

		InventoryUpdate iu = new InventoryUpdate();
		iu.addModifiedItem(arrows);
		sendPacket(iu);
	}

	/**
	 * Equip arrows needed in left hand and send ItemList to the L2PcInstance then return True.
	 */
	@Override
	protected boolean checkAndEquipArrows()
	{
		// Check if nothing is equipped in left hand
		if (getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND) == null)
		{
			// Get the ItemInstance of the arrows needed for this bow
			_arrowItem = getInventory().findArrowForBow(getActiveWeaponItem());

			if (_arrowItem != null)
			{
				// Equip arrows needed in left hand
				getInventory().setPaperdollItem(Inventory.PAPERDOLL_LHAND, _arrowItem);

				// Send ItemList to this L2PcINstance to update left hand equipement
				sendPacket(new ItemList(this, false));
			}
		}
		// Get the ItemInstance of arrows equipped in left hand
		else
			_arrowItem = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);

		return _arrowItem != null;
	}

	/**
	 * Disarm the player's weapon and shield.
	 * @return true if successful, false otherwise.
	 */
	public boolean disarmWeapons()
	{
		// Don't allow disarming a cursed weapon
		if (isCursedWeaponEquipped())
			return false;

		// Unequip the weapon
		ItemInstance wpn = getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
		if (wpn != null)
		{
			ItemInstance[] unequipped = getInventory().unEquipItemInBodySlotAndRecord(wpn.getItem().getBodyPart());
			InventoryUpdate iu = new InventoryUpdate();
			for (ItemInstance itm : unequipped)
				iu.addModifiedItem(itm);
			sendPacket(iu);

			abortAttack();
			broadcastUserInfo();

			// this can be 0 if the user pressed the right mousebutton twice very fast
			if (unequipped.length > 0)
			{
				SystemMessage sm;
				if (unequipped[0].getEnchantLevel() > 0)
					sm = SystemMessage.getSystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED).addNumber(unequipped[0].getEnchantLevel()).addItemName(unequipped[0]);
				else
					sm = SystemMessage.getSystemMessage(SystemMessageId.S1_DISARMED).addItemName(unequipped[0]);

				sendPacket(sm);
			}
		}

		// Unequip the shield
		ItemInstance sld = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		if (sld != null)
		{
			ItemInstance[] unequipped = getInventory().unEquipItemInBodySlotAndRecord(sld.getItem().getBodyPart());
			InventoryUpdate iu = new InventoryUpdate();
			for (ItemInstance itm : unequipped)
				iu.addModifiedItem(itm);
			sendPacket(iu);

			abortAttack();
			broadcastUserInfo();

			// this can be 0 if the user pressed the right mousebutton twice very fast
			if (unequipped.length > 0)
			{
				SystemMessage sm;
				if (unequipped[0].getEnchantLevel() > 0)
					sm = SystemMessage.getSystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED).addNumber(unequipped[0].getEnchantLevel()).addItemName(unequipped[0]);
				else
					sm = SystemMessage.getSystemMessage(SystemMessageId.S1_DISARMED).addItemName(unequipped[0]);

				sendPacket(sm);
			}
		}
		return true;
	}

	public boolean mount(L2Summon pet)
	{
		if (!disarmWeapons())
			return false;

		stopAllToggles();
		Ride mount = new Ride(getObjectId(), Ride.ACTION_MOUNT, pet.getTemplate().getNpcId());
		setMount(pet.getNpcId(), pet.getLevel(), mount.getMountType());
		setMountObjectID(pet.getControlItemId());
		clearPetData();
		startFeed(pet.getNpcId());
		broadcastPacket(mount);

		// Notify self and others about speed change
		broadcastUserInfo();

		pet.unSummon(this);
		return true;
	}
	public boolean isCustomMount()
	{
		return _customMountVisualIndex >= 0 && isMounted();
	}

	public int getCustomMountVisualIndex()
	{
		return _customMountVisualIndex;
	}

	private void restoreMountEquipment()
	{
		final int rightHandId = _mountSavedRightHand;
		final int leftHandId = _mountSavedLeftHand;

		// Limpa antes de equipar para evitar restauracoes repetidas.
		_mountSavedRightHand = 0;
		_mountSavedLeftHand = 0;

		if (!isOnline() || isAlikeDead() || isMounted()
			|| isCursedWeaponEquipped() || isInStoreMode()
			|| isProcessingTransaction())
		{
			return;
		}

		restoreMountHandItem(rightHandId, true);

		if (leftHandId != rightHandId)
			restoreMountHandItem(leftHandId, false);
	}

	private void restoreMountHandItem(int objectId, boolean rightHand)
	{
		if (objectId <= 0)
			return;

		final ItemInstance item =
			getInventory().getItemByObjectId(objectId);

		if (item == null
			|| item.getOwnerId() != getObjectId()
			|| item.getLocation() != ItemInstance.ItemLocation.INVENTORY
			|| item.isEquipped())
		{
			return;
		}

		final int bodyPart = item.getItem().getBodyPart();

		final ItemInstance currentRight =
			getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);

		final ItemInstance currentLeft =
			getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);

		if (rightHand)
		{
			if (bodyPart != Item.SLOT_R_HAND
				&& bodyPart != Item.SLOT_LR_HAND)
			{
				return;
			}

			// Preserva qualquer equipamento colocado posteriormente.
			if (currentRight != null)
				return;

			if (bodyPart == Item.SLOT_LR_HAND && currentLeft != null)
				return;
		}
		else
		{
			if (bodyPart != Item.SLOT_L_HAND || currentLeft != null)
				return;

			// Nao troca uma arma de duas maos pelo escudo.
			if (currentRight != null
				&& currentRight.getItem().getBodyPart() == Item.SLOT_LR_HAND)
			{
				return;
			}
		}

		if (!item.getItem().checkCondition(this, this, false))
			return;

		useEquippableItem(item, true);
	}
	
	public boolean mountCustom(int visualIndex, int controlItemId)
	{
		if (visualIndex < 0 || visualIndex > 44|| isMounted() || getPet() != null|| isAlikeDead() || isFishing()|| isInOlympiadMode() || isCursedWeaponEquipped()|| isAttackingNow() || isInCombat()|| getPvpFlag() > 0)
		{
			return false;
		}

		if (PetDataTable.getInstance().getPetData(12527) == null
			|| NpcTable.getInstance().getTemplate(12527) == null)
		{
			return false;
		}

		// Guarda os itens antes de desequipar.
		final ItemInstance rightHand =
			getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);

		final ItemInstance leftHand =
			getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);

		final int rightHandId =
			rightHand != null ? rightHand.getObjectId() : 0;

		final int leftHandId =
			leftHand != null ? leftHand.getObjectId() : 0;

		if (!disarmWeapons())
			return false;

		_mountSavedRightHand = rightHandId;
		_mountSavedLeftHand = leftHandId;

		stopMove(null);
		stopAllToggles();
		stopFeed();

		_canFeed = false;
		_controlItemId = 0;
		clearPetData();
		_leveldata = null;

		if (!setMount(12527, getLevel(), 1))
		{
			restoreMountEquipment();
			return false;
		}

		_customMountVisualIndex = visualIndex;
		setMountObjectID(controlItemId);

		broadcastPacket(
			new Ride(getObjectId(), Ride.ACTION_MOUNT, visualIndex)
		);

		broadcastUserInfo();
		return true;
	}
	
	public boolean mount(int npcId, int controlItemId, boolean useFood)
	{
		if (!disarmWeapons())
			return false;

		stopAllToggles();
		Ride mount = new Ride(getObjectId(), Ride.ACTION_MOUNT, npcId);
		if (setMount(npcId, getLevel(), mount.getMountType()))
		{
			clearPetData();
			setMountObjectID(controlItemId);
			broadcastPacket(mount);

			// Notify self and others about speed change
			broadcastUserInfo();

			if (useFood)
				startFeed(npcId);

			return true;
		}
		return false;
	}

	public boolean mountPlayer(L2Summon summon)
	{
		if (summon != null && summon.isMountable() && !isMounted() && !isBetrayed())
		{
			if (isDead()) // A strider cannot be ridden when dead.
			{
				sendPacket(SystemMessageId.STRIDER_CANT_BE_RIDDEN_WHILE_DEAD);
				return false;
			}

			if (summon.isDead()) // A dead strider cannot be ridden.
			{
				sendPacket(SystemMessageId.DEAD_STRIDER_CANT_BE_RIDDEN);
				return false;
			}

			if (summon.isInCombat() || summon.isRooted()) // A strider in battle cannot be ridden.
			{
				sendPacket(SystemMessageId.STRIDER_IN_BATLLE_CANT_BE_RIDDEN);
				return false;
			}

			if (isInCombat()) // A strider cannot be ridden while in battle
			{
				sendPacket(SystemMessageId.STRIDER_CANT_BE_RIDDEN_WHILE_IN_BATTLE);
				return false;
			}

			if (isSitting()) // A strider can be ridden only when standing
			{
				sendPacket(SystemMessageId.STRIDER_CAN_BE_RIDDEN_ONLY_WHILE_STANDING);
				return false;
			}

			if (isFishing()) // You can't mount, dismount, break and drop items while fishing
			{
				sendPacket(SystemMessageId.CANNOT_DO_WHILE_FISHING_2);
				return false;
			}

			if (isCursedWeaponEquipped()) // You can't mount, dismount, break and drop items while weilding a cursed weapon
			{
				sendPacket(SystemMessageId.STRIDER_CANT_BE_RIDDEN_WHILE_IN_BATTLE);
				return false;
			}

			if (!Util.checkIfInRange(200, this, summon, true))
			{
				sendPacket(SystemMessageId.TOO_FAR_AWAY_FROM_STRIDER_TO_MOUNT);
				return false;
			}

			if (summon.isHungry())
			{
				sendPacket(SystemMessageId.HUNGRY_STRIDER_NOT_MOUNT);
				return false;
			}

			if (!summon.isDead() && !isMounted())
				mount(summon);
		}
		else if (isMounted())
		{
			if (getMountType() == 2 && isInsideZone(ZoneId.NO_LANDING))
			{
				sendPacket(SystemMessageId.NO_DISMOUNT_HERE);
				return false;
			}

			if (isHungry())
			{
				sendPacket(SystemMessageId.HUNGRY_STRIDER_NOT_MOUNT);
				return false;
			}

			dismount();
		}
		return true;
	}

	public boolean dismount()
	{
		final boolean restoreEquipment = isCustomMount();

		sendPacket(new SetupGauge(3, 0, 0));

		final int petId = _mountNpcId;

		if (setMount(0, 0, 0))
		{
			stopFeed();
			clearPetData();

			broadcastPacket(
				new Ride(getObjectId(), Ride.ACTION_DISMOUNT, 0)
			);

			setMountObjectID(0);
			storePetFood(petId);

			if (restoreEquipment)
				restoreMountEquipment();

			broadcastUserInfo();
			return true;
		}

		return false;
	}

	public void storePetFood(int petId)
	{
		if (_controlItemId != 0 && petId != 0)
		{
			try (Connection con = L2DatabaseFactory.getInstance().getConnection())
			{
				PreparedStatement statement = con.prepareStatement("UPDATE pets SET fed=? WHERE item_obj_id = ?");
				statement.setInt(1, getCurrentFeed());
				statement.setInt(2, _controlItemId);
				statement.executeUpdate();
				statement.close();
				_controlItemId = 0;
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Failed to store Pet [NpcId: " + petId + "] data", e);
			}
		}
	}

	protected class FeedTask implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				if (!isMounted())
				{
					stopFeed();
					return;
				}

				if (getCurrentFeed() > getFeedConsume())
				{
					// eat
					setCurrentFeed(getCurrentFeed() - getFeedConsume());
				}
				else
				{
					// go back to pet control item, or simply said, unsummon it
					setCurrentFeed(0);
					stopFeed();
					dismount();
					sendPacket(SystemMessageId.OUT_OF_FEED_MOUNT_CANCELED);
				}

				int[] foodIds = getPetData(getMountNpcId()).getFood();
				if (foodIds.length == 0)
					return;

				ItemInstance food = null;
				for (int id : foodIds)
				{
					food = getInventory().getItemByItemId(id);
					if (food != null)
						break;
				}

				if (food != null && isHungry())
				{
					IItemHandler handler = ItemHandler.getInstance().getItemHandler(food.getEtcItem());
					if (handler != null)
					{
						handler.useItem(L2PcInstance.this, food, false);
						sendPacket(SystemMessage.getSystemMessage(SystemMessageId.PET_TOOK_S1_BECAUSE_HE_WAS_HUNGRY).addItemName(food));
					}
				}
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Mounted Pet [NpcId: " + getMountNpcId() + "] a feed task error has occurred", e);
			}
		}
	}

	protected synchronized void startFeed(int npcId)
	{
		if (isCustomMount())
		{
			_canFeed = false;
			return;
		}
		
		_canFeed = npcId > 0;
		if (!isMounted())
			return;

		if (getPet() != null)
		{
			setCurrentFeed(((L2PetInstance) getPet()).getCurrentFed());
			_controlItemId = getPet().getControlItemId();
			sendPacket(new SetupGauge(3, getCurrentFeed() * 10000 / getFeedConsume(), getMaxFeed() * 10000 / getFeedConsume()));
			if (!isDead())
				_mountFeedTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new FeedTask(), 10000, 10000);
		}
		else if (_canFeed)
		{
			setCurrentFeed(getMaxFeed());
			sendPacket(new SetupGauge(3, getCurrentFeed() * 10000 / getFeedConsume(), getMaxFeed() * 10000 / getFeedConsume()));
			if (!isDead())
				_mountFeedTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new FeedTask(), 10000, 10000);
		}
	}

	protected synchronized void stopFeed()
	{
		if (_mountFeedTask != null)
		{
			_mountFeedTask.cancel(false);
			_mountFeedTask = null;
		}
	}

	private final void clearPetData()
	{
		_data = null;
	}

	protected final L2PetData getPetData(int npcId)
	{
		if (_data == null)
			_data = PetDataTable.getInstance().getPetData(npcId);

		return _data;
	}

	private final L2PetLevelData getPetLevelData(int npcId)
	{
		if (_leveldata == null)
			_leveldata = PetDataTable.getInstance().getPetData(npcId).getPetLevelData(getMountLevel());

		return _leveldata;
	}

	public int getCurrentFeed()
	{
		return _curFeed;
	}

	protected int getFeedConsume()
	{
		return (isAttackingNow()) ? getPetLevelData(_mountNpcId).getPetFeedBattle() : getPetLevelData(_mountNpcId).getPetFeedNormal();
	}

	public void setCurrentFeed(int num)
	{
		_curFeed = (num > getMaxFeed()) ? getMaxFeed() : num;
		sendPacket(new SetupGauge(3, getCurrentFeed() * 10000 / getFeedConsume(), getMaxFeed() * 10000 / getFeedConsume()));
	}

	private int getMaxFeed()
	{
		return getPetLevelData(_mountNpcId).getPetMaxFeed();
	}

	protected boolean isHungry()
	{
		return _canFeed ? (getCurrentFeed() < (getPetLevelData(getMountNpcId()).getPetMaxFeed() * 0.55)) : false;
	}

	/**
	 * @return the type of attack, depending of the worn weapon.
	 */
	@Override
	public WeaponType getAttackType()
	{
		final Weapon weapon = getActiveWeaponItem();
		if (weapon != null)
			return weapon.getItemType();

		return WeaponType.FIST;
	}

	public void setUptime(long time)
	{
		_uptime = time;
	}

	public long getUptime()
	{
		return System.currentTimeMillis() - _uptime;
	}

	/**
	 * Return True if the L2PcInstance is invulnerable.
	 */
	@Override
	public boolean isInvul()
	{
		return super.isInvul() || isSpawnProtected();
	}

	/**
	 * Return True if the L2PcInstance has a Party in progress.
	 */
	@Override
	public boolean isInParty()
	{
		return _party != null;
	}

	/**
	 * Set the _party object of the L2PcInstance (without joining it).
	 * @param party The object.
	 */
	public void setParty(L2Party party)
	{
		_party = party;
	}

	/**
	 * Set the _party object of the L2PcInstance AND join it.
	 * @param party
	 */
	public void joinParty(L2Party party)
	{
		if (party != null)
		{
			_party = party;
			party.addPartyMember(this);
		}
	}

	/**
	 * Manage the Leave Party task of the L2PcInstance.
	 */
	public void leaveParty()
	{
		if (isInParty())
		{
			_party.removePartyMember(this, MessageType.Disconnected);
			_party = null;
		}
	}

	/**
	 * Return the _party object of the L2PcInstance.
	 */
	@Override
	public L2Party getParty()
	{
		return _party;
	}

	/**
	 * Return True if the L2PcInstance is a GM.
	 */
	@Override
	public boolean isGM()
	{
		return getAccessLevel().isGm();
	}

	/**
	 * Set the _accessLevel of the L2PcInstance.
	 * @param level
	 */
	public void setAccessLevel(int level)
	{
		if (level == AccessLevels.MASTER_ACCESS_LEVEL_NUMBER)
		{
			_log.warning(getName() + " has logged in with Master access level.");
			_accessLevel = AccessLevels.MASTER_ACCESS_LEVEL;
		}
		else if (level == AccessLevels.USER_ACCESS_LEVEL_NUMBER)
			_accessLevel = AccessLevels.USER_ACCESS_LEVEL;
		else
		{
			L2AccessLevel accessLevel = AccessLevels.getInstance().getAccessLevel(level);

			if (accessLevel == null)
			{
				if (level < 0)
				{
					AccessLevels.getInstance().addBanAccessLevel(level);
					_accessLevel = AccessLevels.getInstance().getAccessLevel(level);
				}
				else
				{
					_log.warning("Server tried to set unregistered access level " + level + " to " + getName() + ". His access level have been reseted to user level.");
					_accessLevel = AccessLevels.USER_ACCESS_LEVEL;
				}
			}
			else
			{
				_accessLevel = accessLevel;
				setTitle(_accessLevel.getName());
			}
		}

		getAppearance().setNameColor(_accessLevel.getNameColor());
		getAppearance().setTitleColor(_accessLevel.getTitleColor());
		broadcastUserInfo();

		CharNameTable.getInstance().addName(this);
	}

	public void setAccountAccesslevel(int level)
	{
		LoginServerThread.getInstance().sendAccessLevel(getAccountName(), level);
	}

	/**
	 * @return the _accessLevel of the L2PcInstance.
	 */
	public L2AccessLevel getAccessLevel()
	{
		if (Config.EVERYBODY_HAS_ADMIN_RIGHTS)
			return AccessLevels.MASTER_ACCESS_LEVEL;

		if (_accessLevel == null) /* This is here because inventory etc. is loaded before access level on login, so it is not null */
			setAccessLevel(AccessLevels.USER_ACCESS_LEVEL_NUMBER);

		return _accessLevel;
	}

	/**
	 * Update Stats of the L2PcInstance client side by sending UserInfo/StatusUpdate to this L2PcInstance and CharInfo/StatusUpdate to all L2PcInstance in its _KnownPlayers (broadcast).
	 * @param broadcastType
	 */
	public void updateAndBroadcastStatus(int broadcastType)
	{
		refreshOverloaded();
		refreshExpertisePenalty();
		refreshArmorPenality();
		refreshWeaponPenality();

		if (broadcastType == 1)
			sendPacket(new UserInfo(this));
		else if (broadcastType == 2)
			broadcastUserInfo();
	}

	/**
	 * Send StatusUpdate packet with Karma to the L2PcInstance and all L2PcInstance to inform (broadcast).
	 */
	public void broadcastKarma()
	{
		StatusUpdate su = new StatusUpdate(this);
		su.addAttribute(StatusUpdate.KARMA, getKarma());
		sendPacket(su);

		if (getPet() != null)
			sendPacket(new RelationChanged(getPet(), getRelation(this), false));

		broadcastRelationsChanges();
	}

	/**
	 * Set the online Flag to True or False and update the characters table of the database with online status and lastAccess (called when login and logout).
	 * @param isOnline
	 * @param updateInDb
	 */
	public void setOnlineStatus(boolean isOnline, boolean updateInDb)
	{
		if (_isOnline != isOnline)
			_isOnline = isOnline;

		// Update the characters table of the database with online status and lastAccess (called when login and logout)
		if (updateInDb)
			updateOnlineStatus();
	}

	public void setIsIn7sDungeon(boolean isIn7sDungeon)
	{
		_isIn7sDungeon = isIn7sDungeon;
	}

	/**
	 * Update the characters table of the database with online status and lastAccess of this L2PcInstance (called when login and logout).
	 */
	public void updateOnlineStatus()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("UPDATE characters SET online=?, lastAccess=? WHERE obj_id=?");
			statement.setInt(1, isOnlineInt());
			statement.setLong(2, System.currentTimeMillis());
			statement.setInt(3, getObjectId());
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("could not set char online status:" + e);
		}
	}

	/**
	 * Create a new player in the characters table of the database.
	 * @return true if successful.
	 */
	private boolean createDb()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement(INSERT_CHARACTER);
			statement.setString(1, _accountName);
			statement.setInt(2, getObjectId());
			statement.setString(3, getName());
			statement.setInt(4, getLevel());
			statement.setInt(5, getMaxHp());
			statement.setDouble(6, getCurrentHp());
			statement.setInt(7, getMaxCp());
			statement.setDouble(8, getCurrentCp());
			statement.setInt(9, getMaxMp());
			statement.setDouble(10, getCurrentMp());
			statement.setInt(11, getAppearance().getFace());
			statement.setInt(12, getAppearance().getHairStyle());
			statement.setInt(13, getAppearance().getHairColor());
			statement.setInt(14, getAppearance().getSex() ? 1 : 0);
			statement.setLong(15, getExp());
			statement.setInt(16, getSp());
			statement.setInt(17, getKarma());
			statement.setInt(18, getPvpKills());
			statement.setInt(19, getPkKills());
			statement.setInt(20, getDeaths());
			statement.setInt(21, getClanId());
			statement.setInt(22, getRace().ordinal());
			statement.setInt(23, getClassId().getId());
			statement.setLong(24, getDeleteTimer());
			statement.setInt(25, hasDwarvenCraft() ? 1 : 0);
			statement.setString(26, getTitle());
			statement.setInt(27, getAccessLevel().getLevel());
			statement.setInt(28, isOnlineInt());
			statement.setInt(29, isIn7sDungeon() ? 1 : 0);
			statement.setInt(30, getClanPrivileges());
			statement.setInt(31, wantsPeace() ? 1 : 0);
			statement.setInt(32, getBaseClass());
			statement.setInt(33, isNoble() ? 1 : 0);
			statement.setLong(34, 0);
			statement.setLong(35, System.currentTimeMillis());
			statement.setString(36,StringToHex(Integer.toHexString(getAppearance().getNameColor()).toUpperCase()));
			statement.setString(37,StringToHex(Integer.toHexString(getAppearance().getTitleColor()).toUpperCase()));
			statement.executeUpdate();
			statement.close();

		}
		catch (Exception e)
		{
			_log.severe("Could not insert char data: " + e);
			return false;
		}
		return true;
	}

	/**
	 * Retrieve a L2PcInstance from the characters table of the database and add it in _allObjects of the L2world.
	 * <ul>
	 * <li>Retrieve the L2PcInstance from the characters table of the database</li>
	 * <li>Add the L2PcInstance object in _allObjects</li>
	 * <li>Set the x,y,z position of the L2PcInstance and make it invisible</li>
	 * <li>Update the overloaded status of the L2PcInstance</li>
	 * </ul>
	 * @param objectId Identifier of the object to initialized
	 * @return The L2PcInstance loaded from the database
	 */
	public static L2PcInstance restore(int objectId)
	{
		L2PcInstance player = null;
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement(RESTORE_CHARACTER);
			statement.setInt(1, objectId);
			ResultSet rset = statement.executeQuery();

			while (rset.next())
			{
				final int activeClassId = rset.getInt("classid");
				final PcTemplate template = CharTemplateTable.getInstance().getTemplate(activeClassId);
				final PcAppearance app = new PcAppearance(rset.getByte("face"), rset.getByte("hairColor"), rset.getByte("hairStyle"), rset.getInt("sex") != 0);

				player = new L2PcInstance(objectId, template, rset.getString("account_name"), app);
				player.setName(rset.getString("char_name"));
				player._lastAccess = rset.getLong("lastAccess");

				player.getStat().setExp(rset.getLong("exp"));
				player.setExpBeforeDeath(rset.getLong("expBeforeDeath"));
				player.getStat().setLevel(rset.getByte("level"));
				player.getStat().setSp(rset.getInt("sp"));

				player.setWantsPeace(rset.getInt("wantspeace") == 1);

				player.setHeading(rset.getInt("heading"));

				player.setKarma(rset.getInt("karma"));
				player.setPvpKills(rset.getInt("pvpkills"));
				player.setPkKills(rset.getInt("pkkills"));
				player.setOnlineTime(rset.getLong("onlinetime"));
				player.setNoble(rset.getInt("nobless") == 1, false);

				player.setClanJoinExpiryTime(rset.getLong("clan_join_expiry_time"));
				if (player.getClanJoinExpiryTime() < System.currentTimeMillis())
					player.setClanJoinExpiryTime(0);

				player.setClanCreateExpiryTime(rset.getLong("clan_create_expiry_time"));
				if (player.getClanCreateExpiryTime() < System.currentTimeMillis())
					player.setClanCreateExpiryTime(0);

				player.setPowerGrade(rset.getInt("power_grade"));
				player.setPledgeType(rset.getInt("subpledge"));
				player.setLastRecomUpdate(rset.getLong("last_recom_date"));

				int clanId = rset.getInt("clanid");
				if (clanId > 0)
					player.setClan(ClanTable.getInstance().getClan(clanId));

				if (player.getClan() != null)
				{
					if (player.getClan().getLeaderId() != player.getObjectId())
					{
						if (player.getPowerGrade() == 0)
							player.setPowerGrade(5);

						player.setClanPrivileges(player.getClan().getRankPrivs(player.getPowerGrade()));
					}
					else
					{
						player.setClanPrivileges(L2Clan.CP_ALL);
						player.setPowerGrade(1);
					}
				}
				else
					player.setClanPrivileges(L2Clan.CP_NOTHING);

				player.setDeleteTimer(rset.getLong("deletetime"));

				player.setTitle(rset.getString("title"));
				player.setAccessLevel(rset.getInt("accesslevel"));
				player.setFistsWeaponItem(findFistsWeaponItem(activeClassId));
				player.setUptime(System.currentTimeMillis());

				// Check recs
				player.checkRecom(rset.getInt("rec_have"), rset.getInt("rec_left"));

				player._classIndex = 0;
				try
				{
					player.setBaseClass(rset.getInt("base_class"));
				}
				catch (Exception e)
				{
					player.setBaseClass(activeClassId);
				}

				// Restore Subclass Data (cannot be done earlier in function)
				if (restoreSubClassData(player))
				{
					if (activeClassId != player.getBaseClass())
					{
						for (SubClass subClass : player.getSubClasses().values())
							if (subClass.getClassId() == activeClassId)
								player._classIndex = subClass.getClassIndex();
					}
				}

				player.setApprentice(rset.getInt("apprentice"));
				player.setSponsor(rset.getInt("sponsor"));
				player.setLvlJoinedAcademy(rset.getInt("lvl_joined_academy"));
				player.setIsIn7sDungeon(rset.getInt("isin7sdungeon") == 1);
				player.setPunishLevel(rset.getInt("punish_level"));
				if (player.getPunishLevel() != PunishLevel.NONE)
					player.setPunishTimer(rset.getLong("punish_timer"));
				else
					player.setPunishTimer(0);

				player.getAppearance().setNameColor(Integer.decode((new StringBuilder()).append("0x").append(rset.getString("name_color")).toString()).intValue());
				player.getAppearance().setTitleColor(Integer.decode((new StringBuilder()).append("0x").append(rset.getString("title_color")).toString()).intValue());

				CursedWeaponsManager.getInstance().checkPlayer(player);

				player.setAllianceWithVarkaKetra(rset.getInt("varka_ketra_ally"));

				player.setDeathPenaltyBuffLevel(rset.getInt("death_penalty_level"));

				player.setAio(rset.getInt("aio") == 1 ? true : false);
				player.setAioEndTime(rset.getLong("aio_end"));

				player.setVip(rset.getInt("vip") == 1 ? true : false);
				player.setVipEndTime(rset.getLong("vip_end"));

				player.setTimedHero(rset.getInt("timedHero") == 1 ? true : false);
				player.setTimedHeroEndTime(rset.getLong("timedHero_end"));

				player.pcBangPoint = rset.getInt("pc_point");
				player.setTimedItens(rset.getLong("item_time"));

				player.setArena1x1Wins(rset.getInt("1x1_wins"));
				player.setArena2x2Wins(rset.getInt("2x2_wins"));

				player.setFirstLog(rset.getInt("first_log"));
				player.setSelectArmor(rset.getInt("select_armor"));
				player.setSelectWeapon(rset.getInt("select_weapon"));
				player.setSelectClasse(rset.getInt("select_classe"));

				final int fakeArmor = rset.getInt("fakeArmorObjectId");
				final ItemInstance armorItem = player.getInventory().getItemByObjectId(fakeArmor);

				player.setFakeArmorObjectId(armorItem != null ? fakeArmor : 0);
				player.setFakeArmorItemId(armorItem != null ? armorItem.getItemId() : 0);

				final int fakeWeapon = rset.getInt("fakeWeaponObjectId");
				final ItemInstance weaponItem = player.getInventory().getItemByObjectId(fakeWeapon);

				player.setFakeWeaponObjectId(weaponItem != null ? fakeWeapon : 0);
				player.setFakeWeaponItemId(weaponItem != null ? weaponItem.getItemId() : 0);
				
				player.setActiveSkinId(rset.getInt("activeSkinId"));

				// Set the x,y,z position of the L2PcInstance and make it invisible
				player.setXYZInvisible(rset.getInt("x"), rset.getInt("y"), rset.getInt("z"));

				// Set Hero status if it applies
				if (Hero.getInstance().isActiveHero(objectId))
					player.setHero(true);

				// Set pledge class rank.
				player.setPledgeClass(L2ClanMember.calculatePledgeClass(player));

				// Retrieve from the database all secondary data of this L2PcInstance and reward expertise/lucky skills if necessary.
				// Note that Clan, Noblesse and Hero skills are given separately and not here.
				player.restoreCharData();
				player.rewardSkills();

				// buff and status icons
				if (Config.STORE_SKILL_COOLTIME)
					player.restoreEffects();

				// Restore current CP, HP and MP values
				final double currentHp = rset.getDouble("curHp");

				player.setCurrentCp(rset.getDouble("curCp"));
				player.setCurrentHp(currentHp);
				player.setCurrentMp(rset.getDouble("curMp"));

				if (currentHp < 0.5)
				{
					player.setIsDead(true);
					player.stopHpMpRegeneration();
				}

				// Restore pet if exists in the world
				player.setPet(L2World.getInstance().getPet(player.getObjectId()));
				if (player.getPet() != null)
					player.getPet().setOwner(player);

				player.refreshOverloaded();
				player.refreshExpertisePenalty();
				player.refreshArmorPenality();
				player.refreshWeaponPenality();

				player.restoreFriendList();
				PlayerVariables.loadVariables(player);
				//DailyRewardCBManager.getInstance().onPlayerEnter(player);
				// Retrieve the name and ID of the other characters assigned to this account.
				PreparedStatement stmt = con.prepareStatement("SELECT obj_Id, char_name FROM characters WHERE account_name=? AND obj_Id<>?");
				stmt.setString(1, player._accountName);
				stmt.setInt(2, objectId);
				ResultSet chars = stmt.executeQuery();

				while (chars.next())
					player._chars.put(chars.getInt("obj_Id"), chars.getString("char_name"));

				chars.close();
				stmt.close();
				break;
			}

			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.severe("Could not restore char data: " + e);
		}

		return player;
	}

	public Forum getMail()
	{
		if (_forumMail == null)
		{
			setMail(ForumsBBSManager.getInstance().getForumByName("MailRoot").getChildByName(getName()));

			if (_forumMail == null)
			{
				ForumsBBSManager.getInstance().createNewForum(getName(), ForumsBBSManager.getInstance().getForumByName("MailRoot"), Forum.MAIL, Forum.OWNERONLY, getObjectId());
				setMail(ForumsBBSManager.getInstance().getForumByName("MailRoot").getChildByName(getName()));
			}
		}

		return _forumMail;
	}

	public void setMail(Forum forum)
	{
		_forumMail = forum;
	}

	public Forum getMemo()
	{
		if (_forumMemo == null)
		{
			setMemo(ForumsBBSManager.getInstance().getForumByName("MemoRoot").getChildByName(_accountName));

			if (_forumMemo == null)
			{
				ForumsBBSManager.getInstance().createNewForum(_accountName, ForumsBBSManager.getInstance().getForumByName("MemoRoot"), Forum.MEMO, Forum.OWNERONLY, getObjectId());
				setMemo(ForumsBBSManager.getInstance().getForumByName("MemoRoot").getChildByName(_accountName));
			}
		}

		return _forumMemo;
	}

	public void setMemo(Forum forum)
	{
		_forumMemo = forum;
	}

	/**
	 * Restores sub-class data for the L2PcInstance, used to check the current class index for the character.
	 * @param player The player to make checks on.
	 * @return true if successful.
	 */
	private static boolean restoreSubClassData(L2PcInstance player)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement(RESTORE_CHAR_SUBCLASSES);
			statement.setInt(1, player.getObjectId());

			ResultSet rset = statement.executeQuery();

			while (rset.next())
			{
				SubClass subClass = new SubClass();
				subClass.setClassId(rset.getInt("class_id"));
				subClass.setLevel(rset.getByte("level"));
				subClass.setExp(rset.getLong("exp"));
				subClass.setSp(rset.getInt("sp"));
				subClass.setClassIndex(rset.getInt("class_index"));

				// Enforce the correct indexing of _subClasses against their class indexes.
				player.getSubClasses().put(subClass.getClassIndex(), subClass);
			}
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("Could not restore classes for " + player.getName() + ": " + e);
			e.printStackTrace();
		}

		return true;
	}

	/**
	 * Restores secondary data for the L2PcInstance, based on the current class index.
	 */
	private void restoreCharData()
	{
		// Retrieve from the database all skills of this L2PcInstance and add them to _skills.
		restoreSkills();

		// Retrieve from the database all macroses of this L2PcInstance and add them to _macroses.
		_macroses.restore();

		// Retrieve from the database all shortCuts of this L2PcInstance and add them to _shortCuts.
		_shortCuts.restore();

		// Retrieve from the database all henna of this L2PcInstance and add them to _henna.
		restoreHenna();

		// Retrieve from the database all recom data of this L2PcInstance and add to _recomChars.
		restoreRecom();

		// Retrieve from the database the recipe book of this L2PcInstance.
		if (!isSubClassActive())
			restoreRecipeBook();
	}

	/**
	 * Store recipe book data for this L2PcInstance, if not on an active sub-class.
	 */
	private void storeRecipeBook()
	{
		// If the player is on a sub-class don't even attempt to store a recipe book.
		if (isSubClassActive())
			return;

		if (getCommonRecipeBook().isEmpty() && getDwarvenRecipeBook().isEmpty())
			return;

		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("DELETE FROM character_recipebook WHERE char_id=?");
			statement.setInt(1, getObjectId());
			statement.execute();
			statement.close();

			for (RecipeList recipe : getCommonRecipeBook())
			{
				statement = con.prepareStatement("INSERT INTO character_recipebook (char_id, id, type) values(?,?,0)");
				statement.setInt(1, getObjectId());
				statement.setInt(2, recipe.getId());
				statement.execute();
				statement.close();
			}

			for (RecipeList recipe : getDwarvenRecipeBook())
			{
				statement = con.prepareStatement("INSERT INTO character_recipebook (char_id, id, type) values(?,?,1)");
				statement.setInt(1, getObjectId());
				statement.setInt(2, recipe.getId());
				statement.execute();
				statement.close();
			}
		}
		catch (Exception e)
		{
			_log.warning("Could not store recipe book data: " + e);
		}
	}

	/**
	 * Restore recipe book data for this L2PcInstance.
	 */
	private void restoreRecipeBook()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("SELECT id, type FROM character_recipebook WHERE char_id=?");
			statement.setInt(1, getObjectId());
			ResultSet rset = statement.executeQuery();

			while (rset.next())
			{
				final RecipeList recipe = RecipeTable.getInstance().getRecipeList(rset.getInt("id"));
				if (rset.getInt("type") == 1)
					registerDwarvenRecipeList(recipe);
				else
					registerCommonRecipeList(recipe);
			}

			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("Could not restore recipe book data:" + e);
		}
	}

	/**
	 * Update L2PcInstance stats in the characters table of the database.
	 * @param storeActiveEffects
	 */
	public synchronized void store(boolean storeActiveEffects)
	{
		// update client coords, if these look like true
		if (isInsideRadius(getClientX(), getClientY(), 1000, true))
			setXYZ(getClientX(), getClientY(), getClientZ());

		storeCharBase();
		storeCharSub();
		storeEffect(storeActiveEffects);
		storeRecipeBook();
		SevenSigns.getInstance().saveSevenSignsData(getObjectId());
	}

	public void store()
	{
		store(true);
	}

	private void storeCharBase()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			// Get the exp, level, and sp of base class to store in base table
			int currentClassIndex = getClassIndex();
			_classIndex = 0;
			long exp = getStat().getExp();
			int level = getStat().getLevel();
			int sp = getStat().getSp();
			_classIndex = currentClassIndex;

			PreparedStatement statement = con.prepareStatement(UPDATE_CHARACTER);

			statement.setInt(1, level);
			statement.setInt(2, getMaxHp());
			statement.setDouble(3, getCurrentHp());
			statement.setInt(4, getMaxCp());
			statement.setDouble(5, getCurrentCp());
			statement.setInt(6, getMaxMp());
			statement.setDouble(7, getCurrentMp());
			statement.setInt(8, getAppearance().getFace());
			statement.setInt(9, getAppearance().getHairStyle());
			statement.setInt(10, getAppearance().getHairColor());
			statement.setInt(11, getAppearance().getSex() ? 1 : 0);
			statement.setInt(12, getHeading());
			statement.setInt(13, _observerMode ? _lastX : getX());
			statement.setInt(14, _observerMode ? _lastY : getY());
			statement.setInt(15, _observerMode ? _lastZ : getZ());
			statement.setLong(16, exp);
			statement.setLong(17, getExpBeforeDeath());
			statement.setInt(18, sp);
			statement.setInt(19, getKarma());
			statement.setInt(20, getPvpKills());
			statement.setInt(21, getPkKills());
			statement.setInt(22, getDeaths());
			statement.setInt(23, getRecomHave());
			statement.setInt(24, getRecomLeft());
			statement.setInt(25, getClanId());
			statement.setInt(26, getRace().ordinal());
			statement.setInt(27, getClassId().getId());
			statement.setLong(28, getDeleteTimer());
			//statement.setString(29, getTitle());
			statement.setString(29, getSecondaryAutoFarmStoredTitle());
			statement.setInt(30, getAccessLevel().getLevel());
			statement.setInt(31, isOnlineInt());
			statement.setInt(32, isIn7sDungeon() ? 1 : 0);
			statement.setInt(33, getClanPrivileges());
			statement.setInt(34, wantsPeace() ? 1 : 0);
			statement.setInt(35, getBaseClass());

			long totalOnlineTime = _onlineTime;
			if (_onlineBeginTime > 0)
				totalOnlineTime += (System.currentTimeMillis() - _onlineBeginTime) / 1000;

			statement.setLong(36, totalOnlineTime);
			statement.setInt(37, getPunishLevel().value());
			statement.setLong(38, getPunishTimer());
			statement.setInt(39, isNoble() ? 1 : 0);
			statement.setLong(40, getPowerGrade());
			statement.setInt(41, getPledgeType());
			statement.setLong(42, getLastRecomUpdate());
			statement.setInt(43, getLvlJoinedAcademy());
			statement.setLong(44, getApprentice());
			statement.setLong(45, getSponsor());
			statement.setInt(46, getAllianceWithVarkaKetra());
			statement.setLong(47, getClanJoinExpiryTime());
			statement.setLong(48, getClanCreateExpiryTime());
			statement.setString(49, getName());
			statement.setLong(50, getDeathPenaltyBuffLevel());
			statement.setString(51, StringToHex(Integer.toHexString(getAppearance().getNameColor()).toUpperCase()));
			//statement.setString(52, StringToHex(Integer.toHexString(getAppearance().getTitleColor()).toUpperCase()));
			statement.setString(52, StringToHex(Integer.toHexString(getSecondaryAutoFarmStoredTitleColor()).toUpperCase()));
			statement.setInt(53, isAio() ? 1 : 0);
			statement.setLong(54, getAioEndTime());
			statement.setInt(55, isVip() ? 1 : 0);
			statement.setLong(56, getVipEndTime());
			statement.setInt(57, isTimedHero() ? 1 : 0);
			statement.setLong(58, getTimedHeroEndTime());
			statement.setInt(59, getPcBangScore());
			statement.setLong(60, getTimedItens());
			statement.setInt(61, _arena1x1Wins);
			statement.setInt(62, _arena2x2Wins);
			statement.setLong(63, getFakeArmorObjectId());
			statement.setLong(64, getFakeArmorItemId());
			statement.setLong(65, getFakeWeaponObjectId());
			statement.setLong(66, getFakeWeaponItemId());
			statement.setInt(67, getActiveSkinId());
			statement.setInt(68, getObjectId());

			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("Could not store char base data: " + e);
		}
	}

	private void storeCharSub()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement(UPDATE_CHAR_SUBCLASS);

			if (getTotalSubClasses() > 0)
			{
				for (SubClass subClass : getSubClasses().values())
				{
					statement.setLong(1, subClass.getExp());
					statement.setInt(2, subClass.getSp());
					statement.setInt(3, subClass.getLevel());
					statement.setInt(4, subClass.getClassId());
					statement.setInt(5, getObjectId());
					statement.setInt(6, subClass.getClassIndex());
					statement.execute();
					statement.clearParameters();
				}
			}
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("Could not store sub class data for " + getName() + ": " + e);
		}
	}

	@SuppressWarnings("incomplete-switch")
	private void storeEffect(boolean storeEffects)
	{
		if (!Config.STORE_SKILL_COOLTIME)
			return;

		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			// Delete all current stored effects for char to avoid dupe
			PreparedStatement statement = con.prepareStatement(DELETE_SKILL_SAVE);

			statement.setInt(1, getObjectId());
			statement.setInt(2, getClassIndex());
			statement.execute();
			statement.close();

			int buff_index = 0;

			final List<Integer> storedSkills = new ArrayList<>();

			// Store all effect data along with calulated remaining reuse delays for matching skills. 'restore_type'= 0.
			statement = con.prepareStatement(ADD_SKILL_SAVE);

			if (storeEffects)
			{
				for (L2Effect effect : getAllEffects())
				{
					if (effect == null)
						continue;

					switch (effect.getEffectType())
					{
					case HEAL_OVER_TIME:
					case COMBAT_POINT_HEAL_OVER_TIME:
						continue;
					}

					L2Skill skill = effect.getSkill();
					if (storedSkills.contains(skill.getReuseHashCode()))
						continue;

					storedSkills.add(skill.getReuseHashCode());

					if (!effect.isHerbEffect() && effect.getInUse() && !skill.isToggle())
					{
						statement.setInt(1, getObjectId());
						statement.setInt(2, skill.getId());
						statement.setInt(3, skill.getLevel());
						statement.setInt(4, effect.getCount());
						statement.setInt(5, effect.getTime());

						if (_reuseTimeStamps.containsKey(skill.getReuseHashCode()))
						{
							TimeStamp t = _reuseTimeStamps.get(skill.getReuseHashCode());
							statement.setLong(6, t.hasNotPassed() ? t.getReuse() : 0);
							statement.setDouble(7, t.hasNotPassed() ? t.getStamp() : 0);
						}
						else
						{
							statement.setLong(6, 0);
							statement.setDouble(7, 0);
						}

						statement.setInt(8, 0);
						statement.setInt(9, getClassIndex());
						statement.setInt(10, ++buff_index);
						statement.execute();
					}
				}
			}

			// Store the reuse delays of remaining skills which lost effect but still under reuse delay. 'restore_type' 1.
			for (int hash : _reuseTimeStamps.keySet())
			{
				if (storedSkills.contains(hash))
					continue;

				TimeStamp t = _reuseTimeStamps.get(hash);
				if (t != null && t.hasNotPassed())
				{
					storedSkills.add(hash);

					statement.setInt(1, getObjectId());
					statement.setInt(2, t.getSkillId());
					statement.setInt(3, t.getSkillLvl());
					statement.setInt(4, -1);
					statement.setInt(5, -1);
					statement.setLong(6, t.getReuse());
					statement.setDouble(7, t.getStamp());
					statement.setInt(8, 1);
					statement.setInt(9, getClassIndex());
					statement.setInt(10, ++buff_index);
					statement.execute();
				}
			}
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Could not store char effect data: ", e);
		}
	}

	/**
	 * @return True if the L2PcInstance is online.
	 */
	public boolean isOnline()
	{
		return _isOnline;
	}

	/**
	 * @return an int interpretation of online status.
	 */
	public int isOnlineInt()
	{
		if (_isOnline && getClient() != null)
			return getClient().isDetached() ? 2 : 1;

		return 0;
	}

	public boolean isIn7sDungeon()
	{
		return _isIn7sDungeon;
	}

	/**
	 * Add a skill to the L2PcInstance _skills and its Func objects to the calculator set of the L2PcInstance and save update in the character_skills table of the database.
	 * <ul>
	 * <li>Replace oldSkill by newSkill or Add the newSkill</li>
	 * <li>If an old skill has been replaced, remove all its Func objects of L2Character calculator set</li>
	 * <li>Add Func objects of newSkill to the calculator set of the L2Character</li>
	 * </ul>
	 * @param newSkill The L2Skill to add to the L2Character
	 * @param store
	 * @return The L2Skill replaced or null if just added a new L2Skill
	 */
	public L2Skill addSkill(L2Skill newSkill, boolean store)
	{
		// Add a skill to the L2PcInstance _skills and its Func objects to the calculator set of the L2PcInstance
		L2Skill oldSkill = super.addSkill(newSkill);

		// Add or update a L2PcInstance skill in the character_skills table of the database
		if (store)
			storeSkill(newSkill, oldSkill, -1);

		return oldSkill;
	}

	@Override
	public L2Skill removeSkill(L2Skill skill, boolean store)
	{
		if (store)
			return removeSkill(skill);

		return super.removeSkill(skill, true);
	}

	public L2Skill removeSkill(L2Skill skill, boolean store, boolean cancelEffect)
	{
		if (store)
			return removeSkill(skill);

		return super.removeSkill(skill, cancelEffect);
	}

	/**
	 * Remove a skill from the L2Character and its Func objects from calculator set of the L2Character and save update in the character_skills table of the database.
	 * <ul>
	 * <li>Remove the skill from the L2Character _skills</li>
	 * <li>Remove all its Func objects from the L2Character calculator set</li>
	 * </ul>
	 * @param skill The L2Skill to remove from the L2Character
	 * @return The L2Skill removed
	 */
	@Override
	public L2Skill removeSkill(L2Skill skill)
	{
		// Remove a skill from the L2Character and its Func objects from calculator set of the L2Character
		L2Skill oldSkill = super.removeSkill(skill);

		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement(DELETE_SKILL_FROM_CHAR);

			if (oldSkill != null)
			{
				statement.setInt(1, oldSkill.getId());
				statement.setInt(2, getObjectId());
				statement.setInt(3, getClassIndex());
				statement.execute();
			}
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("Error could not delete skill: " + e);
		}

		// Don't busy with shortcuts if skill was a passive skill.
		if (skill != null && !skill.isPassive())
		{
			for (L2ShortCut sc : getAllShortCuts())
			{
				if (sc != null && sc.getId() == skill.getId() && sc.getType() == L2ShortCut.TYPE_SKILL)
					deleteShortCut(sc.getSlot(), sc.getPage());
			}
		}

		return oldSkill;
	}

	/**
	 * Add or update a L2PcInstance skill in the character_skills table of the database. <BR>
	 * <BR>
	 * If newClassIndex > -1, the skill will be stored with that class index, not the current one.
	 * @param newSkill
	 * @param oldSkill
	 * @param newClassIndex
	 */
	private void storeSkill(L2Skill newSkill, L2Skill oldSkill, int newClassIndex)
	{
		int classIndex = _classIndex;

		if (newClassIndex > -1)
			classIndex = newClassIndex;

		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement;

			if (oldSkill != null && newSkill != null)
			{
				statement = con.prepareStatement(UPDATE_CHARACTER_SKILL_LEVEL);
				statement.setInt(1, newSkill.getLevel());
				statement.setInt(2, oldSkill.getId());
				statement.setInt(3, getObjectId());
				statement.setInt(4, classIndex);
				statement.execute();
				statement.close();
			}
			else if (newSkill != null)
			{
				statement = con.prepareStatement(ADD_NEW_SKILL);
				statement.setInt(1, getObjectId());
				statement.setInt(2, newSkill.getId());
				statement.setInt(3, newSkill.getLevel());
				statement.setInt(4, classIndex);
				statement.execute();
				statement.close();
			}
			else
			{
				_log.warning("storeSkill() couldn't store new skill. It's null type.");
			}
		}
		catch (Exception e)
		{
			_log.warning("Error could not store char skills: " + e);
		}
	}

	/**
	 * Retrieve from the database all skills of this L2PcInstance and add them to _skills.
	 */
	private void restoreSkills()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement(RESTORE_SKILLS_FOR_CHAR);
			statement.setInt(1, getObjectId());
			statement.setInt(2, getClassIndex());
			ResultSet rset = statement.executeQuery();

			// Go though the recordset of this SQL query
			while (rset.next())
			{
				int id = rset.getInt("skill_id");
				int level = rset.getInt("skill_level");

				// Create a L2Skill object for each record
				L2Skill skill = SkillTable.getInstance().getInfo(id, level);

				// Add the L2Skill object to the L2Character _skills and its Func objects to the calculator set of the L2Character
				super.addSkill(skill);
			}

			rset.close();
			statement.close();

			if (SiegeManager.ALLOW_ADDING_SKILL_FOR_CL_CASTLE && getClan() != null && CastleManager.getInstance().getCastleByOwner(getClan()) != null)
			{
				String _castleName = CastleManager.getInstance().getCastleByOwner(getClan()).getName();
				if (isClanLeader())
				{
					if (_castleName.equalsIgnoreCase("Gludio"))
					{
						for (int[] addingSkill : SiegeManager.ADDING_SKILL_FOR_CLAN_LEADER_CASTLE_Gludio)
						{
							L2Skill skill = SkillTable.getInstance().getInfo(addingSkill[0], addingSkill[1]);
							if (skill != null)
								addSkill(skill); 
						} 
						for (int[] addingSkill : SiegeManager.ADDING_SKILL_FOR_CLAN_CASTLE_Gludio) 
						{
							L2Skill skill = SkillTable.getInstance().getInfo(addingSkill[0], addingSkill[1]);
							if (skill != null)
								addSkill(skill); 
						} 
					}
					else if (_castleName.equalsIgnoreCase("Dion")) 
					{
						for (int[] addingSkill : SiegeManager.ADDING_SKILL_FOR_CLAN_LEADER_CASTLE_Dion)
						{
							L2Skill skill = SkillTable.getInstance().getInfo(addingSkill[0], addingSkill[1]);
							if (skill != null)
								addSkill(skill); 
						} 
						for (int[] addingSkill : SiegeManager.ADDING_SKILL_FOR_CLAN_CASTLE_Dion)
						{
							L2Skill skill = SkillTable.getInstance().getInfo(addingSkill[0], addingSkill[1]);
							if (skill != null)
								addSkill(skill); 
						} 
					}
					else if (_castleName.equalsIgnoreCase("Giran"))
					{
						for (int[] addingSkill : SiegeManager.ADDING_SKILL_FOR_CLAN_LEADER_CASTLE_Giran)
						{
							L2Skill skill = SkillTable.getInstance().getInfo(addingSkill[0], addingSkill[1]);
							if (skill != null)
								addSkill(skill); 
						} 
						for (int[] addingSkill : SiegeManager.ADDING_SKILL_FOR_CLAN_CASTLE_Giran)
						{
							L2Skill skill = SkillTable.getInstance().getInfo(addingSkill[0], addingSkill[1]);
							if (skill != null)
								addSkill(skill); 
						} 
					} 
					else if (_castleName.equalsIgnoreCase("Oren"))
					{
						for (int[] addingSkill : SiegeManager.ADDING_SKILL_FOR_CLAN_LEADER_CASTLE_Oren)
						{
							L2Skill skill = SkillTable.getInstance().getInfo(addingSkill[0], addingSkill[1]);
							if (skill != null)
								addSkill(skill); 
						} 
						for (int[] addingSkill : SiegeManager.ADDING_SKILL_FOR_CLAN_CASTLE_Oren)
						{
							L2Skill skill = SkillTable.getInstance().getInfo(addingSkill[0], addingSkill[1]);
							if (skill != null)
								addSkill(skill); 
						} 
					} 
					else if (_castleName.equalsIgnoreCase("Aden"))
					{
						for (int[] addingSkill : SiegeManager.ADDING_SKILL_FOR_CLAN_LEADER_CASTLE_Aden) 
						{
							L2Skill skill = SkillTable.getInstance().getInfo(addingSkill[0], addingSkill[1]);
							if (skill != null)
								addSkill(skill); 
						} 
						for (int[] addingSkill : SiegeManager.ADDING_SKILL_FOR_CLAN_CASTLE_Aden)
						{
							L2Skill skill = SkillTable.getInstance().getInfo(addingSkill[0], addingSkill[1]);
							if (skill != null)
								addSkill(skill); 
						} 
					}
					else if (_castleName.equalsIgnoreCase("Innadril"))
					{
						for (int[] addingSkill : SiegeManager.ADDING_SKILL_FOR_CLAN_LEADER_CASTLE_Innadril)
						{
							L2Skill skill = SkillTable.getInstance().getInfo(addingSkill[0], addingSkill[1]);
							if (skill != null)
								addSkill(skill); 
						} 
						for (int[] addingSkill : SiegeManager.ADDING_SKILL_FOR_CLAN_CASTLE_Innadril) 
						{
							L2Skill skill = SkillTable.getInstance().getInfo(addingSkill[0], addingSkill[1]);
							if (skill != null)
								addSkill(skill); 
						} 
					} 
					else if (_castleName.equalsIgnoreCase("Goddard"))
					{
						for (int[] addingSkill : SiegeManager.ADDING_SKILL_FOR_CLAN_LEADER_CASTLE_Goddard)
						{
							L2Skill skill = SkillTable.getInstance().getInfo(addingSkill[0], addingSkill[1]);
							if (skill != null)
								addSkill(skill); 
						} 
						for (int[] addingSkill : SiegeManager.ADDING_SKILL_FOR_CLAN_CASTLE_Goddard) 
						{
							L2Skill skill = SkillTable.getInstance().getInfo(addingSkill[0], addingSkill[1]);
							if (skill != null)
								addSkill(skill); 
						} 
					}
					else if (_castleName.equalsIgnoreCase("Rune"))
					{
						for (int[] addingSkill : SiegeManager.ADDING_SKILL_FOR_CLAN_LEADER_CASTLE_Rune)
						{
							L2Skill skill = SkillTable.getInstance().getInfo(addingSkill[0], addingSkill[1]);
							if (skill != null)
								addSkill(skill); 
						} 
						for (int[] addingSkill : SiegeManager.ADDING_SKILL_FOR_CLAN_CASTLE_Rune) 
						{
							L2Skill skill = SkillTable.getInstance().getInfo(addingSkill[0], addingSkill[1]);
							if (skill != null)
								addSkill(skill); 
						} 
					}
					else if (_castleName.equalsIgnoreCase("Schuttgart")) 
					{
						for (int[] addingSkill : SiegeManager.ADDING_SKILL_FOR_CLAN_LEADER_CASTLE_Schuttgart)
						{
							L2Skill skill = SkillTable.getInstance().getInfo(addingSkill[0], addingSkill[1]);
							if (skill != null)
								addSkill(skill); 
						} 
						for (int[] addingSkill : SiegeManager.ADDING_SKILL_FOR_CLAN_CASTLE_Schuttgart)
						{
							L2Skill skill = SkillTable.getInstance().getInfo(addingSkill[0], addingSkill[1]);
							if (skill != null)
								addSkill(skill); 
						} 
					} 
				} 
				else if (_castleName.equalsIgnoreCase("Gludio"))
				{
					for (int[] addingSkill : SiegeManager.ADDING_SKILL_FOR_CLAN_CASTLE_Gludio) 
					{
						L2Skill skill = SkillTable.getInstance().getInfo(addingSkill[0], addingSkill[1]);
						if (skill != null)
							addSkill(skill); 
					} 
				}
				else if (_castleName.equalsIgnoreCase("Dion"))
				{
					for (int[] addingSkill : SiegeManager.ADDING_SKILL_FOR_CLAN_CASTLE_Dion)
					{
						L2Skill skill = SkillTable.getInstance().getInfo(addingSkill[0], addingSkill[1]);
						if (skill != null)
							addSkill(skill); 
					} 
				}
				else if (_castleName.equalsIgnoreCase("Giran"))
				{
					for (int[] addingSkill : SiegeManager.ADDING_SKILL_FOR_CLAN_CASTLE_Giran) 
					{
						L2Skill skill = SkillTable.getInstance().getInfo(addingSkill[0], addingSkill[1]);
						if (skill != null)
							addSkill(skill); 
					} 
				}
				else if (_castleName.equalsIgnoreCase("Oren")) 
				{
					for (int[] addingSkill : SiegeManager.ADDING_SKILL_FOR_CLAN_CASTLE_Oren) 
					{
						L2Skill skill = SkillTable.getInstance().getInfo(addingSkill[0], addingSkill[1]);
						if (skill != null)
							addSkill(skill); 
					} 
				}
				else if (_castleName.equalsIgnoreCase("Aden")) 
				{
					for (int[] addingSkill : SiegeManager.ADDING_SKILL_FOR_CLAN_CASTLE_Aden)
					{
						L2Skill skill = SkillTable.getInstance().getInfo(addingSkill[0], addingSkill[1]);
						if (skill != null)
							addSkill(skill); 
					} 
				}
				else if (_castleName.equalsIgnoreCase("Innadril"))
				{
					for (int[] addingSkill : SiegeManager.ADDING_SKILL_FOR_CLAN_CASTLE_Innadril)
					{
						L2Skill skill = SkillTable.getInstance().getInfo(addingSkill[0], addingSkill[1]);
						if (skill != null)
							addSkill(skill); 
					} 
				} 
				else if (_castleName.equalsIgnoreCase("Goddard"))
				{
					for (int[] addingSkill : SiegeManager.ADDING_SKILL_FOR_CLAN_CASTLE_Goddard)
					{
						L2Skill skill = SkillTable.getInstance().getInfo(addingSkill[0], addingSkill[1]);
						if (skill != null)
							addSkill(skill); 
					} 
				}
				else if (_castleName.equalsIgnoreCase("Rune")) 
				{
					for (int[] addingSkill : SiegeManager.ADDING_SKILL_FOR_CLAN_CASTLE_Rune)
					{
						L2Skill skill = SkillTable.getInstance().getInfo(addingSkill[0], addingSkill[1]);
						if (skill != null)
							addSkill(skill); 
					} 
				}
				else if (_castleName.equalsIgnoreCase("Schuttgart"))
				{
					for (int[] addingSkill : SiegeManager.ADDING_SKILL_FOR_CLAN_CASTLE_Schuttgart)
					{
						L2Skill skill = SkillTable.getInstance().getInfo(addingSkill[0], addingSkill[1]);
						if (skill != null)
							addSkill(skill); 
					} 
				} 
			}
		}
		catch (Exception e)
		{
			_log.warning("Could not restore character skills: " + e);
		}
	}

	/**
	 * Retrieve from the database all skill effects of this L2PcInstance and add them to the player.
	 */
	public void restoreEffects()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement(RESTORE_SKILL_SAVE);
			statement.setInt(1, getObjectId());
			statement.setInt(2, getClassIndex());
			ResultSet rset = statement.executeQuery();

			while (rset.next())
			{
				int effectCount = rset.getInt("effect_count");
				int effectCurTime = rset.getInt("effect_cur_time");
				long reuseDelay = rset.getLong("reuse_delay");
				long systime = rset.getLong("systime");
				int restoreType = rset.getInt("restore_type");

				final L2Skill skill = SkillTable.getInstance().getInfo(rset.getInt("skill_id"), rset.getInt("skill_level"));
				if (skill == null)
					continue;

				if (skill.getId() == 426 || skill.getId() == 427 || skill.getId() == 5104 || skill.getId() == 5105)
					continue;

				final long remainingTime = systime - System.currentTimeMillis();
				if (remainingTime > 10)
				{
					disableSkill(skill, remainingTime);
					addTimeStamp(skill, reuseDelay, systime);
				}

				/**
				 * Restore Type 1 The remaning skills lost effect upon logout but were still under a high reuse delay.
				 */
				if (restoreType > 0)
					continue;

				/**
				 * Restore Type 0 These skills were still in effect on the character upon logout. Some of which were self casted and might still have a long reuse delay which also is restored.
				 */
				if (skill.hasEffects())
				{
					final Env env = new Env();
					env.setCharacter(this);
					env.setTarget(this);
					env.setSkill(skill);

					for (EffectTemplate et : skill.getEffectTemplates())
					{
						final L2Effect ef = et.getEffect(env);
						if (ef != null)
						{
							ef.setCount(effectCount);
							ef.setFirstTime(effectCurTime);
							ef.scheduleEffect();
						}
					}
				}
			}

			rset.close();
			statement.close();

			statement = con.prepareStatement(DELETE_SKILL_SAVE);
			statement.setInt(1, getObjectId());
			statement.setInt(2, getClassIndex());
			statement.executeUpdate();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Could not restore " + this + " active effect data: " + e.getMessage(), e);
		}
	}

	/**
	 * Retrieve from the database all Henna of this L2PcInstance, add them to _henna and calculate stats of the L2PcInstance.
	 */
	private void restoreHenna()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement(RESTORE_CHAR_HENNAS);
			statement.setInt(1, getObjectId());
			statement.setInt(2, getClassIndex());
			ResultSet rset = statement.executeQuery();

			for (int i = 0; i < 3; i++)
				_henna[i] = null;

			while (rset.next())
			{
				int slot = rset.getInt("slot");

				if (slot < 1 || slot > 3)
					continue;

				int symbolId = rset.getInt("symbol_id");
				if (symbolId != 0)
				{
					Henna tpl = HennaTable.getInstance().getTemplate(symbolId);
					if (tpl != null)
						_henna[slot - 1] = tpl;
				}
			}

			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("could not restore henna: " + e);
		}

		// Calculate Henna modifiers of this L2PcInstance
		recalcHennaStats();
	}

	/**
	 * Retrieve from the database all Recommendation data of this L2PcInstance, add to _recomChars and calculate stats of the L2PcInstance.
	 */
	private void restoreRecom()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement(RESTORE_CHAR_RECOMS);
			statement.setInt(1, getObjectId());
			ResultSet rset = statement.executeQuery();
			while (rset.next())
				_recomChars.add(rset.getInt("target_id"));

			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("could not restore recommendations: " + e);
		}
	}

	/**
	 * @return the number of Henna empty slot of the L2PcInstance.
	 */
	public int getHennaEmptySlots()
	{
		int totalSlots = 0;
		if (getClassId().level() == 1)
			totalSlots = 2;
		else
			totalSlots = 3;

		for (int i = 0; i < 3; i++)
		{
			if (_henna[i] != null)
				totalSlots--;
		}

		if (totalSlots <= 0)
			return 0;

		return totalSlots;
	}

	/**
	 * Remove a Henna of the L2PcInstance, save update in the character_hennas table of the database and send HennaInfo/UserInfo packet to this L2PcInstance.
	 * @param slot The slot number to make checks on.
	 * @return true if successful.
	 */
	public boolean removeHenna(int slot)
	{
		if (slot < 1 || slot > 3)
			return false;

		slot--;

		if (_henna[slot] == null)
			return false;

		Henna henna = _henna[slot];
		_henna[slot] = null;

		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement(DELETE_CHAR_HENNA);

			statement.setInt(1, getObjectId());
			statement.setInt(2, slot + 1);
			statement.setInt(3, getClassIndex());

			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("could not remove char henna: " + e);
		}

		// Calculate Henna modifiers of this L2PcInstance
		recalcHennaStats();

		// Send HennaInfo packet to this L2PcInstance
		sendPacket(new HennaInfo(this));

		// Send UserInfo packet to this L2PcInstance
		sendPacket(new UserInfo(this));

		reduceAdena("Henna", henna.getPrice() / 5, this, false);

		// Add the recovered dyes to the player's inventory and notify them.
		addItem("Henna", henna.getDyeId(), Henna.getAmountDyeRequire() / 2, this, true);
		sendPacket(SystemMessageId.SYMBOL_DELETED);
		return true;
	}

	/**
	 * Add a Henna to the L2PcInstance, save update in the character_hennas table of the database and send Server->Client HennaInfo/UserInfo packet to this L2PcInstance.
	 * @param henna The Henna template to add.
	 */
	public void addHenna(Henna henna)
	{
		for (int i = 0; i < 3; i++)
		{
			if (_henna[i] == null)
			{
				_henna[i] = henna;

				// Calculate Henna modifiers of this L2PcInstance
				recalcHennaStats();

				try (Connection con = L2DatabaseFactory.getInstance().getConnection())
				{
					PreparedStatement statement = con.prepareStatement(ADD_CHAR_HENNA);

					statement.setInt(1, getObjectId());
					statement.setInt(2, henna.getSymbolId());
					statement.setInt(3, i + 1);
					statement.setInt(4, getClassIndex());

					statement.execute();
					statement.close();
				}
				catch (Exception e)
				{
					_log.warning("could not save char henna: " + e);
				}

				sendPacket(new HennaInfo(this));
				sendPacket(new UserInfo(this));
				sendPacket(SystemMessageId.SYMBOL_ADDED);
				return;
			}
		}
	}

	/**
	 * Calculate Henna modifiers of this L2PcInstance.
	 */
	private void recalcHennaStats()
	{
		_hennaINT = 0;
		_hennaSTR = 0;
		_hennaCON = 0;
		_hennaMEN = 0;
		_hennaWIT = 0;
		_hennaDEX = 0;

		for (int i = 0; i < 3; i++)
		{
			if (_henna[i] == null)
				continue;

			_hennaINT += _henna[i].getStatINT();
			_hennaSTR += _henna[i].getStatSTR();
			_hennaMEN += _henna[i].getStatMEN();
			_hennaCON += _henna[i].getStatCON();
			_hennaWIT += _henna[i].getStatWIT();
			_hennaDEX += _henna[i].getStatDEX();
		}

		if (_hennaINT > 5)
			_hennaINT = 5;

		if (_hennaSTR > 5)
			_hennaSTR = 5;

		if (_hennaMEN > 5)
			_hennaMEN = 5;

		if (_hennaCON > 5)
			_hennaCON = 5;

		if (_hennaWIT > 5)
			_hennaWIT = 5;

		if (_hennaDEX > 5)
			_hennaDEX = 5;
	}

	/**
	 * @param slot A slot to check.
	 * @return the Henna of this L2PcInstance corresponding to the selected slot.
	 */
	public Henna getHenna(int slot)
	{
		if (slot < 1 || slot > 3)
			return null;

		return _henna[slot - 1];
	}

	public int getHennaStatINT()
	{
		return _hennaINT;
	}

	public int getHennaStatSTR()
	{
		return _hennaSTR;
	}

	public int getHennaStatCON()
	{
		return _hennaCON;
	}

	public int getHennaStatMEN()
	{
		return _hennaMEN;
	}

	public int getHennaStatWIT()
	{
		return _hennaWIT;
	}

	public int getHennaStatDEX()
	{
		return _hennaDEX;
	}

	/**
	 * Return True if the L2PcInstance is autoAttackable.
	 * <ul>
	 * <li>Check if the attacker isn't the L2PcInstance Pet</li>
	 * <li>Check if the attacker is L2MonsterInstance</li>
	 * <li>If the attacker is a L2PcInstance, check if it is not in the same party</li>
	 * <li>Check if the L2PcInstance has Karma</li>
	 * <li>If the attacker is a L2PcInstance, check if it is not in the same siege clan (Attacker, Defender)</li>
	 * </ul>
	 */
	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		if (PlayerModeManager.blocksPvp(attacker, this))
		{
		    return false;
		}
		
		// Check if the attacker isn't the L2PcInstance Pet
		if (attacker == this || attacker == getPet())
			return false;

		// Check if the attacker is a L2MonsterInstance
		if (attacker instanceof L2MonsterInstance)
			return true;

		// Check if the attacker is not in the same party
		if (getParty() != null && getParty().getPartyMembers().contains(attacker) && !(isInsideZone(ZoneId.AUTO_PVP_ZONE) && attacker.isInsideZone(ZoneId.AUTO_PVP_ZONE)))
			return false;

		// Check if the attacker is a L2Playable
		if (attacker instanceof L2Playable)
		{
			if (isInsideZone(ZoneId.PEACE))
				return false;

			// Get L2PcInstance
			final L2PcInstance cha = attacker.getActingPlayer();

			// Check if the attacker is in olympiad and olympiad start
			if (attacker instanceof L2PcInstance && cha.isInOlympiadMode())
			{
				if (isInOlympiadMode() && isOlympiadStart() && cha.getOlympiadGameId() == getOlympiadGameId())
					return true;

				return false;
			}

			// is AutoAttackable if both players are in the same duel and the duel is still going on
			if (getDuelState() == Duel.DUELSTATE_DUELLING && getDuelId() == cha.getDuelId())
				return true;

			if (getClan() != null)
			{
				final Siege siege = SiegeManager.getSiege(getX(), getY(), getZ());
				if (siege != null)
				{
					// Check if a siege is in progress and if attacker and the L2PcInstance aren't in the Defender clan
					if (siege.checkIsDefender(cha.getClan()) && siege.checkIsDefender(getClan()))
						return false;

					// Check if a siege is in progress and if attacker and the L2PcInstance aren't in the Attacker clan
					//if (siege.checkIsAttacker(cha.getClan()) && siege.checkIsAttacker(getClan()))
					//	return false;
				}

				// Check if clan is at war
				if (getClan().isAtWarWith(cha.getClanId()) && !wantsPeace() && !cha.wantsPeace() && !isAcademyMember())
					return true;
			}

			// Check if the L2PcInstance is in an arena.
			if (isInArena() && attacker.isInArena())
				return true;

			// Check if the attacker is not in the same ally.
			if (getAllyId() != 0 && getAllyId() == cha.getAllyId() && !(isInsideZone(ZoneId.AUTO_PVP_ZONE) && attacker.isInsideZone(ZoneId.AUTO_PVP_ZONE)))
				return false;

			// Check if the attacker is not in the same clan.
			if (getClan() != null && getClan().isMember(cha.getObjectId()) && !(isInsideZone(ZoneId.AUTO_PVP_ZONE) && attacker.isInsideZone(ZoneId.AUTO_PVP_ZONE)))
				return false;

			// Now check again if the L2PcInstance is in pvp zone (as arenas check was made before, it ends with sieges).
			if (isInsideZone(ZoneId.AUTO_PVP_ZONE) && attacker.isInsideZone(ZoneId.AUTO_PVP_ZONE) || isInsideZone(ZoneId.PVP) && attacker.isInsideZone(ZoneId.PVP) || isInsideZone(ZoneId.ARENA_EVENT) && attacker.isInsideZone(ZoneId.ARENA_EVENT) || isInsideZone(ZoneId.HEAVY_FARM_AREA) && attacker.isInsideZone(ZoneId.HEAVY_FARM_AREA) || isInsideZone(ZoneId.SPECIAL_BOSS_AREA) && attacker.isInsideZone(ZoneId.SPECIAL_BOSS_AREA))
				return true;

			// Check if the attacker is in CTF and CTF is started
			if (CTFEvent.isStarted() && CTFEvent.isPlayerParticipant(getObjectId()))
				return true;

			// Check if the attacker is in DM and DM is started
			if (DMEvent.isStarted() && DMEvent.isPlayerParticipant(getObjectId()))
				return true;

			// Check if the attacker is in FOS and FOS is started
			if (FOSEvent.isStarted() && FOSEvent.isPlayerParticipant(getObjectId()))
				return true;

			// Check if the attacker is in LM and LM is started
			if (LMEvent.isStarted() && LMEvent.isPlayerParticipant(getObjectId()))
				return true;

			// Check if the attacker is in TvT and TvT is started
			if (TvTEvent.isStarted() && TvTEvent.isPlayerParticipant(getObjectId()))
				return true;

			// Check if the attacker is in TvT and TvT is started
			if (MultiTvTEvent.isStarted() && MultiTvTEvent.isPlayerParticipant(getObjectId()))
				return true;

			if (cha.isInArenaEvent())
			{
				L2PcInstance player = null;
				if (attacker instanceof L2PcInstance)
				{
					player = (L2PcInstance) attacker;
				}
				else if (attacker instanceof L2SummonInstance)
				{
					player = ((L2SummonInstance) attacker).getOwner();
				}
				if (player != null)
				{
					if (isArenaAttack() && player.isArenaAttack())
						return true;

					return false;
				}
			}
		}
		else if (attacker instanceof L2SiegeGuardInstance)
		{
			if (getClan() != null)
			{
				final Siege siege = SiegeManager.getSiege(this);
				return (siege != null && siege.checkIsAttacker(getClan()));
			}
		}

		// Check if the L2PcInstance has Karma
		if (getKarma() > 0 || getPvpFlag() > 0)
			return true;

		return false;
	}

	/**
	 * Check if the active L2Skill can be casted.
	 * <ul>
	 * <li>Check if the skill isn't toggle and is offensive</li>
	 * <li>Check if the target is in the skill cast range</li>
	 * <li>Check if the skill is Spoil type and if the target isn't already spoiled</li>
	 * <li>Check if the caster owns enought consummed Item, enough HP and MP to cast the skill</li>
	 * <li>Check if the caster isn't sitting</li>
	 * <li>Check if all skills are enabled and this skill is enabled</li>
	 * <li>Check if the caster own the weapon needed</li>
	 * <li>Check if the skill is active</li>
	 * <li>Check if all casting conditions are completed</li>
	 * <li>Notify the AI with CAST and target</li>
	 * </ul>
	 * @param skill The L2Skill to use
	 * @param forceUse used to force ATTACK on players
	 * @param dontMove used to prevent movement, if not in range
	 */
	@Override
	public boolean useMagic(L2Skill skill, boolean forceUse, boolean dontMove)
	{
		// Check if the skill is active
		if (skill.isPassive())
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}

		if (isAio() && !isGM() && !isInsideZone(ZoneId.TOWN))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}

		// Cancels the use of skills when player uses a cursed weapon or is flying.
		if ((isCursedWeaponEquipped() && !skill.isDemonicSkill()) // If CW, allow ONLY demonic skills.
				|| (getMountType() == 1 && !skill.isStriderSkill()) // If mounted, allow ONLY Strider skills.
				|| (getMountType() == 2 && !skill.isFlyingSkill())) // If flying, allow ONLY Wyvern skills.
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}

		// Players wearing Formal Wear cannot use skills.
		final ItemInstance formal = getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST);
		if (formal != null && formal.getItem().getBodyPart() == Item.SLOT_ALLDRESS)
		{
			sendPacket(SystemMessageId.CANNOT_USE_ITEMS_SKILLS_WITH_FORMALWEAR);
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}

		// ************************************* Check Casting in Progress *******************************************

		// If a skill is currently being used, queue this one if this is not the same
		if (isCastingNow())
		{
			// Check if new skill different from current skill in progress ; queue it in the player _queuedSkill
			if (_currentSkill.getSkill() != null && skill.getId() != _currentSkill.getSkillId())
				setQueuedSkill(skill, forceUse, dontMove);

			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}

		setIsCastingNow(true);

		// Set the player _currentSkill.
		setCurrentSkill(skill, forceUse, dontMove);

		// Wipe queued skill.
		if (_queuedSkill.getSkill() != null)
			setQueuedSkill(null, false, false);

		if (!checkUseMagicConditions(skill, forceUse, dontMove))
		{
			setIsCastingNow(false);
			return false;
		}

		// Check if the target is correct and Notify the AI with CAST and target
		L2Object target = null;

		switch (skill.getTargetType())
		{
		case TARGET_AURA:
		case TARGET_FRONT_AURA:
		case TARGET_BEHIND_AURA:
		case TARGET_GROUND:
		case TARGET_SELF:
		case TARGET_CORPSE_ALLY:
		case TARGET_AURA_UNDEAD:
			target = this;
			break;

		default: // Get the first target of the list
			target = skill.getFirstOfTargetList(this);
			break;
		}

		// Notify the AI with CAST and target
		getAI().setIntention(CtrlIntention.CAST, skill, target);
		return true;
	}

	private boolean checkUseMagicConditions(L2Skill skill, boolean forceUse, boolean dontMove)
	{
		// ************************************* Check Player State *******************************************

		// Check if the player is dead or out of control.
		if (isDead() || isOutOfControl())
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}

		L2SkillType sklType = skill.getSkillType();

		if (isFishing() && (sklType != L2SkillType.PUMPING && sklType != L2SkillType.REELING && sklType != L2SkillType.FISHING))
		{
			// Only fishing skills are available
			sendPacket(SystemMessageId.ONLY_FISHING_SKILLS_NOW);
			return false;
		}

		if (inObserverMode())
		{
			sendPacket(SystemMessageId.OBSERVERS_CANNOT_PARTICIPATE);
			abortCast();
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}

		// Check if the caster is sitted. Toggle skills can be only removed, not activated.
		if (isSitting())
		{
			if (skill.isToggle())
			{
				// Get effects of the skill
				L2Effect effect = getFirstEffect(skill.getId());
				if (effect != null)
				{
					effect.exit();

					// Send ActionFailed to the L2PcInstance
					sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
			}

			// Send a System Message to the caster
			sendPacket(SystemMessageId.CANT_MOVE_SITTING);

			// Send ActionFailed to the L2PcInstance
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}

		// Check if the skill type is TOGGLE
		if (skill.isToggle())
		{
			// Get effects of the skill
			L2Effect effect = getFirstEffect(skill.getId());

			if (effect != null)
			{
				// If the toggle is different of FakeDeath, you can de-activate it clicking on it.
				if (skill.getId() != 60)
					effect.exit();

				// Send ActionFailed to the L2PcInstance
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
		}

		// Check if the player uses "Fake Death" skill
		if (isFakeDeath())
		{
			// Send ActionFailed to the L2PcInstance
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}

		// ************************************* Check Target *******************************************
		// Create and set a L2Object containing the target of the skill
		L2Object target = null;
		SkillTargetType sklTargetType = skill.getTargetType();
		Location worldPosition = getCurrentSkillWorldPosition();

		if (sklTargetType == SkillTargetType.TARGET_GROUND && worldPosition == null)
		{
			_log.info("WorldPosition is null for skill: " + skill.getName() + ", player: " + getName() + ".");
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}

		switch (sklTargetType)
		{
		// Target the player if skill type is AURA, PARTY, CLAN or SELF
		case TARGET_AURA:
		case TARGET_FRONT_AURA:
		case TARGET_BEHIND_AURA:
		case TARGET_AURA_UNDEAD:
		case TARGET_PARTY:
		case TARGET_ALLY:
		case TARGET_CLAN:
		case TARGET_GROUND:
		case TARGET_SELF:
		case TARGET_CORPSE_ALLY:
		case AURA_CORPSE_MOB:
		case TARGET_AREA_SUMMON:
			target = this;
			break;
		case TARGET_PET:
		case TARGET_SUMMON:
			target = getPet();
			break;
		default:
			target = getTarget();
			break;
		}

		// Check the validity of the target
		if (target == null)
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}

		if (target instanceof L2DoorInstance)
		{
			// Siege doors only hittable during siege
			if (!((L2DoorInstance) target).isAutoAttackable(this) || (((L2DoorInstance) target).isUnlockable() && skill.getSkillType() != L2SkillType.UNLOCK)) // unlockable doors
			{
				sendPacket(SystemMessageId.INCORRECT_TARGET);
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
		}

		// Are the target and the player in the same duel?
		if (isInDuel())
		{
			if (target instanceof L2Playable)
			{
				// Get L2PcInstance
				L2PcInstance cha = target.getActingPlayer();
				if (cha.getDuelId() != getDuelId())
				{
					sendPacket(SystemMessageId.INCORRECT_TARGET);
					sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
			}
		}

		// ************************************* Check skill availability *******************************************

		// Siege summon checks. Both checks send a message to the player if it return false.
		if (skill.isSiegeSummonSkill() && (!SiegeManager.checkIfOkToSummon(this) || !SevenSigns.getInstance().checkSummonConditions(this)))
			return false;

		// Check if this skill is enabled (ex : reuse time)
		if (isSkillDisabled(skill))
		{
			if (Config.OLY_SKILL_LIST.contains(skill.getId()) && isSkillProtectOlympiadMode())
				sendMessage("Olympiad: This skill can only be used when the battle starts..");
			else if (Config.ALT_SHOW_REUSE_MSG)
				sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_PREPARED_FOR_REUSE).addSkillName(skill));
			return false;
		}

		// ************************************* Check casting conditions *******************************************

		// Check if all casting conditions are completed
		if (!skill.checkCondition(this, target, false))
		{
			// Send ActionFailed to the L2PcInstance
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}

		// ************************************* Check Skill Type *******************************************

		// Check if this is offensive magic skill
		if (skill.isOffensive())
		{
			if (isInsidePeaceZone(this, target))
			{
				// If L2Character or target is in a peace zone, send a system message TARGET_IN_PEACEZONE ActionFailed
				sendPacket(SystemMessageId.TARGET_IN_PEACEZONE);
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}

			if (isInOlympiadMode() && !isOlympiadStart())
			{
				// if L2PcInstance is in Olympia and the match isn't already start, send ActionFailed
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}

			// Check if the target is attackable
			if (!target.isAttackable() && !getAccessLevel().allowPeaceAttack() && !(target instanceof L2DoorInstance))
			{
				// If target is not attackable, send ActionFailed
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}

			// Check if a Forced ATTACK is in progress on non-attackable target
			if (!target.isAutoAttackable(this) && !forceUse)
			{
				switch (sklTargetType)
				{
				case TARGET_AURA:
				case TARGET_FRONT_AURA:
				case TARGET_BEHIND_AURA:
				case TARGET_AURA_UNDEAD:
				case TARGET_CLAN:
				case TARGET_ALLY:
				case TARGET_PARTY:
				case TARGET_SELF:
				case TARGET_GROUND:
				case TARGET_CORPSE_ALLY:
				case TARGET_AREA_SUMMON:
					break;
				default: // Send ActionFailed to the L2PcInstance
					sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
			}

			// Check if the target is in the skill cast range
			if (dontMove)
			{
				// Calculate the distance between the L2PcInstance and the target
				if (sklTargetType == SkillTargetType.TARGET_GROUND)
				{
					if (!isInsideRadius(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), skill.getCastRange() + getTemplate().getCollisionRadius(), false, false))
					{
						// Send a System Message to the caster
						sendPacket(SystemMessageId.TARGET_TOO_FAR);

						// Send ActionFailed to the L2PcInstance
						sendPacket(ActionFailed.STATIC_PACKET);
						return false;
					}
				}
				else if (skill.getCastRange() > 0 && !isInsideRadius(target, skill.getCastRange() + getTemplate().getCollisionRadius(), false, false))
				{
					// Send a System Message to the caster
					sendPacket(SystemMessageId.TARGET_TOO_FAR);

					// Send ActionFailed to the L2PcInstance
					sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
			}
		}

		// Check if the skill is defensive
		if (!skill.isOffensive() && target instanceof L2MonsterInstance && !forceUse)
		{
			// check if the target is a monster and if force attack is set.. if not then we don't want to cast.
			switch (sklTargetType)
			{
			case TARGET_PET:
			case TARGET_SUMMON:
			case TARGET_AURA:
			case TARGET_FRONT_AURA:
			case TARGET_BEHIND_AURA:
			case TARGET_AURA_UNDEAD:
			case TARGET_CLAN:
			case TARGET_SELF:
			case TARGET_CORPSE_ALLY:
			case TARGET_PARTY:
			case TARGET_ALLY:
			case TARGET_CORPSE_MOB:
			case TARGET_AREA_CORPSE_MOB:
			case TARGET_GROUND:
			case AURA_CORPSE_MOB:
				break;
			default:
			{
				switch (sklType)
				{
				case BEAST_FEED:
				case DELUXE_KEY_UNLOCK:
				case UNLOCK:
					break;
				default:
					sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
				break;
			}
			}
		}

		// Check if the skill is Spoil type and if the target isn't already spoiled
		if (sklType == L2SkillType.SPOIL)
		{
			if (!(target instanceof L2MonsterInstance))
			{
				// Send a System Message to the L2PcInstance
				sendPacket(SystemMessageId.INCORRECT_TARGET);

				// Send ActionFailed to the L2PcInstance
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
		}

		// Check if the skill is Sweep type and if conditions not apply
		if (sklType == L2SkillType.SWEEP && target instanceof L2Attackable)
		{
			if (((L2Attackable) target).isDead())
			{
				final int spoilerId = ((L2Attackable) target).getIsSpoiledBy();
				if (spoilerId == 0)
				{
					// Send a System Message to the L2PcInstance
					sendPacket(SystemMessageId.SWEEPER_FAILED_TARGET_NOT_SPOILED);

					// Send ActionFailed to the L2PcInstance
					sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}

				if (getObjectId() != spoilerId && !isInLooterParty(spoilerId))
				{
					// Send a System Message to the L2PcInstance
					sendPacket(SystemMessageId.SWEEP_NOT_ALLOWED);

					// Send ActionFailed to the L2PcInstance
					sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
			}
		}

		// Check if the skill is Drain Soul (Soul Crystals) and if the target is a MOB
		if (sklType == L2SkillType.DRAIN_SOUL)
		{
			if (!(target instanceof L2MonsterInstance))
			{
				// Send a System Message to the L2PcInstance
				sendPacket(SystemMessageId.INCORRECT_TARGET);

				// Send ActionFailed to the L2PcInstance
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
		}

		// Check if this is a Pvp skill and target isn't a non-flagged/non-karma player
		switch (sklTargetType)
		{
		case TARGET_PARTY:
		case TARGET_ALLY: // For such skills, checkPvpSkill() is called from L2Skill.getTargetList()
		case TARGET_CLAN: // For such skills, checkPvpSkill() is called from L2Skill.getTargetList()
		case TARGET_AURA:
		case TARGET_FRONT_AURA:
		case TARGET_BEHIND_AURA:
		case TARGET_AURA_UNDEAD:
		case TARGET_GROUND:
		case TARGET_SELF:
		case TARGET_CORPSE_ALLY:
			break;
		default:
			if (!checkPvpSkill(target, skill) && !getAccessLevel().allowPeaceAttack())
			{
				// Send a System Message to the L2PcInstance
				sendPacket(SystemMessageId.TARGET_IS_INCORRECT);

				// Send ActionFailed to the L2PcInstance
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
		}

		if ((sklTargetType == SkillTargetType.TARGET_HOLY && !checkIfOkToCastSealOfRule(CastleManager.getInstance().getCastle(this), false, skill, target)) || (sklType == L2SkillType.SIEGEFLAG && !L2SkillSiegeFlag.checkIfOkToPlaceFlag(this, false)) || (sklType == L2SkillType.STRSIEGEASSAULT && !checkIfOkToUseStriderSiegeAssault(skill)) || (sklType == L2SkillType.SUMMON_FRIEND && !(checkSummonerStatus(this) && checkSummonTargetStatus(target, this))))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			abortCast();
			return false;
		}

		// GeoData Los Check here
		if (skill.getCastRange() > 0)
		{
			if (sklTargetType == SkillTargetType.TARGET_GROUND)
			{
				if (!PathFinding.getInstance().canSeeTarget(this, worldPosition))
				{
					sendPacket(SystemMessageId.CANT_SEE_TARGET);
					sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
			}
			else if (!PathFinding.getInstance().canSeeTarget(this, target))
			{
				sendPacket(SystemMessageId.CANT_SEE_TARGET);
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
		}
		// finally, after passing all conditions
		return true;
	}

	public boolean checkIfOkToUseStriderSiegeAssault(L2Skill skill)
	{
		SystemMessage sm;
		Castle castle = CastleManager.getInstance().getCastle(this);

		if (!isRiding())
			sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED).addSkillName(skill);
		else if (!(getTarget() instanceof L2DoorInstance))
			sm = SystemMessage.getSystemMessage(SystemMessageId.INCORRECT_TARGET);
		else if (castle == null || castle.getCastleId() <= 0)
			sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED).addSkillName(skill);
		else if (!castle.getSiege().isInProgress() || castle.getSiege().getAttackerClan(getClan()) == null)
			sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED).addSkillName(skill);
		else
			return true;

		sendPacket(sm);
		return false;
	}

	public boolean checkIfOkToCastSealOfRule(Castle castle, boolean isCheckOnly, L2Skill skill, L2Object target)
	{
		SystemMessage sm;

		if (FOSEvent.isStarted() && isInFOSEvent() /*&& isInsideRadius(FortressSiege.eventCenterX, FortressSiege.eventCenterY, 10000, false)*/)
			return true;

		if (castle == null || castle.getCastleId() <= 0)
			sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED).addSkillName(skill);
		else if (!castle.getArtefacts().contains(target))
			sm = SystemMessage.getSystemMessage(SystemMessageId.INCORRECT_TARGET);
		else if (!castle.getSiege().isInProgress())
			sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED).addSkillName(skill);
		else if (!Util.checkIfInRange(200, this, target, true))
			sm = SystemMessage.getSystemMessage(SystemMessageId.DIST_TOO_FAR_CASTING_STOPPED);
		else if (!isInsideZone(ZoneId.CAST_ON_ARTIFACT))
			sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED).addSkillName(skill);
		else if (castle.getSiege().getAttackerClan(getClan()) == null)
			sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED).addSkillName(skill);
		else
		{
			if (!isCheckOnly)
			{
				sm = SystemMessage.getSystemMessage(SystemMessageId.OPPONENT_STARTED_ENGRAVING);
				castle.getSiege().announceToPlayer(sm, false);
			}
			return true;
		}
		sendPacket(sm);
		return false;
	}

	public boolean isInLooterParty(int LooterId)
	{
		L2PcInstance looter = L2World.getInstance().getPlayer(LooterId);

		// if L2PcInstance is in a CommandChannel
		if (isInParty() && getParty().isInCommandChannel() && looter != null)
			return getParty().getCommandChannel().getMembers().contains(looter);

		if (isInParty() && looter != null)
			return getParty().getPartyMembers().contains(looter);

		return false;
	}

	/**
	 * Check if the requested casting is a Pc->Pc skill cast and if it's a valid pvp condition
	 * @param target L2Object instance containing the target
	 * @param skill L2Skill instance with the skill being casted
	 * @return {@code false} if the skill is a pvpSkill and target is not a valid pvp target, {@code true} otherwise.
	 */
	public boolean checkPvpSkill(L2Object target, L2Skill skill)
	{
		if (target instanceof L2Character
			    && PlayerModeManager.blocksSkill(this, (L2Character) target, skill))
			{
			    return false;
			}
		
		if (skill == null || target == null)
			return false;

		if (!(target instanceof L2Playable))
			return true;

		if (skill.isDebuff() || skill.isOffensive())
		{
			final L2PcInstance targetPlayer = target.getActingPlayer();
			if (targetPlayer == null || this == target)
				return false;

			// Peace Zone
			if (target.isInsideZone(ZoneId.PEACE))
				return false;

			// Auto PvP zone is free for all, including clan, ally and party members.
			if (isInsideZone(ZoneId.AUTO_PVP_ZONE) && targetPlayer.isInsideZone(ZoneId.AUTO_PVP_ZONE))
				return true;

			// Duel
			if (isInDuel() && targetPlayer.isInDuel() && getDuelId() == targetPlayer.getDuelId())
				return true;

			final boolean isCtrlPressed = getCurrentSkill() != null && getCurrentSkill().isCtrlPressed();

			// Party
			if (isInParty() && targetPlayer.isInParty())
			{
				// Same Party
				if (getParty().getLeader() == targetPlayer.getParty().getLeader())
				{
					if (skill.getEffectRange() > 0 && isCtrlPressed && getTarget() == target && skill.isDamage())
						return true;

					return false;
				}
				else if (getParty().getCommandChannel() != null && getParty().getCommandChannel().containsPlayer(targetPlayer))
				{
					if (skill.getEffectRange() > 0 && isCtrlPressed && getTarget() == target && skill.isDamage())
						return true;

					return false;
				}
			}

			// You can debuff anyone except party members while in an arena...
			if (isInsideZone(ZoneId.PVP) && targetPlayer.isInsideZone(ZoneId.PVP) || isInsideZone(ZoneId.ARENA_EVENT) && targetPlayer.isInsideZone(ZoneId.ARENA_EVENT))
				return true;

			// You can debuff anyone except party members while in an flag zone...
			if (isInsideZone(ZoneId.FLAG_AREA) && targetPlayer.isInsideZone(ZoneId.FLAG_AREA))
				return true;

			// You can debuff anyone except party members while in an flag zone...
			if (isInsideZone(ZoneId.FLAG_AREA_SELF) && targetPlayer.isInsideZone(ZoneId.FLAG_AREA_SELF))
				return true;

			// Olympiad
			if (isInOlympiadMode() && targetPlayer.isInOlympiadMode() && getOlympiadGameId() == targetPlayer.getOlympiadGameId())
				return true;

			final L2Clan aClan = getClan();
			final L2Clan tClan = targetPlayer.getClan();

			if (aClan != null && tClan != null)
			{
				if (aClan.isAtWarWith(tClan.getClanId()) && tClan.isAtWarWith(aClan.getClanId()))
				{
					return true; 
					// in clan war player can attack whites even without ctrl
					// Check if skill can do dmg
					// if (skill.getEffectRange() > 0  && isCtrlPressed && getTarget() == target && skill.isAOE())
					// 	return true;

					// return isCtrlPressed;
				}
				else if (getClanId() == targetPlayer.getClanId() || (getAllyId() > 0 && getAllyId() == targetPlayer.getAllyId()))
				{
					// Check if skill can do dmg
					if (skill.getEffectRange() > 0 && isCtrlPressed && getTarget() == target && skill.isDamage())
						return true;

					return false;
				}
			}

			// On retail, it is impossible to debuff a "peaceful" player.
			if (targetPlayer.getPvpFlag() == 0 && targetPlayer.getKarma() == 0)
			{
				// Check if skill can do dmg
				if (skill.getEffectRange() > 0 && isCtrlPressed && getTarget() == target && skill.isDamage())
					return true;

				return false;
			}

			if (targetPlayer.getPvpFlag() > 0 || targetPlayer.getKarma() > 0)
				return true;

			return false;
		}
		return true;
	}

	/**
	 * @return True if the L2PcInstance is a Mage (based on class templates).
	 */
	public boolean isMageClass()
	{
		return getClassId().isMage();
	}

	public boolean isMounted()
	{
		return _mountType > 0;
	}

	/**
	 * This method allows to :
	 * <ul>
	 * <li>change isRiding/isFlying flags</li>
	 * <li>gift player with Wyvern Breath skill if mount is a wyvern</li>
	 * <li>send the skillList (faded icons update)</li>
	 * </ul>
	 * @param npcId the npcId of the mount
	 * @param npcLevel The level of the mount
	 * @param mountType 0, 1 or 2 (dismount, strider or wyvern).
	 * @return always true.
	 */
	public boolean setMount(int npcId, int npcLevel, int mountType)
	{
		_customMountVisualIndex = -1;
		switch (mountType)
		{
		case 0: // Dismounted
			if (isFlying())
				removeSkill(FrequentSkill.WYVERN_BREATH.getSkill());
			break;

		case 2: // Flying Wyvern
			addSkill(FrequentSkill.WYVERN_BREATH.getSkill(), false); // not saved to DB
			break;
		}
		_customMountVisualIndex = -1;
		_mountNpcId = npcId;
		_mountType = mountType;
		_mountLevel = npcLevel;

		sendSkillList(); // Update faded icons && eventual added skills.
		return true;
	}

	@Override
	public boolean isRiding()
	{
		return _mountType == 1;
	}

	@Override
	public boolean isFlying()
	{
		return _mountType == 2;
	}

	/**
	 * @return the type of Pet mounted (0 : none, 1 : Strider, 2 : Wyvern).
	 */
	public int getMountType()
	{
		return _mountType;
	}

	@Override
	public final void stopAllEffects()
	{
		super.stopAllEffects();
		updateAndBroadcastStatus(2);
	}

	@Override
	public final void stopAllEffectsExceptThoseThatLastThroughDeath()
	{
		super.stopAllEffectsExceptThoseThatLastThroughDeath();
		updateAndBroadcastStatus(2);
	}

	/**
	 * Stop all toggle-type effects
	 */
	public final void stopAllToggles()
	{
		_effects.stopAllToggles();
	}

	public final void stopCubics()
	{
		if (getCubics() != null)
		{
			boolean removed = false;
			for (L2CubicInstance cubic : getCubics().values())
			{
				cubic.stopAction();
				delCubic(cubic.getId());
				removed = true;
			}
			if (removed)
				broadcastUserInfo();
		}
	}

	public final void stopCubicsByOthers()
	{
		if (getCubics() != null)
		{
			boolean removed = false;
			for (L2CubicInstance cubic : getCubics().values())
			{
				if (cubic.givenByOther())
				{
					cubic.stopAction();
					delCubic(cubic.getId());
					removed = true;
				}
			}
			if (removed)
				broadcastUserInfo();
		}
	}

	/**
	 * Send UserInfo to this L2PcInstance and CharInfo to all L2PcInstance in its _KnownPlayers.<BR>
	 * <ul>
	 * <li>Send UserInfo to this L2PcInstance (Public and Private Data)</li>
	 * <li>Send CharInfo to all L2PcInstance in _KnownPlayers of the L2PcInstance (Public data only)</li>
	 * </ul>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : DON'T SEND UserInfo packet to other players instead of CharInfo packet. Indeed, UserInfo packet contains PRIVATE DATA as MaxHP, STR, DEX...</B></FONT><BR>
	 * <BR>
	 */
	@Override
	public void updateAbnormalEffect()
	{
		broadcastUserInfo();
	}

	/**
	 * Disable the Inventory and create a new task to enable it after 1.5s.
	 */
	public void tempInventoryDisable()
	{
		_inventoryDisable = true;

		ThreadPoolManager.getInstance().scheduleGeneral(new InventoryEnable(), 1500);
	}

	/**
	 * @return True if the Inventory is disabled.
	 */
	public boolean isInventoryDisabled()
	{
		return _inventoryDisable;
	}

	protected class InventoryEnable implements Runnable
	{
		@Override
		public void run()
		{
			_inventoryDisable = false;
		}
	}

	public Map<Integer, L2CubicInstance> getCubics()
	{
		return _cubics;
	}

	/**
	 * Add a L2CubicInstance to the L2PcInstance _cubics.
	 * @param id
	 * @param level
	 * @param matk
	 * @param activationtime
	 * @param activationchance
	 * @param totalLifetime
	 * @param givenByOther
	 */
	public void addCubic(int id, int level, double matk, int activationtime, int activationchance, int totalLifetime, boolean givenByOther)
	{
		_cubics.put(id, new L2CubicInstance(this, id, level, (int) matk, activationtime, activationchance, totalLifetime, givenByOther));
	}

	/**
	 * Remove a L2CubicInstance from the L2PcInstance _cubics.
	 * @param id
	 */
	public void delCubic(int id)
	{
		_cubics.remove(id);
	}

	/**
	 * @param id
	 * @return the L2CubicInstance corresponding to the Identifier of the L2PcInstance _cubics.
	 */
	public L2CubicInstance getCubic(int id)
	{
		return _cubics.get(id);
	}

	@Override
	public String toString()
	{
		return "player " + getName();
	}

	/**
	 * @return the modifier corresponding to the Enchant Effect of the Active Weapon (Min : 127).
	 */
	public int getEnchantEffect()
	{
		ItemInstance wpn = getActiveWeaponInstance();

		if (wpn == null)
			return 0;

		return Math.min(127, wpn.getEnchantLevel());
	}

	/**
	 * Set the _currentFolkNpc of the player.
	 * @param npc
	 */
	public void setCurrentFolkNPC(L2Npc npc)
	{
		_currentFolkNpc = npc;
	}

	/**
	 * @return the _currentFolkNpc of the player.
	 */
	public L2Npc getCurrentFolkNPC()
	{
		return _currentFolkNpc;
	}

	/**
	 * @return True if L2PcInstance is a participant in the Festival of Darkness.
	 */
	public boolean isFestivalParticipant()
	{
		return SevenSignsFestival.getInstance().isParticipant(this);
	}

	public void addAutoSoulShot(int itemId)
	{
		_activeSoulShots.add(itemId);
	}

	public boolean removeAutoSoulShot(int itemId)
	{
		return _activeSoulShots.remove(itemId);
	}

	public Set<Integer> getAutoSoulShot()
	{
		return _activeSoulShots;
	}

	@Override
	public boolean isChargedShot(ShotType type)
	{
		ItemInstance weapon = getActiveWeaponInstance();
		return weapon != null && weapon.isChargedShot(type);
	}

	@Override
	public void setChargedShot(ShotType type, boolean charged)
	{
		ItemInstance weapon = getActiveWeaponInstance();
		if (weapon != null)
			weapon.setChargedShot(type, charged);
	}


@Override
public void rechargeShots(boolean physical, boolean magic)
{
	if (_activeSoulShots == null || _activeSoulShots.isEmpty())
	{
		return;
	}

	for (int itemId : _activeSoulShots)
	{
		ItemInstance item = getInventory().getItemByItemId(itemId);

		if (item == null)
		{
			removeAutoSoulShot(itemId);
			continue;
		}

		ActionType action = item.getItem().getDefaultAction();

		if ((magic && action == ActionType.spiritshot)
			|| (physical && action == ActionType.soulshot))
		{
			IItemHandler handler =
				ItemHandler.getInstance().getItemHandler(item.getEtcItem());

			if (handler == null)
			{
				continue;
			}
			handler.useItem(this, item, false);
		}
	}
}


	/**
	 * Cancel autoshot use for shot itemId
	 * @param itemId int id to disable
	 * @return true if canceled.
	 */
	public boolean disableAutoShot(int itemId)
	{
		if (_activeSoulShots.contains(itemId))
		{
			removeAutoSoulShot(itemId);
			sendPacket(new ExAutoSoulShot(itemId, 0));
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.AUTO_USE_OF_S1_CANCELLED).addItemName(itemId));
			return true;
		}

		return false;
	}

	/**
	 * Cancel all autoshots for player
	 */
	public void disableAutoShotsAll()
	{
		for (int itemId : _activeSoulShots)
		{
			sendPacket(new ExAutoSoulShot(itemId, 0));
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.AUTO_USE_OF_S1_CANCELLED).addItemName(itemId));
		}
		_activeSoulShots.clear();
	}

	class LookingForFishTask implements Runnable
	{
		boolean _isNoob, _isUpperGrade;
		int _fishType, _fishGutsCheck;
		long _endTaskTime;

		protected LookingForFishTask(int fishWaitTime, int fishGutsCheck, int fishType, boolean isNoob, boolean isUpperGrade)
		{
			_fishGutsCheck = fishGutsCheck;
			_endTaskTime = System.currentTimeMillis() + fishWaitTime + 10000;
			_fishType = fishType;
			_isNoob = isNoob;
			_isUpperGrade = isUpperGrade;
		}

		@Override
		public void run()
		{
			if (System.currentTimeMillis() >= _endTaskTime)
			{
				endFishing(false);
				return;
			}

			if (_fishType == -1)
				return;

			int check = Rnd.get(1000);
			if (_fishGutsCheck > check)
			{
				stopLookingForFishTask();
				startFishCombat(_isNoob, _isUpperGrade);
			}
		}
	}

	public int getClanPrivileges()
	{
		return _clanPrivileges;
	}

	public void setClanPrivileges(int n)
	{
		_clanPrivileges = n;
	}

	// baron etc
	public void setPledgeClass(int classId)
	{
		_pledgeClass = classId;
	}

	public int getPledgeClass()
	{
		return _pledgeClass;
	}

	public void setPledgeType(int typeId)
	{
		_pledgeType = typeId;
	}

	public int getPledgeType()
	{
		return _pledgeType;
	}

	public int getApprentice()
	{
		return _apprentice;
	}

	public void setApprentice(int apprentice_id)
	{
		_apprentice = apprentice_id;
	}

	public int getSponsor()
	{
		return _sponsor;
	}

	public void setSponsor(int sponsor_id)
	{
		_sponsor = sponsor_id;
	}

	@Override
	public void sendMessage(String message)
	{
		sendPacket(SystemMessage.sendString(message));
	}

	public void sendHideWinnerMessage(String message)
	{
		sendPacket(SystemMessage.sendHideCustomString(message));
	}

	/**
	 * Unsummon all types of summons : pets, cubics, normal summons and trained beasts.
	 */
	public void dropAllSummons()
	{
		// Delete summons and pets
		if (getPet() != null)
			getPet().unSummon(this);

		// Delete trained beasts
		if (getTrainedBeast() != null)
			getTrainedBeast().deleteMe();

		// Delete any form of cubics
		stopCubics();
	}

	public void enterObserverMode(int x, int y, int z)
	{
		_lastX = getX();
		_lastY = getY();
		_lastZ = getZ();

		_observerMode = true;

		if (getAgathionId() > 0)
			unSummonAgathion();

		standUp();

		dropAllSummons();
		setTarget(null);
		setIsParalyzed(true);
		startParalyze();
		setIsInvul(true);
		getAppearance().setInvisible();

		sendPacket(new ObservationMode(x, y, z));
		getKnownList().removeAllKnownObjects(); // reinit knownlist
		setXYZ(x, y, z);

		broadcastUserInfo();
	}

	public void setLastCords(int x, int y, int z)
	{
		_lastX = getX();
		_lastY = getY();
		_lastZ = getZ();
	}

	public void enterOlympiadObserverMode(int id)
	{
		final OlympiadGameTask task = OlympiadGameManager.getInstance().getOlympiadTask(id);
		if (task == null)
			return;

		dropAllSummons();

		if (getParty() != null)
			getParty().removePartyMember(this, MessageType.Expelled);

		_olympiadGameId = id;

		standUp();

		if (!_observerMode)
		{
			_lastX = getX();
			_lastY = getY();
			_lastZ = getZ();
		}

		if (getAgathionId() > 0)
			unSummonAgathion();

		_observerMode = true;
		setTarget(null);
		setIsInvul(true);
		getAppearance().setInvisible();
		teleToLocation(task.getZone().getSpawns().get(2), 0);
		sendPacket(new ExOlympiadMode(3));
		broadcastUserInfo();
	}

	public void leaveObserverMode()
	{
		/*
		if (hasAI())
			getAI().setIntention(CtrlIntention.IDLE);

		_observerMode = false;

		setTarget(null);
		getKnownList().removeAllKnownObjects(); // reinit knownlist
		setXYZ(_lastX, _lastY, _lastZ);
		setIsParalyzed(false);
		stopParalyze(false);
		getAppearance().setVisible();
		setIsInvul(false);

		// prevent receive falling damage
		setFalling();

		setLastCords(0, 0, 0);
		sendPacket(new ObservationReturn(this));
		broadcastUserInfo();
		 */
		setTarget(null);
		teleToLocation(_lastX, _lastY, _lastZ, 0);
		setLastCords(0, 0, 0);
		sendPacket(new ObservationReturn(this));
		setIsParalyzed(false);
		getAppearance().setVisible();
		setIsInvul(false);

		if (hasAI())
			getAI().setIntention(CtrlIntention.IDLE);

		if (getAgathionId() > 0)
			unSummonAgathion();

		setFalling();
		_observerMode = false;
		broadcastCharInfo();
	}

	public void leaveOlympiadObserverMode()
	{
		if (_olympiadGameId == -1)
			return;

		_olympiadGameId = -1;
		_observerMode = false;

		setTarget(null);
		sendPacket(new ExOlympiadMode(0));
		teleToLocation(_lastX, _lastY, _lastZ, 20);
		getAppearance().setVisible();
		setIsInvul(false);

		if (hasAI())
			getAI().setIntention(CtrlIntention.IDLE);

		if (getAgathionId() > 0)
			unSummonAgathion();

		setLastCords(0, 0, 0);
		broadcastUserInfo();
	}

	public void setOlympiadSide(int i)
	{
		_olympiadSide = i;
	}

	public int getOlympiadSide()
	{
		return _olympiadSide;
	}

	public void setOlympiadGameId(int id)
	{
		_olympiadGameId = id;
	}

	public int getOlympiadGameId()
	{
		return _olympiadGameId;
	}

	public int getLastX()
	{
		return _lastX;
	}

	public int getLastY()
	{
		return _lastY;
	}

	public int getLastZ()
	{
		return _lastZ;
	}

	public boolean inObserverMode()
	{
		return _observerMode;
	}

	public boolean isInObserverMode()
	{
		return !isInOlympiadMode();
	}

	public int getTeleMode()
	{
		return _telemode;
	}

	public void setTeleMode(int mode)
	{
		_telemode = mode;
	}

	public void setLoto(int i, int val)
	{
		_loto[i] = val;
	}

	public int getLoto(int i)
	{
		return _loto[i];
	}

	public void setRace(int i, int val)
	{
		_race[i] = val;
	}

	public int getRace(int i)
	{
		return _race[i];
	}

	public void setStopExp(boolean mode)
	{
		_stopexp = mode;
	}

	public boolean getStopExp()
	{
		return _stopexp;
	}

	public boolean isPartyInRefuse()
	{
		return _isPartyInRefuse;
	}

	public void setIsPartyInRefuse(boolean value)
	{
		_isPartyInRefuse = value;
	}

	public boolean isInRefusalMode()
	{
		return _messageRefusal;
	}

	public void setInRefusalMode(boolean mode)
	{
		_messageRefusal = mode;
		sendPacket(new EtcStatusUpdate(this));
	}

	public void setTradeRefusal(boolean mode)
	{
		_tradeRefusal = mode;
	}

	public boolean getTradeRefusal()
	{
		return _tradeRefusal;
	}

	public void setExchangeRefusal(boolean mode)
	{
		_exchangeRefusal = mode;
	}

	public boolean getExchangeRefusal()
	{
		return _exchangeRefusal;
	}

	public BlockList getBlockList()
	{
		return _blockList;
	}

	public void setHero(boolean hero)
	{
		if (hero && _baseClass == _activeClass)
		{
			for (L2Skill s : SkillTable.getHeroSkills())
				addSkill(s, false); 
		}
		else if (hero && Config.ALLOW_HERO_SUBSKILL)
		{
			for (L2Skill s : SkillTable.getHeroSkills())
				addSkill(s, false); 
		}
		else 
		{
			for (L2Skill s : SkillTable.getHeroSkills())
				super.removeSkill(s); 
		} 
		_hero = hero;
		sendSkillList();
	}

	public void setIsInOlympiadMode(boolean value)
	{
	    _inOlympiadMode = value;

	    PlayerModeManager.getInstance().enforceContext(this);
	}

	public void setIsOlympiadStart(boolean b)
	{
		_OlympiadStart = b;
	}

	public boolean isOlympiadStart()
	{
		return _OlympiadStart;
	}

	public boolean isHero()
	{
		return _hero;
	}

	public void setHeroAura(final boolean heroAura)
	{
		isPVPHero = heroAura;
		return;
	}

	public boolean getIsPVPHero()
	{
		return isPVPHero;
	}

	public void reloadPVPHeroAura()
	{
		sendPacket(new UserInfo(this));
	}

	public boolean isInOlympiadMode()
	{
		return _inOlympiadMode;
	}

	public boolean isInDuel()
	{
		return _isInDuel;
	}

	public int getDuelId()
	{
		return _duelId;
	}

	public void setDuelState(int mode)
	{
		_duelState = mode;
	}

	public int getDuelState()
	{
		return _duelState;
	}

	/**
	 * Sets up the duel state using a non 0 duelId.
	 * @param duelId 0=not in a duel
	 */
	public void setIsInDuel(int duelId)
	{
	    if (duelId > 0)
	    {
	        _isInDuel = true;
	        _duelState = Duel.DUELSTATE_DUELLING;
	        _duelId = duelId;
	    }
	    else
	    {
	        if (_duelState == Duel.DUELSTATE_DEAD)
	        {
	            enableAllSkills();
	            getStatus().startHpMpRegeneration();
	        }

	        _isInDuel = false;
	        _duelState = Duel.DUELSTATE_NODUEL;
	        _duelId = 0;
	    }

	    PlayerModeManager.getInstance().enforceContext(this);
	}
	/**
	 * This returns a SystemMessage stating why the player is not available for duelling.
	 * @return S1_CANNOT_DUEL... message
	 */
	public SystemMessage getNoDuelReason()
	{
		SystemMessage sm = SystemMessage.getSystemMessage(_noDuelReason);
		sm.addPcName(this);
		_noDuelReason = SystemMessageId.THERE_IS_NO_OPPONENT_TO_RECEIVE_YOUR_CHALLENGE_FOR_A_DUEL;
		return sm;
	}

	/**
	 * Checks if this player might join / start a duel. To get the reason use getNoDuelReason() after calling this function.
	 * @return true if the player might join/start a duel.
	 */
	public boolean canDuel()
	{
		if (isInCombat() || getPunishLevel() == PunishLevel.JAIL)
			_noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_CURRENTLY_ENGAGED_IN_BATTLE;
		else if (isDead() || isAlikeDead() || (getCurrentHp() < getMaxHp() / 2 || getCurrentMp() < getMaxMp() / 2))
			_noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_HP_OR_MP_IS_BELOW_50_PERCENT;
		else if (isInDuel())
			_noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_ALREADY_ENGAGED_IN_A_DUEL;
		else if (isInOlympiadMode())
			_noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_PARTICIPATING_IN_THE_OLYMPIAD;
		else if (isCursedWeaponEquipped())
			_noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_IN_A_CHAOTIC_STATE;
		else if (isInStoreMode())
			_noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_CURRENTLY_ENGAGED_IN_A_PRIVATE_STORE_OR_MANUFACTURE;
		else if (isMounted() || isInBoat())
			_noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_CURRENTLY_RIDING_A_BOAT_WYVERN_OR_STRIDER;
		else if (isFishing())
			_noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_CURRENTLY_FISHING;
		else if (isInsideZone(ZoneId.PVP) || isInsideZone(ZoneId.PEACE) || isInsideZone(ZoneId.SIEGE))
			_noDuelReason = SystemMessageId.S1_CANNOT_MAKE_A_CHALLANGE_TO_A_DUEL_BECAUSE_S1_IS_CURRENTLY_IN_A_DUEL_PROHIBITED_AREA;
		else
			return true;

		return false;
	}

	public boolean isNoble()
	{
		return _noble;
	}

	/**
	 * Set Noblesse Status, and reward with nobles' skills.
	 * @param val Add skills if setted to true, else remove skills.
	 * @param store Store the status directly in the db if setted to true.
	 */
	public void setNoble(boolean val, boolean store)
	{
		if (val)
			for (L2Skill s : SkillTable.getNobleSkills())
				addSkill(s, false); // Dont Save Noble skills to Sql
		else
			for (L2Skill s : SkillTable.getNobleSkills())
				super.removeSkill(s); // Just Remove skills without deleting from Sql

		_noble = val;

		sendSkillList();

		if (store)
		{
			try (Connection con = L2DatabaseFactory.getInstance().getConnection())
			{
				PreparedStatement statement = con.prepareStatement(UPDATE_NOBLESS);
				statement.setBoolean(1, val);
				statement.setInt(2, getObjectId());
				statement.executeUpdate();
				statement.close();
			}
			catch (Exception e)
			{
				_log.log(Level.WARNING, "Could not update " + getName() + " nobless status: " + e.getMessage(), e);
			}
		}
	}

	public void setLvlJoinedAcademy(int lvl)
	{
		_lvlJoinedAcademy = lvl;
	}

	public int getLvlJoinedAcademy()
	{
		return _lvlJoinedAcademy;
	}

	public boolean isAcademyMember()
	{
		return _lvlJoinedAcademy > 0;
	}

	public void setTeam(int team)
	{
		_team = team;
	}

	public int getTeam()
	{
		return _team;
	}

	public void setWantsPeace(boolean wantsPeace)
	{
		_wantsPeace = wantsPeace;
	}

	public boolean wantsPeace()
	{
		return _wantsPeace;
	}

	public boolean isFishing()
	{
		return _fishingLoc != null;
	}

	public void setAllianceWithVarkaKetra(int sideAndLvlOfAlliance)
	{
		_alliedVarkaKetra = sideAndLvlOfAlliance;
	}

	/**
	 * [-5,-1] varka, 0 neutral, [1,5] ketra
	 * @return the side faction.
	 */
	public int getAllianceWithVarkaKetra()
	{
		return _alliedVarkaKetra;
	}

	public boolean isAlliedWithVarka()
	{
		return (_alliedVarkaKetra < 0);
	}

	public boolean isAlliedWithKetra()
	{
		return (_alliedVarkaKetra > 0);
	}

	public void sendSkillList()
	{
		final ItemInstance formal = getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST);
		final boolean isWearingFormalWear = formal != null && formal.getItem().getBodyPart() == Item.SLOT_ALLDRESS;

		boolean isDisabled = false;
		SkillList sl = new SkillList();
		for (L2Skill s : getAllSkills())
		{
			if (s == null)
				continue;

			if (getClan() != null)
				isDisabled = s.isClanSkill() && getClan().getReputationScore() < 0;

			if (isCursedWeaponEquipped()) // Only Demonic skills are available
				isDisabled = !s.isDemonicSkill();
			else if (isMounted()) // else if, because only ONE state is possible
			{
				if (getMountType() == 1) // Only Strider skills are available
					isDisabled = !s.isStriderSkill();
				else if (getMountType() == 2) // Only Wyvern skills are available
					isDisabled = !s.isFlyingSkill();
			}

			if (isWearingFormalWear)
				isDisabled = true;

			if (isAio() && !isGM() && !isInsideZone(ZoneId.TOWN))
				isDisabled = true;

			sl.addSkill(s.getId(), s.getLevel(), s.isPassive(), isDisabled);
		}
		sendPacket(sl);
	}

	/**
	 * 1. Add the specified class ID as a subclass (up to the maximum number of <b>three</b>) for this character.<BR>
	 * 2. This method no longer changes the active _classIndex of the player. This is only done by the calling of setActiveClass() method as that should be the only way to do so.
	 * @param classId
	 * @param classIndex
	 * @return boolean subclassAdded
	 */
	public boolean addSubClass(int classId, int classIndex)
	{
		if (!_subclassLock.tryLock())
			return false;

		removeFakeArmor();
		removeFakeWeapon();

		try
		{
			if (getTotalSubClasses() == 6 || classIndex == 0)
				return false;

			if (getSubClasses().containsKey(classIndex))
				return false;

			// Note: Never change _classIndex in any method other than setActiveClass().
			SubClass newClass = new SubClass();
			newClass.setClassId(classId);
			newClass.setClassIndex(classIndex);

			try (Connection con = L2DatabaseFactory.getInstance().getConnection())
			{
				PreparedStatement statement = con.prepareStatement(ADD_CHAR_SUBCLASS);
				statement.setInt(1, getObjectId());
				statement.setInt(2, newClass.getClassId());
				statement.setLong(3, newClass.getExp());
				statement.setInt(4, newClass.getSp());
				statement.setInt(5, newClass.getLevel());
				statement.setInt(6, newClass.getClassIndex()); // <-- Added

				statement.execute();
				statement.close();
			}
			catch (Exception e)
			{
				_log.warning("WARNING: Could not add character sub class for " + getName() + ": " + e);
				return false;
			}

			// Commit after database INSERT incase exception is thrown.
			getSubClasses().put(newClass.getClassIndex(), newClass);

			ClassId subTemplate = ClassId.values()[classId];
			Collection<L2SkillLearn> skillTree = SkillTreeTable.getInstance().getAllowedSkills(subTemplate);

			if (skillTree == null)
				return true;

			final Map<Integer, L2Skill> prevSkillList = new LinkedHashMap<>();

			for (L2SkillLearn skillInfo : skillTree)
			{
				if (skillInfo.getMinLevel() <= 40)
				{
					L2Skill prevSkill = prevSkillList.get(skillInfo.getId());
					L2Skill newSkill = SkillTable.getInstance().getInfo(skillInfo.getId(), skillInfo.getLevel());

					if (prevSkill != null && (prevSkill.getLevel() > newSkill.getLevel()))
						continue;

					prevSkillList.put(newSkill.getId(), newSkill);
					storeSkill(newSkill, prevSkill, classIndex);
				}
			}

			return true;
		}
		finally
		{
			_subclassLock.unlock();
		}
	}

	/**
	 * 1. Completely erase all existance of the subClass linked to the classIndex.<BR>
	 * 2. Send over the newClassId to addSubClass()to create a new instance on this classIndex.<BR>
	 * 3. Upon Exception, revert the player to their BaseClass to avoid further problems.<BR>
	 * @param classIndex
	 * @param newClassId
	 * @return boolean subclassAdded
	 */
	public boolean modifySubClass(int classIndex, int newClassId)
	{
		if (!_subclassLock.tryLock())
			return false;

		try
		{
			try (Connection con = L2DatabaseFactory.getInstance().getConnection())
			{
				// Remove all henna info stored for this sub-class.
				PreparedStatement statement = con.prepareStatement(DELETE_CHAR_HENNAS);
				statement.setInt(1, getObjectId());
				statement.setInt(2, classIndex);
				statement.execute();
				statement.close();

				// Remove all shortcuts info stored for this sub-class.
				statement = con.prepareStatement(DELETE_CHAR_SHORTCUTS);
				statement.setInt(1, getObjectId());
				statement.setInt(2, classIndex);
				statement.execute();
				statement.close();

				// Remove all effects info stored for this sub-class.
				statement = con.prepareStatement(DELETE_SKILL_SAVE);
				statement.setInt(1, getObjectId());
				statement.setInt(2, classIndex);
				statement.execute();
				statement.close();

				// Remove all skill info stored for this sub-class.
				statement = con.prepareStatement(DELETE_CHAR_SKILLS);
				statement.setInt(1, getObjectId());
				statement.setInt(2, classIndex);
				statement.execute();
				statement.close();

				// Remove all basic info stored about this sub-class.
				statement = con.prepareStatement(DELETE_CHAR_SUBCLASS);
				statement.setInt(1, getObjectId());
				statement.setInt(2, classIndex);
				statement.execute();
				statement.close();
			}
			catch (Exception e)
			{
				_log.warning("Could not modify subclass for " + getName() + " to class index " + classIndex + ": " + e);

				// This must be done in order to maintain data consistency.
				getSubClasses().remove(classIndex);
				return false;
			}

			getSubClasses().remove(classIndex);
		}
		finally
		{
			_subclassLock.unlock();
		}

		return addSubClass(newClassId, classIndex);
	}

	public boolean isSubClassActive()
	{
		return _classIndex > 0;
	}

	public Map<Integer, SubClass> getSubClasses()
	{
		return _subClasses;
	}

	public int getTotalSubClasses()
	{
		return getSubClasses().size();
	}

	public int getBaseClass()
	{
		return _baseClass;
	}

	public int getActiveClass()
	{
		return _activeClass;
	}

	public int getClassIndex()
	{
		return _classIndex;
	}

	private void setClassTemplate(int classId)
	{
		_activeClass = classId;

		PcTemplate t = CharTemplateTable.getInstance().getTemplate(classId);

		if (t == null)
		{
			_log.severe("Missing template for classId: " + classId);
			throw new Error();
		}

		// Set the template of the L2PcInstance
		setTemplate(t);
	}

	/**
	 * Changes the character's class based on the given class index. <BR>
	 * <BR>
	 * An index of zero specifies the character's original (base) class, while indexes 1-3 specifies the character's sub-classes respectively.
	 * @param classIndex
	 * @return true if successful.
	 */
	public boolean setActiveClass(int classIndex)
	{
		if (!_subclassLock.tryLock())
			return false;

		try
		{
			// Remove active item skills before saving char to database because next time when choosing this class, worn items can be different
			for (ItemInstance item : getInventory().getAugmentedItems())
			{
				if (item != null && item.isEquipped())
					item.getAugmentation().removeBonus(this);
			}

			// abort any kind of cast.
			abortCast();

			// Stop casting for any player that may be casting a force buff on this l2pcinstance.
			for (L2Character character : getKnownList().getKnownType(L2Character.class))
				if (character.getFusionSkill() != null && character.getFusionSkill().getTarget() == this)
					character.abortCast();

			store();

			// clear charges
			_charges.set(0);
			stopChargeTask();

			if (classIndex == 0)
				setClassTemplate(getBaseClass());
			else
			{
				try
				{
					setClassTemplate(getSubClasses().get(classIndex).getClassId());
				}
				catch (Exception e)
				{
					_log.info("Could not switch " + getName() + "'s sub class to class index " + classIndex + ": " + e);
					return false;
				}
			}
			_classIndex = classIndex;

			if (isInParty())
				getParty().recalculatePartyLevel();

			if (getPet() instanceof L2SummonInstance)
				getPet().unSummon(this);

			if (hasAgathion())
				unSummonAgathion();

			if (isInParty() && getClassId() == ClassId.bishop || isInParty() && getClassId() == ClassId.cardinal)
			{
				final L2Party party = getParty();
				if (party != null)
					party.removePartyMember(this, MessageType.Expelled);
			}

			for (L2Skill oldSkill : getAllSkills())
				super.removeSkill(oldSkill);

			stopAllEffectsExceptThoseThatLastThroughDeath();
			stopCubics();

			if (isSubClassActive())
			{
				_dwarvenRecipeBook.clear();
				_commonRecipeBook.clear();
			}
			else
				restoreRecipeBook();

			restoreSkills();
			rewardSkills();
			regiveTemporarySkills();

			if (Config.ALT_RESTORE_EFFECTS_ON_SUBCLASS_CHANGE)
				restoreEffects();

			for (ItemInstance itemInstance : getInventory().getItems())
			{
				if (itemInstance == null)
					continue; 
				for (Map.Entry<Integer, Integer> itemSkill : Config.LIST_RUNE_ITEMS.entrySet())
				{
					if (itemInstance.getItemId() == itemSkill.getKey().intValue())
					{
						L2Skill skill = SkillTable.getInstance().getInfo(itemSkill.getValue().intValue(), 1);
						if (skill != null)
						{
							addSkill(skill, false);
							sendSkillList();
						} 
					} 
				} 
			} 

			// Reload skills from armors / jewels / weapons
			getInventory().reloadEquippedItems();

			// Prevents some issues when changing between subclases that shares skills
			if (_disabledSkills != null && !_disabledSkills.isEmpty())
				_disabledSkills.clear();

			checkAllowedSkills();

			updateEffectIcons();
			sendPacket(new EtcStatusUpdate(this));

			removeFakeArmor();

			// If player has quest "Repent Your Sins", remove it
			QuestState st = getQuestState("Q422_RepentYourSins");
			if (st != null)
				st.exitQuest(true);

			for (int i = 0; i < 3; i++)
				_henna[i] = null;

			restoreHenna();
			sendPacket(new HennaInfo(this));

			if (getCurrentHp() > getMaxHp())
				setCurrentHp(getMaxHp());
			if (getCurrentMp() > getMaxMp())
				setCurrentMp(getMaxMp());
			if (getCurrentCp() > getMaxCp())
				setCurrentCp(getMaxCp());

			refreshOverloaded();
			refreshExpertisePenalty();
			refreshArmorPenality();
			refreshWeaponPenality();
			broadcastUserInfo();

			// Clear resurrect xp calculation
			setExpBeforeDeath(0);

			_shortCuts.restore();
			sendPacket(new ShortCutInit(this));

			if (Config.SHOW_NPC_EFFECT_SUBCLASS)
			{
				broadcastPacket(new SocialAction(this, 15));
			}

			sendPacket(new SkillCoolTime(this));
			return true;
		}
		finally
		{
			_subclassLock.unlock();
		}
	}

	public boolean isLocked()
	{
		return _subclassLock.isLocked();
	}

	public void stopWaterTask()
	{
		if (_isInWater)
		{
			_isInWater = false;
			sendPacket(new SetupGauge(2, 0));
			WaterTaskManager.getInstance().remove(this);
		}
	}

	public void startWaterTask()
	{
		if (!isDead() && !_isInWater)
		{
			_isInWater = true;
			final int time = (int) calcStat(Stats.BREATH, 60000 * getRace().getBreathMultiplier(), this, null);

			sendPacket(new SetupGauge(2, time));
			WaterTaskManager.getInstance().add(this, System.currentTimeMillis() + time);
		}
	}

	public void checkWaterState()
	{
		if (isInsideZone(ZoneId.WATER))
			startWaterTask();
		else
			stopWaterTask();
	}

	public void onPlayerEnter()
	{
		if (isCursedWeaponEquipped())
			CursedWeaponsManager.getInstance().getCursedWeapon(getCursedWeaponEquippedId()).cursedOnLogin();

		// Add a task for the "Take Break" message.
		TakeBreakTaskManager.getInstance().add(this);

		// Teleport player if the Seven Signs period isn't the good one, or if the player isn't in a cabal.
		if (isIn7sDungeon() && !isGM())
		{
			if (SevenSigns.getInstance().isSealValidationPeriod() || SevenSigns.getInstance().isCompResultsPeriod())
			{
				if (SevenSigns.getInstance().getPlayerCabal(getObjectId()) != SevenSigns.getInstance().getCabalHighestScore())
				{
					teleToLocation(MapRegionTable.TeleportWhereType.Town);
					setIsIn7sDungeon(false);
				}
			}
			else if (SevenSigns.getInstance().getPlayerCabal(getObjectId()) == SevenSigns.CABAL_NULL)
			{
				teleToLocation(MapRegionTable.TeleportWhereType.Town);
				setIsIn7sDungeon(false);
			}
		}

		// Jail task
		updatePunishState();

		if (isGM())
		{
			if (isInvul())
				sendMessage("Entering world in Invulnerable mode.");
			if (getAppearance().getInvisible())
				sendMessage("Entering world in Invisible mode.");
			if (isInRefusalMode())
				sendMessage("Entering world in Message Refusal mode.");
		}

		revalidateZone(true);
		notifyFriends(true);

		// Fix against exploit on anti-target on login
		decayMe();
		spawnMe();
		broadcastUserInfo();
		// Sincroniza o flag inicial com a interface.
		sendOwnStatusFlag();
	}

	public long getLastAccess()
	{
		return _lastAccess;
	}

	private void checkRecom(int recsHave, int recsLeft)
	{
		Calendar check = Calendar.getInstance();
		check.setTimeInMillis(_lastRecomUpdate);
		check.add(Calendar.DAY_OF_MONTH, 1);

		Calendar min = Calendar.getInstance();

		_recomHave = recsHave;
		_recomLeft = recsLeft;

		if (getStat().getLevel() < 10 || check.after(min))
			return;

		restartRecom();
	}

	public void restartRecom()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement(DELETE_CHAR_RECOMS);
			statement.setInt(1, getObjectId());
			statement.execute();
			statement.close();

			_recomChars.clear();
		}
		catch (Exception e)
		{
			_log.warning("could not clear char recommendations: " + e);
		}

		if (getStat().getLevel() < 20)
		{
			_recomLeft = 3;
			_recomHave--;
		}
		else if (getStat().getLevel() < 40)
		{
			_recomLeft = 6;
			_recomHave -= 2;
		}
		else
		{
			_recomLeft = 9;
			_recomHave -= 3;
		}

		if (_recomHave < 0)
			_recomHave = 0;

		// If we have to update last update time, but it's now before 13, we should set it to yesterday
		Calendar update = Calendar.getInstance();
		if (update.get(Calendar.HOUR_OF_DAY) < 13)
			update.add(Calendar.DAY_OF_MONTH, -1);

		update.set(Calendar.HOUR_OF_DAY, 13);
		_lastRecomUpdate = update.getTimeInMillis();
	}

	@Override
	public void doRevive()
	{
		super.doRevive();
		stopEffects(L2EffectType.CHARMOFCOURAGE);
		sendPacket(new EtcStatusUpdate(this));
		_reviveRequested = 0;
		_revivePower = 0;

		if (isMounted())
			startFeed(_mountNpcId);

		if (isInParty() && getParty().isInDimensionalRift())
		{
			if (!DimensionalRiftManager.getInstance().checkIfInPeaceZone(getX(), getY(), getZ()))
				getParty().getDimensionalRift().memberRessurected(this);
		}
	}

	@Override
	public void doRevive(double revivePower)
	{
		// Restore the player's lost experience, depending on the % return of the skill used (based on its power).
		restoreExp(revivePower);
		doRevive();
	}

	public void reviveRequest(L2PcInstance Reviver, L2Skill skill, boolean Pet)
	{
		if (_reviveRequested == 1)
		{
			// Resurrection has already been proposed.
			if (_revivePet == Pet)
				Reviver.sendPacket(SystemMessageId.RES_HAS_ALREADY_BEEN_PROPOSED);
			else
			{
				if (Pet)
					// A pet cannot be resurrected while it's owner is in the process of resurrecting.
					Reviver.sendPacket(SystemMessageId.CANNOT_RES_PET2);
				else
					// While a pet is attempting to resurrect, it cannot help in resurrecting its master.
					Reviver.sendPacket(SystemMessageId.MASTER_CANNOT_RES);
			}
			return;
		}

		if ((Pet && getPet() != null && getPet().isDead()) || (!Pet && isDead()))
		{
			_reviveRequested = 1;

			if (isPhoenixBlessed())
				_revivePower = 100;
			else if (isAffected(L2EffectFlag.CHARM_OF_COURAGE))
				_revivePower = 0;
			else
				_revivePower = Formulas.calculateSkillResurrectRestorePercent(skill.getPower(), Reviver);

			_revivePet = Pet;

			if (isAffected(L2EffectFlag.CHARM_OF_COURAGE))
			{
				sendPacket(new ConfirmDlg(SystemMessageId.DO_YOU_WANT_TO_BE_RESTORED).addTime(60000));
				return;
			}

			sendPacket(new ConfirmDlg(SystemMessageId.RESSURECTION_REQUEST_BY_S1).addTime(30000).addPcName(Reviver));
		}
	}

	public void reviveAnswer(int answer)
	{
		if (_reviveRequested != 1 || (!isDead() && !_revivePet) || (_revivePet && getPet() != null && !getPet().isDead()))
			return;

		if (answer == 0 && isPhoenixBlessed())
			stopPhoenixBlessing(null);
		else if (answer == 1)
		{
			if (!_revivePet)
			{
				if (_revivePower != 0)
					doRevive(_revivePower);
				else
					doRevive();
			}
			else if (getPet() != null)
			{
				if (_revivePower != 0)
					getPet().doRevive(_revivePower);
				else
					getPet().doRevive();
			}
		}
		_reviveRequested = 0;
		_revivePower = 0;
	}

	public boolean isReviveRequested()
	{
		return (_reviveRequested == 1);
	}

	public boolean isRevivingPet()
	{
		return _revivePet;
	}

	public void removeReviving()
	{
		_reviveRequested = 0;
		_revivePower = 0;
	}

	public void onActionRequest()
	{
		if (isSpawnProtected())
		{
			sendMessage("As you acted, you are no longer under spawn protection.");
			setProtection(false);
		}
	}

	/**
	 * @param expertiseIndex The expertiseIndex to set.
	 */
	public void setExpertiseIndex(int expertiseIndex)
	{
		_expertiseIndex = expertiseIndex;
	}

	/**
	 * @return Returns the expertiseIndex.
	 */
	public int getExpertiseIndex()
	{
		return _expertiseIndex;
	}

	@Override
	public final void onTeleported()
	{
		_treasureHuntSession.finish();
		super.onTeleported();

		// Force a revalidation
		revalidateZone(true);

		if (Config.PLAYER_SPAWN_PROTECTION > 0)
			setProtection(true);

		// Modify the position of the tamed beast if necessary
		if (getTrainedBeast() != null)
		{
			getTrainedBeast().getAI().stopFollow();
			getTrainedBeast().teleToLocation(getPosition().getX(), getPosition().getY(), getPosition().getZ(), 0);
			getTrainedBeast().getAI().startFollow(this);
		}

		// Modify the position of the pet if necessary
		L2Summon pet = getPet();
		if (pet != null)
		{
			pet.setFollowStatus(false);
			pet.teleToLocation(getPosition().getX(), getPosition().getY(), getPosition().getZ(), 0);
			((L2SummonAI) pet.getAI()).setStartFollowController(true);
			pet.setFollowStatus(true);
		}

		if (getAgathionId() > 0 && !inObserverMode())
		{
			if (isOlympiadProtection())
				return;

			getAgathion().setFollowStatus(false);
			getAgathion().teleToLocation(getPosition().getX(), getPosition().getY(), getPosition().getZ(), 0);
			getAgathion().setRunning();
			getAgathion().setFollowStatus(true);
		}

		CTFEvent.onTeleported(this);
		DMEvent.onTeleported(this);
		FOSEvent.onTeleported(this);
		LMEvent.onTeleported(this);
		TvTEvent.onTeleported(this);
		KTBEvent.onTeleported(this);
		MultiTvTEvent.onTeleported(this);
	}

	public void setLastServerPosition(int x, int y, int z)
	{
		_lastServerPosition.setXYZ(x, y, z);
	}

	public boolean checkLastServerPosition(int x, int y, int z)
	{
		return _lastServerPosition.equals(x, y, z);
	}

	public int getLastServerDistance(int x, int y, int z)
	{
		double dx = (x - _lastServerPosition.getX());
		double dy = (y - _lastServerPosition.getY());
		double dz = (z - _lastServerPosition.getZ());

		return (int) Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	@Override
	public void addExpAndSp(long addToExp, int addToSp)
	{
		if (!getStopExp())
			getStat().addExpAndSp(addToExp, addToSp);
		else
			getStat().addExpAndSp(0, 0);
	}

	public void removeExpAndSp(long removeExp, int removeSp)
	{
		getStat().removeExpAndSp(removeExp, removeSp);
	}

	@Override
	public void reduceCurrentHp(double value, L2Character attacker, boolean awake, boolean isDOT, L2Skill skill)
	{
		
		if (value > 0 && PlayerModeManager.blocksPvp(attacker, this))
		{
		    return;
		}
		// ===============================
		// CERTIFICAÇÃO INÍCIO
		// ===============================
				if (Config.CERTIFICATION_ENABLED && value > 0)
				{
					try
					{
						if (attacker != null && attacker != this
							&& (attacker instanceof L2PcInstance || attacker instanceof L2Attackable))
						{
							// Cooldown anti-spam (3 segundos)
							if (canProcCertification())
							{
								final int idx = CertificationManager.getInstance().getCertificationIndex(this);

								if (idx >= 0 && idx < Config.CERTIFICATION_ACTIVE_SKILL_IDS.length)
								{
									if (Rnd.get(100) < Config.CERTIFICATION_DEFAULT_CHANCE)
									{
										final int buffId = Config.CERTIFICATION_ACTIVE_SKILL_IDS[idx];
										final L2Skill buff = SkillTable.getInstance().getInfo(buffId, 1);

										if (buff != null && getFirstEffect(buffId) == null)
										{
											setCertificationProc();
											buff.getEffects(this, this);

											if (Config.CERTIFICATION_NOTIFY_ON_ACTIVATE)
											{
												sendMessage("Sua certificação " + buff.getName() + " foi ativada!");

												sendPacket(new ExShowScreenMessage("Certificação " + buff.getName() + " ativada!",3000));
											}
										}
									}
								}
							}
						}
					}
					catch (Exception e)
					{
						_log.warning("Erro ao ativar certificação: " + e.getMessage());
					}
				}
				// ===============================
				// CERTIFICAÇÃO FIM
				// ===============================
		if (attacker != null)
		{
			L2PcInstance attackerPlayer = attacker.getActingPlayer();
			if (attackerPlayer != null && attackerPlayer != this)
				getAttackByList().add(attackerPlayer);
		}

		if (skill != null)
			getStatus().reduceHp(value, attacker, awake, isDOT, skill.isToggle(), skill.getDmgDirectlyToHP());
		else
			getStatus().reduceHp(value, attacker, awake, isDOT, false, false);

		// notify the tamed beast of attacks
		if (getTrainedBeast() != null)
			getTrainedBeast().onOwnerGotAttacked(attacker);
	}

	public synchronized void addBypass(String bypass)
	{
		if (bypass == null)
			return;

		_validBypass.add(bypass);
	}

	public synchronized void addBypass2(String bypass)
	{
		if (bypass == null)
			return;

		_validBypass2.add(bypass);
	}

	public synchronized boolean validateBypass(String cmd)
	{
		for (String bp : _validBypass)
		{
			if (bp == null)
				continue;

			if (bp.equals(cmd))
				return true;
		}

		for (String bp : _validBypass2)
		{
			if (bp == null)
				continue;

			if (cmd.startsWith(bp))
				return true;
		}
		return false;
	}

	public boolean validateItemManipulationByItemId(int itemId)
	{
		final ItemInstance item = getInventory().getItemByItemId(itemId);

		if (item == null || item.getOwnerId() != getObjectId())
		{
			_log.warning(getObjectId() + ": player tried to Modify TradeList item he is not owner of");
			return false;
		}

		if (getActiveEnchantItem() != null && getActiveEnchantItem().getItemId() == itemId)
			return false;

		if (CursedWeaponsManager.getInstance().isCursed(itemId))
			return false;

		return true;
	}

	/**
	 * Test multiple cases where the item shouldn't be able to manipulate.
	 * @param objectId : The item objectId.
	 * @return true if it the item can be manipulated, false ovtherwise.
	 */
	public boolean validateItemManipulation(int objectId)
	{
		if (_treasureHuntSession.ownsTemporary(objectId))
			return false;
		final ItemInstance item = getInventory().getItemByObjectId(objectId);

		// You don't own the item, or item is null.
		if (item == null || item.getOwnerId() != getObjectId())
		{
			_log.warning(getObjectId() + ": player tried to Modify TradeList item he is not owner of");
			return false;
		}

		// Pet whom item you try to manipulate is summoned/mounted.
		if (getPet() != null && getPet().getControlItemId() == objectId || getMountObjectID() == objectId)
			return false;

		if (getActiveEnchantItem() != null && getActiveEnchantItem().getObjectId() == objectId)
			return false;

		// Can't trade a cursed weapon.
		if (CursedWeaponsManager.getInstance().isCursed(item.getItemId()))
			return false;

		return true;
	}

	public synchronized void clearBypass()
	{
		_validBypass.clear();
		_validBypass2.clear();
	}

	/**
	 * @return Returns the inBoat.
	 */
	public boolean isInBoat()
	{
		return _vehicle != null && _vehicle.isBoat();
	}

	public L2BoatInstance getBoat()
	{
		return (L2BoatInstance) _vehicle;
	}

	public L2Vehicle getVehicle()
	{
		return _vehicle;
	}

	public void setVehicle(L2Vehicle v)
	{
		if (v == null && _vehicle != null)
			_vehicle.removePassenger(this);

		_vehicle = v;
	}

	public void setInCrystallize(boolean inCrystallize)
	{
		_inCrystallize = inCrystallize;
	}

	public boolean isInCrystallize()
	{
		return _inCrystallize;
	}

	public Location getInVehiclePosition()
	{
		return _inVehiclePosition;
	}

	public void setInVehiclePosition(Location pt)
	{
		_inVehiclePosition = pt;
	}

	/**
	 * Manage the delete task of a L2PcInstance (Leave Party, Unsummon pet, Save its inventory in the database, Remove it from the world...).
	 * <ul>
	 * <li>If the L2PcInstance is in observer mode, set its position to its position before entering in observer mode</li>
	 * <li>Set the online Flag to True or False and update the characters table of the database with online status and lastAccess</li>
	 * <li>Stop the HP/MP/CP Regeneration task</li>
	 * <li>Cancel Crafting, Attak or Cast</li>
	 * <li>Remove the L2PcInstance from the world</li>
	 * <li>Stop Party and Unsummon Pet</li>
	 * <li>Update database with items in its inventory and remove them from the world</li>
	 * <li>Remove all L2Object from _knownObjects and _knownPlayer of the L2Character then cancel Attak or Cast and notify AI</li>
	 * <li>Close the connection with the client</li>
	 * </ul>
	 */
	@Override
	public void deleteMe()
	{
		_treasureHuntSession.finish();
	    PlayerModeManager.getInstance().reset(this, false, false);

	    AutofarmManager.INSTANCE.onPlayerLogout(this);
	    AutofarmSecondaryManager.INSTANCE.onPlayerLogout(this);
	    SpoilGatekeeperInstance.onPlayerLogout(this);
	    
	    cleanup();
	    store();

	    super.deleteMe();
	}

	@SuppressWarnings("incomplete-switch")
	private synchronized void cleanup()
	{
		try
		{
			// Put the online status to false
			setOnlineStatus(false, true);

			// abort cast & attack and remove the target. Cancels movement aswell.
			abortAttack();
			abortCast();
			stopMove(null);
			setTarget(null);

			PartyMatchWaitingList.getInstance().removePlayer(this);
			if (_partyroom != 0)
			{
				PartyMatchRoom room = PartyMatchRoomList.getInstance().getRoom(_partyroom);
				if (room != null)
					room.deleteMember(this);
			}

			if (isFlying())
				removeSkill(SkillTable.getInstance().getInfo(4289, 1));

			// Stop all scheduled tasks
			stopAllTimers();

			// Cancel the cast of eventual fusion skill users on this target.
			for (L2Character character : getKnownList().getKnownType(L2Character.class))
				if (character.getFusionSkill() != null && character.getFusionSkill().getTarget() == this)
					character.abortCast();

			// Stop signets & toggles effects.
			for (L2Effect effect : getAllEffects())
			{
				if (effect.getSkill().isToggle())
				{
					effect.exit();
					continue;
				}

				switch (effect.getEffectType())
				{
				case SIGNET_GROUND:
				case SIGNET_EFFECT:
					effect.exit();
					break;
				}
			}

			// Remove the L2PcInstance from the world
			decayMe();

			// Remove from world regions zones
			L2WorldRegion oldRegion = getWorldRegion();
			if (oldRegion != null)
				oldRegion.removeFromZones(this);

			// If a party is in progress, leave it
			if (isInParty())
				leaveParty();

			// If the L2PcInstance has Pet, unsummon it
			if (getPet() != null)
				getPet().unSummon(this);

			// Handle removal from olympiad game
			if (OlympiadManager.getInstance().isRegistered(this) || getOlympiadGameId() != -1)
				OlympiadManager.getInstance().removeDisconnectedCompetitor(this);

			// set the status for pledge member list to OFFLINE
			if (getClan() != null)
			{
				L2ClanMember clanMember = getClan().getClanMember(getObjectId());
				if (clanMember != null)
					clanMember.setPlayerInstance(null);
			}

			if (hasAgathion())
				unSummonAgathion();

			// deals with sudden exit in the middle of transaction
			if (getActiveRequester() != null)
			{
				setActiveRequester(null);
				cancelActiveTrade();
			}

			// If the L2PcInstance is a GM, remove it from the GM List
			if (isGM())
				GmListTable.getInstance().deleteGm(this);

			// Check if the L2PcInstance is in observer mode to set its position to its position
			// before entering in observer mode
			if (inObserverMode())
				setXYZInvisible(_lastX, _lastY, _lastZ);

			// Oust player from boat
			if (getVehicle() != null)
				getVehicle().oustPlayer(this);

			CTFEvent.onLogout(this);
			DMEvent.onLogout(this);
			FOSEvent.onLogout(this);
			LMEvent.onLogout(this);
			TvTEvent.onLogout(this);
			KTBEvent.onLogout(this);
			MultiTvTEvent.onLogout(this);

			// Update inventory and remove them from the world
			getInventory().deleteMe();

			// Update warehouse and remove them from the world
			clearWarehouse();

			// Update freight and remove them from the world
			clearFreight();
			clearDepositedFreight();

			if (isCursedWeaponEquipped())
				CursedWeaponsManager.getInstance().getCursedWeapon(_cursedWeaponEquippedId).setPlayer(null);

			// Remove all L2Object from _knownObjects and _knownPlayer of the L2Character then cancel Attak or Cast and notify AI
			getKnownList().removeAllKnownObjects();

			if (getClanId() > 0)
				getClan().broadcastToOtherOnlineMembers(new PledgeShowMemberListUpdate(this), this);

			// Remove L2Object object from _allObjects of L2World
			L2World.getInstance().removeObject(this);
			L2World.getInstance().removeFromAllPlayers(this); // force remove in case of crash during teleport

			// friends & blocklist update
			notifyFriends(false);
			getBlockList().playerLogout();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Exception on deleteMe()" + e.getMessage(), e);
		}
	}

	public void startFishing(Location loc)
	{
		stopMove(null);
		setIsImmobilized(true);

		_fishingLoc = loc;

		// Starts fishing
		int group = getRandomGroup();

		_fish = FishTable.getFish(getRandomFishLvl(), getRandomFishType(group), group);
		if (_fish == null)
		{
			endFishing(false);
			return;
		}

		sendPacket(SystemMessageId.CAST_LINE_AND_START_FISHING);

		broadcastPacket(new ExFishingStart(this, _fish.getType(_lure.isNightLure()), loc, _lure.isNightLure()));
		sendPacket(new PlaySound(1, "SF_P_01", 0, 0, 0, 0, 0));
		startLookingForFishTask();
	}

	public void stopLookingForFishTask()
	{
		if (_taskforfish != null)
		{
			_taskforfish.cancel(false);
			_taskforfish = null;
		}
	}

	public void startLookingForFishTask()
	{
		if (!isDead() && _taskforfish == null)
		{
			int checkDelay = 0;
			boolean isNoob = false;
			boolean isUpperGrade = false;

			if (_lure != null)
			{
				int lureid = _lure.getItemId();
				isNoob = _fish.getGroup() == 0;
				isUpperGrade = _fish.getGroup() == 2;
				if (lureid == 6519 || lureid == 6522 || lureid == 6525 || lureid == 8505 || lureid == 8508 || lureid == 8511) // low grade
					checkDelay = Math.round((float) (_fish.getGutsCheckTime() * (1.33)));
				else if (lureid == 6520 || lureid == 6523 || lureid == 6526 || (lureid >= 8505 && lureid <= 8513) || (lureid >= 7610 && lureid <= 7613) || (lureid >= 7807 && lureid <= 7809) || (lureid >= 8484 && lureid <= 8486)) // medium grade, beginner, prize-winning & quest special bait
					checkDelay = Math.round((float) (_fish.getGutsCheckTime() * (1.00)));
				else if (lureid == 6521 || lureid == 6524 || lureid == 6527 || lureid == 8507 || lureid == 8510 || lureid == 8513) // high grade
					checkDelay = Math.round((float) (_fish.getGutsCheckTime() * (0.66)));
			}
			_taskforfish = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new LookingForFishTask(_fish.getWaitTime(), _fish.getFishGuts(), _fish.getType(_lure.isNightLure()), isNoob, isUpperGrade), 10000, checkDelay);
		}
	}

	private int getRandomGroup()
	{
		switch (_lure.getItemId())
		{
		case 7807: // green for beginners
		case 7808: // purple for beginners
		case 7809: // yellow for beginners
		case 8486: // prize-winning for beginners
			return 0;

		case 8485: // prize-winning luminous
		case 8506: // green luminous
		case 8509: // purple luminous
		case 8512: // yellow luminous
			return 2;

		default:
			return 1;
		}
	}

	private int getRandomFishType(int group)
	{
		int check = Rnd.get(100);
		int type = 1;
		switch (group)
		{
		case 0: // fish for novices
			switch (_lure.getItemId())
			{
			case 7807: // green lure, preferred by fast-moving (nimble) fish (type 5)
				if (check <= 54)
					type = 5;
				else if (check <= 77)
					type = 4;
				else
					type = 6;
				break;

			case 7808: // purple lure, preferred by fat fish (type 4)
				if (check <= 54)
					type = 4;
				else if (check <= 77)
					type = 6;
				else
					type = 5;
				break;

			case 7809: // yellow lure, preferred by ugly fish (type 6)
				if (check <= 54)
					type = 6;
				else if (check <= 77)
					type = 5;
				else
					type = 4;
				break;

			case 8486: // prize-winning fishing lure for beginners
				if (check <= 33)
					type = 4;
				else if (check <= 66)
					type = 5;
				else
					type = 6;
				break;
			}
			break;

		case 1: // normal fish
			switch (_lure.getItemId())
			{
			case 7610:
			case 7611:
			case 7612:
			case 7613:
				type = 3;
				break;

			case 6519: // all theese lures (green) are prefered by fast-moving (nimble) fish (type 1)
			case 8505:
			case 6520:
			case 6521:
			case 8507:
				if (check <= 54)
					type = 1;
				else if (check <= 74)
					type = 0;
				else if (check <= 94)
					type = 2;
				else
					type = 3;
				break;

			case 6522: // all theese lures (purple) are prefered by fat fish (type 0)
			case 8508:
			case 6523:
			case 6524:
			case 8510:
				if (check <= 54)
					type = 0;
				else if (check <= 74)
					type = 1;
				else if (check <= 94)
					type = 2;
				else
					type = 3;
				break;

			case 6525: // all theese lures (yellow) are prefered by ugly fish (type 2)
			case 8511:
			case 6526:
			case 6527:
			case 8513:
				if (check <= 55)
					type = 2;
				else if (check <= 74)
					type = 1;
				else if (check <= 94)
					type = 0;
				else
					type = 3;
				break;
			case 8484: // prize-winning fishing lure
				if (check <= 33)
					type = 0;
				else if (check <= 66)
					type = 1;
				else
					type = 2;
				break;
			}
			break;

		case 2: // upper grade fish, luminous lure
			switch (_lure.getItemId())
			{
			case 8506: // green lure, preferred by fast-moving (nimble) fish (type 8)
				if (check <= 54)
					type = 8;
				else if (check <= 77)
					type = 7;
				else
					type = 9;
				break;

			case 8509: // purple lure, preferred by fat fish (type 7)
				if (check <= 54)
					type = 7;
				else if (check <= 77)
					type = 9;
				else
					type = 8;
				break;

			case 8512: // yellow lure, preferred by ugly fish (type 9)
				if (check <= 54)
					type = 9;
				else if (check <= 77)
					type = 8;
				else
					type = 7;
				break;

			case 8485: // prize-winning fishing lure
				if (check <= 33)
					type = 7;
				else if (check <= 66)
					type = 8;
				else
					type = 9;
				break;
			}
		}
		return type;
	}

	private int getRandomFishLvl()
	{
		int skilllvl = getSkillLevel(1315);

		final L2Effect e = getFirstEffect(2274);
		if (e != null)
			skilllvl = (int) e.getSkill().getPower();

		if (skilllvl <= 0)
			return 1;

		int randomlvl;

		final int check = Rnd.get(100);
		if (check <= 50)
			randomlvl = skilllvl;
		else if (check <= 85)
		{
			randomlvl = skilllvl - 1;
			if (randomlvl <= 0)
				randomlvl = 1;
		}
		else
		{
			randomlvl = skilllvl + 1;
			if (randomlvl > 27)
				randomlvl = 27;
		}
		return randomlvl;
	}

	public void startFishCombat(boolean isNoob, boolean isUpperGrade)
	{
		_fishCombat = new L2Fishing(this, _fish, isNoob, isUpperGrade, _lure.getItemId());
	}

	public void endFishing(boolean win)
	{
		if (_fishCombat == null)
			sendPacket(SystemMessageId.BAIT_LOST_FISH_GOT_AWAY);
		else
			_fishCombat = null;

		_lure = null;
		_fishingLoc = null;

		// Ends fishing
		broadcastPacket(new ExFishingEnd(win, this));
		sendPacket(SystemMessageId.REEL_LINE_AND_STOP_FISHING);
		setIsImmobilized(false);
		stopLookingForFishTask();
	}

	public L2Fishing getFishCombat()
	{
		return _fishCombat;
	}

	public Location getFishingLoc()
	{
		return _fishingLoc;
	}

	public void setLure(ItemInstance lure)
	{
		_lure = lure;
	}

	public ItemInstance getLure()
	{
		return _lure;
	}

	public int getInventoryLimit()
	{
		return ((getRace() == Race.Dwarf) ? Config.INVENTORY_MAXIMUM_DWARF : Config.INVENTORY_MAXIMUM_NO_DWARF) + (int) getStat().calcStat(Stats.INV_LIM, 0, null, null);
	}

	public static int getQuestInventoryLimit()
	{
		return Config.INVENTORY_MAXIMUM_QUEST_ITEMS;
	}

	public int getWareHouseLimit()
	{
		return ((getRace() == Race.Dwarf) ? Config.WAREHOUSE_SLOTS_DWARF : Config.WAREHOUSE_SLOTS_NO_DWARF) + (int) getStat().calcStat(Stats.WH_LIM, 0, null, null);
	}

	public int getPrivateSellStoreLimit()
	{
		return ((getRace() == Race.Dwarf) ? Config.MAX_PVTSTORE_SLOTS_DWARF : Config.MAX_PVTSTORE_SLOTS_OTHER) + (int) getStat().calcStat(Stats.P_SELL_LIM, 0, null, null);
	}

	public int getPrivateBuyStoreLimit()
	{
		return ((getRace() == Race.Dwarf) ? Config.MAX_PVTSTORE_SLOTS_DWARF : Config.MAX_PVTSTORE_SLOTS_OTHER) + (int) getStat().calcStat(Stats.P_BUY_LIM, 0, null, null);
	}

	public int getFreightLimit()
	{
		return Config.FREIGHT_SLOTS + (int) getStat().calcStat(Stats.FREIGHT_LIM, 0, null, null);
	}

	public int getDwarfRecipeLimit()
	{
		return Config.DWARF_RECIPE_LIMIT + (int) getStat().calcStat(Stats.REC_D_LIM, 0, null, null);
	}

	public int getCommonRecipeLimit()
	{
		return Config.COMMON_RECIPE_LIMIT + (int) getStat().calcStat(Stats.REC_C_LIM, 0, null, null);
	}

	public int getMountNpcId()
	{
		return _mountNpcId;
	}

	public int getMountLevel()
	{
		return _mountLevel;
	}

	public void setMountObjectID(int newID)
	{
		_mountObjectID = newID;
	}

	public int getMountObjectID()
	{
		return _mountObjectID;
	}

	/**
	 * @return the current player skill in use.
	 */
	public SkillUseHolder getCurrentSkill()
	{
		return _currentSkill;
	}

	/**
	 * Update the _currentSkill holder.
	 * @param skill : The skill to update for (or null)
	 * @param ctrlPressed : The boolean information regarding ctrl key.
	 * @param shiftPressed : The boolean information regarding shift key.
	 */
	public void setCurrentSkill(L2Skill skill, boolean ctrlPressed, boolean shiftPressed)
	{
		_currentSkill.setSkill(skill);
		_currentSkill.setCtrlPressed(ctrlPressed);
		_currentSkill.setShiftPressed(shiftPressed);
	}

	/**
	 * @return the current pet skill in use.
	 */
	public SkillUseHolder getCurrentPetSkill()
	{
		return _currentPetSkill;
	}

	/**
	 * Update the _currentPetSkill holder.
	 * @param skill : The skill to update for (or null)
	 * @param ctrlPressed : The boolean information regarding ctrl key.
	 * @param shiftPressed : The boolean information regarding shift key.
	 */
	public void setCurrentPetSkill(L2Skill skill, boolean ctrlPressed, boolean shiftPressed)
	{
		_currentPetSkill.setSkill(skill);
		_currentPetSkill.setCtrlPressed(ctrlPressed);
		_currentPetSkill.setShiftPressed(shiftPressed);
	}

	/**
	 * @return the current queued skill in use.
	 */
	public SkillUseHolder getQueuedSkill()
	{
		return _queuedSkill;
	}

	/**
	 * Update the _queuedSkill holder.
	 * @param skill : The skill to update for (or null)
	 * @param ctrlPressed : The boolean information regarding ctrl key.
	 * @param shiftPressed : The boolean information regarding shift key.
	 */
	public void setQueuedSkill(L2Skill skill, boolean ctrlPressed, boolean shiftPressed)
	{
		_queuedSkill.setSkill(skill);
		_queuedSkill.setCtrlPressed(ctrlPressed);
		_queuedSkill.setShiftPressed(shiftPressed);
	}

	/**
	 * @return the timer to delay animation tasks, based on run speed.
	 */
	public int getAnimationTimer()
	{
		return Math.max(1000, 5000 - getRunSpeed() * 20);
	}

	/**
	 * @return punishment level of player
	 */
	public PunishLevel getPunishLevel()
	{
		return _punishLevel;
	}

	/**
	 * @return True if player is jailed
	 */
	public boolean isInJail()
	{
		return _punishLevel == PunishLevel.JAIL;
	}

	/**
	 * @return True if player is chat banned
	 */
	public boolean isChatBanned()
	{
		return _punishLevel == PunishLevel.CHAT;
	}

	public void setPunishLevel(int state)
	{
		switch (state)
		{
		case 0:
			_punishLevel = PunishLevel.NONE;
			break;
		case 1:
			_punishLevel = PunishLevel.CHAT;
			break;
		case 2:
			_punishLevel = PunishLevel.JAIL;
			break;
		case 3:
			_punishLevel = PunishLevel.CHAR;
			break;
		case 4:
			_punishLevel = PunishLevel.ACC;
			break;
		}
	}

	/**
	 * Sets punish level for player based on delay
	 * @param state
	 * @param delayInMinutes -- 0 for infinite
	 */
	@SuppressWarnings("incomplete-switch")
	public void setPunishLevel(PunishLevel state, int delayInMinutes)
	{
		long delayInMilliseconds = delayInMinutes * 60000L;
		switch (state)
		{
		case NONE: // Remove Punishments
		{
			switch (_punishLevel)
			{
			case CHAT:
			{
				_punishLevel = state;
				stopPunishTask(true);
				sendPacket(new EtcStatusUpdate(this));
				sendMessage("Chatting is now available.");
				sendPacket(new PlaySound("systemmsg_e.345"));
				break;
			}
			case JAIL:
			{
				_punishLevel = state;

				if (!CTFEvent.isInactive() && CTFEvent.isPlayerParticipant(getObjectId()))
					CTFEvent.removeParticipant(getObjectId());

				if (!DMEvent.isInactive() && DMEvent.isPlayerParticipant(getObjectId()))
					DMEvent.removeParticipant(this);

				if (!FOSEvent.isInactive() && FOSEvent.isPlayerParticipant(getObjectId()))
					FOSEvent.removeParticipant(getObjectId());

				if (!LMEvent.isInactive() && LMEvent.isPlayerParticipant(getObjectId()))
					LMEvent.removeParticipant(this);

				if (!TvTEvent.isInactive() && TvTEvent.isPlayerParticipant(getObjectId()))
					TvTEvent.removeParticipant(getObjectId());

				if (!KTBEvent.isInactive() && KTBEvent.isPlayerParticipant(getObjectId()))
					KTBEvent.removeParticipant(this);

				if (!MultiTvTEvent.isInactive() && MultiTvTEvent.isPlayerParticipant(getObjectId()))
					MultiTvTEvent.removeParticipant(getObjectId());

				// Open a Html message to inform the player
				NpcHtmlMessage htmlMsg = new NpcHtmlMessage(0);
				htmlMsg.setFile("data/html/jail_out.htm");
				sendPacket(htmlMsg);
				stopPunishTask(true);
				teleToLocation(17836, 170178, -3507, 20); // Floran village
				break;
			}
			}
			break;
		}
		case CHAT: // Chat ban
		{
			// not allow player to escape jail using chat ban
			if (_punishLevel == PunishLevel.JAIL)
				break;

			_punishLevel = state;
			_punishTimer = 0;
			sendPacket(new EtcStatusUpdate(this));

			// Remove the task if any
			stopPunishTask(false);

			if (delayInMinutes > 0)
			{
				_punishTimer = delayInMilliseconds;

				// start the countdown
				_punishTask = ThreadPoolManager.getInstance().scheduleGeneral(new PunishTask(), _punishTimer);
				sendMessage("Chatting has been suspended for " + delayInMinutes + " minute(s).");
			}
			else
				sendMessage("Chatting has been suspended.");

			// Send same sound packet in both "delay" cases.
			sendPacket(new PlaySound("systemmsg_e.346"));
			break;

		}
		case JAIL: // Jail Player
		{
			_punishLevel = state;
			_punishTimer = 0;

			// Remove the task if any
			stopPunishTask(false);

			if (delayInMinutes > 0)
			{
				_punishTimer = delayInMilliseconds;

				// start the countdown
				_punishTask = ThreadPoolManager.getInstance().scheduleGeneral(new PunishTask(), _punishTimer);
				sendMessage("You are jailed for " + delayInMinutes + " minutes.");
			}

			if (OlympiadManager.getInstance().isRegisteredInComp(this))
				OlympiadManager.getInstance().removeDisconnectedCompetitor(this);

			// Open a Html message to inform the player
			NpcHtmlMessage htmlMsg = new NpcHtmlMessage(0);
			htmlMsg.setFile("data/html/jail_in.htm");
			sendPacket(htmlMsg);
			setIsIn7sDungeon(false);

			teleToLocation(-114356, -249645, -2984, 0); // Jail
			break;
		}
		case CHAR: // Ban Character
		{
			setAccessLevel(-100);
			logout();
			break;
		}
		case ACC: // Ban Account
		{
			setAccountAccesslevel(-100);
			logout();
			break;
		}
		default:
		{
			_punishLevel = state;
			break;
		}
		}

		// store in database
		storeCharBase();
	}

	public long getPunishTimer()
	{
		return _punishTimer;
	}

	public void setPunishTimer(long time)
	{
		_punishTimer = time;
	}

	private void updatePunishState()
	{
		if (getPunishLevel() != PunishLevel.NONE)
		{
			// If punish timer exists, restart punishtask.
			if (_punishTimer > 0)
			{
				_punishTask = ThreadPoolManager.getInstance().scheduleGeneral(new PunishTask(), _punishTimer);
				sendMessage("You are still " + getPunishLevel().string() + " for " + Math.round(_punishTimer / 60000f) + " minutes.");
			}
			if (getPunishLevel() == PunishLevel.JAIL)
			{
				// If player escaped, put him back in jail
				if (!isInsideZone(ZoneId.JAIL))
					teleToLocation(-114356, -249645, -2984, 20);
			}
		}
	}

	public void stopPunishTask(boolean save)
	{
		if (_punishTask != null)
		{
			if (save)
			{
				long delay = _punishTask.getDelay(TimeUnit.MILLISECONDS);
				if (delay < 0)
					delay = 0;
				setPunishTimer(delay);
			}
			_punishTask.cancel(false);
			_punishTask = null;
		}
	}

	protected class PunishTask implements Runnable
	{
		@Override
		public void run()
		{
			setPunishLevel(PunishLevel.NONE, 0);
		}
	}

	public int getPowerGrade()
	{
		return _powerGrade;
	}

	public void setPowerGrade(int power)
	{
		_powerGrade = power;
	}

	public boolean isCursedWeaponEquipped()
	{
		return _cursedWeaponEquippedId != 0;
	}

	public void setCursedWeaponEquippedId(int value)
	{
		_cursedWeaponEquippedId = value;
	}

	public int getCursedWeaponEquippedId()
	{
		return _cursedWeaponEquippedId;
	}

	public void shortBuffStatusUpdate(int magicId, int level, int time)
	{
		if (_shortBuffTask != null)
		{
			_shortBuffTask.cancel(false);
			_shortBuffTask = null;
		}
		_shortBuffTask = ThreadPoolManager.getInstance().scheduleGeneral(new ShortBuffTask(), time * 1000);
		setShortBuffTaskSkillId(magicId);

		sendPacket(new ShortBuffStatusUpdate(magicId, level, time));
	}

	public int getShortBuffTaskSkillId()
	{
		return _shortBuffTaskSkillId;
	}

	public void setShortBuffTaskSkillId(int id)
	{
		_shortBuffTaskSkillId = id;
	}

	public int getDeathPenaltyBuffLevel()
	{
		return _deathPenaltyBuffLevel;
	}

	public void setDeathPenaltyBuffLevel(int level)
	{
		_deathPenaltyBuffLevel = level;
	}

	public void calculateDeathPenaltyBuffLevel(L2Character killer)
	{
		if (_deathPenaltyBuffLevel >= 15) // maximum level reached
			return;

		if ((getKarma() > 0 || Rnd.get(1, 100) <= Config.DEATH_PENALTY_CHANCE) && !(killer instanceof L2PcInstance) && !isGM() && !(getCharmOfLuck() && (killer == null || killer.isRaid())) && !isPhoenixBlessed() && !(isInsideZone(ZoneId.PVP) || isInsideZone(ZoneId.SIEGE)))
		{
			if (_deathPenaltyBuffLevel != 0)
			{
				final L2Skill skill = SkillTable.getInstance().getInfo(5076, _deathPenaltyBuffLevel);
				if (skill != null)
					removeSkill(skill, true);
			}

			_deathPenaltyBuffLevel++;

			addSkill(SkillTable.getInstance().getInfo(5076, _deathPenaltyBuffLevel), false);
			sendPacket(new EtcStatusUpdate(this));
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.DEATH_PENALTY_LEVEL_S1_ADDED).addNumber(_deathPenaltyBuffLevel));
		}
	}

	public void reduceDeathPenaltyBuffLevel()
	{
		if (_deathPenaltyBuffLevel <= 0)
			return;

		final L2Skill skill = SkillTable.getInstance().getInfo(5076, _deathPenaltyBuffLevel);
		if (skill != null)
			removeSkill(skill, true);

		_deathPenaltyBuffLevel--;

		if (_deathPenaltyBuffLevel > 0)
		{
			addSkill(SkillTable.getInstance().getInfo(5076, _deathPenaltyBuffLevel), false);
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.DEATH_PENALTY_LEVEL_S1_ADDED).addNumber(_deathPenaltyBuffLevel));
		}
		else
			sendPacket(SystemMessageId.DEATH_PENALTY_LIFTED);

		sendPacket(new EtcStatusUpdate(this));
	}

	public void restoreDeathPenaltyBuffLevel()
	{
		if (_deathPenaltyBuffLevel > 0)
			addSkill(SkillTable.getInstance().getInfo(5076, _deathPenaltyBuffLevel), false);
	}

	public final Map<Integer, TimeStamp> _reuseTimeStamps = new ConcurrentHashMap<>();

	public Collection<TimeStamp> getReuseTimeStamps()
	{
		return _reuseTimeStamps.values();
	}

	public Map<Integer, TimeStamp> getReuseTimeStamp()
	{
		return _reuseTimeStamps;
	}

	/**
	 * Simple class containing all neccessary information to maintain valid timestamps and reuse for skills upon relog. Filter this carefully as it becomes redundant to store reuse for small delays.
	 * @author Yesod
	 */
	public static class TimeStamp
	{
		private final int _skillId;
		private final int _skillLvl;
		private final long _reuse;
		private final long _stamp;

		public TimeStamp(L2Skill skill, long reuse)
		{
			_skillId = skill.getId();
			_skillLvl = skill.getLevel();
			_reuse = reuse;
			_stamp = System.currentTimeMillis() + reuse;
		}

		public TimeStamp(L2Skill skill, long reuse, long systime)
		{
			_skillId = skill.getId();
			_skillLvl = skill.getLevel();
			_reuse = reuse;
			_stamp = systime;
		}

		public long getStamp()
		{
			return _stamp;
		}

		public int getSkillId()
		{
			return _skillId;
		}

		public int getSkillLvl()
		{
			return _skillLvl;
		}

		public long getReuse()
		{
			return _reuse;
		}

		public long getRemaining()
		{
			return Math.max(_stamp - System.currentTimeMillis(), 0);
		}

		public boolean hasNotPassed()
		{
			return System.currentTimeMillis() < _stamp;
		}
	}

	/**
	 * Index according to skill id the current timestamp of use.
	 * @param skill
	 * @param reuse delay
	 */
	@Override
	public void addTimeStamp(L2Skill skill, long reuse)
	{
		_reuseTimeStamps.put(skill.getReuseHashCode(), new TimeStamp(skill, reuse));
	}

	/**
	 * Index according to skill this TimeStamp instance for restoration purposes only.
	 * @param skill
	 * @param reuse
	 * @param systime
	 */
	public void addTimeStamp(L2Skill skill, long reuse, long systime)
	{
		_reuseTimeStamps.put(skill.getReuseHashCode(), new TimeStamp(skill, reuse, systime));
	}

	@Override
	public L2PcInstance getActingPlayer()
	{
		return this;
	}

	@Override
	public final void sendDamageMessage(L2Character target, int damage, boolean mcrit, boolean pcrit, boolean miss)
	{

		// ObtÃƒÂ©n el ID del objetivo
		int targetId = target.getObjectId();

		// Check if hit is missed
		if (miss)
		{
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.MISSED_TARGET).addNumber(targetId));

			return;
		}

		// Check if hit is critical
		if (pcrit)
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CRITICAL_HIT).addNumber(targetId));

		if (mcrit)
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CRITICAL_HIT_MAGIC).addNumber(targetId));

		if (target.isInvul())
		{
			if (target.isParalyzed()) {
				sendPacket(SystemMessage.getSystemMessage(SystemMessageId.OPPONENT_PETRIFIED).addNumber(targetId));

			}


			else {
				sendPacket(SystemMessage.getSystemMessage(SystemMessageId.ATTACK_WAS_BLOCKED).addNumber(targetId));

			}


		}
		else
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_DID_S1_DMG).addNumber(damage).addNumber(targetId));

		if (isInOlympiadMode() && target instanceof L2PcInstance && ((L2PcInstance) target).isInOlympiadMode() && ((L2PcInstance) target).getOlympiadGameId() == getOlympiadGameId())
			OlympiadGameManager.getInstance().notifyCompetitorDamage(this, damage);
	}

	public void checkItemRestriction()
	{
		for (int i = 0; i < Inventory.PAPERDOLL_TOTALSLOTS; i++)
		{
			ItemInstance equippedItem = getInventory().getPaperdollItem(i);
			if (equippedItem != null && !equippedItem.getItem().checkCondition(this, this, false))
			{
				getInventory().unEquipItemInSlot(i);

				InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(equippedItem);
				sendPacket(iu);

				SystemMessage sm = null;
				if (equippedItem.getEnchantLevel() > 0)
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED);
					sm.addNumber(equippedItem.getEnchantLevel());
					sm.addItemName(equippedItem);
				}
				else
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.S1_DISARMED);
					sm.addItemName(equippedItem);
				}
				sendPacket(sm);
			}
		}
	}

	protected class Dismount implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				dismount();
			}
			catch (Exception e)
			{
				_log.log(Level.WARNING, "Exception on dismount(): " + e.getMessage(), e);
			}
		}
	}

	public void enteredNoLanding(int delay)
	{
		_dismountTask = ThreadPoolManager.getInstance().scheduleGeneral(new Dismount(), delay * 1000);
	}

	public void exitedNoLanding()
	{
		if (_dismountTask != null)
		{
			_dismountTask.cancel(true);
			_dismountTask = null;
		}
	}

	public void setIsInSiege(boolean value)
	{
	    _isInSiege = value;

	    PlayerModeManager.getInstance().enforceContext(this);
	}

	public boolean isInSiege()
	{
		return _isInSiege;
	}

	public FloodProtectors getFloodProtectors()
	{
		return getClient().getFloodProtectors();
	}

	@Override
	public int getSiegeSide()
	{
		return _siegeSide;
	}

	/**
	 * Set the siege side of the L2PcInstance.
	 * @param val
	 */
	public void setSiegeSide(int val)
	{
		_siegeSide = val;
	}

	public boolean isRegisteredOnThisSiegeField(int val)
	{
		if (_siegeSide == 0)
			return false;

		return true;
	} 

	/**
	 * Remove player from BossZones (used on char logout/exit)
	 */
	public void removeFromBossZone()
	{
		try
		{
			for (L2BossZone _zone : GrandBossManager.getInstance().getZones())
				_zone.removePlayer(this);
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Exception on removeFromBossZone(): " + e.getMessage(), e);
		}
	}

	/**
	 * @return the number of charges this L2PcInstance got.
	 */
	public int getCharges()
	{
		return _charges.get();
	}

	public void increaseCharges(int count, int max)
	{
		if (_charges.get() >= max)
		{
			sendPacket(SystemMessageId.FORCE_MAXLEVEL_REACHED);
			return;
		}

		restartChargeTask();

		if (_charges.addAndGet(count) >= max)
		{
			_charges.set(max);
			sendPacket(SystemMessageId.FORCE_MAXLEVEL_REACHED);
		}
		else
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.FORCE_INCREASED_TO_S1).addNumber(_charges.get()));

		sendPacket(new EtcStatusUpdate(this));
	}

	public boolean decreaseCharges(int count)
	{
		if (_charges.get() < count)
			return false;

		if (_charges.addAndGet(-count) == 0)
			stopChargeTask();
		else
			restartChargeTask();

		sendPacket(new EtcStatusUpdate(this));
		return true;
	}

	public void clearCharges()
	{
		_charges.set(0);
		sendPacket(new EtcStatusUpdate(this));
	}

	/**
	 * Starts/Restarts the ChargeTask to Clear Charges after 10 Mins.
	 */
	private void restartChargeTask()
	{
		if (_chargeTask != null)
		{
			_chargeTask.cancel(false);
			_chargeTask = null;
		}
		_chargeTask = ThreadPoolManager.getInstance().scheduleGeneral(new ChargeTask(), 600000);
	}

	/**
	 * Stops the Charges Clearing Task.
	 */
	public void stopChargeTask()
	{
		if (_chargeTask != null)
		{
			_chargeTask.cancel(false);
			_chargeTask = null;
		}
	}

	protected class ChargeTask implements Runnable
	{
		@Override
		public void run()
		{
			clearCharges();
		}
	}

	/**
	 * Signets check used to valid who is affected when he entered in the aoe effect.
	 * @param cha The target to make checks on.
	 * @return true if player can attack the target.
	 */
	public boolean canAttackCharacter(L2Character cha)
	{
		if (cha instanceof L2Attackable)
			return true;

		if (cha instanceof L2Playable)
		{
			if (cha.isInArena())
				return true;

			final L2PcInstance target = cha.getActingPlayer();

			if (isInDuel() && target.isInDuel() && target.getDuelId() == getDuelId())
				return true;

			if (isInParty() && target.isInParty())
			{
				if (getParty() == target.getParty())
					return false;

				if ((getParty().getCommandChannel() != null || target.getParty().getCommandChannel() != null) && (getParty().getCommandChannel() == target.getParty().getCommandChannel()))
					return false;
			}

			if (getClan() != null && target.getClan() != null)
			{
				if (getClanId() == target.getClanId())
					return false;

				if ((getAllyId() > 0 || target.getAllyId() > 0) && getAllyId() == target.getAllyId())
					return false;

				if (getClan().isAtWarWith(target.getClanId()))
					return true;
			}
			else
			{
				if (target.getPvpFlag() == 0 && target.getKarma() == 0)
					return false;
			}
		}
		return true;
	}

	/**
	 * Request Teleport
	 * @param requester The player who requested the teleport.
	 * @param skill The used skill.
	 * @return true if successful.
	 **/
	public boolean teleportRequest(L2PcInstance requester, L2Skill skill)
	{
		if (_summonRequest.getTarget() != null && requester != null)
			return false;

		_summonRequest.setTarget(requester, skill);
		return true;
	}

	/**
	 * Action teleport
	 * @param answer
	 * @param requesterId
	 **/
	public void teleportAnswer(int answer, int requesterId)
	{
		if (_summonRequest.getTarget() == null)
			return;

		if (answer == 1 && _summonRequest.getTarget().getObjectId() == requesterId)
			teleToTarget(this, _summonRequest.getTarget(), _summonRequest.getSkill());

		_summonRequest.setTarget(null, null);
	}

	public static void teleToTarget(L2PcInstance targetChar, L2PcInstance summonerChar, L2Skill summonSkill)
	{
		if (targetChar == null || summonerChar == null || summonSkill == null)
			return;

		if (!checkSummonerStatus(summonerChar))
			return;
		if (!checkSummonTargetStatus(targetChar, summonerChar))
			return;

		int itemConsumeId = summonSkill.getTargetConsumeId();
		int itemConsumeCount = summonSkill.getTargetConsume();
		if (itemConsumeId != 0 && itemConsumeCount != 0)
		{
			// Delete by rocknow
			if (targetChar.getInventory().getInventoryItemCount(itemConsumeId, 0) < itemConsumeCount)
			{
				targetChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_REQUIRED_FOR_SUMMONING).addItemName(summonSkill.getTargetConsumeId()));
				return;
			}
			targetChar.getInventory().destroyItemByItemId("Consume", itemConsumeId, itemConsumeCount, summonerChar, targetChar);
			targetChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_DISAPPEARED).addItemName(summonSkill.getTargetConsumeId()));
		}
		targetChar.teleToLocation(summonerChar.getX(), summonerChar.getY(), summonerChar.getZ(), 20);
	}

	public static boolean checkSummonerStatus(L2PcInstance summonerChar)
	{
		if (summonerChar == null)
			return false;

		if (summonerChar.isInOlympiadMode() || summonerChar.inObserverMode() || summonerChar.isInsideZone(ZoneId.NO_SUMMON_FRIEND) || summonerChar.isMounted())
			return false;

		if (!MultiTvTEvent.onEscapeUse(summonerChar.getObjectId()) || !KTBEvent.onEscapeUse(summonerChar.getObjectId()) ||!TvTEvent.onEscapeUse(summonerChar.getObjectId()) || !DMEvent.onEscapeUse(summonerChar.getObjectId()) || !LMEvent.onEscapeUse(summonerChar.getObjectId()))
		{
			summonerChar.sendPacket(SystemMessageId.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING);
			return false;
		}

		return true;
	}

	public static boolean checkSummonTargetStatus(L2Object target, L2PcInstance summonerChar)
	{
		if (target == null || !(target instanceof L2PcInstance))
			return false;

		L2PcInstance targetChar = (L2PcInstance) target;

		if (targetChar.isAlikeDead())
		{
			summonerChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_IS_DEAD_AT_THE_MOMENT_AND_CANNOT_BE_SUMMONED).addPcName(targetChar));
			return false;
		}

		if (targetChar.isInStoreMode())
		{
			summonerChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_CURRENTLY_TRADING_OR_OPERATING_PRIVATE_STORE_AND_CANNOT_BE_SUMMONED).addPcName(targetChar));
			return false;
		}

		if (targetChar.isRooted() || targetChar.isInCombat())
		{
			summonerChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_IS_ENGAGED_IN_COMBAT_AND_CANNOT_BE_SUMMONED).addPcName(targetChar));
			return false;
		}

		if (targetChar.isInOlympiadMode())
		{
			summonerChar.sendPacket(SystemMessageId.YOU_CANNOT_SUMMON_PLAYERS_WHO_ARE_IN_OLYMPIAD);
			return false;
		}

		if (targetChar.isFestivalParticipant() || targetChar.isMounted())
		{
			summonerChar.sendPacket(SystemMessageId.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING);
			return false;
		}

		if (targetChar.inObserverMode() || targetChar.isInsideZone(ZoneId.NO_SUMMON_FRIEND))
		{
			summonerChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_IN_SUMMON_BLOCKING_AREA).addCharName(targetChar));
			return false;
		}

		if (!CTFEvent.onEscapeUse(targetChar.getObjectId()) || !FOSEvent.onEscapeUse(targetChar.getObjectId()) || !DMEvent.onEscapeUse(targetChar.getObjectId()) || !LMEvent.onEscapeUse(targetChar.getObjectId()) || !TvTEvent.onEscapeUse(targetChar.getObjectId()) || !KTBEvent.onEscapeUse(targetChar.getObjectId()))
		{
			summonerChar.sendPacket(SystemMessageId.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING);
			return false;
		}

		return true;
	}

	public final int getClientX()
	{
		return _clientX;
	}

	public final int getClientY()
	{
		return _clientY;
	}

	public final int getClientZ()
	{
		return _clientZ;
	}

	public final int getClientHeading()
	{
		return _clientHeading;
	}

	public final void setClientX(int val)
	{
		_clientX = val;
	}

	public final void setClientY(int val)
	{
		_clientY = val;
	}

	public final void setClientZ(int val)
	{
		_clientZ = val;
	}

	public final void setClientHeading(int val)
	{
		_clientHeading = val;
	}

	/**
	 * @return the mailPosition.
	 */
	public int getMailPosition()
	{
		return _mailPosition;
	}

	/**
	 * @param mailPosition The mailPosition to set.
	 */
	public void setMailPosition(int mailPosition)
	{
		_mailPosition = mailPosition;
	}

	/**
	 * @param z
	 * @return true if character falling now On the start of fall return false for correct coord sync !
	 */
	public final boolean isFalling(int z)
	{
		if (isDead() || isFlying() || isInsideZone(ZoneId.WATER))
			return false;

		if (System.currentTimeMillis() < _fallingTimestamp)
			return true;

		final int deltaZ = getZ() - z;
		if (deltaZ <= getBaseTemplate().getFallHeight())
			return false;

		final int damage = (int) Formulas.calcFallDam(this, deltaZ);
		if (damage > 0)
		{
			reduceCurrentHp(Math.min(damage, getCurrentHp() - 1), null, false, true, null);
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.FALL_DAMAGE_S1).addNumber(damage));
		}

		setFalling();

		return false;
	}

	/**
	 * Set falling timestamp
	 */
	public final void setFalling()
	{
		_fallingTimestamp = System.currentTimeMillis() + FALLING_VALIDATION_DELAY;
	}

	public boolean isAllowedToEnchantSkills()
	{
		if (isLocked())
			return false;

		if (AttackStanceTaskManager.getInstance().get(this))
			return false;

		if (isCastingNow() || isCastingSimultaneouslyNow())
			return false;

		if (isInBoat())
			return false;

		return true;
	}

	/**
	 * Friendlist / selected Friendlist (for community board)
	 */
	private final List<Integer> _friendList = new ArrayList<>();
	private final List<Integer> _selectedFriendList = new ArrayList<>();

	public List<Integer> getFriendList()
	{
		return _friendList;
	}

	public void selectFriend(Integer friendId)
	{
		if (!_selectedFriendList.contains(friendId))
			_selectedFriendList.add(friendId);
	}

	public void deselectFriend(Integer friendId)
	{
		if (_selectedFriendList.contains(friendId))
			_selectedFriendList.remove(friendId);
	}

	public List<Integer> getSelectedFriendList()
	{
		return _selectedFriendList;
	}

	private void restoreFriendList()
	{
		_friendList.clear();

		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("SELECT friend_id FROM character_friends WHERE char_id = ? AND relation = 0");
			statement.setInt(1, getObjectId());
			ResultSet rset = statement.executeQuery();

			int friendId;
			while (rset.next())
			{
				friendId = rset.getInt("friend_id");
				if (friendId == getObjectId())
					continue;

				_friendList.add(friendId);
			}

			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Error found in " + getName() + "'s friendlist: " + e.getMessage(), e);
		}
	}

	protected void notifyFriends(boolean login)
	{
		for (int id : _friendList)
		{
			L2PcInstance friend = L2World.getInstance().getPlayer(id);
			if (friend != null)
			{
				friend.sendPacket(new FriendList(friend));

				if (login)
					friend.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.FRIEND_S1_HAS_LOGGED_IN).addPcName(this));
			}
		}
	}

	private final List<Integer> _selectedBlocksList = new ArrayList<>();

	public void selectBlock(Integer friendId)
	{
		if (!_selectedBlocksList.contains(friendId))
			_selectedBlocksList.add(friendId);
	}

	public void deselectBlock(Integer friendId)
	{
		if (_selectedBlocksList.contains(friendId))
			_selectedBlocksList.remove(friendId);
	}

	public List<Integer> getSelectedBlocksList()
	{
		return _selectedBlocksList;
	}

	/**
	 * Test if player inventory is under 80% capaity
	 * @param includeQuestInv check also quest inventory
	 * @return
	 */
	public boolean isInventoryUnder80(boolean includeQuestInv)
	{
		if (getInventory().getSize(false) <= (getInventoryLimit() * 0.8))
		{
			if (includeQuestInv)
			{
				if (getInventory().getSize(true) <= (getQuestInventoryLimit() * 0.8))
					return true;
			}
			else
				return true;
		}
		return false;
	}

	@Override
	public void broadcastRelationsChanges()
	{
		for (L2PcInstance player : getKnownList().getKnownType(L2PcInstance.class))
		{
			player.sendPacket(new RelationChanged(this, getRelation(player), isAutoAttackable(player)));
			if (getPet() != null)
				player.sendPacket(new RelationChanged(getPet(), getRelation(player), isAutoAttackable(player)));
		}
	}

	@Override
	public void sendInfo(L2PcInstance activeChar)
	{
		if (isInBoat())
			getPosition().setWorldPosition(getBoat().getPosition().getWorldPosition());

		if (getPoly().isMorphed())
			activeChar.sendPacket(new AbstractNpcInfo.PcMorphInfo(this, getPoly().getNpcTemplate()));
		else
			activeChar.sendPacket(new CharInfo(this));

		final int relation1 = getRelation(activeChar);
		activeChar.sendPacket(new RelationChanged(this, relation1, isAutoAttackable(activeChar)));
		if (getPet() != null)
			activeChar.sendPacket(new RelationChanged(getPet(), relation1, isAutoAttackable(activeChar)));

		final int relation2 = activeChar.getRelation(this);
		sendPacket(new RelationChanged(activeChar, relation2, activeChar.isAutoAttackable(this)));
		if (activeChar.getPet() != null)
			sendPacket(new RelationChanged(activeChar.getPet(), relation2, activeChar.isAutoAttackable(this)));

		if (isInBoat())
			activeChar.sendPacket(new GetOnVehicle(getObjectId(), getBoat().getObjectId(), getInVehiclePosition()));

		// No reason to try to broadcast shop message if player isn't in store mode
		if (isInStoreMode())
		{
			switch (getPrivateStoreType())
			{
			case STORE_PRIVATE_SELL:
			case STORE_PRIVATE_PACKAGE_SELL:
				activeChar.sendPacket(new PrivateStoreMsgSell(this));
				break;
			case STORE_PRIVATE_BUY:
				activeChar.sendPacket(new PrivateStoreMsgBuy(this));
				break;
			case STORE_PRIVATE_MANUFACTURE:
				activeChar.sendPacket(new RecipeShopMsg(this));
				break;
			}
		}
	}

	public String _originalTitle;

	private static String StringToHex(String color)
	{
		switch(color.length())
		{
		case 1:
			color = (new StringBuilder()).append("00000").append(color).toString();
			break;

		case 2:
			color = (new StringBuilder()).append("0000").append(color).toString();
			break;

		case 3:
			color = (new StringBuilder()).append("000").append(color).toString();
			break;

		case 4:
			color = (new StringBuilder()).append("00").append(color).toString();
			break;

		case 5:
			color = (new StringBuilder()).append("0").append(color).toString();
			break;
		}
		return color;
	}

	public String StringToHexForVote(String color) 
	{
		switch (color.length()) 
		{
		case 1:
			color = color + "00000";
			break;
		case 2:
			color = color + "0000";
			break;
		case 3:
			color = color + "000";
			break;
		case 4:
			color = color + "00";
			break;
		case 5:
			color = color + '0';
			break;
		} 
		return color;
	}

	//TVT
	private int _eventPoints = 0;

	public int getPointScore()
	{
		return _eventPoints;
	}

	public void increasePointScore()
	{
		_eventPoints++;
	}

	public void clearPoints()
	{
		_eventPoints = 0;
	}

	//DM
	private int _eventDMPoints = 0;

	public int getDMPointScore()
	{
		return _eventDMPoints;
	}

	public void increaseDMPointScore()
	{
		_eventDMPoints++;
	}

	public void clearDMPoints()
	{
		_eventDMPoints = 0;
	}

	//FOS
	private int _eventFOSPoints = 0;

	public int getFOSPointScore()
	{
		return _eventFOSPoints;
	}

	public void increaseFOSPointScore()
	{
		_eventFOSPoints++;
	}

	public void clearFOSPoints()
	{
		_eventFOSPoints = 0;
	}

	private long _offlineShopStart = 0;

	public long getOfflineStartTime()
	{
		return _offlineShopStart;
	}

	public void setOfflineStartTime(long time)
	{
		_offlineShopStart = time;
	}

	//--------------
	//AIO Settings
	//--------------

	public boolean isAio()
	{
		return _isAio;
	}

	public void setAio(boolean b)
	{
		_isAio = b;
	}

	public long getAioEndTime()
	{
		return _aio_endTime;
	}

	public void setAioEndTime(long val)
	{
		_aio_endTime = val;
	}

	//--------------
	//VIP/AIO Settings
	//--------------

	public void rewardAioSkills()
	{
		for(Integer skillid : Config.AIO_SKILLS.keySet())
		{
			int skilllvl = Config.AIO_SKILLS.get(skillid);
			L2Skill skill = SkillTable.getInstance().getInfo(skillid, skilllvl);

			if (skill != null)
				addSkill(skill, true);
		}
	}

	public void removeSkills()
	{
		for (L2Skill skill : getAllSkills())
			removeSkill(skill);
	}

	public void rewardVipSkills()
	{
		for (Integer skillid : Config.VIP_SKILLS.keySet())
		{
			int skilllvl = Config.VIP_SKILLS.get(skillid);
			L2Skill skill = SkillTable.getInstance().getInfo(skillid,skilllvl);

			if (skill != null)
				addSkill(skill, true);
		}
	}

	public void lostVipSkills()
	{
		for (L2Skill skill : getAllSkills())
			removeSkill(skill);
	}

	//--------------
	//VIP Settings
	//--------------

	public boolean isVip()
	{
		return _isVip;
	}

	public void setVip(boolean val)
	{
		if (_isVip && !val)
			VipAuditLog.logVipEnd(this, _vip_endTime);
		_isVip = val;
	}

	public long getVipEndTime()
	{
		return _vip_endTime;
	}

	public void setVipEndTime(long val)
	{
		_vip_endTime = val;
	}

	//--------------
	//Hero Timed Settings
	//--------------

	public boolean isTimedHero()
	{
		return _isTimedHero;
	}

	public void setTimedHero(boolean val)
	{
		_isTimedHero = val;
	}

	public long getTimedHeroEndTime()
	{
		return _timedHero_endTime;
	}

	public void setTimedHeroEndTime(long val)
	{
		_timedHero_endTime = val;
	}

	//--------------
	//DATA Settings
	//--------------

	public void setEndTime(String process, int val)
	{
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DAY_OF_MONTH, val);
		long end_day = calendar.getTimeInMillis();

		if (process.equals("aio"))
			_aio_endTime = end_day;

		if (process.equals("vip"))
		{
			long oldEnd = _vip_endTime;
			_vip_endTime = end_day;
			VipAuditLog.logVipTimeChange(this, oldEnd, end_day);
		}

		if (process.equals("timedHero"))
			_timedHero_endTime = end_day;
	}

	public void setEndTimebyHour(String process, int val)
	{
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.MINUTE, val);
		long end_day = calendar.getTimeInMillis();

		if (process.equals("aio"))
			_aio_endTime = end_day;

		if (process.equals("vip"))
		{
			long oldEnd = _vip_endTime;
			_vip_endTime = end_day;
			VipAuditLog.logVipTimeChange(this, oldEnd, end_day);
		}

		if (process.equals("timedHero"))
			_timedHero_endTime = end_day;
	}

	//--------------
	//PCBang Settings
	//--------------

	public int getPcBangScore()
	{
		return pcBangPoint;
	}

	public void reducePcBangScore(int to)
	{
		pcBangPoint -= to;
		updatePcBangWnd(to, false, false);
	}

	public void addPcBangScore(int to)
	{
		pcBangPoint += to;
	}

	public void updatePcBangWnd(int score, boolean add, boolean duble)
	{
		ExPCCafePointInfo wnd = new ExPCCafePointInfo(this, score, add, 24, duble);
		sendPacket(wnd);
	}

	public void showPcBangWindow()
	{
		ExPCCafePointInfo wnd = new ExPCCafePointInfo(this, 0, false, 24, false);
		sendPacket(wnd);
	}

	public void restoreCustomStatus()
	{
		if (Config.DEVELOPER)
			_log.info("Restoring character status " + getName() + " from database..."); 
		int hero = 0;
		int noble = 0;
		long hero_end = 0L;
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("SELECT hero, noble, hero_end_date FROM characters_custom_data WHERE obj_Id = ?");
			statement.setInt(1, getObjectId());
			ResultSet result = statement.executeQuery();
			while (result.next())
			{
				hero = result.getInt("hero");
				noble = result.getInt("noble");
				hero_end = result.getLong("hero_end_date");
			} 
			result.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("Error: could not restore char custom data info: " + e);
		} 
		if (hero > 0 && (hero_end == 0L || hero_end > System.currentTimeMillis()))
		{
			setHero(true);
		} 
		else 
		{
			destroyItem("HeroEnd", 6842, 1, (L2Object)this, false);
		} 
		if (noble > 0)
			setNoble(true, true); 
	}

	/**
	 * check player skills and remove unlegit ones (excludes hero, noblesse and cursed weapon skills).
	 */
	public void checkAllowedSkills()
	{
		boolean foundskill = false;
		if (!isGM())
		{
			Collection<L2SkillLearn> skillTree = SkillTreeTable.getInstance().getAllowedSkills(getClassId());
			// loop through all skills of player
			for (L2Skill skill : getAllSkills())
			{
				int skillid = skill.getId();

				foundskill = false;
				// loop through all skills in players skilltree
				for (L2SkillLearn temp : skillTree)
				{
					// if the skill was found and the level is possible to obtain for his class everything is ok
					if (temp.getId() == skillid)
					{
						foundskill = true;
					}
				}

				// exclude noble skills
				if (Config.CHECK_NOBLE_SKILLS)
				{
					if (isNoble() && skillid >= 325 && skillid <= 327)
						foundskill = true;
					if (isNoble() && skillid >= 1323 && skillid <= 1327)
						foundskill = true;
				}
				else
				{
					if (skillid >= 325 && skillid <= 327)
						foundskill = true;
					if (skillid >= 1323 && skillid <= 1327)
						foundskill = true;
				}

				if (Config.CHECK_HERO_SKILLS)
				{
					if (isHero() && skillid >= 395 && skillid <= 396)
						foundskill = true;
					if (isHero() && skillid >= 1374 && skillid <= 1376)
						foundskill = true;
				}
				else
				{
					if (skillid >= 395 && skillid <= 396)
						foundskill = true;
					if (skillid >= 1374 && skillid <= 1376)
						foundskill = true;
				}

				// exclude cursed weapon skills
				if (isCursedWeaponEquipped() && skillid == CursedWeaponsManager.getInstance().getCursedWeapon(_cursedWeaponEquippedId).getSkillId())
				{
					foundskill = true;
				}

				// exclude clan skills
				if (getClan() != null && skillid >= 370 && skillid <= 391)
				{
					foundskill = true;
				}

				// exclude seal of ruler / build siege hq
				if (getClan() != null && (skillid == 246 || skillid == 247))
				{
					if(getClan().getLeaderId() == getObjectId())
					{
						foundskill = true;
					}
				}

				// exclude fishing skills and common skills + dwarfen craft
				if (skillid >= 1312 && skillid <= 1322)
				{
					foundskill = true;
				}

				if (skillid >= 1368 && skillid <= 1373)
				{
					foundskill = true;
				}

				// exclude sa / enchant bonus / penality etc. skills
				if (skillid >= 3000 && skillid < 7000)
				{
					foundskill = true;
				}

				// exclude Skills from AllowedSkills in options.properties
				if (Config.ALLOWED_SKILLS_LIST.contains(skillid))
				{
					foundskill = true;
				}

				// remove skill and do a lil log message
				if (!foundskill)
				{
					removeSkill(skill);

					if (Config.DEBUG)
						_log.warning("Character " + getName() + " of Account " + getAccountName() + " got skill " + skill.getName() + ".. Removed!");
				}
			}
			skillTree = null;
		}
	}

	private boolean _first_log;
	private boolean _select_armor;
	private boolean _select_weapon;
	private boolean _select_classe;

	public boolean getFirstLog()
	{
		return _first_log;
	}

	public boolean getSelectArmor()
	{
		return _select_armor;
	}

	public boolean getSelectWeapon()
	{
		return _select_weapon;
	}

	public boolean getSelectClasse()
	{
		return _select_classe;
	}

	public void setFirstLog(int firstlog)
	{
		_first_log = false;

		if (firstlog == 1)
			_first_log = true;
	}

	public void setSelectArmor(int selectArmor)
	{
		_select_armor = false;

		if (selectArmor == 1)
			_select_armor = true;
	}

	public void setSelectWeapon(int selectWeapon)
	{
		_select_weapon = false;

		if (selectWeapon == 1)
			_select_weapon = true;
	}

	public void setSelectClasse(int selectClasse)
	{
		_select_classe = false;

		if (selectClasse == 1)
			_select_classe = true;
	}

	public void setFirstLog(boolean firstlog)
	{
		_first_log = firstlog;
	}

	public void setSelectArmor(boolean selectArmor)
	{
		_select_armor = selectArmor;
	}

	public void setSelectWeapon(boolean selectWeapon)
	{
		_select_weapon = selectWeapon;
	}

	public void setSelectClasse(boolean selectClasse)
	{
		_select_classe = selectClasse;
	}

	public void updateFirstLog()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("UPDATE characters SET first_log=? WHERE obj_id=?");

			int _fl;

			if (getFirstLog())
				_fl = 1;
			else
				_fl = 0;

			statement.setInt(1, _fl);
			statement.setInt(2, getObjectId());
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("could not set char first login:" + e);
		}
	}

	public void updateSelectArmor()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("UPDATE characters SET select_armor=? WHERE obj_id=?");

			int _sArmor;

			if (getSelectArmor())
				_sArmor = 1;
			else
				_sArmor = 0;

			statement.setInt(1, _sArmor);
			statement.setInt(2, getObjectId());
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("could not set char select armor:" + e);
		}
	}

	public void updateSelectWeapon()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("UPDATE characters SET select_weapon=? WHERE obj_id=?");

			int _sWeapon;

			if (getSelectWeapon())
				_sWeapon = 1;
			else
				_sWeapon = 0;

			statement.setInt(1, _sWeapon);
			statement.setInt(2, getObjectId());
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("could not set char select weapon:" + e);
		}
	}

	public void updateSelectClasse()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("UPDATE characters SET select_classe=? WHERE obj_id=?");

			int _sClasse;

			if (getSelectClasse())
				_sClasse = 1;
			else
				_sClasse = 0;

			statement.setInt(1, _sClasse);
			statement.setInt(2, getObjectId());
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("could not set char select class:" + e);
		}
	}

	private boolean _isCMultisell = false;

	public boolean isCMultisell()
	{
		return _isCMultisell;
	}

	public void setIsUsingCMultisell(boolean b)
	{
		_isCMultisell = b;
	}

	private int[] _timeTeleport;

	public void TimeTeleporterCoords(int[] cords)
	{
		_timeTeleport = cords;
	}

	public int[] getTimeTeleportCoords()
	{
		return _timeTeleport;
	}

	private int _nameChangeItemId = 0;

	public int getNameChangeItemId()
	{
		return _nameChangeItemId;
	}

	public void setNameChangeItemId(int itemId)
	{
		_nameChangeItemId = itemId;
	}

	/*
	public boolean _pincheck;
	public int _pin;

	public void setPincheck(boolean pincheck)
	{
		_pincheck = pincheck;
	}

	public void setPincheck(int pincheck)
	{
		_pincheck = false;
		if(pincheck == 1)
		{
			_pincheck = true;
		}
	}

	public boolean getPincheck()
	{
		return _pincheck;
	}

	public void setPin(int pin)
	{
		_pin = pin;
	}

	public int getPin()
	{
		return _pin;
	}

	public void updatePincheck()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("UPDATE characters SET pinsubmited=? WHERE obj_id=?");

			int _pin;

			if (getPincheck())
			{
				_pin = 1;
			}
			else
			{
				_pin = 0;
			}
			statement.setInt(1, _pin);
			statement.setInt(2, getObjectId());
			statement.execute();
			statement.close();
			con.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			_log.warning("could not set char first login:" + e);
		}
	}
	 */

	private int _classChangeItemId = 0;

	public int getClassChangeItemId()
	{
		return _classChangeItemId;
	}

	public void setClassChangeItemId(int itemId)
	{
		_classChangeItemId = itemId;
	}

	public long getOnlineTime()
	{
		return _onlineTime;
	}

	//HWID Ban 
	private String _hwid;
	private boolean _HwidBlock;

	public String getHWid()
	{
		if (getClient() == null)
		{
			return _hwid;
		}
		_hwid = getClient().getHWID();
		return _hwid;
	}

	public void setHwidBlock(boolean comm)
	{
		_HwidBlock = comm;
	}

	public boolean isHwidBlocked()
	{
		return _HwidBlock;
	}

	public String getIP()
	{
		if (getClient().getConnection() == null)
			return "N/A IP";

		return getClient().getConnection().getInetAddress().getHostAddress();
	}

	//Tournament
	public int duelist_cont = 0, dreadnought_cont = 0, tanker_cont = 0, dagger_cont = 0, archer_cont = 0, bs_cont = 0, archmage_cont = 0, soultaker_cont = 0, mysticMuse_cont = 0, stormScreamer_cont = 0, titan_cont = 0, grandKhauatari_cont = 0, dominator_cont = 0, doomcryer_cont = 0;

	//--------------
	//Check Event
	//--------------

	public boolean atEvent = false;
	public boolean atCTFEvent = false;
	public boolean atTVTEvent = false;
	public boolean atDMEvent = false;
	public boolean atLMEvent = false;
	public boolean atKTBEvent = false;
	public boolean atFOSEvent = false;
	public boolean atMultiTVTEvent = false;

	public boolean isInFunEvent()
	{
		return (atEvent || (MultiTvTEvent.isStarted() && MultiTvTEvent.isPlayerParticipant(getObjectId())) || (TvTEvent.isStarted() && TvTEvent.isPlayerParticipant(getObjectId())) || (CTFEvent.isStarted() && CTFEvent.isPlayerParticipant(getObjectId())) || (FOSEvent.isStarted() && FOSEvent.isPlayerParticipant(getObjectId())) || (DMEvent.isStarted() && DMEvent.isPlayerParticipant(getObjectId())) || (LMEvent.isStarted() && LMEvent.isPlayerParticipant(getObjectId())) || (KTBEvent.isStarted() && KTBEvent.isPlayerParticipant(getObjectId())));
	}

	public boolean isInCTFEvent()
	{
		return (atCTFEvent || (CTFEvent.isStarted() && CTFEvent.isPlayerParticipant(getObjectId())));
	}

	public byte CTFTeamName()
	{
		return (CTFEvent.getParticipantTeamId(getObjectId()));
	}

	public boolean isInTVTEvent()
	{
		return (atTVTEvent || (TvTEvent.isStarted() && TvTEvent.isPlayerParticipant(getObjectId())));
	}

	public byte TVTTeamName()
	{
		return (TvTEvent.getParticipantTeamId(getObjectId()));
	}

	public boolean isInDMEvent()
	{
		return (atDMEvent || (DMEvent.isStarted() && DMEvent.isPlayerParticipant(getObjectId())));
	}

	public boolean isInLMEvent()
	{
		return (atLMEvent || (LMEvent.isStarted() && LMEvent.isPlayerParticipant(getObjectId())));
	}

	public boolean isInKTBEvent()
	{
		return (atKTBEvent || (KTBEvent.isStarted() && KTBEvent.isPlayerParticipant(getObjectId())));
	}

	public boolean isInFOSEvent()
	{
		return (atFOSEvent || (FOSEvent.isStarted() && FOSEvent.isPlayerParticipant(getObjectId())));
	}

	public boolean isInMultiTVTEvent()
	{
		return (atMultiTVTEvent || (MultiTvTEvent.isStarted() && MultiTvTEvent.isPlayerParticipant(getObjectId())));
	}

	public byte MultiTVTTeamName()
	{
		return (MultiTvTEvent.getParticipantTeamId(getObjectId()));
	}

	//--------------
	//Robot Fake Player
	//--------------

	private FakePlayer _fakePlayerUnderControl = null;

	public boolean isControllingFakePlayer()
	{
		return _fakePlayerUnderControl != null;
	}

	public FakePlayer getPlayerUnderControl()
	{
		return _fakePlayerUnderControl;
	}

	public void setPlayerUnderControl(FakePlayer fakePlayer)
	{
		_fakePlayerUnderControl = fakePlayer;
	}

	//--------------
	//Shutdown/Quakesystem
	//--------------

	private boolean _isKillStreak = false;

	public boolean isKillStreak()
	{
		return _isKillStreak;
	}

	public void setKillStreak(boolean b)
	{
		_isKillStreak = b;
	}

	//--------------
	//Tournament
	//---------------

	private boolean _tournamentReg;

	public void setTournamentRegArea(boolean comm)
	{
		_tournamentReg = comm;
	}

	public boolean isTournamentRegArea()
	{
		return _tournamentReg;
	}

	//--------------
	//Timed Itens
	//---------------

	private long _timedItens = 0;

	public void setTimedItens(long val)
	{
		_timedItens = val;
	}

	public long getTimedItens()
	{
		return _timedItens;
	}

	public synchronized void removeTimedItens()
	{
		if (!isOnline() || getTimedItens() == 0)
			return;

		setTimedItens(0);

		for (RewardHolder reward : Config.DAILY_LOG_REWARDS)
		{
			if (getInventory().getItemByItemId(reward.getRewardId()) != null)
			{
				ItemInstance item = getInventory().getItemByItemId(reward.getRewardId());
				destroyItemByItemId("Remove Daily Reward", reward.getRewardId(), item.getCount(), null, true);
			}
		}
		store();
	}

	//---------------
	// Auto Farm
	//---------------

	private boolean _autoFarm;
	
	private AutofarmPlayerRoutine _bot = new AutofarmPlayerRoutine(this);

	public AutofarmPlayerRoutine getBot() {
		if (_bot == null)
			_bot = new AutofarmPlayerRoutine(this);

		return _bot;
	}

	public void setAutoFarm(boolean value)
	{
	    if (value && isPveModeActive())
	    {
	        sendMessage("Desative o PvE Mode antes de iniciar o AutoFarm.");
	        return;
	    }

	    _autoFarm = value;
	}

	public boolean isAutoFarm()
	{
		return _autoFarm;
	}

	private int autoFarmRadius = 1200;

	public void setRadius(int value) 
	{
		autoFarmRadius = MathUtil.limit(value, 200, 3000);
	}

	public int getRadius()
	{
		return autoFarmRadius;
	}

	private int autoFarmShortCut = 9;

	public void setPage(int value) 
	{
		autoFarmShortCut = MathUtil.limit(value, 0, 9);
	}

	public int getPage()
	{
		return autoFarmShortCut;
	}

	private int autoFarmHealPercente = 30;

	public void setHealPercent(int value) 
	{
		autoFarmHealPercente = MathUtil.limit(value, 20, 90);
	}

	public int getHealPercent()
	{
		return autoFarmHealPercente;
	}

	private boolean autoFarmBuffProtection = false;

	public void setNoBuffProtection(boolean val) 
	{
		autoFarmBuffProtection = val;
	}

	public boolean isNoBuffProtected()
	{
		return autoFarmBuffProtection;
	}

	private boolean autoAntiKsProtection = false;

	public void setAntiKsProtection(boolean val) 
	{
		autoAntiKsProtection = val;
	}

	public boolean isAntiKsProtected()
	{
		return autoAntiKsProtection;
	}

	private boolean autoFarmSummonAttack = false;

	public void setSummonAttack(boolean val) 
	{
		autoFarmSummonAttack = val;
	}

	public boolean isSummonAttack()
	{
		return autoFarmSummonAttack;
	}

	private int autoFarmSummonSkillPercente = 0;

	public void setSummonSkillPercent(int value) 
	{
		autoFarmSummonSkillPercente = MathUtil.limit(value, 0, 90);
	}

	public int getSummonSkillPercent()
	{
		return autoFarmSummonSkillPercente;
	}

	private int hpPotionPercent = 60;
	private int mpPotionPercent = 60;

	public void setHpPotionPercentage(int value)
	{
		hpPotionPercent = MathUtil.limit(value, 0, 100);
	}

	public int getHpPotionPercentage() 
	{
		return hpPotionPercent;
	}

	public void setMpPotionPercentage(int value) 
	{
		mpPotionPercent = MathUtil.limit(value, 0, 100);
	}

	public int getMpPotionPercentage()
	{
		return mpPotionPercent;
	}

	private List<Integer> _ignoredMonster = new ArrayList<>();

	public void ignoredMonster(Integer npcId)
	{
		_ignoredMonster.add(npcId);
	}

	public void activeMonster(Integer npcId)
	{
		if (_ignoredMonster.contains(npcId))
			_ignoredMonster.remove(npcId);
	}

	public boolean ignoredMonsterContain(int npcId)
	{
		return _ignoredMonster.contains(npcId);
	}

	//---------------
	// Secondary Auto Farm
	//---------------

	private boolean _secondaryAutoFarm;
	private int _secondaryAutoFarmRadius = 1800;
	private int _secondaryAutoFarmHealPercent = 30;
	private int _secondaryAutoFarmMpPercent = 10;
	private int _secondaryAutoFarmChancePercent = 100;
	private int _secondaryAutoFarmSkillPage;
	private String _secondaryAutoFarmSkillType = "attack";
	private String _secondaryAutoFarmFarmType = "Magic";
	private String _secondaryAutoFarmTargetMode = "mobs";
	private boolean _secondaryAutoFarmAssistLeader;
	private boolean _secondaryAutoFarmAssistMobs;
	private boolean _secondaryAutoFarmRandomSkills = true;
	private boolean _secondaryAutoFarmKeepLocation;
	private boolean _secondaryAutoFarmRunCloseUp;
	private int _secondaryAutoFarmStartX;
	private int _secondaryAutoFarmStartY;
	private int _secondaryAutoFarmStartZ;
	private final List<Integer> _secondaryAutoFarmAttackSkills = new ArrayList<>();
	private final List<Integer> _secondaryAutoFarmChanceSkills = new ArrayList<>();
	private final List<Integer> _secondaryAutoFarmSelfSkills = new ArrayList<>();
	private final List<Integer> _secondaryAutoFarmHealSkills = new ArrayList<>();

	public void setSecondaryAutoFarm(boolean value)
	{
	    if (value && isPveModeActive())
	    {
	        sendMessage("Desative o PvE Mode antes de iniciar o AutoFarm.");
	        return;
	    }

	    _secondaryAutoFarm = value;
	}

	public boolean isSecondaryAutoFarm()
	{
		return _secondaryAutoFarm;
	}

	public void setSecondaryAutoFarmRadius(int value)
	{
		_secondaryAutoFarmRadius = MathUtil.limit(value, 200, 3000);
	}

	public int getSecondaryAutoFarmRadius()
	{
		return _secondaryAutoFarmRadius;
	}

	public void setSecondaryAutoFarmHealPercent(int value)
	{
		_secondaryAutoFarmHealPercent = MathUtil.limit(value, 10, 90);
	}

	public int getSecondaryAutoFarmHealPercent()
	{
		return _secondaryAutoFarmHealPercent;
	}

	public void setSecondaryAutoFarmMpPercent(int value)
	{
		_secondaryAutoFarmMpPercent = MathUtil.limit(value, 0, 100);
	}

	public int getSecondaryAutoFarmMpPercent()
	{
		return _secondaryAutoFarmMpPercent;
	}

	public void setSecondaryAutoFarmChancePercent(int value)
	{
		_secondaryAutoFarmChancePercent = MathUtil.limit(value, 0, 100);
	}

	public int getSecondaryAutoFarmChancePercent()
	{
		return _secondaryAutoFarmChancePercent;
	}

	public void setSecondaryAutoFarmSkillPage(int value)
	{
		_secondaryAutoFarmSkillPage = MathUtil.limit(value, 0, 20);
	}

	public int getSecondaryAutoFarmSkillPage()
	{
		return _secondaryAutoFarmSkillPage;
	}

	public void setSecondaryAutoFarmSkillType(String value)
	{
		if ("chance".equalsIgnoreCase(value) || "self".equalsIgnoreCase(value) || "heal".equalsIgnoreCase(value))
			_secondaryAutoFarmSkillType = value.toLowerCase();
		else
			_secondaryAutoFarmSkillType = "attack";

		_secondaryAutoFarmSkillPage = 0;
	}

	public String getSecondaryAutoFarmSkillType()
	{
		return _secondaryAutoFarmSkillType;
	}

	public void switchSecondaryAutoFarmFarmType()
	{
		_secondaryAutoFarmFarmType = "Magic".equals(_secondaryAutoFarmFarmType) ? "Fighter" : "Magic";
	}

	public String getSecondaryAutoFarmFarmType()
	{
		return _secondaryAutoFarmFarmType;
	}

	public void setSecondaryAutoFarmTargetMode(String value)
	{
		if ("pvp".equalsIgnoreCase(value) || "both".equalsIgnoreCase(value))
			_secondaryAutoFarmTargetMode = value.toLowerCase();
		else
			_secondaryAutoFarmTargetMode = "mobs";
	}

	public String getSecondaryAutoFarmTargetMode()
	{
		return _secondaryAutoFarmTargetMode;
	}

	public void setSecondaryAutoFarmAssistLeader(boolean value)
	{
		_secondaryAutoFarmAssistLeader = value;
	}

	public boolean isSecondaryAutoFarmAssistLeader()
	{
		return _secondaryAutoFarmAssistLeader;
	}

	public void setSecondaryAutoFarmAssistMobs(boolean value)
	{
		_secondaryAutoFarmAssistMobs = value;
	}

	public boolean isSecondaryAutoFarmAssistMobs()
	{
		return _secondaryAutoFarmAssistMobs;
	}

	public void setSecondaryAutoFarmRandomSkills(boolean value)
	{
		_secondaryAutoFarmRandomSkills = value;
	}

	public boolean isSecondaryAutoFarmRandomSkills()
	{
		return _secondaryAutoFarmRandomSkills;
	}

	public void setSecondaryAutoFarmKeepLocation(boolean value)
	{
		_secondaryAutoFarmKeepLocation = value;
	}

	public boolean isSecondaryAutoFarmKeepLocation()
	{
		return _secondaryAutoFarmKeepLocation;
	}

	public void setSecondaryAutoFarmRunCloseUp(boolean value)
	{
		_secondaryAutoFarmRunCloseUp = value;
	}

	public boolean isSecondaryAutoFarmRunCloseUp()
	{
		return _secondaryAutoFarmRunCloseUp;
	}

	public void setSecondaryAutoFarmStartLocation()
	{
		_secondaryAutoFarmStartX = getX();
		_secondaryAutoFarmStartY = getY();
		_secondaryAutoFarmStartZ = getZ();
	}

	public int getSecondaryAutoFarmStartX()
	{
		return _secondaryAutoFarmStartX;
	}

	public int getSecondaryAutoFarmStartY()
	{
		return _secondaryAutoFarmStartY;
	}

	public int getSecondaryAutoFarmStartZ()
	{
		return _secondaryAutoFarmStartZ;
	}

	public void toggleSecondaryAutoFarmSkill(String type, int skillId)
	{
		List<Integer> skills = getSecondaryAutoFarmSkills(type);
		if (skills.contains(skillId))
			skills.remove((Integer) skillId);
		else
			skills.add(skillId);
	}

	public List<Integer> getSecondaryAutoFarmSkills(String type)
	{
		if ("chance".equalsIgnoreCase(type))
			return _secondaryAutoFarmChanceSkills;

		if ("self".equalsIgnoreCase(type))
			return _secondaryAutoFarmSelfSkills;

		if ("heal".equalsIgnoreCase(type))
			return _secondaryAutoFarmHealSkills;

		return _secondaryAutoFarmAttackSkills;
	}

	public void clearSecondaryAutoFarmSkills()
	{
		_secondaryAutoFarmAttackSkills.clear();
		_secondaryAutoFarmChanceSkills.clear();
		_secondaryAutoFarmSelfSkills.clear();
		_secondaryAutoFarmHealSkills.clear();
	}
	private boolean _autogb;

	public void setAutoGB(boolean comm)
	{
		_autogb = comm;
	}

	public boolean isAutoGB()
	{
		return _autogb;
	}

	private boolean _autopotionHp;

	public void setAutoPotionHp(boolean comm)
	{
		_autopotionHp = comm;
	}

	public boolean isAutoPotionHp()
	{
		return _autopotionHp;
	}

	private boolean _autopotionMp;

	public void setAutoPotionMp(boolean comm)
	{
		_autopotionMp = comm;
	}

	public boolean isAutoPotionMp()
	{
		return _autopotionMp;
	}

	//--------------
	//Tournament Settings
	//--------------

	private boolean _TournamentTeleport;

	public void setTournamentTeleport(boolean comm)
	{
		_TournamentTeleport = comm;
	}

	public boolean isTournamentTeleport()
	{
		return _TournamentTeleport;
	}

	private boolean _OlympiadProtection;

	public void setOlympiadProtection(boolean comm)
	{
		_OlympiadProtection = comm;
	}

	public boolean isOlympiadProtection()
	{
		return _OlympiadProtection;
	}

	private int _arena1x1Wins;
	private int _arena2x2Wins;

	public void increaseArena1x1Wins() 
	{
		_arena1x1Wins++;
	}

	public void increaseArena2x2Wins()
	{
		_arena2x2Wins++;
	}

	private void setArena1x1Wins(int val) 
	{		
		_arena1x1Wins = val;
	}

	private void setArena2x2Wins(int val)
	{
		_arena2x2Wins = val;
	}

	// --------------------
	// New Augment Scrolls
	// --------------------

	boolean _willAcceptMightAugment = false; // Active Might Lv: 10

	public boolean willAcceptMightAugment()
	{
		return _willAcceptMightAugment;
	}

	public void willAcceptMightAugment(boolean accept)
	{
		_willAcceptMightAugment = accept;
	}

	boolean _willAcceptDuelMightdAugment = false; // Active Duel Might Lv: 10

	public boolean willAcceptDuelMightAugment()
	{
		return _willAcceptDuelMightdAugment;
	}

	public void willAcceptDuelMightAugment(boolean accept)
	{
		_willAcceptDuelMightdAugment = accept;
	}

	boolean _willAcceptShieldStrAugment = false; // Active Shield Lv: 10
	boolean _willAcceptShieldConAugment = false;
	boolean _willAcceptShieldIntAugment = false;
	boolean _willAcceptShieldMenAugment = false;

	public boolean willAcceptShieldStrAugment()
	{
		return _willAcceptShieldStrAugment;
	}

	public void willAcceptShieldStrAugment(boolean accept)
	{
		_willAcceptShieldStrAugment = accept;
	}

	public boolean willAcceptShieldConAugment()
	{
		return _willAcceptShieldConAugment;
	}

	public void willAcceptShieldConAugment(boolean accept)
	{
		_willAcceptShieldConAugment = accept;
	}

	public boolean willAcceptShieldIntAugment()
	{
		return _willAcceptShieldIntAugment;
	}

	public void willAcceptShieldIntAugment(boolean accept)
	{
		_willAcceptShieldIntAugment = accept;
	}

	public boolean willAcceptShieldMenAugment()
	{
		return _willAcceptShieldMenAugment;
	}

	public void willAcceptShieldMenAugment(boolean accept)
	{
		_willAcceptShieldMenAugment = accept;
	}

	boolean _willAcceptEmpowerAugment = false; // Active Empower Lv: 10

	public boolean willAcceptEmpowerAugment()
	{
		return _willAcceptEmpowerAugment;
	}

	public void willAcceptEmpowerAugment(boolean accept)
	{
		_willAcceptEmpowerAugment = accept;
	}

	boolean _willAcceptWildMagicAugment = false; // Active Wild Magic Lv: 10

	public boolean willAcceptWildMagicAugment()
	{
		return _willAcceptWildMagicAugment;
	}

	public void willAcceptWildMagicAugment(boolean accept)
	{
		_willAcceptWildMagicAugment = accept;
	}

	boolean _willAcceptMagicBarrierStrAugment = false; // Active Magic Barrier Lv: 10
	boolean _willAcceptMagicBarrierConAugment = false;
	boolean _willAcceptMagicBarrierIntAugment = false;
	boolean _willAcceptMagicBarrierMenAugment = false;

	public boolean willAcceptMagicBarrierStrAugment()
	{
		return _willAcceptMagicBarrierStrAugment;
	}

	public void willAcceptMagicBarrierStrAugment(boolean accept)
	{
		_willAcceptMagicBarrierStrAugment = accept;
	}

	public boolean willAcceptMagicBarrierConAugment()
	{
		return _willAcceptMagicBarrierConAugment;
	}

	public void willAcceptMagicBarrierConAugment(boolean accept)
	{
		_willAcceptMagicBarrierConAugment = accept;
	}

	public boolean willAcceptMagicBarrierIntAugment()
	{
		return _willAcceptMagicBarrierIntAugment;
	}

	public void willAcceptMagicBarrierIntAugment(boolean accept)
	{
		_willAcceptMagicBarrierIntAugment = accept;
	}

	public boolean willAcceptMagicBarrierMenAugment()
	{
		return _willAcceptMagicBarrierMenAugment;
	}

	public void willAcceptMagicBarrierMenAugment(boolean accept)
	{
		_willAcceptMagicBarrierMenAugment = accept;
	}

	boolean _willAcceptAgilityAugment = false; // Active Agility Lv: 10

	public boolean willAcceptAgilityAugment()
	{
		return _willAcceptAgilityAugment;
	}

	public void willAcceptAgilityAugment(boolean accept)
	{
		_willAcceptAgilityAugment = accept;
	}

	boolean _willAcceptGuidanceAugment = false; // Active Guidance Lv: 10

	public boolean willAcceptGuidanceAugment()
	{
		return _willAcceptGuidanceAugment;
	}

	public void willAcceptGuidanceAugment(boolean accept)
	{
		_willAcceptGuidanceAugment = accept;
	}

	boolean _willAcceptFocusAugment = false; // Active Focus Lv: 10

	public boolean willAcceptFocusAugment()
	{
		return _willAcceptFocusAugment;
	}

	public void willAcceptFocusAugment(boolean accept)
	{
		_willAcceptFocusAugment = accept;
	}

	boolean _willAcceptSpellRefreshAugment = false; // Active Spell Refresh Lv: 3

	public boolean willAcceptSpellRefreshAugment()
	{
		return _willAcceptSpellRefreshAugment;
	}

	public void willAcceptSpellRefreshAugment(boolean accept)
	{
		_willAcceptSpellRefreshAugment = accept;
	}

	boolean _willAcceptHealEmpowerAugment = false; // Active Spell Refresh Lv: 3

	public boolean willAcceptHealEmpowerAugment()
	{
		return _willAcceptHealEmpowerAugment;
	}

	public void willAcceptHealEmpowerAugment(boolean accept)
	{
		_willAcceptHealEmpowerAugment = accept;
	}

	boolean _willAcceptReflectDamageAugment = false; // Active Reflect Damage Lv: 3

	public boolean willAcceptReflectDamageAugment()
	{
		return _willAcceptReflectDamageAugment;
	}

	public void willAcceptReflectDamageAugment(boolean accept)
	{
		_willAcceptReflectDamageAugment = accept;
	}

	AugmentScrolls _applyAugment = null; // Item Scroll

	public AugmentScrolls getAugmentScroll()
	{
		return _applyAugment;
	}

	public void setAugmentScroll(AugmentScrolls item)
	{
		_applyAugment = item;
	}

	// --------------------
	// Fake Armors
	// --------------------

	private int _fakeArmorObjectId;
	private int _fakeArmorItemId;

	public void setFakeArmorObjectId(int objectId)
	{
		_fakeArmorObjectId = objectId;
	}

	public int getFakeArmorObjectId()
	{
		return _fakeArmorObjectId;
	}

	public void setFakeArmorItemId(int itemId)
	{
		_fakeArmorItemId = itemId;
	}

	public int getFakeArmorItemId()
	{
		return _fakeArmorItemId;
	}

	public void removeFakeArmor()
	{
		if (getFakeArmorObjectId() > 0)
		{
			setFakeArmorObjectId(0);
			setFakeArmorItemId(0);

			broadcastUserInfo();
			sendPacket(new ItemList(this, false));
		}
	}

	// --------------------
	// Fake Weapons
	// --------------------

	private int _fakeWeaponObjectId;
	private int _fakeWeaponItemId;

	public void setFakeWeaponObjectId(int objectId)
	{
		_fakeWeaponObjectId = objectId;
	}

	public int getFakeWeaponObjectId()
	{
		return _fakeWeaponObjectId;
	}

	public void setFakeWeaponItemId(int itemId)
	{
		_fakeWeaponItemId = itemId;
	}

	public int getFakeWeaponItemId()
	{
		return _fakeWeaponItemId;
	}

	public void removeFakeWeapon()
	{
		if (getFakeWeaponObjectId() > 0)
		{
			setFakeWeaponObjectId(0);
			setFakeWeaponItemId(0);

			broadcastUserInfo();
			sendPacket(new ItemList(this, false));
		}
	}

	private boolean _hideWepSkin;
	private boolean _hideSkinHelm;

	public void setHideWepSkin(boolean comm)
	{
		_hideWepSkin = comm;
	}

	public boolean isHideWepSkin()
	{
		return _hideWepSkin;
	}

	public void setHideSkinHelm(boolean comm)
	{
		_hideSkinHelm = comm;
	}

	public boolean isHideSkinHelm()
	{
		return _hideSkinHelm;
	}

	// --------------------
	// Visible Colors
	// --------------------

	private int _visibleNameColor = 0;

	public int getVisibleNameColor()
	{
		if (_visibleNameColor != 0)
			return _visibleNameColor;

		return getAppearance().getNameColor();
	}

	public void setVisibleNameColor(int nameColor)
	{
		_visibleNameColor = nameColor;
	}


	// --------------------
	// Missions Mod
	// --------------------

	public int _tvtCount;

	public int getTvTCont()
	{
		return _tvtCount;
	}

	private boolean _tvtCompleted = false;

	public void setTvTCompleted(boolean value)
	{
		_tvtCompleted = value;
	}

	public boolean isTvTCompleted()
	{
		return _tvtCompleted;
	}

	public int _ctfCount;

	public int getCTFCont()
	{
		return _ctfCount;
	}

	private boolean _ctfCompleted = false;

	public void setCTFCompleted(boolean value)
	{
		_ctfCompleted = value;
	}

	public boolean isCTFCompleted()
	{
		return _ctfCompleted;
	}

	public int _dmCount;

	public int getDMCont()
	{
		return _dmCount;
	}

	private boolean _dmCompleted = false;

	public void setDMCompleted(boolean value)
	{
		_dmCompleted = value;
	}

	public boolean isDMCompleted()
	{
		return _dmCompleted;
	}

	private boolean _ktbCompleted = false;

	public void setKTBCompleted(boolean value)
	{
		_ktbCompleted = value;
	}

	public boolean isKTBCompleted()
	{
		return _ktbCompleted;
	}

	public int _ktbCount;

	public int getKTBCont()
	{
		return _ktbCount;
	}

	private boolean _tournamentCompleted = false;

	public void setTournamentCompleted(boolean value)
	{
		_tournamentCompleted = value;
	}

	public boolean isTournamentCompleted()
	{
		return _tournamentCompleted;
	}

	public int _tournamentCount;

	public int getTournamentCont()
	{
		return _tournamentCount;
	}

	private boolean _1x1Completed = false;

	public void set1x1Completed(boolean value)
	{
		_1x1Completed = value;
	}

	public boolean is1x1Completed()
	{
		return _1x1Completed;
	}

	public int _1x1Count;

	public int get1x1Cont()
	{
		return _1x1Count;
	}

	private boolean _3x3Completed = false;

	public void set3x3Completed(boolean value)
	{
		_3x3Completed = value;
	}

	public boolean is3x3Completed()
	{
		return _3x3Completed;
	}

	public int _3x3Count;

	public int get3x3Cont()
	{
		return _3x3Count;
	}

	private boolean _5x5Completed = false;

	public void set5x5Completed(boolean value)
	{
		_5x5Completed = value;
	}

	public boolean is5x5Completed()
	{
		return _5x5Completed;
	}

	public int _5x5Count;

	public int get5x5Cont()
	{
		return _5x5Count;
	}

	private boolean _9x9Completed = false;

	public void set9x9Completed(boolean value)
	{
		_9x9Completed = value;
	}

	public boolean is9x9Completed()
	{
		return _9x9Completed;
	}

	public int _9x9Count;

	public int get9x9Cont()
	{
		return _9x9Count;
	}

	private boolean _farmCompleted = false;

	public void setFarmCompleted(boolean value)
	{
		_farmCompleted = value;
	}

	public boolean isFarmCompleted()
	{
		return _farmCompleted;
	}

	public int _farmCount;

	public int getFarmCont()
	{
		return _farmCount;
	}

	private boolean _championCompleted = false;

	public void setChampionCompleted(boolean value)
	{
		_championCompleted = value;
	}

	public boolean isChampionCompleted()
	{
		return _championCompleted;
	}

	public int _championCount;

	public int getChampionCont()
	{
		return _championCount;
	}

	private boolean _pvpCompleted = false;

	public void setPVPCompleted(boolean value)
	{
		_pvpCompleted = value;
	}

	public boolean isPVPCompleted()
	{
		return _pvpCompleted;
	}

	public int _pvpCount;

	public int getPVPCont()
	{
		return _pvpCount;
	}

	public int _raidkill_1;
	public int _raidkill_2;
	public int _raidkill_3;
	public int _raidkill_4;
	public int _raidkill_5;
	public int _raidkill_6;

	public int getRaidKill_1()
	{
		return _raidkill_1;
	}

	public int getRaidKill_2()
	{
		return _raidkill_2;
	}

	public int getRaidKill_3()
	{
		return _raidkill_3;
	}

	public int getRaidKill_4()
	{
		return _raidkill_4;
	}

	public int getRaidKill_5()
	{
		return _raidkill_5;
	}

	public int getRaidKill_6()
	{
		return _raidkill_6;
	}

	private boolean _raidkillCompleted = false;

	public void setRaidKillCompleted(final boolean value)
	{
		_raidkillCompleted = value;
	}

	public boolean isRaidKillCompleted()
	{
		return _raidkillCompleted;
	}

	public void setTvTCont(int value)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement stmt = con.prepareStatement("UPDATE characters_mission SET tvt_event=? WHERE obj_Id=?");
			stmt.setInt(1, value);
			stmt.setInt(2, getObjectId());
			stmt.execute();
			stmt.close();
			stmt = null;

			_tvtCount = value;
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "setTvTCont: " + e.getMessage(), e);
		}	
	}

	public void setCTFCont(int value)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement stmt = con.prepareStatement("UPDATE characters_mission SET ctf_event=? WHERE obj_Id=?");
			stmt.setInt(1, value);
			stmt.setInt(2, getObjectId());
			stmt.execute();
			stmt.close();
			stmt = null;

			_ctfCount = value;
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "setCTFCont: " + e.getMessage(), e);
		}	
	}

	public void setDMCont(int value)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement stmt = con.prepareStatement("UPDATE characters_mission SET dm_event=? WHERE obj_Id=?");
			stmt.setInt(1, value);
			stmt.setInt(2, getObjectId());
			stmt.execute();
			stmt.close();
			stmt = null;

			_dmCount = value;
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "setDMCont: " + e.getMessage(), e);
		}	
	}

	public void setKTBCont(int value)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement stmt = con.prepareStatement("UPDATE characters_mission SET ktb_event=? WHERE obj_Id=?");
			stmt.setInt(1, value);
			stmt.setInt(2, getObjectId());
			stmt.execute();
			stmt.close();
			stmt = null;

			_ktbCount = value;
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "setKTBCont: " + e.getMessage(), e);
		}	
	}

	public void setTournamentCont(int value)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement stmt = con.prepareStatement("UPDATE characters_mission SET tournament_event=? WHERE obj_Id=?");
			stmt.setInt(1, value);
			stmt.setInt(2, getObjectId());
			stmt.execute();
			stmt.close();
			stmt = null;

			_tournamentCount = value;
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "setTournamentCont: " + e.getMessage(), e);
		}	
	}

	public void set1x1Cont(int value)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement stmt = con.prepareStatement("UPDATE characters_mission SET 1x1_event=? WHERE obj_Id=?");
			stmt.setInt(1, value);
			stmt.setInt(2, getObjectId());
			stmt.execute();
			stmt.close();
			stmt = null;

			_1x1Count = value;
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "set1x1Cont: " + e.getMessage(), e);
		}	
	}

	public void set3x3Cont(int value)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement stmt = con.prepareStatement("UPDATE characters_mission SET 3x3_event=? WHERE obj_Id=?");
			stmt.setInt(1, value);
			stmt.setInt(2, getObjectId());
			stmt.execute();
			stmt.close();
			stmt = null;

			_3x3Count = value;
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "set3x3Cont: " + e.getMessage(), e);
		}	
	}

	public void set5x5Cont(int value)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement stmt = con.prepareStatement("UPDATE characters_mission SET 5x5_event=? WHERE obj_Id=?");
			stmt.setInt(1, value);
			stmt.setInt(2, getObjectId());
			stmt.execute();
			stmt.close();
			stmt = null;

			_5x5Count = value;
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "set5x5Cont: " + e.getMessage(), e);
		}	
	}

	public void set9x9Cont(int value)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement stmt = con.prepareStatement("UPDATE characters_mission SET 9x9_event=? WHERE obj_Id=?");
			stmt.setInt(1, value);
			stmt.setInt(2, getObjectId());
			stmt.execute();
			stmt.close();
			stmt = null;

			_9x9Count = value;
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "set9x9Cont: " + e.getMessage(), e);
		}	
	}

	public void setFarmCont(int value)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement stmt = con.prepareStatement("UPDATE characters_mission SET farm_event=? WHERE obj_Id=?");
			stmt.setInt(1, value);
			stmt.setInt(2, getObjectId());
			stmt.execute();
			stmt.close();
			stmt = null;

			_farmCount = value;
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "setFarmCont: " + e.getMessage(), e);
		}	
	}

	public void setChampionCont(int value)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement stmt = con.prepareStatement("UPDATE characters_mission SET champion_event=? WHERE obj_Id=?");
			stmt.setInt(1, value);
			stmt.setInt(2, getObjectId());
			stmt.execute();
			stmt.close();
			stmt = null;

			_championCount = value;
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "setChampionCont: " + e.getMessage(), e);
		}	
	}

	public void setPVPCont(int value)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement stmt = con.prepareStatement("UPDATE characters_mission SET pvp_event=? WHERE obj_Id=?");
			stmt.setInt(1, value);
			stmt.setInt(2, getObjectId());
			stmt.execute();
			stmt.close();
			stmt = null;

			_pvpCount = value;
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "setPVPCont: " + e.getMessage(), e);
		}	
	}

	public synchronized boolean checkTvTHWid(String value)
	{
		boolean result = true;
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("SELECT tvt_hwid FROM characters_mission WHERE tvt_hwid=?");
			statement.setString(1, value);
			ResultSet rset = statement.executeQuery();
			result = rset.next();
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "checkTvTHWid: " + e.getMessage(), e);
		}
		return result;
	}

	public synchronized boolean checkCTFHWid(String value)
	{
		boolean result = true;
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("SELECT ctf_hwid FROM characters_mission WHERE ctf_hwid=?");
			statement.setString(1, value);
			ResultSet rset = statement.executeQuery();
			result = rset.next();
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "checkCTFHWid: " + e.getMessage(), e);
		}
		return result;
	}

	public synchronized boolean checkDMHWid(String value)
	{
		boolean result = true;
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("SELECT dm_hwid FROM characters_mission WHERE dm_hwid=?");
			statement.setString(1, value);
			ResultSet rset = statement.executeQuery();
			result = rset.next();
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "checkDMHWid: " + e.getMessage(), e);
		}
		return result;
	}

	public synchronized boolean checkKTBHWid(String value)
	{
		boolean result = true;
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("SELECT ktb_hwid FROM characters_mission WHERE ktb_hwid=?");
			statement.setString(1, value);
			ResultSet rset = statement.executeQuery();
			result = rset.next();
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "checkKTBHWid: " + e.getMessage(), e);
		}
		return result;
	}

	public synchronized boolean checkTournamentHWid(String value)
	{
		boolean result = true;
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("SELECT tournament_hwid FROM characters_mission WHERE tournament_hwid=?");
			statement.setString(1, value);
			ResultSet rset = statement.executeQuery();
			result = rset.next();
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "checkTournamentHWid: " + e.getMessage(), e);
		}
		return result;
	}

	public synchronized boolean check1x1HWid(String value)
	{
		boolean result = true;
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("SELECT 1x1_hwid FROM characters_mission WHERE 1x1_hwid=?");
			statement.setString(1, value);
			ResultSet rset = statement.executeQuery();
			result = rset.next();
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "check1x1HWid: " + e.getMessage(), e);
		}
		return result;
	}

	public synchronized boolean check3x3HWid(String value)
	{
		boolean result = true;
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("SELECT 3x3_hwid FROM characters_mission WHERE 3x3_hwid=?");
			statement.setString(1, value);
			ResultSet rset = statement.executeQuery();
			result = rset.next();
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "check3x3HWid: " + e.getMessage(), e);
		}
		return result;
	}

	public synchronized boolean check5x5HWid(String value)
	{
		boolean result = true;
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("SELECT 5x5_hwid FROM characters_mission WHERE 5x5_hwid=?");
			statement.setString(1, value);
			ResultSet rset = statement.executeQuery();
			result = rset.next();
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "check5x5HWid: " + e.getMessage(), e);
		}
		return result;
	}

	public synchronized boolean check9x9HWid(String value)
	{
		boolean result = true;
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("SELECT 9x9_hwid FROM characters_mission WHERE 9x9_hwid=?");
			statement.setString(1, value);
			ResultSet rset = statement.executeQuery();
			result = rset.next();
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "check9x9HWid: " + e.getMessage(), e);
		}
		return result;
	}

	public synchronized boolean checkFarmHWid(String value)
	{
		boolean result = true;
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("SELECT farm_hwid FROM characters_mission WHERE farm_hwid=?");
			statement.setString(1, value);
			ResultSet rset = statement.executeQuery();
			result = rset.next();
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "checkFarmHWid: " + e.getMessage(), e);
		}
		return result;
	}

	public synchronized boolean checkChampionHWid(String value)
	{
		boolean result = true;
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("SELECT champion_hwid FROM characters_mission WHERE champion_hwid=?");
			statement.setString(1, value);
			ResultSet rset = statement.executeQuery();
			result = rset.next();
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "checkChampionHWid: " + e.getMessage(), e);
		}
		return result;
	}

	public synchronized boolean checkPVPHWid(String value)
	{
		boolean result = true;
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("SELECT pvp_hwid FROM characters_mission WHERE pvp_hwid=?");
			statement.setString(1, value);
			ResultSet rset = statement.executeQuery();
			result = rset.next();
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "checkPVPHWid: " + e.getMessage(), e);
		}
		return result;
	}

	public synchronized boolean checkRaidKillHWid(String value)
	{
		boolean result = true;
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("SELECT raid_kill_hwid FROM characters_mission WHERE raid_kill_hwid=?");
			statement.setString(1, value);
			ResultSet rset = statement.executeQuery();
			result = rset.next();
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "checkRaidKillHWid: " + e.getMessage(), e);
		}
		return result;
	}

	public void setRaidKill_1(int value)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement stmt = con.prepareStatement("UPDATE characters_mission SET raid_1=? WHERE obj_Id=?");
			stmt.setInt(1, value);
			stmt.setInt(2, getObjectId());
			stmt.execute();
			stmt.close();
			stmt = null;

			_raidkill_1 = value;
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "setRaidKill_1: " + e.getMessage(), e);
		}
	}	

	public void setRaidKill_2(int value)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement stmt = con.prepareStatement("UPDATE characters_mission SET raid_2=? WHERE obj_Id=?");
			stmt.setInt(1, value);
			stmt.setInt(2, getObjectId());
			stmt.execute();
			stmt.close();
			stmt = null;

			_raidkill_2 = value;
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "setRaidKill_2: " + e.getMessage(), e);
		}
	}	

	public void setRaidKill_3(int value)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement stmt = con.prepareStatement("UPDATE characters_mission SET raid_3=? WHERE obj_Id=?");
			stmt.setInt(1, value);
			stmt.setInt(2, getObjectId());
			stmt.execute();
			stmt.close();
			stmt = null;

			_raidkill_3 = value;
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "setRaidKill_3: " + e.getMessage(), e);
		}
	}

	public void setRaidKill_4(int value)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement stmt = con.prepareStatement("UPDATE characters_mission SET raid_4=? WHERE obj_Id=?");
			stmt.setInt(1, value);
			stmt.setInt(2, getObjectId());
			stmt.execute();
			stmt.close();
			stmt = null;

			_raidkill_4 = value;
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "setRaidKill_4: " + e.getMessage(), e);
		}
	}	

	public void setRaidKill_5(int value)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement stmt = con.prepareStatement("UPDATE characters_mission SET raid_5=? WHERE obj_Id=?");
			stmt.setInt(1, value);
			stmt.setInt(2, getObjectId());
			stmt.execute();
			stmt.close();
			stmt = null;

			_raidkill_5 = value;
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "setRaidKill_5: " + e.getMessage(), e);
		}
	}	

	public void setRaidKill_6(int value)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement stmt = con.prepareStatement("UPDATE characters_mission SET raid_6=? WHERE obj_Id=?");
			stmt.setInt(1, value);
			stmt.setInt(2, getObjectId());
			stmt.execute();
			stmt.close();
			stmt = null;

			_raidkill_6 = value;
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "setRaidKill_6: " + e.getMessage(), e);
		}
	}	

	public void ReloadMission()
	{
		int tvt_event = 0;
		int tvt_completed = 0;

		int ctf_event = 0;
		int ctf_completed = 0;

		int dm_event = 0;
		int dm_completed = 0;

		int ktb_event = 0;
		int ktb_completed = 0;

		int tournament_event = 0;
		int tournament_completed = 0;

		int x1_event = 0;
		int x1_completed = 0;

		int x3_event = 0;
		int x3_completed = 0;

		int x5_event = 0;
		int x5_completed = 0;

		int x9_event = 0;
		int x9_completed = 0;

		int farm_event = 0;
		int farm_completed = 0;

		int champion_event = 0;
		int champion_completed = 0;

		int pvp_event = 0;
		int pvp_completed = 0;

		int raid_1 = 0;
		int raid_2 = 0;
		int raid_3 = 0;
		int raid_4 = 0;
		int raid_5 = 0;
		int raid_6 = 0;
		int raid_kill_completed= 0;

		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("SELECT tvt_event,tvt_completed,ctf_event,ctf_completed,dm_event,dm_completed,ktb_event,ktb_completed,tournament_event,tournament_completed,1x1_event,1x1_completed,3x3_event,3x3_completed,5x5_event,5x5_completed,9x9_event,9x9_completed,farm_event,farm_completed,champion_event,champion_completed,pvp_event,pvp_completed,raid_1,raid_2,raid_3,raid_4,raid_5,raid_6,raid_kill_completed FROM characters_mission WHERE obj_Id = ?");
			statement.setInt(1, getObjectId());

			ResultSet rset = statement.executeQuery();

			while (rset.next())
			{
				tvt_event = rset.getInt("tvt_event");
				tvt_completed = rset.getInt("tvt_completed");

				ctf_event = rset.getInt("ctf_event");
				ctf_completed = rset.getInt("ctf_completed");

				dm_event = rset.getInt("dm_event");
				dm_completed = rset.getInt("dm_completed");

				ktb_event = rset.getInt("ktb_event");
				ktb_completed = rset.getInt("ktb_completed");

				tournament_event = rset.getInt("tournament_event");
				tournament_completed = rset.getInt("tournament_completed");

				x1_event = rset.getInt("1x1_event");
				x1_completed = rset.getInt("1x1_completed");

				x3_event = rset.getInt("3x3_event");
				x3_completed = rset.getInt("3x3_completed");

				x5_event = rset.getInt("5x5_event");
				x5_completed = rset.getInt("5x5_completed");

				x9_event = rset.getInt("9x9_event");
				x9_completed = rset.getInt("9x9_completed");

				farm_event = rset.getInt("farm_event");
				farm_completed = rset.getInt("farm_completed");

				champion_event = rset.getInt("champion_event");
				champion_completed = rset.getInt("champion_completed");

				pvp_event = rset.getInt("pvp_event");
				pvp_completed = rset.getInt("pvp_completed");

				raid_1 = rset.getInt("raid_1");
				raid_2 = rset.getInt("raid_2");
				raid_3 = rset.getInt("raid_3");
				raid_4 = rset.getInt("raid_4");
				raid_5 = rset.getInt("raid_5");
				raid_6 = rset.getInt("raid_6");
				raid_kill_completed = rset.getInt("raid_kill_completed");
			}
			rset.close();
			statement.close();
			statement = null;
			rset = null;
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "reloadMission: " + e.getMessage(), e);
		}

		_tvtCount = tvt_event;			
		if (tvt_completed >= 1)
			setTvTCompleted(true);

		_ctfCount = ctf_event;			
		if (ctf_completed >= 1)
			setCTFCompleted(true);

		_dmCount = dm_event;			
		if (dm_completed >= 1)
			setDMCompleted(true);

		_ktbCount = ktb_event;			
		if (ktb_completed >= 1)
			setKTBCompleted(true);

		_tournamentCount = tournament_event;			
		if (tournament_completed >= 1)
			setTournamentCompleted(true);

		_1x1Count = x1_event;			
		if (x1_completed >= 1)
			set1x1Completed(true);

		_3x3Count = x3_event;			
		if (x3_completed >= 1)
			set3x3Completed(true);

		_5x5Count = x5_event;			
		if (x5_completed >= 1)
			set5x5Completed(true);

		_9x9Count = x9_event;			
		if (x9_completed >= 1)
			set9x9Completed(true);

		_farmCount = farm_event;			
		if (farm_completed >= 1)
			setFarmCompleted(true);

		_championCount = champion_event;			
		if (champion_completed >= 1)
			setChampionCompleted(true);

		_pvpCount = pvp_event;			
		if (pvp_completed >= 1)
			setPVPCompleted(true);

		_raidkill_1 = raid_1;
		_raidkill_2 = raid_2;
		_raidkill_3 = raid_3;
		_raidkill_4 = raid_4;
		_raidkill_5 = raid_5;
		_raidkill_6 = raid_6;

		if (raid_kill_completed >= 1)
			setRaidKillCompleted(true);
	}

	public synchronized boolean checkMissions(int charid)
	{
		if (this instanceof FakePlayer)
			return false;

		boolean result = true;
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("SELECT obj_Id FROM characters_mission WHERE obj_Id=?");
			statement.setInt(1, charid);
			ResultSet rset = statement.executeQuery();
			result = rset.next();
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "checkMissions: " + e.getMessage(), e);
		}
		return result;
	}

	public void updateMissions()
	{
		if (this instanceof FakePlayer)
			return;

		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement stmt = con.prepareStatement("REPLACE INTO characters_mission (obj_Id,char_name,tvt_event,tvt_completed,tvt_hwid,ctf_event,ctf_completed,ctf_hwid,dm_event,dm_completed,dm_hwid,ktb_event,ktb_completed,ktb_hwid,tournament_event,tournament_completed,tournament_hwid,1x1_event,1x1_completed,1x1_hwid,3x3_event,3x3_completed,3x3_hwid,5x5_event,5x5_completed,5x5_hwid,9x9_event,9x9_completed,9x9_hwid,farm_event,farm_completed,farm_hwid,champion_event,champion_completed,champion_hwid,pvp_event,pvp_completed,pvp_hwid,raid_1,raid_2,raid_3,raid_4,raid_5,raid_6,raid_kill_completed,raid_kill_hwid) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

			stmt.setInt(1, getObjectId());
			stmt.setString(2, getName());
			stmt.setInt(3, getTvTCont());
			stmt.setInt(4, 0);
			stmt.setString(5, " ");
			stmt.setInt(6, getCTFCont());
			stmt.setInt(7, 0);
			stmt.setString(8, " ");
			stmt.setInt(9, getDMCont());
			stmt.setInt(10, 0);
			stmt.setString(11, " ");
			stmt.setInt(12, getKTBCont());
			stmt.setInt(13, 0);
			stmt.setString(14, " ");
			stmt.setInt(15, getTournamentCont());
			stmt.setInt(16, 0);
			stmt.setString(17, " ");
			stmt.setInt(18, get1x1Cont());
			stmt.setInt(19, 0);
			stmt.setString(20, " ");
			stmt.setInt(21, get3x3Cont());
			stmt.setInt(22, 0);
			stmt.setString(23, " ");
			stmt.setInt(24, get5x5Cont());
			stmt.setInt(25, 0);
			stmt.setString(26, " ");
			stmt.setInt(27, get9x9Cont());
			stmt.setInt(28, 0);
			stmt.setString(29, " ");
			stmt.setInt(30, getFarmCont());
			stmt.setInt(31, 0);
			stmt.setString(32, " ");
			stmt.setInt(33, getChampionCont());
			stmt.setInt(34, 0);
			stmt.setString(35, " ");
			stmt.setInt(36, getPVPCont());
			stmt.setInt(37, 0);
			stmt.setString(38, " ");
			stmt.setInt(39, getRaidKill_1());
			stmt.setInt(40, getRaidKill_2());
			stmt.setInt(41, getRaidKill_3());
			stmt.setInt(42, getRaidKill_4());
			stmt.setInt(43, getRaidKill_5());
			stmt.setInt(44, getRaidKill_6());
			stmt.setInt(45, 0);
			stmt.setString(46, " ");	   				
			stmt.execute();
			stmt.close();
			stmt = null;
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "updateMission: " + e.getMessage(), e);
		}
	}

	//----------------------
	//NoCarrier
	//----------------------

	public void scheduleDelete()
	{
		long time = Config.NO_CARRIER_SYSTEM_TIMER;

		setNoCarrier(true);
		setIsInvul(true);
		setIsParalyzed(true);
		getAppearance().setInvisible();
		startAbnormalEffect(0x000080);

		scheduleDelete(time * 1000L);
	}

	public void scheduleDelete(long time)
	{
		broadcastCharInfo();

		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			@Override
			public void run()
			{
				setNoCarrier(false);
				logout();
			}
		}, time);
	}

	//----------------------
	//FOS Event Engine
	//----------------------

	public boolean _FOSRulerSkills = false;

	public Map<Integer,L2Skill> returnSkills()
	{
		return _skills;
	}

	//----------------------
	//Daily Reward
	//----------------------

	private final Map<String, PlayerVar> _vars = new ConcurrentHashMap<>();

	public Map<String, PlayerVar> getVariables()
	{
		return _vars;
	}

	//----------------------
	//Not Used
	//----------------------

	public int _originalTitleColorTournament = 0;
	public String _originalTitleTournament;

	private boolean boughtAugment = false;
	private int augsBought = 0;

	public void setAugsBought(int val)
	{
		augsBought = val;
	}

	public int getAugsBought()
	{
		return augsBought;
	}

	public void setBoughtAugment(boolean val)
	{
		boughtAugment = val;
	}

	public boolean hasBoughtAugment()
	{
		return boughtAugment;
	}

	public boolean isInSameClanOrAlly(L2PcInstance target)
	{
		if (getClan() == null)
			return false;

		if (getAllyId() != 0)
			return false;

		if (target.getClan() == null)
			return false;

		if (target.getAllyId() != 0)
			return false;

		if ((getClan() == target.getClan()) || (getAllyId() == target.getAllyId()))
			return true;

		return false;
	}

	/** The _active_boxes. */
	public int _active_boxes = -1;

	/** The active_boxes_characters. */
	public List<String> active_boxes_characters = new ArrayList<>();

	/**
	 * check if local player can make multibox and also refresh local boxes instances number.
	 * @return true, if successful
	 */
	public boolean checkMultiBox()
	{
		boolean output = true;

		int boxes_number = 0; // this one
		final List<String> active_boxes = new ArrayList<>();

		if (getClient() != null && getClient().getConnection() != null && !getClient().getConnection().isClosed() && getClient().getConnection().getInetAddress() != null)
		{
			final String thisip = getIP();
			if (Config.MULTIBOX_PROTECTION_BYPASS_IPS.contains(thisip))
			{
				_active_boxes = 1;
				active_boxes_characters.clear();
				active_boxes_characters.add(this.getName());
				return true;
			}
			final Collection<L2PcInstance> allPlayers = L2World.getInstance().getL2Players();
			for (final L2PcInstance player : allPlayers)
			{
				if (player != null)
				{
					if (player.isOnline() && !player.isFakeOffline() && player.getClient() != null && player.getClient().getConnection() != null && !player.getClient().getConnection().isClosed() && player.getClient().getConnection().getInetAddress() != null && !player.getName().equals(this.getName()))
					{

						final String ip = player.getIP();
						if (thisip.equals(ip) && this != player)
						{
							if (!Config.ALLOW_DUALBOX)
							{
								output = false;
								break;
							}

							if (boxes_number + 1 > Config.ALLOWED_BOXES)
							{ 
								// actual count+actual player one
								output = false;
								break;
							}
							boxes_number++;
							active_boxes.add(player.getName());
						}
					}
				}
			}
		}

		if (output)
		{
			_active_boxes = boxes_number + 1; // current number of boxes+this one
			if (!active_boxes.contains(this.getName()))
			{
				active_boxes.add(this.getName());

				this.active_boxes_characters = active_boxes;
			}
			refreshOtherBoxes();
		}
		return output;
	}

	/**
	 * increase active boxes number for local player and other boxer for same ip.
	 */
	public void refreshOtherBoxes()
	{
		if (getClient() != null && getClient().getConnection() != null && !getClient().getConnection().isClosed() && getClient().getConnection().getInetAddress() != null)
		{

			final String thisip = getIP();
			if (Config.MULTIBOX_PROTECTION_BYPASS_IPS.contains(thisip))
				return;
			
			final Collection<L2PcInstance> allPlayers = L2World.getInstance().getL2Players();
			final L2PcInstance[] players = allPlayers.toArray(new L2PcInstance[allPlayers.size()]);

			for (final L2PcInstance player : players)
			{
				if (player != null && player.isOnline() && !player.isFakeOffline())
				{
					if (player.getClient() != null && player.getClient().getConnection() != null && !player.getClient().getConnection().isClosed() && !player.getName().equals(this.getName()))
					{

						final String ip = player.getIP();
						if (thisip.equals(ip) && this != player)
						{
							player._active_boxes = _active_boxes;
							player.active_boxes_characters = active_boxes_characters;
						}
					}
				}
			}
		}
	}

	/**
	 * descrease active boxes number for local player and other boxer for same ip.
	 */
	public void decreaseBoxes()
	{
		_active_boxes = _active_boxes - 1;
		active_boxes_characters.remove(this.getName());
		refreshOtherBoxes();
	}

	private final HashMap<Integer, Long> confirmDlgRequests = new HashMap<>();

	public void addConfirmDlgRequestTime(int requestId, int time)
	{
		confirmDlgRequests.put(Integer.valueOf(requestId), Long.valueOf(System.currentTimeMillis() + time + 2000L));
	}

	public Long getConfirmDlgRequestTime(int requestId)
	{
		return confirmDlgRequests.get(Integer.valueOf(requestId));
	}

	public void removeConfirmDlgRequestTime(int requestId)
	{
		confirmDlgRequests.remove(Integer.valueOf(requestId));
	}

	private String _html_save = "";

	public final String getHtmlSave()
	{
		return _html_save;
	}

	public final void setHtmlSave(String value)
	{
		_html_save = value;
	}

	public void sendChatMessage(int objectId, int messageType, String charName, String text)
	{
		sendPacket(new CreatureSay(objectId, messageType, charName, text));
	}

	private int _charId = 199546;

	public int getCharId()
	{
		return _charId;
	}

	public void setCharId(int charId)
	{
		_charId = charId;
	}

	public void rndWalkTvT()
	{
	    int x = getX();
	    int y = getY();
	    int z = getZ();
	    

	    if (Rnd.get(100) > 30)
	    {

	        boolean moveBackwardFirst = Rnd.get(2) == 0;
	        

	        if (moveBackwardFirst)
	        {
	            switch (Rnd.get(1, 5)) 
	            {
	                case 1:
	                    x -= 350;
	                    y += 45;
	                    break;
	                case 2:
	                    x += 100; 
	                    y -= 30;
	                    break;
	                case 3:
	                    x -= 350;
	                    y += 30;
	                    break;
	                case 4:
	                    x -= 100;
	                    y += 25;
	                    break;
	                case 5:
	                    x += 350;
	                    y -= 20;
	                    break;
	            }
	            
	            if (!isTooClose(x, y))
	            {

	                setRunning();
	                getAI().setIntention(CtrlIntention.MOVE_TO, new L2CharPosition(x, y, z, calcHeading(x, y)));
	                
	   
	                long delay = Rnd.get(1000, 3000);
	                ThreadPoolManager.getInstance().scheduleGeneral(() ->
	                {

	                    int newX = getX();
	                    int newY = getY();
	                    int newZ = getZ();
	                    
	                    switch (Rnd.get(1, 4)) // Variacoes de movimentacao para frente
	                    {
	                        case 1:
	                            newX += 250; 
	                            newY -= 50;
	                            break;
	                        case 2:
	                            newX -= 200; 
	                            newY += 50;
	                            break;
	                        case 3:
	                            newX += 150;
	                            newY -= 30;
	                            break;
	                        case 4:
	                            newX += 200;
	                            newY -= 40;
	                            break;
	                    }
	                    

	                    if (!isTooClose(newX, newY))
	                    {
	                        // Move para frente e ataca
	                        setRunning();
	                        getAI().setIntention(CtrlIntention.MOVE_TO, new L2CharPosition(newX, newY, newZ, calcHeading(newX, newY)));
	                    }
	                    
	                }, delay); 
	            }
	        }
	    }
	}

	private boolean isTooClose(int targetX, int targetY)
	{
	    final int MIN_DISTANCE = 500;

	    // Itera sobre todos os fake players no mundo
	    for (FakePlayer otherPlayer : getAllFakePlayers())
	    {
	        if (otherPlayer != this)
	        {
	            int distance = (int) Math.sqrt(Math.pow(otherPlayer.getX() - targetX, 2) + Math.pow(otherPlayer.getY() - targetY, 2));
	            if (distance < MIN_DISTANCE)
	            {
	                return true; 
	            }
	        }
	    }
	    return false;
	}

	private List<FakePlayer> getAllFakePlayers()
	{
	    return new ArrayList<>(); 
	}

	private int _agathionId = 0;

	public void setAgathionId(int npcId)
	{
		_agathionId = npcId;
	}

	public int getAgathionId()
	{
		return _agathionId;
	}

	@Override
	public L2AgathionInstance getAgathion()
	{
		return _agathion;
	}

	public void setAgathion(L2AgathionInstance agathion)
	{
		_agathion = agathion;
	}

	public void unSummonAgathion()
	{
		if (Config.AGATHIONS_STOP_SKILL_ENABLED)
		{
			for (L2Effect effect : getAllEffects())
			{
				if (Config.AGATHIONS_STOP_SKILL_LIST.contains(effect.getSkill().getId()))
					stopSkillEffects(effect.getSkill().getId());
			}
		}
		_agathion.deleteMe();
		setAgathionId(0);
	}

	public synchronized boolean ChatProtection(String hwidban)
	{
		boolean result = true;
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("SELECT hwid FROM ban_hwid_chat WHERE hwid=?");
			statement.setString(1, hwidban);
			ResultSet rset = statement.executeQuery();
			result = rset.next();
			rset.close();
			statement.close();
		}
		catch (SQLException e)
		{
			_log.log(Level.WARNING, "ChatProtection: " + e.getMessage(), e);
		}
		return result;
	}

	public long _ChatBanTime;

	public long getChatBanTimer()
	{
		return _ChatBanTime;
	}

	public void setChatBanTimer(long deleteTimer)
	{
		_ChatBanTime = deleteTimer;
	}

	private boolean _ChatBlock;

	public void setChatBlock(boolean comm)
	{
		_ChatBlock = comm;
	}

	public boolean isChatBlocked()
	{
		return _ChatBlock;
	}

	// --- INÍCIO SISTEMA DE AGATHION INVISÍVEL ---
		private boolean _canSeeAgathions = false;

		public boolean canSeeAgathions() 
		{
			return _canSeeAgathions;
		}

		public void setSeeAgathions(boolean val) 
		{
			_canSeeAgathions = val;
			broadcastUserInfo();
		}
		// --- FIM SISTEMA DE AGATHION INVISÍVEL ---
	
	//--------------
	//Hide Settings
	//---------------

	private boolean _heroAura;

	public void setDisableHeroAura(boolean comm)
	{
		_heroAura = comm;
	}

	public boolean isDisableHeroAura()
	{
		return _heroAura;
	}

	private boolean _enchantGlow;

	public void setDisableGlowWeapon(boolean comm)
	{
		_enchantGlow = comm;
		if (antilagController != null)
			antilagController.set("hideEnchantGlow", Boolean.valueOf(comm));
	}

	public boolean isDisableGlowWeapon()
	{
		return antilagController != null ? antilagController.isHideEnchantGlow() : _enchantGlow;
	}

	private boolean _partyAura = true; // Disabled by default; true means Wide Aura is hidden.

	public void setDisablepartyAura(boolean comm)
	{
		_partyAura = comm;
	}

	public boolean isDisablepartyAura()
	{
		return _partyAura;
	}

	private boolean _hideMsg;

	public void setHideMsg(boolean comm)
	{
		_hideMsg = comm;
	}

	public boolean isHideMsg()
	{
		return _hideMsg;
	}

	private boolean _hideGiran;

	public void setHideGiran(boolean comm)
	{
		_hideGiran = comm;
	}
	
	private boolean _pvpEvent;
	
	public boolean isPvPEvent()
	{
		return _pvpEvent;
	}

	public void setPvPEvent(boolean comm)
	{
		_pvpEvent = comm;
	}

	public boolean isHideGiran()
	{
		return _hideGiran;
	}

	public final boolean isInSameParty(L2PcInstance playable)
	{
		return isInParty() && getParty().containsPlayer(playable.getActingPlayer());
	}

	private AntilagController antilagController;
	public AntilagController getAntilagController()
	{
		return antilagController;
	}
	
	private boolean _timeAvaiable = false;
	private boolean _isInTimeInstance = false;
	private int _timeInstanceMobs = 0;
	
	public boolean TimeInstanceAvaiable()
	{
		return _timeAvaiable;
	}
	
	public void setTimeInstanceAvaiable(boolean b)
	{
		_timeAvaiable = b;
	}
	
	public boolean isInTimeInstance()
	{
		return _isInTimeInstance;
	}
	
	public void setIsInTimeInstance(boolean b)
	{
		_isInTimeInstance = b;
	}
	
	public int getTimeInstanceMobs()
	{
		return _timeInstanceMobs;
	}
	
	public void setTimeInstanceMobs(int i)
	{
		_timeInstanceMobs = i;
	}
	
	public void increaseTimeInstanceMobsCount()
	{
		_timeInstanceMobs += 1;
	}
	private boolean _timeInstanceEnter = false;
	private int _timeInstanceEnterCount = 0;

	public void setTimeInstanceCount(int val)
	{
		_timeInstanceEnterCount = val;
	}

	public int getTimeInstanceCount()
	{
		return _timeInstanceEnterCount;
	}

	public void setInstanceEnter(boolean val)
	{
		_timeInstanceEnter = val;
	}

	public boolean hasInstanceEnter()
	{
		return _timeInstanceEnter;
	}
	
    public void loadTimeInstanceData()
    {
        try (Connection con = L2DatabaseFactory.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT entry_count FROM time_instance_entries WHERE player_id = ?"))
        {
            ps.setInt(1, getObjectId());

            try (ResultSet rs = ps.executeQuery())
            {
            	if (rs.next())
            		_timeInstanceEnterCount = rs.getInt("entry_count");
            }
        }
        catch (Exception e)
        {
            _log.warning("Failed to load Time Instance data for player: " + getObjectId());
        }
    }

    public void updateTimeInstanceData()
    {
        try (Connection con = L2DatabaseFactory.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement("INSERT INTO time_instance_entries (player_id, entry_count) VALUES (?, ?) " +
                             "ON DUPLICATE KEY UPDATE entry_count = ?"))
        {
            ps.setInt(1, getObjectId());
            ps.setInt(2, _timeInstanceEnterCount);
            ps.setInt(3, _timeInstanceEnterCount);
            ps.executeUpdate();
        }
        catch (Exception e)
        {
            _log.warning("Failed to update Time Instance data for player: " + getObjectId());
        }
    }

    public void resetTimeInstanceData()
    {
        _timeInstanceEnterCount = 0;
        updateTimeInstanceData();
    }
    
 // --------------------
 // Active Skin (slot-free)
 // --------------------

 private int _activeSkinId = 0;

 /**
  * Returns the skin ID currently active on this player (0 = no skin).
  * The skin is applied visually without requiring an item to be equipped.
  */
 public int getActiveSkinId()
 {
     return _activeSkinId;
 }

//==========================================
	// SISTEMA DE SPOIL INSTANCE (PERSISTENTE)
	// ==========================================
	private long _spoilCooldown = 0;
	private boolean _instanceSpoil = false;
	private long _spoilTimeLeft = 0;
	private long _lastSpoilStart = 0;

	public long getSpoilCooldown()
	{
		return _spoilCooldown;
	}

	public void setSpoilCooldown(long time)
	{
		_spoilCooldown = time;
	}

	public boolean isInstanceSpoil()
	{
		return _instanceSpoil;
	}

	public void setInstanceSpoil(boolean val)
	{
		_instanceSpoil = val;
	}

	public long getSpoilTimeLeft()
	{
		return Math.max(0, _spoilTimeLeft);
	}

	public void setSpoilTimeLeft(long millis)
	{
		_spoilTimeLeft = millis;
	}

	public long getLastSpoilStart()
	{
		return _lastSpoilStart;
	}

	public void setLastSpoilStart(long time)
	{
		_lastSpoilStart = time;
	}
 
 /**
  * Sets the active skin ID. Use 0 to remove the skin.
  * Call broadcastUserInfo() after setting to update visuals.
  */
 public void setActiveSkinId(int skinId)
 {
     // Remove stats da skin anterior
     if (_activeSkinId > 0)
     {
         final ItemInstance oldSkin = getInventory().getItemByItemId(_activeSkinId);
         if (oldSkin != null)
             removeStatsByOwner(oldSkin); // ja atualiza o cliente automaticamente
     }

     _activeSkinId = skinId;

     // Aplica stats da nova skin
     if (skinId > 0)
     {
         final ItemInstance newSkin = getInventory().getItemByItemId(skinId);
         if (newSkin != null)
             addStatFuncs(newSkin.getStatFuncs(this)); // ja atualiza o cliente automaticamente
     }
 }
 //METODO DO COOLDOWN DAS CERTIFICAÇOES;
 private long _lastCertificationProc = 0;

 public boolean canProcCertification()
 {
     return System.currentTimeMillis() - _lastCertificationProc > 3000; // 3s
 }

 public void setCertificationProc()
 {
     _lastCertificationProc = System.currentTimeMillis();
 }


public void tryAutoEnableShotsByGrade()
{
	final Weapon weapon = getActiveWeaponItem();

	if (weapon == null)
	{
		return;
	}

	final boolean isMagical = weapon.isMagical();
	final CrystalType crystalType = weapon.getCrystalType();

	int shotId = 0;

	switch (crystalType)
	{
		case D:
			shotId = isMagical ? 2510 : 1463;
			break;
		case C:
			shotId = isMagical ? 3949 : 1464;
			break;
		case B:
			shotId = isMagical ? 2512 : 1465;
			break;
		case A:
			shotId = isMagical ? 3951 : 1466;
			break;
		case S:
			shotId = isMagical ? 2514 : 1467;
			break;
		default:
			return;
	}

	final ItemInstance item = getInventory().getItemByItemId(shotId);

	if (item == null)
	{
		return;
	}
	
	addAutoSoulShot(shotId);
	sendPacket(new ExAutoSoulShot(shotId, 1));
	rechargeShots(!isMagical, isMagical);
}
		//METODOS DO AUTO FARM TITLE
		private String _farmOriginalTitle;
		private int _farmOriginalTitleColor;
		
		private String _farmStoredTitle;
		private int _farmStoredTitleColor;
		
		private boolean _farmAppearanceSaved;
		private boolean _farmOriginalWasPlayerMode;
		
		private String _secondaryAutoFarmTitle;
		private int _secondaryAutoFarmTitleColor;
		
		private String _spoilFarmTitle;
		private int _spoilFarmTitleColor;
		
		private void saveFarmAppearance()
		{
		    if (_farmAppearanceSaved)
		        return;
		
		    _farmOriginalTitle = getTitle();
		    _farmOriginalTitleColor = getAppearance().getTitleColor();
		
		    _farmStoredTitle = getPlayerModeStoredTitle();
		    _farmStoredTitleColor = getPlayerModeStoredTitleColor();
		
		    _farmOriginalWasPlayerMode = _playerModeAppearanceSaved;
		    _farmAppearanceSaved = true;
		}
		
		private void refreshFarmAppearance(boolean broadcast)
		{
		    final String title;
		    final int color;
		
		    if (_spoilFarmTitle != null)
		    {
		        title = _spoilFarmTitle;
		        color = _spoilFarmTitleColor;
		    }
		    else if (_secondaryAutoFarmTitle != null)
		    {
		        title = _secondaryAutoFarmTitle;
		        color = _secondaryAutoFarmTitleColor;
		    }
		    else if (_farmAppearanceSaved)
		    {
		        final boolean modeEnded =
		            _farmOriginalWasPlayerMode && !_playerModeAppearanceSaved;
		
		        title = modeEnded ? _farmStoredTitle : _farmOriginalTitle;
		        color = modeEnded ? _farmStoredTitleColor : _farmOriginalTitleColor;
		
		        _farmAppearanceSaved = false;
		        _farmOriginalTitle = null;
		        _farmStoredTitle = null;
		    }
		    else
		    {
		        return;
		    }
		
		    final boolean changed =
		        !java.util.Objects.equals(title, getTitle())
		        || color != getAppearance().getTitleColor();
		
		    setTitle(title);
		    getAppearance().setTitleColor(color);
		
		    if (broadcast && changed)
		    {
		        broadcastUserInfo();
		        broadcastCharInfo();
		    }
		}
		
		public synchronized void applySecondaryAutoFarmAppearance(String title, int color)
		{
		    if (title == null || title.trim().isEmpty())
		        return;
		
		    saveFarmAppearance();
		
		    _secondaryAutoFarmTitle = title.trim();
		    _secondaryAutoFarmTitleColor = color;
		
		    refreshFarmAppearance(false);
		}
		
		public synchronized void restoreSecondaryAutoFarmAppearance()
		{
		    if (_secondaryAutoFarmTitle == null)
		        return;
		
		    _secondaryAutoFarmTitle = null;
		
		    refreshFarmAppearance(false);
		}
		
		public synchronized void applySpoilFarmAppearance(String title, int color)
		{
		    if (title == null || title.trim().isEmpty())
		        return;
		
		    saveFarmAppearance();
		
		    _spoilFarmTitle = title.trim();
		    _spoilFarmTitleColor = color;
		
		    refreshFarmAppearance(true);
		}
		
		public synchronized void restoreSpoilFarmAppearance()
		{
		    if (_spoilFarmTitle == null)
		        return;
		
		    _spoilFarmTitle = null;
		
		    refreshFarmAppearance(true);
		}
		
		public synchronized String getSecondaryAutoFarmStoredTitle()
		{
		    return _farmAppearanceSaved
		        ? _farmStoredTitle
		        : getPlayerModeStoredTitle();
		}
		
		public synchronized int getSecondaryAutoFarmStoredTitleColor()
		{
		    return _farmAppearanceSaved
		        ? _farmStoredTitleColor
		        : getPlayerModeStoredTitleColor();
		}

		
		public void initializePkContour()
		{
			synchronized (_pkContourPacketLock)
			{
				final L2GameClient client = _client;

				if (client == null || client.isDetached())
					return;

				client.sendPacket(
						ExPkContour.visibility(Config.PK_CONTOUR_ENABLED || Config.PVP_CONTOUR_ENABLED));

				client.sendPacket(ExPkContour.visualConfig());
				client.sendPacket(ExPkContour.geometryConfig());

				_pkContourReady = true;
			}

			refreshPkContour();

			for (L2PcInstance target :
				getKnownList().getKnownType(L2PcInstance.class))
			{
				sendRemotePkContour(target);
			}
		}

		public void refreshPkContour()
		{
			synchronized (_pkContourPacketLock)
			{
				final L2GameClient client = _client;

				if (!_pkContourReady || client == null || client.isDetached())
					return;

				client.sendPacket(ExPkContour.local(this));
			}

			// Envia aos observadores sem manter o lock deste jogador.
			for (L2PcInstance viewer :
				getKnownList().getKnownType(L2PcInstance.class))
			{
				if (viewer != this)
					viewer.sendRemotePkContour(this);
			}
		}

		private void sendRemotePkContour(L2PcInstance target)
		{
			if (target == null || target == this)
				return;

			synchronized (_pkContourPacketLock)
			{
				final L2GameClient client = _client;

				if (!_pkContourReady || client == null || client.isDetached())
					return;

				client.sendPacket(
					ExPkContour.remoteTargetLow(target.getObjectId())
				);

				client.sendPacket(
					ExPkContour.remoteTargetHigh(target.getObjectId())
				);

				client.sendPacket(ExPkContour.remote(target));
			}
		}
		
 // Usado pelo PcInventory quando o item sai do inventário
 public void clearActiveSkin(ItemInstance item)
 {
     removeStatsByOwner(item);
     _activeSkinId = 0;
 }

}