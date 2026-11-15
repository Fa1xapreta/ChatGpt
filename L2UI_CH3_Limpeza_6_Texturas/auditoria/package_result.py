from pathlib import Path
import json,zipfile,hashlib,shutil
R=Path(__file__).resolve().parent;D=Path('/workspace/work/github-delivery/L2UI_CH3_Limpeza_6_Texturas');D.mkdir(exist_ok=True)
report=json.loads((R/'VALIDACAO.json').read_text())
text='''# L2UI_CH3 — teste sem as seis texturas adicionadas

O pacote foi reconstruído a partir das duas partes do ZIP enviado. Foram removidas as texturas:

- AbnormalWnd.TitleViwer
- AbnormalWnd.head_pvp_3
- AbnormalWnd.head_pvp_2
- AbnormalWnd.head_pvp_1
- AbnormalWnd.CounterAttack_on
- AbnormalWnd.board_back5

Também foi removida uma exportação inválida AbnormalWnd.None, marcada como pública, que estava entre as últimas texturas. Esse registro corresponde diretamente à condição da mensagem Assertion failed: Export.ObjectName != NAME_None || !(Export.ObjectFlags & RF_Public). Não foi excluída outra textura com nome válido.

## Testar

Feche o jogo e o editor. Guarde o L2UI_CH3.u anterior. Extraia o ZIP e tente abrir o L2UI_CH3.u desta cópia no seu editor. Se for testar no cliente, substitua no mesmo local em que você usa o seu L2UI_CH3.u. Mantenha interface.u, interface.xdat, DLLs e demais pacotes atuais.

O arquivo U tem cerca de 302 MiB descompactado. O ZIP é menor por compressão; o nome do arquivo interno continua L2UI_CH3.u. O formato original sem criptografia foi preservado.

## O que foi preservado e conferido

As seis texturas e o registro inválido eram as sete últimas exportações. Foram retirados os dados dessas exportações e reconstruído o final da tabela, sem renumerar os objetos anteriores. A tabela de imports foi relocada e seus bytes preservados. As contagens e posições das tabelas no cabeçalho foram atualizadas, incluindo a contagem da geração.

Os dados serializados de todas as 4.570 texturas restantes e dos 145 grupos são idênticos aos originais. Todos os índices, classes, nomes, grupos, flags e offsets dos objetos restantes foram preservados. Nomes, imports, GUID e versão foram mantidos. Foram verificados os limites das referências da tabela e 830 referências de propriedades de objetos/classes; nenhuma aponta para os registros removidos. Não foram encontradas propriedades de array que exigissem remapeamento.

Os recursos atuais de inventário, incluindo InventoryWnd.MileniumCloakSlot, InventoryWnd.Inventory_EquipSlot e AbnormalWnd.Inventory_DF_CharacterEquipBg, permanecem no pacote. Os nomes das texturas removidas podem continuar na tabela global de nomes; não existem mais exportações de textura correspondentes e seus dados foram retirados.

O pacote resultante foi relido e comparado, e o ZIP/hashes foram conferidos. O editor e o jogo Windows não foram executados neste ambiente: esta cópia é para confirmar se a abertura volta a funcionar no seu computador. O pacote original enviado permanece preservado no arquivo de entrada.
'''
(R/'LEIA-ME.txt').write_text(text,encoding='utf-8-sig');(D/'README.md').write_text(text)
shutil.copy2(R/'VALIDACAO.json',D/'VALIDACAO.json')
zpath=D/'L2UI_CH3_sem_6_texturas.zip'
with zipfile.ZipFile(zpath,'w',zipfile.ZIP_DEFLATED,compresslevel=6,allowZip64=True) as z:
 z.write(R/'L2UI_CH3.u','L2UI_CH3.u');z.write(R/'LEIA-ME.txt','LEIA-ME.txt');z.write(R/'VALIDACAO.json','VALIDACAO.json')
with zipfile.ZipFile(zpath) as z:
 assert z.testzip() is None
 h=hashlib.sha256()
 with z.open('L2UI_CH3.u') as f:
  while b:=f.read(1024*1024):h.update(b)
 assert h.hexdigest()==report['result_sha256']
 hzip=hashlib.sha256(zpath.read_bytes()).hexdigest()
(D/'SHA256.json').write_text(json.dumps({'L2UI_CH3.u':report['result_sha256'],'L2UI_CH3_sem_6_texturas.zip':hzip},indent=2))
A=D/'auditoria';A.mkdir(exist_ok=True)
s=(R/'clean_package.py').read_text().replace("D=json.loads((ROOT/'catalog-original.json').read_text())","D=catalog(FILE)")
(A/'clean_package.py').write_text(s);shutil.copy2('/workspace/work/cloak-audit/catalog_unreal.py',A/'catalog_unreal.py');shutil.copy2(R/'package_result.py',A/'package_result.py')
main=D.parent/'README.md';main.write_text('## L2UI_CH3 — teste sem as seis texturas recentes\n\n[Baixar ZIP](L2UI_CH3_Limpeza_6_Texturas/L2UI_CH3_sem_6_texturas.zip) · [Instruções](L2UI_CH3_Limpeza_6_Texturas/README.md). Remove TitleViwer, head_pvp_1/2/3, CounterAttack_on, board_back5 e a exportação pública inválida None que corresponde à asserção do editor. Preserva dados e índices das demais texturas. Abertura no editor Windows ainda precisa de confirmação.\n\n'+main.read_text())
print('ZIP verified:',zpath.stat().st_size,'bytes; unpacked U:',report['result_size'],'bytes; SHA256',report['result_sha256'])
