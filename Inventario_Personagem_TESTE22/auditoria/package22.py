from pathlib import Path
import sys,json,hashlib,zipfile,shutil
R=Path(__file__).resolve().parent;P=R.parent;O=P/'TESTE22';D=Path('/workspace/work/github-delivery/Inventario_Personagem_TESTE22');D.mkdir(exist_ok=True)
sys.path.insert(0,'/workspace/work/cloak-audit/vendor');import pefile
new=O/'system/MileniumPortraitNative.dll';pe=pefile.PE(str(new));end=0x1000
for sec in pe.sections:
 assert sec.VirtualAddress==end
 end=sec.VirtualAddress+((max(sec.Misc_VirtualSize,sec.SizeOfRawData)+4095)&~4095)
 assert sec.PointerToRawData+sec.SizeOfRawData<=len(pe.__data__)
assert end==pe.OPTIONAL_HEADER.SizeOfImage
old=pefile.PE(str(P/'TESTE21/system/MileniumPortraitNative.dll'))
assert [(e.name,e.address) for e in old.DIRECTORY_ENTRY_EXPORT.symbols]==[(e.name,e.address) for e in pe.DIRECTORY_ENTRY_EXPORT.symbols]
for n in ['interface.u','interface.xdat']:assert (O/'system'/n).read_bytes()==(P/'TESTE21/system'/n).read_bytes()
a=json.loads((P/'TESTE21/REPLAY_R17.json').read_text())['results'];b=json.loads((O/'REPLAY_R17.json').read_text())['results'];assert [x['camera'] for x in a]==[x['camera'] for x in b]
text='''# Inventory Viewr — TESTE 22: exceção de cena no desenho nativo das capas

Feche jogo e launcher. Substitua somente system/MileniumPortraitNative.dll no cliente. Mantenha interface.u, interface.xdat, CapasClassic.dll, texturas e demais arquivos atuais. O pacote inclui a DLL do TESTE 21 para restaurar o estado anterior.

O log R21 tem 30 registros. Nos 29 registros após a primeira passagem, o módulo estava reconhecido, mas cape=-4: nenhuma entrada para o jogador na tabela consultada. O anexo comum passou pela verificação de Base, mas foi descartado por ausência de Mesh (reject=0,1,0). A busca no registro de atores auxiliares não alcançou a capa equipada. Essa busca específica do TESTE 21 foi retirada.

A análise do código de CapasClassic identifica uma exclusão explícita de FActorSceneNode, tipo usado na prévia. Em seu callback de preparação da matriz do submodelo, o trecho compara a opção sombra_ignora_actor e a tabela virtual da cena; se a opção está ativa e a cena é FActorSceneNode, sai sem executar o processamento seguinte. O callback é chamado pelo desvio que o módulo instala na preparação de transformação de USubSkeletalMeshInstance.Render.

O TESTE 22 acrescenta uma exceção nesse ponto somente quando a passagem da prévia está ativa, na mesma thread, com o mesmo ponteiro de cena capturado no desenho do corpo e com o mesmo jogador. Para os demais casos, a instrução CMP original é reproduzida com seu operando relocado, seguida das condições originais. A opção global sombra_ignora_actor não é alterada. O arquivo CapasClassic.dll em disco e seu INI não são substituídos.

A interceptação em memória exige cabeçalho/versão e assinatura de código reconhecidos, incluindo a conferência do endereço relocado da opção. Código incompatível ou já alterado nesse ponto é preservado e gera estado de falha. O desenho e a preparação da capa continuam a cargo do módulo original; não é criado um ator ou uma malha de capa artificial.

Zoom, enquadramento padrão, rotação pelas setas e bloqueio dos nomes permanecem. As coordenadas padrão foram comparadas às versões anteriores em 54 cenários, com igualdade; a prioridade do perfil numérico aprovado continua mesmo com as funções de ossos disponíveis.

Teste com uma capa equipada: abra a prévia e gire para olhar as costas. Troque e retire a capa, mantendo a janela aberta. Confirme nomes e zoom. A exceção de cena foi validada em emulação, mas a presença e a aparência da capa ainda precisam de confirmação no jogo.

Se não aparecer, envie system/Inventory_Viewr_R22.log. clothScene contém estado, número de exceções concedidas, número de consultas durante a prévia e endereço do módulo. Estado 1 significa interceptação instalada; número de exceções maior que zero significa que o callback passou pelo bloqueio para a cena/jogador corretos, sem comprovar pixels visíveis. Estados -1 módulo ausente; -2 cabeçalho/versão incompatível; -3 assinatura ou endereço da opção incompatível; -4 falha de proteção/escrita. clothCtx registra a cena e o jogador vistos na última consulta. Isso permite distinguir uma exclusão ainda ativa de outro estágio do desenho.

Validação específica: 18 cenários em seis bases PE relocadas executaram os bytes reais, relocados, do trecho de decisão de CapasClassic em Unicorn. Antes da mudança, a cena do tipo ator foi recusada com a opção ativa. Após a mudança, a cena e o jogador da prévia foram aceitos; outro ponteiro de cena, outro jogador e outra thread continuaram recusados. Fora da prévia, o destino do salto, registradores e flags coincidiram com os originais. A opção global permaneceu igual. Versão e código incompatíveis não foram sobrescritos. O teste do renderer também verifica que o escopo da cena envolve sua chamada original.

Também passaram os cenários anteriores de câmera/ciclo do renderer (18), nomes/zoom/anexos (18), Engine incompatível (18), limites numéricos sem funções de ossos (54) e com essas funções disponíveis (54). PE, exportações, ZIP e hashes conferidos. As APIs Engine/Windows e o desenho dos pixels são simulados; o jogo Windows não foi executado neste ambiente. A reprodução real do bloqueio não prova sozinha que essa seja a única causa da ausência da capa.

Para restaurar, feche o jogo e copie VOLTAR_TESTE21/system/MileniumPortraitNative.dll para system. As interceptações em memória deixam de existir ao fechar o processo. Nenhum arquivo de servidor, textura, física de capa, aura ou efeito de imagem é substituído.
'''
(O/'LEIA-ME.txt').write_text(text,encoding='utf-8-sig');(D/'README.md').write_text(text)
backup=O/'VOLTAR_TESTE21/system/MileniumPortraitNative.dll';backup.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(P/'TESTE21/system/MileniumPortraitNative.dll',backup)
files=[new,backup,O/'LEIA-ME.txt']
for n in ['VALIDACAO.json','REPLAY_R17.json','FEATURES.json','UNSUPPORTED_ENGINE.json','CAMERA_WITH_ENGINE_APIS.json','CLASSIC_SCENE.json']:
 files.append(O/n);shutil.copy2(O/n,D/n)
