# Migração das capas Milenium: diagnóstico do cliente atual

**Estado: análise inicial concluída; troca não implementada nem testada.** Nenhum arquivo instalado no servidor ou cliente foi alterado. Esta pasta não é um patch de instalação.

## Conjunto analisado

ZIP dividido completo: 36 partes mais system1.zip. Diretório central com 289 entradas e 2.640.679.776 bytes descompactados. Foram extraídos seletivamente 51 arquivos de dados/configuração/binários, com CRC e tamanho conferidos. Isso não equivale a extrair e testar todos os recursos do cliente.

## Motor atual

- l2.exe importa DSETUP.dll e CliExt.dll.
- dsetup.dll referencia carregamento de dsetup_base.dll e CustomWndSchuster.dll. Portanto, não é um carregador exclusivo de capas.
- dsetup_base.dll contém o motor L2Feel Cloak v2.3.20 IVAN, referências a LineShieldCloaks, recepção de configuração do servidor, deformação e renderização das capas.
- Recursos antigos no arquivo enviado: essence_capes.bin (121.485.008 bytes), essence_cloth.bin (28.622.331 bytes), essence_fx.bin (4.253.229 bytes). O pacote LineShieldCloaks não está entre as entradas da system e pode estar nas pastas externas.
- d3d9.dll contém MileniumFX, MileniumRain e leitura de MileniumFX.ini. Não deve ser substituído como se fosse exclusivo das capas.
- L2modExCore.dll cita CapasClassic.dll e preservação de encadeamento. É um indício favorável à coexistência, não comprovação de carregamento e execução compatíveis.
- O controlador atual em L2.ini é Interface.CustomPlayerController. A referência usa LucDev.LucDevPC; copiar todo o L2.ini mudaria outras funções.

## Conflitos confirmados

1. A referência usa d3d9.dll para carregar CapasClassic.dll e chamar CCD3DSync. O destino usa o mesmo nome de DLL para efeitos Milenium. É necessário adaptar ou encadear os carregadores, preservando as duas funções.
2. Engine.dll, D3DDrv.dll e NWindow.dll são diferentes entre os dois clientes. Core.dll, npkscrypt.dll e ogg.dll coincidem por SHA-256. Isso não valida automaticamente os hooks do novo motor na Engine atual.
3. Cloak.xml possui 42 itens: 31100–31140 e 31073. Todos usam back e multiplicadores de HP/CP de 1.05. A referência tem 25 registros customizados identificados em 313xx. Não existe correspondência automática de 42 capas para 25.
4. cloak_system.properties descreve mapeamento 31007–31073 para visuais 31000–31066, diferente do intervalo principal do XML. É preciso conferir o código que envia as configurações, sem tratar comentários como prova do comportamento efetivo.
5. Os seis pacotes LucDev estão protegidos. Não foi recuperada sua lógica interna ou demonstrada a remoção integral de referências. Renomear esses binários diretamente pode quebrar o carregamento. A licença de CapasClassic não foi alterada.
6. Reunir texturas em MileniumCloaks.utx exige reconstruir materiais e referências, inclusive caminhos eventualmente fixos no motor. Os PNGs da auditoria não são um pacote UTX pronto.

## Alterações delimitadas para quando a integração estiver pronta

Retirar o carregamento do motor de capas em dsetup_base.dll e guardar seus recursos em backup; não simplesmente apagar uma DLL que o carregador procura. Editar somente registros específicos de armorgrp/itemname, mantendo os demais itens. Preservar interface.u, interface.xdat, autoequip, controlador atual, efeitos, login, retratos e mod de janelas. Não apagar registros do inventário/banco de dados dos jogadores para trocar visuais. Registrar e preservar os bônus atuais, salvo decisão explícita de rebalancear.

## Arquivos necessários para fechar a implementação

- Código Java que lê cloak_system.properties e envia os parâmetros. Localizar por EnableCloakSystem, SendClientConfig, EssenceClothWidthScale ou reload cloak.
- Fontes do carregador dsetup.dll e do proxy MileniumFX d3d9.dll, ou interfaces/versões que permitam integrar o novo motor sem retirar essas funções.
- Fonte/SDK ou integração documentada de CapasClassic compatível com esta Engine. A DLL possui etapa de licença e ainda não foi validada neste destino.
- Recursos associados em Animations/SysTextures, principalmente os ícones e dependências externas da referência.

