package net.sf.l2j.gameserver.model.entity.events.treasurehunt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.StoneMinerInstance;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;

public final class MiningInteractionManager implements Runnable
{
    private static final Logger LOG =
        Logger.getLogger(MiningInteractionManager.class.getName());

    private static final int RANGE = 200;

    private static final long LEASE =
        TimeUnit.MILLISECONDS.toNanos(1500);

    private final Map<Integer, StoneMinerInstance> stones =
        new HashMap<>();

    private final Map<Integer, Attempt> attempts =
        new HashMap<>();

    private final Map<Integer, Offer> offers =
        new HashMap<>();

    private static final class Attempt
    {
        final L2PcInstance player;
        final StoneMinerInstance stone;
        final int request;
        final long started;

        long lastHold;
        long lastProgress;

        int percent = -1;

        Attempt(L2PcInstance p, StoneMinerInstance s, int r)
        {
            player = p;
            stone = s;
            request = r;
            started = lastHold = System.nanoTime();
        }
    }

    private static final class Offer
    {
        final L2PcInstance player;
        final int stoneId;

        long sent;
        long lastStart;

        Offer(L2PcInstance p, int id)
        {
            player = p;
            stoneId = id;
        }
    }

    private MiningInteractionManager()
    {
        ThreadPoolManager.getInstance().scheduleGeneral(this, 250);
    }

    private static final class Holder
    {
        static final MiningInteractionManager INSTANCE =
            new MiningInteractionManager();
    }

    public static MiningInteractionManager getInstance()
    {
        return Holder.INSTANCE;
    }

    public synchronized void register(StoneMinerInstance stone)
    {
        stones.put(stone.getObjectId(), stone);
    }

    public synchronized void unregister(StoneMinerInstance stone)
    {
        if (stones.get(stone.getObjectId()) == stone)
            stones.remove(stone.getObjectId());

        stopStone(stone, false);
    }

    private static boolean online(L2PcInstance player)
    {
        return L2World.getInstance().getPlayer(
            player.getObjectId()
        ) == player;
    }

    private static boolean near(
        L2PcInstance player,
        StoneMinerInstance stone)
    {
        double dx = (double) player.getX() - stone.getX();
        double dy = (double) player.getY() - stone.getY();
        double dz = (double) player.getZ() - stone.getZ();

        return dx * dx + dy * dy + dz * dz
            <= (double) RANGE * RANGE;
    }

    private static boolean usable(
        L2PcInstance player,
        StoneMinerInstance stone)
    {
        return online(player)
            && !player.isAlikeDead()
            && !player.isTeleporting()
            && !player.isMounted()
            && !player.isInBoat()
            && stone.isVisible()
            && !stone.isDead()
            && stone.getTreasureHunt() != null
            && near(player, stone);
    }

    /** Starts the existing server-side session before issuing any attack. */
    private static boolean beginMining(
        L2PcInstance player,
        StoneMinerInstance stone)
    {
        if (!usable(player, stone))
            return false;

        if (player.getTarget() != stone)
            player.setTarget(stone);

        if (player.getTarget() != stone
            || !player.getTreasureHuntSession().begin(stone))
        {
            return false;
        }

        player.getAI().setIntention(CtrlIntention.ATTACK, stone);
        return true;
    }

    private static void send(
        L2PcInstance player,
        String action,
        String data)
    {
        if (online(player))
        {
            player.sendPacket(new ExShowScreenMessage(
                "CustomEvent=MiningV1|" + action + "|" + data,
                1
            ));
        }
    }

    private static String ids(Attempt attempt)
    {
        return "ObjID=" + attempt.stone.getObjectId()
            + " Request=" + attempt.request;
    }

    private static int percent(StoneMinerInstance stone)
    {
        if (stone.getMaxHp() <= 0)
            return 0;

        return Math.max(
            0,
            Math.min(
                99,
                (int) (
                    (1.0 - stone.getCurrentHp() / stone.getMaxHp())
                    * 100.0
                )
            )
        );
    }

