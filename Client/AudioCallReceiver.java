package Client;

import javax.sound.sampled.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class AudioCallReceiver {

    private static volatile boolean recibiendo = true;

    public static void iniciarRecepcion(int puertoEscucha) {
        recibiendo = true;
        try {
            AudioFormat formato = new AudioFormat(44100.0f, 16, 1, true, false);
            SourceDataLine altavoz = AudioSystem.getSourceDataLine(formato);
            altavoz.open(formato);
            altavoz.start();

            DatagramSocket socket = new DatagramSocket(puertoEscucha);
            byte[] buffer = new byte[4096];

            System.out.println("Esperando audio entrante en el puerto " + puertoEscucha + "...");

            while (recibiendo) {
                DatagramPacket paquete = new DatagramPacket(buffer, buffer.length);
                socket.receive(paquete);
                altavoz.write(paquete.getData(), 0, paquete.getLength());
            }

            altavoz.stop();
            altavoz.close();
            socket.close();
            System.out.println("Recepción de audio finalizada.");

        } catch (Exception e) {
            System.err.println("Error al recibir audio: " + e.getMessage());
        }
    }

    public static void terminarRecepcion() {
        recibiendo = false;
    }
}
