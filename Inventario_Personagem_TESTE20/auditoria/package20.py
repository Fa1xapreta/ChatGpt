from pathlib import Path
import sys,json,hashlib,zipfile,shutil
R=Path(__file__).resolve().parent;P=R.parent;O=P/'TESTE20';D=Path('/workspace/work/github-delivery/Inventario_Personagem_TESTE20');D.mkdir(exist_ok=True)
sys.path.insert(0,'/workspace/work/cloak-audit/vendor');import pefile
new=O/'system/MileniumPortraitNative.dll';pe=pefile.PE(str(new));end=0x1000
for sec in pe.sections:
 assert sec.VirtualAddress==end
 end=sec.VirtualAddress+((max(sec.Misc_VirtualSize,sec.SizeOfRawData)+4095)&~4095)
 assert sec.PointerToRawData+sec.SizeOfRawData<=len(pe.__data__)
assert end==pe.OPTIONAL_HEADER.SizeOfImage
old=pefile.PE(str(P/'TESTE19/system/MileniumPortraitNative.dll'))
assert [(e.name,e.address) for e in old.DIRECTORY_ENTRY_EXPORT.symbols]==[(e.name,e.address) for e in pe.DIRECTORY_ENTRY_EXPORT.symbols]
for n in ['interface.u','interface.xdat']:assert (O/'system'/n).read_bytes()==(P/'TESTE19/system'/n).read_bytes()
a=json.loads((P/'TESTE19/REPLAY_R17.json').read_text())['results'];b=json.loads((O/'REPLAY_R17.json').read_text())['results'];assert [x['camera'] for x in a]==[x['camera'] for x in b]
text='''# Inventory Viewr — TESTE 20: módulos corretos para nomes e anexos

Feche jogo e launcher. Substitua somente system/MileniumPortraitNative.dll no cliente. Mantenha interface.u, interface.xdat, texturas e demais DLLs atuais. O pacote inclui a DLL do TESTE 19 para voltar ao estado com zoom aprovado.

O log R19 enviado contém 2.377 registros: caption=-1 e attach=-1,0,0 em todos eles. Isso confirma que as duas interceptações não foram instaladas; não significa que a lista de anexos tenha sido efetivamente consultada e estivesse vazia.

Foi encontrado um erro concreto no código anterior. No estado privado da DLL original, o campo RVA 0x17dd4 é o módulo Engine e o campo 0x17dd8 é NWindow. A inicialização original confirma essa associação carregando NWindow.dll e guardando o resultado em 0x17dd8. A versão 19 procurava as funções de desenho e verificava as assinaturas em NWindow, embora elas pertençam ao Engine. Os testes anteriores usavam um mesmo endereço fictício para os dois papéis e não verificavam o módulo de GetProcAddress; essa limitação deixou o erro passar.

Esta versão procura DrawTargetName e verifica as funções de desenho dos anexos no Engine correto. A leitura dos dados do jogador continua no NWindow, onde pertence. Core.dll é obtido explicitamente para as funções de nomes/classes. Os testes agora mapeiam Engine e NWindow separadamente e verificam cada argumento de módulo nas consultas de símbolos.

Os nomes e títulos são bloqueados somente durante o desenho da prévia e na mesma thread, preservando a passagem normal do mundo. A passagem dos anexos procura atores com malha na lista Attached, valida o Base, elimina duplicatas e desenha os atores existentes na mesma cena do corpo. Código de Engine incompatível ou previamente interceptado no alvo dos anexos é preservado, com falha registrada. Isso corrige a ativação dessas duas funções; ainda é preciso confirmar que a capa específica está nessa lista e que o resultado visual corresponde ao pedido.

O zoom pela roda, seus limites, a posição dos pés e as setas permanecem. Para impedir que a correção da busca dos ossos altere o tamanho aprovado, a câmera dá prioridade às mesmas dimensões numéricas validadas do TESTE 19. Foram comparadas as coordenadas padrão em 54 cenários, com igualdade; outros 54 cenários verificaram a manutenção desse perfil mesmo com as funções dos ossos disponíveis.

Teste abrindo a prévia: confira se o nome duplicado desapareceu, gire para ver as costas, equipe e retire a capa e confira o zoom. Se algum dos dois continuar falhando, envie system/Inventory_Viewr_R20.log. caption=1 indica o bloqueio instalado; nameCalls conta as chamadas bloqueadas. attach registra estado, quantidade de anexos e quantidade desenhada na passagem anterior. attachPasses informa quantas passagens do personagem chegaram ao callback dos anexos. O log inclui também os módulos e offsets para distinguir falha de ativação de falha em encontrar a capa.

Validação: 18 cenários de personagem/câmera e ciclo do renderer; 18 de bloqueio alternativo, roda, limites, liberação e anexos; 18 de Engine incompatível ou modificado; 54 com dimensões do log sem funções dos ossos e 54 com essas funções disponíveis. Seis bases PE realmente relocadas, módulos Engine/NWindow distintos e verificação de qual módulo atende cada símbolo. PE, exportações, integridade do ZIP e hashes conferidos. As chamadas Engine/Windows são simuladas. O jogo Windows não foi executado neste ambiente; nomes e capa ainda precisam de confirmação no cliente.

Para restaurar, com o jogo fechado copie VOLTAR_TESTE19/system/MileniumPortraitNative.dll para system. A interface não mudou. Nenhum Engine.dll em disco, pacote de capa, aura, textura ou arquivo do servidor é substituído. As interceptações só existem na memória durante a execução do jogo.
'''
(O/'LEIA-ME.txt').write_text(text,encoding='utf-8-sig');(D/'README.md').write_text(text)
backup=O/'VOLTAR_TESTE19/system/MileniumPortraitNative.dll';backup.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(P/'TESTE19/system/MileniumPortraitNative.dll',backup)
files=[new,backup,O/'LEIA-ME.txt']
for n in ['VALIDACAO.json','REPLAY_R17.json','FEATURES.json','UNSUPPORTED_ENGINE.json','CAMERA_WITH_ENGINE_APIS.json']:
 files.append(O/n);shutil.copy2(O/n,D/n)
