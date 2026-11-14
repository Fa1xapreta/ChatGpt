from pathlib import Path
import sys,struct,json,copy,re,hashlib
R=Path(__file__).resolve().parent;sys.path.insert(0,str(R));from xdat import Reader,schema
sys.path.insert(0,'/workspace/work/cloak-audit/vendor');import pefile
U=Path('/workspace/attachments/6e516f3b-8810-4af1-8516-fe9e4339c975/interface.u').read_bytes(); X=Path('/workspace/attachments/82b9f1aa-c8bb-41a3-a663-ede2cc0958c0/interface.xdat').read_bytes();D=json.loads((R/'interface-catalog.json').read_text())
OUT=R/'TESTE1';(OUT/'system').mkdir(parents=True,exist_ok=True);(OUT/'Classes').mkdir(exist_ok=True)
ROOT='MileniumInventoryPreviewWnd'; VIEW=ROOT+'.MobViewport';BUTTON='btnMileniumPreview'
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
# Default switch case jumps to appended handler. Existing branch offsets are unchanged.
e,b,sp,cp,sz=function('InventoryWnd.OnClickButton');code=b[cp:-7];assert code.endswith(b'\x0a\xff\xff\x06'+struct.pack('<H',sz-2)+b'\x04\x0b')
c=Code(sz);c.ifnot(seq(raw(b'\x7a'),ref(0,exp('InventoryWnd.OnClickButton.strID')),string(BUTTON),raw(b'\x16')),'return')
c.ifnot(visible(ROOT),'open');c.put(release());c.put(hide(ROOT));c.put(RET);c.label('open')
c.put(hide('MobInformerWnd'));c.put(show(ROOT));c.put(show(VIEW));c.put(callwindow(ROOT,'SetAnchor',string('InventoryWnd'),string('TopLeft'),string('TopRight'),raw(b'\x1d'+struct.pack('<i',-6)),raw(b'\x1d'+struct.pack('<i',33))))
actor=static('MobPreviewNative','GetWorldActor',static('UIDATA_PLAYER','GetPlayerID'))
bind=static('MobPreviewNative','BindPreview',handle(VIEW),actor,fl(35),fl(145),fl(45))
# Keep the status/result visible for feedback; the native renderer remains responsible for scene drawing.
concat=seq(raw(b'\x70'),string('Render 3D: '),raw(b'\x53'),bind,raw(b'\x16'))
c.put(context(handle(ROOT+'.RenderStatus'),vf('SetText',concat)))
c.label('return');c.put(RET);tail=c.done();code=code[:-5]+b'\x06'+struct.pack('<H',sz)+code[-2:]+tail.b
new=bytearray(b[:cp]);struct.pack_into('<I',new,sp,sz+tail.n);new+=code+b[-7:];changes[e['index']]=bytes(new)
# Cleanup replaces the final return, so existing branches to that return enter cleanup.
for s in ['InventoryWnd.OnHide','MobInformerWnd.OnShow']:
 e,b,sp,cp,sz=function(s);assert b[-9:-7]==b'\x04\x0b'
 c=Code(sz-2);c.ifnot(visible(ROOT),'return');c.put(release());c.put(hide(ROOT));c.label('return');c.put(RET);tail=c.done()
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
# XDAT serializer retains all original nodes byte-for-byte, and writes only newly cloned controls.
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
inv=next(w for w in windows if w['fields']['name']=='InventoryWnd');mob=next(w for w in windows if w['fields']['name']=='MobInformerWnd')
btn=copy.deepcopy(next(x for x in inv['fields']['children'] if x['fields']['name']=='btnInfo'));btn['fields'].update(name=BUTTON,anchor_parent=1,anchor_this=1,anchor_ctrl='',anchor_x=15,anchor_y=36,size_absolute_width=16,size_absolute_height=16)
# Recipe icon is present in the user's texture package and is kept deliberately small.
btn['fields'].update(normalTex='L2UI_CH3.MenuWnd.Icon_Character',pushedTex='L2UI_CH3.MenuWnd.Icon_Character_Down',highlightTex='L2UI_CH3.MenuWnd.Icon_Character_Over')
inv['fields']['children'].append(btn)
win=copy.deepcopy(mob);win['fields'].update(name=ROOT,unk102='undefined',unk101='undefined',size_absolute_width=270,size_absolute_height=429,anchor_parent=1,anchor_this=3,anchor_ctrl='InventoryWnd',anchor_x=-6,anchor_y=33,unk104=0,unk107=0,unk111=-9999,unk122=0,children=[])
vp=copy.deepcopy(next(x for x in mob['fields']['children'] if x['fields']['name']=='MobViewport'));vp['fields'].update(unk4=ROOT,anchor_x=4,anchor_y=26,size_absolute_width=262,size_absolute_height=375)
label=copy.deepcopy(next(x for x in inv['fields']['children'] if x['type']=='TextBox'));label['fields'].update(name='RenderStatus',unk4=ROOT,unk7=1,size_absolute_width=260,size_absolute_height=20,anchor_parent=1,anchor_this=1,anchor_ctrl='',anchor_x=5,anchor_y=405,text='Personagem - TESTE 1',textAlign=2,sysstring=-9999,systemMsg=-9999,textColor=-1,autosize=0)
# Use an existing dark panel texture from the provided interface as background.
bg=copy.deepcopy(next(x for x in mob['fields']['children'] if x['type']=='Texture'));bg['fields'].update(name='PanelBackground',unk4=ROOT,anchor_ctrl='',anchor_parent=1,anchor_this=1,anchor_x=0,anchor_y=0,size_absolute_width=270,size_absolute_height=429,file='L2UI_CH3.AutoPotions.Autopotion_Panel',uSize=32.0,vSize=32.0,alpha=255,layer=2)
title=copy.deepcopy(label);title['fields'].update(name='PreviewTitle',anchor_y=3,text='Personagem',size_absolute_height=20)
win['fields']['children']=[bg,title,vp,label]
x=struct.pack('<i',count+1)+b''.join(encnode(w,False) for w in windows)+encnode(win,False)+X[wend:];(OUT/'system/interface.xdat').write_bytes(x)
# Extend ONLY the parent-window name check for the existing mob viewport.
# Keep original HUD, browser, monster bindings and exports. New code is PIC (ASLR-safe).
p=Path('/workspace/attachments/4bd198f4-d35d-488f-88d6-785bfc6b168f/MileniumPortraitNative.dll');nb=bytearray(p.read_bytes());pe=pefile.PE(data=nb)
align=lambda n,a:(n+a-1)//a*a
last=pe.sections[-1];rva=last.VirtualAddress+align(last.Misc_VirtualSize,16);rawptr=last.PointerToRawData+align(last.Misc_VirtualSize,16);assert last.Characteristics&0x20000000 and not last.Characteristics&0x02000000
site=0x4fb8;orig=bytes(nb[pe.get_offset_from_rva(site):pe.get_offset_from_rva(site)+5]);assert orig[0]==0xe8 and site+5+struct.unpack('<i',orig[1:])[0]==0x1f0b
code=bytearray(b'\x55\x89\xe5\xff\x75\x0c\xff\x75\x08')
def call(target):
 pos=rva+len(code);code.extend(b'\xe8'+struct.pack('<i',target-pos-5))
