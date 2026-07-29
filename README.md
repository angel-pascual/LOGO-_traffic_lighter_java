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

Desarrollar una alternativa de telecontrol vial económica, transparente, escalable y libre de licencias comerciales costosas (como WinCC o InTouch), demostrando la convergencia directa entre el desarrollo de software HMI/GUI a medida (IT) y los sistemas de automatización de control operativo en campo (OT).

---

## 🧩 1. ARQUITECTURA Y PROGRAMACIÓN EN LADDER (PLC SIEMENS LOGO!)

El núcleo del control de campo reside en el programa cargado en la CPU del PLC Siemens LOGO!. La lógica en lenguaje KOP/Ladder fue diseñada bajo el principio de **autonomía determinista**: la CPU ejecuta de forma ininterrumpida la secuencia de luces y la gestión de tiempos, garantizando que el semáforo siga funcionando con seguridad operativa incluso si el enlace de comunicación con la interfaz gráfica se interrumpe.

### 1.1 Mapeo de Memoria Virtual (Tabla VM / DB1)

Para permitir la lectura y escritura remota desde la interfaz Java sin alterar el ciclo de escaneo del PLC, se estructuró un bloque de variables dentro de la **Memoria VM (Variable Memory)** del LOGO!, mapeada en el protocolo S7 bajo la librería Moka7 como el Bloque de Datos 1 (`DB1`):

| Dirección PLC | Variable SCADA | Tipo de Dato | Unidades PLC | Descripción Lógica en Ladder |
| :--- | :--- | :--- | :--- | :--- |
| **VW0** | `ModoComando` | Word (16 bits) | Entero (1 - 6) | Selección de modo mediante comparadores de magnitud. |
| **VW2** | `TiempoVerde` | Word (16 bits) | Centésimas de s ($t \times 100$) | Parámetro asignado al bloque temporizador de Verde (T003). |
| **VW4** | `TiempoAmbar` | Word (16 bits) | Centésimas de s ($t \times 100$) | Parámetro asignado al bloque temporizador de Ámbar (T004). |
| **VW6** | `TiempoRojo` | Word (16 bits) | Centésimas de s ($t \times 100$) | Parámetro asignado al bloque temporizador de Rojo (T005). |
| **VW8** | `TiempoBucle` | Word / DWord | Centésimas de s ($t \times 100$) | Parámetro asignado al Temporizador Maestro de Ciclo (T007). |

---

### 1.2 Lógica del Bucle Maestro y Secuenciamiento Cíclico

La secuencia del semáforo no utiliza retardos fijos, sino una **arquitectura de temporización dinámica desacoplada**:

* **Temporizador Maestro de Ciclo (T007):** Este bloque coordina la duración total de una vuelta completa del semáforo. Su consigna de tiempo se actualiza desde `VW8`, cuyo valor equivale a la suma estricta de $T_{verde} + T_{ambar} + T_{rojo}$. Al finalizar su conteo, el temporizador autorresetea la cadena, reiniciando el ciclo continuo.
* **Cascada de Fases (T003, T004, T005):** Cada fase (Verde, Ámbar y Rojo) está enlazada a un temporizador con retardo a la conexión/desconexión cuyos parámetros internos apuntan directamente a los registros `VW2`, `VW4` y `VW6`. 
* **Prevención de Solapamiento:** Se implementaron interbloqueos cruzados (contactos normalmente cerrados en serie con las bobinas de salida) para garantizar que jamás puedan encenderse simultáneamente dos luces de distintas fases en el mismo sentido de vía.

---

### 1.3 Decodificación de Modos de Operación y Prioridades

El estado de funcionamiento es determinado por el valor numérico contenido en la palabra `VW0`, el cual activa o inhibe ramas específicas del diagrama Ladder mediante **Comparadores Analógicos/Digitales de Valor**:

```text
 [ VW0 == 1 ] ----( )-- Habilita Secuencia Cíclica Automática (T003 -> T004 -> T005)
 [ VW0 == 2 ] ----( )-- Activa Modo Noche: Desconecta Verde/Rojo y conmuta Ámbar a Marca M8 (Parpadeo)
 [ VW0 == 3 ] ----( )-- Activa Onda Verde: Fuerza Salida Q1 (Verde) e inhabilita temporizadores de cambio
 [ VW0 == 4 ] ----( )-- Activa Ciclo Peatonal: Fuerza transición rápida a Rojo e inhabilita reinicio automático
 [ VW0 == 6 ] ----( )-- Paro Total de Emergencia: Desactiva todas las salidas y bloquea el sistema en Rojo
```
---
# 🚦 Interfaz de Control de Semáforo en Java (HMI para PLC)

Este documento detalla la arquitectura, el diseño y la lógica de programación detrás de la Interfaz Gráfica de Usuario (GUI) desarrollada en Java, la cual actúa como sistema central de control y monitoreo para nuestro proyecto de semáforo automatizado gestionado por un PLC.

---

## ⚙️ Arquitectura del Sistema

La aplicación fue desarrollada utilizando **Java Swing** para el diseño visual, pero su núcleo va mucho más allá de una simple pantalla. Funciona como el puente de comunicación bidireccional entre el operador y el hardware industrial (PLC).

El flujo del programa se divide en tres capas fundamentales:
1. **Capa de Presentación (UI):** Formularios interactivos (`JFrame`), botones y paneles que representan visualmente el estado del semáforo.
2. **Capa de Control (Eventos):** Manejadores de acciones (`ActionListeners`) que capturan los clics del usuario y determinan qué instrucción debe procesarse.
3. **Capa de Comunicación (Backend):** Procesos en segundo plano (Hilos) encargados de establecer la conexión de red (ej. sockets TCP/IP o Modbus TCP) para enviar comandos al PLC y recibir la retroalimentación de sus sensores/actuadores.

---

## 🖥️ Componentes y Lógica de la Interfaz

La interfaz está diseñada para ofrecer control intuitivo y monitoreo en tiempo real sin latencia perceptible.

*   **Panel de Monitoreo en Tiempo Real:** 
    Utilizamos componentes gráficos (como `JPanel` o `JLabel` con iconos) que actualizan su color (Rojo, Amarillo, Verde) dependiendo de las lecturas que la interfaz recibe del PLC. Esto garantiza que lo que se ve en la pantalla de Java es exactamente lo que está ocurriendo físicamente en el semáforo.
*   **Selector de Modo de Operación:** 
    Se implementaron botones para alternar entre el **Modo Automático** (donde el PLC ejecuta su rutina de tiempos pre-programada) y el **Modo Manual** (donde la interfaz de Java toma el control maestro para forzar los cambios de luces).
*   **Control de Emergencia (E-Stop):** 
    Un botón de interrupción prioritaria. Al presionarlo, Java envía un comando crítico al PLC para poner todo el sistema en parpadeo intermitente o luces rojas, bloqueando cualquier otra instrucción hasta que el sistema sea reiniciado.

---

## 🧠 Funcionamiento del Código y Concurrencia

Para garantizar que la interfaz no se congele ni se bloquee mientras espera respuestas del hardware, la programación se estructuró con técnicas de concurrencia:

### 1. Gestión de Eventos (Asincronía)
Las acciones del usuario se procesan de forma inmediata. Cuando el operador presiona un botón, se dispara un evento que traduce esa acción a un comando de máquina:

```java
btnModoManual.addActionListener(new ActionListener() {
    public void actionPerformed(ActionEvent e) {
        // Enviar instrucción de activación de bobina al PLC
        controladorPLC.enviarComando("SET_MODO_MANUAL");
        actualizarEstadoVisual("Modo Manual Activado");
    }
});
