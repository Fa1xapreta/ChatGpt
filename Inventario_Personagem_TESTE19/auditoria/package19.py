from pathlib import Path
import sys,json,hashlib,zipfile,shutil
R=Path(__file__).resolve().parent;P=R.parent;O=P/'TESTE19';D=Path('/workspace/work/github-delivery/Inventario_Personagem_TESTE19');D.mkdir(exist_ok=True)
sys.path.insert(0,'/workspace/work/cloak-audit/vendor');import pefile
new=O/'system/MileniumPortraitNative.dll';pe=pefile.PE(str(new));end=0x1000
for sec in pe.sections:
 assert sec.VirtualAddress==end
 end=sec.VirtualAddress+((max(sec.Misc_VirtualSize,sec.SizeOfRawData)+4095)&~4095)
 assert sec.PointerToRawData+sec.SizeOfRawData<=len(pe.__data__)
assert end==pe.OPTIONAL_HEADER.SizeOfImage
old=pefile.PE(str(P/'TESTE18/system/MileniumPortraitNative.dll'))
assert [(e.name,e.address) for e in old.DIRECTORY_ENTRY_EXPORT.symbols]==[(e.name,e.address) for e in pe.DIRECTORY_ENTRY_EXPORT.symbols]
for n in ['interface.u','interface.xdat']:assert (O/'system'/n).read_bytes()==(P/'TESTE18/system'/n).read_bytes()
a=json.loads((P/'TESTE18/REPLAY_R17.json').read_text())['results'];b=json.loads((O/'REPLAY_R17.json').read_text())['results'];assert [x['camera'] for x in a]==[x['camera'] for x in b]
text='''# Inventory Viewr — TESTE 19: nomes, anexos e zoom

Feche jogo e launcher. Copie somente system/MileniumPortraitNative.dll para a system do cliente, substituindo a DLL do TESTE 18. Mantenha interface.u, interface.xdat, L2Devil e L2UI_CH3 que já funcionam. O ZIP contém apenas a DLL nova, relatórios e a DLL anterior para restauração.

O tamanho e a posição padrão do TESTE 18 foram preservados. A comparação dos 54 cenários com as dimensões do log R17 produziu exatamente as mesmas coordenadas de câmera antes de usar o zoom. Os arquivos de interface e XDAT não mudam.

Zoom: coloque o cursor dentro do painel e gire a roda para cima para aproximar, para baixo para afastar. A roda é consumida apenas dentro do painel, com vínculo válido e desenho recente. Fora dele ou após liberação da prévia, a mensagem segue para o jogo. Cada passo muda a distância em cerca de 10%; há limites de 0,60 a 1,60 vezes a distância inicial. A posição projetada dos pés é mantida durante o zoom. Fechar e reabrir restaura o tamanho aprovado. As setas continuam controlando a rotação.

Nomes: o log anterior indicava caption=-1, isto é, a interceptação não havia sido instalada. Esta DLL acrescenta uma resolução alternativa por endereço relativo conhecido, validando 24 bytes do Engine e ignorando apenas os operandos que mudam com a relocação PE. A função de desenho de nomes é bloqueada somente durante a passagem da prévia e na mesma thread. Fora dessa passagem, é executada normalmente. A identificação normal dos jogadores no mundo deve ser preservada. Se o código não coincidir com a referência ou houver outra interceptação no alvo, a alternativa é ignorada e o estado aparece no log.

Capas: foi incluída uma passagem adicional para atores com malha anexados ao jogador. Ela usa o mesmo nó de cena, iluminação e câmera do desenho do corpo, obtendo os dados de renderização do ator anexo existente. Não copia a malha, não move nem gira o jogador/capa do mundo. A lista é limitada e validada; entradas repetidas, o próprio jogador, atores cujo Base é outro personagem e anexos sem malha são ignorados. A interceptação da função FDynamicActor.Render só é instalada se o código conferir com a versão analisada. Isso é uma adaptação para anexos; não é prova de que a capa do cliente será encontrada nessa lista ou desenhada com todos os efeitos do módulo CapasClassic.

Teste: abra a prévia, confirme que não aparece nome/título duplicado, gire para ver as costas, equipe e retire uma capa, troque a arma e teste o zoom dentro/fora do painel. Repita com outro personagem. Envie Inventory_Viewr_R19.log se faltar nome oculto ou capa: caption=1 indica interceptação instalada; attach contém estado, quantidade de anexos e quantidade desenhada na passagem anterior; wheel=1 indica captura da roda instalada. O registro contém também o fator de zoom. As contagens não comprovam por si só o resultado visual.

Validação realizada: 18 cenários x86 de personagem/câmera e ciclo do renderer; 54 cenários de dimensões extraídas do log real; 18 cenários de resolução alternativa de nomes, roda de alta resolução, limites de zoom, liberação da prévia, reset e desenho de anexos; 18 cenários com código do Engine incompatível ou modificado, preservando os bytes existentes. Seis bases PE realmente relocadas foram usadas. Estrutura PE, exportações, ZIP e hashes conferidos. As chamadas Engine/Windows são simuladas. Não foi executado o jogo Windows; nomes, capa e interação da roda precisam de confirmação no cliente. O teste de memória dos anexos cobre o callback e seus filtros com renderização simulada, não todos os efeitos internos de um render real.

Para restaurar: com o jogo fechado, copie VOLTAR_TESTE18/system/MileniumPortraitNative.dll para system. Essa restauração da DLL é compatível porque a interface não mudou desde o TESTE 18. Nenhum Engine.dll, pacote de capa, aura, textura ou arquivo do servidor é substituído. As interceptações do Engine ocorrem apenas na memória do processo e deixam de existir ao fechar o jogo.
'''
(O/'LEIA-ME.txt').write_text(text,encoding='utf-8-sig');(D/'README.md').write_text(text)
backup=O/'VOLTAR_TESTE18/system/MileniumPortraitNative.dll';backup.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(P/'TESTE18/system/MileniumPortraitNative.dll',backup)
files=[new,backup,O/'LEIA-ME.txt']
for n in ['VALIDACAO.json','REPLAY_R17.json','FEATURES.json','UNSUPPORTED_ENGINE.json']:
 files.append(O/n);shutil.copy2(O/n,D/n)
