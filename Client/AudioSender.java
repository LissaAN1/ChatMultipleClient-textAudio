package Client;

import javax.sound.sampled.*;
import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class AudioSender {

    public static void grabarYEnviarAudio(Socket socket, String destino, boolean esGrupo) {
        try {
            Scanner scanner = new Scanner(System.in);

            // Pedir destino si está vacío
            if (destino == null || destino.trim().isEmpty()) {
                System.out.println(esGrupo ? "Ingresa el nombre del grupo:" : "Ingresa el nombre del usuario:");
                destino = scanner.nextLine();
            }

            // Parámetros de grabación
            AudioFormat formato = new AudioFormat(16000, 16, 1, true, true);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, formato);
            if (!AudioSystem.isLineSupported(info)) {
                System.out.println("Micrófono no soportado");
                return;
            }

            TargetDataLine mic = (TargetDataLine) AudioSystem.getLine(info);
            mic.open(formato);
            mic.start();

            System.out.println("Grabando... Presiona ENTER para detener");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];

            // Hilo para detener grabación al presionar ENTER
            Thread stopper = new Thread(() -> {
                Scanner stopScanner = new Scanner(System.in);
                stopScanner.nextLine();
                mic.stop();
                mic.close();
            });
            stopper.start();

            while (mic.isOpen()) {
                int count = mic.read(buffer, 0, buffer.length);
                if (count > 0) {
                    out.write(buffer, 0, count);
                }
            }
            stopper.join();

            // Guardar archivo WAV temporal
            byte[] audioData = out.toByteArray();
            File archivoWav = new File("temp_audio.wav");
            try (ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
                 AudioInputStream ais = new AudioInputStream(bais, formato, audioData.length / formato.getFrameSize())) {
                AudioSystem.write(ais, AudioFileFormat.Type.WAVE, archivoWav);
            }

            // Enviar al servidor
            enviarArchivoAudio(socket, destino, esGrupo, archivoWav);

            // Borrar archivo temporal
            archivoWav.delete();

            System.out.println("Audio enviado correctamente.");


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void enviarArchivoAudio(Socket socket, String destino, boolean esGrupo, File archivo) throws IOException {
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());

        // Enviar comando (5 o 6)
        out.println(esGrupo ? "6" : "5");

        // Enviar destino
        out.println(destino);

        // Enviar nombre de archivo y tamaño
        dataOut.writeUTF(archivo.getName());
        dataOut.writeLong(archivo.length());

        // Enviar contenido del archivo
        try (FileInputStream fis = new FileInputStream(archivo)) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = fis.read(buffer)) > 0) {
                dataOut.write(buffer, 0, count);
            }
            dataOut.flush();
        }
    }

    public static void reproducirAudioDesdeBytes(byte[] audioBytes) {
        try (AudioInputStream ais = AudioSystem.getAudioInputStream(
                new java.io.ByteArrayInputStream(audioBytes))) {

            AudioFormat formato = ais.getFormat();
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, formato);
            SourceDataLine linea = (SourceDataLine) AudioSystem.getLine(info);

            linea.open(formato);
            linea.start();

            byte[] buffer = new byte[4096];
            int bytesLeidos;
            while ((bytesLeidos = ais.read(buffer, 0, buffer.length)) != -1) {
                linea.write(buffer, 0, bytesLeidos);
            }

            linea.drain();
            linea.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
