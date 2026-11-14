from pathlib import Path
import json,hashlib,struct,sys
R=Path(__file__).resolve().parent;W=R.parent;sys.path.insert(0,str(W/'cloak-audit/vendor'));import pefile
files={'original':W/'cloak-migration/current-system/D3DDrv.dll','model':W/'cloak-audit/extracted/system/D3DDrv.dll'};rows={}
def fnv(b):
 h=0x811c9dc5
 for v in b:h=((h^v)*0x1000193)&0xffffffff
 return h
for k,p in files.items():
 b=p.read_bytes();rows[k]={'bytes':len(b),'sha256':hashlib.sha256(b).hexdigest(),'fnv32':hex(fnv(b))}
assert rows['original']['fnv32']=='0x167faa65' and rows['model']['fnv32']=='0xc2b0e85b'
p=pefile.PE(str(W/'cloak-migration/current-system/L2modExCore.dll'));code=p.get_data(0x5cf4,0x24);assert code[:10]==bytes.fromhex('81bd78ffffff14801300');assert bytes.fromhex('3d65aa7f16') in code
report={'finding':'Original aura module refuses the model D3DDrv before installing hooks','files':rows,'native_gate':{'module':'L2modExCore.dll','size_compare_va':'0x68005cf4','required_size':'0x138014','hash_compare_va':'0x68005d02','required_fnv32':'0x167faa65','reject_log':'D3DDrv diferente da system auditada. Efeito desativado. FNV32','disassembly_bytes_hex':code.hex()},'repair':'Restore the users original D3DDrv.dll; do not patch aura or cape checks','runtime_verified':False,'preserve':['Fighter.ukx TESTE7 confirmed by user','CapasClassic DLL and INI','MileniumFX and d3d9 bridge','CliExt, MilAuraWait and L2modExCore','server files and other race assets']}
(R/'AUDITORIA.json').write_text(json.dumps(report,indent=2));print(json.dumps(report,indent=2))
