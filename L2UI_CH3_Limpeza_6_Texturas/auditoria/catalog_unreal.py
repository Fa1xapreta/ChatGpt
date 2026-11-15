from pathlib import Path
import mmap,struct,json,collections,re
ROOT=Path(__file__).resolve().parent
class Reader:
 def __init__(self,p):
  self.file=p.open('rb');self.m=mmap.mmap(self.file.fileno(),0,access=mmap.ACCESS_READ);self.skip=0;self.key=None;self.format='raw'
  if self.m[:8]==b'L\0i\0n\0e\0':
   self.format=self.m[:28].decode('utf-16le',errors='replace');self.skip=28
   if self.format.endswith('111'):self.key=172
   elif self.format.endswith('121'):self.key=sum(p.name.lower().encode('ascii'))&255
   else:raise ValueError('encrypted '+self.format)
  if self.read(0,4)!=b'\xc1\x83\x2a\x9e':raise ValueError('unsupported signature '+self.m[:12].hex())
 def read(self,p,n):
  if p<0 or p+n>len(self.m)-self.skip:raise ValueError('read outside package')
  x=self.m[p+self.skip:p+self.skip+n]
  return bytes(c^self.key for c in x) if self.key is not None else x
 def u32(self,p):return struct.unpack('<I',self.read(p,4))[0]
 def i32(self,p):return struct.unpack('<i',self.read(p,4))[0]
 def ci(self,p):
  n=self.read(p,1)[0];p+=1;v=n&63;negative=n&128;more=n&64;shift=6;k=0
  while more:
   n=self.read(p,1)[0];p+=1;v|=(n&127)<<shift;shift+=7;more=n&128;k+=1
   if k>4:raise ValueError('compact integer overflow')
  return (-v if negative else v),p
 def close(self):self.m.close();self.file.close()
def catalog(p):
 r=Reader(p)
 try:
  h=r.read(0,36);version,licensee=struct.unpack_from('<HH',h,4);nc,no,ec,eo,ic,io=struct.unpack_from('<6I',h,12)
  assert nc<200000 and ec<200000 and ic<200000
  names=[];pos=no
  for _ in range(nc):
   n,pos=r.ci(pos);assert abs(n)<20000
   if n<0:raw=r.read(pos,-n*2);pos-=n*2;s=raw.decode('utf-16le',errors='replace').rstrip('\0')
   else:raw=r.read(pos,n);pos+=n;s=raw.decode('latin1').rstrip('\0')
   flags=r.u32(pos);pos+=4;names.append(s)
  imports=[];pos=io
  for _ in range(ic):
   cp,pos=r.ci(pos);cl,pos=r.ci(pos);outer=r.i32(pos);pos+=4;n,pos=r.ci(pos)
   imports.append({'package_class':names[cp],'class':names[cl],'outer':outer,'name':names[n]})
  exports=[];pos=eo
  for i in range(ec):
   cl,pos=r.ci(pos);su,pos=r.ci(pos);outer=r.i32(pos);pos+=4;n,pos=r.ci(pos);flags=r.u32(pos);pos+=4;size,pos=r.ci(pos);off=0
   if size:off,pos=r.ci(pos)
   exports.append({'index':i+1,'class_ref':cl,'super_ref':su,'outer':outer,'name':names[n],'flags':flags,'size':size,'offset':off})
  def resolve(ref,seen=None):
   if ref==0:return ''
   seen=set() if seen is None else seen
   if ref in seen:return '?cycle'
   seen.add(ref);d=(exports[ref-1] if ref>0 else imports[-ref-1]);outer=resolve(d['outer'],seen)
   return (outer+'.' if outer else '')+d['name']
  for i,e in enumerate(exports):e['path']=resolve(i+1);e['class']=resolve(e['class_ref']) if e['class_ref'] else 'Core.Class';e['super']=resolve(e['super_ref'])
  return {'file':p.name,'status':'parsed','encoding':r.format,'version':version,'licensee':licensee,'names':names,'imports':imports,'exports':exports,'classes':dict(collections.Counter(e['class'] for e in exports))}
 finally:r.close()
if __name__=='__main__':
 folder=ROOT/'reports/packages';folder.mkdir(exist_ok=True)
 summary=[]
 for p in sorted((ROOT/'extracted/system').glob('*.u')):
  try:d=catalog(p);(folder/(p.name+'.json')).write_text(json.dumps(d,ensure_ascii=True,indent=2));row={k:v for k,v in d.items() if k not in ['names','imports','exports']};row.update(names=len(d['names']),imports=len(d['imports']),exports=len(d['exports']))
  except Exception as e:row={'file':p.name,'status':'not_decoded','reason':str(e)}
  summary.append(row)
  if any(x in p.name.lower() for x in ['lucdev','classic','dragoncloak','ccfxemit','ccmfighter']):print(row,flush=True)
 (ROOT/'reports/package-summary.json').write_text(json.dumps(summary,indent=2))
 print('Cataloged',len(summary),'packages; parsed',sum(d['status']=='parsed' for d in summary),flush=True)
