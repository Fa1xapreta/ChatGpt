from pathlib import Path
import os,subprocess,shutil,json
R=Path(__file__).resolve().parent;source=(R/'test6/Ajustar.ps1').read_text(encoding='utf-8-sig');a=source.index(' $identity=');z=source.index(' if(Get-Process',a);source=source[:a]+source[z:];baseline=(R.parent/'cloak-audit/extracted/system/CapasClassic.dll').read_bytes();candidate=(R/'candidate-CapasClassic.dll').read_bytes();env=dict(os.environ,XDG_CACHE_HOME='/tmp/cloak-powershell-cache',XDG_CONFIG_HOME='/tmp/cloak-powershell-config',XDG_DATA_HOME='/tmp/cloak-powershell-data');passed=[]
for case in ['normal','unknown_version','modified_after','write_failure','game_open','bad_backup']:
 b=R/'test6-fixtures'/case
 if b.exists():shutil.rmtree(b)
 (b/'system').mkdir(parents=True);(b/'kit').mkdir();(b/'system/L2.exe').write_bytes(b'fixture')
 initial=baseline if case!='unknown_version' else baseline+b'changed'
 (b/'system/CapasClassic.dll').write_bytes(initial)
 unchanged={'CapasClassic.ini':b'mountsettings','d3d9.dll':b'graphics','CliExt.dll':b'aura','mount.u':b'mounts'}
 for name,data in unchanged.items():(b/'system'/name).write_bytes(data)
 s=source
 if case=='write_failure':s=s.replace('   [IO.File]::WriteAllBytes($path,$newBytes)',"   [IO.File]::WriteAllText($path,'partial')\n   throw 'injected write failure'")
 if case=='game_open':s=s.replace(" if(Get-Process -Name L2 -ErrorAction SilentlyContinue)"," if($true)")
 if case=='bad_backup':(b/'Milenium_Corrida_Backup_TESTE6').mkdir();(b/'Milenium_Corrida_Backup_TESTE6/CapasClassic.dll').write_bytes(b'bad')
 (b/'kit/Ajustar.ps1').write_text(s)
 def run(action):
  harness=b/'run.ps1';harness.write_text("function global:Read-Host {param([string]$Prompt) return ''}\n& '"+str(b/'kit/Ajustar.ps1')+"' -Acao "+action+" -Cliente '"+str(b)+"'\nexit $LASTEXITCODE")
  r=subprocess.run(['/tmp/cloak-powershell/pwsh','-NoProfile','-File',str(harness)],env=env,capture_output=True,text=True);(b/(action+'.txt')).write_text(r.stdout+r.stderr);return r
 r=run('Aplicar')
 if case in ['unknown_version','write_failure','game_open','bad_backup']:assert r.returncode!=0,(case,r.stdout+r.stderr);assert (b/'system/CapasClassic.dll').read_bytes()==initial
 else:
  assert r.returncode==0,(case,r.stdout+r.stderr);assert (b/'system/CapasClassic.dll').read_bytes()==candidate;assert run('Aplicar').returncode==0
  if case=='modified_after':
   changed=candidate+b'newedit';(b/'system/CapasClassic.dll').write_bytes(changed);assert run('Desfazer').returncode!=0;assert (b/'system/CapasClassic.dll').read_bytes()==changed
  else:
   assert run('Desfazer').returncode==0;assert (b/'system/CapasClassic.dll').read_bytes()==baseline;assert run('Aplicar').returncode==0;assert run('Desfazer').returncode==0
 for name,data in unchanged.items():assert (b/'system'/name).read_bytes()==data
 passed.append(case);print('PASS',case)
(R/'test6/TESTES.json').write_text(json.dumps({'passed':passed,'platform':'PowerShell 7 Linux, Windows UI/elevation mocked; actual DLL input; no game runtime test'},indent=2))