hashes={str(f.relative_to(O)):hashlib.sha256(f.read_bytes()).hexdigest() for f in files};(O/'SHA256.json').write_text(json.dumps(hashes,indent=2));files.append(O/'SHA256.json')
zpath=D/'Milenium_Inventory_Viewr_TESTE19.zip'
with zipfile.ZipFile(zpath,'w',zipfile.ZIP_DEFLATED) as z:
 for f in files:z.write(f,str(f.relative_to(O)))
with zipfile.ZipFile(zpath) as z:
 assert z.testzip() is None
 for n,h in hashes.items():assert hashlib.sha256(z.read(n)).hexdigest()==h
A=D/'auditoria';A.mkdir(exist_ok=True)
for n in ['viewer.c','engine_patterns.h','compile.py','build.py','test.py','test_features.py','test_unsupported.py','test_log_replay.py','log_samples.json','iat.h','link.ld','metadata.json','package19.py']:shutil.copy2(R/n,A/n)
main=D.parent/'README.md';main.write_text('## Inventory Viewr — TESTE 19\n\n[Baixar ZIP](Inventario_Personagem_TESTE19/Milenium_Inventory_Viewr_TESTE19.zip) · [Instruções](Inventario_Personagem_TESTE19/README.md). Mantém o enquadramento aprovado do TESTE 18, acrescenta zoom pela roda dentro do painel, resolução alternativa do bloqueio de nomes e desenho de atores anexados. Substitua somente a DLL. Nome e capa precisam de confirmação no cliente.\n\n'+main.read_text());print('ZIP / PE / exports / default camera verified:',zpath.stat().st_size,'bytes')
