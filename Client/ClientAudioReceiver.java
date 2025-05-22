package Client;

import javax.sound.sampled.*;
import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class ClientAudioReceiver {
    private Socket socket;
    private DataInputStream dataIn;

    public ClientAudioReceiver(Socket socket) throws IOException {
        this.socket = socket;
        this.dataIn = new DataInputStream(socket.getInputStream());
    }

    public File recibirArchivoAudio(String nombreEmisor) throws IOException {
        String nombreArchivo = dataIn.readUTF();
        long tamArchivo = dataIn.readLong();

        File carpeta = new File("audios_recibidos");
        if (!carpeta.exists()) carpeta.mkdir();  // crear si no existe

        File archivo = new File(carpeta, "de_" + nombreEmisor + "_" + nombreArchivo);
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

        System.out.println("Has recibido una nota de voz de " + nombreEmisor + ": " + archivo.getName());
        System.out.println("Presiona ENTER para reproducirla...");

        // Esperar ENTER
        new Scanner(System.in).nextLine();
        reproducirAudio(archivo);

        return archivo;
    }

    public static void reproducirAudio(File archivo) {
        try (AudioInputStream audioStream = AudioSystem.getAudioInputStream(archivo)) {
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            clip.start();
            // Esperar a que termine
            Thread.sleep(clip.getMicrosecondLength() / 1000);
        } catch (Exception e) {
            System.out.println("Error al reproducir el audio: " + e.getMessage());
        }
    }
}
