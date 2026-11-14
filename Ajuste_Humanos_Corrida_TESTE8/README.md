# TESTE 8 — encaixe original e inclinação acompanhando a passada

[Baixar o teste reversível](Milenium_Humanos_Corrida_TESTE8.zip).

1. Feche totalmente o jogo e extraia o ZIP inteiro.
2. Execute **APLICAR.bat**. Ele aceita o Fighter.ukx original ou o TESTE 7 e retira automaticamente o deslocamento do TESTE 7.
3. Confira corrida, ombros, paradas, giros, ataques e montarias nos dois humanos fighter.

O usuário identificou que o deslocamento de âncora do TESTE 7 afasta o topo da capa durante a corrida. Este teste devolve **as posições originais da âncora** e troca o afastamento por uma pequena inclinação variável, limitada a 6 graus, nas seis corridas masculinas e seis femininas.

A fase usa a altura dos pés e seu deslocamento para trás, calculados com as trilhas existentes da passada. Aplica suavização cíclica e amostras por frame. As posições da âncora são idênticas às originais; suas rotações mudam. A geometria do eixo lateral superior foi verificada numericamente, mas isso não substitui a visualização do encaixe no motor do jogo.

**É um teste de inclinação ao redor do encaixe, não uma deformação independente apenas das pontas nem um novo sistema de colisão com os pés.** O movimento é maior quanto mais distante do eixo. A simulação nativa ainda pode alterar o resultado, portanto a redução de interseção e a aparência dos ombros precisam de confirmação no jogo.

O ajuste é restrito aos humanos fighter. As 21.754 outras trilhas e todas as trilhas de movimento raiz foram comparadas; as 143 malhas, eventos e tabelas de sequências foram preservados. Os dois exports de animação foram reconstruídos no fim do pacote, recalculando ponteiros absolutos e a tabela de exports, sem renumerar nomes, imports ou objetos. Os blocos foram interpretados novamente até o fim e conferidos. Não houve execução do cliente Windows neste ambiente.

Os arquivos de DLLs, aura A1, INIs, montarias e outras raças não são alterados. **DESFAZER.bat restaura o Fighter.ukx original, sem TESTE 7/8.** O arquivo encontrado antes do teste também é guardado separadamente em `Milenium_Humanos_Backup_TESTE8/Fighter_antes_TESTE8.ukx`.

O instalador confere os arquivos de entrada e de saída por SHA-256, protege os backups, recupera o estado encontrado se a gravação falhar e recusa sobrescrever alterações posteriores desconhecidas. Os testes de instalação usam os arquivos reais original, TESTE 7 e TESTE 8 no PowerShell 7 Linux, simulando a elevação e a janela de seleção do Windows.

Os scripts Python de construção/verificação dependem dos arquivos e parsers da pasta de análise local; não são necessários para instalar. O descritor contém as curvas calculadas e os hashes do pacote. O arquivo-base do cliente modelo continua indisponível, e o teste é uma adaptação sobre o arquivo do usuário.
