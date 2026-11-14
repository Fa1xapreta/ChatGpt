from pathlib import Path
import sys,struct,json
R=Path(__file__).resolve().parent;sys.path[:0]=[str(R/'test-vendor'),'/workspace/work/cloak-audit/vendor']
import pefile
from unicorn import Uc,UC_ARCH_X86,UC_MODE_32,UC_HOOK_CODE
from unicorn.x86_const import *
p=pefile.PE(str(R/'TESTE3/system/MileniumPortraitNative.dll'));image=p.get_memory_mapped_image();cases=[]
for base in [0x10000000,0x33000000,0x00500000]:
 for role,name,want in [(1,'StatusWnd',0x4556),(0,'MileniumInventoryPreviewWnd',0x4556),(0,'MobInformerWnd',0x45c3),(0,'TeleportWnd',0x45c3),(0,'UnknownWnd',0x45c3)]:
  u=Uc(UC_ARCH_X86,UC_MODE_32);u.mem_map(base,0x184000);u.mem_write(base,image);stack=0x0f000000;u.mem_map(stack,0x10000);heap=0x02000000;u.mem_map(heap,0x10000)
  ctx=heap;ctrl=heap+0x1000;pa=heap+0x2000;pb=heap+0x3000;parent=heap+0x4000
  u.mem_write(ctx+4,struct.pack('<I',ctrl));u.mem_write(ctx+0x228,struct.pack('<I',role));u.mem_write(ctrl+0xb8,struct.pack('<I',pa));u.mem_write(pa+0xc,struct.pack('<I',pb));u.mem_write(pb+4,struct.pack('<I',parent))
  esp=stack+0xf000;ebp=stack+0xe000;u.reg_write(UC_X86_REG_ESP,esp);u.reg_write(UC_X86_REG_EBP,ebp);u.mem_write(ebp+8,struct.pack('<I',ctx));saved={UC_X86_REG_EBX:123,UC_X86_REG_ESI:234,UC_X86_REG_EDI:345,UC_X86_REG_EDX:456}
  for reg,val in saved.items():u.reg_write(reg,val)
  result=[];checks=[]
  def hook(uc,addr,size,data):
   if addr in [base+0x4556,base+0x45c3]:result.append(addr-base);uc.emu_stop();return
   if addr==base+0x1f0b:
    sp=uc.reg_read(UC_X86_REG_ESP);ret,obj,ptr=struct.unpack('<III',uc.mem_read(sp,12));assert obj==parent
    text=bytearray()
    while (c:=uc.mem_read(ptr+len(text),2))!=b'\0\0':text+=c
    expected=text.decode('utf-16le');checks.append(expected);uc.reg_write(UC_X86_REG_EAX,int(expected==name));uc.reg_write(UC_X86_REG_ESP,sp+4);uc.reg_write(UC_X86_REG_EIP,ret)
  u.hook_add(UC_HOOK_CODE,hook);u.emu_start(base+0x4542,base+0x184000,count=1000)
  assert result==[want],(role,name,result,checks)
  assert u.reg_read(UC_X86_REG_ESP)==esp and u.reg_read(UC_X86_REG_EBP)==ebp
  assert all(u.reg_read(reg)==val for reg,val in saved.items())
  assert u.reg_read(UC_X86_REG_EAX)==ctx+0x228 and u.reg_read(UC_X86_REG_ECX)==role
  cases.append({'base':hex(base),'original_hud_role':role,'parent_name':name,'branch':hex(want),'name_checks':checks})
original=pefile.PE('/workspace/attachments/4bd198f4-d35d-488f-88d6-785bfc6b168f/MileniumPortraitNative.dll');assert original.get_data(0x4cb5,0x14)==p.get_data(0x4cb5,0x14)
(R/'TESTE3/TESTE_VISIBILIDADE.json').write_text(json.dumps({'actual_x86_emulated':True,'name_predicate_mocked':True,'original_restore_actor_flags_code_unchanged':True,'cases':cases,'windows_game_executed':False},indent=2));print('PASS: 15 x86 branch executions at 3 bases; only HUD and exact inventory parent enter scoped visibility; flags restoration code unchanged')
