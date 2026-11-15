# Inventory Viewr — TESTE 22: exceção de cena no desenho nativo das capas

Feche jogo e launcher. Substitua somente system/MileniumPortraitNative.dll no cliente. Mantenha interface.u, interface.xdat, CapasClassic.dll, texturas e demais arquivos atuais. O pacote inclui a DLL do TESTE 21 para restaurar o estado anterior.

O log R21 tem 30 registros. Nos 29 registros após a primeira passagem, o módulo estava reconhecido, mas cape=-4: nenhuma entrada para o jogador na tabela consultada. O anexo comum passou pela verificação de Base, mas foi descartado por ausência de Mesh (reject=0,1,0). A busca no registro de atores auxiliares não alcançou a capa equipada. Essa busca específica do TESTE 21 foi retirada.

A análise do código de CapasClassic identifica uma exclusão explícita de FActorSceneNode, tipo usado na prévia. Em seu callback de preparação da matriz do submodelo, o trecho compara a opção sombra_ignora_actor e a tabela virtual da cena; se a opção está ativa e a cena é FActorSceneNode, sai sem executar o processamento seguinte. O callback é chamado pelo desvio que o módulo instala na preparação de transformação de USubSkeletalMeshInstance.Render.

O TESTE 22 acrescenta uma exceção nesse ponto somente quando a passagem da prévia está ativa, na mesma thread, com o mesmo ponteiro de cena capturado no desenho do corpo e com o mesmo jogador. Para os demais casos, a instrução CMP original é reproduzida com seu operando relocado, seguida das condições originais. A opção global sombra_ignora_actor não é alterada. O arquivo CapasClassic.dll em disco e seu INI não são substituídos.

A interceptação em memória exige cabeçalho/versão e assinatura de código reconhecidos, incluindo a conferência do endereço relocado da opção. Código incompatível ou já alterado nesse ponto é preservado e gera estado de falha. O desenho e a preparação da capa continuam a cargo do módulo original; não é criado um ator ou uma malha de capa artificial.

Zoom, enquadramento padrão, rotação pelas setas e bloqueio dos nomes permanecem. As coordenadas padrão foram comparadas às versões anteriores em 54 cenários, com igualdade; a prioridade do perfil numérico aprovado continua mesmo com as funções de ossos disponíveis.

Teste com uma capa equipada: abra a prévia e gire para olhar as costas. Troque e retire a capa, mantendo a janela aberta. Confirme nomes e zoom. A exceção de cena foi validada em emulação, mas a presença e a aparência da capa ainda precisam de confirmação no jogo.

Se não aparecer, envie system/Inventory_Viewr_R22.log. clothScene contém estado, número de exceções concedidas, número de consultas durante a prévia e endereço do módulo. Estado 1 significa interceptação instalada; número de exceções maior que zero significa que o callback passou pelo bloqueio para a cena/jogador corretos, sem comprovar pixels visíveis. Estados -1 módulo ausente; -2 cabeçalho/versão incompatível; -3 assinatura ou endereço da opção incompatível; -4 falha de proteção/escrita. clothCtx registra a cena e o jogador vistos na última consulta. Isso permite distinguir uma exclusão ainda ativa de outro estágio do desenho.

Validação específica: 18 cenários em seis bases PE relocadas executaram os bytes reais, relocados, do trecho de decisão de CapasClassic em Unicorn. Antes da mudança, a cena do tipo ator foi recusada com a opção ativa. Após a mudança, a cena e o jogador da prévia foram aceitos; outro ponteiro de cena, outro jogador e outra thread continuaram recusados. Fora da prévia, o destino do salto, registradores e flags coincidiram com os originais. A opção global permaneceu igual. Versão e código incompatíveis não foram sobrescritos. O teste do renderer também verifica que o escopo da cena envolve sua chamada original.

Também passaram os cenários anteriores de câmera/ciclo do renderer (18), nomes/zoom/anexos (18), Engine incompatível (18), limites numéricos sem funções de ossos (54) e com essas funções disponíveis (54). PE, exportações, ZIP e hashes conferidos. As APIs Engine/Windows e o desenho dos pixels são simulados; o jogo Windows não foi executado neste ambiente. A reprodução real do bloqueio não prova sozinha que essa seja a única causa da ausência da capa.

Para restaurar, feche o jogo e copie VOLTAR_TESTE21/system/MileniumPortraitNative.dll para system. As interceptações em memória deixam de existir ao fechar o processo. Nenhum arquivo de servidor, textura, física de capa, aura ou efeito de imagem é substituído.
