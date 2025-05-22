package Client;

import javax.sound.sampled.*;
import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 6789;

    public static void main(String[] args) {
        try (
            Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            DataInputStream dataIn = new DataInputStream(socket.getInputStream());
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            Scanner scanner = new Scanner(System.in);
        ) {
            // Hilo para recibir tanto mensajes de texto como audios
            Thread recibir = new Thread(() -> {
                try {
                    escucharMensajes(in, dataIn);
                } catch (IOException e) {
                    System.out.println("Error recibiendo mensajes del servidor.");
                    e.printStackTrace();
                }
            });
            recibir.start();

            // Hilo principal para enviar mensajes y notas de voz
            while (true) {
                String linea = scanner.nextLine();

                if (linea.equals("5")) { 
                    AudioSender.grabarYEnviarAudio(socket, "", false);
                    continue;
                }
                if (linea.equals("6")) { 
                    AudioSender.grabarYEnviarAudio(socket, "", true);
                    continue;
                }

                out.println(linea);

                if (linea.equals("4")) {
                    break;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


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

    public static void escucharMensajes(BufferedReader in, DataInputStream dataIn) throws IOException {
        while (true) {
            String tipo = dataIn.readUTF();

            if ("AUDIO".equals(tipo)) {
                String nombreEmisor = dataIn.readUTF();

                String nombreArchivo = dataIn.readUTF();
                long tam = dataIn.readLong();

                File carpeta = new File("audios_recibidos");
                if (!carpeta.exists()) carpeta.mkdir();

                File archivo = new File(carpeta, "de_" + nombreEmisor + "_" + nombreArchivo);
                try (FileOutputStream fos = new FileOutputStream(archivo)) {
                    byte[] buffer = new byte[4096];
                    long leidos = 0;
                    while (leidos < tam) {
                        int porLeer = (int) Math.min(buffer.length, tam - leidos);
                        int r = dataIn.read(buffer, 0, porLeer);
                        if (r == -1) throw new EOFException("Fin inesperado del stream");
                        fos.write(buffer, 0, r);
                        leidos += r;
                    }
                }

                System.out.println("Has recibido una nota de voz de " + nombreEmisor + ": " + archivo.getName());
                System.out.println("Presiona ENTER para reproducirla...");
                new Scanner(System.in).nextLine();

                reproducirAudio(archivo);

            } else {
                System.out.println(tipo);
            }
        }
    }

}