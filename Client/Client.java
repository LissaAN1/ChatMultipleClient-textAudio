package Client;

import javax.sound.sampled.*;
import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 6789;

    public static void reproducirAudio(File archivoAudio) {
        try (AudioInputStream audioStream = AudioSystem.getAudioInputStream(archivoAudio)) {
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            clip.start();

            // Esperar a que termine la reproducción
            Thread.sleep(clip.getMicrosecondLength() / 1000);
        } catch (Exception e) {
            System.out.println("Error al reproducir audio: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        try (
            Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            Scanner scanner = new Scanner(System.in);
        ) {
            // Hilo para recibir mensajes y audios
           
           Thread recibir = new Thread(() -> {
                try {
                    String linea;
                    while (true) {
                        linea = in.readLine();
                        if (linea == null) break;

                        if (linea.equals("AUDIO")) {
                            String nombreArchivo = in.readLine();
                            int tamaño = Integer.parseInt(in.readLine());

                            byte[] audioBytes = new byte[tamaño];
                            int bytesLeidos = 0;
                            while (bytesLeidos < tamaño) {
                                int leidos = socket.getInputStream().read(audioBytes, bytesLeidos, tamaño - bytesLeidos);
                                if (leidos == -1) break;
                                bytesLeidos += leidos;
                            }

                            // Guardar bytes en archivo temporal
                            File archivoTemp = new File("tempAudio_" + nombreArchivo);
                            try (FileOutputStream fos = new FileOutputStream(archivoTemp)) {
                                fos.write(audioBytes);
                            } catch (IOException e) {
                                System.out.println("Error al guardar archivo temporal: " + e.getMessage());
                                continue; // seguir escuchando en caso de error
                            }

                            // Reproducir audio
                            reproducirAudio(archivoTemp);

                            // Borrar archivo temporal
                            if (!archivoTemp.delete()) {
                                System.out.println("No se pudo borrar el archivo temporal.");
                            }
                        }

                    }
                } catch (Exception e) {
                    System.out.println("Conexión cerrada por el servidor.");
                }
            });
            recibir.start(); 

            // Hilo principal para enviar mensajes y audios
            while (true) {
                String linea = scanner.nextLine();

                // Audio privado
                if (linea.equals("5")) {
                    AudioSender.grabarYEnviarAudio(socket, "", false); 
                    continue;
                }

                // Audio grupo
                if (linea.equals("6")) {
                    AudioSender.grabarYEnviarAudio(socket, "", true); 
                    continue;
                }

                out.println(linea);

                if (linea.equals("4")) break;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
}
