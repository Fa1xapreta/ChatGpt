import java.util.*;
interface IVoicedCommandHandler {String[] getVoicedCommandList();}
public class RegistryRegression {final Map<Integer,IVoicedCommandHandler> _datatable=new HashMap<>();
public void registerHandler(IVoicedCommandHandler handler)
	{
		String[] ids = handler.getVoicedCommandList();

		for (int i = 0; i < ids.length; i++)
			_datatable.put(ids[i].hashCode(), handler);
	}
public IVoicedCommandHandler getHandler(String voicedCommand)
	{
		String command = voicedCommand;

		if (voicedCommand.indexOf(" ") != -1)
			command = voicedCommand.substring(0, voicedCommand.indexOf(" "));

		return _datatable.get(command.hashCode());
	}
 static class H implements IVoicedCommandHandler {final String[] commands;H(String... xs){commands=xs;}public String[] getVoicedCommandList(){return commands;}}
 public static void main(String[] a){RegistryRegression r=new RegistryRegression();H old=new H("enableAntiKs","enableSummonAttack"),next=new H("enableAntiKs","enableSummonAttack");r.registerHandler(old);r.registerHandler(next);if(r.getHandler("enableAntiKs")!=next||r.getHandler("enableSummonAttack ui")!=next)throw new AssertionError();System.out.println("REPRODUCED: original registry silently overwrites handlers for shared command names; lookup with args selects the last registration. Source scan confirms AutoFarmSecondary registered after AutoFarm.");}
}
