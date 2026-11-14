from pathlib import Path
import sys,struct,json,hashlib,shutil
R=Path(__file__).resolve().parent;sys.path.insert(0,'/workspace/work/cloak-audit/vendor');import pefile
O=R/'TESTE5';(O/'system').mkdir(parents=True,exist_ok=True)
old=(R/'TESTE4/system/MileniumPortraitNative.dll').read_bytes();p=pefile.PE(data=old)
entries=[e for b in p.DIRECTORY_ENTRY_BASERELOC for e in b.entries if e.rva==0x2d1f and e.type==pefile.RELOCATION_TYPE['IMAGE_REL_BASED_HIGHLOW']];assert len(entries)==1
offset=entries[0].struct.get_file_offset();word=struct.unpack_from('<H',old,offset)[0];assert word>>12==3
new=bytearray(old);struct.pack_into('<H',new,offset,word&0x0fff)
assert [i for i,(a,b) in enumerate(zip(old,new)) if a!=b]==[offset+1]
(O/'system/MileniumPortraitNative.dll').write_bytes(new)
for n in ['interface.u','interface.xdat']:shutil.copy2(R/'TESTE4/system'/n,O/'system'/n)
q=pefile.PE(data=new);assert next(e for b in q.DIRECTORY_ENTRY_BASERELOC for e in b.entries if e.rva==0x2d1f).type==0
result={'change':'neutralize only obsolete HIGHLOW relocation overlapping the replaced MOV operand','original_operand_rva':'0x2d1f','patch_jump_rva':'0x2d1d','relocation_word_file_offset':hex(offset),'old_word':hex(word),'new_word':hex(word&0x0fff),'changed_file_bytes':1,'size_unchanged':True,'sha256_test4':hashlib.sha256(old).hexdigest(),'sha256_fixed':hashlib.sha256(new).hexdigest(),'interface_u_xdat_unchanged_from_test4':True,'windows_game_tested':False}
(O/'CORRECAO_ASLR.json').write_text(json.dumps(result,indent=2));print(json.dumps(result,indent=2))
