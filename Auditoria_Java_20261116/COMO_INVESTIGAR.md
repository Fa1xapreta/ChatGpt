# Como confirmar qual mecanismo está ocorrendo no servidor

O relatório demonstra defeitos no fonte, mas a causa de cada clique perdido deve ser correlacionada com o processo em execução. Uma ocorrência pode envolver mais de um mecanismo.

1. **Conferir a versão em execução.** Identificar o JAR/classpath do gameserver e a origem da compilação. A presença de uma mudança no RAR não comprova que essa versão foi publicada. Registrar um personagem de teste e o horário de uma reprodução com apenas um clique.
2. **Verificar o limitador de bypass efetivo.** Em `config/main/floodprotector.properties`, conferir `FloodProtectorServerBypassInterval` e `FloodProtectorServerBypassLogFlooding`. O fonte usa 5 ticks como padrão, mas o arquivo pode sobrescrever. Para uma janela curta de diagnóstico em teste, a opção existente `FloodProtectorServerBypassLogFlooding=True` mostra rejeições. Manter o intervalo inicialmente; não zerá-lo para esconder o problema. Usar o procedimento de recarga/reinício suportado pelo projeto. Esse log é uma pista: só o primeiro excesso pode ser registrado até outra solicitação ser aceita.
3. **Separar os dois limitadores.** O `ServerBypass` é específico do tipo de ação. `ClientStats`/`dropPacket` opera antes de escolher o tipo e pode bloquear qualquer pacote. Comparar `shortFloods`, `longFloods`, `droppedPackets`, `totalQueueOverflows`, `totalBursts` e tamanho da fila antes/durante/depois da falha. Esses contadores já existem no código; a forma de expô-los precisa ser integrada ao diagnóstico do servidor.
4. **Rastrear por objectId, não no servidor inteiro.** Pontos de instrumentação: entrada de `RequestBypassToServer.readImpl`, início de `runImpl`, antes/depois de `MiningInteractionManager.handle`, resultado do flood, resultado de `validateBypass`, início/fim do handler e contexto do HTML enviado. Registrar correlação, comando/tipo, tamanho da fila e duração monotônica. Para descartar perda anterior ao handler, registrar também rejeições em `L2GamePacketHandler.handlePacket` e enfileiramento. Um log ausente no handler sozinho não prova defeito do cliente: o pacote pode ter sido rejeitado antes.
5. **Identificar mudanças de HTML.** Correlacionar o arquivo/contexto enviado e a atualização da whitelist com a página em que o jogador clicou. Nos comandos `npc_`/`Quest`, registrar se o texto validado ainda possui `%objectId%` e comparar com o comando recebido. O teste da whitelist não deve ser extrapolado a `voiced_`, cuja rota não faz essa validação neste fonte.
6. **Observar locks e workers.** Quando travar, capturar três dumps de threads com alguns segundos entre eles, usando o PID do gameserver e as ferramentas do JDK disponíveis. Exemplo: `jcmd PID Thread.print -l > threads_1.txt`, repetir com `threads_2.txt` e `threads_3.txt`. Procurar `MiningInteractionManager`, `AutofarmSecondaryManager`, geodata/PathFinding, obtenção de conexão e trabalhadores de pacotes parados no mesmo stack. Contar também as threads `phantom.task.ThreadPool` e comparar com 750 × processadores lógicos.
7. **Distinguir os motores de farm.** Registrar ambos os mapas/flags (`isAutoFarm` e `isSecondaryAutoFarm`) antes e depois de OFF. Um estado visual OFF de um motor não comprova que o outro está inativo.
8. **Controlar o teste.** Mesmo personagem, mesmo HTML e mesma ação. Comparar com farm desligado, consultas automáticas desabilitadas apenas na instância de teste e cliente original compatível. Fazer alterações em etapas permite atribuir o resultado a uma causa.

## Leitura dos resultados

| Evidência | Encaminhamento |
|---|---|
| Pacote lido e enfileirado, mas executado só depois de outro pacote | Priorizar F01 (continuação da fila). |
| Retorno false do ServerBypass após uma consulta automática | Priorizar F02 (limitador compartilhado). |
| Descarte anterior ao opcode junto com contadores de flood | Priorizar F15 (tráfego total, especialmente tentativas de skills). |
| `npc_`/`Quest` recebido, mas validação falhou | Investigar F03 e troca de contexto do HTML. |
| Worker parado no monitor da mineração ou do jogador | Medir F06/F07; não retirar todos os locks. |
| Nenhum pacote lido nem rejeição anterior identificada | Avançar para rede/cliente, sem concluir a causa apenas pela ausência de log. |
| OFF confirmado e o outro mapa de farm continua ativo | F08 (motores simultâneos). |
| Pools registram falha de índice/retornam null ou há excesso de threads | F04/F05. |

Os arquivos de configuração, os HTMLs, o esquema SQL, as bibliotecas do transporte e os dados do processo não estavam no RAR. São os complementos necessários para validar as hipóteses condicionais e executar uma compilação/teste integrado. Não é necessário reenviar os 1.817 fontes para essa próxima etapa.
