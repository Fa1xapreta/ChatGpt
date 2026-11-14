from pathlib import Path
import json,sys
R=Path(__file__).resolve().parent;sys.path.insert(0,str(R));from parse_fighter import Stream
sys.path.insert(0,str(R.parent/'cloak-audit'));from catalog_unreal import Reader
p=R/'uploaded/Fighter.ukx';r=Reader(p);parsed=json.loads((R/'Fighter.parsed.json').read_text());result=[]
def track(s):
 flags=s.u();nq=s.count();qo=s.p;q=[s.floats(4) for _ in range(nq)];np=s.count();po=s.p;pos=[s.floats(3) for _ in range(np)];nt=s.count();to=s.p;t=s.floats(nt);return dict(flags=flags,quat=q,position=pos,time=t,quat_offset=qo,position_offset=po,time_offset=to)
for a in parsed:
 if a['class_name']!='Engine.MeshAnimation':continue
 raw=r.read(a['export_offset'],next(e['size'] for e in json.load(open(R/'Fighter.catalog.json'))['exports'] if e['name']==a['name']));moves=[]
 for m in a['moves']:
  s=Stream(raw,[],m['offset']);speed=s.floats(3);duration=s.floats(1)[0];startbone=s.i();flags=s.u();indices=[s.i() for _ in range(s.count())];tracks=[track(s) for _ in range(s.count())];root=track(s);assert s.p==m['offset']+m['bytes']
  moves.append(dict(speed=speed,duration=duration,start_bone=startbone,flags=flags,indices=indices,tracks=tracks,root=root))
 result.append(dict(name=a['name'],bones=a['bones'],sequences=a['sequences'],moves=moves,export_offset=a['export_offset']))
 for seq,m in zip(a['sequences'],moves):
  if seq['name'].startswith('Run_'):
   idx=m['indices'][36] if len(m['indices'])>36 else None;print(a['name'],seq['name'],'indices',len(m['indices']),'tracks',len(m['tracks']),'capeindex',idx,'cape',m['tracks'][idx] if idx is not None and idx>=0 else None)
r.close()
# Save compact JSON; arrays represent the actual serialized input, not inferred animation curves.
(R/'Fighter.motion.json').write_text(json.dumps(result,separators=(',',':')))
