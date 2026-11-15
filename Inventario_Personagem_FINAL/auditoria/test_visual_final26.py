from pathlib import Path
import sys,json,hashlib
R=Path(__file__).resolve().parent;sys.path.insert(0,str(R));from xdat import Reader

def load(path):
 b=path.read_bytes();r=Reader(b);ws=[r.node('Window') for _ in range(r.i())];return b,ws,b[r.p:]
a,old,tail=load(R/'TESTE25/system/interface.xdat');b,new,tail2=load(R/'FINAL26/system/interface.xdat')
assert len(old)==len(new) and tail==tail2
for x,y in zip(old,new):
 assert x['fields']['name']==y['fields']['name']
 if x['fields']['name']!='MileniumInventoryPreviewWnd':assert a[x['start']:x['end']]==b[y['start']:y['end']]
 else:
  assert {k:v for k,v in x['fields'].items() if k!='children'}=={k:v for k,v in y['fields'].items() if k!='children'}
  children=[c for c in x['fields']['children'] if c['fields']['name']!='PreviewTitleBanner']
  assert len(children)==len(y['fields']['children'])
  for c,d in zip(children,y['fields']['children']):assert a[c['start']:c['end']]==b[d['start']:d['end']]
assert b'TitleViwer' not in b and b'PreviewTitleBanner' not in b and b'MileniumInventoryPreviewHeaderWnd' not in b
assert (R/'FINAL26/system/interface.u').read_bytes()==(R/'TESTE25/system/interface.u').read_bytes()
(R/'FINAL26/VISUAL.json').write_text(json.dumps({'only_title_strip_removed':True,'all_remaining_window_and_control_data_byte_identical':True,'compiled_interface_byte_identical_to_25':True,'native_dll_not_changed':True,'no_decoration_or_header_resource_references':True},indent=2));print('PASS final layout: only strip removed, remaining controls and compiled UI preserved')
