from pathlib import Path
import sys,struct,json,copy,re,hashlib
R=Path(__file__).resolve().parent;sys.path.insert(0,str(R));from xdat import Reader,schema
sys.path.insert(0,'/workspace/work/cloak-audit/vendor');import pefile
U=Path('/workspace/attachments/6e516f3b-8810-4af1-8516-fe9e4339c975/interface.u').read_bytes(); X=Path('/workspace/attachments/82b9f1aa-c8bb-41a3-a663-ede2cc0958c0/interface.xdat').read_bytes();D=json.loads((R/'interface-catalog.json').read_text())
OUT=R/'FINAL26';(OUT/'system').mkdir(parents=True,exist_ok=True);(OUT/'Classes').mkdir(exist_ok=True)
ASSET='L2UI_CH3.AbnormalWnd.Inventory_DF_CharacterEquipBg'
ROOT='MileniumInventoryPreviewWnd'; VIEW=ROOT+'.MobViewport';BUTTON='btnMileniumPreview';HEADER='MileniumInventoryPreviewHeaderWnd'
def ci(v):
 neg=v<0;v=abs(v);a=[(v&63)|(128 if neg else 0)];v>>=6
 if v:a[0]|=64
 while v:a.append((v&127)|(128 if v>>7 else 0));v>>=7
 return bytes(a)
def st(v):
 if not v:return b'\0'
 b=v.encode('latin1')+b'\0';return ci(len(b))+b
def rdci(b,p):
 r=Reader(b);r.p=p;n=r.ci();return n,r.p
# Every generated expression tracks its expanded in-memory bytecode length.
class E:
 def __init__(self,b,n=None):self.b=b;self.n=len(b) if n is None else n
 def __add__(self,x):return E(self.b+x.b,self.n+x.n)
def seq(*a):
 z=E(b'')
 for x in a:z=z+x
 return z
def raw(b):return E(b)
def ref(op,n):return E(bytes([op])+ci(n),5)
def string(s):return raw(b'\x1f'+s.encode('ascii')+b'\0')
def fl(v):return raw(b'\x1e'+struct.pack('<f',v))
def name(s):return D['names'].index(s)
def vf(s,*args):return seq(E(b'\x1b'+ci(name(s)),5),*args,raw(b'\x16'))
def ff(n,*args):return seq(E(b'\x1c'+ci(n),5),*args,raw(b'\x16'))
def exp(s):return next(x['index'] for x in D['exports'] if x['path']==s)
def imp(s):return next(-i-1 for i,x in enumerate(D['imports']) if x['name']==s)
def context(obj,fn,size=0,cl=False):return seq(raw(bytes([0x12 if cl else 0x19])),obj,raw(struct.pack('<HB',fn.n,size)),fn)
def static(cl,fn,*args):return context(ref(0x20,exp(cl) if any(x['path']==cl for x in D['exports']) else imp(cl)),ff(exp(cl+'.'+fn),*args) if any(x['path']==cl+'.'+fn for x in D['exports']) else vf(fn,*args),4,True)
def handle(s):return ff(-27,string(s))
def callwindow(s,fn,*args,size=0):return context(handle(s),ff(imp(fn),*args),size)
def show(s):return callwindow(s,'ShowWindow')
def hide(s):return callwindow(s,'HideWindow')
def visible(s):return callwindow(s,'IsShowWindow',size=4)
def release():return static('MobPreviewNative','ReleasePreview')
def jif(n,cond):return seq(raw(b'\x07'+struct.pack('<H',n)),cond)
RET=raw(b'\x04\x0b')
class Code:
 def __init__(self,start=0):self.parts=[];self.n=start;self.labels={};self.fix=[]
 def put(self,e):self.parts.append(e);self.n+=e.n
 def label(self,s):self.labels[s]=self.n
 def ifnot(self,cond,label):self.fix.append((len(self.parts),label));self.put(jif(0,cond))
 def done(self):
  for i,k in self.fix:
   b=bytearray(self.parts[i].b);struct.pack_into('<H',b,1,self.labels[k]);self.parts[i]=E(bytes(b),self.parts[i].n)
  return seq(*self.parts)
changes={}
def function(s):
 e=next(x for x in D['exports'] if x['path']==s);b=U[e['offset']:e['offset']+e['size']];p=0
 for _ in range(7):_,p=rdci(b,p)
 p+=8;sz=struct.unpack_from('<I',b,p)[0];return e,b,p,p+4,sz
