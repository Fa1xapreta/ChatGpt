from pathlib import Path
import os,subprocess,shutil,json
R=Path(__file__).resolve().parent;source=(R/'test8/Ajustar.ps1').read_text(encoding='utf-8-sig');a=source.index(' $identity=');z=source.index(' if(Get-Process',a);source=source[:a]+source[z:];original=(R/'uploaded/Fighter.ukx').read_bytes();t7=(R/'Fighter_TESTE7.ukx').read_bytes();target=(R/'Fighter_TESTE8.ukx').read_bytes();env=dict(os.environ,XDG_CACHE_HOME='/tmp/cloak-powershell-cache',XDG_CONFIG_HOME='/tmp/cloak-powershell-config',XDG_DATA_HOME='/tmp/cloak-powershell-data');passed=[]
for case in ['from_test7','from_original','already_applied','unknown_version','modified_after','write_failure','tampered_payload','game_open','bad_backup']:
 b=R/'test8-fixture'
 if b.exists():shutil.rmtree(b)
 (b/'system').mkdir(parents=True);(b/'Animations').mkdir();(b/'kit').mkdir();(b/'system/L2.exe').write_bytes(b'fixture')
 initial=original if case=='from_original' else t7
 if case=='already_applied':initial=target
 if case=='unknown_version':initial=original+b'changed'
 path=b/'Animations/Fighter.ukx';path.write_bytes(initial)
 keep={'D3DDrv.dll':b'aura-original-driver','L2modExCore.dll':b'aura','CapasClassic.dll':b'cape-engine','CapasClassic.ini':b'mount-settings','MileniumFX.ini':b'colors','d3d9.dll':b'graphic-bridge'}
 for name,data in keep.items():(b/'system'/name).write_bytes(data)
 (b/'Animations/Elf.ukx').write_bytes(b'other-race')
 for name in ['alteracao.json','cabecalho.bin','animacoes.bin']:shutil.copy(R/'test8'/name,b/'kit'/name)
 if case=='tampered_payload':p=b/'kit/animacoes.bin';data=bytearray(p.read_bytes());data[0]^=1;p.write_bytes(data)
 if case=='bad_backup':p=b/'Milenium_Humanos_Backup_TESTE8';p.mkdir();(p/'Fighter_original.ukx').write_bytes(b'bad')
 s=source
 if case=='write_failure':s=s.replace('   [IO.File]::WriteAllBytes($path,$newBytes)',"   [IO.File]::WriteAllText($path,'partial')\n   throw 'injected write failure'")
 if case=='game_open':s=s.replace(" if(Get-Process -Name L2 -ErrorAction SilentlyContinue)"," if($true)")
 (b/'kit/Ajustar.ps1').write_text(s)
 def run(action):
  h=b/'run.ps1';h.write_text("function global:Read-Host {param([string]$Prompt) return ''}\n& '"+str(b/'kit/Ajustar.ps1')+"' -Acao "+action+" -Cliente '"+str(b)+"'\nexit $LASTEXITCODE")
  q=subprocess.run(['/tmp/cloak-powershell/pwsh','-NoProfile','-File',str(h)],env=env,capture_output=True,text=True);(R/('test8-'+case+'-'+action+'.log')).write_text(q.stdout+q.stderr);return q
 q=run('Aplicar')
 if case in ['unknown_version','write_failure','tampered_payload','game_open','bad_backup']:assert q.returncode!=0,(case,q.stdout+q.stderr);assert path.read_bytes()==initial
 else:
  assert q.returncode==0,(case,q.stdout+q.stderr);assert path.read_bytes()==target;assert run('Aplicar').returncode==0
  if case=='modified_after':path.write_bytes(target+b'newedit');assert run('Desfazer').returncode!=0;assert path.read_bytes()==target+b'newedit'
  elif case!='already_applied':
   backup=b/'Milenium_Humanos_Backup_TESTE8';assert (backup/'Fighter_antes_TESTE8.ukx').read_bytes()==initial;assert (backup/'Fighter_original.ukx').read_bytes()==original
   assert run('Desfazer').returncode==0;assert path.read_bytes()==original
 for name,data in keep.items():assert (b/'system'/name).read_bytes()==data
 assert (b/'Animations/Elf.ukx').read_bytes()==b'other-race'
 passed.append(case);print('PASS',case,flush=True);shutil.rmtree(b)
(R/'test8/TESTES.json').write_text(json.dumps(dict(passed=passed,platform='PowerShell 7 Linux with original/T7/T8 bytes; Windows elevation/UI mocked; no game runtime test',aura_and_other_race_files_unchanged=True),indent=2))
