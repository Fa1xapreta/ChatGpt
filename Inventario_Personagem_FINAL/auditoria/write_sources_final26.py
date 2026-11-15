from pathlib import Path
import re,json,hashlib
R=Path(__file__).resolve().parent;OUT=R/'FINAL26/Classes';OUT.mkdir(exist_ok=True);BASE=Path('/workspace/work/classes-upload/Classes')
ROOT='MileniumInventoryPreviewWnd';BTN='InventoryWnd.btnMileniumPreview'
def bounds(s,name):
 m=re.search(r'\bfunction\s+'+re.escape(name)+r'\s*\(',s);assert m,name
 a=s.index('{',m.end());n=1;i=a+1;mode='code'
 while n:
  ch=s[i];pair=s[i:i+2]
  if mode=='line':
   if ch=='\n':mode='code'
  elif mode=='comment':
   if pair=='*/':mode='code';i+=1
  elif mode=='str':
   if ch=='\\':i+=1
   elif ch=='"':mode='code'
  else:
   if pair=='//':mode='line';i+=1
   elif pair=='/*':mode='comment';i+=1
   elif ch=='"':mode='str'
   elif ch=='{':n+=1
   elif ch=='}':n-=1
  i+=1
 return m.start(),a,i-1

def append(s,name,extra):
 _,a,b=bounds(s,name);return s[:b]+extra+s[b:]
