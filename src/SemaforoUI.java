import Moka7.S7Client;
import Moka7.S7;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SemaforoUI extends JFrame {

    // 1. Herramientas de comunicación
    S7Client plc = new S7Client();

    // 2. Componentes Visuales con nombres claros (Swing Moderno)
    private JPanel contentPane;
    private JPanel panelLateral;
    private JPanel panelCentral;
    private JTextArea consolaEstado;
    private JLabel lblEstadoConexion;

    // NUEVOS COMPONENTES: Cajas de texto para modificar tiempos
    private JTextField txtTiempoVerde;
    private JTextField txtTiempoAmbar;
    private JTextField txtTiempoRojo;

    // Colores del Dashboard de referencia (Minimalistas y Universitarios)
    private Color colorFondoLateral = new Color(42, 42, 42); // Gris muy oscuro
    private Color colorAguamarina = new Color(32, 219, 203);   // Turquesa brillante
    private Color colorFondoCentral = new Color(242, 242, 242); // Gris muy claro
    private Color colorTextoClaro = Color.WHITE;

    public SemaforoUI() {
        // Configuraciones básicas
        setTitle("Dashboard Semáforo - Módulo SCADA v1.1");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 580); // Ligeramente más grande para acomodar los nuevos controles
        setLocationRelativeTo(null); // Centrar en pantalla

        // Contenedor principal
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(0, 0, 0, 0));
        contentPane.setLayout(new BorderLayout(0, 0));
        setContentPane(contentPane);

        // --- PANEL LATERAL (Navegación y Conexión) ---
        panelLateral = new JPanel();
        panelLateral.setBackground(colorFondoLateral);
        panelLateral.setPreferredSize(new Dimension(220, 10)); // Ancho fijo
        contentPane.add(panelLateral, BorderLayout.WEST);
        panelLateral.setLayout(null); // Layout null para control total

        // Logo/Título de la aplicación
        JLabel lblTitulo = new JLabel("SCADA PLC");
        lblTitulo.setHorizontalAlignment(SwingConstants.CENTER);
        lblTitulo.setForeground(colorAguamarina);
        lblTitulo.setFont(new Font("Segoe UI Semibold", Font.BOLD, 18));
        lblTitulo.setBounds(20, 25, 180, 25);
        panelLateral.add(lblTitulo);

        // Indicador de Estado de Conexión (Minimalista)
        lblEstadoConexion = new JLabel(" Desconectado ");
        lblEstadoConexion.setOpaque(true);
        lblEstadoConexion.setBackground(new Color(220, 53, 69)); // Rojo suave
        lblEstadoConexion.setForeground(Color.WHITE);
        lblEstadoConexion.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblEstadoConexion.setHorizontalAlignment(SwingConstants.CENTER);
        lblEstadoConexion.setBounds(20, 70, 180, 22);
        lblEstadoConexion.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panelLateral.add(lblEstadoConexion);

        // Botón de Conexión (Aguamarina)
        JButton btnConectar = crearBotónLateral("ESTABLECER CONEXIÓN", 120);
        btnConectar.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                plc.SetConnectionType(S7.S7_BASIC);
                actualizarConsola("Intentando conectar al PLC...");

                plc.SetConnectionParams("192.168.1.3", 0x0100, 0x0200);
                int res = plc.Connect();

                if (res == 0) {
                    actualizarConsola("¡PLC CONECTADO EXITOSAMENTE!");
                    actualizarEstadoConexion(true);
                } else {
                    actualizarConsola("FALLO DE CONEXIÓN: " + plc.ErrorText(res));
                    actualizarEstadoConexion(false);
                }
            }
        });
        panelLateral.add(btnConectar);

        // Botones de Comandos
        JButton btnAuto = crearBotónLateral("1. Modo Automático", 180);
        btnAuto.addActionListener(e -> enviarComando(1, "Activando Modo Automático..."));
        panelLateral.add(btnAuto);

        JButton btnNoche = crearBotónLateral("2. Modo Noche", 230);
        btnNoche.addActionListener(e -> enviarComando(2, "Activando Modo Noche Intermitente..."));
        panelLateral.add(btnNoche);

        JButton btnOndaVerde = crearBotónLateral("3. Onda Verde", 280);
        btnOndaVerde.addActionListener(e -> enviarComando(3, "Activando Onda Verde de EMERGENCIA!"));
        panelLateral.add(btnOndaVerde);

        JButton btnPeaton = crearBotónLateral("4. Paso Peatonal", 330);
        btnPeaton.addActionListener(e -> enviarComando(4, "Comando de cruce peatonal enviado."));
        panelLateral.add(btnPeaton);

        JButton btnBloqueo = crearBotónLateral("5. PARO", 380);
        btnBloqueo.setBackground(new Color(150, 0, 0));
        btnBloqueo.addActionListener(e -> enviarComando(6, "DETENIENDO PROCESOS..."));
        panelLateral.add(btnBloqueo);

        // Botón Exit
        JButton btnExit = crearBotónLateral("Exit", 480);
        btnExit.addActionListener(e -> System.exit(0));
        panelLateral.add(btnExit);


        // --- PANEL CENTRAL (Consola de Estado + Configuración de Tiempos) ---
        panelCentral = new JPanel();
        panelCentral.setBackground(colorFondoCentral);
        panelCentral.setBorder(new EmptyBorder(20, 20, 20, 20));
        contentPane.add(panelCentral, BorderLayout.CENTER);
        panelCentral.setLayout(new BorderLayout(0, 15));

        // Título del área de monitoreo
        JLabel lblTituloCentral = new JLabel("Consola de Estado y Monitoreo SCADA");
        lblTituloCentral.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTituloCentral.setForeground(colorFondoLateral);
        panelCentral.add(lblTituloCentral, BorderLayout.NORTH);

        // La Consola de Texto
        consolaEstado = new JTextArea();
        consolaEstado.setEditable(false);
        consolaEstado.setBackground(Color.WHITE);
        consolaEstado.setForeground(colorFondoLateral);
        consolaEstado.setFont(new Font("Consolas", Font.PLAIN, 12));
        consolaEstado.setMargin(new Insets(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(consolaEstado);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        panelCentral.add(scrollPane, BorderLayout.CENTER);

        // --- NUEVO SUBPANEL: CONFIGURADOR DE TIEMPOS INDUSTRIAL ---
        JPanel panelTiempos = new JPanel();
        panelTiempos.setBackground(Color.WHITE);
        panelTiempos.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                "Configuración de Tiempos Remotos (Segundos)",
                0, 0, new Font("Segoe UI Semibold", Font.PLAIN, 12), colorFondoLateral));
        panelTiempos.setLayout(new FlowLayout(FlowLayout.LEFT, 15, 10));

        // Entradas de Texto (con valores por defecto consistentes)
        panelTiempos.add(new JLabel("T. Verde:"));
        txtTiempoVerde = new JTextField("5", 4);
        panelTiempos.add(txtTiempoVerde);

        panelTiempos.add(new JLabel("T. Ámbar:"));
        txtTiempoAmbar = new JTextField("2", 4);
        panelTiempos.add(txtTiempoAmbar);

        panelTiempos.add(new JLabel("T. Rojo:"));
        txtTiempoRojo = new JTextField("5", 4);
        panelTiempos.add(txtTiempoRojo);

        // Botón de actualización masiva
        JButton btnActualizarTiempos = new JButton("ACTUALIZAR TIEMPOS");
        btnActualizarTiempos.setFont(new Font("Segoe UI Semibold", Font.BOLD, 12));
        btnActualizarTiempos.setBackground(colorFondoLateral);
        btnActualizarTiempos.setForeground(colorTextoClaro);
        btnActualizarTiempos.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnActualizarTiempos.setFocusPainted(false);

        // Acción del botón para leer las cajas y mandar las variables al PLC
        btnActualizarTiempos.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    // 1. Capturamos los valores numéricos ingresados por el operador (en segundos)
                    int tVerde = Integer.parseInt(txtTiempoVerde.getText());
                    int tAmbar = Integer.parseInt(txtTiempoAmbar.getText());
                    int tRojo  = Integer.parseInt(txtTiempoRojo.getText());

                    // 2. EL TRUCO MAESTRO: Calculamos el tiempo total para el T007 (en segundos)
                    int tiempoTotal = tVerde + tAmbar + tRojo;

                    actualizarConsola("Enviando nuevos parámetros de temporización...");

                    // 3. Enviamos cada variable a su respectiva palabra de memoria (VW)
                    // Nota: enviarTiempo ahora convierte segundos a unidades PLC multiplicando por 100 (según tu ajuste)
                    enviarTiempo(2, tVerde, "Tiempo Verde (T003)");   // CORRECCIÓN AQUI: multiplicador x100 aplicado dentro de enviarTiempo
                    enviarTiempo(4, tAmbar, "Tiempo Ámbar (T004)");  // CORRECCIÓN AQUI: multiplicador x100 aplicado dentro de enviarTiempo
                    enviarTiempo(6, tRojo,  "Tiempo Rojo (T005)");   // CORRECCIÓN AQUI: multiplicador x100 aplicado dentro de enviarTiempo

                    // 4. Actualizamos el Bucle Maestro
                    enviarTiempo(8, tiempoTotal, "Tiempo Bucle Maestro (T007)"); // CORRECCIÓN AQUI: multiplicador x100 aplicado dentro de enviarTiempo

                } catch (NumberFormatException ex) {
                    actualizarConsola("ERROR: Por favor ingresa solo números enteros válidos.");
                }
            }
        });
        panelTiempos.add(btnActualizarTiempos);

        // Añadimos la barra de tiempos en la base del panel central
        panelCentral.add(panelTiempos, BorderLayout.SOUTH);

        actualizarConsola("SCADA Iniciado. Esperando conexión...");
    }

    // --- MÉTODOS AUXILIARES ---

    private JButton crearBotónLateral(String texto, int y) {
        JButton btn = new JButton(texto);
        btn.setBounds(20, y, 180, 35);
        btn.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 13));
        btn.setForeground(colorTextoClaro);
        btn.setBackground(colorAguamarina);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void actualizarConsola(String mensaje) {
        consolaEstado.append("\n> " + mensaje);
        consolaEstado.setCaretPosition(consolaEstado.getDocument().getLength());
    }

    private void actualizarEstadoConexion(boolean conectado) {
        if (conectado) {
            lblEstadoConexion.setText(" CONECTADO ");
            lblEstadoConexion.setBackground(new Color(40, 167, 69));
        } else {
            lblEstadoConexion.setText(" DESCONECTADO ");
            lblEstadoConexion.setBackground(new Color(220, 53, 69));
        }
    }

    public void enviarComando(int numero, String mensajeConsola) {
        actualizarConsola(mensajeConsola);
        if (!plc.Connected) {
            actualizarConsola("ERROR: Primero debes conectar el PLC.");
            return;
        }
        byte[] datos = new byte[2];
        S7.SetWordAt(datos, 0, numero);
        int resultadoEscritura = plc.WriteArea(S7.S7AreaDB, 1, 0, 2, datos); // VW0

        if (resultadoEscritura == 0) {
            actualizarConsola("Comando " + numero + " enviado a VW0 con éxito.");
        } else {
            actualizarConsola("ERROR DE RED: " + plc.ErrorText(resultadoEscritura));
        }
    }

    // --- NUEVO TRANSMISOR DE TIEMPOS EXCLUSIVO PARA PARAMETRIZACIÓN (versión ajustada x100) ---
    // CORRECCIÓN PRINCIPAL: convertir segundos a la unidad esperada por tu PLC multiplicando por 100 (en lugar de 1000)
    // CORRECCIÓN ADICIONAL: si el valor en unidades PLC cabe en 16 bits se escribe como WORD (2 bytes),
    //                      si no cabe se escribe como DWORD (4 bytes) para evitar overflow.
    public void enviarTiempo(int direccionByte, int valorSegundos, String nombreVariable) {
        if (!plc.Connected) {
            actualizarConsola("ERROR: Desconectado. No se pudo guardar: " + nombreVariable);
            return;
        }

        // === CORRECCIÓN AQUI ===
        // Antes: se multiplicaba por 1000 (ms). Según tu observación, ahora multiplicamos por 100.
        // Esto convierte segundos a la "unidad PLC" que estás usando (segundos * 100).
        final long MULTIPLICADOR = 100L; // <-- CORRECCIÓN: multiplicador cambiado de 1000 a 100
        long valorPLCUnits = (long) valorSegundos * MULTIPLICADOR;
        // =======================

        // Seguridad: límites y mensajes
        if (valorSegundos < 0) {
            actualizarConsola("-> ERROR: El tiempo no puede ser negativo: " + nombreVariable);
            return;
        }

        try {
            // Límite para WORD sin signo: 65535 (en unidades PLC)
            if (valorPLCUnits <= 0xFFFFL) {
                // Cabe en 16 bits -> escribimos como WORD (2 bytes)
                int valorWord = (int) valorPLCUnits;
                byte[] datosTiempo = new byte[2];
                // CORRECCIÓN: ahora se escribe el valor convertido (segundos * 100) como WORD
                S7.SetWordAt(datosTiempo, 0, valorWord);
                int resultado = plc.WriteArea(S7.S7AreaDB, 1, direccionByte, 2, datosTiempo);

                if (resultado == 0) {
                    actualizarConsola("-> " + nombreVariable + " actualizado en VW" + direccionByte + " a: " + valorWord + " (unidad PLC) [" + valorSegundos + " s].");
                } else {
                    actualizarConsola("-> ERROR al escribir " + nombreVariable + ": " + plc.ErrorText(resultado));
                }
            } else {
                // No cabe en 16 bits -> escribimos como DWORD (4 bytes)
                long valorDWord = valorPLCUnits;
                byte[] datosTiempo = new byte[4];
                // CORRECCIÓN: usamos SetDWordAt para colocar un DWORD (32 bits) con el valor en unidades PLC
                S7.SetDWordAt(datosTiempo, 0, (int) valorDWord);
                int resultado = plc.WriteArea(S7.S7AreaDB, 1, direccionByte, 4, datosTiempo);

                if (resultado == 0) {
                    actualizarConsola("-> " + nombreVariable + " actualizado en VW" + direccionByte + " (DWORD) a: " + valorDWord + " (unidad PLC) [" + valorSegundos + " s].");
                } else {
                    actualizarConsola("-> ERROR al escribir " + nombreVariable + " (DWORD): " + plc.ErrorText(resultado));
                }
            }
        } catch (Exception ex) {
            actualizarConsola("-> EXCEPCIÓN al preparar tiempo: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new SemaforoUI().setVisible(true);
        });
    }
}
