# Correção: início da mineração pela interface

## Erro corrigido

O `MiningInteractionManager.java` enviado chama `stone.beginMiningFromInterface(player)`, mas o `StoneMinerInstance` do projeto não possui esse método.

A correção substitui a chamada por um método privado no próprio `MiningInteractionManager`. Ele:

1. Revalida as condições já usadas por esse controlador (`usable`).
2. Seleciona a pedra como alvo e confirma que a seleção foi aceita.
3. Chama a sessão já existente: `player.getTreasureHuntSession().begin(stone)`.
4. Somente após o sucesso da sessão, inicia `CtrlIntention.ATTACK` contra a pedra.

Se a sessão recusar o início, o fluxo existente encerra a tentativa e não envia um falso sucesso. Não foi adicionado um método vazio que apenas retorna `true`.

## Instalação

1. Faça backup do seu `MiningInteractionManager.java`.
2. Substitua somente essa classe no pacote:
   `net.sf.l2j.gameserver.model.entity.events.treasurehunt`.
3. Salve e recompile o GameServer. Na IDE, atualize o projeto e use o procedimento normal de build.
4. Teste o botão da mineração perto de uma pedra, com início, cancelamento e conclusão.

Não precisa alterar `StoneMinerInstance` para resolver esta chamada. Mantenha as suas outras integrações de registro, ofertas, packets e controle de dano. Esta entrega não muda as classes da limpeza no restart.

O ZIP contém fonte, patch e testes, não um JAR pronto. O arquivo de produção é apenas `src/net/sf/l2j/gameserver/model/entity/events/treasurehunt/MiningInteractionManager.java`. Não copie os arquivos em `tests` para o core.

## Validação realizada

Compilação com nível Java 8 e testes isolados usando o controlador corrigido e as classes reais do mod, incluindo `TreasureHuntSession`, `StoneMinerInstance` e `CustomTreasureHunt`. As APIs do core são simuladas explicitamente nos testes; não é uma compilação do seu GameServer inteiro.

Passaram os cenários de início sem oferta, início válido com ferramenta temporária, ataque após confirmação, Start repetido, cancelamento com recuperação da arma anterior, recusa pela sessão, jogador fora do alcance/offline e pedra invisível. Resultado em `test-results.txt`.

Para repetir os testes: `python tests/run.py`, com Python 3 e Java que contenha `jdk.compiler`. Ainda precisa de build integrado e teste no seu servidor.
