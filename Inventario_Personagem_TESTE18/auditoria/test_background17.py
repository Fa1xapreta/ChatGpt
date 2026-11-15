from pathlib import Path
import sys,struct,json
R=Path(__file__).resolve().parent;sys.path[:0]=[str(R/'test-vendor'),'/workspace/work/cloak-audit/vendor']
import pefile
from unicorn import Uc,UC_ARCH_X86,UC_MODE_32,UC_HOOK_CODE
from unicorn.x86_const import *
data=(R/'TESTE17/system/MileniumPortraitNative.dll').read_bytes();cases=[]
for base in [0x10000000,0x10100000,0x10340000,0x33000000,0x6fba0000,0x00500000]:
 p=pefile.PE(data=data);p.relocate_image(base);image=p.get_memory_mapped_image()
 for name in ['MileniumInventoryPreviewWnd','MobInformerWnd','UnknownWnd']:
  for w,h in [(270,429),(206,286)]:
   u=Uc(UC_ARCH_X86,UC_MODE_32);u.mem_map(base,0x188000);u.mem_write(base,image);stack=0x0f000000;heap=0x02000000;u.mem_map(stack,0x10000);u.mem_map(heap,0x10000)
   ctx=heap;ctrl=heap+0x1000;pa=heap+0x2000;pb=heap+0x3000;parent=heap+0x4000
   for ptr,v in [(ctx+4,ctrl),(ctrl+0xb8,pa),(pa+0xc,pb),(pb+4,parent)]:u.mem_write(ptr,struct.pack('<I',v))
   esp=stack+0xf000;ebp=stack+0xe000;u.reg_write(UC_X86_REG_ESP,esp);u.reg_write(UC_X86_REG_EBP,ebp);u.mem_write(ebp+8,struct.pack('<I',ctx))
   api=0x76543210;u.mem_write(base+0x17e14,struct.pack('<I',api))
   args=struct.pack('<9f2I',0,0,w,h,2,44,w+2,h+44,1,heap+0x5000,0xffffffff);u.mem_write(esp,args)
   saved={UC_X86_REG_EBX:123,UC_X86_REG_ESI:234,UC_X86_REG_EDI:345,UC_X86_REG_EDX:0,UC_X86_REG_ECX:heap+0x6000}
   for reg,val in saved.items():u.reg_write(reg,val)
   reached=[]
   def hook(uc,addr,size,data):
    if addr==base+0x2d23:reached.append(True);uc.emu_stop();return
    if addr==base+0x1f0b:
     sp=uc.reg_read(UC_X86_REG_ESP);ret,obj,ptr=struct.unpack('<III',uc.mem_read(sp,12));assert obj==parent
     text=bytearray()
     while (c:=uc.mem_read(ptr+len(text),2))!=b'\0\0':text+=c
     assert text.decode('utf-16le')=='MileniumInventoryPreviewWnd'
     uc.reg_write(UC_X86_REG_EAX,int(name=='MileniumInventoryPreviewWnd'));uc.reg_write(UC_X86_REG_ECX,0xcccccccc);uc.reg_write(UC_X86_REG_EDX,0xdddddddd);uc.reg_write(UC_X86_REG_ESP,sp+4);uc.reg_write(UC_X86_REG_EIP,ret)
   u.hook_add(UC_HOOK_CODE,hook);u.emu_start(base+0x2d1d,base+0x188000,count=1000)
   assert reached and u.reg_read(UC_X86_REG_ESP)==esp and u.reg_read(UC_X86_REG_EBP)==ebp
   assert all(u.reg_read(reg)==val for reg,val in saved.items()) and u.reg_read(UC_X86_REG_EAX)==api
   result=struct.unpack('<9f2I',u.mem_read(esp,len(args)));expected=(0,0,284,419) if name=='MileniumInventoryPreviewWnd' else (2,44,w+2,h+44)
   assert result[4:8]==expected and result[:4]==(0,0,w,h) and result[8:]==(1,heap+0x5000,0xffffffff)
   cases.append({'base':hex(base),'parent':name,'viewport':[w,h],'source_uv':list(result[4:8])})
(R/'TESTE17/TESTE_UV_FUNDO.json').write_text(json.dumps({'real_x86_emulated':True,'engine_name_predicate_mocked':True,'actual_PE_base_relocations_applied_before_emulation':True,'all_registers_stack_and_other_draw_parameters_checked':True,'cases':cases,'windows_tested':False},indent=2));print('PASS: 36 x86 background cases, 6 genuinely relocated images, 2 viewport sizes; exact inventory UV crop, other windows unchanged')
