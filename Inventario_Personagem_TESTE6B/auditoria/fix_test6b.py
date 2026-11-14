from pathlib import Path
import sys,struct,json,hashlib,shutil
R=Path(__file__).resolve().parent;sys.path.insert(0,'/workspace/work/cloak-audit/vendor');import pefile
src=(R/'TESTE6/system/MileniumPortraitNative.dll').read_bytes();p=pefile.PE(data=src);out=bytearray(src)
# Windows SEC_IMAGE loading requires consecutive aligned section ranges.
# Reserve the entire two-page range up to the fixed address of .ivdata.
s=p.sections[3];assert s.Name.rstrip(b'\0')==b'.iview';assert s.Misc_VirtualSize==0x793
struct.pack_into('<I',out,s.get_file_offset()+8,0x2000)
q=pefile.PE(data=out);end=(q.OPTIONAL_HEADER.SizeOfHeaders+4095)&~4095;rows=[]
for sec in q.sections:
 assert sec.VirtualAddress==end,(sec.Name,hex(sec.VirtualAddress),hex(end))
 end=sec.VirtualAddress+((max(sec.Misc_VirtualSize,sec.SizeOfRawData)+4095)&~4095)
 assert sec.PointerToRawData+sec.SizeOfRawData<=len(out)
 rows.append({'section':sec.Name.rstrip(b'\0').decode(),'rva':hex(sec.VirtualAddress),'next':hex(end)})
assert end==q.OPTIONAL_HEADER.SizeOfImage
# No changes to code, imports, exports, relocations or payload bytes.
for a,b in zip(p.sections,q.sections):assert p.get_data(a.VirtualAddress,a.SizeOfRawData)==q.get_data(b.VirtualAddress,b.SizeOfRawData)
assert [(e.name,e.address) for e in p.DIRECTORY_ENTRY_EXPORT.symbols]==[(e.name,e.address) for e in q.DIRECTORY_ENTRY_EXPORT.symbols]
changes=[i for i,(a,b) in enumerate(zip(src,out)) if a!=b];assert all(s.get_file_offset()+8<=i<s.get_file_offset()+12 for i in changes)
O=R/'TESTE6B';(O/'system').mkdir(parents=True,exist_ok=True)
(O/'system/MileniumPortraitNative.dll').write_bytes(out)
for n in ['interface.u','interface.xdat']:shutil.copy2(R/'TESTE6/system'/n,O/'system'/n)
report={'original_sha256':hashlib.sha256(src).hexdigest(),'corrected_sha256':hashlib.sha256(out).hexdigest(),'changed_file_offsets':changes,'fix':'Reserve .iview virtual range through 0x184000; eliminate unmapped section gap at RVA 0x183000','section_layout':rows,'all_sections_consecutive':True,'code_imports_exports_relocations_unchanged':True,'windows_load_not_executed_here':True}
(O/'CORRECAO.json').write_text(json.dumps(report,indent=2));print(json.dumps(report,indent=2))
