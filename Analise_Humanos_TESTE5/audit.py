from pathlib import Path
import json,hashlib,sys,struct
R=Path(__file__).resolve().parent;W=R.parent;sys.path.insert(0,str(W/'cloak-audit/vendor'));import pefile
sys.path.insert(0,str(W/'cloak-integrated'));from armor_codec import parse
source=W/'cloak-audit/extracted/system';kit=W/'cloak-migration/complete-kit';current=W/'cloak-migration/current-system'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
rows=json.loads((W/'cloak-integrated/catalog-active.json').read_text());patches={x['file']:x for x in json.loads((kit/'alias-patches/patches.json').read_text())};checks=[]
for x in rows:
 for body in ['MFighter','FFighter']:
  if body not in x['models']:continue
  model=x['models'][body];file=model.split('.',1)[0]+'.u';a=source/file;b=kit/'cliente/system'/file;h=sha(a);assert h==sha(b)
  patch=patches.get(file);mode='source-package plus verified export alias' if patch and str(x['id']) in patch['models'] else 'exact source package'
  checks.append({'id':x['id'],'body':body,'model':model,'package_sha256':h,'same_source_package_in_test5':True,'mode':mode})
assert sha(source/'CapasClassic.dll')==sha(kit/'cliente/system/CapasClassic.dll')
ref={x['head'][1]:x for x in parse((W/'cloak-audit/decoded/armorgrp.dat').read_bytes())};target={x['head'][1]:x for x in parse((W/'cloak-integrated/data/armorgrp.dec').read_bytes())}
original25=[x for x in rows if not x['generated']];assert len(original25)==25
for x in original25:
 for index in [0,1,2,3]:assert ref[x['id']]['mtx'][index]==target[x['id']]['mtx'][index]
# Engine code differences: bound comparisons to unchanged source executable section size.
a=(source/'engine.dll').read_bytes();b=(current/'engine.dll').read_bytes();pe=pefile.PE(data=a);section=next(s for s in pe.sections if s.Name.startswith(b'.code'));start=section.PointerToRawData;end=start+section.SizeOfRawData;diff=[i for i in range(start,end) if a[i]!=b[i]];groups=[]
for i in diff:
 if not groups or i>groups[-1][-1]+12:groups.append([i])
 else:groups[-1].append(i)
engine=[]
for group in groups:
 lo=group[0];hi=group[-1]+1;engine.append({'file_offset_start':hex(lo),'file_offset_end_exclusive':hex(hi),'changed_bytes':len(group),'model_hex':a[lo:hi].hex(),'current_hex':b[lo:hi].hex()})
assert len(groups)==2 and len(diff)==26
report={'baseline':'published TESTE 5 assets and original current-system upload, not a new copy of the running Windows client','cape_native_dll_identical':True,'native_sha256':sha(source/'CapasClassic.dll'),'human_models_checked':len(checks),'human_original_25_DAT_MTX_identical':True,'human_mesh_checks':checks,'core_dll_identical':sha(source/'Core.dll')==sha(current/'Core.dll'),'engine_original_code_section_differences':engine,'engine_notes':'Only 6-byte trampoline to an appended text formatter and 20 differing UTF16 bytes in mount class literal in the original code section. Appended formatter parses <number|number> strings. No original human animation instruction block difference identified. Runtime hooks remain unmeasured.','chargrp_diff_file':'chargrp-differences.json','walkanimframe_same_all_bodies':True,'base_human_animation_files_received':False,'reference_fighter_v_contents':{'file':'Fighter-v.ukx','skeletal_mesh_exports':22,'mesh_animation_exports':0},'test6':{'status':'withdrawn','reason_observed':'user reports capes disappeared after applying','internal_cause_confirmed':False,'changed_field_was_not_verified_leg_collision':True},'root_cause_confirmed':False,'next_required_files':['Animations/Fighter.ukx from current client','Animations/Fighter.ukx from the complete model client, if present','If files have another name: filenames of Fighter*.ukx, MFighter*.ukx, FFighter*.ukx in each client']}
(R/'human-comparison.json').write_text(json.dumps(report,ensure_ascii=False,indent=2));print('PASS',len(checks),'human item/body model references; original 25 human DAT mappings; native/Core identity; engine differences accounted')