def buttonstate(which,foreign=False):
 tex='L2UI_CH3.ShortcutWnd.AS_ShortcutWnd_'+which
 helper='InventoryWnd(GetScript("InventoryWnd")).SetTooltip' if foreign else 'SetTooltip'
 return f'''ButtonHandle(GetHandle("{BTN}")).SetTexture("{tex}", "{tex}_Over", "{tex}_Over");
ButtonHandle(GetHandle("{BTN}")).SetTooltipCustomType({helper}("Inventory Viewr"));'''
def close(foreign=False):
 return f'''GetHandle("{ROOT}").HideWindow();
class'MobPreviewNative'.static.ReleasePreview();
'''+buttonstate('ADD',foreign)
def indent(s,n):return '\n'.join(' '*n+x if x else '' for x in s.splitlines())
s=(BASE/'InventoryWnd.uc').read_text()
s=append(s,'OnLoad','\n    // Inventory Viewr final: start closed.\n'+indent(f'GetHandle("{ROOT}").HideWindow();\n'+buttonstate('ADD'),4)+'\n')
for name in ['OnShow','OnHide']:s=append(s,name,'\n    // Inventory Viewr final: release the preview with the inventory.\n'+indent(close(),4)+'\n')
event='''if(Event_ID == 40)
{
'''+indent(close(),4)+'''
}
else if(!GetHandle("InventoryWnd").IsShowWindow())
{
    if(GetHandle("'''+ROOT+'''").IsShowWindow())
    {
'''+indent(close(),8)+'''
    }
}'''
s=append(s,'OnEvent','\n    // Inventory Viewr final: restart/hidden-inventory cleanup; no item-event rebind.\n'+indent(event,4)+'\n')
opening=f'''GetHandle("MobInformerWnd").HideWindow();
GetHandle("{ROOT}.MobViewport").ShowWindow();
GetHandle("{ROOT}").SetAnchor("InventoryWnd", "TopLeft", "TopRight", 0, 33);
GetHandle("{ROOT}").ShowWindow();
class'MobPreviewNative'.static.BindPreview(
    GetHandle("{ROOT}.MobViewport"),
    class'MobPreviewNative'.static.GetWorldActor(class'UIDATA_PLAYER'.static.GetPlayerID()),
    25.0, 30.0, 100.0);
class'MobPreviewNative'.static.SetStageBackground(
    Texture(DynamicLoadObject("L2UI_CH3.AbnormalWnd.Inventory_DF_CharacterEquipBg", class'Texture', true)));
ButtonHandle(GetHandle("{ROOT}.RotateLeft")).SetTexture(
    "L2Devil.Log.Inventory_DF_Btn_RotateRight", "L2Devil.Log.Inventory_DF_Btn_RotateRight_Down", "L2Devil.Log.Inventory_DF_Btn_RotateRight_Over");
ButtonHandle(GetHandle("{ROOT}.RotateRight")).SetTexture(
    "L2Devil.Log.Inventory_DF_Btn_RotateLeft", "L2Devil.Log.Inventory_DF_Btn_RotateLeft_Down", "L2Devil.Log.Inventory_DF_Btn_RotateLeft_Over");
GetHandle("{ROOT}.RotateLeft").ShowWindow();
GetHandle("{ROOT}.RotateRight").ShowWindow();
'''+buttonstate('DEL')
handler=f'''// Inventory Viewr final: only this button opens/binds the real player.
if(strID == "btnMileniumPreview")
{{
    if(GetHandle("{ROOT}").IsShowWindow())
    {{
'''+indent(close(),8)+'''
    }
    else if(class'MobPreviewNative'.static.GetWorldActor(class'UIDATA_PLAYER'.static.GetPlayerID()) != None)
    {
'''+indent(opening,8)+'''
    }
}'''
_,a,b=bounds(s,'OnClickButton');body=s[a:b];needle='        default:\n        break;';assert body.endswith('\n    }\n') and body.count(needle)>=1
pos=body.rfind(needle);body=body[:pos]+'        default:\n'+indent(handler,12)+'\n        break;'+body[pos+len(needle):];s=s[:a]+body+s[b:]
# Full source files, including every unchanged original inventory/mob function and defaults.
(OUT/'InventoryWnd.uc').write_bytes(s.replace('\r\n','\n').replace('\n','\r\n').encode('utf-8'))
m=(BASE/'MobInformerWnd.uc').read_text();m=append(m,'OnShow','\n    // Inventory Viewr final: shared preview renderer must be released.\n'+indent(close(True),4)+'\n')
(OUT/'MobInformerWnd.uc').write_bytes(m.replace('\r\n','\n').replace('\n','\r\n').encode('utf-8'))
(OUT/'MobPreviewNative.uc').write_bytes((BASE/'MobPreviewNative.uc').read_bytes())
# Check that only the intended six original functions have changed in the maintained sources.
checks=[]
for name,expected in [('InventoryWnd',{'OnLoad','OnShow','OnHide','OnClickButton','OnEvent'}),('MobInformerWnd',{'OnShow'})]:
 old=(BASE/(name+'.uc')).read_text();new=(OUT/(name+'.uc')).read_text();changed=[]
 functions=re.findall(r'\bfunction\s+([A-Za-z_][A-Za-z_0-9]*)\s*\(',old)
 for fn in functions:
  _,a,b=bounds(old,fn);_,c,d=bounds(new,fn)
  if old[a:b]!=new[c:d]:changed.append(fn)
 assert set(changed)==expected,(name,changed)
 restored=new
 for fn in expected:
  _,a,b=bounds(old,fn);_,c,d=bounds(restored,fn);restored=restored[:c]+old[a:b]+restored[d:]
 assert restored==old,'Unexpected source change outside the six approved function bodies'
 assert 'TitleViwer' not in new and 'MileniumInventoryPreviewHeaderWnd' not in new
 checks.append({'class':name,'changed_functions':changed,'all_other_function_bodies_unchanged':True,'full_original_defaultproperties_preserved':old[old.rfind('defaultproperties'):]==new[new.rfind('defaultproperties'):]})
assert (OUT/'MobPreviewNative.uc').read_bytes()==(BASE/'MobPreviewNative.uc').read_bytes()
report={'full_sources_based_on_user_classes':True,'source_compilation_with_ucc_not_performed':True,'binary_interface_supplied_ready_to_use':True,'changed_original_functions':checks,'native_signatures_unchanged':True,'source_file_sha256':{f.name:hashlib.sha256(f.read_bytes()).hexdigest() for f in OUT.glob('*.uc')}}
(R/'FINAL26/FONTES.json').write_text(json.dumps(report,indent=2));print('Full UC sources generated; only six intended functions changed; native declarations unchanged')
