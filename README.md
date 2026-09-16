# AnyFind

Mod de Fabric para Minecraft **26.3** que funciona como buscador de items para tu zona de cofres.
Abrís una interfaz, escribís lo que buscás, ves cuántos hay disponibles y, al elegir un item,
el mod te guía hasta el o los cofres que lo tienen.

---

## Estado actual

| Área                     | Estado         |
|--------------------------|----------------|
| Proyecto base (Loom/Gradle) | ✅ Configurado (Gradle 9.7.0, Loom 1.18) |
| Diseño del escaneo       | ✅ Definido    |
| Fase 1: escaneo base     | 🧪 Implementada, falta probar en el juego |
| Fase 2: buscador (Ctrl+F) | 🧪 Implementada, falta probar en el juego |
| Fase 3: guía hasta el cofre | 🧪 Implementada, falta probar en el juego |
| Fase 4: zonas            | 🧪 Implementada, falta probar en el juego |
| Mod Menu + configuración | 🧪 Implementado, falta probar en el juego |
| Todo lo demás            | ⏳ Pendiente   |

---

## Cómo funciona (visión general)

```
[Tecla / comando] → Pantalla de búsqueda → Resultados (item + cantidad total)
                                               │
                                     elegir un item
                                               ▼
                         Resaltar los cofres + guía hacia el más cercano
```

---

## Compatibilidad con modpacks

Pensado para pesar lo menos posible cuando se combina con otros mods:

- **Sin mixins.** No se parchea ninguna clase del juego; todo pasa por eventos de Fabric API. Es la
  mayor fuente de incompatibilidades entre mods y acá no existe.
- **Sin trabajo en segundo plano.** El servidor solo escanea cuando vos abrís el buscador o corrés
  el comando. No hay tareas por tick ni caché que mantener.
- **El cliente no hace nada sin selección.** El tick del resaltado corta en la primera línea si no
  elegiste ningún item.
- **Sin bloques, items ni recetas propias.** No agrega nada al registro ni toca la generación del
  mundo, así que no cambia mundos existentes ni choca con mods de contenido.
- **Dependencias:** solo Fabric API. Mod Menu es opcional (`suggests`, `clientCompileOnly`).

## Alcance

Está pensado para **un jugador** y para **mundos compartidos con Essential Mod**. No está endurecido
para servidores públicos: cualquier jugador puede ver el contenido de los cofres que estén dentro del
radio, no hay límite de frecuencia de escaneo y las zonas son compartidas y las puede borrar
cualquiera. Entre jugadores de confianza eso no molesta; si algún día se apunta a un servidor
público, habría que agregar permisos, cooldown y configuración del lado del servidor.

## Plan del escaneo

### Limitación clave: qué puede ver cada lado

- **Servidor** (incluye el mundo singleplayer, que usa un servidor integrado): puede leer el
  contenido de cualquier contenedor cargado sin abrirlo.
- **Cliente solo**: en un servidor que **no** tiene el mod, el cliente **no conoce el contenido
  de un cofre hasta que el jugador lo abre**.

Por eso la arquitectura es **híbrida**:

1. **Modo servidor (principal):** el servidor escanea y le manda los resultados al cliente con
   paquetes de red. Funciona siempre en singleplayer y en servidores con el mod instalado.
2. **Modo solo cliente (secundario, a futuro):** si el servidor no tiene el mod, el cliente guarda
   en caché el contenido de cada cofre que abrís. Es menos preciso (los datos pueden quedar
   desactualizados), pero funciona en servidores vanilla.

### ¿Cuándo se escanea? Opciones evaluadas

| Opción | Ventajas | Desventajas |
|--------|----------|-------------|
| **A. Radio cercano automático** (constante, alrededor del jugador) | No requiere configurar nada | Consume recursos sin parar; incluye cofres que no son de la "zona" (dungeons, cofres de otros jugadores) |
| **B. Por comando** (`/anyfind scan`) | Simple, predecible y barato | Si te olvidás de re-escanear, los datos quedan viejos |
| **C. Detección de grupos de cofres** (detecta muchos cofres juntos) | Cómodo y "mágico" | Heurística difícil de afinar; posibles falsos positivos |

### Decisión propuesta: zonas definidas + escaneo al buscar

Combinar lo mejor de cada opción:

1. **Zonas explícitas.** El jugador define su zona de cofres:
   - `/anyfind zone create <nombre> <radio>`: zona centrada en la posición actual.
   - `/anyfind zone create <nombre> <pos1> <pos2>`: zona por esquinas (caja).
   - Las zonas se guardan por mundo (datos persistentes del servidor).
