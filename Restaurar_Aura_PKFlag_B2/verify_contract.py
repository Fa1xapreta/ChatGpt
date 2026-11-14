from pathlib import Path
import sys,hashlib,json,struct
R=Path(__file__).resolve().parent;W=R.parent;sys.path.insert(0,str(W/'cloak-audit/vendor'));import pefile
cli=pefile.PE(str(W/'cloak-migration/complete-kit/cliente/system/CliExt.dll'));core=pefile.PE(str(W/'cloak-migration/current-system/L2modExCore.dll'));old=pefile.PE(str(W/'cloak-integrated/native/MilAuraWait.dll'));new=pefile.PE(str(R/'native/MilAuraWait.dll'))
def exports(p):return {e.name.decode() for e in getattr(p,'DIRECTORY_ENTRY_EXPORT',type('D',(),{'symbols':[]})()).symbols if e.name}
assert 'ContornoDourado_Status' not in exports(old);assert {'ContornoDourado_Status','ContornoDourado_Versao'}<=exports(new);assert new.FILE_HEADER.Machine==0x14c
# These are the original consumer's exact preconditions for its two core hooks.
assert core.get_data(0x53f9,5)==bytes.fromhex('e890d7ffff')
assert core.get_data(0x56a5,8)==cli.get_data(0x303c,8)
base=cli.OPTIONAL_HEADER.ImageBase
for newbase in [base,0x33000000]:
 p=pefile.PE(str(W/'cloak-migration/complete-kit/cliente/system/CliExt.dll'));p.relocate_image(newbase)
 assert p.get_data(0x1162,7)==bytes.fromhex('5553575683ec08');assert p.get_data(0x1174,1)==b'\xa3';assert struct.unpack('<I',p.get_data(0x1175,4))[0]==newbase+0x4044
 assert p.get_data(0x13f3,2)==b'\x8b\x35';assert struct.unpack('<I',p.get_data(0x13f5,4))[0]==newbase+0x4044
assert cli.OPTIONAL_HEADER.SizeOfImage>=0x8000
sec=cli.get_section_by_rva(0x4044);assert sec.Characteristics&0x80000000
info=dict(original_shim_missing_status_export=True,new_shim_exports_status_and_version=True,new_shim_x86=True,legacy_core_hook_patterns_match=True,cliext_module_slot_rva='0x4044',cliext_fingerprint_checked_with_relocation=True,legacy_getproc_status_site='0x13b0..0x13c5',legacy_core_base_reload_site='0x13f3',native_game_tested=False,sha256_new=hashlib.sha256((R/'native/MilAuraWait.dll').read_bytes()).hexdigest(),observed_report='A1: original D3DDrv restored, core reports 1, PK/flag aura remains absent')
(R/'CONTRATO.json').write_text(json.dumps(info,indent=2));print('PASS: actual PE exports, legacy hook guards, module-slot contract and relocation fingerprints')
