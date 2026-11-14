from pathlib import Path
import sys,struct,json,math
R=Path(__file__).resolve().parent;sys.path.insert(0,str(R.parent/'cloak-audit'))
from catalog_unreal import Reader,catalog
from export_textures import ci,props
class Stream:
 def __init__(self,b,names,p=0):self.b=b;self.names=names;self.p=p
 def skip(self,n):assert n>=0 and self.p+n<=len(self.b),(self.p,n,len(self.b));self.p+=n
 def ci(self):v,self.p=ci(self.b,self.p);return v
 def count(self):n=self.ci();assert 0<=n<1000000,(self.p,n);return n
 def u(self):n=struct.unpack_from('<I',self.b,self.p)[0];self.p+=4;return n
 def i(self):n=struct.unpack_from('<i',self.b,self.p)[0];self.p+=4;return n
 def floats(self,n):v=struct.unpack_from('<'+'f'*n,self.b,self.p);self.p+=n*4;assert all(math.isfinite(x) for x in v);return v
 def name(self):i=self.ci();assert 0<=i<len(self.names),(self.p,i);return self.names[i]
 def arrskip(self,size):n=self.count();self.skip(n*size);return n
 def refs(self):return [self.ci() for _ in range(self.count())]
def mesh(b,names):
 end,pr=props(b,names);s=Stream(b,names,end);s.skip(41);ver=s.i();verts=s.i();assert 1<ver<=6,ver;s.arrskip(4);textures=s.refs();scale=s.floats(3);origin=s.floats(3);rot=[s.i() for _ in range(3)]
 s.arrskip(2);s.arrskip(8);s.arrskip(2);s.arrskip(10);s.arrskip(8);s.skip(24)
 if ver>=3:s.skip(4);s.ci();s.skip(12+12+12+4+12)
 if ver>=4:s.skip(4)
 if ver>=5:s.skip(4)
 if ver>=6:s.skip(1)
 npoints=s.arrskip(12);bone_start=s.p;n=s.count();assert 1<=n<=256,(s.p,n);bones=[]
 for i in range(n):
  off=s.p;name=s.name();flags=s.u();q=s.floats(4);pos=s.floats(3);length=s.floats(1)[0];size=s.floats(3);children=s.i();parent=s.i();assert 0<=parent<n,(name,parent);assert 0<=children<n
  bones.append(dict(index=i,name=name,flags=flags,quat=q,position=pos,length=length,size=size,children=children,parent=parent,relative_offset=off))
 anim=s.ci();return dict(version=ver,vertices=verts,scale=scale,origin=origin,rotation=rot,bones=bones,bones_offset=bone_start,animation_ref=anim,parsed_to=s.p)
def anim(b,names,absolute):
 end,pr=props(b,names);s=Stream(b,names,end);ver=s.i();bones=[]
 for i in range(s.count()):bones.append(dict(index=i,name=s.name(),flags=s.u(),parent=s.i()))
 skip=s.u();n=s.count();moves=[]
 for i in range(n):
  stop=s.u();start=s.p;assert absolute+start<=stop<=absolute+len(b);s.p=stop-absolute;moves.append(dict(offset=start,bytes=s.p-start))
 assert absolute+s.p==skip
 seqs=[]
 for i in range(s.count()):
  start=s.p;f28=s.floats(1)[0];name=s.name();groups=[s.name() for _ in range(s.count())];startframe=s.i();frames=s.i();notifies=[]
  for j in range(s.count()):notifies.append(dict(time=s.floats(1)[0],function=s.name(),object=s.ci()))
  rate=s.floats(1)[0];s.skip(12);s.ci();s.skip(8) # lineage license30 f2c,f30,f34,obj,f3c,f40
  s.skip(1);s.arrskip(8)
  for j in range(s.count()):s.skip(4);s.arrskip(8)
  s.skip(8);s.arrskip(8)
  seqs.append(dict(index=i,name=name,groups=groups,start_frame=startframe,frames=frames,rate=rate,notifies=notifies,offset=start,bytes=s.p-start))
 assert s.p==len(b),(s.p,len(b));assert len(seqs)==len(moves),(len(seqs),len(moves));return dict(version=ver,bones=bones,moves=moves,sequences=seqs)
def parse_package(path,which=None):
 d=catalog(path);r=Reader(path);out=[]
 for e in d['exports']:
  if which and e['name'] not in which:continue
  if e['class'] not in ['Engine.SkeletalMesh','Engine.MeshAnimation']:continue
  raw=r.read(e['offset'],e['size'])
  try:
   parsed=mesh(raw,d['names']) if e['class']=='Engine.SkeletalMesh' else anim(raw,d['names'],e['offset']);out.append(dict(name=e['name'],class_name=e['class'],export_offset=e['offset'],**parsed))
  except Exception as ex:raise RuntimeError((path.name,e['name'],str(ex))) from ex
 r.close();return out
if __name__=='__main__':
 p=R/'uploaded/Fighter.ukx';d=parse_package(p);(R/'Fighter.parsed.json').write_text(json.dumps(d,indent=2));print('Parsed',len(d),'mesh/animation exports')
 for x in d:
  if x['class_name']=='Engine.MeshAnimation':print(x['name'],len(x['bones']),'bones',len(x['sequences']),'sequences')
