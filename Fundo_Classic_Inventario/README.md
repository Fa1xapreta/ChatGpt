# Fundo Classic localizado

A textura que corresponde ao fundo da referência é:

`L2UI_CT1.InventoryWnd.Inventory_DF_CharacterEquipBg`

Ela contém as faixas escuras e o emblema circular sob o personagem. Foi extraída do L2UI_CT1.utx do conjunto SysTextures.zip enviado, com tamanho e CRC do arquivo conferidos. Somente os volumes z38 a z41 foram necessários para extrair esse pacote.

## Arquivos

- Inventory_DF_CharacterEquipBg.png: textura original completa, 512 x 512, com transparência.
- Inventory_DF_CharacterEquipBg.tga: os mesmos pixels em TGA 32 bits, sem redesenho ou redimensionamento.
- PREVIA.png: recorte da área útil, 284 x 419; remove somente o espaço transparente externo para visualizar o fundo.
- METADADOS.json: referência completa, coordenadas UV, origem e hashes.

A área útil está em x=0..283 e y=0..418. O restante do atlas é transparente. Amostrar o atlas inteiro sem ajustar o retângulo UV reduz a imagem e deixa espaço vazio. UV: U=0..0.5546875 e V=0..0.818359375.

Este ZIP contém as imagens extraídas para integração; não modifica automaticamente o cliente. O pacote Classic de origem é versão 133/40, enquanto o L2UI_CH3.u atual do usuário é 123/30. A integração deve aproveitar a imagem e preservar o pacote atual, as capas e os efeitos já ajustados.

Também existe FestivalWnd.FestivalWnd_CharacterBg com a mesma arte em outra composição, mas o arquivo separado aqui é o de InventoryWnd.
