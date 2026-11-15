from pathlib import Path
import sys,json,hashlib,zipfile,shutil
R=Path(__file__).resolve().parent;P=R.parent;O=P/'TESTE23';D=Path('/workspace/work/github-delivery/Inventario_Personagem_TESTE23');D.mkdir(exist_ok=True)
sys.path.insert(0,'/workspace/work/cloak-audit/vendor');import pefile
pe=pefile.PE(str(O/'system/MileniumPortraitNative.dll'));end=0x1000
for sec in pe.sections:
 assert sec.VirtualAddress==end
 end=sec.VirtualAddress+((max(sec.Misc_VirtualSize,sec.SizeOfRawData)+4095)&~4095)
 assert sec.PointerToRawData+sec.SizeOfRawData<=len(pe.__data__)
assert end==pe.OPTIONAL_HEADER.SizeOfImage
old=pefile.PE(str(P/'TESTE22/system/MileniumPortraitNative.dll'));assert [(e.name,e.address) for e in old.DIRECTORY_ENTRY_EXPORT.symbols]==[(e.name,e.address) for e in pe.DIRECTORY_ENTRY_EXPORT.symbols]
a=json.loads((P/'TESTE22/REPLAY_R17.json').read_text())['results'];b=json.loads((O/'REPLAY_R17.json').read_text())['results'];assert [x['camera'] for x in a]==[x['camera'] for x in b]
text='''# Inventory Viewr — atualização visual 23

Feche jogo e launcher. Copie os TRÊS arquivos da pasta system para a system do cliente, substituindo os anteriores: interface.u, interface.xdat e MileniumPortraitNative.dll. Esta atualização muda a interface e o XDAT, além de retirar o registro de diagnóstico da DLL. Mantenha L2UI_CH3 e os demais pacotes atuais que contêm as texturas indicadas.

A faixa L2UI_CH3.AbnormalWnd.TitleViwer aparece no tamanho original de 256x32, centralizada acima do título. Ela é uma decoração separada, ancorada à prévia, que abre e fecha junto com ela. A janela original da prévia e TODOS os seus controles foram mantidos byte a byte no XDAT: posição, tamanho, área 3D, fundo, título e setas não foram redimensionados ou deslocados.

O botão de abrir/fechar agora tem 32x32. Com a prévia fechada, usa L2UI_CH3.ShortcutWnd.AS_ShortcutWnd_ADD e AS_ShortcutWnd_ADD_Over ao passar o mouse. Com a prévia aberta, usa AS_ShortcutWnd_DEL e AS_ShortcutWnd_DEL_Over. Como não foi fornecida uma textura Down, o estado pressionado usa a textura Over correspondente. A descrição ao passar o mouse é Abrir Inventory Viewr ou Fechar Inventory Viewr. Os estados são atualizados no clique, na inicialização e nos fechamentos do inventário ou da sessão. A janela continua abrindo pelo clique.

Foi desativada a geração dos arquivos Inventory_Viewr_R*.log. O logger antigo da DLL já estava desativado e permanece assim. Não é necessário manter os logs antigos do Inventory Viewr; os arquivos que já existem podem ser apagados manualmente. Esta atualização não desativa os registros de outros módulos do cliente.

Capas, bloqueio dos nomes, rotação, zoom e enquadramento seguem a implementação aprovada do TESTE 22. O código de diagnóstico não escreve mais arquivos; o código de desenho não ganhou novos ajustes de câmera ou de capa. A exceção restrita da cena do CapasClassic permanece.

Verificação realizada: bytecode das seis funções alteradas decodificado, com saltos e referências conferidos; 64 cenários de interface executados em interpretação simulada, verificando ADD/DEL, texturas de mouse, descrição e abertura/fechamento da faixa. Todos os objetos não envolvidos permanecem iguais ao original. O XDAT foi lido novamente e comparado ao instalado: janela da prévia inteira e demais janelas/controles não envolvidos ficaram byte a byte iguais; só o botão do inventário foi alterado e uma janela de decoração foi acrescentada.

Também passaram os testes x86 de nomes, zoom, desenho da capa no trecho real de decisão do CapasClassic e enquadramento, em seis bases relocadas, com chamadas Engine/Windows simuladas. As chamadas de diagnóstico/arquivo foram proibidas nos testes dos callbacks. Conferidos ausência do nome de arquivo de diagnóstico e de referências aos imports de escrita no código novo, retorno imediato do logger antigo, PE, exportações, ZIP e hashes. O jogo Windows não foi executado neste ambiente; confira a aparência das texturas no seu cliente.

Para voltar, feche o jogo e copie os TRÊS arquivos de VOLTAR_TESTE22/system para system. Essa pasta contém a DLL e a interface/XDAT que você aprovou antes do ajuste visual. Nenhum arquivo de servidor, pacote de capa, aura ou textura é substituído por esta atualização.
'''
(O/'LEIA-ME.txt').write_text(text,encoding='utf-8-sig');(D/'README.md').write_text(text)
files=[O/'LEIA-ME.txt']
for name in ['interface.u','interface.xdat','MileniumPortraitNative.dll']:
 files.append(O/'system'/name)
 back=O/'VOLTAR_TESTE22/system'/name;back.parent.mkdir(parents=True,exist_ok=True)
 if not back.exists():back.hardlink_to(P/'TESTE22/system'/name)
 files.append(back)
for name in ['TESTE_UI.json','VISUAL.json','SEM_LOGS.json','VALIDACAO.json','FEATURES.json','CLASSIC_SCENE.json','UNSUPPORTED_ENGINE.json','REPLAY_R17.json','CAMERA_WITH_ENGINE_APIS.json']:
 files.append(O/name);shutil.copy2(O/name,D/name)
hashes={str(f.relative_to(O)):hashlib.sha256(f.read_bytes()).hexdigest() for f in files};(O/'SHA256.json').write_text(json.dumps(hashes,indent=2));files.append(O/'SHA256.json')
zpath=D/'Milenium_Inventory_Viewr_TESTE23.zip'
with zipfile.ZipFile(zpath,'w',zipfile.ZIP_DEFLATED) as z:
 for f in files:z.write(f,str(f.relative_to(O)))
with zipfile.ZipFile(zpath) as z:
 assert z.testzip() is None
 for name,h in hashes.items():assert hashlib.sha256(z.read(name)).hexdigest()==h
A=D/'auditoria';A.mkdir(exist_ok=True)
for n in ['viewer.c','engine_patterns.h','classic_scene_pattern.h','compile.py','build.py','test.py','test_features.py','test_unsupported.py','test_log_replay.py','test_cached_priority.py','test_classic_scene.py','log_samples.json','iat.h','link.ld','metadata.json','package23.py']:shutil.copy2(R/n,A/n)
for n in ['build_ui23.py','test_ui23.py','test_visual23.py']:shutil.copy2(P/n,A/n)
main=D.parent/'README.md';main.write_text('## Inventory Viewr — atualização visual 23\n\n[Baixar ZIP](Inventario_Personagem_TESTE23/Milenium_Inventory_Viewr_TESTE23.zip) · [Instruções](Inventario_Personagem_TESTE23/README.md). Faixa TitleViwer de 256x32 acima do título, botão ADD/DEL de 32x32 com mouse e descrição, logs do Inventory Viewr desativados. Preserva o funcionamento aprovado do TESTE 22. Copie os três arquivos de system.\n\n'+main.read_text());print('ZIP / PE / exports / default camera verified:',zpath.stat().st_size,'bytes')
