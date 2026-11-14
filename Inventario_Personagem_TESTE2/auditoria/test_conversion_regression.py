from pathlib import Path
import json,sys
R=Path(__file__).resolve().parent;sys.path.insert(0,str(R));from verify_test import Decoder,getscript,D,orig
sys.path.insert(0,'/workspace/work/cloak-audit');from catalog_unreal import catalog
passed=[]
for path in ['InventoryWnd.SetItemCount','StatusWnd.UpdateUserInfo','InventoryWnd.SetAdenaText']:
 old=json.loads((R/'interface-catalog.json').read_text());e=next(e for e in old['exports'] if e['path']==path);script,size=getscript(orig,e);d=Decoder(script)
 while d.p<len(script):d.token()
 assert d.v==size,(path,d.v,size)
 assert b'\x39\x53' in script
 passed.append({'original_function':path,'runtime_size':size,'conversion_prefix_39_53_present':True})
p=R/'TESTE1/system/interface.u';old=catalog(p);b=p.read_bytes();e=next(e for e in old['exports'] if e['path']=='InventoryWnd.OnClickButton');script,size=getscript(b,e);d=Decoder(script);rejected=False
try:
 while d.p<len(script):d.token()
except AssertionError as ex:
 assert '0x53' in str(ex),str(ex);rejected=True;failure=str(ex)
assert rejected,'The validator must reject the actual previously released broken bytecode'
for name in ['interface.xdat','MileniumPortraitNative.dll']:assert (R/'TESTE1/system'/name).read_bytes()==(R/'TESTE2/system'/name).read_bytes()
result={'original_client_conversion_fixtures':passed,'actual_broken_test1_rejected':True,'actual_error':failure,'xdat_and_native_dll_unchanged_from_test1':True,'game_launch_validation_pending':True}
(R/'TESTE2/REGRESSAO_TOKEN_53.json').write_text(json.dumps(result,indent=2));print(json.dumps(result,indent=2))
