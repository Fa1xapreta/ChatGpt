from pathlib import Path
import shutil,subprocess,json,os
R=Path(__file__).resolve().parent;W=R.parent;K=R/'kit';env=dict(os.environ,XDG_CACHE_HOME='/tmp/cloak-powershell-cache',XDG_CONFIG_HOME='/tmp/cloak-powershell-config',XDG_DATA_HOME='/tmp/cloak-powershell-data');passed=[]
def strip(s):
 a=s.index(' $identity=');z=s.index(' if(Get-Process',a);return s[:a]+s[z:]
for case in ['after_A1','before_A1','original_controller','already_installed','unknown_controller','tampered_payload','write_failure','modified_after','bad_backup']:
 b=R/'fixture'
 if b.exists():shutil.rmtree(b)
 (b/'client/system').mkdir(parents=True);(b/'client/Animations').mkdir();root=b/'client';kit=b/'kit';shutil.copytree(K,kit);(root/'system/L2.exe').write_bytes(b'fixture')
 initial={}
 for name,p in [('L2modExCore.dll',W/'cloak-migration/current-system/L2modExCore.dll'),('CapasClassic.dll',W/'cloak-audit/extracted/system/CapasClassic.dll'),('CliExt.dll',W/'cloak-migration/complete-kit/cliente/system/CliExt.dll'),('D3DDrv.dll',W/'cloak-migration/current-system/D3DDrv.dll'),('MilAuraWait.dll',W/'cloak-integrated/native/MilAuraWait.dll')]:initial[name]=p.read_bytes()
 if case=='before_A1':initial['D3DDrv.dll']=(W/'cloak-audit/extracted/system/D3DDrv.dll').read_bytes()
 if case=='original_controller':initial['CliExt.dll']=(W/'cloak-migration/current-system/CliExt.dll').read_bytes();del initial['MilAuraWait.dll']
 if case=='already_installed':initial['MilAuraWait.dll']=(K/'payload/MilAuraWait.dll').read_bytes()
 if case=='unknown_controller':initial['CliExt.dll']+=b'unknown'
 for n,v in initial.items():(root/'system'/n).write_bytes(v)
 (root/'Animations/Fighter.ukx').write_bytes(b'test8-running-animation');(root/'system/MileniumFX.ini').write_bytes(b'vivid-colors');(root/'system/CapasClassic.ini').write_bytes(b'mounts')
 for name in ['Instalar.ps1','Restaurar.ps1']:
  s=strip((kit/name).read_text(encoding='utf-8-sig'))
  if case=='write_failure' and name=='Instalar.ps1':s=s.replace('Copy-Item -LiteralPath $f.source -Destination $path -Force;',"[IO.File]::WriteAllText($path,'partial');throw 'injected write failure';")
  (kit/name).write_text(s)
 if case=='tampered_payload':(kit/'payload/MilAuraWait.dll').write_bytes(b'tampered')
 def run(script,args=''):
  h=b/'run.ps1';h.write_text("function global:Read-Host {param([string]$Prompt) return ''}\n& '"+str(script)+"' "+args+"\nexit $LASTEXITCODE")
  q=subprocess.run(['/tmp/cloak-powershell/pwsh','-NoProfile','-File',str(h)],env=env,capture_output=True,text=True);(R/(case+'-'+script.stem+'.log')).write_text(q.stdout+q.stderr);return q
 q=run(kit/'Instalar.ps1',"-Cliente '"+str(root)+"'")
 if case in ['unknown_controller','tampered_payload','write_failure']:
  assert q.returncode!=0,(case,q.stdout,q.stderr)
  for n,v in initial.items():assert (root/'system'/n).read_bytes()==v
 else:
  assert q.returncode==0,(case,q.stdout,q.stderr)
  for n in ['CliExt.dll','D3DDrv.dll','MilAuraWait.dll']:assert (root/'system'/n).read_bytes()==(K/'payload'/n).read_bytes()
  assert run(kit/'Instalar.ps1',"-Cliente '"+str(root)+"'").returncode==0
  if case!='already_installed':
   backup=next(root.glob('Milenium_Aura_Backup_B2_*'))
   if case=='modified_after':
    p=root/'system/MilAuraWait.dll';p.write_bytes(p.read_bytes()+b'edit');assert run(kit/'Restaurar.ps1',"-SemPausa -Cliente '"+str(root)+"'").returncode!=0;assert p.read_bytes().endswith(b'edit')
   elif case=='bad_backup':
    (backup/'MilAuraWait.dll').write_bytes(b'corrupt');assert run(kit/'Restaurar.ps1',"-SemPausa -Cliente '"+str(root)+"'").returncode!=0
   else:
    q=run(kit/'Restaurar.ps1',"-SemPausa -Cliente '"+str(root)+"'");assert q.returncode==0,(case,q.stdout,q.stderr)
    for n,v in initial.items():assert (root/'system'/n).read_bytes()==v,(case,n)
    if case=='original_controller':assert not(root/'system/MilAuraWait.dll').exists()
 assert (root/'Animations/Fighter.ukx').read_bytes()==b'test8-running-animation';assert (root/'system/MileniumFX.ini').read_bytes()==b'vivid-colors';assert (root/'system/CapasClassic.ini').read_bytes()==b'mounts'
 passed.append(case);print('PASS',case,flush=True);shutil.rmtree(b)
(K/'TESTES.json').write_text(json.dumps(dict(passed=passed,platform='PowerShell 7 Linux fixtures with real DLL bytes, elevation/UI mocked; Windows game not executed',native_abi_contract_checked=True,native_binding_state_machine_tested=True,pvp_visual_confirmed=False),indent=2))
