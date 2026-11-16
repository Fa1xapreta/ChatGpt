package net.sf.l2j.gameserver.handler.voicedcommandhandlers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.datatables.SkillTreeTable;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.instancemanager.autofarmsecondary.AutofarmSecondaryManager;
import net.sf.l2j.gameserver.model.L2ShortCut;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2SkillLearn;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.zone.ZoneId;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.AutoHuntingState;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage.SMPOS;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.util.StringUtil;

public class VoicedAutoFarmSecondary implements IVoicedCommandHandler
{
    private static final int SKILLS_PER_PAGE = 6;

    private static final String ACTIVED =
        "<font color=00FF00>ON</font>";

    private static final String DESATIVED =
        "<font color=FF0000>OFF</font>";

    private static final String[] SKILL_TYPES =
    {
        "attack",
        "chance",
        "self",
        "heal"
    };

    private final String[] VOICED_COMMANDS =
    {
        "autofarm2",
        "enableAutoFarm2",
        "startAutoFarm2",
        "stopAutoFarm2",
        "changeSkillType2",
        "toggleSkillAutoFarm2",
        "openSkillPickerAutoFarm2",
        "pickSkillAutoFarm2",
        "removeSkillAutoFarm2",
        "pageAutoFarm2",
        "radiusAutoFarm2",
        "chanceAutoFarm2",
        "healAutoFarm2",
        "mpAutoFarm2",
        "switchFarmType2",
        "assistLeaderAutoFarm2",
        "assistMobsAutoFarm2",
        "randomSkillsAutoFarm2",
        "keepLocAutoFarm2",
        "runCloseAutoFarm2",
        "targetModeAutoFarm2",
        "openSkillPickerPageAutoFarm2",
        "enableAntiKs",
        "enableSummonAttack",
        "autoFarmUiStatus2",
        "enableBuffProtect2"
    };

