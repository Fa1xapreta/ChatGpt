from pathlib import Path
import os,subprocess,shutil,json
R=Path(__file__).resolve().parent;source=(R/'test7/Ajustar.ps1').read_text(encoding='utf-8-sig');a=source.index(' $identity=');z=source.index(' if(Get-Process',a);source=source[:a]+source[z:];baseline=(R/'uploaded/Fighter.ukx').read_bytes();candidate=(R/'Fighter_TESTE7.ukx').read_bytes();env=dict(os.environ,XDG_CACHE_HOME='/tmp/cloak-powershell-cache',XDG_CONFIG_HOME='/tmp/cloak-powershell-config',XDG_DATA_HOME='/tmp/cloak-powershell-data');passed=[]
for case in ['normal','unknown_version','modified_after','write_failure','bad_descriptor','game_open']:
 b=R/'test7-fixtures'
 if b.exists():shutil.rmtree(b)
 (b/'system').mkdir(parents=True);(b/'Animations').mkdir();(b/'kit').mkdir();(b/'system/L2.exe').write_bytes(b'fixture')
 initial=baseline if case!='unknown_version' else baseline+b'changed'
 path=b/'Animations/Fighter.ukx';path.write_bytes(initial)
 unchanged={'CapasClassic.ini':b'mountsettings','d3d9.dll':b'graphics','CliExt.dll':b'aura','CapasClassic.dll':b'cape engine'}
 for name,data in unchanged.items():(b/'system'/name).write_bytes(data)
 s=source
 if case=='write_failure':s=s.replace('   [IO.File]::WriteAllBytes($path,$newBytes)',"   [IO.File]::WriteAllText($path,'partial')\n   throw 'injected write failure'")
 if case=='game_open':s=s.replace(" if(Get-Process -Name L2 -ErrorAction SilentlyContinue)"," if($true)")
 (b/'kit/Ajustar.ps1').write_text(s);spec=json.loads((R/'test7/alteracao.json').read_text())
 if case=='bad_descriptor':spec['changes'][0]['after'][0]^=1
 (b/'kit/alteracao.json').write_text(json.dumps(spec))
 def run(action):
  h=b/'run.ps1';h.write_text("function global:Read-Host {param([string]$Prompt) return ''}\n& '"+str(b/'kit/Ajustar.ps1')+"' -Acao "+action+" -Cliente '"+str(b)+"'\nexit $LASTEXITCODE")
  q=subprocess.run(['/tmp/cloak-powershell/pwsh','-NoProfile','-File',str(h)],env=env,capture_output=True,text=True);(R/('test7-'+case+'-'+action+'.log')).write_text(q.stdout+q.stderr);return q
 q=run('Aplicar')
 if case in ['unknown_version','write_failure','bad_descriptor','game_open']:assert q.returncode!=0,(case,q.stdout+q.stderr);assert path.read_bytes()==initial
 else:
  assert q.returncode==0,(case,q.stdout+q.stderr);assert path.read_bytes()==candidate;assert run('Aplicar').returncode==0
  if case=='modified_after':path.write_bytes(candidate+b'newedit');assert run('Desfazer').returncode!=0;assert path.read_bytes()==candidate+b'newedit'
  else:assert run('Desfazer').returncode==0;assert path.read_bytes()==baseline
 for name,data in unchanged.items():assert (b/'system'/name).read_bytes()==data
 passed.append(case);print('PASS',case,flush=True)
 shutil.rmtree(b)
(R/'test7/TESTES.json').write_text(json.dumps({'passed':passed,'platform':'PowerShell 7 Linux, Windows elevation/UI mocked; actual Fighter input; no Windows game test'},indent=2))
