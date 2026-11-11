# ChatGpt
arquivos do chat disponíveis 

## Slots de equipamento

**V6 confirmada no jogo e preservada:** [Slots_separados_v6.zip](Slots_separados_v6.zip). Mantém arma, capa e escudo nas três colunas. [Instruções da V6](Slots_separados_v6_INSTALAR.txt).

### Ajuste da capa para teste — V7

[Capa_ajustada_v7.zip](Capa_ajustada_v7.zip) altera apenas os quadrados da capa e sua textura de botão: remove o desenho duplicado do fundo, preserva a moldura e centraliza o ícone com margem transparente de 3 pixels. A textura do botão fica no grupo InventoryWnd, conforme o caminho usado pelo InventoryWnd.uc enviado. Não altera posições dos controles, pixels da arma/escudo, interface.u ou XDAT.

Substitua apenas system/L2UI_CH3.u, guardando a cópia da V6 para voltar se necessário. Mantenha o XDAT da V6. [Instruções da V7](Capa_ajustada_v7_INSTALAR.txt). A V7 ainda precisa de teste no jogo, com a capa vazia e equipada.

Prévia composta com os assets da V7 (não é captura do jogo):

![Prévia da capa com margem](Capa_ajustada_v7_previa.png)

## Auto Equipe — análise e proposta de fonte

[Análise e limitações](Auto_Equipe_ANALISE.txt) · [Fonte proposto e diff](Auto_Equipe_fonte_para_compilar.zip).

O fonte atual só aciona o conjunto por peitoral/corpo inteiro, usa apenas a lista 0 e não filtra classe nem ignora peças já equipadas. A proposta muda o acionamento para o clique, altera o nome para Auto Equipe e acrescenta seleção de família e fila com confirmação.

**Não é um interface.u pronto para instalar.** Falta compilar com o SDK compatível e validar os IDs/metadados dos conjuntos customizados, além de confirmar a classificação mago/fighter desejada. O ZIP contém fonte e diff, com as regras e limitações documentadas. Não altera os pacotes de textura V6/V7, que foram confirmados pelo usuário no jogo.

### Titanium — causa confirmada nos DATs e correção para teste

Nos DATs enviados, os peitorais Titanium Heavy (9301), Light (9306) e Robe (9311) têm set_ids vazio; o peitoral Draconic (6379) tem a lista preenchida. Isso impede a rotina atual de encontrar as peças Titanium por GetSetItemIDList.

[Titanium_cadastro_sets.zip](Titanium_cadastro_sets.zip) altera apenas esses três campos em itemname-e.dat. [Instruções e escopo](Titanium_cadastro_sets_INSTALAR.txt). Os 10.255 registros foram relidos e comparados após codificação/decodificação 413. Precisa de teste no jogo, clicando no peitoral. Não muda o nome da caixa, o filtro de classe ou o acionamento por outras peças. Não substitui interface.u, XDAT nem texturas confirmadas.