Sem esses elementos não há uma troca pronta que cumpra simultaneamente a preservação dos demais mods, a ausência integral de LucDev e a execução igual à referência. Uma implementação própria Milenium é outra alternativa, mas demanda desenvolvimento e testes no jogo; não pode ser apresentada como simples renomeação do sistema protegido.

## Evidências desta pasta

conferencia_42_itens.csv cruza os nomes XML e cliente. current-server-items.json preserva a estrutura dos 42 itens. current-selected-inventory.json registra tamanho, CRC e SHA-256 dos 51 arquivos selecionados. Não foram publicados configurações de login nem binários do cliente.

## Atualização: pacote-fonte enviado em seguida

Recebidos L2Feel_Cloak_GitHub_v2.2_FINAL.rar e CloakSystemManager.java adaptado ao servidor net.sf.l2j. O RAR contém fontes C++ do motor antigo (cloakhook.cpp, cloth.h, essence.h, fx.h e auxiliares) e o patch Java com ExCloakSystem. Embora o nome externo diga v2.2, VERSION informa 2.3.21; isso não prova correspondência exata com a DLL instalada, cujas strings identificam 2.3.20.

Evidências novas:

- O C++ fixa LineShieldCloaks, visuais 31000–31066 e slot 13. O gerenciador Java enviado aceita IDs por capa em 31007–31073 ou 9400–9466; chaves específicas para 31100–31140 seriam rejeitadas nesse parser. Ajustes globais e por corpo continuam sendo outra categoria.
- ExCloakSystem utiliza 0x81 e controles D0–D3. O gerenciador enviado chama disable() quando EnableCloakSystem ou SendClientConfig está falso. No C++, BEGIN com versão zero apenas coloca g_srvActive=-1, restaurando fallback local; não descarrega o motor. Portanto, mudar a configuração para False não é uma remoção das capas.
- O projeto também contém efeitos de drop: InitThread/Install chama InstallDropFxHooks e LoadConfig habilita dropFx. É preciso separar essas funções antes de retirar o motor antigo. O pacote fornecido pode ser uma revisão diferente da instalada; não presumimos equivalência integral.
- O projeto compila uma DLL chamada dsetup, que no cliente atual foi incorporada como dsetup_base.dll atrás de outro carregador. O fonte desse carregador externo e do proxy MileniumFX não foi identificado neste RAR.
- CMakeLists.txt referencia third_party/minhook, mas esse diretório não apareceu entre os arquivos extraídos; há avisos de licença do MinHook. A dependência de build precisa ser completada e a compilação verificada antes de prometer uma DLL pronta. Os scripts BAT recebidos não foram executados.
- O RAR é fonte do motor antigo, não o fonte de CapasClassic ou dos pacotes LucDev protegidos. A integração do motor de referência e a retirada de nomes internos continuam sem solução comprovada.

Isso permite estudar a separação do motor antigo de outras funções, mas não conclui a instalação do sistema de referência. Nenhuma DLL modificada foi produzida nesta etapa.

## Atualização: Animations e SysTextures da referência recebidos

### Integridade do conjunto e recursos identificados