    @Override
    public boolean useVoicedCommand(
        String command,
        L2PcInstance activeChar,
        String args)
    {
        if (activeChar == null || command == null)
            return false;

        final boolean fromUi = command.endsWith(" ui");

        if (fromUi)
            command = command.substring(0, command.length() - 3).trim();

        try
        {
            if ("autoFarmUiStatus2".equals(command))
                return false;

            if ("enableBuffProtect2".equals(command))
            {
                if (!activeChar.isInsideZone(ZoneId.OLYMPIAD))
                {
                    activeChar.setNoBuffProtection(
                        !activeChar.isNoBuffProtected()
                    );
                }
                else
                {
                    activeChar.sendMessage(
                        "Nao e permitido configurar o AutoFarm na Olympiad."
                    );
                }

                if (!fromUi)
                    showAutoFarm(activeChar);

                return false;
            }

            if (command.startsWith("autofarm2"))
            {
                if (!fromUi)
                    showAutoFarm(activeChar);

                return false;
            }

            if ("stopAutoFarm2".equals(command)
                || "startAutoFarm2".equals(command)
                || command.startsWith("enableAutoFarm2"))
            {
                // Explicit OFF stays OFF, even if the same request is repeated.
                if ("stopAutoFarm2".equals(command))
                    AutofarmSecondaryManager.INSTANCE.stopFarm(activeChar);
                else if ("startAutoFarm2".equals(command))
                    AutofarmSecondaryManager.INSTANCE.startFarm(activeChar);
                else if (activeChar.isSecondaryAutoFarm()
                    || AutofarmSecondaryManager.INSTANCE.isAutofarming(activeChar))
                    AutofarmSecondaryManager.INSTANCE.stopFarm(activeChar);
                else
                    AutofarmSecondaryManager.INSTANCE.startFarm(activeChar);

                activeChar.broadcastUserInfo();
                activeChar.broadcastCharInfo();

                if (!fromUi)
                    showAutoFarm(activeChar);

                return false;
            }

            if (activeChar.isInsideZone(ZoneId.OLYMPIAD))
            {
                activeChar.sendMessage(
                    "Command allowed only in Combat areas."
                );
                return false;
            }

            if (command.startsWith("enableAntiKs"))
            {
                activeChar.setAntiKsProtection(
                    !activeChar.isAntiKsProtected()
                );

                if (activeChar.isAntiKsProtected())
                {
                    activeChar.sendPacket(
                        new SystemMessage(
                            SystemMessageId.ACTIVATE_RESPECT_HUNT
                        )
                    );

                    activeChar.sendPacket(
                        new ExShowScreenMessage(
                            "Respect Hunt On",
                            3000,
                            SMPOS.TOP_CENTER,
                            false
                        )
                    );
                }
                else
                {
                    activeChar.sendPacket(
                        new SystemMessage(
                            SystemMessageId.DESACTIVATE_RESPECT_HUNT
                        )
                    );

                    activeChar.sendPacket(
                        new ExShowScreenMessage(
                            "Respect Hunt Off",
                            3000,
                            SMPOS.TOP_CENTER,
                            false
                        )
                    );
                }

                if (!fromUi)
                    showAutoFarm(activeChar);

                return false;
            }

            if (command.startsWith("enableSummonAttack"))
            {
                activeChar.setSummonAttack(
                    !activeChar.isSummonAttack()
                );

                if (!activeChar.isSummonAttack()
                    && activeChar.getPet() != null)
                {
                    activeChar.getPet().abortAttack();
                    activeChar.getPet().setTarget(null);

                    activeChar.getPet().getAI().setIntention(
                        CtrlIntention.ACTIVE
                    );
                }

                if (activeChar.isSummonAttack())
                {
                    activeChar.sendPacket(
                        new SystemMessage(
                            SystemMessageId.ACTIVATE_SUMMON_ACTACK
                        )
                    );

                    activeChar.sendPacket(
                        new ExShowScreenMessage(
                            "Auto Farm Summon Attack On",
                            3000,
                            SMPOS.TOP_CENTER,
                            false
                        )
                    );
                }
                else
                {
                    activeChar.sendPacket(
                        new SystemMessage(
                            SystemMessageId.DESACTIVATE_SUMMON_ACTACK
                        )
                    );

                    activeChar.sendPacket(
                        new ExShowScreenMessage(
                            "Auto Farm Summon Attack Off",
                            3000,
                            SMPOS.TOP_CENTER,
                            false
                        )
                    );
                }

                if (!fromUi)
                    showAutoFarm(activeChar);

                return false;
            }

            if (command.startsWith("changeSkillType2"))
            {
                activeChar.setSecondaryAutoFarmSkillType(
                    nextToken(command, "attack")
                );
            }
            else if (command.startsWith("toggleSkillAutoFarm2"))
            {
                toggleSkill(command, activeChar);
            }
            else if (command.startsWith("openSkillPickerAutoFarm2"))
            {
                openSkillPicker(command, activeChar);
                return false;
            }
            else if (command.startsWith("openSkillPickerPageAutoFarm2"))
            {
                openSkillPickerPage(command, activeChar);
                return false;
            }
            else if (command.startsWith("pickSkillAutoFarm2"))
            {
                pickSkill(command, activeChar);
                return false;
            }
            else if (command.startsWith("removeSkillAutoFarm2"))
            {
                removeSkill(command, activeChar);
            }
            else if (command.startsWith("pageAutoFarm2"))
            {
                changePage(command, activeChar);
            }
            else if (command.startsWith("radiusAutoFarm2"))
            {
                changeRadius(command, activeChar);
            }
            else if (command.startsWith("chanceAutoFarm2"))
            {
                changeChance(command, activeChar);
            }
            else if (command.startsWith("healAutoFarm2"))
            {
                changeHeal(command, activeChar);
            }
            else if (command.startsWith("mpAutoFarm2"))
            {
                changeMp(command, activeChar);
            }
            else if (command.startsWith("switchFarmType2"))
            {
                activeChar.switchSecondaryAutoFarmFarmType();
            }
            else if (command.startsWith("assistLeaderAutoFarm2"))
            {
                activeChar.setSecondaryAutoFarmAssistLeader(
                    !activeChar.isSecondaryAutoFarmAssistLeader()
                );
            }
            else if (command.startsWith("assistMobsAutoFarm2"))
            {
                activeChar.setSecondaryAutoFarmAssistMobs(
                    !activeChar.isSecondaryAutoFarmAssistMobs()
                );
            }
            else if (command.startsWith("randomSkillsAutoFarm2"))
            {
                activeChar.setSecondaryAutoFarmRandomSkills(
                    !activeChar.isSecondaryAutoFarmRandomSkills()
                );
            }
            else if (command.startsWith("keepLocAutoFarm2"))
            {
                activeChar.setSecondaryAutoFarmKeepLocation(
                    !activeChar.isSecondaryAutoFarmKeepLocation()
                );
            }
            else if (command.startsWith("runCloseAutoFarm2"))
            {
                activeChar.setSecondaryAutoFarmRunCloseUp(
                    !activeChar.isSecondaryAutoFarmRunCloseUp()
                );
            }
            else if (command.startsWith("targetModeAutoFarm2"))
            {
                activeChar.setSecondaryAutoFarmTargetMode(
                    nextToken(command, "mobs")
                );
            }

            if (!fromUi)
                showAutoFarm(activeChar);

            return false;
        }
        finally
        {
            activeChar.sendPacket(new AutoHuntingState(activeChar));
        }
    }

