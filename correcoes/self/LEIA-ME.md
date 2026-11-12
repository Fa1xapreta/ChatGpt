# L2Playable — mensagens de SELF na tela

Adaptação da referência `Playable.java` para a `net.sf.l2j.gameserver.model.actor.L2Playable` enviada pelo usuário.

## Mensagens

As quatro mensagens foram mantidas em inglês conforme o trecho fornecido, com duração de 6.000 ms:

| Destinatário | Mensagem |
| --- | --- |
| Vítima | `You was SELF !` |
| Atacante | `VITIMA was SELF !` |
| Clã do atacante | `A Clan member ATACANTE left VITIMA was SELF !` |
| Clã da vítima | `A Clan member VITIMA was SELF !` |

A vítima e o atacante recebem suas mensagens pessoais antes das distribuições aos clãs. Cada jogador recebe no máximo uma mensagem de tela por ocorrência. Se os clãs coincidirem, os outros integrantes recebem somente a mensagem do clã da vítima. Integrantes offline, referências nulas e FakePlayers não recebem as novas mensagens de tela.

O atacante é identificado por `killer.getActingPlayer()`, sem converter um NPC diretamente para jogador. Isso também atribui um abate por summon ao dono. Em morte sem atacante jogador, somente vítima e seu clã recebem aviso. Em morte pelo próprio jogador, ele recebe apenas a mensagem pessoal de vítima. Morte de pet/summon não envia ao dono uma falsa mensagem de SELF pessoal. O chat já existente para essas situações não foi removido.

## Quando aparece

A configuração existente `Config.GET_SELF_ANNOUNCE` controla tanto o chat já existente quanto os novos avisos de tela. Nenhuma configuração nova foi criada.

Foram mantidos os seis caminhos de SELF da L2Playable enviada: arena 5x5, arena 9x9, FLAG_AREA_SELF, LSZONE, PARTY_FARM e o ramo de LEAVE_BUFFS_ON_DIE. As regras de remoção/preservação de buffs e a precedência das bênçãos não foram alteradas. Assim como no código original, o ramo de arena sinaliza SELF conforme sua própria condição; esta entrega não altera a relação entre as opções de arena e remoção de buffs.

Os avisos são emitidos ao fim do processamento desta morte, após `onKillUpdatePvPKarma`. Isso evita que o popup de HP enviado nesse callback substitua imediatamente a mensagem de SELF.

Os anúncios atuais de chat (`Broadcast.SelfAnnounce` e `Config.GET_SELF_MSG`) foram preservados, com envio uma vez por ocorrência e o mesmo texto. A mudança acrescenta os avisos de tela, não elimina o chat global.

## Instalação

1. Faça backup do `L2Playable.java` atual.
2. Substitua a classe pelo arquivo em `src/net/sf/l2j/gameserver/model/actor/L2Playable.java` deste ZIP, no mesmo pacote do seu projeto; ou revise/aplique `self-screen.patch`.
3. Mantenha a configuração correspondente a `GET_SELF_ANNOUNCE` habilitada.
4. Recompile o GameServer pelo processo normal e instale o artefato gerado.
5. Teste uma morte nos caminhos de SELF, verificando vítima, atacante e membros online dos dois clãs.

Não precisa substituir `L2PcInstance`, as classes de mineração ou as texturas. O arquivo completo parte da L2Playable de 11.568 bytes enviada nesta conversa; se ela mudou depois, aplique o patch para preservar suas outras alterações.

O ZIP contém fonte, não um JAR pronto. Não copie os arquivos de teste para o core.

## Compatibilidade e validação

Adapta `Player` para `L2PcInstance` e `ClanMember` para `L2ClanMember`. Usa o `phantom.FakePlayer` que já aparece no L2PcInstance do core enviado. Evita o problema da referência em que `player` representa a vítima, mas o texto é de clã do atacante.

A classe L2Playable completa modificada foi compilada com nível Java 8 e testada usando simulações explícitas das APIs do core. Os testes cobrem destinatários, clãs diferentes/iguais/ausentes, membros offline/nulos, NPC/ambiente, summons, FakePlayer, morte repetida, configuração desativada, duração, precedência das bênçãos, os seis caminhos de SELF e prioridade sobre o popup de HP do callback de abate.

Resultado em `test-results.txt`. Para repetir: `python tests/run.py`, com Python 3 e Java contendo `jdk.compiler`.

Não foi compilado ou executado o core completo de produção; ainda é necessário validar a integração no seu GameServer.