- Animations: 23 partes mais animations.zip; diretório central validado com 492 entradas e 936.230.354 bytes descompactados.
- SysTextures: 14 partes mais systextures.zip; diretório central validado com 203 entradas e 858.341.064 bytes descompactados.
- Os nomes de todas as partes necessárias estão presentes na remessa. Isso comprova a composição nominal dos conjuntos; não equivale a conferir o CRC dos 695 arquivos. A extração foi seletiva para evitar duplicar quase 1,8 GB de recursos alheios às capas.
- Extraídos e conferidos por CRC/tamanho: CapasClassicIcons.utx, iconfox-v3.utx e DynastyCloak.ukx. Hashes em recursos_externos_capas.csv.
- CapasClassicIcons contém 65 Engine.Texture; iconfox-v3 contém 231. As 25 referências de ícones do mapa de capas foram resolvidas com sucesso nos dois pacotes, incluindo os nomes dos objetos, sem depender de suposição pelo nome do arquivo.
- DynastyCloak.ukx contém 28 SkeletalMesh e duas texturas. Há também DynastyCloak.u na system da referência. Ambos têm 32 exports, mesmos nomes/imports e versão 123/licença 30, mas os arquivos e payloads serializados não são idênticos. Tamanhos de textura e ScriptText diferem; a comparação bruta de payloads não prova que todas as geometrias sejam distintas. Não foi escolhida uma versão arbitrariamente.
- RaccoonNewMantles e RaccoonNewMantles_R não aparecem nos diretórios dos conjuntos externos. Os scripts legados que os mencionam continuam sem essas dependências, mas não foi demonstrado que essa rota seja a ativa no motor nativo.

### Separação de LucDev: nova evidência

Os imports dos 100 pacotes de capas selecionados apontam principalmente para Core/Engine, com referências adicionais a NWindow e CCFxMesh. Não apareceu import direto de LucDev nesses pacotes. Também não apareceu a string LucDev nas strings legíveis examinadas de CapasClassic.dll.

Isso permite considerar **omitir os pacotes/controlador LucDev**, em vez de renomear internamente binários protegidos, mantendo Interface.CustomPlayerController do cliente atual. Não comprova independência total: carregamentos dinâmicos e comportamento de inicialização ainda exigem verificação no jogo. Não foram removidos avisos de autoria/licença.

### Retirada do motor antigo: candidato preparado em laboratório

A análise das instruções de dsetup.dll confirma que o carregador monta o caminho dsetup_base.dll, carrega a biblioteca e resolve **ordinal 11**. dsetup_base.dll exporta esse ordinal como DirectXSetupGetVersion, encaminhado para dsetup_ms.DirectXSetupGetVersion. dsetup_ms.dll já possui a mesma função no ordinal 11.

Foi preparada uma cópia de laboratório alterando exclusivamente a string de destino `%sdsetup_base.dll` para `%sdsetup_ms.dll`, mantendo o tamanho e todo o restante do arquivo. O carregamento de CustomWndSchuster foi preservado. Isso evita inicializar o motor antigo e suas funções de drop pelo caminho encontrado, mantendo o destino original da função encaminhada. Outras rotas de carregamento dinâmico não foram descartadas por teste de execução.

Verificações estáticas: ocorrência única do texto original, export ordinal 11 dos dois destinos, igualdade de todos os bytes fora do trecho alterado, mesmo tamanho final e PE ainda legível. Hashes e offset em loader-patch-review.json. Não foi executado, instalado ou disponibilizado como patch pronto. O objetivo é retirar o motor anterior; isso **não instala** o novo motor.

### O que ainda impede declarar a troca pronta

1. Integrar os registros de capas ao armorgrp/itemname do destino, sem substituir o DAT inteiro da referência e sem perder seus demais itens. Definir a correspondência das 42 capas existentes com os 25 visuais mapeados ou manter os IDs novos em teste. Não foram apagados itens de jogadores.
2. Validar CapasClassic na Engine atual e seu carregamento sem controlador LucDev. O usuário esclareceu que os efeitos Milenium/Discord/drop não precisam ser preservados; portanto o conflito com MileniumFX pode ser resolvido na versão de teste substituindo seu proxy, em vez de exigir a coexistência dos efeitos. As montarias e a interface continuam dentro do escopo de preservação.
3. Resolver a versão carregada de DynastyCloak e referências externas base do cliente, como LineageWeaponsTex, com teste de carregamento de objetos.
4. Verificar a etapa de licença do motor de referência no destino sem modificá-la. A disponibilidade dos recursos externos não equivale a disponibilidade de seu fonte.
5. Executar testes no cliente Windows: inicialização, equipar/remover, 14 corpos, movimento/montaria e observação por outro jogador. A análise desta sessão permanece estática.

A exigência de concentrar todas as texturas num único UTX ainda não foi implementada. Reempacotar exige mudar referências de materiais e conferir as referências nativas; não basta juntar PNGs ou renomear extensões.