    private static void toggleSkill(
        String command,
        L2PcInstance activeChar)
    {
        final StringTokenizer st = new StringTokenizer(command, " ");
        st.nextToken();

        try
        {
            final String type = st.nextToken();
            final int skillId = Integer.parseInt(st.nextToken());

            final List<Integer> skills =
                activeChar.getSecondaryAutoFarmSkills(type);

            if (skills.contains(skillId))
                skills.remove((Integer) skillId);
            else
                addUniqueSkill(activeChar, type, skillId);
        }
        catch (Exception e)
        {
            activeChar.sendMessage("Invalid skill selection.");
        }
    }

    private static void openSkillPicker(
        String command,
        L2PcInstance activeChar)
    {
        final StringTokenizer st = new StringTokenizer(command, " ");
        st.nextToken();

        final String type = st.hasMoreTokens()
            ? st.nextToken()
            : activeChar.getSecondaryAutoFarmSkillType();

        activeChar.setSecondaryAutoFarmSkillType(type);

        if (st.hasMoreTokens())
        {
            final String pageAction = st.nextToken();

            if (pageAction.startsWith("inc"))
            {
                activeChar.setSecondaryAutoFarmSkillPage(
                    activeChar.getSecondaryAutoFarmSkillPage() + 1
                );
            }
            else if (pageAction.startsWith("dec"))
            {
                activeChar.setSecondaryAutoFarmSkillPage(
                    activeChar.getSecondaryAutoFarmSkillPage() - 1
                );
            }
        }

        showSkillPicker(activeChar);
    }

    private static void openSkillPickerPage(
        String command,
        L2PcInstance activeChar)
    {
        final StringTokenizer st = new StringTokenizer(command, " ");
        st.nextToken();

        final String type = st.hasMoreTokens()
            ? st.nextToken()
            : activeChar.getSecondaryAutoFarmSkillType();

        activeChar.setSecondaryAutoFarmSkillType(type);

        try
        {
            final int page = st.hasMoreTokens()
                ? Integer.parseInt(st.nextToken())
                : activeChar.getSecondaryAutoFarmSkillPage();

            activeChar.setSecondaryAutoFarmSkillPage(page);
        }
        catch (Exception e)
        {
            activeChar.setSecondaryAutoFarmSkillPage(0);
        }

        showSkillPicker(activeChar);
    }

    private static void pickSkill(
        String command,
        L2PcInstance activeChar)
    {
        final StringTokenizer st = new StringTokenizer(command, " ");
        st.nextToken();

        try
        {
            final String type = st.nextToken();
            final int skillId = Integer.parseInt(st.nextToken());

            final List<Integer> skills =
                activeChar.getSecondaryAutoFarmSkills(type);

            if (skills.contains(skillId))
                skills.remove((Integer) skillId);
            else
                addUniqueSkill(activeChar, type, skillId);

            activeChar.setSecondaryAutoFarmSkillType(type);
            showAutoFarm(activeChar);
        }
        catch (Exception e)
        {
            activeChar.sendMessage("Invalid skill selection.");
            showAutoFarm(activeChar);
        }
    }