2. **Escaneo bajo demanda al abrir la búsqueda.** Cuando abrís la interfaz, el servidor recorre los
   contenedores de la zona en ese momento. Así los datos **siempre están frescos** y no hace falta
   mantener una caché sincronizada.
   - Es barato: no se recorren bloques uno por uno, sino la **lista de block entities** de los
     chunks que tocan la zona, filtrando los que son contenedores.
   - Solo se escanean chunks **cargados** (no se fuerza la carga de chunks).
3. **Comando manual** `/anyfind scan` para forzar un escaneo y ver un resumen en el chat (útil para
   probar y depurar).
4. **Detección de grupos (opcional, más adelante).** Si el mod detecta muchos cofres juntos cerca
   del jugador y no hay zona creada, muestra una *sugerencia* ("¿Crear zona aquí?") en lugar de
   crearla sola.
5. **Sin zona definida:** se usa un radio por defecto alrededor del jugador (configurable, ej. 32
   bloques) para que el mod sirva desde el primer momento.

### Detalles del escaneo

- **Contenedores soportados:** cofres, cofres trampa, barriles y shulker boxes colocadas. Más
  adelante: hoppers, dispensers, etc. (configurable).
- **Cofres tuyos vs. cofres generados:** se ignoran dos casos.
  1. Los que todavía tienen loot sin generar (nunca fueron abiertos): leerlos generaría el loot.
  2. Los que están dentro de una **estructura generada** (mazmorras, aldeas, minas, fortalezas...),
     usando `StructureManager.getStructureWithPieceAt`. Esto atrapa los cofres de mazmorra que ya
     abriste, que si no serían indistinguibles de uno tuyo.
  Es configurable ("Ignorar estructuras", activado por defecto). Contrapartida: un cofre que hayas
  puesto vos dentro de una aldea también se ignora. Las zonas (Fase 4) van a ser la solución fina.
- **Cofres dobles:** contar cada cofre doble una sola vez (unificar las dos mitades).
- **Shulker boxes dentro de cofres:** leer también su contenido (fase posterior), indicando
  "cofre X → shulker".
- **Resultado del escaneo:** índice `item → lista de (posición, cantidad)` más el total por item.
- **Límites:** máximo 2000 contenedores por escaneo (corta y avisa), y como mucho 24 posiciones por
  item viajan al cliente, las más cercanas, para no pasarse del tamaño máximo de un paquete (1 MiB).
  El total sí cuenta todos los contenedores encontrados.

---

## Guía hacia los cofres

- Resaltar con un **contorno visible a través de las paredes** los cofres que tienen el item.
- Indicador en el HUD con **dirección y distancia** hacia el cofre más cercano.
- El resaltado desaparece al abrir el cofre, al pasar un tiempo o al cancelar la búsqueda.

---

## Hoja de ruta

### Fase 1: Escaneo base (servidor)
- [x] Recorrer los contenedores en un radio alrededor del jugador
- [x] Construir el índice item → posiciones/cantidades
- [x] Unificar cofres dobles
- [x] Comando `/anyfind scan` que muestra un resumen en el chat
- [ ] Probar en el juego (cofres simples/dobles, barriles, shulkers, cofres de loot)

**Uso:** `/anyfind scan` (radio 32), `/anyfind scan <radio>` (1 a 128) o
`/anyfind scan <radio> <incluirEstructuras>` para incluir los cofres de estructuras. El radio forma un cubo
alrededor del jugador. Muestra los 10 items con más cantidad y el contenedor más cercano de cada uno.

**Notas de implementación:**
- `scan/ContainerScanner`: recorre las block entities de los chunks cargados en rango.
- `scan/ScanResult`: índice `Item → (posición → cantidad)` más el total.
- Por ahora los items se agrupan por tipo (`Item`), sin mirar componentes: por ejemplo, todos los
  libros encantados cuentan como el mismo item.
- Los contenedores con loot sin generar (cofres de dungeons, aldeas, etc.) se ignoran, porque leerlos
  generaría su loot.

### Fase 2: Búsqueda y red
- [x] Paquetes cliente ↔ servidor (pedir escaneo / recibir resultados)
- [x] Atajo **Ctrl + F** para abrir el buscador (en el mundo y desde inventarios/cofres)
- [x] Pantalla con campo de texto, filtro y grilla de items con cantidades
- [ ] Probar en el juego

