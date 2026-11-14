from pathlib import Path
import json,math,hashlib,sys,struct
R=Path(__file__).resolve().parent;W=R.parent;f=json.loads((R/'Fighter.parsed.json').read_text());motion=json.loads((R/'Fighter.motion.json').read_text());cape=json.loads((R/'human-skeleton-comparison.json').read_text());meshes={x['name']:x for x in f if x['class_name']=='Engine.SkeletalMesh'};out=[]
sys.path.insert(0,str(W/'cloak-audit/vendor'));import pefile
native=pefile.PE(str(W/'cloak-audit/extracted/system/CapasClassic.dll'));image=native.get_memory_mapped_image()
for body,slot in [('MFighter',1),('FFighter',0)]:
 anim=next(x for x in motion if x['name']==body+'_anim');profile=struct.unpack_from('<I',image,0x7a290+4*slot)[0]-native.OPTIONAL_HEADER.ImageBase
 indices=struct.unpack_from('<6I',image,profile+0x28);leg_names=[anim['bones'][i]['name'] for i in indices];expected=struct.unpack_from('<I',image,profile+8)[0];assert expected==len(anim['bones']);assert leg_names==['Bip01_L_Thigh','Bip01_L_Calf','bip01_l_foot','Bip01_R_Thigh','Bip01_R_Calf','bip01_r_foot']
 comparisons=[];wanted=set(indices)|{1,2,3,4,36,37}
 for item in cape:
  if not item['mesh'].startswith(body+'_DynCover_'):continue
  for suffix in ['m000_f','m000_u','m001_u']:
   actual=meshes[body+'_'+suffix];same=len(actual['bones'])==len(item['bone_list']);maxpos=0;maxangle=0;names_and_parents=True
   for i in wanted:
    a=actual['bones'][i];b=item['bone_list'][i];assert a['name'].lower()==b['name'].lower() and a['parent']==b['parent'];maxpos=max(maxpos,math.dist(a['position'],b['position']))
    qa=a['quat'];qb=b['quat'];dot=abs(sum(x*y for x,y in zip(qa,qb)))/math.sqrt(sum(x*x for x in qa)*sum(x*x for x in qb));maxangle=max(maxangle,math.degrees(2*math.acos(min(1,dot))))
   assert same and maxpos<0.0001 and maxangle<0.001,(maxpos,maxangle)
   comparisons.append(dict(source_package=item['package'],source_mesh=item['mesh'],current_mesh=actual['name'],same_leg_spine_cape_names_indices_parents=True,max_bind_translation_delta=maxpos,max_normalized_quaternion_angle_degrees=maxangle))
 runs=[]
 for seq,move in zip(anim['sequences'],anim['moves']):
  if not seq['name'].startswith('Run_'):continue
  assert len(move['indices'])==len(anim['bones']);assert len(move['tracks'])==len(anim['bones']);assert move['indices'][36]==36
  t=move['tracks'][36];assert len(t['position'])==len(t['quat'])==1
  runs.append(dict(name=seq['name'],frames=seq['frames'],rate=seq['rate'],cape_bone_index=36,cape_track_position=t['position'][0],cape_track_quaternion=t['quat'][0]))
 out.append(dict(body=body,animation=anim['name'],bones=len(anim['bones']),native_expected_bones=expected,animation_sequences=len(anim['sequences']),native_leg_indices=list(indices),native_leg_names=leg_names,cape_anchor_index=36,running_sequences=runs,comparisons=comparisons))
summary={'input_file':'Fighter.ukx','input_origin':'current client with TESTE 5, confirmed by user','sha256':hashlib.sha256((R/'uploaded/Fighter.ukx').read_bytes()).hexdigest(),'package_version':123,'licensee':30,'skeletal_mesh_exports':143,'animation_exports':3,'all_animation_payloads_parsed_to_end':True,'bodies':out,'root_cause_confirmed':False,'notes':['The original model patch contains no base Fighter.ukx to compare running tracks byte-for-byte.','Leg skeleton compatibility does not prove dynamic cloth clearance.','Cape cloth rigs have their own bone count; they were not treated as identical to the body rig.','No client file was modified. TESTE 6 remains withdrawn.']}
(R/'Fighter_RESUMO.json').write_text(json.dumps(summary,indent=2));print('PASS: 18 bind-pose comparisons, all leg/anchor indices, native bone counts, 12 running sequences; no changes to uploaded file')
