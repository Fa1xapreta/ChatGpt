# Inventory Viewr — TESTE 9

Com jogo e launcher fechados, copie os TRÊS arquivos da pasta system para a system do cliente. Mantenha o L2UI_CH3 atual.

Correção da regressão relatada no TESTE 8: removida a cópia dos três IDs internos AttackItemClassID, DefenseItemClassID e ShieldItemClassID. O sincronismo das malhas volta ao usado pelo TESTE 7, acrescentando apenas o valor de encantamento. O campo do rodapé volta às dimensões originais e fica transparente, em vez de ter tamanho zero.

Mantidos: personagem aproximadamente 10% menor que no TESTE 7, posição mais baixa, painel encostado no inventário, título e rotação. As alterações acima são uma correção candidata: não foi possível reproduzir a renderização do cliente Windows neste ambiente, portanto a causa exata da prévia vazia não está confirmada.

Validado: estrutura PE com seções consecutivas, seis emulações x86 com relocação, 36 casos do fundo e leitura do layout. Chamadas do Engine são simuladas; aparência, cor e brilho do encantamento precisam ser conferidos no jogo.

Se ainda ficar vazio, restaure os três arquivos de VOLTAR_TESTE7/system incluídos e informe o resultado. Essa é a versão que mostrou o personagem na captura anterior.
