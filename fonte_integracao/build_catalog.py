from pathlib import Path
import sys,json,copy,struct,re,csv,subprocess,xml.etree.ElementTree as ET
R=Path(__file__).resolve().parent;A=R.parent/'cloak-audit';M=R.parent/'cloak-migration';sys.path.insert(0,str(R));from armor_codec import parse,record_bytes,U,T
sys.path.insert(0,str(A));from catalog_unreal import catalog
BODIES=['MFighter','FFighter','MDarkElf','FDarkElf','MDwarf','FDwarf','MElf','FElf','MMagic','FMagic','MOrc','FOrc','MShaman','FShaman']
base=(M/'decoded/armorgrp.dat').read_bytes();original=parse(base);reference=parse((A/'decoded/armorgrp.dat').read_bytes());by_ref={x['head'][1]:x for x in reference};template=by_ref[31382]
known=json.loads((A/'reports/cloak-armor-records.json').read_text());native=json.loads((R.parent/'cloak-video/native-catalog.json').read_text());native_map={x['id']:x for x in native};items={x['id']:x for x in json.loads((A/'reports/itemname-records.json').read_text())};rows=[]
for x in known:
 models={body:by_ref[x['id']]['mtx'][i*2]['meshes'][0] for i,body in enumerate(BODIES)}
 if x['id']==31390:
  for body in ['FOrc','MShaman','FShaman']:models[body]='CCFxEMGuardCloak.'+body+'_FxEMGuard_m_ad00'
 rows.append({'id':x['id'],'name':x['name'],'models':models,'icon':x['icons'][0],'provenance':'registro original do DAT de referencia','category':'universal','generated':False})
styles={31300:'FxRogu',31301:'FxSwrdBlue',31302:'FxSwrdDark',31303:'FxArch',31304:'FxEnch',31305:'FxKnig',31306:'FxOlyB',31309:'FxRus2024',31312:'FxWarr'}
icons=catalog(M/'reference-external/systextures/CapasClassicIcons.utx');icon_names=[x['path'] for x in icons['exports'] if x['class']=='Engine.Texture']
for id,style in styles.items():
 icon=next(s for s in icon_names if s.startswith('i_'+str(id)+'_'))
 rows.append({'id':id,'name':native_map[id]['native_label'],'models':{body:'CC'+body+'Festival.'+body+'_'+style+'_m_ad00' for body in BODIES},'icon':'CapasClassicIcons.'+icon,'provenance':'malha Festival original com alias de nome reconhecido pelo motor','category':'universal','generated':True})
for x in native:
 p=x['pattern']
 if not re.fullmatch(r'(e0[0-9]|a0[0-9]|j0[0-8]|jr0|m00|m01|m07|m08|mr0|w00)',p):continue
 suffix=p.upper();package='CCEssenceBody' if suffix=='E04' else 'CCEss'+('' if suffix.startswith('E') else suffix[0])+suffix[1:]
 # E00 -> CCEss00; A00 -> CCEssA00; JR0 -> CCEssJR0.
 if suffix.startswith('E') and suffix!='E04':package='CCEss'+suffix[1:]
 elif suffix!='E04':package='CCEss'+suffix
 d=json.loads((A/'reports/packages'/(package+'.u.json')).read_text());available={e['path'] for e in d['exports'] if e['class']=='Engine.SkeletalMesh'}
 models={body:package+'.'+body+'_'+suffix for body in BODIES if body+'_'+suffix in available};assert models
 rows.append({'id':x['id'],'name':x['native_label'],'models':models,'icon':'CapasClassicIcons.i_ess_temp_cloak','provenance':'malhas existentes e seletor da tabela interna do motor; icone generico do modelo','category':'universal' if len(models)==14 else 'corpos_especificos','generated':True})
rows.sort(key=lambda x:x['id']);assert len(rows)==70 and len({x['id'] for x in rows})==70
existing_names=json.loads((M/'current-itemnames.json').read_text());assert not {x['id'] for x in existing_names}&{x['id'] for x in rows}
assert not {x['head'][1] for x in original}&{x['id'] for x in rows}
# Validate every non-alias model against actual exports, not its filename alone.
for x in rows:
 for body,path in x['models'].items():
  pkg,name=path.split('.',1)
  if x['id'] in styles:
   d=json.loads((A/'reports/packages'/(pkg+'.u.json')).read_text());assert any(e['path']==body+'_FestivalCloth_'+styles[x['id']] for e in d['exports'])
  else:
   d=json.loads((A/'reports/packages'/(pkg+'.u.json')).read_text());assert any(e['path']==name and e['class']=='Engine.SkeletalMesh' for e in d['exports'])
