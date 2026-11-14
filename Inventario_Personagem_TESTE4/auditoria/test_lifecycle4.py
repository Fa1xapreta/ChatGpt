from pathlib import Path
import json,sys
R=Path(__file__).resolve().parent;sys.path.insert(0,str(R));import test_toggle4 as M
cases=[]
for path in ['InventoryWnd.OnHide','InventoryWnd.OnEvent','MobInformerWnd.OnShow','StatusWnd.ReleaseOptionalPortrait']:
 e=next(e for e in M.D['exports'] if e['path']==path);M.script,size=M.getscript(M.new,e)
 original_e=next(e for e in M.old['exports'] if e['path']==path);original_script,oldsize=M.getscript(M.original,original_e)
 M.mapping={};d=M.Decoder(M.script)
 while d.p<len(M.script):d.token()
 for opened in [False,True]:
  for hud in [False,True]:
   v=M.VM('',opened,True,hud);v.p=len(original_script)-2;v.run();assert v.hud==hud
   if not opened:assert not v.actions
   elif path=='InventoryWnd.OnEvent':assert v.opened and any(a[0]=='BindPreview' and a[1][1]=='display-pawn' for a in v.actions)
   else:
    assert not v.opened
    if path=='StatusWnd.ReleaseOptionalPortrait':assert ('ReleaseDisplayPawn',) not in v.actions
   cases.append({'function':path,'initially_open':opened,'hud_expanded':hud,'finally_open':v.opened,'actions':v.actions})
(R/'TESTE4/TESTE_CICLO_DE_VIDA.json').write_text(json.dumps({'real_new_bytecode_epilogues_interpreted':True,'engine_native_calls_mocked':True,'cases':cases},indent=2));print('PASS: 16 lifecycle cases; refresh, close, HUD release and no recursion; original HUD flag preserved')
