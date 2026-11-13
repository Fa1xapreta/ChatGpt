package net.sf.l2j.gameserver.model.cloak;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.commons.config.ExProperties;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ExCloakSystem;


public final class CloakSystemManager
{
	private static final Logger LOGGER = Logger.getLogger(CloakSystemManager.class.getName());
	private static final String CONFIG_FILE = "./config/custom/cloak_system.properties";
	private static final int PROTOCOL_VERSION = 1;
	private static final int SERVER_ITEM_FIRST = 31007;
	private static final int SERVER_ITEM_LAST = 31073;

	private static final String[] BODIES =
	{
		"MFighter", "FFighter", "MMagic", "FMagic", "MElf", "FElf", "MDarkElf",
		"FDarkElf", "MDwarf", "FDwarf", "MOrc", "FOrc", "MShaman", "FShaman"
	};

	private static final String[] STATES =
	{
		"Idle", "Walk", "Run", "Sit", "Attack"
	};

	private volatile List<Record> _records = Collections.emptyList();
	private volatile boolean _enabled;
	private volatile boolean _sendClientConfig;

	private CloakSystemManager()
	{
		reload();
	}

	public synchronized void reload()
	{
		final ExProperties props = new ExProperties();
		try
		{
			props.load(new File(CONFIG_FILE));
		}
		catch (Exception e)
		{
			_enabled = false;
			_sendClientConfig = false;
			_records = Collections.emptyList();
			LOGGER.log(Level.WARNING, "Could not load cloak visual configuration: " + CONFIG_FILE, e);
			return;
		}

		_enabled = props.getProperty("EnableCloakSystem", true);
		_sendClientConfig = props.getProperty("SendClientConfig", true);

		final List<Record> parsed = new ArrayList<Record>();
		final Set<String> names = props.stringPropertyNames();
		final List<String> keys = new ArrayList<String>(names);

		Collections.sort(keys, new Comparator<String>()
		{
			@Override
			public int compare(String a, String b)
			{
				final int pa = isIvanCloakKey(a) ? 1 : 0;
				final int pb = isIvanCloakKey(b) ? 1 : 0;
				if (pa != pb)
					return pa < pb ? -1 : 1;

				return a.compareTo(b);
			}
		});

		for (String key : keys)
		{
			if (key == null || key.startsWith("EnableCloakSystem") || key.startsWith("SendClientConfig") || key.startsWith("DefaultVisibility"))
				continue;

			final String value = props.getProperty(key);
			if (value == null || value.trim().isEmpty())
				continue;

			try
			{
				parseProperty(parsed, key.trim(), value.trim());
			}
			catch (Exception e)
			{
				LOGGER.warning("Ignoring invalid cloak property '" + key + "=" + value + "'.");
			}
		}

		_records = Collections.unmodifiableList(parsed);
		LOGGER.info("L2Feel Cloak: loaded " + _records.size() + " visual values (Ivan ids " + SERVER_ITEM_FIRST + ".." + SERVER_ITEM_LAST + ").");
	}

	public int getRecordCount()
	{
		return _records.size();
	}

	public void sendConfig(L2PcInstance player)
	{
		if (player == null)
			return;

		if (!_enabled || !_sendClientConfig)
		{
			player.sendPacket(ExCloakSystem.disable());
			return;
		}

		player.sendPacket(ExCloakSystem.begin(PROTOCOL_VERSION));
		for (Record r : _records)
		{
			player.sendPacket(ExCloakSystem.selector(r.selector));
			player.sendPacket(ExCloakSystem.value(r.value));
		}
		player.sendPacket(ExCloakSystem.commit());
	}

	private static boolean isIvanCloakKey(String key)
	{
		if (key == null)
			return false;

		for (int id = SERVER_ITEM_FIRST; id <= SERVER_ITEM_LAST; id++)
		{
			if (key.contains(Integer.toString(id)))
				return true;
		}
		return false;
	}

