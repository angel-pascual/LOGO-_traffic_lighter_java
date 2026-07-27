# SCADA-LOGO-TrafficControl: Sistema de Semaforización Inteligente con PLC Siemens LOGO! e Interfaz SCADA en Java

![Java](https://img.shields.io/badge/Language-Java%208%2B-red)
![PLC](https://img.shields.io/badge/PLC-Siemens%20LOGO!%200BA8-009999)
![Protocol](https://img.shields.io/badge/Protocol-S7%20over%20Ethernet-blue)
![Library](https://img.shields.io/badge/Library-Moka7%20%2F%20Snap7-orange)
![Status](https://img.shields.io/badge/Status-Tested%20on%20Hardware-success)

## 📖 Descripción General

Este proyecto implementa una solución integral de automatización industrial y telecontrol para la gestión inteligente del tráfico urbano. El sistema combina la robustez de un **Controlador Lógico Programable (PLC) Siemens LOGO!** a nivel de campo con una **interfaz SCADA (Supervisory Control and Data Acquisition)** personalizada, desarrollada completamente en **Java**.

A través de una conexión Ethernet industrial utilizando el protocolo nativo Siemens S7, el software en Java permite a un centro de control supervisar el estado de la vía, cambiar entre múltiples modos de operación (Automático, Noche, Onda Verde de Emergencia, Peatonal y Paro) y **reconfigurar los tiempos de temporización en tiempo real** sin necesidad de detener la ejecución de la CPU del PLC ni alterar el programa Ladder subyacente.

### 🎯 Objetivo del Proyecto

Desarrollar una alternativa de telecontrol vial económica, transparente, escalable y libre de licencias comerciales costosas (como WinCC o InTouch), demostrando la convergencia directa entre el desarrollo de software de alto nivel (IT) y los sistemas de automatización de control operativo en campo (OT) aplicados a la problemática del tráfico urbano.

---

## 🧩 ARQUITECTURA Y PROGRAMACIÓN EN LADDER (PLC SIEMENS LOGO!)

El núcleo del control de campo reside en el programa cargado en la CPU del PLC Siemens LOGO!. La lógica en lenguaje KOP/Ladder fue diseñada bajo el principio de **autonomía determinista**: la CPU ejecuta de forma ininterrumpida la secuencia de luces y la gestión de tiempos, garantizando que el semáforo siga funcionando con seguridad operativa incluso si el enlace de comunicación con el servidor SCADA se interrumpe.

### 1. Mapeo de Memoria Virtual (Tabla VM / DB1)

Para permitir la lectura y escritura remota desde Java sin alterar el ciclo de escaneo del PLC, se estructuró un bloque de variables dentro de la **Memoria VM (Variable Memory)** del LOGO!, mapeada en el protocolo S7 bajo la librería Moka7 como el Bloque de Datos 1 (`DB1`):

| Dirección PLC | Variable SCADA | Tipo de Dato | Unidades PLC | Descripción Lógica en Ladder |
| :--- | :--- | :--- | :--- | :--- |
| **VW0** | `ModoComando` | Word (16 bits) | Entero (1 - 6) | Selección de modo mediante comparadores de magnitud. |
| **VW2** | `TiempoVerde` | Word (16 bits) | Centésimas de s ($t \times 100$) | Parámetro asignado al bloque temporizador de Verde (T003). |
| **VW4** | `TiempoAmbar` | Word (16 bits) | Centésimas de s ($t \times 100$) | Parámetro asignado al bloque temporizador de Ámbar (T004). |
| **VW6** | `TiempoRojo` | Word (16 bits) | Centésimas de s ($t \times 100$) | Parámetro asignado al bloque temporizador de Rojo (T005). |
| **VW8** | `TiempoBucle` | Word / DWord | Centésimas de s ($t \times 100$) | Parámetro asignado al Temporizador Maestro de Ciclo (T007). |

---

### 2. Lógica del Bucle Maestro y Secuenciamiento Cíclico

La secuencia del semáforo no utiliza retardos fijos, sino una **arquitectura de temporización dinámica desacoplada**:

* **Temporizador Maestro de Ciclo (T007):** Este bloque coordina la duración total de una vuelta completa del semáforo. Su consigna de tiempo se actualiza desde `VW8`, cuyo valor equivale a la suma estricta de $T_{verde} + T_{ambar} + T_{rojo}$. Al finalizar su conteo, el temporizador autorresetea la cadena, reiniciando el ciclo continuo.
* **Cascada de Fases (T003, T004, T005):** Cada fase (Verde, Ámbar y Rojo) está enlazada a un temporizador con retardo a la conexión/desconexión cuyos parámetros internos apuntan directamente a los registros `VW2`, `VW4` y `VW6`. 
* **Prevención de Solapamiento:** Se implementaron interbloqueos cruzados (contactos normalmente cerrados en serie con las bobinas de salida) para garantizar que jamás puedan encenderse simultáneamente dos luces de distintas fases en el mismo sentido de vía.

---

### 3. Decodificación de Modos de Operación y Prioridades

El estado de funcionamiento es determinado por el valor numérico contenido en la palabra `VW0`, el cual activa o inhibe ramas específicas del diagrama Ladder mediante **Comparadores Analógicos/Digitales de Valor**:

```text
 [ VW0 == 1 ] ----( )-- Habilita Secuencia Cíclica Automática (T003 -> T004 -> T005)
 [ VW0 == 2 ] ----( )-- Activa Modo Noche: Desconecta Verde/Rojo y conmuta Ámbar a Marca M8 (Parpadeo)
 [ VW0 == 3 ] ----( )-- Activa Onda Verde: Fuerza Salida Q1 (Verde) e inhabilita temporizadores de cambio
 [ VW0 == 4 ] ----( )-- Activa Ciclo Peatonal: Fuerza transición rápida a Rojo e inhabilita reinicio automático
 [ VW0 == 6 ] ----( )-- Paro Total de Emergencia: Desactiva todas las salidas y bloquea el sistema en Rojo