call(0x1f0b);code+=b'\x83\xc4\x08\x85\xc0';success=len(code);code+=b'\x75\x00'
# Compute the loaded image base without an absolute relocation.
call(rva+len(code)+5);basepoint=rva+len(code);code+=b'\x59\x81\xe9'+struct.pack('<I',basepoint)
code+=b'\x8d\x91'+struct.pack('<I',0x14d28)+b'\x39\x55\x0c';unknown=len(code);code+=b'\x75\x00'
strpatch=len(code)+2;code+=b'\x8d\x81\0\0\0\0\x50\xff\x75\x08';call(0x1f0b);code+=b'\x83\xc4\x08'
done=len(code);code+=b'\xc9\xc3';code[success+1]=done-success-2;code[unknown+1]=done-unknown-2
strrva=rva+len(code);struct.pack_into('<I',code,strpatch,strrva);code+=(ROOT+'\0').encode('utf-16le')
assert rawptr+len(code)<=last.PointerToRawData+last.SizeOfRawData
assert not any(nb[rawptr:rawptr+len(code)])
nb[rawptr:rawptr+len(code)]=code
struct.pack_into('<I',nb,last.get_field_absolute_offset('Misc_VirtualSize'),max(last.Misc_VirtualSize,rva-last.VirtualAddress+len(code)))
struct.pack_into('<I',nb,pe.OPTIONAL_HEADER.get_field_absolute_offset('CheckSum'),0)
o=pe.get_offset_from_rva(site);nb[o:o+5]=b'\xe8'+struct.pack('<i',rva-site-5);(OUT/'system/MileniumPortraitNative.dll').write_bytes(nb)
# Document exact source-level changes rather than alter the embedded original ScriptText.
source=(R/'classes/Classes/InventoryWnd.uc').read_text()
old='''        default:
        break;
    }
}

function Argumentations()'''
new='''        default:
            if(strID == "btnMileniumPreview")
            {
                if(GetHandle("MileniumInventoryPreviewWnd").IsShowWindow())
                {
                    class'MobPreviewNative'.static.ReleasePreview();
                    GetHandle("MileniumInventoryPreviewWnd").HideWindow();
                }
                else
                {
                    GetHandle("MobInformerWnd").HideWindow();
                    GetHandle("MileniumInventoryPreviewWnd").ShowWindow();
                    GetHandle("MileniumInventoryPreviewWnd.MobViewport").ShowWindow();
                    GetHandle("MileniumInventoryPreviewWnd").SetAnchor("InventoryWnd","TopLeft","TopRight",-6,33);
                    TextBoxHandle(GetHandle("MileniumInventoryPreviewWnd.RenderStatus")).SetText("Render 3D: " $ string(class'MobPreviewNative'.static.BindPreview(GetHandle("MileniumInventoryPreviewWnd.MobViewport"),class'MobPreviewNative'.static.GetWorldActor(class'UIDATA_PLAYER'.static.GetPlayerID()),35.0,145.0,45.0)));
                }
            }
        break;
    }
}

function Argumentations()'''
assert old in source;source=source.replace(old,new,1)
cleanup='''
    if(GetHandle("MileniumInventoryPreviewWnd").IsShowWindow())
    {
        class'MobPreviewNative'.static.ReleasePreview();
        GetHandle("MileniumInventoryPreviewWnd").HideWindow();
    }
'''
def appendbody(s,fn,extra):
 start=s.index('function '+fn+'(');op=s.index('{',start);depth=1;p=op+1
 while depth:
  depth+= (s[p]=='{')-(s[p]=='}');p+=1
 return s[:p-1]+extra+s[p-1:]