**Uso:** apretá **Ctrl + F**. Se escanea un radio de 32 bloques (o tu zona) y aparece una grilla con
todos los items. Escribí para filtrar (busca por nombre traducido y por id, ej. `oak_log`), usá la
rueda del mouse para desplazarte y hacé clic en un item (o Enter para el primero) para seleccionarlo.
El botón **Escanear**, al lado del campo de búsqueda, vuelve a escanear sin cerrar la pantalla.
Al seleccionar, se resaltan los cofres y se dibuja el camino hasta el más cercano (Fase 3).

**Atajo configurable:**
- La tecla se cambia en Opciones → Controles → AnyFind, o con el botón "Cambiar tecla" de la
  pantalla de opciones del mod (que lleva directo ahí).
- La tecla combinada se elige en las opciones del mod: **Ctrl** (por defecto), Alt, Shift o Ninguna.
- F también es "cambiar a la otra mano" en vanilla: cuando el atajo se dispara, se cancela ese
  cambio para que solo se abra el buscador. En el menú de controles F puede aparecer como conflicto.

**Notas de implementación:**
- Si se abre desde un cofre, el cofre se cierra primero.
- `network/`: `RequestScanPayload` (C→S) y `ScanResultsPayload` (S→C).
- `client/SearchKeyHandler`: atajo. `client/screen/ItemSearchScreen`: pantalla.
  `client/SearchSelection`: item seleccionado (lo va a usar la Fase 3).
- Textos traducibles en `assets/anyfind/lang` (es_es, en_us).

### Fase 3: Guía
- [x] Resaltado de cofres a través de paredes (caja sobre cada cofre)
- [x] Camino de partículas trazado **en el suelo**, desde el jugador hasta el cofre más cercano
- [x] Marca "física" sobre el cofre (columna de partículas)
- [x] Se apaga al **abrir** el cofre resaltado, o al vencer el tiempo
- [ ] Probar en el juego
- [ ] Indicador en el HUD con dirección y distancia (cuando el cofre está fuera de vista)

**Cómo se dibuja:** Minecraft 26.x trae el sistema de *gizmos* (`net.minecraft.gizmos.Gizmos`), que
dibuja cajas, líneas y flechas en el mundo. Se juntan durante el tick del cliente abriendo una
colección con `Minecraft.collectPerTickGizmos()` y el `LevelRenderer` las dibuja en el pase normal
de render, sin depender del modo depuración. `setAlwaysOnTop()` las hace visibles a través de las
paredes.

**Detalles:**
- Se resaltan hasta 12 cofres a la vez, los más cercanos, hasta 160 bloques.
- El más cercano se pinta en verde y el resto en amarillo.
- El camino son partículas `END_ROD` separadas 1,25 bloques, cuyo punto de partida avanza cada tick
  para que se vean fluyendo hacia el cofre. Antes era una flecha de gizmo, pero se veía a debug.
- Cada punto se apoya en el piso: se sigue la línea recta horizontal y, en cada paso, se busca la
  superficie más cercana (hasta 3 bloques para arriba y 6 para abajo) y se dibuja justo encima. Así
  el rastro sube escaleras y baja pozos, aunque **no esquiva paredes**: marca la dirección, no una
  ruta calculada.
- Los gizmos quedaron solo para la caja del cofre.
- Un cofre doble se resalta en una sola de sus mitades.
- `client/highlight/OpenedContainerWatcher` apaga el resaltado cuando abrís uno de los contenedores
  resaltados: guarda el bloque que clickeaste (`UseBlockCallback`) y, al abrirse una pantalla de
  contenedor, lo compara con las posiciones resaltadas (contando las dos mitades de un cofre doble).
  Acercarse ya no alcanza para apagarlo: hay que abrirlo.

### Fase 4: Zonas
- [x] Comandos para crear, listar y borrar zonas
- [x] Guardado persistente por dimensión
- [x] El escaneo usa la zona en la que está el jugador
- [ ] Probar en el juego
- [ ] Mostrar los límites de la zona en el mundo (opcional)

**Comandos:**

| Comando | Qué hace |
|---------|----------|
| `/anyfind zone create <nombre>` | Zona cúbica de radio 24 alrededor tuyo |
| `/anyfind zone create <nombre> <radio>` | Igual, con el radio que le pongas |
| `/anyfind zone create <nombre> <pos1> <pos2>` | Zona entre dos esquinas (acepta `~ ~ ~`) |
| `/anyfind zone list` | Lista las zonas de la dimensión |
| `/anyfind zone here` | Dice en qué zona estás parado |
| `/anyfind zone remove <nombre>` | Borra una zona |

