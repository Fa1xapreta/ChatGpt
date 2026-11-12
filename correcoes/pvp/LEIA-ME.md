# L2PcInstance — anúncios de sequência de kills

Adaptação do trecho enviado para o core do usuário. Usa `L2PcInstance`, `L2World.getInstance().getL2Players()` e o `sendChatMessage` que já existe na classe.

## Comportamento

Os anúncios são enviados aos jogadores online exatamente nestes marcos:

- 10: `Monster! NOME Killed 10 consecutive players`
- 15: `Legendary! NOME Killed 15 consecutive players`
- 20: `WTF ! NOME Killed 20 consecutive players`

O canal é o CRITICAL_ANNOUNCE do Interlude (18). Não foi acrescentada dependência de uma constante `Say2.CRITICAL_ANNOUNCE` que pode não existir nesse fork; o valor fica em uma constante privada nomeada. Referência de protocolo: [ChatType do L2J Interlude](https://github.com/Hl4p3x/L2JServer_C6_Interlude/blob/master/java/org/l2jserver/gameserver/enums/ChatType.java).

## Integração com o sistema existente

A classe já possui `QuakeSystem()`, o contador `quakeSystem`, recompensas configuráveis e a flag `ALLOW_QUAKE_SYSTEM`. A nova chamada foi integrada a esse método, usando a mesma sequência. Não foi criado um segundo contador concorrente.

É necessário manter `Config.ALLOW_QUAKE_SYSTEM` habilitado na configuração já usada pelo projeto. Os abates continuam seguindo os mesmos critérios e caminhos que já chamavam `QuakeSystem`: as regras de PvP, anti-farm, arena/eventos e PK não foram alteradas. Um abate que o core não encaminha a esse método também não entra na nova sequência.

Nos marcos 10, 15 e 20, a mensagem crítica nova substitui apenas o anúncio Quake antigo daquele marco, evitando dois anúncios Quake para a mesma sequência. As recompensas de `QUAKE_VALUES`/`ALLOW_QUAKE_REWARD` permanecem funcionando. Outros marcos configurados mantêm a mensagem antiga. Os anúncios gerais de PvP do servidor e `QuakeSoundSystem()` não foram alterados.

## Correção do reset

Antes, o contador só era zerado em `doDie` quando `isKillStreak()` já estivesse ativo. Morrer antes do primeiro marco poderia manter a contagem para a próxima vida. Agora `quakeSystem` é zerado imediatamente após `super.doDie(killer)` confirmar a morte. Uma chamada de morte recusada não zera o contador.

O contador permanece em memória, como antes: reconectar ou reiniciar o GameServer também inicia uma nova instância com contador zero.

## Instalação

1. Faça backup do `L2PcInstance.java` atual.
2. Substitua a classe no pacote `net.sf.l2j.gameserver.model.actor.instance` pelo arquivo completo em `src` deste ZIP, ou aplique/revise o patch.
3. Mantenha a configuração de Quake habilitada.
4. Recompile o GameServer e instale o artefato gerado pelo seu processo normal.
5. Teste os marcos 10/15/20 e uma morte antes de atingir dez kills.

O arquivo foi baseado no `L2PcInstance` de 457.381 bytes enviado nesta conversa. Se ele foi editado depois, use o patch para preservar as alterações posteriores. O ZIP contém fonte, não um JAR pronto. Os arquivos de teste não devem ser copiados para o core.

## Validação e limites

O patch mantém intactos `onKillUpdatePvPKarma` e `increasePvpKills`. A integração de mineração e os outros métodos do arquivo foram preservados.

Os métodos editados reais (`quake`, `QuakeSystem` e o trecho inicial de reset de `doDie`) foram extraídos do fonte modificado, compilados com nível Java 8 e executados com simulações explícitas de `Config`, `L2World`, `Broadcast` e jogador. Os testes verificam marcos, destinatários online, canal, ausência de repetição, recompensas, outros marcos e reset com/sem flag ativa. Resultado em `test-results.txt`; repetição com `python tests/run.py`, usando Java com `jdk.compiler`.

Não foi compilado o `L2PcInstance` inteiro com todas as dependências do seu core nem executado um GameServer completo. A compilação integrada e o teste no servidor ainda são necessários.
