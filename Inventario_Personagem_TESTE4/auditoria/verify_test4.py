from pathlib import Path
import sys,struct,json,hashlib,math
R=Path(__file__).resolve().parent;sys.path.insert(0,str(R));from xdat import Reader
sys.path.insert(0,'/workspace/work/cloak-audit');from catalog_unreal import catalog
sys.path.insert(0,'/workspace/work/cloak-audit/vendor');import pefile,capstone
O=R/'TESTE4';orig=Path('/workspace/attachments/6e516f3b-8810-4af1-8516-fe9e4339c975/interface.u').read_bytes();new=(O/'system/interface.u').read_bytes();D=catalog(O/'system/interface.u');old=json.loads((R/'interface-catalog.json').read_text());refs=len(D['exports']);imports=len(D['imports'])
class Decoder:
 def __init__(self,b):self.b=b;self.p=0;self.v=0;self.starts=set();self.jumps=[];self.refs=[];self.skips=[]
 def take(self,n):z=self.b[self.p:self.p+n];assert len(z)==n;self.p+=n;self.v+=n;return z
 def ci(self,name=False):
  r=Reader(self.b);r.p=self.p;n=r.ci();self.p=r.p;self.v+=4
  if name:assert 0<=n<len(D['names'])
  else:assert -imports<=n<=refs;self.refs.append(n)
  return n
 def short(self):return struct.unpack('<H',self.take(2))[0]
 def args(self):
  while self.b[self.p]!=0x16:self.token()
  self.take(1)
 def token(self):
  self.starts.add(self.v);at=self.p;op=self.take(1)[0]
  if op>=0x60:
   if op<0x70:self.take(1)
   self.args();return
  if op in [0,1,2,0x20,0x29]:self.ci();return
  if op in [0x0b,0x0c,0x0d,0x0e,0x16,0x17,0x25,0x26,0x27,0x28,0x2a,0x30,0x31]:return
  if op in [4,0x2d,0x37]:self.token();return
  if op==0x39:
   conversion=self.take(1)[0];assert 0x3a<=conversion<=0x63,('Bad conversion selector',hex(conversion));self.token();return
  if op==0x46:self.take(8);return
  if op in [0x0f,0x10,0x14,0x1a]:self.token();self.token();return
  if op==5:self.take(1);self.token();return
  if op in [6,7,0x0a]:
   n=self.short()
   if n!=0xffff:self.jumps.append(n)
   if op==7 or (op==0x0a and n!=0xffff):self.token()
   return
  if op==0x18:self.take(2);self.token();return
  if op in [0x12,0x19]:
   self.token();n=self.short();self.take(1);start=self.v;self.token();assert self.v-start==n,(at,hex(op),self.v-start,n);self.skips.append(n);return
  if op in [0x1b,0x1c,0x38]:self.ci(op!=0x1c);self.args();return
  if op in [0x1d,0x1e]:self.take(4);return
  if op==0x1f:
   while self.take(1)!=b'\0':pass
   return
  if op==0x21:self.ci(True);return
  if op in [0x22,0x23]:self.take(12);return
  if op in [0x24,0x2c]:self.take(1);return
  if op in [0x13,0x2e,0x36]:self.ci();self.token();return
  raise AssertionError(('Unknown opcode',hex(op),at))
def getscript(b,e):
 raw=b[e['offset']:e['offset']+e['size']];p=0;r=Reader(raw)
 for _ in range(7):r.ci()
 p=r.p+8;n=struct.unpack_from('<I',raw,p)[0];return raw[p+4:-7],n
changed=[]
for a,b in zip(old['exports'],D['exports']):
 assert {k:a[k] for k in ['index','name','outer','class_ref','super_ref','flags']}=={k:b[k] for k in ['index','name','outer','class_ref','super_ref','flags']}
 if orig[a['offset']:a['offset']+a['size']]!=new[b['offset']:b['offset']+b['size']]:changed.append(b['path'])
