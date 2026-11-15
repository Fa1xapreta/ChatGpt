from pathlib import Path
import zipfile,json,hashlib,shutil,sys
P=Path(__file__).resolve().parent;O=P/'TESTE25';D=Path('/workspace/work/github-delivery/Inventario_Personagem_TESTE25');D.mkdir(exist_ok=True)
sys.path[:0]=[str(P),'/workspace/work/cloak-audit'];from catalog_unreal import catalog
old=catalog(P/'TESTE24/system/interface.u');new=catalog(O/'system/interface.u')
assert old['imports']==new['imports'] and old['names']==new['names']
for a,b in zip(old['exports'],new['exports']):assert {k:a[k] for k in ['index','class_ref','super_ref','outer','name','flags']}=={k:b[k] for k in ['index','class_ref','super_ref','outer','name','flags']}
text='''# Inventory Viewr — cabeçalho integrado 25

Feche jogo e launcher. Copie somente interface.u e interface.xdat da pasta system para a system do cliente. Mantenha a DLL e as texturas atuais.

A faixa TitleViwer agora pertence ao próprio painel Inventory Viewr, no canto superior esquerdo interno, com 270x32: exatamente a largura do painel do personagem. O título foi centralizado sobre a faixa, em vez de ficar abaixo dela. A janela flutuante usada na versão anterior foi removida. A imagem de origem continua usando seus 256x32 pixels completos, esticados apenas na largura para preencher os 270 pixels do painel.

O cabeçalho fica dentro da janela, sem espaço externo e sem avançar sobre o inventário principal. A altura total, a posição e a área 3D do painel permanecem. Personagem, fundo, setas e câmera não foram redimensionados ou deslocados. O botão ADD/DEL de 32x32 e o tooltip Inventory Viewr permanecem.

Verificação: 64 cenários interpretando bytecode da interface, com saltos e referências conferidos. XDAT relido e comparado: a janela flutuante foi eliminada; as demais janelas ficaram byte a byte iguais; no painel, só foram acrescentados a textura do cabeçalho e o ajuste vertical do título. Geometria nativa, área 3D, fundo e setas permanecem byte a byte iguais. Conferidas largura do cabeçalho, título dentro da faixa, ordem dos controles, UV de origem, imports/exports, ZIP e hashes. O jogo Windows não foi executado neste ambiente; confira a aparência no cliente.

Capas, nomes, zoom e ausência de logs continuam com a DLL atual, que não é substituída.

Para voltar, feche o jogo e copie os dois arquivos de VOLTAR_TESTE24/system para system.
'''
(O/'LEIA-ME.txt').write_text(text,encoding='utf-8-sig');(D/'README.md').write_text(text)
files=[O/'LEIA-ME.txt']
for name in ['interface.u','interface.xdat']:
 files.append(O/'system'/name)
 back=O/'VOLTAR_TESTE24/system'/name;back.parent.mkdir(parents=True,exist_ok=True)
 if not back.exists():back.hardlink_to(P/'TESTE24/system'/name)
 files.append(back)
for name in ['TESTE_UI.json','VISUAL.json']:
 files.append(O/name);shutil.copy2(O/name,D/name)
hashes={str(f.relative_to(O)):hashlib.sha256(f.read_bytes()).hexdigest() for f in files};(O/'SHA256.json').write_text(json.dumps(hashes,indent=2));files.append(O/'SHA256.json')
zpath=D/'Milenium_Inventory_Viewr_TESTE25.zip'
with zipfile.ZipFile(zpath,'w',zipfile.ZIP_DEFLATED) as z:
 for f in files:z.write(f,str(f.relative_to(O)))
with zipfile.ZipFile(zpath) as z:
 assert z.testzip() is None
 for name,h in hashes.items():assert hashlib.sha256(z.read(name)).hexdigest()==h
 assert not any(n.lower().endswith('.dll') for n in z.namelist())
A=D/'auditoria';A.mkdir(exist_ok=True)
for n in ['build_ui25.py','test_ui25.py','test_visual25.py','package25.py']:shutil.copy2(P/n,A/n)
main=D.parent/'README.md';main.write_text('## Inventory Viewr — cabeçalho integrado 25\n\n[Baixar ZIP](Inventario_Personagem_TESTE25/Milenium_Inventory_Viewr_TESTE25.zip) · [Instruções](Inventario_Personagem_TESTE25/README.md). Faixa integrada ao topo do próprio painel, com 270x32 e título sobre a textura. Sem janela flutuante. Substitua somente interface.u e interface.xdat.\n\n'+main.read_text());print('ZIP verified; UI identities compatible; native DLL excluded:',zpath.stat().st_size,'bytes')
