from pathlib import Path
import zipfile,json,hashlib,shutil
P=Path(__file__).resolve().parent;O=P/'FINAL27';D=Path('/workspace/work/github-delivery/Inventario_Personagem_FINAL');D.mkdir(exist_ok=True)
assert (O/'system/interface.xdat').read_bytes()==(P/'FINAL26/system/interface.xdat').read_bytes()
text='''# Inventory Viewr — final sem faixa, IDs das 34 capas do XML

Com jogo e launcher fechados, copie system/interface.u e system/interface.xdat para a system do patch/cliente. Mantenha todas as DLLs e texturas atuais. O XDAT não mudou em relação à versão final sem faixa; a mudança está na regra compilada de reconhecimento das capas e no fonte InventoryWnd.uc.

IsBordaItemAllowed aceita somente os IDs contidos no XML enviado: 31300 a 31312, 31346 a 31347 e 31380 a 31398. São 34 itens. Os intervalos antigos 31007 a 31073 e 31100 a 31140 foram substituídos. Os intervalos vazios entre os novos grupos não foram liberados.

A regra é usada para reconhecer as capas e encaminhar as equipadas para InventoryWnd.BordaItem, além dos comportamentos de clique e uso já existentes. O XML do servidor não foi alterado. A permissão real de equipar continua dependendo da definição do item no servidor.

A pasta Classes contém os fontes completos InventoryWnd.uc, MobInformerWnd.uc e MobPreviewNative.uc. Copie esses arquivos para Classes do seu projeto Interface se estiver atualizando os fontes. A única alteração em relação às classes finais anteriores está no retorno de IsBordaItemAllowed de InventoryWnd.uc. As outras duas classes e as declarações nativas são idênticas. Os arquivos UC não substituem os compilados da pasta system; para usar o cliente pronto, utilize os arquivos system incluídos.

Os fontes não foram recompilados com UCC neste ambiente. A interface.u pronta foi atualizada diretamente, mantendo imports, exports e demais objetos compatíveis. O predicado foi lido do bytecode real e executado para todos os 65.536 IDs entre 0 e 65535, além dos extremos de int32. O conjunto aceito coincide exatamente com os 34 IDs do XML e com os intervalos do fonte UC. Os tamanhos de salto de curto-circuito foram conferidos. Também passaram os 64 cenários de ciclo de vida da interface. O XDAT e as outras classes permanecem iguais. ZIPs e hashes conferidos. O jogo Windows não foi executado neste ambiente.

Zoom, capas no visor, posição/tamanho do personagem, rotação, botão ADD/DEL de 32x32, tooltip Inventory Viewr, bloqueio de nomes, ausência de faixa e ausência de geração dos logs continuam como estavam. Nenhuma DLL ou textura é substituída.

Para voltar aos IDs antigos, feche o jogo e copie os dois arquivos de VOLTAR_FINAL26/system para system. Essa pasta contém a versão final anterior à troca da lista de capas.
'''
(O/'LEIA-ME.txt').write_text(text,encoding='utf-8-sig');(D/'README.md').write_text(text)
C=D/'Classes';C.mkdir(exist_ok=True)
for f in (O/'Classes').glob('*.uc'):shutil.copy2(f,C/f.name)
files=[O/'LEIA-ME.txt']+list((O/'Classes').glob('*.uc'))
for name in ['interface.u','interface.xdat']:
 files.append(O/'system'/name)
 back=O/'VOLTAR_FINAL26/system'/name;back.parent.mkdir(parents=True,exist_ok=True)
 if not back.exists():back.hardlink_to(P/'FINAL26/system'/name)
 files.append(back)
for name in ['TESTE_UI.json','VISUAL.json','FONTES.json','CAPAS_XML.json','TESTE_IDS.json']:
 files.append(O/name);shutil.copy2(O/name,D/name)
hashes={str(f.relative_to(O)):hashlib.sha256(f.read_bytes()).hexdigest() for f in files};(O/'SHA256.json').write_text(json.dumps(hashes,indent=2));files.append(O/'SHA256.json')
def archive(path,selected):
 with zipfile.ZipFile(path,'w',zipfile.ZIP_DEFLATED) as z:
  for f in selected:z.write(f,str(f.relative_to(O)))
 with zipfile.ZipFile(path) as z:
  assert z.testzip() is None
  for f in selected:assert z.read(str(f.relative_to(O)))==f.read_bytes()
archive(D/'Milenium_Inventory_Viewr_FINAL.zip',files)
archive(D/'Classes_Inventory_Viewr_FINAL.zip',[O/'LEIA-ME.txt']+list((O/'Classes').glob('*.uc'))+[O/'FONTES.json',O/'CAPAS_XML.json',O/'TESTE_IDS.json'])
A=D/'auditoria';A.mkdir(exist_ok=True)
for name in ['build_final27.py','test_ui_final27.py','test_cloak_ids27.py','package_final27.py']:shutil.copy2(P/name,A/name)
main=D.parent/'README.md';main.write_text('## Inventory Viewr — FINAL com as 34 capas do XML\n\n[Pacote final](Inventario_Personagem_FINAL/Milenium_Inventory_Viewr_FINAL.zip) · [Somente classes UC](Inventario_Personagem_FINAL/Classes_Inventory_Viewr_FINAL.zip) · [Instruções](Inventario_Personagem_FINAL/README.md). Regra do slot atualizada para os 34 IDs do XML, no fonte UC e na interface compilada. Inclui as três classes completas; layout final sem faixa preservado. Atualize interface.u/interface.xdat e mantenha as DLLs atuais.\n\n'+main.read_text());print('Final package and full UC classes ZIPs verified')