expected={'InventoryWnd.OnClickButton','InventoryWnd.OnHide','MobInformerWnd.OnShow','InventoryWnd.OnEvent','StatusWnd.ReleaseOptionalPortrait'}
assert set(changed)==expected|{'InventoryWnd.ScriptText','MobInformerWnd.ScriptText','StatusWnd.ScriptText'},(changed,expected)
checks={}
for path in expected:
 e=next(e for e in D['exports'] if e['path']==path);script,size=getscript(new,e);d=Decoder(script)
 while d.p<len(script):d.token()
 assert d.v==size,(path,d.v,size)
 assert set(d.jumps)<=d.starts,(path,set(d.jumps)-d.starts)
 checks[path]={'serialized_bytes':len(script),'runtime_bytes':size,'jump_targets_checked':len(d.jumps),'context_skips_checked':len(d.skips),'all_references_in_range':True}
# All original root controls retain their exact bytes, except the inventory child count/button append.
x=(O/'system/interface.xdat').read_bytes();r=Reader(x);count=r.i();windows=[r.node('Window') for _ in range(count)];assert count==190
oldwindows=json.loads((R/'xdat-tree.json').read_text());oldx=Path('/workspace/attachments/82b9f1aa-c8bb-41a3-a663-ede2cc0958c0/interface.xdat').read_bytes()
for a,b in zip(oldwindows,windows):
 if a['fields']['name']!='InventoryWnd':assert oldx[a['start']:a['end']]==x[b['start']:b['end']]
 else:
  assert len(b['fields']['children'])==len(a['fields']['children'])+1
  for c,d in zip(a['fields']['children'],b['fields']['children']):assert oldx[c['start']:c['end']]==x[d['start']:d['end']]
assert x[r.p:]==oldx[oldwindows[-1]['end']:]
root=next(w for w in windows if w['fields']['name']=='MileniumInventoryPreviewWnd')
bg=next(w for w in root['fields']['children'] if w['fields']['name']=='PanelBackground')['fields']
vp=next(w for w in root['fields']['children'] if w['fields']['name']=='MobViewport')['fields']
assert bg['file']=='L2UI_CH3.AbnormalWnd.Inventory_DF_CharacterEquipBg' and [bg['uSize'],bg['vSize']]==[284,419]
assert [vp['anchor_x'],vp['anchor_y'],vp['size_absolute_width'],vp['size_absolute_height']]==[0,0,270,429]
# PE exports/imports and all original code except the single guarded call remain unchanged.
a=pefile.PE('/workspace/attachments/4bd198f4-d35d-488f-88d6-785bfc6b168f/MileniumPortraitNative.dll');b=pefile.PE(str(O/'system/MileniumPortraitNative.dll'))
assert a.FILE_HEADER.Machine==b.FILE_HEADER.Machine==0x14c
assert [(e.name,e.address,e.ordinal) for e in a.DIRECTORY_ENTRY_EXPORT.symbols]==[(e.name,e.address,e.ordinal) for e in b.DIRECTORY_ENTRY_EXPORT.symbols]
assert a.get_data(a.OPTIONAL_HEADER.DATA_DIRECTORY[5].VirtualAddress,a.OPTIONAL_HEADER.DATA_DIRECTORY[5].Size)==b.get_data(b.OPTIONAL_HEADER.DATA_DIRECTORY[5].VirtualAddress,b.OPTIONAL_HEADER.DATA_DIRECTORY[5].Size)
ac=a.get_data(0x1000,0x12c00);bc=b.get_data(0x1000,0x12c00)
assert ac[:0x1d1d]==bc[:0x1d1d] and ac[0x1d23:0x3550]==bc[0x1d23:0x3550] and ac[0x3556:0x3fb8]==bc[0x3556:0x3fb8] and ac[0x3fbd:]==bc[0x3fbd:]
assert a.get_data(0x14000,0x3e00)==b.get_data(0x14000,0x3e00)
result={'bytecode_checked':checks,'xdat_190_roots_parsed':True,'original_inventory_controls_preserved':True,'all_other_root_windows_byte_identical':True,'xdat_shortcuts_and_tail_preserved':True,'native_exports_and_existing_relocations_preserved':True,'native_original_data_unchanged':True,'windows_game_executed':False}
(O/'VALIDACAO.json').write_text(json.dumps(result,indent=2));print(json.dumps(result,indent=2))