# Reuse a display Pawn that already has the HUD appearance-copy and idle-pose logic.
def status():return seq(ref(0x2e,exp('StatusWnd')),ff(imp('GetScript'),string('StatusWnd')))
def statusprop(n,boolprop=False):
 member=ref(1,exp('StatusWnd.'+n))
 if boolprop:member=seq(raw(b'\x2d'),member)
 return context(status(),member,4)
def statuscall(fn):return context(status(),ff(exp('StatusWnd.'+fn)),0)
def expanded(v):return seq(raw(b'\x14'),context(status(),seq(raw(b'\x2d'),ref(1,exp('StatusWnd.PortraitExpanded'))),0),raw(b'\x27' if v else b'\x28'))
def prepare(c,prefix):
 pass # The preview uses the real Pawn; no display-Pawn preparation or idle override.
def previewbutton(opened):
 tex='L2UI_CH3.ShortcutWnd.AS_ShortcutWnd_'+('DEL' if opened else 'ADD')
 obj=seq(ref(0x2e,imp('ButtonHandle')),handle('InventoryWnd.'+BUTTON))
 return seq(context(obj,ff(-138,string(tex),string(tex+'_Over'),string(tex+'_Over'))),context(obj,ff(imp('SetTooltipCustomType'),ff(exp('InventoryWnd.SetTooltip'),string('Inventory Viewr')))))
def cleanup(c,prefix):
 c.put(hide(ROOT));c.put(release());c.put(previewbutton(False))
def bindclone():
 actor=static('MobPreviewNative','GetWorldActor',static('UIDATA_PLAYER','GetPlayerID'))
 return static('MobPreviewNative','BindPreview',handle(VIEW),actor,fl(25),fl(30),fl(100))
def background():
 texture=seq(ref(0x2e,imp('Texture')),ff(imp('DynamicLoadObject'),string(ASSET),ref(0x20,imp('Texture')),raw(b'\x27')))
 return static('MobPreviewNative','SetStageBackground',texture)
def textbind():
 # Native side effects must not depend on the diagnostic TextBox being usable.
 def buttontex(n,tex):
  obj=seq(ref(0x2e,imp('ButtonHandle')),handle(ROOT+'.'+n))
  return context(obj,ff(-138,string('L2Devil.Log.'+tex),string('L2Devil.Log.'+tex+'_Down'),string('L2Devil.Log.'+tex+'_Over')))
 return seq(bindclone(),background(),buttontex('RotateLeft','Inventory_DF_Btn_RotateRight'),buttontex('RotateRight','Inventory_DF_Btn_RotateLeft'),show(ROOT+'.RotateLeft'),show(ROOT+'.RotateRight'))
# Default switch case jumps to appended handler. Existing branch offsets are unchanged.
e,b,sp,cp,sz=function('InventoryWnd.OnClickButton');code=b[cp:-7];assert code.endswith(b'\x0a\xff\xff\x06'+struct.pack('<H',sz-2)+b'\x04\x0b')
c=Code(sz);c.ifnot(seq(raw(b'\x7a'),ref(0,exp('InventoryWnd.OnClickButton.strID')),string(BUTTON),raw(b'\x16')),'return')
c.ifnot(visible(ROOT),'open');cleanup(c,'button_');c.put(RET);c.label('open')
c.ifnot(seq(raw(b'\x77'),static('MobPreviewNative','GetWorldActor',static('UIDATA_PLAYER','GetPlayerID')),raw(b'\x2a'),raw(b'\x16')),'return')
c.put(hide('MobInformerWnd'));c.put(show(VIEW));c.put(callwindow(ROOT,'SetAnchor',string('InventoryWnd'),string('TopLeft'),string('TopRight'),raw(b'\x1d'+struct.pack('<i',0)),raw(b'\x1d'+struct.pack('<i',33))))
prepare(c,'button_');c.put(show(ROOT));c.put(textbind());c.put(previewbutton(True))
c.label('return');c.put(RET);tail=c.done();code=code[:-5]+b'\x06'+struct.pack('<H',sz)+code[-2:]+tail.b
new=bytearray(b[:cp]);struct.pack_into('<I',new,sp,sz+tail.n);new+=code+b[-7:];changes[e['index']]=bytes(new)
# Cleanup replaces the final return, so existing branches to that return enter cleanup.
for s in ['InventoryWnd.OnLoad','InventoryWnd.OnShow','InventoryWnd.OnHide','MobInformerWnd.OnShow','InventoryWnd.OnEvent']:
 e,b,sp,cp,sz=function(s);assert b[-9:-7]==b'\x04\x0b'
 c=Code(sz-2)
 if s=='InventoryWnd.OnEvent':
  cond=seq(raw(b'\x9a'),ref(0,exp('InventoryWnd.OnEvent.Event_ID')),raw(b'\x1d'+struct.pack('<i',40)),raw(b'\x16'))
  c.ifnot(cond,'check_hidden');cleanup(c,'restart_');c.put(RET)
  c.label('check_hidden');c.ifnot(visible('InventoryWnd'),'inventory_hidden');c.put(RET)
  c.label('inventory_hidden');c.ifnot(visible(ROOT),'return');cleanup(c,'hidden_')
 elif s=='InventoryWnd.OnLoad':c.put(hide(ROOT));c.put(previewbutton(False))
 else:cleanup(c,'hide_')
 c.label('return');c.put(RET);tail=c.done()
 new=bytearray(b[:cp]);struct.pack_into('<I',new,sp,sz-2+tail.n);new+=b[cp:-9]+tail.b+b[-7:];changes[e['index']]=bytes(new)
