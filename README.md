# Poketbot

Juego de combate por turnos para Android: colecciona robots de bolsillo, mejóralos
con chatarra y supera una campaña de 20 combates.

- **Kotlin + Jetpack Compose**, sin motor de juego ni dependencias pesadas.
- **Sprites procedurales** dibujados en `Canvas`: cero imágenes, cero licencias de terceros.
- **Efectos de sonido sintetizados** en tiempo real con `AudioTrack`: cero archivos de audio.
- **100 % sin conexión**: no pide permisos, no recoge datos, no lleva anuncios ni analítica.

## Cómo se juega

Eliges entre cuatro acciones cada turno:

| Acción | Efecto |
|---|---|
| **Atacar** | Daño basado en tu ataque contra la defensa rival. La velocidad da probabilidad de crítico. |
| **Defender** | Reduce a la mitad el daño recibido hasta tu siguiente acción. |
| **Especial** | Habilidad única de cada bot. Cuesta energía (ganas 1 punto por turno). |
| **Reparar** | Recupera un 28 % de tu blindaje. Tres usos por combate. |

El orden del turno lo decide la velocidad. Ganar da chatarra, que se gasta en el
Taller para subir Blindaje, Ataque, Defensa y Velocidad (10 niveles cada uno) o
para desbloquear los otros cinco bots.

### Los seis bots

| Bot | Perfil | Especial |
|---|---|---|
| Chippy | Equilibrado (gratis) | Sobrecarga — duplica el daño del siguiente ataque |
| Voltik | Rápido y frágil | Pulso EMP — daña y aturde un turno |
| Búnker | Tanque | Muro Escudo — duplica la defensa 3 turnos |
| Ripclaw | Cañón de cristal | Doble Golpe — dos impactos seguidos |
| Medix | Aguante | Drenaje — daña y se repara la mitad |
| Óxido | Pegada pesada | Bomba Chatarra — atraviesa media defensa |

## Compilar

Necesitas JDK 17 y el SDK de Android (compileSdk 36).

```bash
./gradlew testDebugUnitTest   # lógica de combate y progresión
./gradlew assembleDebug       # APK de pruebas
./gradlew bundleRelease       # AAB para Google Play
```

Sin `keystore.properties` la variante release se firma con la clave de depuración
para que el proyecto siempre compile. Para una build publicable, mira
[`PLAYSTORE.md`](PLAYSTORE.md).

## Estructura

```
app/src/main/java/com/eljeff13/poketbot/
├── game/        Lógica pura (motor de combate, campaña, progresión) — sin Android
├── data/        Guardado en SharedPreferences
├── audio/       Sintetizador de efectos
├── ui/          Pantallas Compose y dibujo de los bots
└── MainActivity.kt
app/src/test/    Tests JUnit del motor y la progresión
```

El paquete `game/` no depende de Android, así que toda la lógica se prueba en la JVM.

## Licencia

Código propiedad del autor. Todo el contenido audiovisual se genera en tiempo de
ejecución, así que no hay assets de terceros que atribuir.
