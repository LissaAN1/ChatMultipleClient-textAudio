package Client;

import javax.sound.sampled.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class AudioCallSender {
    private static volatile boolean enviando = true;

public static void iniciarLlamada(String ipDestino, int puertoDestino) {
        enviando = true;
        try {
            AudioFormat formato = new AudioFormat(44100.0f, 16, 1, true, false);
            TargetDataLine microfono = AudioSystem.getTargetDataLine(formato);
            microfono.open(formato);
            microfono.start();

            DatagramSocket socket = new DatagramSocket();
            byte[] buffer = new byte[4096];

            System.out.println("Enviando audio a " + ipDestino + ":" + puertoDestino + "...");

            while (enviando) {
                int bytesRead = microfono.read(buffer, 0, buffer.length);
                DatagramPacket paquete = new DatagramPacket(buffer, bytesRead, InetAddress.getByName(ipDestino), puertoDestino);
                socket.send(paquete);
            }

            microfono.stop();
            microfono.close();
            socket.close();
            System.out.println("Envío de audio finalizado.");

        } catch (Exception e) {
            System.err.println("Error al enviar audio: " + e.getMessage());
        }
    }

    public static void terminarLlamada() {
        enviando = false;
    }
}