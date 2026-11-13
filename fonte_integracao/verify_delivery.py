from pathlib import Path
import sys,json,hashlib,subprocess,os,shutil
R=Path(__file__).resolve().parent;K=R.parent/'cloak-migration/complete-kit';sys.path.insert(0,str(R.parent/'cloak-audit/vendor'));import pefile
sys.path.insert(0,str(R));from armor_codec import parse,record_bytes
original=parse((R.parent/'cloak-migration/decoded/armorgrp.dat').read_bytes());new=parse((R/'data/armorgrp.dec').read_bytes());assert len(new)==1537 and len(original)==1467
assert all(record_bytes(a)==record_bytes(b) for a,b in zip(original,new))
assert (R/'data/armorgrp.dec').read_bytes()==(R/'data/armorgrp.verify').read_bytes();assert (R/'data/itemname-e.dec').read_bytes()==(R/'data/itemname-e.verify').read_bytes()
for name in ['d3d9.dll','MilAuraWait.dll','MileniumGraphics.dll','L2modExCore.dll','CliExt.dll']:
 p=pefile.PE(str(K/'cliente/system'/name));assert p.FILE_HEADER.Machine==0x14c;print(name,'PE32 OK')
p=pefile.PE(str(K/'cliente/system/d3d9.dll'));exports={e.name.decode() for e in p.DIRECTORY_ENTRY_EXPORT.symbols if e.name};assert {'Direct3DCreate9','Direct3DCreate9Ex','HangGetPresentAgeMs'}<=exports
# Validate actual append/header via the shipped PowerShell implementation and undo original bytes.
patch=json.loads((K/'alias-patches/patches.json').read_text())[0];temp=R/'actual-alias-test.u';shutil.copyfile(K/'cliente/system'/patch['file'],temp)
script=(K/'Instalar.ps1').read_text(encoding='utf-8-sig');functions=script[script.index('function Get-Sha'):script.index('\ntry{\n Elevate-IfNeeded')];# elevation function defined but never called
script="$kit='"+str(K)+"'\n"+functions+"\n$p=Get-Content -Raw '"+str(K/'alias-patches/patches.json')+"' | ConvertFrom-Json\nApply-Alias '"+str(temp)+"' $p[0]\n"
f=R/'actual-alias-test.ps1';f.write_text(script)
env=dict(os.environ,XDG_CACHE_HOME='/tmp/cloak-powershell-cache',XDG_CONFIG_HOME='/tmp/cloak-powershell-config',XDG_DATA_HOME='/tmp/cloak-powershell-data')
r=subprocess.run(['/tmp/cloak-powershell/pwsh','-NoProfile','-File',str(f)],env=env,capture_output=True,text=True);assert r.returncode==0,r.stdout+r.stderr
assert hashlib.sha256(temp.read_bytes()).hexdigest()==patch['after'];temp.unlink();f.unlink()
print('PASS: actual Unreal package PowerShell patch, original armor rows, DAT roundtrips, x86 exports')
