from pathlib import Path
import zipfile,json,hashlib,shutil
P=Path(__file__).resolve().parent;O=P/'FINAL26';D=Path('/workspace/work/github-delivery/Inventario_Personagem_FINAL');D.mkdir(exist_ok=True)
assert (O/'system/interface.u').read_bytes()==(P/'TESTE25/system/interface.u').read_bytes()
text='''# Inventory Viewr — versão final sem faixa

A faixa TitleViwer foi removida. Personagem, título, tamanho, posição, capas, efeitos, zoom, rotação, botão ADD/DEL de 32x32 e tooltip Inventory Viewr foram preservados. A geração de logs do Inventory Viewr continua desativada pela DLL já instalada.

## Atualizar o patch/cliente pronto

Feche jogo e launcher. Copie interface.u e interface.xdat da pasta system para a system do patch/cliente. A interface.u é idêntica à da versão 25; a mudança visual está no XDAT, que remove somente a faixa. Mantenha as DLLs atuais, especialmente Interface.dll e MileniumPortraitNative.dll, e os pacotes L2UI_CH3/L2Devil que já estão funcionando. Nenhuma DLL ou textura é substituída por este pacote.

## Classes UC completas para o projeto

A pasta Classes contém:

- InventoryWnd.uc: classe completa baseada na enviada por você, com abertura/fechamento da prévia, vínculo ao jogador real, texturas do botão ADD/DEL, tooltip, fundo, setas e limpeza ao reiniciar/fechar o inventário.
- MobInformerWnd.uc: classe completa, com a limpeza da prévia do inventário ao abrir a janela de monstros que compartilha o renderer.
- MobPreviewNative.uc: declarações nativas originais, incluídas como dependência. Não foram acrescentadas novas funções nativas.

Atualize esses arquivos na pasta Classes do seu projeto Interface. Os demais fontes do seu projeto permanecem. Os arquivos UC são fontes de interface; não devem substituir os arquivos compilados da pasta system. O funcionamento nativo de câmera, zoom, capas e bloqueio de nomes continua nas DLLs existentes; ele não foi convertido para UnrealScript.

Os fontes foram montados a partir das classes completas que você enviou, preservando propriedades, defaults e todas as funções não envolvidas. Foram alteradas apenas cinco funções de InventoryWnd e OnShow de MobInformerWnd. O helper de tooltip do InventoryWnd já existente é reutilizado. Em MobInformerWnd, a chamada ao helper usa explicitamente o script InventoryWnd. O resultado comportamental corresponde ao da interface compilada incluída, mas uma recompilação gera outro binário e depende do seu projeto, pacotes base e compilador.

As classes UC foram revisadas e comparadas, mas não foram compiladas com UCC neste ambiente. Para usar a versão pronta, não é necessário recompilar: os arquivos da pasta system já estão preparados.

## Verificação

64 cenários de ciclo de vida interpretaram o bytecode da interface. O XDAT foi relido e comparado à versão anterior: somente a textura da faixa foi retirada; os demais dados de janelas e controles permanecem byte a byte iguais. Confirmados ausência de referências à faixa/janela flutuante e interface.u idêntica à versão 25. Nos fontes UC, a restauração das seis funções modificadas reproduz exatamente as classes originais enviadas por você, incluindo defaults e todas as demais funções. MobPreviewNative.uc está byte a byte igual à declaração original. ZIPs e hashes conferidos. O jogo Windows e a compilação UCC dos fontes não foram executados neste ambiente.

## Restaurar

Com o jogo fechado, copie os dois arquivos de VOLTAR_TESTE25/system para system. A pasta contém a interface/XDAT anteriores. Não é necessário mexer nas DLLs.
'''
(O/'LEIA-ME.txt').write_text(text,encoding='utf-8-sig');(D/'README.md').write_text(text)
C=D/'Classes';C.mkdir(exist_ok=True)
for f in (O/'Classes').glob('*.uc'):shutil.copy2(f,C/f.name)
files=[O/'LEIA-ME.txt']+list((O/'Classes').glob('*.uc'))
for name in ['interface.u','interface.xdat']:
 files.append(O/'system'/name)
 back=O/'VOLTAR_TESTE25/system'/name;back.parent.mkdir(parents=True,exist_ok=True)
 if not back.exists():back.hardlink_to(P/'TESTE25/system'/name)
 files.append(back)
for name in ['TESTE_UI.json','VISUAL.json','FONTES.json']:
 files.append(O/name);shutil.copy2(O/name,D/name)
hashes={str(f.relative_to(O)):hashlib.sha256(f.read_bytes()).hexdigest() for f in files};(O/'SHA256.json').write_text(json.dumps(hashes,indent=2));files.append(O/'SHA256.json')
def archive(path,selected):
 with zipfile.ZipFile(path,'w',zipfile.ZIP_DEFLATED) as z:
  for f in selected:z.write(f,str(f.relative_to(O)))
 with zipfile.ZipFile(path) as z:
  assert z.testzip() is None
  for f in selected:assert z.read(str(f.relative_to(O)))==f.read_bytes()
archive(D/'Milenium_Inventory_Viewr_FINAL.zip',files)
archive(D/'Classes_Inventory_Viewr_FINAL.zip',[O/'LEIA-ME.txt']+list((O/'Classes').glob('*.uc'))+[O/'FONTES.json'])
A=D/'auditoria';A.mkdir(exist_ok=True)
for name in ['build_final26.py','test_ui_final26.py','test_visual_final26.py','write_sources_final26.py','package_final26.py']:shutil.copy2(P/name,A/name)
main=D.parent/'README.md';main.write_text('## Inventory Viewr — FINAL sem faixa\n\n[Pacote final](Inventario_Personagem_FINAL/Milenium_Inventory_Viewr_FINAL.zip) · [Somente classes UC](Inventario_Personagem_FINAL/Classes_Inventory_Viewr_FINAL.zip) · [Instruções](Inventario_Personagem_FINAL/README.md). Faixa removida e restante preservado. Inclui as três classes UC completas para o projeto. Atualize interface.u/interface.xdat e mantenha as DLLs atuais.\n\n'+main.read_text());print('Final package and full UC classes ZIPs verified')
