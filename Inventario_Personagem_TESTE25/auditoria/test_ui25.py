from pathlib import Path
import ast,sys,struct,json
R=Path(__file__).resolve().parent;sys.path[:0]=[str(R),str(R.parent/'cloak-audit')];from xdat import Reader;from catalog_unreal import catalog
new=(R/'TESTE25/system/interface.u').read_bytes();D=catalog(R/'TESTE25/system/interface.u');old=json.loads((R/'interface-catalog.json').read_text());refs=len(D['exports']);imports=len(D['imports']);original=Path('/workspace/attachments/6e516f3b-8810-4af1-8516-fe9e4339c975/interface.u').read_bytes()
tree=ast.parse((R/'verify_test4.py').read_text());selected=[n for n in tree.body if isinstance(n,ast.ClassDef) and n.name=='Decoder' or isinstance(n,ast.FunctionDef) and n.name=='getscript'];exec(compile(ast.Module(body=selected,type_ignores=[]),'decoder','exec'))
checks={};expected={'InventoryWnd.OnLoad','InventoryWnd.OnShow','InventoryWnd.OnClickButton','InventoryWnd.OnEvent','InventoryWnd.OnHide','MobInformerWnd.OnShow'}
for name in expected:
 e=next(e for e in D['exports'] if e['path']==name);b,size=getscript(new,e);d=Decoder(b)
 while d.p<len(b):d.token()
 assert d.v==size and set(d.jumps)<=d.starts;checks[name]={'runtime_bytes':size,'jumps':len(d.jumps)}
changed=[b['path'] for a,b in zip(old['exports'],D['exports']) if original[a['offset']:a['offset']+a['size']]!=new[b['offset']:b['offset']+b['size']]];assert set(changed)==expected
ROOT='MileniumInventoryPreviewWnd';mapping={};base_token=Decoder.token
def tok(self):mapping[self.v]=self.p;return base_token(self)
Decoder.token=tok
s=(R/'test_toggle4.py').read_text();s=s[s.index('class VM:'):s.index('\ncases=[]')]
s=s.replace("else:assert D['imports'][-cls-1]['name']=='Texture'","else:assert D['imports'][-cls-1]['name'] in ['Texture','ButtonHandle']")
s=s.replace("if op==0:return self.key if D['exports'][self.compact()-1]['name']=='strID' else None", "if op==0:\n   field=D['exports'][self.compact()-1]['name'];return self.key if field=='strID' else self.event if field=='Event_ID' else None")
s=s.replace("if name=='GetHandle':return ('window',a[0])", "if name=='GetHandle':\n    assert not a[0].endswith('RenderStatus')\n    return ('window',a[0])")
s=s.replace("if name=='IsShowWindow':assert ctx==('window',ROOT);return self.opened", "if name=='IsShowWindow':return self.opened if ctx==('window',ROOT) else self.inventory_visible")
s=s.replace("'SetAnchor','SetText'","'SetAnchor','SetText','SetTexture','SetTooltipText','SetTooltipCustomType'")
s=s.replace("if op in [0x7a,0x70]:", "if op==0x2a:return None\n  if op in [0x7a,0x70,0x77,0x9a]:")
s=s.replace("return a==b if op==0x7a else a+str(b)", "return a!=b if op==0x77 else a==b if op in [0x7a,0x9a] else a+str(b)")
s=s.replace("if name=='GetHandle':","if name=='SetTooltip':return ('tooltip',a[0])\n   if name=='GetHandle':")
exec(compile(s,'vm','exec'))
def setup(name):
 global script,oldscript,mapping
 fn=next(e for e in D['exports'] if e['path']==name);script,_=getscript(new,fn);fn0=next(e for e in old['exports'] if e['path']==name);oldscript,_=getscript(original,fn0);mapping={};d=Decoder(script)
 while d.p<len(script):d.token()
def assert_button(v,opened):
 key=('window','InventoryWnd.btnMileniumPreview')
 tex=[a for a in v.actions if a[0]=='SetTexture' and a[1]==key]
 tips=[a for a in v.actions if a[0]=='SetTooltipCustomType' and a[1]==key]
 expected='L2UI_CH3.ShortcutWnd.AS_ShortcutWnd_'+('DEL' if opened else 'ADD')
 assert tex and tex[-1][2]==[expected,expected+'_Over',expected+'_Over'],v.actions
 assert tips and tips[-1][2]==[('tooltip','Inventory Viewr')],v.actions
cases=[];setup('InventoryWnd.OnClickButton')
for hud in [False,True]:
 for key,opened,actor in [('btnMileniumPreview',False,True),('btnMileniumPreview',True,True),('unhandled',False,True),('btnMileniumPreview',False,False)]:
  v=VM(key,opened,actor,hud);v.inventory_visible=True;v.run();assert v.hud==hud
  if key=='unhandled' or (not opened and not actor):assert not v.actions and not v.opened
  elif opened:
   assert not v.opened and v.actions[0][0]=='HideWindow' and v.actions[1][0]=='ReleasePreview';assert_button(v,False)
  else:
   assert_button(v,True)
   anchor=next(a for a in v.actions if a[0]=='SetAnchor');assert anchor[2][-2:]==[0,33]
   bind=next(a for a in v.actions if a[0]=='BindPreview');assert bind[1][1]=='player' and bind[1][2:]==[25.0,30.0,100.0]
   assert all(a[0] not in ['PrepareDisplayPawn','ReleaseDisplayPawn','PortraitExpanded'] for a in v.actions)
  cases.append({'function':'click','key':key,'opened':opened,'actor':actor,'hud':hud})
for name in ['InventoryWnd.OnLoad','InventoryWnd.OnShow','InventoryWnd.OnHide','MobInformerWnd.OnShow']:
 setup(name)
 for opened in [False,True]:
  v=VM('',opened);v.inventory_visible=True;v.p=len(oldscript)-2;v.run();assert not v.opened;assert all(a[0] not in ['BindPreview','ShowWindow'] for a in v.actions)
  assert v.actions[0][0]=='HideWindow';assert_button(v,False)
  assert ('ReleasePreview',[]) in v.actions if name!='InventoryWnd.OnLoad' else ('ReleasePreview',[]) not in v.actions
  cases.append({'function':name,'initially_open':opened,'closed_after':True})
setup('InventoryWnd.OnEvent')
for event in [40,180,650,2570,2580,2590,2600,2610,2620,2630,2631,1710]:
 for inv_visible in [False,True]:
  for opened in [False,True]:
   v=VM('',opened);v.event=event;v.inventory_visible=inv_visible;v.p=len(oldscript)-2;v.run()
   assert all(a[0] not in ['BindPreview','ShowWindow'] for a in v.actions)
   close=event==40 or (not inv_visible and opened)
   if close:
    assert not v.opened and v.actions[0][0]=='HideWindow' and v.actions[1][0]=='ReleasePreview';assert_button(v,False)
   else:assert not v.actions and v.opened==opened
   cases.append({'function':'event','event':event,'inventory_visible':inv_visible,'preview_visible':opened,'cleanup':close,'no_rebind':True})
(R/'TESTE25/TESTE_UI.json').write_text(json.dumps({'real_bytecode_interpreted':True,'engine_calls_mocked':True,'no_event_driven_rebind':True,'startup_show_hide_restart_cleanup':True,'null_actor_does_not_open_preview':True,'all_other_objects_unchanged':True,'function_checks':checks,'button_add_del_and_tooltips_verified':True,'cases':cases},indent=2));print('PASS',len(cases),'UI lifecycle cases; no event rebind; only explicit valid click opens')
