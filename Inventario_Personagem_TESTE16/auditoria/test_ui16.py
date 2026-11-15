from pathlib import Path
import ast,sys,struct,json
R=Path(__file__).resolve().parent;sys.path[:0]=[str(R),str(R.parent/'cloak-audit')];from xdat import Reader;from catalog_unreal import catalog
new=(R/'TESTE16/system/interface.u').read_bytes();D=catalog(R/'TESTE16/system/interface.u');old=json.loads((R/'interface-catalog.json').read_text());refs=len(D['exports']);imports=len(D['imports'])
tree=ast.parse((R/'verify_test4.py').read_text());selected=[n for n in tree.body if isinstance(n,ast.ClassDef) and n.name=='Decoder' or isinstance(n,ast.FunctionDef) and n.name=='getscript'];exec(compile(ast.Module(body=selected,type_ignores=[]),'decoder','exec'))
checks={}
for name in ['InventoryWnd.OnClickButton','InventoryWnd.OnEvent','InventoryWnd.OnHide','MobInformerWnd.OnShow']:
 e=next(e for e in D['exports'] if e['path']==name);b,size=getscript(new,e);d=Decoder(b)
 while d.p<len(b):d.token()
 assert d.v==size and set(d.jumps)<=d.starts;checks[name]={'runtime_bytes':size,'jumps':len(d.jumps)}
fn=next(e for e in D['exports'] if e['path']=='InventoryWnd.OnClickButton');script,size=getscript(new,fn);original_fn=next(e for e in old['exports'] if e['path']==fn['path']);original=Path('/workspace/attachments/6e516f3b-8810-4af1-8516-fe9e4339c975/interface.u').read_bytes();oldscript,oldsize=getscript(original,original_fn)
mapping={};oldtoken=Decoder.token
def tok(self):mapping[self.v]=self.p;return oldtoken(self)
Decoder.token=tok;d=Decoder(script)
while d.p<len(script):d.token()
ROOT='MileniumInventoryPreviewWnd'
s=(R/'test_toggle4.py').read_text();a=s.index('class VM:');z=s.index('\ncases=[]',a);s=s[a:z]
s=s.replace("else:assert D['imports'][-cls-1]['name']=='Texture'","else:assert D['imports'][-cls-1]['name'] in ['Texture','ButtonHandle']")
s=s.replace("if name=='GetHandle':return ('window',a[0])","if name=='GetHandle':\n    assert not a[0].endswith('RenderStatus'),'Binding must not depend on hidden footer'\n    return ('window',a[0])")
s=s.replace("'SetAnchor','SetText'","'SetAnchor','SetText','SetTexture'")
exec(compile(s,'vm','exec'))
cases=[]
for hud in [False,True]:
 for key,opened,actor in [('btnMileniumPreview',False,True),('btnMileniumPreview',True,True),('unhandled',False,True),('btnMileniumPreview',False,False)]:
  v=VM(key,opened,actor,hud);v.run();assert v.hud==hud
  if key=='unhandled':assert not v.actions
  elif not opened:
   bind=next(a for a in v.actions if a[0]=='BindPreview');assert bind[1][1]==('player' if actor else None)
   assert all(a[0] not in ['PrepareDisplayPawn','ReleaseDisplayPawn','PortraitExpanded'] for a in v.actions)
   for btn,texture in [('RotateLeft','RotateRight'),('RotateRight','RotateLeft')]:
    assert ('ShowWindow',('window',ROOT+'.'+btn),[]) in v.actions
    assert ('SetTexture',('window',ROOT+'.'+btn),['L2Devil.Log.Inventory_DF_Btn_'+texture,'L2Devil.Log.Inventory_DF_Btn_'+texture+'_Down','L2Devil.Log.Inventory_DF_Btn_'+texture+'_Over']) in v.actions
   anchor=next(a for a in v.actions if a[0]=='SetAnchor');assert anchor[2][-2:]==[0,33]
  cases.append({'key':key,'opened':opened,'actor':actor,'hud':hud})
# All objects except the four required UI function bodies are byte-identical to the source.
changed=[]
for a,b in zip(old['exports'],D['exports']):
 if original[a['offset']:a['offset']+a['size']]!=new[b['offset']:b['offset']+b['size']]:changed.append(b['path'])
assert set(changed)=={'InventoryWnd.OnClickButton','InventoryWnd.OnEvent','InventoryWnd.OnHide','MobInformerWnd.OnShow'},changed
(R/'TESTE16/TESTE_UI.json').write_text(json.dumps({'real_bytecode_interpreted':True,'engine_calls_mocked':True,'footer_handle_not_accessed':True,'actual_world_actor_bound':True,'no_hud_pawn_borrow_or_cleanup':True,'all_other_objects_unchanged':True,'explicit_texture_setup_and_arrow_show':True,'function_checks':checks,'cases':cases},indent=2));print('PASS: actual bytecode; 8 real-actor toggle cases; HUD untouched; only four UI functions changed')