	private static void parseProperty(List<Record> out, String key, String value)
	{
		if (key.equalsIgnoreCase("EssenceFitStateBlend"))
		{
			add(out, 7, 15, 127, 0, value);
			return;
		}
		if (key.equalsIgnoreCase("EssenceScale"))
		{
			add(out, 8, 15, 127, 0, value);
			return;
		}
		if (key.equalsIgnoreCase("EssenceBright"))
		{
			add(out, 9, 15, 127, 0, value);
			return;
		}
		if (key.equalsIgnoreCase("EssenceBlend"))
		{
			add(out, 10, 15, 127, 0, value);
			return;
		}
		if (starts(key, "EssenceClothWidthScale"))
		{
			scalar(out, 11, key.substring("EssenceClothWidthScale".length()), value, false);
			return;
		}

		if (starts(key, "EssenceOffset"))
		{
			final Target t = target(key.substring("EssenceOffset".length()), true);
			final String[] xyz = value.split("\\s*,\\s*");
			if (xyz.length != 3)
				throw new IllegalArgumentException();

			add(out, 0, t.body, t.cloak, t.state, xyz[0]);
			add(out, 1, t.body, t.cloak, t.state, xyz[1]);
			add(out, 2, t.body, t.cloak, t.state, xyz[2]);
			return;
		}

		if (starts(key, "EssenceClothSnug"))
		{
			scalar(out, 4, key.substring("EssenceClothSnug".length()), value, true);
			return;
		}
		if (starts(key, "EssenceClothSkin"))
		{
			scalar(out, 5, key.substring("EssenceClothSkin".length()), value, true);
			return;
		}
		if (starts(key, "EssenceSnug"))
		{
			scalar(out, 3, key.substring("EssenceSnug".length()), value, true);
			return;
		}
		if (starts(key, "EssenceWalkAsRun"))
		{
			scalar(out, 6, key.substring("EssenceWalkAsRun".length()), value, false);
		}
	}

	private static void scalar(List<Record> out, int param, String suffix, String value, boolean allowState)
	{
		final Target t = target(suffix, allowState);
		add(out, param, t.body, t.cloak, t.state, value);
	}

	private static Target target(String rawSuffix, boolean allowState)
	{
		String s = rawSuffix == null ? "" : rawSuffix.trim();
		int state = 0;

		if (allowState)
		{
			for (int i = 0; i < STATES.length; i++)
			{
				if (ends(s, STATES[i]))
				{
					state = i + 1;
					s = s.substring(0, s.length() - STATES[i].length());
					break;
				}
			}
		}

		int body = 15;
		for (int i = 0; i < BODIES.length; i++)
		{
			if (starts(s, BODIES[i]))
			{
				body = i;
				s = s.substring(BODIES[i].length());
				break;
			}
		}

		int cloak = 127;
		if (!s.isEmpty())
		{
			final int itemId = Integer.parseInt(s);
			if (itemId >= SERVER_ITEM_FIRST && itemId <= SERVER_ITEM_LAST)
				cloak = itemId - SERVER_ITEM_FIRST;
			else if (itemId >= 9400 && itemId <= 9466)
				cloak = itemId - 9400;
			else
				throw new IllegalArgumentException();
		}

		return new Target(body, cloak, state);
	}

	private static void add(List<Record> out, int param, int body, int cloak, int state, String raw)
	{
		final double value = Double.parseDouble(raw.replace(',', '.'));
		final int selector = (param & 0x3F) | ((state & 0x7) << 6) | ((body & 0xF) << 9) | ((cloak & 0x7F) << 13);
		out.add(new Record(selector, value));
	}

	private static boolean starts(String value, String prefix)
	{
		return value.regionMatches(true, 0, prefix, 0, prefix.length());
	}

	private static boolean ends(String value, String suffix)
	{
		return value.toLowerCase(Locale.ROOT).endsWith(suffix.toLowerCase(Locale.ROOT));
	}

	private static final class Target
	{
		final int body;
		final int cloak;
		final int state;

		Target(int body, int cloak, int state)
		{
			this.body = body;
			this.cloak = cloak;
			this.state = state;
		}
	}

	private static final class Record
	{
		final int selector;
		final double value;

		Record(int selector, double value)
		{
			this.selector = selector;
			this.value = value;
		}
	}

	public static CloakSystemManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		private static final CloakSystemManager INSTANCE = new CloakSystemManager();
	}
}