    private static void addUniqueSkill(
        L2PcInstance activeChar,
        String type,
        int skillId)
    {
        removeSkillFromAllTypes(activeChar, skillId);

        final List<Integer> skills =
            activeChar.getSecondaryAutoFarmSkills(type);

        if (skills.size() >= 2)
            skills.remove(0);

        skills.add(skillId);
    }

    private static void removeSkillFromAllTypes(
        L2PcInstance activeChar,
        int skillId)
    {
        for (String type : SKILL_TYPES)
        {
            activeChar.getSecondaryAutoFarmSkills(type)
                .remove((Integer) skillId);
        }
    }

    private static void removeSkill(
        String command,
        L2PcInstance activeChar)
    {
        final StringTokenizer st = new StringTokenizer(command, " ");
        st.nextToken();

        try
        {
            final String type = st.nextToken();
            final int skillId = Integer.parseInt(st.nextToken());

            activeChar.getSecondaryAutoFarmSkills(type)
                .remove((Integer) skillId);

            activeChar.setSecondaryAutoFarmSkillType(type);
        }
        catch (Exception e)
        {
            activeChar.sendMessage("Invalid skill selection.");
        }
    }

    private static void changePage(
        String command,
        L2PcInstance activeChar)
    {
        final String param = nextToken(command, "");

        if (param.startsWith("inc"))
        {
            activeChar.setSecondaryAutoFarmSkillPage(
                activeChar.getSecondaryAutoFarmSkillPage() + 1
            );
        }
        else if (param.startsWith("dec"))
        {
            activeChar.setSecondaryAutoFarmSkillPage(
                activeChar.getSecondaryAutoFarmSkillPage() - 1
            );
        }
    }

    private static void changeRadius(
        String command,
        L2PcInstance activeChar)
    {
        final String param = nextToken(command, "");

        if (param.startsWith("inc"))
        {
            activeChar.setSecondaryAutoFarmRadius(
                activeChar.getSecondaryAutoFarmRadius() + 100
            );
        }
        else if (param.startsWith("dec"))
        {
            activeChar.setSecondaryAutoFarmRadius(
                activeChar.getSecondaryAutoFarmRadius() - 100
            );
        }
    }

    private static void changeChance(
        String command,
        L2PcInstance activeChar)
    {
        final String param = nextToken(command, "");

        if (param.startsWith("inc"))
        {
            activeChar.setSecondaryAutoFarmChancePercent(
                activeChar.getSecondaryAutoFarmChancePercent() + 10
            );
        }
        else if (param.startsWith("dec"))
        {
            activeChar.setSecondaryAutoFarmChancePercent(
                activeChar.getSecondaryAutoFarmChancePercent() - 10
            );
        }
    }

    private static void changeHeal(
        String command,
        L2PcInstance activeChar)
    {
        final String param = nextToken(command, "");

        if (param.startsWith("inc"))
        {
            activeChar.setSecondaryAutoFarmHealPercent(
                activeChar.getSecondaryAutoFarmHealPercent() + 10
            );
        }
        else if (param.startsWith("dec"))
        {
            activeChar.setSecondaryAutoFarmHealPercent(
                activeChar.getSecondaryAutoFarmHealPercent() - 10
            );
        }
    }

    private static void changeMp(
        String command,
        L2PcInstance activeChar)
    {
        final String param = nextToken(command, "");

        if (param.startsWith("inc"))
        {
            activeChar.setSecondaryAutoFarmMpPercent(
                activeChar.getSecondaryAutoFarmMpPercent() + 10
            );
        }
        else if (param.startsWith("dec"))
        {
            activeChar.setSecondaryAutoFarmMpPercent(
                activeChar.getSecondaryAutoFarmMpPercent() - 10
            );
        }
    }

    private static String nextToken(
        String command,
        String defaultValue)
    {
        final StringTokenizer st = new StringTokenizer(command, " ");
        st.nextToken();

        return st.hasMoreTokens() ? st.nextToken() : defaultValue;
    }

