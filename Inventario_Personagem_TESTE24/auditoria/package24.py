from pathlib import Path
import zipfile,json,hashlib,shutil,sys
P=Path(__file__).resolve().parent;O=P/'TESTE24';D=Path('/workspace/work/github-delivery/Inventario_Personagem_TESTE24');D.mkdir(exist_ok=True)
sys.path[:0]=[str(P),'/workspace/work/cloak-audit'];from catalog_unreal import catalog
old=catalog(P/'TESTE23/system/interface.u');new=catalog(O/'system/interface.u')
assert old['imports']==new['imports'] and old['names']==new['names']
for a,b in zip(old['exports'],new['exports']):assert {k:a[k] for k in ['index','class_ref','super_ref','outer','name','flags']}=={k:b[k] for k in ['index','class_ref','super_ref','outer','name','flags']}
text='''# Inventory Viewr — correção visual 24

Feche jogo e launcher. Copie somente interface.u e interface.xdat da pasta system para a system do cliente. Mantenha a MileniumPortraitNative.dll e as texturas atuais. Esta correção não inclui nem altera a DLL.

A faixa L2UI_CH3.AbnormalWnd.TitleViwer permanece com 256x32, centralizada sobre os 270 pixels do painel do personagem. Sua posição agora usa diretamente a mesma origem do inventário usada para posicionar a prévia: ponto superior esquerdo do inventário, ponto superior direito da faixa, deslocamentos -7 em X e +1 em Y. Assim, a faixa ocupa o intervalo horizontal de -263 a -7 em relação à esquerda do inventário, totalmente dentro dos -270 a 0 do painel do personagem. Seu limite inferior coincide com o início da prévia, em Y=33. Ela fica acima do título do Inventory Viewr, sem atravessar o inventário principal.

A âncora também é aplicada explicitamente ao abrir a janela. A faixa continua abrindo e fechando junto com a prévia. A janela original da prévia, seu fundo, título, área 3D e setas permanecem byte a byte iguais no XDAT.

O tooltip do botão agora mostra somente Inventory Viewr, tanto aberto quanto fechado. Ele usa SetTooltipCustomType com a função SetTooltip já existente no InventoryWnd, seguindo o mesmo mecanismo dos outros botões do inventário. Os ícones ADD/DEL, texturas de mouse e tamanho 32x32 permanecem.

Zoom, capas, tamanho/posição do personagem, bloqueio dos nomes e ausência de geração dos logs continuam com a DLL aprovada, sem substituição.

Verificação: 64 cenários de abertura, fechamento, inicialização e reinício executaram o bytecode real em interpretação simulada, conferindo a âncora aplicada, as texturas e o tooltip personalizado. Saltos, tamanhos expandidos, referências e manutenção dos demais objetos foram conferidos. O XDAT foi relido e comparado, preservando todos os controles não envolvidos. A largura e os limites da faixa foram verificados. Imports, nomes e identidade dos exports da interface permanecem compatíveis com a versão anterior. ZIP e hashes conferidos. O jogo Windows não foi executado neste ambiente; confira a posição visual no cliente.

Para voltar, com o jogo fechado copie os dois arquivos de VOLTAR_TESTE23/system para system. Não é necessário substituir DLLs ou pacotes de textura.
'''
(O/'LEIA-ME.txt').write_text(text,encoding='utf-8-sig');(D/'README.md').write_text(text)
files=[O/'LEIA-ME.txt']
for name in ['interface.u','interface.xdat']:
 files.append(O/'system'/name)
 back=O/'VOLTAR_TESTE23/system'/name;back.parent.mkdir(parents=True,exist_ok=True)
 if not back.exists():back.hardlink_to(P/'TESTE23/system'/name)
 files.append(back)
for name in ['TESTE_UI.json','VISUAL.json']:
 files.append(O/name);shutil.copy2(O/name,D/name)
hashes={str(f.relative_to(O)):hashlib.sha256(f.read_bytes()).hexdigest() for f in files};(O/'SHA256.json').write_text(json.dumps(hashes,indent=2));files.append(O/'SHA256.json')
zpath=D/'Milenium_Inventory_Viewr_TESTE24.zip'
with zipfile.ZipFile(zpath,'w',zipfile.ZIP_DEFLATED) as z:
 for f in files:z.write(f,str(f.relative_to(O)))
with zipfile.ZipFile(zpath) as z:
 assert z.testzip() is None
 for name,h in hashes.items():assert hashlib.sha256(z.read(name)).hexdigest()==h
 assert not any(n.lower().endswith('.dll') for n in z.namelist())
A=D/'auditoria';A.mkdir(exist_ok=True)
for n in ['build_ui24.py','test_ui24.py','test_visual24.py','package24.py']:shutil.copy2(P/n,A/n)
main=D.parent/'README.md';main.write_text('## Inventory Viewr — correção visual 24\n\n[Baixar ZIP](Inventario_Personagem_TESTE24/Milenium_Inventory_Viewr_TESTE24.zip) · [Instruções](Inventario_Personagem_TESTE24/README.md). Faixa 256x32 posicionada acima apenas do painel do personagem e tooltip personalizado Inventory Viewr. Substitua somente interface.u e interface.xdat; mantenha a DLL atual.\n\n'+main.read_text());print('ZIP verified; UI identities compatible; native DLL excluded:',zpath.stat().st_size,'bytes')
