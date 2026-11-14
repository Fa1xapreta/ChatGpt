## Humanos fighter — TESTE 8, sem o afastamento do TESTE 7

[**Baixar teste de inclinação na passada**](Ajuste_Humanos_Corrida_TESTE8/Milenium_Humanos_Corrida_TESTE8.zip) · [Escopo e limites](Ajuste_Humanos_Corrida_TESTE8/README.md)

O TESTE 7 foi retirado após relato de afastamento da capa nos ombros. O TESTE 8 restaura as posições originais da âncora e testa inclinação suave ligada à passada; não implementa deformação independente apenas das pontas nem nova colisão. Instala sobre o original ou TESTE 7 e preserva a aura A1. O resultado visual ainda precisa de confirmação no jogo. DESFAZER.bat restaura o original sem TESTE 7/8.

---

## Aura Milenium — restauração A1

[**Baixar a restauração da aura**](Restaurar_Aura_Milenium_A1/Milenium_Restaurar_Aura_A1.zip) · [Causa e instruções](Restaurar_Aura_Milenium_A1/README.md)

A DLL da aura recusa o D3DDrv.dll do modelo instalado no TESTE 3/5. O pacote devolve apenas o driver original aceito, preservando o Fighter.ukx instalado e os arquivos dos efeitos de imagem. Feche o jogo, execute RESTAURAR_AURA.bat, abra o cliente e pressione F10 sem Ctrl. O retorno visual ainda precisa de confirmação no jogo. Há backup, desfazer e verificação de logs caso permaneça ausente.

---

## TESTE 7 — histórico retirado por afastamento nos ombros

[Restauração do TESTE 7](Ajuste_Humanos_Corrida_TESTE7/Milenium_Humanos_Corrida_TESTE7.zip) · [Escopo e limites](Ajuste_Humanos_Corrida_TESTE7/README.md)

Aplicação retirada: o usuário confirmou melhora nos pés, mas encontrou afastamento da capa nos ombros. Use DESFAZER.bat para restaurar ou consulte o TESTE 8 experimental. O TESTE 6 também continua retirado.

---

## Humanos fighter — comparação dirigida ao TESTE 5

[**Resultado da análise**](Analise_Humanos_TESTE5/README.md): malhas humanas, mapeamentos dos 25 itens originais e DLL de capas do TESTE 5 coincidem com o modelo. A causa da interseção na corrida ainda não foi confirmada. O Fighter.ukx do cliente atual foi recebido e analisado: índices das pernas e poses-base conferem com as malhas auxiliares das capas; as 12 sequências de corrida humanas foram lidas. Falta a contraparte Fighter.ukx do cliente completo do modelo para comparar as duas corridas. O Fighter-v.ukx do patch só contém malhas de armaduras. Não foi publicado outro ajuste experimental.

---

## TESTE 6 retirado — restauração das capas

As capas deixaram de aparecer no cliente após o TESTE 6. **Feche o jogo e execute DESFAZER.bat** da pasta desse teste. A aplicação foi bloqueada na versão publicada.

[**Baixar ferramenta de restauração**](Ajuste_Corrida_Freya_31394/Milenium_Corrida_Freya_TESTE6.zip) · [Instruções](Ajuste_Corrida_Freya_31394/README.md)

A ferramenta usa a DLL original de `Milenium_Corrida_Backup_TESTE6`, dentro do cliente. A correção dos pés continua pendente.

---

## Multisell das 70 capas novas

[**Baixar cinco multisells (16 + 16 + 16 + 16 + 6) + HTML da loja**](Milenium_Capas_Integrado_70/Multisell_Capas_70_5_Botoes.zip). Cloaks.xml, Cloack2.xml, Cloack3.xml e Cloack4.xml contêm 16 capas cada; Cloack5.xml contém 6. O HTML contém apenas os cinco botões das capas, respeitando o limite de 16 por botão. Mantém o preço original (20453 × 300000, 20454 × 200, 3470 × 200) e entrega uma capa. Use o HTML no arquivo original da loja. As 26 capas de corpos específicos estão identificadas nos comentários, mas a multisell não restringe a compra por corpo.

## Capas — catálogo 70 e integração gráfica TESTE 5

