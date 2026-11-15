from pathlib import Path
import sys,json
R=Path(__file__).resolve().parent;sys.path.insert(0,str(R));from xdat import Reader

def parse(file):
 b=file.read_bytes();r=Reader(b);w=[r.node('Window') for _ in range(r.i())];return b,w,r.p
old,ws,tail=parse(R/'TESTE22/system/interface.xdat');new,ns,tail2=parse(R/'TESTE23/system/interface.xdat')
assert len(ns)==len(ws)+1 and old[tail:]==new[tail2:]
for a,b in zip(ws,ns):
 assert a['fields']['name']==b['fields']['name']
 if a['fields']['name']!='InventoryWnd':assert old[a['start']:a['end']]==new[b['start']:b['end']]
 else:
  assert len(a['fields']['children'])==len(b['fields']['children'])
  for c,d in zip(a['fields']['children'],b['fields']['children']):
   assert c['fields']['name']==d['fields']['name']
   if c['fields']['name']!='btnMileniumPreview':assert old[c['start']:c['end']]==new[d['start']:d['end']]
root0=next(w['fields'] for w in ws if w['fields']['name']=='MileniumInventoryPreviewWnd');root=next(w['fields'] for w in ns if w['fields']['name']=='MileniumInventoryPreviewWnd')
assert root['anchor_y']==33 and root['size_absolute_height']==429
header=ns[-1]['fields'];assert header['name']=='MileniumInventoryPreviewHeaderWnd'
assert (header['anchor_ctrl'],header['anchor_x'],header['anchor_y'],header['size_absolute_width'],header['size_absolute_height'])==('MileniumInventoryPreviewWnd',7,-32,256,32)
banner=header['children'][0]['fields'];assert len(header['children'])==1
assert (banner['anchor_x'],banner['anchor_y'],banner['size_absolute_width'],banner['size_absolute_height'])==(0,0,256,32)
assert (banner['file'],banner['u'],banner['v'],banner['uSize'],banner['vSize'])==('L2UI_CH3.AbnormalWnd.TitleViwer',0.0,0.0,256.0,32.0)
inv=next(w['fields'] for w in ns if w['fields']['name']=='InventoryWnd');button=next(c['fields'] for c in inv['children'] if c['fields']['name']=='btnMileniumPreview')
assert button['size_absolute_width']==32 and button['size_absolute_height']==32
assert button['normalTex']=='L2UI_CH3.ShortcutWnd.AS_ShortcutWnd_ADD'
assert button['highlightTex']==button['pushedTex']=='L2UI_CH3.ShortcutWnd.AS_ShortcutWnd_ADD_Over'
report={'windows_game_run':False,'all_unrelated_windows_and_inventory_controls_byte_identical':True,'native_preview_parent_and_all_children_byte_identical':True,'native_viewport_remains_270x429':True,'independent_decoration_256x32_anchored_above_title':True,'button_32x32_add_initial_and_over':True,'root_geometry_completely_unchanged':True}
(R/'TESTE23/VISUAL.json').write_text(json.dumps(report,indent=2));print('PASS visual structure: separate header 256x32; native parent and viewport byte-identical; 32x32 button')
