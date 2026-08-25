# Publicar Poketbot en Google Play

Guía de los pasos que quedan fuera del repositorio. El código ya cumple los
requisitos técnicos: `targetSdk 36`, salida AAB, R8 activado y build reproducible.

## 1. Crear la clave de firma

Hazlo **una sola vez** y guarda copia de seguridad: si pierdes el keystore no
podrás volver a actualizar la app nunca.

```bash
keytool -genkeypair -v \
  -keystore poketbot-release.jks \
  -keyalg RSA -keysize 4096 -validity 10000 \
  -alias poketbot
```

Crea `keystore.properties` en la raíz del proyecto (ya está en `.gitignore`):

```properties
storeFile=poketbot-release.jks
storePassword=TU_CLAVE
keyAlias=poketbot
keyPassword=TU_CLAVE
```

En CI puedes usar las variables de entorno equivalentes en lugar del fichero:
`POKETBOT_STORE_FILE`, `POKETBOT_STORE_PASSWORD`, `POKETBOT_KEY_ALIAS`,
`POKETBOT_KEY_PASSWORD`.

## 2. Generar el bundle

```bash
./gradlew clean bundleRelease
```

Resultado: `app/build/outputs/bundle/release/app-release.aab`.

Comprueba antes de subirlo:

```bash
./gradlew testDebugUnitTest lintDebug
```

## 3. Antes de subir: numeración de versiones

Cada subida necesita un `versionCode` mayor. Está en `app/build.gradle.kts`:

```kotlin
versionCode = 1        // súbelo en cada release
versionName = "1.0.0"  // lo que ve el usuario
```

## 4. Ficha de Play Console

**Requisitos gráficos** (los tienes que generar tú a partir del icono del juego):

| Recurso | Tamaño | Obligatorio |
|---|---|---|
| Icono de la app | 512 × 512 PNG (32 bits) | Sí |
| Gráfico de funciones | 1024 × 500 PNG/JPG | Sí |
| Capturas de teléfono | mínimo 2, entre 320 px y 3840 px de lado | Sí (recomendado 4–8) |
| Capturas de tablet 7"/10" | opcional | No |

**Textos sugeridos:**

- *Título* (30 car.): `Poketbot: Batallas de Robots`
- *Descripción breve* (80 car.):
  `Colecciona robots de bolsillo, mejóralos y gana la arena por turnos.`
- *Descripción completa*: usa la sección "Cómo se juega" del README como base.

**Clasificación de contenido:** el cuestionario dará PEGI 3 / Everyone. El juego
tiene violencia fantástica entre robots, sin sangre, sin texto de usuario, sin
compras ni anuncios. Contesta con sinceridad a esas cuatro preguntas.

**Seguridad de los datos (Data Safety):** declara **no se recoge ni comparte
ningún dato**. Es cierto: la app no pide permisos, no tiene red y solo guarda la
partida en `SharedPreferences` locales. El respaldo en la nube de Android
(`allowBackup`) es del sistema, no un envío tuyo.

**Anuncios:** marca "no contiene anuncios".

## 5. Política de privacidad

Play la exige aunque no recojas datos. Tienes una plantilla lista en
[`PRIVACY.md`](PRIVACY.md). Publícala en una URL pública — GitHub Pages sobre este
mismo repositorio sirve — y pega el enlace en la ficha.

## 6. Prueba cerrada obligatoria

Si tu cuenta de desarrollador es **personal y se creó a partir de noviembre de
2023**, Google exige antes de poder publicar en producción:

- **12 testers** como mínimo, apuntados a una prueba cerrada,
- que sigan inscritos **14 días seguidos**,
- y después solicitar el acceso a producción.

Plan: sube el AAB a un canal de **prueba cerrada**, crea una lista de correos con
tus 12 testers, y deja pasar las dos semanas. Si tu cuenta es de organización, este
paso no aplica y puedes ir directo a producción.

## 7. Orden recomendado

1. Genera el keystore y guárdalo en sitio seguro (más de una copia).
2. `./gradlew clean bundleRelease`.
3. Crea la app en Play Console y rellena la ficha, la clasificación y Data Safety.
4. Sube el AAB a prueba cerrada e invita a los 12 testers.
5. Pasados los 14 días, solicita producción y publica.
