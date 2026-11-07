// SPDX-License-Identifier: GPL-3.0-or-later
package net.sf.l2j.gameserver.model.entity.events.treasurehunt;

import java.io.BufferedReader;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Append-only journal: A adds a living stone, D durably retires a broken one. */
public final class TreasureHuntActiveStore implements AutoCloseable
{
    private static final Logger LOG = Logger.getLogger(TreasureHuntActiveStore.class.getName());
    private final Map<String, Record> active = new LinkedHashMap<>();
    private FileChannel channel;
    private FileLock lock;
    private boolean ready;

    public static final class Record
    {
        private final String id;
        private final TreasureHunt definition;
        private final int x, y, z;
        private Record(String id, TreasureHunt definition, int x, int y, int z)
        { this.id = id; this.definition = definition; this.x = x; this.y = y; this.z = z; }
        public String getId() { return id; }
        public TreasureHunt getDefinition() { return definition; }
        public int getX() { return x; }
        public int getY() { return y; }
        public int getZ() { return z; }
    }

    public TreasureHuntActiveStore(Path file, TreasureHuntSpawnHistory history)
    {
        try
        {
            if (!history.isReady()) throw new IllegalStateException("Location history unavailable");
            Path path = file.toAbsolutePath();
            Files.createDirectories(path.getParent());
            if (!Files.exists(path))
            {
                if (history.isActiveStoreInitialized())
                    throw new IllegalStateException("Active stones file is missing after initialization; restore its backup");
                channel = FileChannel.open(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.READ, StandardOpenOption.WRITE);
            }
            else channel = FileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE);
            lock = channel.tryLock();
            if (lock == null) throw new IllegalStateException("Active stones file is locked by another server");
            if (channel.size() > 0)
            {
                ByteBuffer tail = ByteBuffer.allocate(1);
                channel.read(tail, channel.size() - 1);
                if (tail.get(0) != '\n') throw new IllegalArgumentException("Incomplete active-stones journal; restore a consistent backup");
            }
            channel.position(0);
            // Borrow the locked channel, as required by Windows. Owner closes it in close().
            BufferedReader reader = new BufferedReader(Channels.newReader(channel, StandardCharsets.UTF_8.newDecoder(), -1));
            Set<String> seen = new HashSet<>();
            String line;
            while ((line = reader.readLine()) != null)
            {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] fields = line.split("\\s+");
                if (fields.length == 11 && fields[0].equals("A"))
                {
                    String id = UUID.fromString(fields[1]).toString();
                    if (!seen.add(id)) throw new IllegalArgumentException("Repeated active-stone identifier");
                    TreasureHunt definition = new TreasureHunt(Integer.parseInt(fields[2]), fields[3], Integer.parseInt(fields[4]),
                        Integer.parseInt(fields[5]), Integer.parseInt(fields[6]), new String(Base64.getUrlDecoder().decode(fields[7]), StandardCharsets.UTF_8));
                    int x = Integer.parseInt(fields[8]), y = Integer.parseInt(fields[9]), z = Integer.parseInt(fields[10]);
                    if (!insideRegion(definition, x, y, z)) throw new IllegalArgumentException("Stored stone outside its region");
                    active.put(id, new Record(id, definition, x, y, z));
                }
                else if (fields.length == 2 && fields[0].equals("D"))
                {
                    if (active.remove(fields[1]) == null) throw new IllegalArgumentException("Unknown broken-stone identifier");
                }
                else throw new IllegalArgumentException("Invalid active-stones journal record");
            }
            channel.force(true);
            if (!history.initializeActiveStore()) throw new IllegalStateException("Active-stones marker could not be saved");
            ready = true;
        }
        catch (Exception e)
        {
            close();
            LOG.log(Level.SEVERE, "TreasureHunt: active-stones journal unavailable; spawning and restoration blocked.", e);
        }
    }

    public static TreasureHuntActiveStore getInstance() { return Holder.INSTANCE; }
    public synchronized boolean isReady() { return ready; }
    public synchronized List<Record> snapshot() { return new ArrayList<>(active.values()); }

    public synchronized String add(TreasureHunt definition, int x, int y, int z)
    {
        if (!ready || !insideRegion(definition, x, y, z)) return null;
        String id = UUID.randomUUID().toString();
        String name = Base64.getUrlEncoder().withoutPadding().encodeToString(definition.getTerritoryName().getBytes(StandardCharsets.UTF_8));
        try
        {
            append("A " + id + " " + definition.getId() + " " + definition.getRegion() + " " + definition.getNpcId() + " " +
                definition.getWeaponId() + " " + definition.getChance() + " " + name + " " + x + " " + y + " " + z + "\n");
            active.put(id, new Record(id, definition, x, y, z));
            return id;
        }
        catch (Exception e)
        {
            ready = false;
            LOG.log(Level.SEVERE, "TreasureHunt: living stone cannot be persisted; creation blocked.", e);
            return null;
        }
    }

    /** Persist retirement before giving a reward, preventing broken stones from returning. */
    public synchronized boolean remove(String id)
    {
        if (id == null || !active.containsKey(id)) return true;
        if (!ready) return false;
        try
        {
            append("D " + id + "\n");
            active.remove(id);
            return true;
        }
        catch (Exception e)
        {
            ready = false;
            LOG.log(Level.SEVERE, "TreasureHunt: broken stone cannot be saved; reward/death blocked.", e);
            return false;
        }
    }

    private void append(String line) throws java.io.IOException
    {
        ByteBuffer record = ByteBuffer.wrap(line.getBytes(StandardCharsets.UTF_8));
        channel.position(channel.size());
        while (record.hasRemaining()) channel.write(record);
        channel.force(true);
    }

    private static boolean insideRegion(TreasureHunt definition, int x, int y, int z)
    {
        return x >= definition.getMinX() && x < definition.getMinX() + 32768 &&
            y >= definition.getMinY() && y < definition.getMinY() + 32768 && z >= Short.MIN_VALUE && z <= Short.MAX_VALUE;
    }

    @Override public synchronized void close()
    {
        ready = false;
        try { if (lock != null) lock.release(); }
        catch (Exception e) { LOG.log(Level.WARNING, "TreasureHunt active journal lock release failed", e); }
        try { if (channel != null) channel.close(); }
        catch (Exception e) { LOG.log(Level.WARNING, "TreasureHunt active journal close failed", e); }
    }

    private static final class Holder
    {
        private static final TreasureHuntActiveStore INSTANCE = new TreasureHuntActiveStore(
            Paths.get("./data/treasurehunt-active-stones.log"), TreasureHuntSpawnHistory.getInstance());
    }
}
