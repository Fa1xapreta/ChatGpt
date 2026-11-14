from pathlib import Path
import struct,sys,json,shutil
R=Path(__file__).resolve().parent;P=R.parent;sys.path[:0]=['/workspace/work/cloak-audit/vendor',str(P)]
import pefile
elf=(R/'viewer.elf').read_bytes();shoff=struct.unpack_from('<I',elf,32)[0];_,ents,n,si=struct.unpack_from('<HHHH',elf,44)
ss=[struct.unpack_from('<10I',elf,shoff+i*ents) for i in range(n)];strings=elf[ss[si][4]:ss[si][4]+ss[si][5]]
name=lambda a:strings[a:].split(b'\0')[0].decode();sects={name(s[0]):s for s in ss};symbols={}
for s in ss:
 if s[1]==2:
  st=ss[s[6]];strs=elf[st[4]:st[4]+st[5]]
  for pos in range(s[4],s[4]+s[5],16):
   nm,v,sz,inf,oth,idx=struct.unpack_from('<IIIBBH',elf,pos);nm=strs[nm:].split(b'\0')[0].decode()
   if nm:symbols[nm]=v
code=bytearray(elf[sects['.text'][4]:sects['.text'][4]+sects['.text'][5]])
while len(code)%16:code+=b'\x90'
tr=0x182000+len(code)
# Save all registers; recover actual module base; call C callback and replay instructions.
t=bytearray(b'\x60\xe8\0\0\0\0\x59\x81\xe9'+struct.pack('<I',tr+6)+b'\x55\x51')
t+=b'\xe8'+struct.pack('<i',symbols['viewer_tick']-0x10000000-(tr+len(t)+5));t+=b'\x83\xc4\x08\x61\x8b\x45\x08\x83\xc0\x08';t+=b'\xe9'+struct.pack('<i',0x4a26-(tr+len(t)+5));code+=t
assert len(code)<0x2000
base=(P/'TESTE5/system/MileniumPortraitNative.dll').read_bytes();pe=pefile.PE(data=base)
assert pe.get_data(0x4a20,6)==bytes.fromhex('8b450883c008')
rel={e.rva for block in pe.DIRECTORY_ENTRY_BASERELOC for e in block.entries if e.type==3}
assert not any(0x4a1d<=r<0x4a26 for r in rel)
for s in ss:
 if s[1]==9:
  for pos in range(s[4],s[4]+s[5],8):
   off,info=struct.unpack_from('<II',elf,pos);typ=info&255
   assert typ in (1,2),typ
   if typ==1:rel.add(off-0x10000000)
blocks=bytearray()
for page in sorted({r&~4095 for r in rel}):
 vals=[0x3000|(r&4095) for r in sorted(rel) if r&~4095==page]
 if len(vals)%2:vals.append(0)
 blocks+=struct.pack('<II',page,8+2*len(vals))+struct.pack('<'+'H'*len(vals),*vals)
data=bytearray(0x1400)
for k in ['.rodata','.data']:
 s=sects[k];off=s[3]-0x10184000;data[off:off+s[5]]=elf[s[4]:s[4]+s[5]]
out=bytearray(base[:512]+bytes(512)+base[512:]);fh=pe.DOS_HEADER.e_lfanew+4;opt=fh+20;headers=opt+pe.FILE_HEADER.SizeOfOptionalHeader
for i,s in enumerate(pe.sections):struct.pack_into('<I',out,headers+i*40+20,s.PointerToRawData+512)
struct.pack_into('<H',out,fh+2,6);struct.pack_into('<I',out,opt+60,1024)
for i,(nm,rva,payload,flags) in enumerate([(b'.iview',0x182000,code,0x60000020),(b'.ivdata',0x184000,data,0xc0000040),(b'.ivrel',0x186000,blocks,0x42000040)],3):
 while len(out)%512:out+=b'\0'
 raw=len(out);sz=(len(payload)+511)&~511;out+=payload+bytes(sz-len(payload));struct.pack_into('<8sIIIIIIHHI',out,headers+i*40,nm,len(payload),rva,sz,raw,0,0,0,0,flags)
struct.pack_into('<I',out,headers+3*40+8,0x2000)
struct.pack_into('<I',out,opt+56,0x187000);struct.pack_into('<II',out,opt+96+5*8,0x186000,len(blocks));struct.pack_into('<I',out,opt+64,0)
p=pefile.PE(data=out);off=p.get_offset_from_rva(0x4a20);out[off:off+6]=b'\xe9'+struct.pack('<i',tr-0x4a25)+b'\x90'
O=P/'TESTE7/system';O.mkdir(parents=True,exist_ok=True);(O/'MileniumPortraitNative.dll').write_bytes(out);shutil.copy2(P/'TESTE5/system/interface.u',O/'interface.u')
from xdat import Reader
b=(P/'TESTE5/system/interface.xdat').read_bytes();reader=Reader(b);count=reader.i();nodes=[reader.node('Window') for _ in range(count)]
root=next(x for x in nodes if x['fields']['name']=='MileniumInventoryPreviewWnd');title=next(x for x in root['fields']['children'] if x['fields']['name']=='PreviewTitle');a,z=title['spans']['text'];txt=b'Inventory Viewr\0';assert len(txt)<64
(O/'interface.xdat').write_bytes(b[:a]+bytes([len(txt)])+txt+b[z:]);(R/'metadata.json').write_text(json.dumps({'symbols':symbols,'trampoline':tr,'relocations':len(rel)},indent=2));print('Built',len(out),symbols)
