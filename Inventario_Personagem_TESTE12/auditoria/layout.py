from pathlib import Path
import sys,struct,copy,json
R=Path(__file__).resolve().parent;P=R.parent;sys.path.insert(0,str(P));from xdat import Reader
X=(P/'TESTE9/system/interface.xdat').read_bytes();r=Reader(X);n=r.i();windows=[r.node('Window') for _ in range(n)];end=r.p;original={}
def remember(node):
 for k,v in node['fields'].items():
  original[(node['start'],k)]=copy.deepcopy(v)
  if isinstance(v,list):
   for child in v:remember(child)
for w in windows:remember(w)
def ci(v):
 sign=128 if v<0 else 0;v=abs(v);a=[(v&63)|sign];v>>=6
 if v:a[0]|=64
 while v:a.append((v&127)|(128 if v>>7 else 0));v>>=7
 return bytes(a)
def st(v):
 if not v:return b'\0'
 b=v.encode('latin1')+b'\0';return ci(len(b))+b
def enc(node,typed=True):
 z=st(node['type']) if typed else b'';cursor=next(iter(node['spans'].values()))[0]
 for k,(a,b) in node['spans'].items():
  z+=X[cursor:a];cursor=b;v=node['fields'][k]
  if not isinstance(v,list) and v==original[(node['start'],k)]:z+=X[a:b]
  elif isinstance(v,list):z+=struct.pack('<i',len(v))+b''.join(enc(x,k=='children') for x in v)
  elif isinstance(v,str):z+=st(v)
  elif isinstance(v,float):z+=struct.pack('<f',v)
  else:z+=struct.pack('<i',v)
 return z
for w in windows:assert enc(w,False)==X[w['start']:w['end']]
root=next(w for w in windows if w['fields']['name']=='MileniumInventoryPreviewWnd');inv=next(w for w in windows if w['fields']['name']=='InventoryWnd');proto=next(c for c in inv['fields']['children'] if c['type']=='Button')
for name,x,tex,down,over in [('RotateLeft',55,'Inventory_DF_Btn_RotateRight','Inventory_DF_Btn_RotateRight_down','Inventory_DF_Btn_RotateRight_over'),('RotateRight',151,'Inventory_DF_Btn_RotateLeft','Inventory_DF_Btn_RotateLeft_Down','Inventory_DF_Btn_RotateLeft_Over')]:
 b=copy.deepcopy(proto);b['fields'].update(name=name,unk2=1,popupType='undefined',popupValue=-9999,unk4='MileniumInventoryPreviewWnd',unk7=1,size=1,size_absolute_values=1,size_absolute_width=64,size_absolute_height=32,anchor=1,anchor_parent=1,anchor_this=1,anchor_ctrl='',anchor_x=x,anchor_y=389,normalTex='L2Devil.Log.'+tex,pushedTex='L2Devil.Log.'+down,highlightTex='L2Devil.Log.'+over,dropTex='undefined',buttonName=-9999,noHighlight=0,disableTime=0)
 root['fields']['children'].append(b)
out=struct.pack('<i',n)+b''.join(enc(w,False) for w in windows)+X[end:];(P/'TESTE12/system/interface.xdat').write_bytes(out)
r=Reader(out);new=[r.node('Window') for _ in range(r.i())]
for a,b in zip(windows,new):
 if a is root:continue
 assert X[a['start']:a['end']]==out[b['start']:b['end']]
rw=next(w for w in new if w['fields']['name']=='MileniumInventoryPreviewWnd');buttons=[c['fields'] for c in rw['fields']['children'] if c['fields']['name'] in ['RotateLeft','RotateRight']];assert len(buttons)==2
(P/'TESTE12/LAYOUT.json').write_text(json.dumps(buttons,indent=2));print('Two 64x32 buttons added; all other windows byte-identical')