**Cómo se usan:**
- Si estás **parado dentro de una zona**, tanto el buscador (Ctrl + F) como `/anyfind scan` escanean
  toda esa zona en vez del radio. Si hay zonas superpuestas, gana la más chica.
- Dentro de una zona **no** se aplica el filtro de estructuras: vos dijiste qué mirar. Así funciona
  si construiste tu base adentro de una aldea.
- Si no estás en ninguna zona, sigue valiendo el radio de la configuración.
- La pantalla muestra si usó una zona ("zona bodega") o el radio.
- Límite: 256 bloques por lado.
- Las zonas se guardan **por dimensión** y son compartidas: en un servidor, cualquier jugador puede
  crear, ver y borrar zonas. Si hace falta, se puede limitar por permisos más adelante.

**Implementación:** `zone/Zone` (nombre + caja), `zone/ZoneStorage` (`SavedData` guardado con el
mundo) y `scan/ScanRequest`, que decide si se escanea la zona o el cubo del radio.

### Compatibilidad con otros mods
- [x] **Mod Menu**: ícono, descripción y botón de opciones propio
- [x] Configuración en `config/anyfind.json` (radio, tecla combinada, abrir desde inventarios,
      estructuras, resaltado, camino, marca y duración)
- [x] **Essential Mod**: no hace falta código extra (ver abajo)
- [ ] Probar ambos en el juego

Mod Menu es **opcional**: se declara en `suggests`, se compila con `clientCompileOnly` y la clase
`client/integration/AnyfindModMenuIntegration` solo la carga Mod Menu si está instalado.

Sobre **Essential Mod** (essential.gg): no comparten nada conflictivo con AnyFind.
- Essential no usa Ctrl + F, y de todas formas la tecla es configurable.
- Solo interceptamos teclas en pantallas de contenedores (`AbstractContainerScreen`), así que las
  pantallas de Essential no se tocan.
- En mundos hosteados con Essential, el buscador funciona si el que hostea también tiene AnyFind;
  si no, la pantalla avisa en vez de quedarse cargando.

### Fase 5: Extras
- [ ] Contenido de shulker boxes dentro de cofres
- [ ] Sugerencia automática al detectar un grupo de cofres
- [ ] Más opciones de configuración (contenedores soportados)
- [ ] Modo solo cliente (caché al abrir cofres) para servidores sin el mod

---

## Registro de cambios

- **2026-09-15:** Proyecto creado. Arreglado el error de Gradle (Loom 1.18.1 requiere Gradle 9.7.0).
  README y plan inicial del escaneo.
- **2026-09-15:** Fase 1: escáner de contenedores (cofres, cofres trampa, barriles, shulker boxes)
  y comando `/anyfind scan [radio]`.
- **2026-09-15:** Fase 2: buscador integrado con atajo Ctrl + F, paquetes de red y traducciones.
- **2026-09-15:** Límites de seguridad: tope de 2000 contenedores por escaneo, 24 posiciones por item
  en el paquete, se saltea el chequeo de estructuras en chunks sin estructuras y el camino de
  partículas se dibuja cada 2 ticks.
- **2026-09-15:** Atajo configurable: tecla combinada a elección (Ctrl/Alt/Shift/Ninguna) y acceso
  directo a los controles del juego desde las opciones del mod.
- **2026-09-15:** El resaltado y el camino se apagan al abrir el cofre resaltado, no al acercarse.
- **2026-09-15:** El camino de partículas ahora se apoya en el suelo y la pantalla de búsqueda tiene
  botón "Escanear".
- **2026-09-15:** Fase 4: zonas con `/anyfind zone create|list|here|remove`, guardadas por dimensión,
  y el escaneo las usa cuando estás parado adentro.
- **2026-09-15:** El escaneo ignora los cofres dentro de estructuras generadas (opción
  "Ignorar estructuras"), para separar los cofres del jugador de los de mazmorras y aldeas.
- **2026-09-15:** Fase 3: resaltado de cofres con gizmos, camino de partículas, marca sobre el cofre
  y opciones nuevas (resaltado, camino, marca, duración).
- **2026-09-15:** Integración con Mod Menu (ícono, metadata y pantalla de opciones), configuración
  en `config/anyfind.json` y `.gitignore` ampliado.

---

## Desarrollo

- Java 25
- Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.160.5+26.3
- Ejecutar el cliente de pruebas: tarea Gradle `runClient`
