from pathlib import Path
import struct,json
R=Path(__file__).resolve().parent
class Reader:
 def __init__(self,b):self.b=b;self.p=0
 def u(self):v=struct.unpack_from('<I',self.b,self.p)[0];self.p+=4;return v
 def text(self):
  n=self.u();assert n%2==0 and n<100000;(s,)=struct.unpack_from(f'<{n}s',self.b,self.p);self.p+=n;return s.decode('utf-16le').rstrip('\0')
 def arr(self,n):return [self.text() for _ in range(n)]
def parse(path):
 r=Reader(path.read_bytes());rows=[]
 for _ in range(15):
  start=r.p;d={'face_icon':r.text()};ns=[r.u() for _ in range(4)]
  for key,n in zip(['hair_mesh','hair_tex','face_mesh','face_tex'],ns):d[key]=r.arr(n)
  d['body_mesh']=r.arr(4);d['body_tex']=r.arr(4);d['attack_eff']=r.text();d['walkanimframe']=r.u();ns=[r.u() for _ in range(3)]
  for key,n in zip(['snd_att','snd_def','snd_dmg'],ns):d[key]=r.arr(n)
  for key in ['voice_snd_hand','voice_snd_1hs','voice_snd_2hs','voice_snd_dual','voice_snd_pole','voice_snd_bow','voice_snd_unknown','voice_snd_fist']:d[key]=r.arr(r.u())
  d['_start']=start;d['_end']=r.p;rows.append(d)
 print(path,len(path.read_bytes()),'parsed',r.p,'tail',r.b[r.p:].hex());return rows
ref=parse(R.parent/'cloak-audit/decoded/chargrp.dat');cur=parse(R.parent/'cloak-migration/decoded/chargrp.dat');diff=[]
for i,(a,b) in enumerate(zip(ref,cur)):
 d={k:{'model':a[k],'current':b[k]} for k in a if not k.startswith('_') and a[k]!=b[k]};diff.append({'index':i,'face_icon':a['face_icon'],'changes':d});print(i,a['face_icon'],'walkframe',a['walkanimframe'],b['walkanimframe'],'diff',list(d))
(R/'chargrp-differences.json').write_text(json.dumps(diff,indent=2,ensure_ascii=False));(R/'chargrp-model.json').write_text(json.dumps(ref,indent=2));(R/'chargrp-current.json').write_text(json.dumps(cur,indent=2))