hashes={str(f.relative_to(O)):hashlib.sha256(f.read_bytes()).hexdigest() for f in files};(O/'SHA256.json').write_text(json.dumps(hashes,indent=2));files.append(O/'SHA256.json')
zpath=D/'Milenium_Inventory_Viewr_TESTE22.zip'
with zipfile.ZipFile(zpath,'w',zipfile.ZIP_DEFLATED) as z:
 for f in files:z.write(f,str(f.relative_to(O)))
with zipfile.ZipFile(zpath) as z:
 assert z.testzip() is None
 for n,h in hashes.items():assert hashlib.sha256(z.read(n)).hexdigest()==h
A=D/'auditoria';A.mkdir(exist_ok=True)
for n in ['viewer.c','engine_patterns.h','classic_scene_pattern.h','compile.py','build.py','test.py','test_features.py','test_unsupported.py','test_log_replay.py','test_cached_priority.py','test_classic_scene.py','log_samples.json','iat.h','link.ld','metadata.json','package22.py']:shutil.copy2(R/n,A/n)
main=D.parent/'README.md';main.write_text('## Inventory Viewr — TESTE 22\n\n[Baixar ZIP](Inventario_Personagem_TESTE22/Milenium_Inventory_Viewr_TESTE22.zip) · [Instruções](Inventario_Personagem_TESTE22/README.md). Exceção restrita à cena/jogador da prévia no bloqueio de FActorSceneNode do CapasClassic. Preserva opção global de sombras, zoom, nomes e câmera. Substitua somente a DLL. Capa ainda precisa de confirmação visual.\n\n'+main.read_text());print('ZIP / PE / exports / default camera verified:',zpath.stat().st_size,'bytes')
