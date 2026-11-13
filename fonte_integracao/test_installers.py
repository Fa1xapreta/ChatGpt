from pathlib import Path
import os,json,hashlib,subprocess,shutil,xml.etree.ElementTree as ET
K=Path('/workspace/work/cloak-migration/complete-kit');T=Path('/workspace/work/cloak-integrated/fixtures');T.mkdir(exist_ok=True)
env=dict(os.environ,XDG_CACHE_HOME='/tmp/cloak-powershell-cache',XDG_CONFIG_HOME='/tmp/cloak-powershell-config',XDG_DATA_HOME='/tmp/cloak-powershell-data');ps='/tmp/cloak-powershell/pwsh'
def sha(b):return hashlib.sha256(b).hexdigest()
def run(script,args=''):
 p=script.parent/'harness.ps1';p.write_text("function global:Read-Host {param([string]$Prompt) return ''}\n& '"+str(script)+"' "+args+"\nexit $LASTEXITCODE\n")
 r=subprocess.run([ps,'-NoLogo','-NoProfile','-File',str(p)],env=env,text=True,capture_output=True);(script.parent/'last-test.txt').write_text(r.stdout+r.stderr);return r

def strip(s):
 if 'function Elevate-IfNeeded' in s:
  start=s.index('function Elevate-IfNeeded');end=s.index('\ntry{\n Elevate-IfNeeded',start);# installer includes Apply-Alias between elevation and main
  if 'function Apply-Alias' in s[start:end]:end=s.index('function Apply-Alias',start)
  return s[:start]+'function Elevate-IfNeeded {}\n'+s[end:]
 start=s.index(' $identity=');end=s.index(' if(Get-Process -Name L2',start);return s[:start]+s[end:]
results=[]
for name,mode in [('original','base'),('existing_assets','base'),('modified_asset','modified'),('missing_asset','missing'),('tamper','tamper'),('alias_failure','failure'),('compatibility','compat')]:
 b=T/name
 if b.exists():shutil.rmtree(b)
 kit=b/'kit';root=b/'client';(kit/'suporte').mkdir(parents=True);(root/'system').mkdir(parents=True);(kit/'alias-patches').mkdir()
 baseline=b'H'*64+b'PAYLOAD'*20;after=b'N'*64+baseline[64:]+b'APPEND'
 payload={'cliente/system/d3d9.dll':b'newbridge','cliente/system/CliExt.dll':b'newcliext','cliente/system/armorgrp.dat':b'newdat','cliente/system/CapasClassic.ini':b'defaultmount','cliente/system/MileniumFX.ini':b'defaultcolor','cliente/system/model.u':baseline}
 initial={'system/L2.exe':b'fixture','system/Engine.dll':b'engine','system/d3d9.dll':b'oldgfx','system/CliExt.dll':b'oldcli','system/armorgrp.dat':b'olddat','system/CapasClassic.ini':b'approvedmount','system/MileniumFX.ini':b'vividcolors','system/model.u':baseline,'system/mount.u':b'keepmount','system/dsetup_base.dll':b'oldcape'}
 if mode=='modified':initial['system/model.u']=b'CUSTOM'*40
 if mode=='missing':del initial['system/model.u']
 for f,data in initial.items():p=root/f;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(data)
 for f,data in payload.items():p=kit/f;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(data)
 for f,data in [('new.head',b'N'*64),('new.append',b'APPEND')]: (kit/'alias-patches'/f).write_bytes(data)
 (kit/'alias-patches/patches.json').write_text(json.dumps([dict(file='model.u',before=sha(baseline),after=sha(after),original_bytes=len(baseline),header_file='new.head',append_file='new.append')]))
 (kit/'cliente_aceito.json').write_text(json.dumps({'Engine.dll':[sha(b'engine')],'d3d9.dll':[sha(b'oldgfx')],'CliExt.dll':[sha(b'oldcli')]}))
 for f in ['Restaurar.ps1','ModoCompativel.ps1']:(kit/'suporte'/f).write_text(strip((K/'suporte'/f).read_text(encoding='utf-8-sig')))
 for f in ['RESTAURAR.bat','MODO_COMPATIVEL.bat']:(kit/'suporte'/f).write_text('fixture')
 for f,data in [('d3d9_modo_capas.dll',b'compatgfx'),('CliExt_modo_capas.dll',b'compatcli')]:(kit/'suporte'/f).write_bytes(data)
 s=strip((K/'Instalar.ps1').read_text(encoding='utf-8-sig'))
 if mode=='failure':s=s.replace('$stream.Flush()','$stream.Flush();throw "injected partial alias failure"')
 (kit/'Instalar.ps1').write_text(s)
 manifest=[dict(file=p.relative_to(kit).as_posix(),sha256=sha(p.read_bytes())) for p in kit.rglob('*') if p.is_file()];(kit/'arquivos_sha256.json').write_text(json.dumps(manifest))
 if mode=='tamper':(kit/'cliente/system/armorgrp.dat').write_bytes(b'bad')
 r=run(kit/'Instalar.ps1',"-Cliente '"+str(root)+"'")
 if mode in ('tamper','failure'):assert r.returncode!=0,(name,r.stdout,r.stderr)
 else:
  assert r.returncode==0,(name,r.stdout,r.stderr)
  assert (root/'system/model.u').read_bytes()==after
  assert (root/'system/MileniumFX.ini').read_bytes()==b'vividcolors'
  assert (root/'system/CapasClassic.ini').read_bytes()==b'approvedmount'
  backup=next(root.glob('Milenium_Integrado_Backup_*'))
  if mode=='compat':
   q=run(backup/'ModoCompativel.ps1');assert q.returncode==0,q.stdout+q.stderr;assert (root/'system/d3d9.dll').read_bytes()==b'compatgfx'
  q=run(backup/'Restaurar.ps1','-SemPausa');assert q.returncode==0,q.stdout+q.stderr
 for f,data in initial.items():assert (root/f).read_bytes()==data,(name,f)
 if mode=='missing':assert not(root/'system/model.u').exists()
 results.append(name);print('PASS',name)