hashes={str(f.relative_to(O)):hashlib.sha256(f.read_bytes()).hexdigest() for f in files};(O/'SHA256.json').write_text(json.dumps(hashes,indent=2));files.append(O/'SHA256.json')
zpath=D/'Milenium_Inventory_Viewr_TESTE20.zip'
with zipfile.ZipFile(zpath,'w',zipfile.ZIP_DEFLATED) as z:
 for f in files:z.write(f,str(f.relative_to(O)))
with zipfile.ZipFile(zpath) as z:
 assert z.testzip() is None
 for n,h in hashes.items():assert hashlib.sha256(z.read(n)).hexdigest()==h
A=D/'auditoria';A.mkdir(exist_ok=True)
for n in ['viewer.c','engine_patterns.h','compile.py','build.py','test.py','test_features.py','test_unsupported.py','test_log_replay.py','test_cached_priority.py','log_samples.json','iat.h','link.ld','metadata.json','package20.py']:shutil.copy2(R/n,A/n)
main=D.parent/'README.md';main.write_text('## Inventory Viewr — TESTE 20\n\n[Baixar ZIP](Inventario_Personagem_TESTE20/Milenium_Inventory_Viewr_TESTE20.zip) · [Instruções](Inventario_Personagem_TESTE20/README.md). Corrige a referência ao módulo Engine para ativar bloqueio de nomes e desenho de anexos. Mantém o zoom e o enquadramento aprovados. Substitua somente a DLL. Validação visual no cliente ainda necessária.\n\n'+main.read_text());print('ZIP / PE / exports / default camera verified:',zpath.stat().st_size,'bytes')
