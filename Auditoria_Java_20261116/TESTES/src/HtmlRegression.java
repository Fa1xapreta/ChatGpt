import java.util.*;
class L2PcInstance {final List<String> _validBypass=new ArrayList<>(),_validBypass2=new ArrayList<>();boolean isGM(){return false;}void sendMessage(String s){}void setHtmlSave(String s){}
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
public synchronized void clearBypass()
	{
		_validBypass.clear();
		_validBypass2.clear();
	}
}
class HtmlClient {L2PcInstance p;HtmlClient(L2PcInstance x){p=x;}L2PcInstance getActiveChar(){return p;}}
class NpcHtmlMessage {boolean _validate=true;String _file,_html;HtmlClient c;NpcHtmlMessage(HtmlClient client,String html){c=client;_html=html;}HtmlClient getClient(){return c;}
public void runImpl()
	{
		if (!_validate)
			return;

		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;

		if (activeChar.isGM() && _file != null)
			activeChar.sendMessage("Html File: " + _file);

		if (_file != null)
			activeChar.setHtmlSave(_file);

		activeChar.clearBypass();
		for (int i = 0; i < _html.length(); i++)
		{
			int start = _html.indexOf("\"bypass ", i);
			int finish = _html.indexOf("\"", start + 1);
			if (start < 0 || finish < 0)
				break;

			if (_html.substring(start + 8, start + 10).equals("-h"))
				start += 11;
			else
				start += 8;

			i = finish;
			int finish2 = _html.indexOf("$", start);
			if (finish2 < finish && finish2 > 0)
				activeChar.addBypass2(_html.substring(start, finish2).trim());
			else
				activeChar.addBypass(_html.substring(start, finish).trim());
		}
	}
}
public class HtmlRegression {static void check(boolean b,String s){if(!b)throw new AssertionError(s);}public static void main(String[] a){L2PcInstance p=new L2PcInstance();HtmlClient c=new HtmlClient(p);NpcHtmlMessage m=new NpcHtmlMessage(c,"<button action=\"bypass -h npc_123_Chat 1\">");m.runImpl();check(p.validateBypass("npc_123_Chat 1"),"initial whitelist");new NpcHtmlMessage(c,"<html>unrelated</html>").runImpl();check(!p.validateBypass("npc_123_Chat 1"),"new HTML invalidates old whitelist");m.runImpl();check(p.validateBypass("npc_123_Chat 1"),"reopening restores whitelist");new NpcHtmlMessage(c,"<button action=\"bypass -h npc_%objectId%_Chat 1\">").runImpl();check(!p.validateBypass("npc_123_Chat 1")&&p.validateBypass("npc_%objectId%_Chat 1"),"placeholder before serialization mismatch");System.out.println("REPRODUCED: new HTML clears previous whitelist; reopening restores it. objectId placeholder remains in whitelist when scanner runs before serialization. Production scanner and validation methods; transport mocked.");}}
