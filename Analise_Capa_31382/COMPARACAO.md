# Capa 31382 — comparação dirigida após teste no jogo

Personagem informado: humano fighter, como no vídeo de referência. O novo vídeo de 27,4 segundos foi aberto e examinado por quadros. Não é necessário produzir uma filmagem idêntica para esta etapa.

## Igualdades já verificadas

- O registro completo de armorgrp para 31382 tem 3.194 bytes. O trecho acrescentado ao DAT do destino é idêntico ao da referência, não uma reconstrução por aproximação.
- O kit inclui o mesmo CCDraconicCloak.u e as mesmas malhas auxiliares por corpo. Os arquivos do kit foram conferidos por SHA-256 contra a referência.
- CapasClassic.dll, d3d9.dll, CapasClassic.ini e os recursos nativos de efeitos/animação foram copiados sem alterar parâmetros ou código da referência. A alteração feita para retirar o motor anterior fica no carregador dsetup.dll do destino.
- cloak.int não foi substituído: após decodificação, o arquivo que já existia no destino é idêntico ao da referência.
- Foram encontrados, entre os exports do Engine da referência, 20 nomes literais usados pela DLL de capas; os 20 também existem no Engine do destino, nos mesmos RVAs, com os primeiros 64 bytes de cada entrada iguais. Para Core são 12, também coincidentes. Isso não comprova igualdade integral das funções, layouts de objetos ou compatibilidade com hooks aplicados durante a execução.

## Diferenças e limites

Engine, D3DDrv, NWindow e chargrp do destino não são arquivos inteiramente idênticos aos da referência. Em chargrp existem diferenças nas referências de roupas-base, como m000/m001; isso não demonstra, sozinho, defeito na capa. O controlador do destino foi preservado e componentes protegidos do controlador da referência não foram incluídos.

A imagem de uma capa equipada comprova que algum caminho de renderização a desenha. Ela não comprova que todas as rotinas nativas de simulação, interpolação e efeitos estejam ativas. Os vídeos isolados não identificam qual rotina foi executada nem permitem atribuir uma causa única à diferença percebida.

## Diagnóstico interno identificado

A própria CapasClassic.dll cria um mapeamento chamado Local\\CapasClassicLive_v1, de 256 bytes. Em sua inicialização são escritos:

| Offset | Campo confirmado |
|---:|---|
| 0x00 | Assinatura CCLV (0x564C4343) |
| 0x04 | Versão 2 |
| 0x08 | PID do processo |
| 0x0C | Tick da atualização de estado; não presumido como heartbeat contínuo |
| 0x10 | Etapa da inicialização |
| 0x90 | Texto de estado, até 96 bytes |

A análise das instruções mostra mensagens de espera de Engine, Engine carregada, runtime bloqueado e hooks ativos. O estado 3 é escrito com a mensagem hooks ativos após a etapa de instalação. Uma mensagem de runtime bloqueado não prova que a causa seja licença: há verificações internas adicionais.

Foi preparado um verificador de leitura que abre somente esse canal existente, confere os 112 arquivos de cliente do kit e lista os módulos selecionados carregados no processo L2. Não injeta código, não escreve na memória, não altera arquivos do cliente e não modifica licença. O arquivo Resultado_Capas.txt é salvo somente na pasta do verificador.

Verificação do verificador: sintaxe analisada em PowerShell 7.7 e testes com fixtures para canal ativo, bloqueado, ausente e arquivo diferente. Não foi executado contra o jogo Windows nesta sessão. O código usa chamadas compatíveis com Windows PowerShell 5.1 e não depende de sintaxe de métodos genéricos do PowerShell 7.

O resultado real de execução é necessário para localizar a próxima alteração. Nenhum novo ajuste visual foi aplicado nesta etapa e o kit que já funciona foi mantido intacto.


## Resultado real recebido após o primeiro verificador

O relatório enviado confirmou **112 recursos iguais, nenhum ausente ou diferente**. As oito amostras mostraram estado 3, com PID correspondente ao processo aberto e mensagem UTF-8 `hooks ativos — jogando`. Não houve bloqueio de inicialização registrado nessa captura. O primeiro leitor usava CP1252 para esse texto; a leitura foi corrigida para UTF-8 no V2.

Isso confirma que os arquivos conferem e que a DLL chegou à etapa de hooks ativos. Não demonstra que cada callback continue executando ou que cada etapa de animação receba o objeto esperado. A lista de módulos do primeiro relatório veio vazia; ela não foi usada como prova de módulos ausentes.