source=appendbody(source,'OnHide',cleanup);(OUT/'Classes/InventoryWnd.uc').write_text(source)
source=(R/'classes/Classes/MobInformerWnd.uc').read_text();source=appendbody(source,'OnShow',cleanup);(OUT/'Classes/MobInformerWnd.uc').write_text(source)
# Keep embedded ScriptText synchronized with the delivered source changes.
for cls in ['InventoryWnd','MobInformerWnd']:
 e=next(x for x in D['exports'] if x['path']==cls+'.ScriptText');old=U[e['offset']:e['offset']+e['size']]
 q=0;_,q=rdci(old,q);q+=8
 source=(OUT/'Classes'/(cls+'.uc')).read_text().split('defaultproperties')[0].rstrip()+'\r\n'
 rawsource=source.encode('latin1')+b'\0';changes[e['index']]=old[:q]+ci(len(rawsource))+rawsource
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
report={'kit':'InventoryPreview_TESTE1','visual_runtime_tested':False,'shared_mob_renderer':True,'hud_renderer_preserved':True,'original_function_addresses_preserved':True,'patched_functions':[D['exports'][i-1]['path'] for i in changes],'native_pic_wrapper_in_unused_padding_rva':hex(rva),'native_only_existing_instruction_patch':hex(site),'no_server_or_graphics_or_cloak_dll_changes':True,'original_dll_sha256':hashlib.sha256(p.read_bytes()).hexdigest()}
(OUT/'VERIFICACOES.json').write_text(json.dumps(report,indent=2));print(json.dumps(report,indent=2))
