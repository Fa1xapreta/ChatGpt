from pathlib import Path
import sys,struct,json,hashlib,tempfile
R=Path(__file__).resolve().parent;sys.path.insert(0,str(R.parent/'cloak-audit'))
from catalog_unreal import catalog,Reader
BODIES=['MFighter','FFighter','MDarkElf','FDarkElf','MDwarf','FDwarf','MElf','FElf','MMagic','FMagic','MOrc','FOrc','MShaman','FShaman']
NEW_STYLES={31300:'FxRogu',31301:'FxSwrdBlue',31302:'FxSwrdDark',31303:'FxArch',31304:'FxEnch',31305:'FxKnig',31306:'FxOlyB',31309:'FxRus2024',31312:'FxWarr'}
def ci(v):
 neg=v<0;v=abs(v);first=v&63;v>>=6;out=bytearray([first|(128 if neg else 0)|(64 if v else 0)])
 while v:
  n=v&127;v>>=7;out.append(n|(128 if v else 0))
 return bytes(out)
def U(v):return struct.pack('<I',v)
def build_for(body):
 p=R.parent/'cloak-audit/extracted/system'/('CC'+body+'Festival.u');data=p.read_bytes();assert data[:4]==b'\xc1\x83*\x9e';d=catalog(p);rd=Reader(p)
 count,off=struct.unpack_from('<II',data,12);start=off
 entries=[]
 for _ in range(count):
  pos=off;n,off=rd.ci(off);off+=abs(n)*(2 if n<0 else 1);off+=4;entries.append(data[pos:off])
 names=list(d['names']);exports=[dict(x) for x in d['exports']];alias_models={}
 for id,style in NEW_STYLES.items():
  old=next(e for e in exports if e['path']==body+'_FestivalCloth_'+style)
  name=body+'_'+style+'_m_ad00';idx=len(names);names.append(name);entry=ci(len(name)+1)+name.encode()+b'\0'+U(0x70010);entries.append(entry)
  e=dict(old);e['name']=name;e['outer']=0;e['name_index']=idx;exports.append(e);alias_models[id]=p.stem+'.'+name
 table=b''.join(entries);exp_off=len(data)+len(table);exp_table=b''
 for e in exports:
  name_index=e.get('name_index',d['names'].index(e['name'])) if 'name_index' not in e else e['name_index']
  exp_table+=ci(e['class_ref'])+ci(e['super_ref'])+struct.pack('<i',e['outer'])+ci(name_index)+U(e['flags'])+ci(e['size'])
  if e['size']:exp_table+=ci(e['offset'])
 header=bytearray(data[:64]);struct.pack_into('<4I',header,12,len(names),len(data),len(exports),exp_off)
 generations=struct.unpack_from('<I',data,52)[0];assert generations==1
 struct.pack_into('<2I',header,56,len(exports),len(names))
 append=table+exp_table;new=bytes(header)+data[64:]+append
 # Sequential temp files avoid keeping another 600 MB of identical texture data.
 q=R/'alias-validation.u';q.write_bytes(new);parsed=catalog(q);by_name={x['path']:x for x in parsed['exports']};check=Reader(q)
 for id,name in alias_models.items():
  alias=by_name[name.split('.',1)[1]];source=next(e for e in d['exports'] if e['path']==body+'_FestivalCloth_'+NEW_STYLES[id]);assert alias['class']==source['class'];assert check.read(alias['offset'],alias['size'])==data[source['offset']:source['offset']+source['size']]
 assert parsed['imports']==d['imports'];assert len(parsed['exports'])==len(d['exports'])+9
 check.close();rd.close();q.unlink()
 out=R/'alias-patches';out.mkdir(exist_ok=True);(out/(p.name+'.head')).write_bytes(header);(out/(p.name+'.append')).write_bytes(append)
 return {'file':p.name,'before':hashlib.sha256(data).hexdigest(),'after':hashlib.sha256(new).hexdigest(),'original_bytes':len(data),'final_bytes':len(new),'header_file':p.name+'.head','append_file':p.name+'.append','models':alias_models}
if __name__=='__main__':
 rows=[build_for(body) for body in BODIES];(R/'alias-patches/patches.json').write_text(json.dumps(rows,indent=2));print('Validated',len(rows),'packages,',len(rows)*9,'aliases; original serialized mesh payloads preserved')