    public static void showAutoFarm(L2PcInstance activeChar)
    {
        activeChar.sendPacket(new AutoHuntingState(activeChar));

        final NpcHtmlMessage html = new NpcHtmlMessage(0);

        html.setFile("data/html/mods/menu/AutoFarmSecondary.htm");

        // Keep the existing HTML template, but make its action idempotent.
        html.replace(
            "enableAutoFarm2",
            activeChar.isSecondaryAutoFarm() ? "stopAutoFarm2" : "startAutoFarm2"
        );

        html.replace(
            "%activate%",
            "<font color=LEVEL>Auto Farm</font>"
        );

        html.replace(
            "%status%",
            activeChar.isSecondaryAutoFarm() ? ACTIVED : DESATIVED
        );

        html.replace(
            "%button%",
            activeChar.isSecondaryAutoFarm() ? "OFF" : "ON"
        );

        html.replace(
            "%farmType%",
            activeChar.getSecondaryAutoFarmFarmType()
        );

        html.replace(
            "%targetMode%",
            targetModeLabel(activeChar.getSecondaryAutoFarmTargetMode())
        );

        html.replace(
            "%farmIcon%",
            "Fighter".equals(activeChar.getSecondaryAutoFarmFarmType())
                ? "icon.skill0413"
                : "icon.skill1342"
        );

        html.replace(
            "%selectedType%",
            activeChar.getSecondaryAutoFarmSkillType().toUpperCase()
        );

        html.replace("%skillSlots%", buildSkillSlots(activeChar));

        html.replace(
            "%page%",
            StringUtil.formatNumber(
                activeChar.getSecondaryAutoFarmSkillPage() + 1
            )
        );

        html.replace(
            "%chance%",
            StringUtil.formatNumber(
                activeChar.getSecondaryAutoFarmChancePercent()
            )
        );

        html.replace(
            "%heal%",
            StringUtil.formatNumber(
                activeChar.getSecondaryAutoFarmHealPercent()
            )
        );

        html.replace(
            "%mp%",
            StringUtil.formatNumber(
                activeChar.getSecondaryAutoFarmMpPercent()
            )
        );

        html.replace(
            "%distance%",
            StringUtil.formatNumber(
                activeChar.getSecondaryAutoFarmRadius()
            )
        );

        html.replace(
            "%assistLeader%",
            checkbox(activeChar.isSecondaryAutoFarmAssistLeader())
        );

        html.replace(
            "%assistMobs%",
            checkbox(activeChar.isSecondaryAutoFarmAssistMobs())
        );

        html.replace(
            "%randomSkills%",
            checkbox(activeChar.isSecondaryAutoFarmRandomSkills())
        );

        html.replace(
            "%keepLoc%",
            checkbox(activeChar.isSecondaryAutoFarmKeepLocation())
        );

        html.replace(
            "%runClose%",
            checkbox(activeChar.isSecondaryAutoFarmRunCloseUp())
        );

        activeChar.sendPacket(html);
    }

    public static void showSkillPicker(L2PcInstance activeChar)
    {
        final NpcHtmlMessage html = new NpcHtmlMessage(0);

        html.setFile(
            "data/html/mods/menu/AutoFarmSecondarySkills.htm"
        );

        html.replace(
            "%selectedType%",
            activeChar.getSecondaryAutoFarmSkillType().toUpperCase()
        );

        html.replace("%page%", skillPickerPageLabel(activeChar));
        html.replace("%skillPickerNav%", buildSkillPickerNav(activeChar));
        html.replace("%skillPicker%", buildSkillPicker(activeChar));

        activeChar.sendPacket(html);
    }

    private static String buildSkillSlots(L2PcInstance activeChar)
    {
        final StringBuilder sb = new StringBuilder();

        sb.append("<tr>");

        appendSkillSlots(sb, activeChar, "attack", 2);
        appendSkillSlots(sb, activeChar, "chance", 2);
        appendSkillSlots(sb, activeChar, "self", 2);
        appendSkillSlots(sb, activeChar, "heal", 2);

        sb.append("</tr>");

        return sb.toString();
    }

