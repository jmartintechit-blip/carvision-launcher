# Radios del cliente (datos para la fase 4, CAN)

Recibido el 2026-11-12. Pendiente de usar en la etapa 4 de `docs/HOJA_DE_RUTA.md`.

## Mazda

- Marca y modelo: genérica china, sin marca. En Ajustes sale «Car Navigation», modelo **CMZX65-U1**.
- Chip: **MediaTek MT6580** (plataforma `k80-bsp`), 4 núcleos Cortex-A53 a 1,5 GHz; 4 GB de RAM y 32 GB.
- Android: en «Acerca de» pone **Android 16** (sin comprobar: estas radios suelen mostrar una versión falsa).
- Caja CAN y su app: sin datos.

## Ford Focus

- Marca / firmware: **NWD** (fabricante chino genérico).
- Modelo: **K2401** (build `K2401_NWD_S212802`, mayo de 2025).
- Chip: **Allwinner A133**.
- Android: **10 Go** (aunque dice 14); **1 GB de RAM**; 32 GB.
- Bluetooth: módulo XR829 (app `com.bt.bc03`).
- App de la caja CAN: **CanAllInOne** (`com.nwd.can.setting`), de NWD.
- Marca de la caja CAN: sin confirmar. Opciones: la pegatina de la caja, o Ajustes de fábrica (código abajo) › tipo de
  CAN, donde sale el protocolo (Raise, Hiworld, Simple Soft…).
- Códigos de la radio: desarrollador `345543`; fábrica `1617` / `16176699`; Style UI `111333`.

## Lo que implica

- **1 GB de RAM y Android 10 Go** en la radio del Focus: la app y el 3D tienen que ir bien con muy poca memoria;
  hay que probarlo ahí antes de lanzar.
- Dos chips muy comunes en radios chinas (MediaTek y Allwinner) y dos fabricantes distintos: buena muestra para
  la detección automática del tipo de radio.
