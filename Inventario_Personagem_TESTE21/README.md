# Inventory Viewr — TESTE 21: ator da capa pelo registro CapasClassic

Feche jogo e launcher. Substitua somente system/MileniumPortraitNative.dll no cliente. Mantenha interface.u, interface.xdat, texturas e demais DLLs que já funcionam. O pacote contém a DLL do TESTE 20 para restaurar.

O log R20 enviado tem caption=1 e attach=1 nos 124 registros. A passagem dos anexos estava ativa, chegando a 6.049 chamadas, mas a contagem era attach=1,1,0 ou attach=1,2,0: um ou dois anexos encontrados e nenhum desenhado. O log antigo não registrava em qual filtro eles eram descartados. O bloqueio de nomes estava ativo e acumulava chamadas bloqueadas.

A adaptação desta versão acrescenta uma busca no registro próprio do módulo CapasClassic. A análise de seu binário de referência identifica uma tabela de 96 registros, cada um de 184 bytes, com o jogador no primeiro campo e seu ator principal de capa no segundo. A rotina nativa de busca compara o jogador e exige que o ator da capa não seja nulo; a rotina de liberação destaca e destrói esse ator. A nova DLL consulta essa relação para obter o ator da capa existente, mesmo que ele esteja fora de Actor.Attached ou esteja sem Base.

A leitura depende de uma versão reconhecida de CapasClassic.dll já carregada: timestamp, SizeOfImage, assinatura de código e operando relocado da tabela precisam conferir. A DLL não é carregada à força nem substituída. O código não escreve no registro das capas nem altera equipamento, posicionamento, física ou ciclo de vida dos atores. O desenho usa o mesmo nó de cena, iluminação e câmera do corpo. Se o ator já foi desenhado pela lista normal, não é desenhado outra vez. Apenas o registro pertencente ao jogador da prévia é aceito.

Zoom, posição padrão, setas e bloqueio dos nomes permanecem. Os arquivos da interface e XDAT não mudam. A igualdade da câmera padrão foi conferida com os mesmos 54 cenários do log, inclusive quando as funções dos ossos estão disponíveis.

Teste com a capa equipada: abra a prévia, gire para olhar as costas, retire e equipe a capa e troque por outro modelo. Confirme também que o zoom e os nomes continuam corretos. Esta é uma adaptação para o registro nativo conhecido, ainda sem confirmação visual no jogo.

Se a capa não aparecer, envie system/Inventory_Viewr_R21.log. O campo cape registra estado, índice do registro, ator e malha. Estado 3 significa que a chamada de desenho foi feita pelo registro; 2 significa que o ator já havia sido desenhado como anexo. Os estados negativos distinguem: -1 módulo ausente; -2 versão/cabeçalho incompatível; -3 assinatura ou operando da tabela diferente; -4 jogador sem registro ativo; -5 ator inválido; -6 malha ausente/inválida; -7 dados de renderização incompatíveis. Uma chamada realizada não comprova pixels visíveis. O campo reject conta descartes por Base, malha e dados de renderização na lista normal.

Validação: 18 cenários do novo registro, em seis bases PE relocadas, usando cabeçalho e assinatura reais do CapasClassic com registros de atores simulados. Verificados: capa fora de Attached e sem Base; registro de outro jogador ignorado; entrada vazia ignorada; retirada e substituição da capa; deduplicação; ausência do módulo; versão ou operando incompatível; ator igual ao jogador e malha ausente rejeitados; registro e memória dos atores preservados pelo callback com renderização simulada. Também passaram os testes anteriores de câmera, zoom, nomes, ciclo do renderer, versões incompatíveis do Engine e perfis numéricos. PE, exportações, ZIP e hashes conferidos.

As APIs Engine/Windows e a renderização nos testes são simuladas. O jogo Windows não foi executado neste ambiente. A presença e a aparência da capa precisam ser confirmadas no cliente, assim como a compatibilidade da versão de CapasClassic efetivamente instalada.

Para voltar: com o jogo fechado, copie VOLTAR_TESTE20/system/MileniumPortraitNative.dll para system. Nenhum arquivo de servidor, Engine.dll em disco, CapasClassic.dll, textura ou configuração de aura é substituído.
