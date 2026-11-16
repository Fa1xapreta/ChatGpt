import java.util.concurrent.atomic.AtomicInteger;import java.util.logging.*;
class GameTimeController {static int ticks;static final int MILLIS_IN_TICK=100;static final GameTimeController I=new GameTimeController();static GameTimeController getInstance(){return I;}int getGameTicks(){return ticks;}}
class FloodConfig {int FLOOD_PROTECTION_INTERVAL=5,PUNISHMENT_LIMIT,PUNISHMENT_TIME;boolean LOG_FLOODING;String PUNISHMENT_TYPE;}
class FloodGate {static final Logger _log=Logger.getAnonymousLogger();final FloodConfig _config=new FloodConfig();int _nextGameTick;boolean _punishmentInProgress,_logged;final AtomicInteger _count=new AtomicInteger();void log(String...s){}void kickPlayer(){}void banAccount(){}void jailChar(){}
public boolean tryPerformAction(final String command)
	{
		final int curTick = GameTimeController.getInstance().getGameTicks();

		if (curTick < _nextGameTick || _punishmentInProgress)
		{
			if (_config.LOG_FLOODING && !_logged && _log.isLoggable(Level.WARNING))
			{
				log(" called command ", command, " ~", String.valueOf((_config.FLOOD_PROTECTION_INTERVAL - (_nextGameTick - curTick)) * GameTimeController.MILLIS_IN_TICK), " ms after previous command");
				_logged = true;
			}

			_count.incrementAndGet();

			if (!_punishmentInProgress && _config.PUNISHMENT_LIMIT > 0 && _count.get() >= _config.PUNISHMENT_LIMIT && _config.PUNISHMENT_TYPE != null)
			{
				_punishmentInProgress = true;

				if ("kick".equals(_config.PUNISHMENT_TYPE))
					kickPlayer();
				else if ("ban".equals(_config.PUNISHMENT_TYPE))
					banAccount();
				else if ("jail".equals(_config.PUNISHMENT_TYPE))
					jailChar();

				_punishmentInProgress = false;
			}

			return false;
		}

		if (_count.get() > 0)
		{
			if (_config.LOG_FLOODING && _log.isLoggable(Level.WARNING))
				log(" issued ", String.valueOf(_count), " extra requests within ~", String.valueOf(_config.FLOOD_PROTECTION_INTERVAL * GameTimeController.MILLIS_IN_TICK), " ms");
		}

		_nextGameTick = curTick + _config.FLOOD_PROTECTION_INTERVAL;
		_logged = false;
		_count.set(0);

		return true;
	}
}
public class FloodRegression {public static void main(String[] a){FloodGate g=new FloodGate();GameTimeController.ticks=100;if(!g.tryPerformAction("NpcHeaderInfo 123"))throw new AssertionError();GameTimeController.ticks=101;if(g.tryPerformAction("voiced_stopAutoFarm2 ui"))throw new AssertionError("query did not block click");GameTimeController.ticks=104;if(g.tryPerformAction("npc_123_Chat 1"))throw new AssertionError();GameTimeController.ticks=105;if(!g.tryPerformAction("npc_123_Chat 1"))throw new AssertionError();System.out.println("REPRODUCED: background query at tick 100 blocks distinct manual actions at 101 and 104; tick 105 succeeds. Production method, default interval 5 ticks, mocked clock/config.");}}
