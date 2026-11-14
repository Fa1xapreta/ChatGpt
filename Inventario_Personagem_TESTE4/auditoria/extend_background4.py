from pathlib import Path
import sys,struct,json
R=Path(__file__).resolve().parent;sys.path.insert(0,'/workspace/work/cloak-audit/vendor');import pefile
path=R/'TESTE4/system/MileniumPortraitNative.dll';raw=bytearray(path.read_bytes());p=pefile.PE(data=raw);last=p.sections[-1];align=lambda n:(n+15)//16*16
rva=last.VirtualAddress+align(last.Misc_VirtualSize);off=last.PointerToRawData+align(last.Misc_VirtualSize);site=0x2d1d;o=p.get_offset_from_rva(site)
assert raw[o:o+6]==b'\x8b\x05'+struct.pack('<I',p.OPTIONAL_HEADER.ImageBase+0x17e14)
root='MileniumInventoryPreviewWnd';stringrva=last.VirtualAddress+p.get_data(last.VirtualAddress,last.SizeOfRawData).index((root+'\0').encode('utf-16le'))
code=bytearray(b'\x60')
def call(to):
 q=rva+len(code);code.extend(b'\xe8'+struct.pack('<i',to-q-5))
def jump(to):
 q=rva+len(code);code.extend(b'\xe9'+struct.pack('<i',to-q-5))
# EBX is preserved by the existing cdecl name predicate; the original Canvas ECX/EDX are saved.
call(rva+len(code)+5);basepoint=rva+len(code);code+=b'\x5b\x81\xeb'+struct.pack('<I',basepoint)
code+=b'\x8b\x45\x08\x8b\x40\x04\x8b\x80\xb8\x00\x00\x00\x8b\x40\x0c\x8b\x40\x04\x8d\x93'+struct.pack('<I',stringrva)+b'\x52\x50'
call(0x1f0b);code+=b'\x83\xc4\x08\x85\xc0';conditional=len(code);code+=b'\x74\0'
# DrawTile arguments: x,y,width,height,u0,v0,u1,v1,z,texture,color.
# ESP is 32 bytes below the original parameter block due to PUSHAD.
for stackoffset,value in [(48,0.0),(52,0.0),(56,284.0),(60,419.0)]:code+=b'\xc7\x44\x24'+bytes([stackoffset])+struct.pack('<f',value)
end=len(code);code[conditional+1]=end-conditional-2
code+=b'\x8b\x83'+struct.pack('<I',0x17e14)+b'\x89\x44\x24\x1c\x61';jump(0x2d23)
assert off+len(code)<=last.PointerToRawData+last.SizeOfRawData and not any(raw[off:off+len(code)])
raw[off:off+len(code)]=code;raw[o:o+6]=b'\xe9'+struct.pack('<i',rva-site-5)+b'\x90';struct.pack_into('<I',raw,last.get_field_absolute_offset('Misc_VirtualSize'),rva-last.VirtualAddress+len(code));path.write_bytes(raw)
(R/'TESTE4/NATIVE_FUNDO.json').write_text(json.dumps({'patched_site':hex(site),'wrapper_rva':hex(rva),'wrapper_bytes':len(code),'inventory_parent_only':True,'uv_pixel_rectangle':[0,0,284,419],'texture_reference':'L2UI_CH3.AbnormalWnd.Inventory_DF_CharacterEquipBg','original_texture_dimensions_assumed':[512,512],'user_imported_texture_not_replaced':True,'windows_tested':False},indent=2));print('Inventory-only background UV wrapper',hex(rva),len(code))
