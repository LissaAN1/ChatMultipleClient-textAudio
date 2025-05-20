package Server;
import java.io.*;
import java.net.Socket;
import java.util.*;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private String clientName;
    private BufferedReader in;
    private PrintWriter out;
    private static final Map<String, ClientHandler> users = Collections.synchronizedMap(new HashMap<>());
    private static final Map<String, Set<ClientHandler>> groups = Collections.synchronizedMap(new HashMap<>());

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            // Solicitar nombre
            out.println("Ingresa tu nombre:");
            clientName = in.readLine();
            users.put(clientName, this);
            out.println("¡Hola " + clientName + "!");

            // Menú principal
            String opcion;
            while (true) {
                out.println("\nMENU:\n1. Enviar mensaje a usuario\n2. Crear grupo\n3. Enviar mensaje a grupo\n4. Salir\nElige opcion:");
                opcion = in.readLine();

                if (opcion == null || opcion.equals("4")) break;

                switch (opcion) {
                    case "1":
                        enviarPrivado();
                        break;
                    case "2":
                        crearGrupo();
                        break;
                    case "3":
                        enviarAGrupo();
                        break;
                    default:
                        out.println("Opcion no válida.");
                        break;
                }

            }

        } catch (IOException e) {
            System.out.println("Error con el cliente " + clientName);
        } finally {
            try {
                users.remove(clientName);
                for (Set<ClientHandler> grupo : groups.values()) grupo.remove(this);
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void enviarPrivado() throws IOException {
        out.println("¿A que usuario deseas enviar el mensaje?");
        String destino = in.readLine();
        out.println("Escribe tu mensaje:");
        String mensaje = in.readLine();

        ClientHandler receptor = users.get(destino);
        if (receptor != null) {
            receptor.out.println("Mensaje privado de " + clientName + ": " + mensaje);
        } else {
            out.println("Usuario no encontrado.");
        }
    }

    private void crearGrupo() throws IOException {
        out.println("Nombre del grupo:");
        String nombreGrupo = in.readLine();

        // Crear el grupo si no existe
        groups.putIfAbsent(nombreGrupo, new HashSet<>());
        groups.get(nombreGrupo).add(this); // El creador se une automáticamente

        out.println("Grupo '" + nombreGrupo + "' creado.");
        out.println("Usuarios disponibles para agregar:");

        // Mostrar usuarios conectados (menos el propio)
        synchronized (users) {
            for (String nombre : users.keySet()) {
                if (!nombre.equals(this.clientName)) {
                    out.println(" - " + nombre);
                }
            }
        }

        out.println("Escribe los nombres de los usuarios a agregar, separados por comas:");
        String linea = in.readLine();
        if (linea == null || linea.trim().isEmpty()) return;

        String[] nombres = linea.split(",");

        for (String nombre : nombres) {
            String limpio = nombre.trim();
            if (!limpio.equals(clientName) && users.containsKey(limpio)) {
                groups.get(nombreGrupo).add(users.get(limpio));
                users.get(limpio).out.println("Has sido agregado al grupo '" + nombreGrupo + "' por " + clientName + ".");
            } else {
                out.println("No se pudo agregar a '" + limpio + "' (no existe o es tu propio nombre).");
            }
        }

        out.println("Miembros actuales del grupo '" + nombreGrupo + "':");
        for (ClientHandler miembro : groups.get(nombreGrupo)) {
            out.println(" - " + miembro.clientName);
        }
    }


    private void enviarAGrupo() throws IOException {
        out.println("Nombre del grupo:");
        String grupo = in.readLine();
        if (!groups.containsKey(grupo)) {
            out.println("Grupo no encontrado.");
            return;
        }

        out.println("Escribe tu mensaje para el grupo:");
        String mensaje = in.readLine();

        for (ClientHandler miembro : groups.get(grupo)) {
            if (!miembro.clientName.equals(this.clientName)) {
                miembro.out.println("[" + grupo + "] " + clientName + ": " + mensaje);
            }
        }
    }
}