    private static void appendSkillSlots(
        StringBuilder sb,
        L2PcInstance activeChar,
        String type,
        int slots)
    {
        final List<Integer> selectedSkills =
            activeChar.getSecondaryAutoFarmSkills(type);

        for (int i = 0; i < slots; i++)
        {
            if (i < selectedSkills.size())
            {
                final int skillId = selectedSkills.get(i);

                StringUtil.append(
                    sb,
                    "<td width=36 align=center><button action=\"bypass -h voiced_removeSkillAutoFarm2 ",
                    type,
                    " ",
                    skillId,
                    "\" value=\"\" width=32 height=32 back=\"",
                    skillIcon(skillId),
                    "\" fore=\"",
                    skillIcon(skillId),
                    "\"></td>"
                );
            }
            else
            {
                StringUtil.append(
                    sb,
                    "<td width=36 align=center><button action=\"bypass -h voiced_openSkillPickerAutoFarm2 ",
                    type,
                    "\" value=\"+\" width=32 height=32 back=\"L2UI_CH3.multisell_plusicon\" fore=\"L2UI_CH3.multisell_plusicon\"></td>"
                );
            }
        }
    }

    private static String buildSkillPickerNav(L2PcInstance activeChar)
    {
        final String type =
            activeChar.getSecondaryAutoFarmSkillType();

        final int totalPages = getSkillPickerTotalPages(activeChar);

        final int page = Math.max(
            0,
            Math.min(
                activeChar.getSecondaryAutoFarmSkillPage(),
                totalPages - 1
            )
        );

        final int prevPage = Math.max(0, page - 1);
        final int nextPage = Math.min(totalPages - 1, page + 1);

        final StringBuilder sb = new StringBuilder();

        StringUtil.append(
            sb,
            "<table width=298 bgcolor=1F1818><tr><td width=70><button value=\"Prev\" action=\"bypass -h voiced_openSkillPickerPageAutoFarm2 ",
            type,
            " ",
            String.valueOf(prevPage),
            "\" width=60 height=20 back=\"L2UI_CH3.bigbutton2_down\" fore=\"L2UI_CH3.bigbutton2\"></td>"
        );

        StringUtil.append(
            sb,
            "<td width=158 align=center><button value=\"Back\" action=\"bypass -h voiced_autofarm2\" width=70 height=20 back=\"L2UI_CH3.bigbutton2_down\" fore=\"L2UI_CH3.bigbutton2\"></td>"
        );

        StringUtil.append(
            sb,
            "<td width=70 align=right><button value=\"Next\" action=\"bypass -h voiced_openSkillPickerPageAutoFarm2 ",
            type,
            " ",
            String.valueOf(nextPage),
            "\" width=60 height=20 back=\"L2UI_CH3.bigbutton2_down\" fore=\"L2UI_CH3.bigbutton2\"></td></tr></table>"
        );

        return sb.toString();
    }

    private static String skillPickerPageLabel(L2PcInstance activeChar)
    {
        final int totalPages = getSkillPickerTotalPages(activeChar);

        final int page = Math.max(
            0,
            Math.min(
                activeChar.getSecondaryAutoFarmSkillPage(),
                totalPages - 1
            )
        );

        if (page != activeChar.getSecondaryAutoFarmSkillPage())
            activeChar.setSecondaryAutoFarmSkillPage(page);

        return StringUtil.formatNumber(page + 1)
            + "/"
            + StringUtil.formatNumber(totalPages);
    }

    private static int getSkillPickerTotalPages(
        L2PcInstance activeChar)
    {
        final int totalSkills = collectSkillPickerSkills(activeChar).size();

        return Math.max(
            1,
            (totalSkills + SKILLS_PER_PAGE - 1) / SKILLS_PER_PAGE
        );
    }

