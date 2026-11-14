from pathlib import Path
import sys,struct,json
R=Path(__file__).resolve().parent;sys.path.insert(0,'/workspace/work/cloak-audit/vendor');import pefile
path=R/'TESTE3/system/MileniumPortraitNative.dll';raw=bytearray(path.read_bytes());p=pefile.PE(data=raw);last=p.sections[-1];align=lambda n:(n+15)//16*16;rva=last.VirtualAddress+align(last.Misc_VirtualSize);offset=last.PointerToRawData+align(last.Misc_VirtualSize)
site=0x4550;o=p.get_offset_from_rva(site);assert raw[o:o+2]==b'\x0f\x84';assert site+6+struct.unpack_from('<i',raw,o+2)[0]==0x45c3
root='MileniumInventoryPreviewWnd';oldbytes=p.get_data(last.VirtualAddress,last.SizeOfRawData);stringrva=last.VirtualAddress+oldbytes.index((root+'\0').encode('utf-16le'))
code=bytearray()
def jcc(op,to):
 q=rva+len(code);code.extend(bytes([0x0f,op])+struct.pack('<i',to-q-6))
def jump(to):
 q=rva+len(code);code.extend(b'\xe9'+struct.pack('<i',to-q-5))
def call(to):
 q=rva+len(code);code.extend(b'\xe8'+struct.pack('<i',to-q-5))
# Preserve the original HUD branch. Other viewports must match the new panel's exact parent name.
jcc(0x85,0x4556);code+=b'\x60\x8b\x45\x08\x8b\x40\x04\x8b\x80\xb8\x00\x00\x00\x8b\x40\x0c\x8b\x40\x04'
call(rva+len(code)+5);basepoint=rva+len(code);code+=b'\x59\x81\xe9'+struct.pack('<I',basepoint)+b'\x8d\x91'+struct.pack('<I',stringrva)+b'\x52\x50';call(0x1f0b);code+=b'\x83\xc4\x08\x85\xc0\x61';jcc(0x84,0x45c3);jump(0x4556)
assert offset+len(code)<=last.PointerToRawData+last.SizeOfRawData and not any(raw[offset:offset+len(code)])
raw[offset:offset+len(code)]=code;raw[o:o+6]=b'\xe9'+struct.pack('<i',rva-site-5)+b'\x90';struct.pack_into('<I',raw,last.get_field_absolute_offset('Misc_VirtualSize'),rva-last.VirtualAddress+len(code));path.write_bytes(raw)
(R/'TESTE3/NATIVE_VISIBILIDADE.json').write_text(json.dumps({'hook_rva':hex(site),'wrapper_rva':hex(rva),'wrapper_bytes':len(code),'hud_branch_preserved':True,'new_inventory_parent_only':True,'original_restore_flags_code_rva':'0x4cb5..0x4cc8','source_character_visibility_not_changed_outside_preview':True,'windows_runtime_tested':False},indent=2))
print('Added scoped preview visibility at',hex(rva),'bytes',len(code))
