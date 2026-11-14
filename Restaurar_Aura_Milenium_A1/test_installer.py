from pathlib import Path
import os,subprocess,shutil,json
R=Path(__file__).resolve().parent;W=R.parent;source=(R/'kit/Restaurar.ps1').read_text(encoding='utf-8-sig');a=source.index(' $identity=');z=source.index(' if(Get-Process',a);source=source[:a]+source[z:];baseline=(W/'cloak-audit/extracted/system/D3DDrv.dll').read_bytes();target=(W/'cloak-migration/current-system/D3DDrv.dll').read_bytes();env=dict(os.environ,XDG_CACHE_HOME='/tmp/cloak-powershell-cache',XDG_CONFIG_HOME='/tmp/cloak-powershell-config',XDG_DATA_HOME='/tmp/cloak-powershell-data');passed=[]
for case in ['normal','already_original','unknown_version','modified_after','write_failure','tampered_payload','game_open']:
 b=R/'fixture'
 if b.exists():shutil.rmtree(b)
 (b/'system').mkdir(parents=True);(b/'Animations').mkdir();(b/'kit/original').mkdir(parents=True);(b/'system/L2.exe').write_bytes(b'fixture')
 initial=target if case=='already_original' else baseline
 if case=='unknown_version':initial+=b'changed'
 path=b/'system/D3DDrv.dll';path.write_bytes(initial);(b/'Animations/Fighter.ukx').write_bytes(b'test7-anchor-adjusted')
 untouched={'CapasClassic.ini':b'mountsettings','d3d9.dll':b'graphics','CliExt.dll':b'aura-loader','CapasClassic.dll':b'cape-engine','L2modExCore.dll':b'aura-original','MileniumFX.ini':b'vivid-colors'}
 for name,data in untouched.items():(b/'system'/name).write_bytes(data)
 (b/'kit/original/D3DDrv.dll').write_bytes(target if case!='tampered_payload' else b'tampered')
 s=source
 if case=='write_failure':s=s.replace('   [IO.File]::WriteAllBytes($path,$newBytes)',"   [IO.File]::WriteAllText($path,'partial')\n   throw 'injected write failure'")
 if case=='game_open':s=s.replace(" if(Get-Process -Name L2 -ErrorAction SilentlyContinue)"," if($true)")
 (b/'kit/Restaurar.ps1').write_text(s)
 def run(action):
  h=b/'run.ps1';h.write_text("function global:Read-Host {param([string]$Prompt) return ''}\n& '"+str(b/'kit/Restaurar.ps1')+"' -Acao "+action+" -Cliente '"+str(b)+"'\nexit $LASTEXITCODE")
  q=subprocess.run(['/tmp/cloak-powershell/pwsh','-NoProfile','-File',str(h)],env=env,capture_output=True,text=True);(R/(case+'-'+action+'.log')).write_text(q.stdout+q.stderr);return q
 q=run('Aplicar')
 if case in ['unknown_version','write_failure','tampered_payload','game_open']:assert q.returncode!=0,(case,q.stdout+q.stderr);assert path.read_bytes()==initial
 else:
  assert q.returncode==0,(case,q.stdout+q.stderr);assert path.read_bytes()==target;assert run('Aplicar').returncode==0
  if case=='normal':assert run('Desfazer').returncode==0;assert path.read_bytes()==baseline
  elif case=='modified_after':path.write_bytes(target+b'newedit');assert run('Desfazer').returncode!=0;assert path.read_bytes()==target+b'newedit'
 for name,data in untouched.items():assert (b/'system'/name).read_bytes()==data
 assert (b/'Animations/Fighter.ukx').read_bytes()==b'test7-anchor-adjusted'
 # Diagnostic only writes its own report; it preserves the client tree.
 if case=='normal':
  (b/'kit/Verificar.ps1').write_bytes((R/'kit/Verificar.ps1').read_bytes());(b/'system/ContornoDourado.log').write_text('D3DDrv diferente\n')
  h=b/'verify.ps1';h.write_text("function global:Read-Host {param([string]$Prompt) return ''}\n& '"+str(b/'kit/Verificar.ps1')+"' -Cliente '"+str(b)+"'\nexit $LASTEXITCODE")
  v=subprocess.run(['/tmp/cloak-powershell/pwsh','-NoProfile','-File',str(h)],env=env,capture_output=True,text=True);assert v.returncode==0,v.stdout+v.stderr;assert 'D3DDrv diferente' in (b/'kit/Resultado_Aura_A1.txt').read_text(encoding='utf-8-sig');assert path.read_bytes()==baseline
 passed.append(case);print('PASS',case,flush=True);shutil.rmtree(b)
(R/'kit/TESTES.json').write_text(json.dumps({'passed':passed,'diagnostic_read_only_checked':True,'platform':'PowerShell 7 Linux, actual driver bytes, Windows elevation/UI mocked; client not run'},indent=2))