    private static List<L2Skill> collectSkillPickerSkills(
        L2PcInstance activeChar)
    {
        final Map<Integer, L2Skill> skillMap = new LinkedHashMap<>();

        for (L2Skill skill : activeChar.getAllSkills())
        {
            if (skill == null || skill.isPassive())
                continue;

            skillMap.put(skill.getId(), skill);
        }

        final Collection<L2SkillLearn> classSkills =
            SkillTreeTable.getInstance().getAllowedSkills(
                activeChar.getClassId()
            );

        if (classSkills != null)
        {
            for (L2SkillLearn skillLearn : classSkills)
            {
                if (skillLearn == null
                    || skillLearn.getMinLevel() > activeChar.getLevel())
                {
                    continue;
                }

                final L2Skill skill = SkillTable.getInstance().getInfo(
                    skillLearn.getId(),
                    skillLearn.getLevel()
                );

                if (skill == null || skill.isPassive())
                    continue;

                final L2Skill current = skillMap.get(skill.getId());

                if (current == null
                    || skill.getLevel() > current.getLevel())
                {
                    skillMap.put(skill.getId(), skill);
                }
            }
        }

        for (L2ShortCut shortcut : activeChar.getAllShortCuts())
        {
            if (shortcut == null
                || shortcut.getType() != L2ShortCut.TYPE_SKILL)
            {
                continue;
            }

            L2Skill skill = activeChar.getKnownSkill(shortcut.getId());

            if (skill == null)
            {
                final int level = shortcut.getLevel() > 0
                    ? shortcut.getLevel()
                    : activeChar.getSkillLevel(shortcut.getId());

                if (level > 0)
                {
                    skill = SkillTable.getInstance().getInfo(
                        shortcut.getId(),
                        level
                    );
                }
            }

            if (skill != null && !skill.isPassive())
                skillMap.put(skill.getId(), skill);
        }

        final List<L2Skill> skills =
            new ArrayList<>(skillMap.values());

        skills.sort(Comparator.comparing(L2Skill::getName));

        return skills;
    }

    private static String buildSkillPicker(L2PcInstance activeChar)
    {
        final String type =
            activeChar.getSecondaryAutoFarmSkillType();

        final List<L2Skill> skills =
            collectSkillPickerSkills(activeChar);

        final int page =
            activeChar.getSecondaryAutoFarmSkillPage();

        final int start = page * SKILLS_PER_PAGE;

        if (start >= skills.size() && page > 0)
        {
            activeChar.setSecondaryAutoFarmSkillPage(page - 1);
            return buildSkillPicker(activeChar);
        }

        final int end = Math.min(
            start + SKILLS_PER_PAGE,
            skills.size()
        );

        final StringBuilder sb = new StringBuilder();

        if (skills.isEmpty())
        {
            sb.append(
                "<tr><td width=298 align=center>No active skills found.</td></tr>"
            );

            return sb.toString();
        }

        for (int i = start; i < end; i++)
        {
            final L2Skill skill = skills.get(i);

            final boolean selected =
                activeChar.getSecondaryAutoFarmSkills(type)
                    .contains(skill.getId());

            StringUtil.append(
                sb,
                "<tr><td width=6 height=34></td><td width=40 height=34 align=center><img src=\"",
                skillIcon(skill.getId()),
                "\" width=32 height=32></td><td width=6 height=34></td><td width=155 height=34>",
                escape(skill.getName()),
                "</td><td width=88 height=34 align=center><button value=\"",
                selected ? "Remove" : "Select",
                "\" action=\"bypass -h voiced_",
                selected ? "removeSkillAutoFarm2" : "pickSkillAutoFarm2",
                " ",
                type,
                " ",
                String.valueOf(skill.getId()),
                "\" width=76 height=20 back=\"L2UI_CH3.bigbutton2_down\" fore=\"L2UI_CH3.bigbutton2\"></td></tr>"
            );
        }

        return sb.toString();
    }

    private static String skillIcon(int skillId)
    {
        if (skillId == 1320)
            return "icon.skill1320";

        if (skillId == 1321)
            return "icon.skill0172";

        if (skillId == 1322)
            return "icon.skill1320";

        if (skillId == 4699 || skillId == 4700)
            return "icon.skill1331";

        if (skillId == 4702 || skillId == 4703)
            return "icon.skill1332";

        if (skillId >= 4551 && skillId <= 4554)
            return "icon.skill1164";

        if (skillId < 10000)
            return String.format("icon.skill%04d", skillId);

        return "icon.skill" + skillId;
    }

    private static String targetModeLabel(String mode)
    {
        if ("pvp".equalsIgnoreCase(mode))
            return "PvP";

        if ("both".equalsIgnoreCase(mode))
            return "Both";

        return "Mobs";
    }

    private static String checkbox(boolean enabled)
    {
        return enabled
            ? "back=L2UI.CheckBox_checked fore=L2UI.CheckBox_checked"
            : "back=L2UI.CheckBox fore=L2UI.CheckBox";
    }

    private static String escape(String text)
    {
        if (text == null)
            return "";

        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }

    @Override
    public String[] getVoicedCommandList()
    {
        return VOICED_COMMANDS;
    }
}