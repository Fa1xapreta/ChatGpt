from pathlib import Path
import sys,json,math,struct,hashlib,bisect
R=Path(__file__).resolve().parent;sys.path.insert(0,str(R));from parse_fighter import Stream,parse_package
sys.path.insert(0,str(R.parent/'cloak-audit'));from catalog_unreal import Reader,catalog
source=R/'uploaded/Fighter.ukx';original=source.read_bytes();d=catalog(source);r=Reader(source);assert r.format=='Lineage2Ver111';raw=r.read(0,len(original)-r.skip);skip=r.skip;key=r.key;r.close();motion=json.loads((R/'Fighter.motion.json').read_text());raw_moves={x['name']:x['moves'] for x in json.loads((R/'Fighter.parsed.json').read_text()) if x['class_name']=='Engine.MeshAnimation'};changed={};records=[]
def ci(n):
 neg=n<0;n=abs(n);o=bytearray([(n&63)|(128 if neg else 0)]);n>>=6
 if n:o[0]|=64
 while n:
  x=n&127;n>>=7;o.append(x|(128 if n else 0))
 return bytes(o)
def U(n):return struct.pack('<I',n)
def floats(v):return struct.pack('<'+'f'*len(v),*v)
def norm(q):
 n=math.sqrt(sum(v*v for v in q));return tuple(v/n for v in q)
def conj(q):return (-q[0],-q[1],-q[2],q[3])
def mul(a,b):
 x,y,z,w=a;X,Y,Z,W=b;return (w*X+x*W+y*Z-z*Y,w*Y-x*Z+y*W+z*X,w*Z+x*Y-y*X+z*W,w*W-x*X-y*Y-z*Z)
def rotate(q,v):return mul(mul(q,(*v,0)),conj(q))[:3]
def sample(t,field,time,duration):
 vals=t[field]
 if len(vals)==1:return tuple(vals[0])
 ts=t['time'] if len(t['time'])==len(vals) else [duration*i/len(vals) for i in range(len(vals))]
 i=max(0,min(len(vals)-1,bisect.bisect_right(ts,time)-1));j=min(len(vals)-1,i+1);alpha=0 if i==j else (time-ts[i])/(ts[j]-ts[i]);a=vals[i];b=vals[j]
 if field=='position':return tuple(x+(y-x)*alpha for x,y in zip(a,b))
 a=norm(a);b=norm(b);dot=sum(x*y for x,y in zip(a,b))
 if dot<0:b=tuple(-x for x in b);dot=-dot
 if dot>.9995:return norm(tuple(x+(y-x)*alpha for x,y in zip(a,b)))
 angle=math.acos(min(1,dot));return norm(tuple((math.sin((1-alpha)*angle)*x+math.sin(alpha*angle)*y)/math.sin(angle) for x,y in zip(a,b)))
def pose(a,m,time):
 qs=[];ps=[]
 for i,b in enumerate(a['bones']):
  ti=m['indices'][i];assert ti>=0;t=m['tracks'][ti];q=norm(sample(t,'quat',time,m['duration']));p=sample(t,'position',time,m['duration'])
  if i:q=conj(q);parent=b['parent'];p=tuple(x+y for x,y in zip(ps[parent],rotate(qs[parent],p)));q=mul(qs[parent],q)
  qs.append(q);ps.append(p)
 return qs,ps
for a in motion:
 if a['name'] not in ['MFighter_anim','FFighter_anim']:continue
 e=next(e for e in d['exports'] if e['name']==a['name']);b=raw[e['offset']:e['offset']+e['size']];replacements={}
 for seq,m in zip(a['sequences'],a['moves']):
  if not seq['name'].startswith('Run_'):continue
  t=m['tracks'][36];assert m['indices'][36]==36 and a['bones'][36]['name']=='Cape_Bone';assert a['bones'][36]['parent']==4;assert len(t['position'])==len(t['quat'])==1
  foots=(40,45) if a['name'].startswith('M') else (44,49);frames=seq['frames'];poses=[pose(a,m,float(i)) for i in range(frames)];ground=min(ps[j][2] for qs,ps in poses for j in foots);factors=[];foot_report=[]
  for qs,ps in poses:
   vals=[];details=[]
   for j in foots:
    lift=max(0,ps[j][2]-ground);rel=rotate(conj(qs[4]),tuple(x-y for x,y in zip(ps[j],ps[4])));behind=rel[1]-t['position'][0][1]
    h=max(0,min(1,(lift-2)/10));back=max(0,min(1,(behind+3)/7));h=h*h*(3-2*h);back=back*back*(3-2*back);vals.append(h*back);details.append(dict(bone=j,lift=lift,behind_anchor=behind))
   factors.append(max(vals));foot_report.append(details)
  smooth=[.25*factors[(i-1)%frames]+.5*factors[i]+.25*factors[(i+1)%frames] for i in range(frames)];angles=[6*v for v in smooth];base=norm(t['quat'][0]);quats=[]
  for angle in angles:
   h=math.radians(angle)/2;q=norm(mul(base,(0,0,math.sin(h),math.cos(h))));quats.append(q)
   # Bone position never changes; its lateral top axis is essentially fixed.
   assert math.dist(rotate(conj(base),(0,0,15)),rotate(conj(q),(0,0,15)))<.0001
   old_hem=rotate(conj(base),(50,0,0));new_hem=rotate(conj(q),(50,0,0));assert new_hem[1]>=old_hem[1]-1e-5 and new_hem[0]>=old_hem[0]-1e-5
  start=t['quat_offset']-len(ci(1))-4;end=t['time_offset']+len(t['time'])*4
  pos_raw=b[t['position_offset']:t['position_offset']+12]
  track=U(t['flags'])+ci(frames)+b''.join(floats(q) for q in quats)+ci(1)+pos_raw+ci(frames)+floats(range(frames))
  replacements[seq['index']]=(start,end,track)
  records.append(dict(animation=a['name'],sequence=seq['name'],frames=frames,anchor_position=t['position'][0],angle_degrees=angles,foot_samples=foot_report))
 # Store source prefix, chunks, and sequence table; only Run cape tracks change.
 s=Stream(b,d['names']);from export_textures import props
 s.p=props(b,d['names'])[0];s.i()
 for _ in range(s.count()):s.name();s.u();s.i()
 global_pointer=s.p;old_end=s.u()-e['offset'];count=s.count();assert count==len(a['moves']);prefix=b[:global_pointer];chunks=[]
 for i,m in enumerate(raw_moves[a['name']]):
  if i in replacements:
   start,end,track=replacements[i];chunk=b[m['offset']:start]+track+b[end:m['offset']+m['bytes']]
  else:chunk=b[m['offset']:m['offset']+m['bytes']]
  chunks.append(chunk)
 changed[e['index']]=dict(prefix=prefix,chunks=chunks,sequence_bytes=b[old_end:],old=a)
