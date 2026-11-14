from pathlib import Path
import sys,struct,json
R=Path(__file__).resolve().parent;sys.path[:0]=[str(R/'test-vendor'),'/workspace/work/cloak-audit/vendor']
from unicorn import Uc,UC_ARCH_X86,UC_MODE_32,UC_HOOK_CODE
from unicorn.x86_const import *
import pefile
p=pefile.PE(str(R/'TESTE2/system/MileniumPortraitNative.dll'));image=p.get_memory_mapped_image();root='MileniumInventoryPreviewWnd';cases=[]
# Emulate the actual injected x86 bytes; the original engine-name predicate is mocked.
for base in [0x10000000,0x33000000,0x00500000]:
 for actual,expected_rva,want in [('MobInformerWnd',0x14d28,1),(root,0x14d28,1),('StatusWnd',0x14cfc,1),(root,0x14cfc,0),('TeleportWnd',0x14d10,1),('UnknownWnd',0x14d28,0)]:
  u=Uc(UC_ARCH_X86,UC_MODE_32);u.mem_map(base,0x184000);u.mem_write(base,image);stack=0x0f000000;u.mem_map(stack,0x10000);stop=0x03000000;u.mem_map(stop,0x1000);esp=stack+0xf000;u.mem_write(esp,struct.pack('<III',stop,0x02000000,base+expected_rva));u.reg_write(UC_X86_REG_ESP,esp)
  saved={UC_X86_REG_EBP:0x12340000,UC_X86_REG_EBX:0x12345678,UC_X86_REG_ESI:0x23456789,UC_X86_REG_EDI:0x34567890}
  for reg,val in saved.items():u.reg_write(reg,val)
  calls=[]
  def hook(uc,addr,size,data):
   if addr==stop:uc.emu_stop();return
   if addr==base+0x1f0b:
    sp=uc.reg_read(UC_X86_REG_ESP);ret,obj,ptr=struct.unpack('<III',uc.mem_read(sp,12));text=bytearray()
    while True:
     c=uc.mem_read(ptr+len(text),2)
     if c==b'\0\0':break
     text+=c
    text=text.decode('utf-16le');calls.append(text);uc.reg_write(UC_X86_REG_EAX,int(actual==text));uc.reg_write(UC_X86_REG_ESP,sp+4);uc.reg_write(UC_X86_REG_EIP,ret)
  u.hook_add(UC_HOOK_CODE,hook);u.emu_start(base+0x181230,stop,count=1000)
  assert u.reg_read(UC_X86_REG_EAX)==want,(hex(base),actual,calls)
  assert u.reg_read(UC_X86_REG_ESP)==esp+4
  assert all(u.reg_read(reg)==val for reg,val in saved.items())
  cases.append({'base':hex(base),'actual':actual,'expected_rva':hex(expected_rva),'result':want,'calls':calls})
(R/'TESTE2/TESTE_NATIVE.json').write_text(json.dumps({'cases':cases,'actual_x86_instructions_emulated':True,'engine_name_predicate_mocked':True,'windows_game_tested':False},indent=2))
print('PASS: 18 actual x86 wrapper executions at 3 load bases; original names, added name, rejection, stack and callee-saved registers')
