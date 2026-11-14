# Inventory Viewr — TESTE 12

Feche o jogo e o launcher. Copie os TRÊS arquivos da pasta system para a system do cliente, substituindo os anteriores. Neste teste interface.u também mudou. Mantenha os pacotes L2Devil.u e L2UI_CH3 já instalados.

## O que foi conferido

O ZIP coletado contém exatamente interface.u, interface.xdat e MileniumPortraitNative.dll do TESTE 11. Não foi encontrado erro de cópia. L2Devil.u contém as seis texturas RotateLeft/RotateRight com estados normal, Down e Over, todas 64x32 e com canal alfa válido.

O DDS Inventory_Equip_Slot_NoBr é um atlas de ícones de slots, 256x512 DXT3; não é o fundo do personagem nem as setas. Não foi aplicado, pois não foi solicitada a troca dos slots.

## Alterações

- BindPreview e SetStageBackground passam a ser chamadas independentes, fora da atualização de texto do controle RenderStatus oculto. Essa dependência era frágil: um contexto inválido no TextBox poderia pular as chamadas que inicializam a prévia. A causa exata da captura vazia ainda não foi reproduzida no cliente Windows.
- As duas setas recebem suas texturas por ButtonHandle.SetTexture ao abrir/atualizar o painel e são explicitamente mostradas. O protótipo do botão usa as flags do botão funcional do inventário, sem herdar o tooltip de cristalização.
- O SetAnchor executado pelo script agora usa deslocamento horizontal zero; anteriormente ele sobrescrevia o valor ajustado apenas no XDAT.
- Restaurado o cálculo de câmera do TESTE 9, que mostrou o personagem. O novo afastamento/deslocamento do TESTE 10 não faz parte deste teste. O alinhamento final na rosa dos ventos continua pendente.
- Rotação somente pelas setas, por clique ou mantendo pressionado. Arrastar no corpo não gira.
- Registro de até cinco quadros em system/Inventory_Viewr_R12.log, se o cliente puder escrever nessa pasta. Registra ator, dimensões e câmera para investigar o caminho de desenho. Não registra conta, senha ou conversas.

## Conferir

Abra o inventário e a prévia. Confira se aparece o personagem e se as duas setas estão visíveis. Clique e mantenha pressionada cada seta. Se o personagem continuar invisível, envie Inventory_Viewr_R12.log e uma captura. Se não houver log, informe que ele não foi criado.

Capa e brilho de encantamento continuam pendentes. Este pacote não os declara corrigidos.

## Validação

As funções modificadas foram decodificadas com verificação de saltos/referências/contextos. O bytecode real passou por oito casos simulados de abertura/fechamento, HUD expandido/recolhido e jogador ausente, verificando a inicialização independente do rodapé e a ativação das setas. Seis emulações x86 após relocação PE verificaram os controles e o caminho de gravação do diagnóstico; 36 casos verificaram o fundo. As APIs do jogo e do Windows são simuladas: não equivalem à confirmação de renderização no cliente Windows.

## Restaurar

Com jogo fechado, copie os três arquivos de VOLTAR_TESTE9/system para a system do cliente. Nenhum arquivo do servidor, das capas do mundo, montarias, aura ou efeitos de imagem é substituído.