assert len(records)==12
newraw=bytearray(raw);newexports=[dict(x) for x in d['exports']]
for e in newexports:
 if e['index'] not in changed:continue
 z=changed[e['index']];offset=len(newraw);p=offset+len(z['prefix'])+4+len(ci(len(z['chunks'])));parts=[]
 for chunk in z['chunks']:p+=4+len(chunk);parts.append(U(p)+chunk)
 blob=z['prefix']+U(p)+ci(len(parts))+b''.join(parts)+z['sequence_bytes'];e['offset']=offset;e['size']=len(blob);newraw.extend(blob)
# Preserve original name indices by decoding them from the original export table.
s=Stream(raw,d['names'],struct.unpack_from('<I',raw,24)[0]);name_indices=[]
for _ in d['exports']:
 s.ci();s.ci();s.i();name_indices.append(s.ci());s.u();size=s.ci()
 if size:s.ci()
export_offset=len(newraw);table=b''
for e,ni in zip(newexports,name_indices):
 table+=ci(e['class_ref'])+ci(e['super_ref'])+struct.pack('<i',e['outer'])+ci(ni)+U(e['flags'])+ci(e['size'])
 if e['size']:table+=ci(e['offset'])
newraw.extend(table);struct.pack_into('<I',newraw,24,export_offset)
target=original[:skip]+bytes(v^key for v in newraw);candidate=R/'Fighter_TESTE8.ukx';candidate.write_bytes(target);nd=catalog(candidate)
assert nd['names']==d['names'] and nd['imports']==d['imports'];assert len(nd['exports'])==len(d['exports'])
assert len({e['offset'] for e in nd['exports'] if e['index'] in changed})==2
for old,new in zip(d['exports'],nd['exports']):
 if old['index'] not in changed:assert old==new
# All non-animation exports remain at their original bytes and offsets.
newr=Reader(candidate)
for e in d['exports']:
 if e['index'] not in changed:assert raw[e['offset']:e['offset']+e['size']]==newr.read(e['offset'],e['size'])
newr.close();parsed=parse_package(candidate,{a['name'] for a in motion});assert len(parsed)==3
for a in parsed:
 old=next(x for x in motion if x['name']==a['name']);assert a['bones']==old['bones'];assert len(a['moves'])==len(old['moves'])
 # The complete sequence table is preserved even though its byte offsets moved.
 for x,y in zip(a['sequences'],old['sequences']):assert {k:v for k,v in x.items() if k!='offset'}=={k:v for k,v in y.items() if k!='offset'}
report=dict(source_sha256=hashlib.sha256(original).hexdigest(),test7_sha256='dcd14791f086d0f3053837f12dca8883ee0f2c1e6ea17ba68aab52d88d4ae170',target_sha256=hashlib.sha256(target).hexdigest(),source_bytes=len(original),target_bytes=len(target),max_hinge_degrees=6,changed_sequences=12,anchor_position_original=True,upper_axis_max_drift_less_than=.0001,all_non_target_exports_unchanged=True,sequence_tables_unchanged=True,new_collision_detection=False,game_tested=False,phase_source='Foot height and posterior displacement from FK of existing Run tracks; smoothed cyclically',records=records)
kit=R/'test8';header=target[:skip+64];append=target[len(original):];(kit/'cabecalho.bin').write_bytes(header);(kit/'animacoes.bin').write_bytes(append)
report['header_sha256']=hashlib.sha256(header).hexdigest();report['append_sha256']=hashlib.sha256(append).hexdigest();report['remove_test7_changes']=json.load(open(R/'test7/alteracao.json'))['changes'];(kit/'alteracao.json').write_text(json.dumps(report,indent=2));print('T8 built',len(target),'bytes;',len(append),'append; SHA',report['target_sha256']);print('angles',[(x['sequence'],round(min(x['angle_degrees']),2),round(max(x['angle_degrees']),2)) for x in records])
