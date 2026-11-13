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
