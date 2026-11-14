# TESTE 7 — âncora da capa na corrida dos humanos fighter

[Baixar o teste reversível](Milenium_Humanos_Corrida_TESTE7.zip).

Feche totalmente o jogo, extraia e execute **APLICAR.bat** sobre o cliente com TESTE 5. Teste a Freya 31394 correndo, virando, parando, atacando e montando. **DESFAZER.bat** restaura o Fighter.ukx original guardado em `Milenium_Humanos_Backup_TESTE7`.

A mudança é uma adaptação experimental do Fighter.ukx enviado pelo usuário. Ela desloca a componente Y local da âncora `Cape_Bone` em +4 unidades nas seis sequências Run masculinas e seis femininas. **O resultado visual ainda não foi confirmado no jogo. Não foi adicionada colisão com as pernas.** A intenção é avaliar mais folga entre a capa e a passada. A parte superior pode ficar mais afastada durante a corrida.

A alteração afeta todas as capas desses dois corpos durante Run. Foram preservadas as trilhas de pernas/pés, as outras trilhas e sequências, as 143 malhas, os eventos e as tabelas. O diff tem 42 bytes, contidos nos 12 campos de quatro bytes ajustados. As animações de montaria, paradas, ataques e caminhada estão inalteradas, mas as transições a partir de Run também precisam de teste visual.

O script altera somente `Animations/Fighter.ukx` e reconhece apenas o hash do arquivo recebido. Cria backup antes da gravação, confere o resultado e recupera a origem se houver falha de gravação. A restauração recusa sobrescrever alterações posteriores desconhecidas. Os testes de instalação usam o arquivo real recebido no PowerShell 7 Linux, com elevação/interface Windows simuladas; não executam o cliente Windows.

O TESTE 6 permanece retirado. A causa interna do clipping continua sem confirmação. A análise do arquivo atual confirmou compatibilidade dos índices das pernas e das poses-base com as malhas auxiliares das capas. O usuário também confirmou que desligar o MileniumFX por Ctrl+F10 não removeu a interseção; isso não prova que todo o pipeline gráfico esteja fora da causa. O patch do modelo não fornece seu Fighter.ukx base, portanto não se afirma que esta adaptação reproduza a animação do modelo.

Os arquivos de outras raças, DLLs, INIs e dados do servidor não são alterados. Os scripts Python de construção/teste dependem da estrutura e dos arquivos locais da análise, não sendo necessários para instalar. O descritor registra os trechos e hashes conferidos.