Foi preparado **Verificar_Capa_31382_V2.zip**, sem alterações no jogo: lê também o nome do objeto publicado em 0x40 e os campos numéricos publicados em 0x14–0x38 e 0xF0–0xFC durante 12 segundos. Os contadores 0x20, 0x24 e 0x28 têm incrementos confirmados em callbacks no código nativo. Os demais campos foram mantidos com nomes de offset, sem atribuir significados não comprovados. Não são ponteiros lidos arbitrariamente do processo, mas campos do canal diagnóstico existente, aberto somente para leitura.

O V2 também inicia o PowerShell de 64 bits via Sysnative quando necessário. Os testes com fixtures cobriram estado ativo, bloqueado, canal ausente e arquivo divergente, incluindo extração do nome e dos campos numéricos. Esses testes não substituem a captura real no Windows.

Não houve troca de Engine, NWindow, parâmetros de tecido ou assets em resposta a esse resultado. A próxima decisão depende de observar o processamento por quadro, não apenas o estado de inicialização.


## Resultado real do V2 e teste de renderização separado

O V2 recebido confirmou novamente 112 arquivos iguais, nenhum ausente/divergente. O objeto publicado foi MFighter_Draconic_m_ad00 em todas as amostras. Os contadores de callbacks 0x20/0x24/0x28 avançaram juntos de 875 a 1560. A inicialização permaneceu em estado 3, hooks ativos — jogando. Isso demonstra execução dos callbacks com o objeto rastreado esperado; não prova equivalência integral da física ou da aparência com o vídeo de referência.

Os valores zero dos campos não nomeados não foram tratados como erros. Não foi identificada uma falha específica de simulação nessa captura. As primeiras amostras apresentam menor incremento dos contadores, seguido de incrementos regulares quando a captura prossegue; isso, isoladamente, não comprova problema de desempenho.

D3DDrv.dll tem o mesmo tamanho (1.277.972 bytes), mesmas tabelas de import/export e mesmos layouts de seções nos dois clientes. Há 19 bytes diferentes em oito intervalos, incluindo duas regiões executáveis. Na versão de destino, uma condição em 0x18328 foi transformada em salto incondicional; outra em 0x1C419 foi substituída por NOPs. A segunda região está junto a mensagens de detecção de suporte AGP; a primeira percorre uma rotina com GetDesktopWindow/GetDC e tabela de 256 entradas. Não se afirmou que essas alterações sejam a causa da diferença da capa.

Foi preparado Milenium_Render_Modelo_TESTE_3.zip: teste A/B de um único componente, usando D3DDrv.dll da referência. Inclui o original do destino e APLICAR/DESFAZER com verificação de hashes, backup e reversão em caso de falha de cópia. Não altera Engine, NWindow, chargrp, Java, XML ou arquivos de montarias. É uma tentativa controlada de igualar a renderização, não correção comprovada da física.

Testes do instalador com arquivos simulados: aplicar/restaurar e repetição idempotente; recusa de versão desconhecida; recusa de payload alterado; restauração após falha de cópia injetada. Não houve execução desse driver no jogo durante a sessão.


## Pedido posterior: montarias e abertura suave ao equipar

Após o usuário informar melhora no TESTE 3, foi preparado um perfil experimental para montarias, sem trocar mais DLLs. A análise do leitor de CapasClassic.ini e de seus consumidores identificou a estrutura retornada pela função 0x10006480: mount_stiffness em +0x08, mount_stiffness_slow em +0x10 e mount_wind_cap em +0x18. No trecho 0x100045EC–0x10004682, os valores de rigidez são interpolados conforme a velocidade e o limite de vento é usado nessa rota montada.

O TESTE 4 modifica somente esses três valores: 0.55→0.45, 0.75→0.60 e 33→24. A intenção é reduzir rigidez e vento montado; a melhora ainda não foi validada no jogo. As chaves de colisão strider_* e os parâmetros globais foram preservados.

O usuário esclareceu que o efeito ao equipar desejado é o tecido abrindo/caindo suavemente. Essa transição **não foi implementada**: não foi localizada uma chave comprovada que a controle diretamente. Não foi apresentada a ativação de vento global ou a alteração de pano_sem_reset como substituta desse efeito.

O instalador modifica o INI existente preservando todos os bytes fora dos três números. Tem backup/restauração com hashes e recusa desfazer automaticamente caso o usuário tenha editado o INI depois do teste. Testes com fixtures: INI original; BOM UTF-8 e CRLF com comentários/segunda seção; recusa de chave ausente/duplicada; recusa de UTF-16; proteção de alterações posteriores; reversão após falha de escrita. Não houve execução do perfil dentro do jogo nesta sessão.
