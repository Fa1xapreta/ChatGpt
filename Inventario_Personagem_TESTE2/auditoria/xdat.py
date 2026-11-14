from pathlib import Path
import struct,re,json
R=Path(__file__).resolve().parent
class Reader:
 def __init__(self,b):self.b=b;self.p=0
 def i(self):v=struct.unpack_from('<i',self.b,self.p)[0];self.p+=4;return v
 def ci(self):
  n=self.b[self.p];self.p+=1;v=n&63;s=6;neg=n&128;more=n&64
  while more:n=self.b[self.p];self.p+=1;v|=(n&127)<<s;s+=7;more=n&128
  return -v if neg else v
 def st(self):
  n=self.ci();assert abs(n)<20000,(self.p,n)
  k=abs(n)*(2 if n<0 else 1);b=self.b[self.p:self.p+k];self.p+=k;return b.decode('utf-16le' if n<0 else 'latin1').rstrip('\0')
 def val(self,t):
  if t=='String':return self.st()
  if t=='float':v=struct.unpack_from('<f',self.b,self.p)[0];self.p+=4;return v
  if t=='boolean':return self.i()
  if t.startswith('List<'):
   n=self.i();assert 0<=n<10000,(self.p,n);return [self.node(None if t=='List<DefaultProperty>' else t[5:-1]) for _ in range(n)]
  return self.i()
 def node(self,typ=None):
  start=self.p
  if typ is None:typ=self.st()
  d={'type':typ,'start':start,'fields':{},'spans':{}}
  def rd(k,t):
   p=self.p;d['fields'][k]=self.val(t);d['spans'][k]=[p,self.p]
  if typ in schema and typ not in ['TabElement','ListElement','ComboBoxElement','Shortcut','WndDefPos','Action']:
   for k,t in [('name','String'),('superName','String'),('unk2','int'),('unk3','int'),('unk4','String'),('unk5','String'),('unk6','String'),('unk7','int'),('size','boolean')]:rd(k,t)
   if d['fields']['size']:
    rd('size_absolute_values','boolean')
    if not d['fields']['size_absolute_values']:
     assert self.ci()==0;rd('size_percent_width','float');rd('size_percent_height','float')
    rd('size_absolute_width','int');rd('size_absolute_height','int')
   rd('anchor','boolean')
   if d['fields']['anchor']:
    for k,t in [('anchor_parent','int'),('anchor_this','int'),('anchor_ctrl','String'),('anchor_x','int'),('anchor_y','int')]:rd(k,t)
   for k,t in [('unk22','int'),('unk23','int'),('unk24','int'),('popupType','String'),('popupValue','int')]:rd(k,t)
  for k,t in schema[typ]:rd(k,t)
  d['end']=self.p;return d
schema={}
for p in (R/'schema').glob('*.groovy'):
 if p.stem in ['DefaultProperty','Alignment','XDAT']:continue
 text=p.read_text();part=text.split('class '+p.stem,1)[1];part=part[part.index('{')+1:]
 # Stop before nested classes, enum declarations or methods.
 part=re.split(r'\n\s*(?:@Override|@Deprecated|enum |static class |String toString|Iterator<)',part)[0]
 fields=[]
 for line in part.splitlines():
  m=re.match(r'\s*(\w+(?:<\w+>)?)\s+(\w+)(?:\s*=.*)?\s*$',line)
  if m:fields.append((m[2],m[1]))
 schema[p.stem]=fields
schema['TabElement']=[('buttonName','int'),('target','String'),('width','int'),('height','int'),('normalTex','String'),('pushedTex','String'),('movable','boolean'),('gap','int'),('tooltip','int'),('noHighlight','int')]
schema['ListElement']=[('textStringId','int'),('width','int'),('unk108','boolean'),('unk109','boolean'),('unk110','boolean')]
schema['ComboBoxElement']=[('sysString','int'),('systemMsg','int'),('text','String'),('reserved','int')]
if __name__=='__main__':
 b=Path('/workspace/attachments/82b9f1aa-c8bb-41a3-a663-ede2cc0958c0/interface.xdat').read_bytes();r=Reader(b);n=r.i();out=[]
 for i in range(n):
  try:out.append(r.node('Window'))
  except Exception as e:print('FAIL',i,r.p,b[r.p-30:r.p+40],e);raise
 (R/'xdat-tree.json').write_text(json.dumps(out,indent=2));print('windows',n,'end',r.p,'total',len(b))
 for d in out:
  if d['fields']['name'] in ['InventoryWnd','StatusWnd','MobInformerWnd']:print(d['fields']['name'],d['start'],d['end'],len(d['fields']['children']))
