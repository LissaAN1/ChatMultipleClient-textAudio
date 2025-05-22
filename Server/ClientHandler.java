package Server;

import java.io.*;
import java.net.Socket;
import java.util.*;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private String clientName;
    private BufferedReader in;
    private PrintWriter out;

    // Usuarios conectados: nombre -> ClientHandler
    private static final Map<String, ClientHandler> users = Collections.synchronizedMap(new HashMap<>());

    // Grupos: nombre grupo -> conjunto de ClientHandler miembros
    private static final Map<String, Set<ClientHandler>> groups = Collections.synchronizedMap(new HashMap<>());

    private DataInputStream dataIn;
    private DataOutputStream dataOut;

    public ClientHandler(Socket socket) throws IOException {
        this.clientSocket = socket;
        this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        this.out = new PrintWriter(clientSocket.getOutputStream(), true);
        this.dataIn = new DataInputStream(clientSocket.getInputStream());
        this.dataOut = new DataOutputStream(clientSocket.getOutputStream());
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            dataIn = new DataInputStream(clientSocket.getInputStream());
            dataOut = new DataOutputStream(clientSocket.getOutputStream());

            out.println("Ingresa tu nombre:");
            clientName = in.readLine();

            synchronized (users) {
                if (users.containsKey(clientName)) {
                    out.println("Nombre ya en uso. Conexión terminada.");
                    clientSocket.close();
                    return;
                }
                users.put(clientName, this);
            }

            out.println("¡Hola " + clientName + "!");

            // Menú principal
            String opcion;
            while (true) {
                out.println("\nMENU:\n1. Enviar mensaje a usuario\n2. Crear grupo\n3. Enviar mensaje a grupo\n4. Salir\n5. Nota de voz privada\n6. Nota de voz a grupo\nElige opcion:");
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
                    case "5":
                        recibirYReenviarNotaVoz(false);
                        break;
                    case "6":
                        recibirYReenviarNotaVoz(true);
                        break;
                    default:
                        out.println("Opción no válida.");
                        break;
                }
            }

        } catch (IOException e) {
            System.out.println("Error con el cliente " + clientName);
        } finally {
            try {
                // si se desconecta el cliente,  eliminar usuario y eliminarlo de grupos
                synchronized (users) {
                    users.remove(clientName);
                }
                synchronized (groups) {
                    for (Set<ClientHandler> grupo : groups.values()) {
                        grupo.remove(this);
                    }
                }
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void enviarPrivado() throws IOException {
        List<String> disponibles = new ArrayList<>();
        synchronized (users) {
            for (String nombre : users.keySet()) {
                if (!nombre.equals(this.clientName)) {
                    disponibles.add(nombre);
                }
            }
        }

        if (disponibles.isEmpty()) {
            out.println("No hay otros usuarios conectados en este momento.");
            return;
        }

        out.println("Usuarios disponibles:");
        for (String nombre : disponibles) {
            out.println(" - " + nombre);
        }

        out.println("¿A qué usuario deseas enviar el mensaje?");
        String destino = in.readLine();
        out.println("Escribe tu mensaje:");
        String mensaje = in.readLine();

        ClientHandler receptor;
        synchronized (users) {
            receptor = users.get(destino);
        }
        if (receptor != null && !destino.equals(clientName)) {
            receptor.out.println("Mensaje privado de " + clientName + ": " + mensaje);
        } else {
            out.println("Usuario no encontrado o inválido.");
        }
    }

    private void crearGrupo() throws IOException {
        out.println("Nombre del grupo:");
        String nombreGrupo = in.readLine();

        synchronized (groups) {
            groups.putIfAbsent(nombreGrupo, Collections.synchronizedSet(new HashSet<>()));
            groups.get(nombreGrupo).add(this);
        }

        out.println("Grupo '" + nombreGrupo + "' creado.");
        out.println("Usuarios disponibles para agregar:");

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

        synchronized (groups) {
            for (String nombre : nombres) {
                String limpio = nombre.trim();
                if (!limpio.equals(clientName)) {
                    ClientHandler ch;
                    synchronized (users) {
                        ch = users.get(limpio);
                    }
                    if (ch != null) {
                        groups.get(nombreGrupo).add(ch);
                        ch.out.println("Has sido agregado al grupo '" + nombreGrupo + "' por " + clientName + ".");
                    } else {
                        out.println("No se pudo agregar a '" + limpio + "' (no existe).");
                    }
                } else {
                    out.println("No se pudo agregar a '" + limpio + "' (es tu propio nombre).");
                }
            }
        }

        out.println("Miembros actuales del grupo '" + nombreGrupo + "':");
        synchronized (groups) {
            for (ClientHandler miembro : groups.get(nombreGrupo)) {
                out.println(" - " + miembro.clientName);
            }
        }
    }

    private void enviarAGrupo() throws IOException {
        if (groups.isEmpty()) {
            out.println("No hay grupos creados aún.");
            return;
        }

        out.println("Grupos disponibles:");
        synchronized (groups) {
            for (String nombreGrupo : groups.keySet()) {
                out.println(" - " + nombreGrupo);
            }
        }

        out.println("Nombre del grupo al que deseas enviar mensaje:");
        String grupo = in.readLine();

        synchronized (groups) {
            if (!groups.containsKey(grupo)) {
                out.println("Grupo no encontrado.");
                return;
            }
        }

        out.println("Escribe tu mensaje para el grupo:");
        String mensaje = in.readLine();

        synchronized (groups) {
            for (ClientHandler miembro : groups.get(grupo)) {
                if (!miembro.clientName.equals(this.clientName)) {
                    miembro.out.println("[" + grupo + "] " + clientName + ": " + mensaje);
                }
            }
        }
    }

    public File recibirArchivoAudio() throws IOException {
        String nombreArchivo = dataIn.readUTF();
        long tamArchivo = dataIn.readLong();

        File archivo = new File("server_received_" + nombreArchivo);
        try (FileOutputStream fos = new FileOutputStream(archivo)) {
            byte[] buffer = new byte[4096];
            long bytesLeidos = 0;
            while (bytesLeidos < tamArchivo) {
                int bytesPorLeer = (int) Math.min(buffer.length, tamArchivo - bytesLeidos);
                int read = dataIn.read(buffer, 0, bytesPorLeer);
                if (read == -1) throw new EOFException("Fin inesperado del stream");
                fos.write(buffer, 0, read);
                bytesLeidos += read;
            }
            fos.flush();
        }
        System.out.println("Archivo recibido: " + archivo.getName() + " (" + tamArchivo + " bytes)");
        return archivo;
    }

    public void reenviarArchivoAudio(Socket destinoSocket, File archivo) throws IOException {
        DataOutputStream dos = new DataOutputStream(destinoSocket.getOutputStream());
        dos.writeUTF(archivo.getName());
        dos.writeLong(archivo.length());

        try (FileInputStream fis = new FileInputStream(archivo)) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = fis.read(buffer)) > 0) {
                dos.write(buffer, 0, count);
            }
            dos.flush();
        }
        System.out.println("Archivo reenviado a: " + destinoSocket.getInetAddress());
    }

    public void recibirYReenviarNotaVoz(boolean esGrupo) {
        try {
            String destino = in.readLine(); // nombre usuario o grupo

            File archivo = recibirArchivoAudio();

            if (esGrupo) {
                Set<ClientHandler> miembros = groups.get(destino);
                if (miembros == null) {
                    out.println("Grupo no existe.");
                    return;
                }

                for (ClientHandler miembro : miembros) {
                    if (!miembro.clientName.equals(this.clientName)) {
                        reenviarArchivoAudio(miembro.clientSocket, archivo);
                    }
                }
                out.println("Audio reenviado a grupo " + destino);

            } else {
                ClientHandler receptor = users.get(destino);
                if (receptor != null) {
                    reenviarArchivoAudio(receptor.clientSocket, archivo);
                    out.println("Audio reenviado a usuario " + destino);
                } else {
                    out.println("Usuario destino no encontrado.");
                }
            }

            archivo.delete();

        } catch (IOException e) {
            e.printStackTrace();
            out.println("Error al recibir o reenviar audio.");
        }
    }
}