# Append updated objects and rebuild only the export table; all untouched serial data stays in place.
u=bytearray(U);table=[]
for e in D['exports']:
 e=copy.deepcopy(e)
 if e['index'] in changes:e['offset']=len(u);e['size']=len(changes[e['index']]);u+=changes[e['index']]
 table.append(e)
eo=len(u)
for e in table:
 u+=ci(e['class_ref'])+ci(e['super_ref'])+struct.pack('<i',e['outer'])+ci(name(e['name']))+struct.pack('<I',e['flags'])+ci(e['size'])
 if e['size']:u+=ci(e['offset'])
struct.pack_into('<I',u,24,eo);(OUT/'system/interface.u').write_bytes(u)
# Modify the installed, working XDAT, preserving its native viewport geometry.
X=(R/'TESTE22/system/interface.xdat').read_bytes()
def encval(t,v):
 if t=='String':return st(v)
 if t=='float':return struct.pack('<f',v)
 if t.startswith('List<'):return struct.pack('<i',len(v))+b''.join(encnode(x,t=='List<DefaultProperty>') for x in v)
 return struct.pack('<i',v)
def encnode(d,typed=True):
 f=d['fields'];typ=d['type'];first=next(iter(d['spans'].values()))[0];z=st(typ) if typed else b'';cursor=first
 for k,(a,b) in d['spans'].items():
  z+=X[cursor:a];cursor=b
  v=f[k];ov=original_fields[(d['start'],k)]
  if not isinstance(v,list) and v==ov:z+=X[a:b];continue
  if isinstance(v,list):z+=struct.pack('<i',len(v))+b''.join(encnode(n,k=='children') for n in v);continue
  if isinstance(v,str):z+=st(v)
  elif isinstance(v,float):z+=struct.pack('<f',v)
  else:z+=struct.pack('<i',v)
 return z
rd=Reader(X);count=rd.i();windows=[rd.node('Window') for _ in range(count)];wend=rd.p
original_fields={}
def remember(n):
 for k,v in n['fields'].items():
  original_fields[(n['start'],k)]=copy.deepcopy(v)
  if isinstance(v,list):
   for ch in v:remember(ch)
for w in windows:remember(w)
for w in windows:
 a=encnode(w,False);b=X[w['start']:w['end']]
 if a!=b:
  q=next((i for i in range(min(len(a),len(b))) if a[i]!=b[i]),-1);print('DIFF',w['fields']['name'],len(a),len(b),q,a[max(0,q-5):q+12].hex(),b[max(0,q-5):q+12].hex());raise AssertionError()

inv=next(w for w in windows if w['fields']['name']=='InventoryWnd')
root=next(w for w in windows if w['fields']['name']==ROOT)
button=next(c for c in inv['fields']['children'] if c['fields']['name']==BUTTON)
button['fields'].update(size_absolute_width=32,size_absolute_height=32,normalTex='L2UI_CH3.ShortcutWnd.AS_ShortcutWnd_ADD',pushedTex='L2UI_CH3.ShortcutWnd.AS_ShortcutWnd_ADD_Over',highlightTex='L2UI_CH3.ShortcutWnd.AS_ShortcutWnd_ADD_Over')
# Final version: no decorative strip; keep the approved title position.
title=next(c for c in root['fields']['children'] if c['fields']['name']=='PreviewTitle')
title['fields']['anchor_y']=6
x=struct.pack('<i',count)+b''.join(encnode(w,False) for w in windows)+X[wend:]
(OUT/'system/interface.xdat').write_bytes(x)
print('Built final UI without title strip; all other working controls preserved')