new=[]
for x in rows:
 d=copy.deepcopy(by_ref[x['id']] if not x['generated'] else template);d['head'][1]=x['id'];d['icons'][0]=x['icon'];d['props'][1]=100
 if x['id']==31390:
  for i,body in enumerate(BODIES):d['mtx'][i*2]['meshes']=[x['models'][body]]
 if x['generated']:
  d['mtx']=[{'meshes':[],'materials':[]} for _ in range(31)]
  for i,body in enumerate(BODIES):
   if body in x['models']:d['mtx'][i*2]={'meshes':[x['models'][body]],'materials':list(template['mtx'][0]['materials'])}
 new.append(d)
arm=U(len(original)+len(new))+base[4:-13]+b''.join(record_bytes(d) for d in new)+base[-13:];decoded=parse(arm);assert len(decoded)==1537;assert arm[4:len(base)-13]==base[4:-13]
# Itemname records are appended while all existing bytes remain untouched.
def ci(v):
 neg=v<0;v=abs(v);first=v&63;v>>=6;out=bytearray([first|(128 if neg else 0)|(64 if v else 0)])
 while v:n=v&127;v>>=7;out.append(n|(128 if v else 0))
 return bytes(out)
def ascf(s):b=s.encode('cp1252')+b'\0';return ci(len(b))+b
def item_bytes(x):
 old=items.get(x['id'],items[31382]);name=x['name'];description='Milenium cloak. Available bodies: '+', '.join(x['models'])+'.'
 if not x['generated']:
  # Keep source name/additional name, replace only descriptive catalog text.
  name=old['name']
 return U(x['id'])+T(name)+T(old.get('add_name','') if not x['generated'] else '')+ascf(description)+U(old['popup'])+b''.join(ascf('') for _ in range(4))+bytes.fromhex(old['unk'])+U(old['special_enchant_amount'])+ascf('')
ib=(M/'decoded/itemname-e.dat').read_bytes();iname=U(len(existing_names)+len(rows))+ib[4:-13]+b''.join(item_bytes(x) for x in rows)+ib[-13:];assert iname[4:len(ib)-13]==ib[4:-13]
D=R/'data';D.mkdir(exist_ok=True);(D/'armorgrp.dec').write_bytes(arm);(D/'itemname-e.dec').write_bytes(iname)
exe=str((R.parent/'l2encdec-bin/l2encdec-1.5.1-Linux/l2encdec').resolve())
for n in ['armorgrp','itemname-e']:
 src=D/(n+'.dec');dest=D/(n+'.dat');subprocess.run([exe,'-c','encode','-p','413','-o',str(dest),str(src)],check=True,capture_output=True);verify=D/(n+'.verify');subprocess.run([exe,'-c','decode','-o',str(verify),str(dest)],check=True,capture_output=True);assert verify.read_bytes()==src.read_bytes()
server=R/'server';(server/'itens').mkdir(parents=True,exist_ok=True);root=ET.Element('list')
for x in rows:
 e=ET.SubElement(root,'item',id=str(x['id']),type='Armor',name=x['name'])
 for k,v in [('default_action','equip'),('bodypart','back'),('crystal_type','NONE'),('armor_type','NONE'),('weight','100'),('price','10000'),('is_enchantable','false'),('is_oly_restricted','true')]:ET.SubElement(e,'set',name=k,val=v)
 f=ET.SubElement(e,'for')
 for stat in ['maxHp','maxCp']:ET.SubElement(f,'mul',val='1.05',order='0x30',stat=stat)
ET.indent(root,space='\t');ET.ElementTree(root).write(server/'itens/Milenium_Capas_70.xml',encoding='UTF-8',xml_declaration=True)
allids={x['id'] for x in rows};pending=[dict(x,reason='recurso por corpo nao localizado nos pacotes enviados ou dependencia de animacao externa ausente') for x in native if x['id'] not in allids];assert len(pending)==21
(R/'catalog-active.json').write_text(json.dumps(rows,indent=2));(R/'catalog-pending.json').write_text(json.dumps(pending,indent=2))
with (R/'CATALOGO_CAPAS.csv').open('w',encoding='utf-8-sig',newline='') as f:
 w=csv.writer(f);w.writerow(['ID','Nome','Estado','Corpos','Origem'])
 for x in rows:w.writerow([x['id'],x['name'],x['category'],' '.join(x['models']),x['provenance']])
 for x in pending:w.writerow([x['id'],x['native_label'],'NAO_HABILITADA','','dependencias incompletas; entrada de diagnostico nao e um item pronto'])
print('Catalog: active',len(rows),'universal',sum(len(x['models'])==14 for x in rows),'specific',sum(len(x['models'])<14 for x in rows),'pending',len(pending));print('DATs',len(decoded),len(existing_names)+len(rows),'all existing bytes preserved; encode roundtrips OK')
