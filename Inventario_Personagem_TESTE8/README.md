# Inventory Viewr — TESTE 8

Com o jogo e launcher fechados, copie os TRÊS arquivos da pasta system deste ZIP para a system do cliente, substituindo os anteriores. O interface.xdat mudou neste teste. Mantenha seu L2UI_CH3 atual.

Mudanças:
- Tamanho projetado aproximadamente 10% menor que no TESTE 7, mantendo distância estável ao trocar set.
- Enquadramento deslocado para baixo cerca de 18 pixels na janela padrão.
- Rodapé de diagnóstico oculto, inclusive quando o script atualiza o texto.
- Distância horizontal entre prévia e inventário removida (âncora de -6 para 0).
- Sincronização de AttackItemClassID, DefenseItemClassID, ShieldItemClassID e AttackItemEnchantedValue, além de malhas e materiais das armas e capas. O nível de encantamento agora é transmitido à prévia.

A cor e os emissores de encantamento precisam ser confirmados no cliente. Esta alteração copia os parâmetros da arma; não compartilha emissores ou ponteiros de efeitos em execução do jogador real. Efeitos customizados podem exigir tratamento próprio.

Conferir: abrir a prévia, trocar armas com encantamentos diferentes e comparar a cor com o personagem real. Verificar também posição, rodapé, giro e troca de set/capa.

Validação: seis emulações x86 em bases diferentes com relocação PE aplicada, incluindo cópia do nível de encantamento; 36 casos do fundo; análise do XDAT e dos intervalos consecutivos das seções PE. Chamadas do Engine e Windows simuladas. Cliente Windows não executado aqui.

Para restaurar, copie os três arquivos de VOLTAR_TESTE7/system com o jogo fechado. Não há alterações no servidor nem nas DLLs das capas, aura ou montarias.
