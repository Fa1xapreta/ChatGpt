from pathlib import Path
import sys,struct,json,hashlib
R=Path(__file__).resolve().parent;A=R.parent/'cloak-audit';sys.path.insert(0,str(A/'vendor'));import pefile
source=A/'extracted/system/CapasClassic.dll';data=source.read_bytes();pe=pefile.PE(data=data);image=pe.get_memory_mapped_image();base=pe.OPTIONAL_HEADER.ImageBase
profile=struct.unpack_from('<I',image,0x7a290+4)[0];name=struct.unpack_from('<I',image,profile-base)[0];assert image[name-base:name-base+20].decode('utf-16le').rstrip('\0')=='MFighter_'
va=profile+0x298;off=pe.get_offset_from_rva(va-base);assert struct.unpack_from('<d',data,off)[0]==-50.0
new=bytearray(data);struct.pack_into('<d',new,off,-65.0);new=bytes(new);before=hashlib.sha256(data).hexdigest();after=hashlib.sha256(new).hexdigest()
assert data[:off]==new[:off] and data[off+8:]==new[off+8:]
for sec in pe.sections:
 if sec.Characteristics&0x20000000:assert sec.get_data()==new[sec.PointerToRawData:sec.PointerToRawData+sec.SizeOfRawData]
for p in struct.unpack_from('<14I',image,0x7a290):
 at=pe.get_offset_from_rva(p-base)
 if p!=profile:assert new[at:at+0x310]==data[at:at+0x310]
for f in [0x250,0x270,0x2b0,0x2d0,0x2f0]:
 at=pe.get_offset_from_rva(profile-base+f);assert new[at:at+32]==data[at:at+32]
report={'scope':'Experimental MFighter movement profile; all capes using that body, not item-specific','reported_item':31394,'body':'MFighter_','source_sha256':before,'patched_sha256':after,'file_offset':off,'va':hex(va),'profile_va':hex(profile),'field_offset':'0x298','before_double':-50.0,'after_double':-65.0,'original_bytes_hex':data[off:off+8].hex(),'patched_bytes_hex':new[off:off+8].hex(),'instruction_code_unchanged':True,'other_body_profiles_unchanged':True,'standing_walk_and_mount_profile_entries_unchanged':True,'game_validated':False,'new_leg_collision_detection_added':False,'evidence':{'profile_reader':'0x10004450: interpolation thresholds 70,120; entries +0x250,+0x270,+0x290; mounted mix +0x2b0; outputs +0x80..+0x98','read_run_component':'0x1000452f: movsd xmm5, qword ptr [eax + 0x298]','body_name':'UTF16 MFighter_ via profile pointer table entry 1'}}
(R/'test6-audit.json').write_text(json.dumps(report,indent=2));(R/'candidate-CapasClassic.dll').write_bytes(new)
print(json.dumps(report,indent=2))
