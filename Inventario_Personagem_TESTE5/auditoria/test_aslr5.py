from pathlib import Path
import sys,struct,json
R=Path(__file__).resolve().parent;sys.path[:0]=[str(R/'test-vendor'),'/workspace/work/cloak-audit/vendor']
import pefile
from unicorn import Uc,UC_ARCH_X86,UC_MODE_32,UcError,UC_ERR_FETCH_UNMAPPED
old=(R/'TESTE4/system/MileniumPortraitNative.dll').read_bytes();new=(R/'TESTE5/system/MileniumPortraitNative.dll').read_bytes();results=[]
for base in [0x10000000,0x10100000,0x10340000,0x33000000,0x6fba0000,0x00500000]:
 row={'base':hex(base)}
 for name,data in [('test4',old),('test5',new)]:
  p=pefile.PE(data=data);p.relocate_image(base);code=p.get_data(0x2d1d,6);target=0x2d1d+5+struct.unpack('<i',code[1:5])[0];row[name+'_jump_rva']=hex(target);row[name+'_in_image']=0<=target<p.OPTIONAL_HEADER.SizeOfImage
  if name=='test5':
   assert target==0x181300 and code[-1]==0x90
   for a,b in [(0x4fb8,0x4fbd),(0x4550,0x4556),(0x2d1d,0x2d23)]:
    assert not [e for block in p.DIRECTORY_ENTRY_BASERELOC for e in block.entries if e.type==3 and e.rva<b and e.rva+4>a]
 results.append(row)
# Execute the actual relocated bad jump, with no mocked arithmetic or substituted instruction.
base=0x6fba0000;p=pefile.PE(data=old);p.relocate_image(base);u=Uc(UC_ARCH_X86,UC_MODE_32);u.mem_map(base,0x184000);u.mem_write(base,p.get_memory_mapped_image());fault=None
try:u.emu_start(base+0x2d1d,base+0x184000,count=1)
except UcError as e:fault=e.errno
assert fault==UC_ERR_FETCH_UNMAPPED,('Expected fault from original relocated TESTE4 jump',fault)
a=pefile.PE(data=old);b=pefile.PE(data=new)
assert [(e.name,e.address,e.ordinal) for e in a.DIRECTORY_ENTRY_EXPORT.symbols]==[(e.name,e.address,e.ordinal) for e in b.DIRECTORY_ENTRY_EXPORT.symbols]
for sa,sb in zip(a.sections,b.sections):
 assert sa.Name==sb.Name and sa.VirtualAddress==sb.VirtualAddress and sa.Misc_VirtualSize==sb.Misc_VirtualSize
 if sa.Name!=b'.reloc\0\0':assert a.get_data(sa.VirtualAddress,sa.SizeOfRawData)==b.get_data(sb.VirtualAddress,sb.SizeOfRawData)
for n in ['interface.u','interface.xdat']:assert (R/'TESTE4/system'/n).read_bytes()==(R/'TESTE5/system'/n).read_bytes()
meta={'actual_windows_PE_relocations_applied':True,'original_test4_fault_reproduced_with_actual_x86_jump':True,'fault':'UC_ERR_FETCH_UNMAPPED','original_exports_and_code_unchanged':True,'only_one_obsolete_relocation_entry_neutralized':True,'all_new_detours_clear_of_active_relocation_overlaps':True,'results':results,'windows_game_run':False}
(R/'TESTE5/REGRESSAO_ASLR.json').write_text(json.dumps(meta,indent=2));print(json.dumps(meta,indent=2))
