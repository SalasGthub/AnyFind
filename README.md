<div align="center">

<img src="src/main/resources/assets/anyfind/icon.png" width="120" alt="AnyFind">

# AnyFind

**Dejá de revolver cuarenta cofres para encontrar un stack de hierro.**

Buscador de items para tu zona de cofres: apretás una tecla, escribís lo que buscás y el mod te
muestra todo lo que tenés guardado alrededor, cuánto hay y en qué cofre está. Después te guía hasta
ahí.

[![Minecraft](https://img.shields.io/badge/Minecraft-26.3-brightgreen)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Loader-Fabric-lightgrey)](https://fabricmc.net/)
[![Licencia](https://img.shields.io/badge/Licencia-MIT-blue)](LICENSE.txt)

</div>

---

## Cómo se usa

1. **Apretá `Ctrl + F`.** Se abre el buscador y se escanean tus contenedores en ese momento.
2. **Escribí lo que buscás.** La grilla filtra mientras tipeás, por nombre o por id (`oak_log`).
3. **Hacé clic en el item.** Los cofres que lo tienen se resaltan a través de las paredes y un
   rastro de partículas se dibuja por el suelo hasta el más cercano.
4. **Abrí el cofre.** La guía se apaga sola.

No hay que craftear nada ni construir ninguna máquina. Se instala y funciona.

---

## Qué trae

### Una búsqueda que refleja tu depósito
- **Resultados del momento.** Cada búsqueda lee los cofres ahí mismo, así que no te miente después
  de que un amigo reordenó el depósito.
- **Grilla con totales**, para distinguir de un vistazo tres stacks de tres items sueltos.
- **Filtro por nombre o por id**, en el idioma en el que tengas el juego.
- **Botón de escanear** para refrescar sin cerrar la pantalla.
- Lee cofres, cofres trampa, cofres dobles, barriles y shulker boxes colocadas. Si querés, también
  hoppers, droppers, dispensers, hornos y cualquier cosa con inventario.
- **Mira adentro de las shulker boxes guardadas en tus cofres**, así aparece el pico que habías
  empaquetado.

### Guía hasta el cofre
- Una **caja alrededor de cada cofre** que tiene el item, visible a través de las paredes, con el
  más cercano en verde.
- Un **rastro de partículas apoyado en el suelo**, que avanza hacia ese cofre: sube escaleras y
  sigue los desniveles en vez de flotar en el aire.
- Una **columna de partículas sobre el cofre**, para ubicarlo apenas doblás la esquina.
- Todo se apaga al abrir el cofre, o después del tiempo que elijas.

### Tus cofres, no los del mundo
- Los cofres generados en **mazmorras, aldeas, minas y demás estructuras quedan afuera**, incluso
  después de saquearlos, para que los barriles de una aldea no te tapen tu propio depósito.
- **Zonas:** marcás tu depósito una vez y buscar adentro mira la zona entera, del tamaño que sea y
  estés parado donde estés. ¿Armaste la base adentro de una aldea? Dentro de una zona cuenta todo.
- Los cofres de loot sin abrir nunca se leen, así que no se genera su contenido antes de tiempo.

### Pensado para convivir en un modpack
- **Sin mixins.** No se parchea ninguna clase del juego: todo pasa por eventos de Fabric API, que es
  la principal fuente de peleas entre mods.
- **Sin trabajo en segundo plano.** El servidor solo escanea cuando abrís el buscador.
- **Sin bloques, items ni recetas.** No toca los registros ni la generación del mundo: si sacás el
  mod, tu mundo queda igual.
- Topes de tamaño en el escaneo y en los paquetes, para que un depósito enorme no golpee al servidor.

---

## Instalación

1. Instalá [Fabric Loader](https://fabricmc.net/use/) 0.19.5 o superior para Minecraft 26.3.
2. Poné [Fabric API](https://modrinth.com/mod/fabric-api) en la carpeta `mods`.
3. Poné ahí también el jar de AnyFind.
4. Opcional: [Mod Menu](https://modrinth.com/mod/modmenu), para configurarlo desde el menú.

Sirve en un jugador, en mundos LAN y en servidores, incluidos los que se hostean con Essential. El
escaneo corre del lado del servidor, así que **el mod tiene que estar donde corre el mundo**. Un
cliente sin el mod simplemente no tiene la función, y uno con el mod te avisa claramente si el
servidor no lo tiene.

---

## Comandos

| Comando | Qué hace |
|---|---|
| `/anyfind scan [radio] [incluirEstructuras]` | Escanea y muestra un resumen en el chat |
| `/anyfind zone create <nombre> [radio]` | Crea una zona alrededor tuyo |
| `/anyfind zone create <nombre> <pos1> <pos2>` | Zona entre dos esquinas |
| `/anyfind zone list` | Lista las zonas de la dimensión |
| `/anyfind zone here` | Te dice en qué zona estás parado |
| `/anyfind zone remove <nombre>` | Borra una zona |

Si buscás fuera de toda zona y aparecen ocho o más contenedores, el chat te ofrece guardarlos como
zona. Es solo una sugerencia: nunca se crea nada sin que vos lo pidas.

---

## Configuración

Todo se configura desde **Mod Menu**, o en `config/anyfind.json`:

| Opción | Para qué sirve |
|---|---|
| Radio del escaneo | De 16 a 128 bloques (adentro de una zona se escanea la zona entera) |
| Tecla combinada | Ctrl, Alt, Shift o ninguna; la tecla se reasigna en los controles del juego |
| Abrir desde inventarios | Si el atajo también anda con un cofre abierto |
| Ignorar estructuras | Deja afuera los cofres de mazmorras y aldeas |
| Otros contenedores | Suma hoppers, hornos y demás |
| Mirar dentro de shulkers | Cuenta los items dentro de shulkers guardadas |
| Resaltar cofres / camino / marca | Prendés y apagás cada ayuda visual |
| Duración del resaltado | De 15 segundos a 5 minutos |

---

## Cosas a saber

- Solo se escanean **chunks cargados**. El mod nunca fuerza la carga, así que un depósito en una
  parte del mundo que el servidor no tiene cargada no aparece.
- Los items se agrupan **por tipo**: todos los libros encantados cuentan juntos.
- El rastro marca la **dirección**, no una ruta calculada: atraviesa una pared en vez de rodearla.
- Los resultados son una foto del momento. Si algo cambia, apretá **Escanear** de nuevo.
- No está endurecido para servidores públicos: cualquier jugador puede ver el contenido de los
  cofres dentro del radio y las zonas son compartidas. Está pensado para partidas propias y mundos
  entre amigos.

---

## Compilar desde el código

```bash
./gradlew build
```

El jar queda en `build/libs/anyfind-<versión>.jar`. Para probar mientras desarrollás,
`./gradlew runClient` abre un Minecraft con el mod ya cargado.

Requiere JDK 25. Las notas de desarrollo, el plan del escaneo y el registro de cambios están en
[DEVELOPMENT.md](DEVELOPMENT.md).

---

## Estado

El mod está completo y compila, pero **todavía no se probó a fondo dentro de una partida**. Si
encontrás algo raro, abrí un issue: los reportes en esta etapa valen oro.

---

## Licencia

MIT — ver [LICENSE.txt](LICENSE.txt). Podés usarlo, modificarlo y redistribuirlo, incluso dentro de
un modpack, manteniendo el aviso de copyright.

*No afiliado a Mojang ni a Microsoft.*
