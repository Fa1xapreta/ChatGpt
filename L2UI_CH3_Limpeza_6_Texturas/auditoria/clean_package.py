from pathlib import Path
import sys,json,struct,hashlib,collections,os
ROOT=Path(__file__).resolve().parent;sys.path.insert(0,'/workspace/work/cloak-audit');from catalog_unreal import Reader,catalog
FILE=ROOT/'L2UI_CH3.u';D=catalog(FILE)
targets={'TitleViwer','head_pvp_3','head_pvp_2','head_pvp_1','CounterAttack_on','board_back5'}
found=[e for e in D['exports'] if e['name'] in targets];assert len(found)==6 and {e['name'] for e in found}==targets
bad=[e for e in D['exports'] if e['name']=='None' and e['flags']&4];assert len(bad)==1 and bad[0]['index']==4721
removed=sorted(found+bad,key=lambda e:e['index']);keep=D['exports'][:removed[0]['index']-1]
assert [e['index'] for e in removed]==list(range(len(keep)+1,len(D['exports'])+1))
assert len(keep)==4715 and all(e['class']=='Engine.Texture' for e in removed)
cut=min(e['offset'] for e in removed);assert max(e['offset']+e['size'] for e in keep)==cut
assert all(e['offset']+e['size']<=cut for e in keep)
for e in keep:
 for k in ['class_ref','super_ref','outer']:assert -len(D['imports'])<=e[k]<=len(keep)
for i in D['imports']:assert -len(D['imports'])<=i['outer']<=len(keep)
r=Reader(FILE);assert r.format=='raw';header=r.read(0,64);nc,no,ec,eo,ic,io=struct.unpack_from('<6I',header,12)
assert io==max(e['offset']+e['size'] for e in removed)
# Keep the original export-row bytes; do not canonicalize or renumber names/references.
pos=eo
for _ in keep:
 _,pos=r.ci(pos);_,pos=r.ci(pos);pos+=4;_,pos=r.ci(pos);pos+=4;size,pos=r.ci(pos)
 if size:_,pos=r.ci(pos)
export_bytes=r.read(eo,pos-eo);imports=r.read(io,eo-io)
# Check references in tagged object/class properties before removing the tail.
properties=collections.Counter();object_refs=0;arrays=collections.Counter()
for e in keep:
 b=r.read(e['offset'],min(e['size'],4096));p=0
 def ci(p):
  n=b[p];p+=1;v=n&63;neg=n&128;more=n&64;shift=6
  while more:n=b[p];p+=1;v|=(n&127)<<shift;shift+=7;more=n&128
  return (-v if neg else v),p
 for _ in range(200):
  n,p=ci(p);name=D['names'][n]
  if name=='None':break
  info=b[p];p+=1;typ=info&15;code=(info>>4)&7
  if typ==10:_,p=ci(p)
  if code<5:size=[1,2,4,12,16][code]
  elif code==5:size=b[p];p+=1
  elif code==6:size=struct.unpack_from('<H',b,p)[0];p+=2
  else:size=struct.unpack_from('<I',b,p)[0];p+=4
  if info&128 and typ!=3:_,p=ci(p)
  if p+size>len(b):b=r.read(e['offset'],e['size'])
  assert p+size<=len(b),(e['path'],name,p,size)
  properties[str(typ)]+=1
  if typ in [5,8]:
   ref,end=ci(p);assert end==p+size,(e['path'],name,size,end-p)
   assert -len(D['imports'])<=ref<=len(keep),(e['path'],name,ref)
   object_refs+=1
  if typ==9:arrays[name]+=1
  p+=size
 else:raise AssertionError(('Unterminated properties',e['path']))
r.close()
# Digest every retained byte with only the four modified summary fields masked.
mask=[(20,28),(32,36),(56,60)]
def prefix_digest(path,n):
 h=hashlib.sha256()
 with path.open('rb') as f:
  first=bytearray(f.read(64))
  for a,b in mask:first[a:b]=bytes(b-a)
  h.update(first);remaining=n-64
  while remaining:
   q=f.read(min(1024*1024,remaining));assert q;h.update(q);remaining-=len(q)
 return h.hexdigest()
def digest(path):
 h=hashlib.sha256()
 with path.open('rb') as f:
  while b:=f.read(1024*1024):h.update(b)
 return h.hexdigest()
before_sha=digest(FILE);before_prefix=prefix_digest(FILE,cut)
assert struct.unpack_from('<I',header,52)[0]==1 and struct.unpack_from('<I',header,56)[0]==ec
with FILE.open('r+b') as f:
 f.seek(cut);f.write(imports);f.write(export_bytes);f.truncate()
 for pos,val in [(20,len(keep)),(24,cut+len(imports)),(32,cut),(56,len(keep))]:f.seek(pos);f.write(struct.pack('<I',val))
 f.flush();os.fsync(f.fileno())
N=catalog(FILE);assert N['names']==D['names'] and N['imports']==D['imports'] and N['exports']==keep
assert not any(e['name'] in targets for e in N['exports'])
assert not any(e['name']=='None' and e['flags']&4 for e in N['exports'])
assert before_prefix==prefix_digest(FILE,cut)
assert len(N['exports'])==4715 and N['classes']=={'Engine.Texture':4570,'Core.Package':145}
critical=['InventoryWnd.MileniumCloakSlot','InventoryWnd.Inventory_EquipSlot','AbnormalWnd.Inventory_DF_CharacterEquipBg']
assert all(any(e['path']==name for e in N['exports']) for name in critical)
report={'source_sha256':before_sha,'result_sha256':digest(FILE),'source_size':317089175,'result_size':FILE.stat().st_size,'source_encoding':'raw','result_encoding':'raw','requested_textures_removed':[e['path'] for e in found],'additional_invalid_public_none_export_removed':bad,'original_export_count':len(D['exports']),'result_export_count':len(keep),'remaining_texture_count':4570,'package_groups_preserved':145,'names_and_imports_identical':True,'all_remaining_export_rows_indices_offsets_and_flags_identical':True,'all_remaining_serialized_bytes_unchanged':True,'retained_prefix_sha256_masking_summary_fields':before_prefix,'object_and_class_property_refs_validated':object_refs,'array_properties_found':dict(arrays),'critical_inventory_resources_preserved':critical,'guid_and_version_unchanged':True,'latest_generation_export_count_updated':True,'invalid_public_none_export_remaining':False,'windows_editor_runtime_tested':False}
(ROOT/'VALIDACAO.json').write_text(json.dumps(report,indent=2));print(json.dumps({k:v for k,v in report.items() if k not in ['additional_invalid_public_none_export_removed']},indent=2))
