from pathlib import Path
import sys,json,struct
R=Path(__file__).resolve().parent;sys.path.insert(0,str(R));from verify_test4 import Decoder,getscript,D,new,old
from xdat import Reader
fn=next(e for e in D['exports'] if e['path']=='InventoryWnd.OnClickButton');script,size=getscript(new,fn);original_fn=next(e for e in old['exports'] if e['path']==fn['path']);original=Path('/workspace/attachments/6e516f3b-8810-4af1-8516-fe9e4339c975/interface.u').read_bytes();oldscript,oldsize=getscript(original,original_fn)
# Decode real bytecode first, recording expanded offset -> serialized offset.
mapping={};oldtoken=Decoder.token
def tok(self):mapping[self.v]=self.p;return oldtoken(self)
Decoder.token=tok;de=Decoder(script)
while de.p<len(script):de.token()
ROOT='MileniumInventoryPreviewWnd'
class VM:
 def __init__(self,key,opened=False,actor=True,hud=False,background=True):self.key=key;self.opened=opened;self.actor=actor;self.actions=[];self.p=len(oldscript);self.stopped=False;self.hud=hud;self.background=background;self.pawn='display-pawn' if opened else None
 def read(self,n):x=script[self.p:self.p+n];self.p+=n;return x
 def compact(self):r=Reader(script);r.p=self.p;n=r.ci();self.p=r.p;return n
 def args(self):
  out=[]
  while script[self.p]!=0x16:out.append(self.expr())
  self.read(1);return out
 def expr(self,ctx=None):
  op=self.read(1)[0]
  if op==0:return self.key if D['exports'][self.compact()-1]['name']=='strID' else None
  if op==1:
   n=self.compact();field=D['exports'][n-1]['name']
   if field=='PortraitExpanded':return self.hud
   if field=='PortraitPawn':return self.pawn
   raise AssertionError(field)
  if op==0x2d:return self.expr(ctx)
  if op==0x2e:
   cls=self.compact();obj=self.expr()
   if cls>0:assert D['exports'][cls-1]['name']=='StatusWnd' and obj==('script','StatusWnd')
   else:assert D['imports'][-cls-1]['name']=='Texture'
   return obj
  if op==0x14:
   # Validate the exact serialized BoolProperty lvalue before applying simulated assignment.
   assert self.read(1)==b'\x19';obj=self.expr();assert obj==('script','StatusWnd');self.read(3)
   assert self.read(2)==b'\x2d\x01';n=self.compact();assert D['exports'][n-1]['path']=='StatusWnd.PortraitExpanded'
   self.hud=self.expr();self.actions.append(('PortraitExpanded',self.hud));return None
  if op in [0x27,0x28]:return op==0x27
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
   if name=='GetScript':assert a==['StatusWnd'];return ('script','StatusWnd')
   if name=='UpdateOptionalPortrait':
    assert self.hud;self.pawn='display-pawn' if self.actor else None;self.actions.append(('PrepareDisplayPawn',));return None
   if name=='ReleaseOptionalPortrait':self.pawn=None;self.actions.append(('ReleaseDisplayPawn',));return None
   if name=='DynamicLoadObject':
    assert a==['L2UI_CH3.AbnormalWnd.Inventory_DF_CharacterEquipBg',('class','Texture'),True];return ('texture',a[0]) if self.background else None
   if name=='SetStageBackground':self.actions.append((name,a));return int(a[0] is not None)
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
   elif op==6:self.p+=1;self.p=mapping[struct.unpack('<H',self.read(2))[0]]
   elif op==4:self.p+=1;self.expr();return
   else:self.expr()
  raise AssertionError('did not return')
cases=[]
for hud in [False,True]:
 for key,opened,actor in [('btnMileniumPreview',False,True),('btnMileniumPreview',True,True),('unhandled',False,True),('btnMileniumPreview',False,False)]:
  v=VM(key,opened,actor,hud);v.run();assert v.hud==hud
  if key=='unhandled':assert not v.actions
  elif opened:
   assert not v.opened
   assert ('ReleaseDisplayPawn',) in v.actions if not hud else ('ReleaseDisplayPawn',) not in v.actions
  else:
   assert v.opened
   bind=next(x for x in v.actions if x[0]=='BindPreview');assert bind[1][1]==('display-pawn' if actor else None) and bind[1][2:]==[35.0,30.0,100.0]
   assert v.actions[-1][0]=='SetText' and v.actions[-1][2]==['Render 3D: '+('1' if actor else '-3')+' / Fundo: 1']
  cases.append({'button':key,'initially_open':opened,'player_available':actor,'hud_initial':hud,'hud_final':v.hud,'actions':v.actions})
v=VM('btnMileniumPreview',False,True,False,False);v.run();assert v.actions[-1][2]==['Render 3D: 1 / Fundo: 0'];cases.append({'missing_texture_case':True,'actions':v.actions})
(R/'TESTE4/TESTE_BOTAO.json').write_text(json.dumps({'real_bytecode_interpreted':True,'game_window_and_native_apis_mocked':True,'cases':cases},indent=2));print('PASS: 9 real bytecode cases; imported background path, missing-texture fallback, independent display pawn, preserved HUD state, cleanup, missing player and unrelated buttons')
