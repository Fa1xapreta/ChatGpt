from pathlib import Path
import struct,json,hashlib,sys
R=Path(__file__).resolve().parent;sys.path.insert(0,str(R));from parse_fighter import parse_package
sys.path.insert(0,str(R.parent/'cloak-audit'));from catalog_unreal import Reader,catalog
p=R/'uploaded/Fighter.ukx';original=p.read_bytes();r=Reader(p);skip=r.skip;key=r.key;assert r.format=='Lineage2Ver111' and key==172;r.close();data=bytearray(original);motions=json.loads((R/'Fighter.motion.json').read_text());changes=[]
for a in motions:
 if a['name'] not in ['MFighter_anim','FFighter_anim']:continue
 assert a['bones'][36]['name']=='Cape_Bone'
 for seq,move in zip(a['sequences'],a['moves']):
  if not seq['name'].startswith('Run_'):continue
  idx=move['indices'][36];track=move['tracks'][idx];assert idx==36 and len(track['position'])==1
  off=skip+a['export_offset']+track['position_offset']+4;before=bytes(v^key for v in original[off:off+4]);value=struct.unpack('<f',before)[0];assert value==track['position'][0][1]
  after=bytes(v^key for v in struct.pack('<f',value+4.0));data[off:off+4]=after
  changes.append(dict(animation=a['name'],sequence=seq['name'],bone='Cape_Bone',bone_index=36,component='local_position_Y',offset=off,before=list(original[off:off+4]),after=list(after),old_value=value,new_value=struct.unpack('<f',bytes(v^key for v in after))[0]))
assert len(changes)==12
allowed={i for x in changes for i in range(x['offset'],x['offset']+4)};diff=[i for i,(a,b) in enumerate(zip(original,data)) if a!=b];assert set(diff)<=allowed
candidate=R/'Fighter_TESTE7.ukx';candidate.write_bytes(data);oldcat=catalog(p);newcat=catalog(candidate);assert oldcat['names']==newcat['names'] and oldcat['imports']==newcat['imports'] and oldcat['exports']==newcat['exports']
oldr=Reader(p);newr=Reader(candidate)
for e in oldcat['exports']:
 if e['class']!='Engine.MeshAnimation':assert oldr.read(e['offset'],e['size'])==newr.read(e['offset'],e['size'])
# Parse all candidate animation blocks to their end; then compare every track.
parsed=parse_package(candidate,{a['name'] for a in motions});assert len(parsed)==3
for a in parsed:
 old=next(x for x in motions if x['name']==a['name']);raw=newr.read(a['export_offset'],next(e['size'] for e in newcat['exports'] if e['name']==a['name']))
 from parse_motion import track # import also revalidates the untouched original motion source
 from parse_fighter import Stream
 for seq,m in zip(a['sequences'],a['moves']):
  s=Stream(raw,[],m['offset']);speed=s.floats(3);duration=s.floats(1)[0];start=s.i();flags=s.u();indices=[s.i() for _ in range(s.count())];tracks=[track(s) for _ in range(s.count())];root=track(s);assert s.p==m['offset']+m['bytes']
  oi=seq['index'];om=old['moves'][oi];assert speed==tuple(om['speed']) and duration==om['duration'] and start==om['start_bone'] and flags==om['flags'] and indices==om['indices'];assert a['sequences'][oi]==old['sequences'][oi]
  for j,t in enumerate(tracks):
   ot=om['tracks'][j]
   for field in ['flags','quat','position','time','quat_offset','position_offset','time_offset']:
    val=t[field];val=json.loads(json.dumps(val));ov=ot[field]
    if field=='position' and a['name'] in ['MFighter_anim','FFighter_anim'] and seq['name'].startswith('Run_') and j==36:
     assert len(val)==len(ov)==1 and val[0][0]==ov[0][0] and val[0][2]==ov[0][2] and abs(val[0][1]-ov[0][1]-4)<1e-5
    else:assert val==ov,(a['name'],seq['name'],j,field)
  assert json.loads(json.dumps(root))==om['root']
oldr.close();newr.close()
report=dict(name='TESTE7 human running cape anchor',source_sha256=hashlib.sha256(original).hexdigest(),patched_sha256=hashlib.sha256(data).hexdigest(),file_bytes=len(data),delta_local_Y=4.0,changed_sequences=12,all_sequences=340,changed_tracks='Cape_Bone local Y in human Run sequences only',actual_changed_bytes=len(diff),all_other_tracks_and_sequences_unchanged=True,all_mesh_and_notify_exports_unchanged=True,package_tables_and_sizes_unchanged=True,game_tested=False,reference_running_animations_available=False,changes=changes)
(R/'test7/alteracao.json').write_text(json.dumps(report,indent=2));print('PASS TESTE7:',len(changes),'anchor keys;',len(diff),'changed bytes; all other tracks/meshes/notifications unchanged')