# Server scenarios: old cape rows, unrelated definitions, collision protection, repeat, exact undo.
for name in ['new','old25','mixed','collision']:
 b=T/('xml_'+name)
 if b.exists():shutil.rmtree(b)
 kit=b/'kit';items=b/'items';kit.mkdir(parents=True);items.mkdir();(kit/'itens').mkdir()
 source=(K/'servidor/itens/Milenium_Capas_70.xml').read_bytes();(kit/'itens/Milenium_Capas_70.xml').write_bytes(source)
 doc=ET.fromstring(source);old=list(doc)[:25];other=ET.Element('item',{'id':'31100','name':'keep','type':'Armor'})
 if name=='old25':(items/'old.xml').write_bytes(ET.tostring(ET.Element('list'))[:-3]+b'') if False else None
 if name in ['old25','mixed','collision']:
  d=ET.Element('list');d.extend(old)
  if name=='mixed':d.append(other)
  if name=='collision':old[0].set('type','Weapon')
  (items/'old.xml').write_bytes(ET.tostring(d))
 initial={p.name:p.read_bytes() for p in items.glob('*')}
 (kit/'Xml.ps1').write_text(strip((K/'servidor/Xml.ps1').read_text(encoding='utf-8-sig')))
 r=run(kit/'Xml.ps1',"-Itens '"+str(items)+"'")
 if name=='collision':assert r.returncode!=0,r.stdout+r.stderr
 else:
  assert r.returncode==0,(name,r.stdout,r.stderr)
  assert len(ET.parse(items/'Milenium_Capas_70.xml').getroot())==70
  if name=='mixed':assert len(ET.parse(items/'old.xml').getroot())==1
  q=run(kit/'Xml.ps1',"-Itens '"+str(items)+"'");assert q.returncode==0,q.stdout+q.stderr
  q=run(kit/'Xml.ps1','-Acao Desfazer');assert q.returncode==0,q.stdout+q.stderr
 assert {p.name:p.read_bytes() for p in items.glob('*')}==initial,name
 results.append('xml_'+name);print('PASS xml',name)
(T.parent/'installer-test-results.json').write_text(json.dumps({'passed':results,'platform':'PowerShell 7 Linux fixtures; Windows elevation/UI mocked; game execution not tested'},indent=2))
