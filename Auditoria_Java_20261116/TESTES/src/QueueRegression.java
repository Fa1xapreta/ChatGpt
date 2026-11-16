import java.util.*;import java.util.concurrent.*;import java.util.concurrent.locks.*;import java.util.logging.*;
class ReceivablePacket<T> implements Runnable {Runnable work;ReceivablePacket(Runnable r){work=r;}public void run(){work.run();}}
class ActionFailed {static final Object STATIC_PACKET=new Object();}
enum GameClientState {CONNECTED,IN_GAME}
class Config {static final boolean PACKET_HANDLER_DEBUG=false;}
class ThreadPoolManager {static final ThreadPoolManager INSTANCE=new ThreadPoolManager();final ArrayDeque<Runnable> jobs=new ArrayDeque<>();static ThreadPoolManager getInstance(){return INSTANCE;}void executeIOPacket(Runnable r){jobs.add(r);}void executePacket(Runnable r){jobs.add(r);}boolean isShutdown(){return false;}void drain(){int safety=100;while(!jobs.isEmpty() && safety-->0)jobs.remove().run();if(safety<=0)throw new AssertionError("stuck scheduler");}}
class Stats {int processedPackets,longFloods,shortFloods;boolean countFloods(){return false;}boolean countQueueOverflow(){return false;}boolean countBurst(int n){return n>=70;}}
class L2GameClient implements Runnable {
 ArrayBlockingQueue<ReceivablePacket<L2GameClient>> _packetQueue=new ArrayBlockingQueue<>(80);
 final ReentrantLock _queueLock=new ReentrantLock();boolean _isDetached;GameClientState _state=GameClientState.IN_GAME;
 final Stats stats=new Stats();static final Logger _log=Logger.getAnonymousLogger();Stats getStats(){return stats;}void closeNow(){}void sendPacket(Object p){}
public void execute(ReceivablePacket<L2GameClient> packet)
	{
		if (getStats().countFloods())
		{
			_log.severe("Client " + toString() + " - Disconnected, too many floods:" + getStats().longFloods + " long and " + getStats().shortFloods + " short.");
			closeNow();
			return;
		}

		if (!_packetQueue.offer(packet))
		{
			if (getStats().countQueueOverflow())
			{
				_log.severe("Client " + toString() + " - Disconnected, too many queue overflows.");
				closeNow();
			}
			else
				sendPacket(ActionFailed.STATIC_PACKET);

			return;
		}

		if (_queueLock.isLocked()) // already processing
			return;

		try
		{
			if (_state == GameClientState.CONNECTED)
			{
				if (getStats().processedPackets > 3)
				{
					if (Config.PACKET_HANDLER_DEBUG)
						_log.severe("Client " + toString() + " - Disconnected, too many packets in non-authed state.");
					closeNow();
					return;
				}

				ThreadPoolManager.getInstance().executeIOPacket(this);
			}
			else
				ThreadPoolManager.getInstance().executePacket(this);
		}
		catch (RejectedExecutionException e)
		{
			// if the server is shutdown we ignore
			if (!ThreadPoolManager.getInstance().isShutdown())
			{
				_log.severe("Failed executing: " + packet.getClass().getSimpleName() + " for Client: " + toString());
			}
		}
	}
public void run()
	{
		if (!_queueLock.tryLock())
			return;

		try
		{
			int count = 0;
			ReceivablePacket<L2GameClient> packet;
			while (true)
			{
				packet = _packetQueue.poll();
				if (packet == null) // queue is empty
					return;

				if (_isDetached) // clear queue immediately after detach
				{
					_packetQueue.clear();
					return;
				}

				try
				{
					packet.run();
				}
				catch (Exception e)
				{
					_log.severe("Exception during execution " + packet.getClass().getSimpleName() + ", client: " + toString() + "," + e.getMessage());
				}

				count++;
				if (getStats().countBurst(count))
					return;
			}
		}
		finally
		{
			_queueLock.unlock();
		}
	}
}

public class QueueRegression {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 public static void main(String[] args){
  ThreadPoolManager pool=ThreadPoolManager.getInstance();L2GameClient c=new L2GameClient();int[] done={0};
  c.execute(new ReceivablePacket<L2GameClient>(() -> {done[0]++;for(int i=0;i<70;i++)c.execute(new ReceivablePacket<L2GameClient>(() -> done[0]++));}));
  pool.drain();check(done[0]==70 && c._packetQueue.size()==1 && pool.jobs.isEmpty(),"burst reproduction");
  c.execute(new ReceivablePacket<L2GameClient>(() -> done[0]++));pool.drain();check(done[0]==72,"next packet wakes pending work");
  L2GameClient race=new L2GameClient();int[] count={0};
  race._packetQueue=new ArrayBlockingQueue<ReceivablePacket<L2GameClient>>(80){boolean injected;public ReceivablePacket<L2GameClient> poll(){ReceivablePacket<L2GameClient> p=super.poll();if(p==null&&!injected){injected=true;race.execute(new ReceivablePacket<L2GameClient>(() -> count[0]++));}return p;}};
  race.run();check(race._packetQueue.size()==1 && pool.jobs.isEmpty() && count[0]==0,"empty/unlock race reproduction");
  System.out.println("REPRODUCED: burst leaves 1 packet pending without worker; next packet resumes it. Empty-poll/enqueue/unlock race also leaves 1 pending packet. Original execute/run methods, mocked scheduler and stats.");
 }
}
