from pathlib import Path
import struct,sys,json
R=Path(__file__).resolve().parent;P=R.parent;sys.path.insert(0,str(P));from xdat import Reader
b=(P/'TESTE7/system/interface.xdat').read_bytes();r=Reader(b);nodes=[r.node('Window') for _ in range(r.i())];root=next(x for x in nodes if x['fields']['name']=='MileniumInventoryPreviewWnd');label=next(x for x in root['fields']['children'] if x['fields']['name']=='RenderStatus');patches=[]
for node,k,v in [(root,'anchor_x',0),(label,'textColor',0),(label,'size_absolute_width',0),(label,'size_absolute_height',0)]:
 a,z=node['spans'][k];assert z-a==4;patches.append((a,z,struct.pack('<i',v)))
for a,z,d in sorted(patches,reverse=True):b=b[:a]+d+b[z:]
(P/'TESTE8/system/interface.xdat').write_bytes(b)
r=Reader(b);nodes=[r.node('Window') for _ in range(r.i())];root=next(x for x in nodes if x['fields']['name']=='MileniumInventoryPreviewWnd');label=next(x for x in root['fields']['children'] if x['fields']['name']=='RenderStatus');assert root['fields']['anchor_x']==0;assert label['fields']['textColor']==0 and label['fields']['size_absolute_height']==0
print('Layout verified: no horizontal gap; diagnostic label transparent with zero dimensions')