[**Baixar o kit em um ZIP**](https://github.com/Fa1xapreta/ChatGpt/archive/refs/heads/kit-capas-integrado-70.zip) · [Instruções](Milenium_Capas_Integrado_70/README.md) · [DATs e XML separados](Milenium_Capas_Integrado_70/DAT_XML_Capas_70.zip)

Extraia fora do jogo, execute **INSTALAR.bat** e depois **INSTALAR_XML.bat**, escolhendo a pasta items do gameserver. O Java anterior permanece igual. Há backup e restauração.

70 itens: **44 com malhas para 14 corpos e 26 de corpos específicos**; 21 entradas sem dependências suficientes não estão habilitadas. A nova ponte procura manter MileniumFX e o contorno original junto das capas; compilação e testes simulados passaram, mas a recuperação visual ainda precisa de confirmação no cliente Windows. O efeito de abrir/cair ao equipar permanece pendente. Os DATs separados dependem do kit completo.

---

## Kit anterior — TESTE 2 (histórico)

[**Baixar o kit completo TESTE 2**](https://github.com/Fa1xapreta/ChatGpt/archive/refs/heads/kit-capas-completo-teste2.zip) · [Instrucoes simples](Milenium_Capas_Kit_Completo_TESTE_2/README.md)

Inclui os recursos do modelo. Extraia fora do jogo e execute INSTALAR.bat. Aceita a copia manual dos tres arquivos do TESTE_1. Versao de teste; o motor ainda precisa ser validado no jogo.

---

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

### Auto Equipe — DATs para teste

[Auto_Equipe_DATs.zip](Auto_Equipe_DATs.zip) inclui o sysstring-e.dat com a entrada 1907 renomeada para **Auto Equipe** e o itemname-e.dat com o cadastro dos três conjuntos Titanium. [Instruções e escopo](Auto_Equipe_DATs_INSTALAR.txt).

Substitui apenas esses dois DATs, após backup. Mantém texturas V7, XDAT e interface.u. O nome e os cadastros foram validados por releitura e codificação/decodificação 413. Precisam de confirmação no jogo. O clique em qualquer peça e o filtro de classe continuam pendentes da compilação da lógica; a rotina atual ainda aciona pelo peitoral/corpo inteiro.

## TreasureHunt — limpar pedras ao reiniciar o GameServer

[TreasureHunt-limpeza-no-restart.zip](TreasureHunt-limpeza-no-restart.zip) · [Instalação e validação](TreasureHunt-limpeza-no-restart_LEIA-ME.md).

Altera apenas TreasureHuntActiveStore.java e TreasureHuntManager.java: o journal de pedras da execução anterior é limpo na inicialização, sob lock exclusivo. A primeira rodada nova espera o intervalo configurado. Reload preserva as pedras atuais. Histórico de locais e recuperação de equipamentos dos jogadores são mantidos. É necessário substituir as duas classes no projeto e recompilar o GameServer; não contém um JAR pronto.

Compilação com nível Java 8 e testes isolados com as classes reais do mod e simulações do core passaram, incluindo encerramento abrupto de JVM, nova inicialização sem restauração, geração posterior, reload, preservação de histórico/recuperação e lock concorrente. Ainda precisa de validação integrada no GameServer do usuário.

## Mineração — método de início ausente

[MiningInteractionManager-corrigido.zip](MiningInteractionManager-corrigido.zip) · [Classe corrigida](correcoes/mineracao/MiningInteractionManager.java) · [Instruções](correcoes/mineracao/LEIA-ME.md).

Corrige a chamada a `StoneMinerInstance.beginMiningFromInterface`, que não existe nessa versão. O controlador passa a iniciar pela sessão existente (`getTreasureHuntSession().begin(stone)`) e somente após sucesso solicita o ataque. Substitui apenas MiningInteractionManager.java e exige recompilar o GameServer. Não modifica a limpeza das pedras no restart. Compilação e testes isolados passaram; ainda é necessário validar com o core de produção.

## PvP — anúncios de sequência de kills

Versão atual: [L2PcInstance-kill-streak-sem-SYS.zip](L2PcInstance-kill-streak-sem-SYS.zip) · [Classe completa](correcoes/pvp/L2PcInstance.java) · [Patch incremental](correcoes/pvp/remover-SYS.patch) · [Instruções](correcoes/pvp/LEIA-ME.md).

Mantém os anúncios novos em 10, 15 e 20 kills e remove do QuakeSystem os anúncios antigos SYS (Dominating, Rampage etc.). Preserva contagem, reset na morte e recompensas configuradas. Mantenha AllowQuakeSystem=True. A L2Playable/SELF não precisa de alteração. Os testes isolados passaram; ainda exige recompilar e validar com o core. A primeira entrega continua disponível como L2PcInstance-kill-streak.zip.

## SELF — somente os avisos de tela

Versão atual: [L2Playable-SELF-sem-SYS.zip](L2Playable-SELF-sem-SYS.zip) · [Classe](correcoes/self/L2Playable.java) · [Patch incremental](correcoes/self/remover-SELF-SYS.patch) · [Instruções](correcoes/self/LEIA-ME.md).

Remove somente o anúncio vermelho SYS de SELF no chat. Mantém os avisos de tela de seis segundos para os mesmos destinatários, condições de área e regras de buffs. Mantenha AllowSelfAnnounce=True para os avisos de tela. Não altera anúncios de kills. Compilação e testes isolados passaram; requer recompilar o GameServer e validar integrado. A versão anterior permanece em L2Playable-SELF.zip.


## Auditoria das capas do cliente de referência

[Relatório técnico](Auditoria_capas_referencia/RELATORIO.md), inventário completo, mapa de itens, separador PowerShell, texturas extraídas e evidências em [Auditoria_capas_referencia](Auditoria_capas_referencia/). Análise estática; os binários originais não foram executados ou republicados.


## Milenium — capas, versao de teste 1

[Baixar kit](Milenium_Capas_TESTE_1.zip) e [instrucoes](Milenium_Capas_TESTE_1_LEIA-ME.md). Inclui DATs com 25 capas, XML, fonte Java, instalador e restauracao. Usa os recursos originais da system do modelo como entrada local. Candidato sem teste no jogo; nao e substituicao final validada.
