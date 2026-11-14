from pathlib import Path
import sys,json,struct,math
R=Path(__file__).resolve().parent;sys.path.insert(0,str(R));from parse_fighter import Stream,parse_package
sys.path.insert(0,str(R.parent/'cloak-audit'));from catalog_unreal import Reader,catalog
p=R/'Fighter_TESTE8.ukx';c=catalog(p);r=Reader(p);old={x['name']:x for x in json.load(open(R/'Fighter.motion.json'))};parsed=parse_package(p,set(old));changed=0;unchanged=0

def track(s):
 flags=s.u();q=[s.floats(4) for _ in range(s.count())];pos=[s.floats(3) for _ in range(s.count())];t=list(s.floats(s.count()));return dict(flags=flags,quat=[list(x) for x in q],position=[list(x) for x in pos],time=t)
for a in parsed:
 original=old[a['name']];e=next(e for e in c['exports'] if e['name']==a['name']);raw=r.read(e['offset'],e['size'])
 for seq,m,om in zip(a['sequences'],a['moves'],original['moves']):
  s=Stream(raw,[],m['offset']);speed=s.floats(3);duration=s.floats(1)[0];start=s.i();flags=s.u();indices=[s.i() for _ in range(s.count())];tracks=[track(s) for _ in range(s.count())];root=track(s);assert s.p==m['offset']+m['bytes']
  assert list(speed)==om['speed'] and duration==om['duration'] and start==om['start_bone'] and flags==om['flags'] and indices==om['indices']
  for i,t in enumerate(tracks):
   prior={k:v for k,v in om['tracks'][i].items() if not k.endswith('_offset')}
   target=i==36 and a['name'] in ['MFighter_anim','FFighter_anim'] and seq['name'].startswith('Run_')
   if target:
    assert t['position']==prior['position'] and t['flags']==prior['flags'];assert len(t['quat'])==len(t['time'])==seq['frames'];assert t['time']==list(range(seq['frames']))
    base=prior['quat'][0];baseN=math.sqrt(sum(x*x for x in base))
    for q in t['quat']:
     assert abs(sum(x*x for x in q)-1)<1e-6;angle=math.degrees(2*math.acos(min(1,abs(sum(x*y for x,y in zip(q,base)))/baseN)));assert angle<6.001
    changed+=1
   else:assert t==prior,(a['name'],seq['name'],i);unchanged+=1
  assert root=={k:v for k,v in om['root'].items() if not k.endswith('_offset')}
r.close();assert changed==12;print('PASS: 12 moving cape rotations; anchor positions exact; all',unchanged,'other tracks and all root tracks exact; all lazy pointers and sequence payloads valid')
(R/'test8/VALIDACAO.json').write_text(json.dumps(dict(changed_cape_tracks=changed,unchanged_other_tracks=unchanged,original_anchor_positions=True,all_root_motion_preserved=True,all_move_skip_pointers_valid=True,game_tested=False),indent=2))