    private void finish(Attempt attempt, boolean success)
    {
        if (attempts.get(attempt.player.getObjectId()) != attempt)
            return;

        attempts.remove(attempt.player.getObjectId());

        try
        {
            // Nao interrompe um novo ataque contra outro alvo.
            if (attempt.player.getTarget() == attempt.stone)
            {
                attempt.player.abortAttack();

                attempt.player.getAI().setIntention(
                    CtrlIntention.IDLE
                );
            }

            attempt.player.getTreasureHuntSession()
                .finishIfTarget(attempt.stone);
        }
        finally
        {
            send(
                attempt.player,
                "End",
                ids(attempt)
                    + " Success="
                    + (success ? "1" : "0")
            );
        }
    }

    public synchronized void stopStone(
        StoneMinerInstance stone,
        boolean success)
    {
        for (Attempt attempt : new ArrayList<>(attempts.values()))
        {
            if (attempt.stone == stone)
                finish(attempt, success);
        }
    }

    public synchronized boolean permitsDamage(
        L2PcInstance player,
        StoneMinerInstance stone)
    {
        Attempt attempt = attempts.get(player.getObjectId());

        return attempt != null
            && attempt.stone == stone
            && usable(player, stone)
            && player.getTarget() == stone
            && System.nanoTime() - attempt.lastHold <= LEASE;
    }

    public synchronized boolean handle(
        L2PcInstance player,
        String command)
    {
        if (command == null)
            return false;

        boolean start = command.startsWith("MiningStart ");
        boolean hold = command.startsWith("MiningHold ");
        boolean cancel = command.startsWith("MiningCancel ");

        if (!start && !hold && !cancel)
            return false;

        if (command.length() > 80)
            return true;

        String[] args = command.trim().split("\\s+");

        if (args.length != 3)
            return true;

        final int objectId;
        final int request;

        try
        {
            objectId = Integer.parseInt(args[1]);
            request = Integer.parseInt(args[2]);
        }
        catch (NumberFormatException e)
        {
            return true;
        }

        if (objectId <= 0 || request <= 0)
            return true;

        Attempt current = attempts.get(player.getObjectId());

        if (hold || cancel)
        {
            if (current == null
                || current.stone.getObjectId() != objectId
                || current.request != request)
            {
                return true;
            }

            if (cancel)
            {
                finish(current, false);
            }
            else if (System.nanoTime() - current.lastHold <= LEASE
                && usable(player, current.stone))
            {
                long holdNow = System.nanoTime();

                if (holdNow - current.lastHold
                    >= TimeUnit.MILLISECONDS.toNanos(100))
                {
                    current.lastHold = holdNow;
                }
            }
            else
            {
                finish(current, false);
            }

            return true;
        }

        // Repetir Start nao prolonga a tentativa.
        if (current != null
            && current.stone.getObjectId() == objectId
            && current.request == request)
        {
            return true;
        }

        Offer offer = offers.get(player.getObjectId());
        long now = System.nanoTime();

        if (offer == null
            || offer.player != player
            || offer.stoneId != objectId
            || (offer.lastStart != 0
                && now - offer.lastStart
                    < TimeUnit.MILLISECONDS.toNanos(350)))
        {
            send(
                player,
                "End",
                "ObjID=" + objectId
                    + " Request=" + request
                    + " Success=0"
            );

            return true;
        }

        offer.lastStart = now;

        StoneMinerInstance stone = stones.get(objectId);

        if (stone == null || !usable(player, stone))
        {
            send(
                player,
                "End",
                "ObjID=" + objectId
                    + " Request=" + request
                    + " Success=0"
            );

            return true;
        }

        if (current != null)
            finish(current, false);

        Attempt added = new Attempt(player, stone, request);

        attempts.put(player.getObjectId(), added);

        try
        {
            if (!beginMining(player, stone))
            {
                finish(added, false);
                return true;
            }

            send(
                player,
                "Start",
                ids(added) + " Percent=" + percent(stone)
            );
        }
        catch (Exception e)
        {
            finish(added, false);

            LOG.log(Level.WARNING, "Mining start failed", e);
        }

        return true;
    }

