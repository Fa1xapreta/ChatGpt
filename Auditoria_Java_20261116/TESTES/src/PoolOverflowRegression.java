public class PoolOverflowRegression {static int _threadPoolRandomizer;
private static <T> T getPool(T[] threadPools)
	{
		return threadPools[_threadPoolRandomizer++ % threadPools.length];
	}
 public static void main(String[] a){_threadPoolRandomizer=Integer.MAX_VALUE;Integer[] pools={0,1,2};getPool(pools);boolean threw=false;try{getPool(pools);}catch(ArrayIndexOutOfBoundsException e){threw=true;}if(!threw)throw new AssertionError();System.out.println("REPRODUCED: original pool selection produces a negative array index after int overflow. Scheduling wrappers catch this exception and can return null. No real threads created.");}
}
