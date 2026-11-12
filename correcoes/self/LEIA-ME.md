# SELF — remover somente o SYS vermelho do chat

Remove somente as chamadas Broadcast.SelfAnnounce da L2Playable adaptada nesta conversa. Mantém os avisos ExShowScreenMessage de seis segundos para vítima, atacante e membros online dos respectivos clãs, com a deduplicação e os destinatários que já estavam funcionando.

As condições das áreas, regras de buffs, prioridade das bênçãos, envio após o popup de HP e sistema de kills permanecem como estavam. Nenhuma inclusão na Chaotic Free foi feita.

## Instalação

1. Faça backup do L2Playable.java atual.
2. Substitua somente a classe net.sf.l2j.gameserver.model.actor.L2Playable pelo arquivo em src deste ZIP, ou aplique o pequeno patch remover-SELF-SYS.patch se tiver outras alterações locais.
3. Recompile o GameServer e instale o artefato gerado.
4. Mantenha AllowSelfAnnounce=True (Config.GET_SELF_ANNOUNCE), pois essa configuração também habilita os avisos de tela que ficam.
5. Teste o SELF: o SYS vermelho desse evento não deve aparecer no chat; os avisos de tela continuam.

A correção não bloqueia mensagens vermelhas de outros sistemas e não muda L2PcInstance.java, anúncios de kills ou a mineração. O ZIP contém fonte, não JAR pronto.

A classe completa compilou com nível Java 8 usando simulações explícitas das APIs do core. Os testes isolados confirmaram ausência do chat SELF e preservação dos avisos de tela, destinatários, duração, áreas e demais comportamentos. Resultado em test-results.txt. Ainda precisa de compilação integrada e teste no seu servidor.
