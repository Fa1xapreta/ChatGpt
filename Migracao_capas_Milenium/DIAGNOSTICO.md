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