    @Override
    public void run()
    {
        try
        {
            tick();
        }
        catch (Exception e)
        {
            LOG.log(
                Level.WARNING,
                "Mining interaction tick failed",
                e
            );
        }
        finally
        {
            ThreadPoolManager.getInstance()
                .scheduleGeneral(this, 250);
        }
    }

    private synchronized void tick()
    {
        long now = System.nanoTime();

        for (Attempt attempt : new ArrayList<>(attempts.values()))
        {
            if (attempt.stone.isDead())
            {
                finish(attempt, true);
                continue;
            }

            if (!usable(attempt.player, attempt.stone)
                || attempt.player.getTarget() != attempt.stone
                || now - attempt.lastHold > LEASE
                || !attempt.player.getTreasureHuntSession()
                    .isEquippedFor(attempt.stone))
            {
                finish(attempt, false);
                continue;
            }

            int value = percent(attempt.stone);

            if (value != attempt.percent
                || now - attempt.lastProgress
                    >= TimeUnit.SECONDS.toNanos(1))
            {
                attempt.percent = value;
                attempt.lastProgress = now;

                send(
                    attempt.player,
                    "Progress",
                    ids(attempt) + " Percent=" + value
                );
            }
        }

        Map<Integer, L2PcInstance> players = new HashMap<>();

        Map<Integer, StoneMinerInstance> nearest =
            new HashMap<>();

        for (StoneMinerInstance stone
            : new ArrayList<>(stones.values()))
        {
            if (stone.getTreasureHunt() == null
                || !stone.isVisible()
                || stone.isDead())
            {
                unregister(stone);
                continue;
            }

            for (L2PcInstance player
                : stone.getKnownList().getKnownType(
                    L2PcInstance.class))
            {
                if (!usable(player, stone))
                    continue;

                int id = player.getObjectId();

                StoneMinerInstance previous = nearest.get(id);

                if (previous == null
                    || distanceSquared(player, stone)
                        < distanceSquared(player, previous))
                {
                    players.put(id, player);
                    nearest.put(id, stone);
                }
            }
        }

        for (Offer old : new ArrayList<>(offers.values()))
        {
            if (!players.containsKey(old.player.getObjectId()))
            {
                offers.remove(old.player.getObjectId());

                send(old.player, "Offer", "ObjID=0");
            }
        }

        for (Map.Entry<Integer, StoneMinerInstance> entry
            : nearest.entrySet())
        {
            int id = entry.getKey();

            L2PcInstance player = players.get(id);
            Attempt attempt = attempts.get(id);

            int stoneId = attempt == null
                ? entry.getValue().getObjectId()
                : attempt.stone.getObjectId();

            Offer offer = offers.get(id);

            if (offer == null
                || offer.player != player
                || offer.stoneId != stoneId)
            {
                long lastStart =
                    offer == null ? 0 : offer.lastStart;

                offer = new Offer(player, stoneId);
                offer.lastStart = lastStart;

                offers.put(id, offer);
            }

            if (offer.sent == 0
                || now - offer.sent
                    >= TimeUnit.SECONDS.toNanos(1))
            {
                offer.sent = now;

                send(
                    player,
                    "Offer",
                    "ObjID=" + stoneId
                );
            }
        }
    }

    private static double distanceSquared(
        L2PcInstance player,
        StoneMinerInstance stone)
    {
        double dx = (double) player.getX() - stone.getX();
        double dy = (double) player.getY() - stone.getY();
        double dz = (double) player.getZ() - stone.getZ();

        return dx * dx + dy * dy + dz * dz;
    }
}