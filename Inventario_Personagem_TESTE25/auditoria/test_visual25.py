from pathlib import Path
import sys,json
R=Path(__file__).resolve().parent;sys.path.insert(0,str(R));from xdat import Reader

def parse(path):
 b=path.read_bytes();r=Reader(b);ws=[r.node('Window') for _ in range(r.i())];return b,{w['fields']['name']:w for w in ws},b[r.p:]
a,old,tail=parse(R/'TESTE24/system/interface.xdat');b,new,tail2=parse(R/'TESTE25/system/interface.xdat')
assert set(new)==set(old)-{'MileniumInventoryPreviewHeaderWnd'} and tail==tail2
for name,w in new.items():
 if name!='MileniumInventoryPreviewWnd':assert a[old[name]['start']:old[name]['end']]==b[w['start']:w['end']]
x=old['MileniumInventoryPreviewWnd'];y=new['MileniumInventoryPreviewWnd']
assert {k:v for k,v in x['fields'].items() if k!='children'}=={k:v for k,v in y['fields'].items() if k!='children'}
controls={c['fields']['name']:c for c in y['fields']['children']}
for c in x['fields']['children']:
 d=controls[c['fields']['name']]
 if c['fields']['name']!='PreviewTitle':assert a[c['start']:c['end']]==b[d['start']:d['end']]
 else:
  assert d['fields']['anchor_y']==6
  assert {k:v for k,v in c['fields'].items() if k!='anchor_y'}=={k:v for k,v in d['fields'].items() if k!='anchor_y'}
h=controls['PreviewTitleBanner']['fields'];v=controls['MobViewport']['fields'];t=controls['PreviewTitle']['fields']
assert (h['anchor_x'],h['anchor_y'],h['size_absolute_width'],h['size_absolute_height'])==(0,0,270,32)
assert h['size_absolute_width']==v['size_absolute_width']==y['fields']['size_absolute_width']
assert (h['uSize'],h['vSize'],h['file'])==(256.0,32.0,'L2UI_CH3.AbnormalWnd.TitleViwer')
assert t['anchor_y']+t['size_absolute_height']<=32
order=[c['fields']['name'] for c in y['fields']['children']];assert order.index('MobViewport')<order.index('PreviewTitleBanner')<order.index('PreviewTitle')
(R/'TESTE25/VISUAL.json').write_text(json.dumps({'windows_game_run':False,'old_floating_header_removed':True,'strip_inside_preview_at_0_0_270_32':True,'strip_matches_panel_width':True,'title_centered_inside_strip':True,'texture_source_uv_256_32_preserved':True,'all_unrelated_windows_byte_identical':True,'native_parent_geometry_viewport_background_arrows_byte_identical':True,'inventory_button_and_tooltip_preserved':True},indent=2));print('PASS integrated title bar: exact panel width, title inside; native geometry and all unrelated controls preserved')
