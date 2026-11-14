from pathlib import Path
import sys,struct,json
R=Path(__file__).resolve().parent;P=R.parent;sys.path[:0]=[str(P/'test-vendor'),'/workspace/work/cloak-audit/vendor'];import pefile
from unicorn import *
from unicorn.x86_const import *
meta=json.loads((R/'metadata.json').read_text());results=[]
for base in [0x10000000,0x10100000,0x10340000,0x33000000,0x6fba0000,0x500000]:
 p=pefile.PE(str(P/'TESTE10/system/MileniumPortraitNative.dll'));p.relocate_image(base);u=Uc(UC_ARCH_X86,UC_MODE_32);u.mem_map(base,0x188000);u.mem_write(base,p.get_memory_mapped_image());u.mem_map(0x2000000,0x100000);u.mem_map(0xf000000,0x10000);u.mem_map(0x40000000,0x380000)
 def put(a,v):u.mem_write(a,struct.pack('<I',v&0xffffffff))
 def get(a):return struct.unpack('<I',u.mem_read(a,4))[0]
 def fput(a,v):u.mem_write(a,struct.pack('<f',v))
 def fg(a):return struct.unpack('<f',u.mem_read(a,4))[0]
 def wide(a):
  r=[]
  while getword:=struct.unpack('<H',u.mem_read(a,2))[0]:r.append(chr(getword));a+=2
  return ''.join(r)
 ctx=0x2001000;actor=0x2002000;world=0x2003000;info=0x2004000;fp=0xf008000
 for a,v in [(ctx+4,0x2005000),(ctx+8,actor),(0x20050b8,0x2006000),(0x200600c,0x2007000),(0x2007004,0x2008000),(base+0x17dd8,0x40000000),(base+0x14550,0x100),(0x403663bc,0x2009000),(info+0x18,100),(info+0x204,world),(fp+8,ctx),(fp-8,50),(fp-12,50),(fp-16,270),(fp-20,429)]:put(a,v)
 for off,v in [(0x100,100),(0x104,200),(0x108,300),(0x120,23),(0x208,340)]:fput(actor+off,v)
 clock={'now':1000,'x':100,'y':100,'down':0,'copy':1,'root':True};calls=[];stubs={};n=[0]
 def stub(name,pop=0):
  a=0x2020000+n[0]*16;n[0]+=1;stubs[a]=(name,pop);return a
 for d in p.DIRECTORY_ENTRY_IMPORT:
  for imp in d.imports:
   if imp.name:
    name=imp.name.decode();pop={'GetTickCount':0,'GetProcAddress':8,'GetForegroundWindow':0,'GetWindowThreadProcessId':8,'GetCurrentProcessId':0,'GetCursorPos':4,'GetAsyncKeyState':4,'ScreenToClient':8}.get(name)
    if pop is not None:put(imp.address,stub(name,pop))
 find=stub('find');event=stub('event',12);construct=stub('construct');arraycopy=stub('arraycopy',12);put(0x200c000,0x200c100);put(base+0x17de4,find)
 hooks={base+0x1000:('read',0),base+0x141e:('offset',0),base+0x1f0b:('name',0),base+0x10cb:('log',0),base+0x5e04:('copy',0),0x4014dd00:('info',0),**stubs}
 def hook(uc,addr,size,ud):
  if addr not in hooks:return
  name,pop=hooks[addr];sp=uc.reg_read(UC_X86_REG_ESP);args=lambda i:get(sp+4+4*i);ret=1
  if name=='read':ret=int((0x2000000<=args(0)<0x2100000)or(0x40000000<=args(0)<0x40380000))
  elif name=='offset':
   field=wide(args(1)); extra={'ExtraMeshDatas':0x500,'Owner':0x60,'ExtraMesh':0x64,'ExtraTex':0x68,'ExtraBodyPart':0x6c}; names=['RightHandMesh','LeftHandMesh','RightArmMesh','LeftArmMesh','CloakMesh','CloakCoverMesh','RightHandSkins','LeftHandSkins','RightArmSkins','LeftArmSkins','CloakSkins','WeaponScale','RightHandBone','LeftHandBone','RightArmBone','LeftArmBone','SpineBone','LowbodyBone','CapeBone','AttackItemEnchantedValue','AttackItemClassID'];ret=0x120 if field=='CollisionHeight' else (0x200 if field=='PortraitFocus' else (extra[field] if field in extra else 0x300+names.index(field)*16))
  elif name=='name':ret=int(clock['root'])
  elif name=='find':ret=0x200a000 if 'PrepareClassic' in wide(args(2)) else 0x200b000
  elif name=='GetProcAddress':
   z=bytes(uc.mem_read(args(1),160)).split(b'\0')[0];ret=construct if b'StaticConstructObject' in z else (arraycopy if b'CopyCompleteValue' in z else (0x200c000 if b'GError' in z else event))
  elif name=='construct':
   assert args(1)==actor and args(2)==0 and args(5)==0x200c100
   ret=0x2040000
  elif name=='arraycopy':
   assert args(0)==actor+0x500 and args(1)==world+0x500 and args(2)==actor
   count=get(world+0x504);put(actor+0x500,0x2050000);put(actor+0x504,count);put(actor+0x508,count)
   if count:put(0x2050000,get(get(world+0x500)))
  elif name=='GetTickCount':ret=clock['now']
  elif name=='GetForegroundWindow':ret=55
  elif name=='GetWindowThreadProcessId':put(args(1),77)
  elif name=='GetCurrentProcessId':ret=77
  elif name=='GetAsyncKeyState':ret=0x8000 if clock['down'] else 0
  elif name=='GetCursorPos':put(args(0),clock['x']);put(args(0)+4,clock['y'])
  elif name=='info':ret=info
  elif name=='copy':calls.append('copy');ret=clock['copy']
  elif name=='event':
   assert uc.reg_read(UC_X86_REG_ECX)==actor
   calls.append('prepare' if args(0)==0x200a000 else 'pose')
   if args(0)==0x200a000:assert get(args(1))==world
  uc.reg_write(UC_X86_REG_EAX,ret);uc.reg_write(UC_X86_REG_ESP,sp+4+pop);uc.reg_write(UC_X86_REG_EIP,get(sp))
 u.hook_add(UC_HOOK_CODE,hook)
 def run():
  sp=0xf00f000;put(sp,0x202f000);put(sp+4,base);put(sp+8,fp);u.reg_write(UC_X86_REG_ESP,sp);u.emu_start(base+meta['symbols']['viewer_tick']-0x10000000,0x202f000,count=10000);assert u.reg_read(UC_X86_REG_EIP)==0x202f000
 put(world+0x500,0x2031000);put(world+0x504,1);put(world+0x508,1);put(0x2031000,0x2030000);put(0x2030060,world);put(0x2030064,0x200d500);put(0x2030068,0x200d600);put(0x203006c,13);put(0x2030070,0xdeadbeef)
 put(world+0x440,7575);put(world+0x300,0x200d000);put(world+0x340,0x200d100);put(world+0x430,25);world_before=bytes(u.mem_read(world,0x500))
 run();assert get(actor+0x430)==25 and get(actor+0x440)==7575
 assert get(get(actor+0x500))==0x2040000 and get(0x2040060)==actor and get(0x2040064)==0x200d500 and get(0x2040070)==0
 assert get(actor+0x300)==0x200d000 and get(actor+0x340)==0x200d100
 initial=[fg(fp-0x54+i*4) for i in range(3)];assert calls==['copy','prepare','pose'],calls
 # Different equipped bounds supplied by original renderer must not change camera.
 clock.update(now=1100,copy=2);fput(actor+0x120,100);fput(fp-0x54,9999);run();assert [fg(fp-0x54+i*4) for i in range(3)]==initial;assert calls[-2:]==['copy','pose']
 clock.update(now=1110,down=1);run();clock.update(now=1120,x=228);run();assert get(fp-0x78)==16384
 clock.update(now=1130,down=0);run();assert get(fp-0x78)==16384
 # Changes to hands/cape must propagate even when the armor cache reports unchanged.
 clock.update(now=1250,copy=2);put(world+0x300,0x200d200);put(world+0x340,0x200d300);world_before=bytes(u.mem_read(world,0x500));run();assert get(actor+0x300)==0x200d200 and get(actor+0x340)==0x200d300;assert calls[-3:]==['copy','prepare','pose']
 clock.update(now=1360);put(world+0x300,0);put(world+0x340,0);world_before=bytes(u.mem_read(world,0x500));run();assert get(actor+0x300)==0 and get(actor+0x340)==0
 clock.update(now=1470);put(0x2030064,0x200d700);run();assert get(0x2040064)==0x200d700
 clock.update(now=1580);put(world+0x504,0);run();assert get(actor+0x504)==0
 before=len(calls);clock.update(root=False,now=1700);run();assert len(calls)==before
 assert bytes(u.mem_read(world,0x500))==world_before
 assert get(0x2030060)==world and get(0x2030070)==0xdeadbeef
 # Execute actual relocated detour and preserve incoming registers/stack.
 clock['root']=True;sp=0xf00f000;u.reg_write(UC_X86_REG_ESP,sp);u.reg_write(UC_X86_REG_EBP,fp);u.reg_write(UC_X86_REG_EBX,12345);u.emu_start(base+0x4a20,base+0x4a26,count=15000);assert u.reg_read(UC_X86_REG_EIP)==base+0x4a26;assert u.reg_read(UC_X86_REG_EAX)==ctx+8;assert u.reg_read(UC_X86_REG_EBX)==12345;assert u.reg_read(UC_X86_REG_ESP)==sp
 results.append({'base':hex(base),'fixed_camera':initial,'equipment_refresh':True,'drag_rotation':True,'source_untouched':True,'actual_relocated_detour':True})
(P/'TESTE10/VALIDACAO.json').write_text(json.dumps({'windows_game_run':False,'engine_calls_mocked':True,'results':results},indent=2));print('PASS',len(results),'relocated bases')
