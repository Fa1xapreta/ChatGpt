from pathlib import Path
import sys,json,struct
R=Path(__file__).resolve().parent;sys.path.insert(0,str(R));from verify_test import Decoder,getscript,D,new,old
from xdat import Reader
fn=next(e for e in D['exports'] if e['path']=='InventoryWnd.OnClickButton');script,size=getscript(new,fn);original_fn=next(e for e in old['exports'] if e['path']==fn['path']);original=Path('/workspace/attachments/6e516f3b-8810-4af1-8516-fe9e4339c975/interface.u').read_bytes();oldscript,oldsize=getscript(original,original_fn)
# Decode real bytecode first, recording expanded offset -> serialized offset.
mapping={};oldtoken=Decoder.token
def tok(self):mapping[self.v]=self.p;return oldtoken(self)
Decoder.token=tok;de=Decoder(script)
while de.p<len(script):de.token()
ROOT='MileniumInventoryPreviewWnd'
class VM:
 def __init__(self,key,opened=False,actor=True):self.key=key;self.opened=opened;self.actor=actor;self.actions=[];self.p=len(oldscript);self.stopped=False
 def read(self,n):x=script[self.p:self.p+n];self.p+=n;return x
 def compact(self):r=Reader(script);r.p=self.p;n=r.ci();self.p=r.p;return n
 def args(self):
  out=[]
  while script[self.p]!=0x16:out.append(self.expr())
  self.read(1);return out
 def expr(self,ctx=None):
  op=self.read(1)[0]
  if op==0:return self.key if D['exports'][self.compact()-1]['name']=='strID' else None
  if op==0x1f:
   z=b''
   while True:
    c=self.read(1)
    if c==b'\0':return z.decode()
    z+=c
  if op in [0x1d,0x1e]:return struct.unpack('<i' if op==0x1d else '<f',self.read(4))[0]
  if op==0x20:
   n=self.compact();return ('class',D['exports'][n-1]['name'] if n>0 else D['imports'][-n-1]['name'])
  if op in [0x12,0x19]:
   obj=self.expr();self.read(3);return self.expr(obj)
  if op in [0x1b,0x1c]:
   n=self.compact();name=D['names'][n] if op==0x1b else D['exports'][n-1]['name'] if n>0 else D['imports'][-n-1]['name'];a=self.args()
   if name=='GetHandle':return ('window',a[0])
   if name=='GetPlayerID':return 1377
   if name=='GetWorldActor':assert a==[1377];return 'player' if self.actor else None
   if name=='IsShowWindow':assert ctx==('window',ROOT);return self.opened
   if name=='BindPreview':self.actions.append((name,a));return 1 if self.actor else -3
   if name=='ReleasePreview':self.actions.append((name,a));return 1
   if name in ['ShowWindow','HideWindow','SetAnchor','SetText']:
    self.actions.append((name,ctx,a))
    if ctx==('window',ROOT) and name in ['ShowWindow','HideWindow']:self.opened=name=='ShowWindow'
    return None
   raise AssertionError((name,a,ctx))
  if op in [0x7a,0x70]:
   a=self.expr();b=self.expr();assert self.read(1)==b'\x16';return a==b if op==0x7a else a+str(b)
  if op==0x39:
   assert self.read(1)==b'\x53';return str(self.expr())
  if op==0x0b:return None
  raise AssertionError((hex(op),self.p))
 def run(self):
  for _ in range(100):
   op=script[self.p]
   if op==7:
    self.p+=1;target=struct.unpack('<H',self.read(2))[0]
    if not self.expr():self.p=mapping[target]
   elif op==4:self.p+=1;self.expr();return
   else:self.expr()
  raise AssertionError('did not return')
cases=[]
for key,opened,actor in [('btnMileniumPreview',False,True),('btnMileniumPreview',True,True),('unhandled',False,True),('btnMileniumPreview',False,False)]:
 v=VM(key,opened,actor);v.run()
 if key=='unhandled':assert not v.actions
 elif opened:assert [a[0] for a in v.actions]==['ReleasePreview','HideWindow'] and not v.opened
 else:
  assert v.opened
  bind=next(x for x in v.actions if x[0]=='BindPreview');assert bind[1][1]==('player' if actor else None) and bind[1][2:]==[35.0,145.0,45.0]
  assert v.actions[-1][0]=='SetText' and v.actions[-1][2]==['Render 3D: '+('1' if actor else '-3')]
 cases.append({'button':key,'initially_open':opened,'player_available':actor,'actions':v.actions})
(R/'TESTE2/TESTE_BOTAO.json').write_text(json.dumps({'real_bytecode_interpreted':True,'game_window_and_native_apis_mocked':True,'cases':cases},indent=2));print('PASS: actual bytecode toggles open/close, ignores unrelated buttons, binds live player and displays missing-player status')
