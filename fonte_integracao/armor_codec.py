from pathlib import Path
import struct,json
class Reader:
 def __init__(self,b):self.b=b;self.p=0
 def u(self):
  v=struct.unpack_from('<I',self.b,self.p)[0];self.p+=4;return v
 def text(self):
  n=self.u();assert n%2==0 and n<100000,(self.p,n)
  s=self.b[self.p:self.p+n].decode('utf-16le');self.p+=n;return s
 def record(self):
  start=self.p;d={'head':[self.u() for _ in range(7)],'drop':[self.text() for _ in range(6)],'icons':[self.text() for _ in range(5)],'props':[self.u() for _ in range(6)]};d['mtx']=[]
  for _ in range(31):
   n=self.u();assert n<=50,(start,self.p,n);meshes=[self.text() for _ in range(n)];n=self.u();assert n<=50,(start,self.p,n);materials=[self.text() for _ in range(n)];d['mtx'].append({'meshes':meshes,'materials':materials})
  d['effect']=self.text();n=self.u();assert n<=50;d['sounds']=[self.text() for _ in range(n)];d['drop_sound']=self.text();d['equip_sound']=self.text();d['tail']=[self.u() for _ in range(8)];d['_start']=start;d['_end']=self.p;return d
U=lambda x:struct.pack('<I',x)
def T(s):b=s.encode('utf-16le');return U(len(b))+b
def record_bytes(d):
 b=b''.join(U(x) for x in d['head'])+b''.join(T(s) for s in d['drop']+d['icons'])+b''.join(U(x) for x in d['props'])
 for a in d['mtx']:
  b+=U(len(a['meshes']))+b''.join(T(s) for s in a['meshes'])+U(len(a['materials']))+b''.join(T(s) for s in a['materials'])
 return b+T(d['effect'])+U(len(d['sounds']))+b''.join(T(s) for s in d['sounds'])+T(d['drop_sound'])+T(d['equip_sound'])+b''.join(U(x) for x in d['tail'])
def parse(b):
 r=Reader(b);n=r.u();rows=[r.record() for _ in range(n)];assert b[r.p:]==b'\x0cSafePackage\0',(r.p,len(b),b[r.p:r.p+16]);assert U(n)+b''.join(record_bytes(d) for d in rows)+b[r.p:]==b;return rows
if __name__=='__main__':
 for path in ['work/cloak-audit/decoded/armorgrp.dat','work/cloak-migration/decoded/armorgrp.dat']:
  try:r=parse(Path(path).read_bytes());print(path,len(r),'fully parsed, byte roundtrip OK')
  except Exception as e:print(path,type(e).__name__,str(e));raise
