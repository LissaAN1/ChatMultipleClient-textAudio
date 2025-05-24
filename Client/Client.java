package Client;

import javax.sound.sampled.*;
import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 6789;
    public static volatile boolean llamadaActiva = false;

    private static volatile boolean esperandoEnter = false;
    private static final Object enterLock = new Object();

    public static void main(String[] args) {
        try (
            Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            DataInputStream dataIn = new DataInputStream(socket.getInputStream());
            DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());
            Scanner scanner = new Scanner(System.in);
        ) {
            new File("audios_recibidos").mkdirs();

            Thread recibir = new Thread(() -> {
                try {
                    String linea;
                    while ((linea = in.readLine()) != null) {
                        if (linea.equals("AUDIO_INCOMING")) {
                            recibirYReproducirAudio(dataIn);
                        } else if (linea.equals("IMAGEN_INCOMING")) {
                            ImageReceiver.recibirImagen(dataIn);
                        } else if (linea.startsWith("IP_DESTINO:")) {
                            String ip = linea.split(":")[1];
                            String puertoLine = in.readLine();
                            int puerto = Integer.parseInt(puertoLine.split(":")[1]);

                            System.out.println("Llamando a " + ip + ":" + puerto + "...");
                            new Thread(() -> AudioCallSender.iniciarLlamada(ip, puerto)).start();
                            new Thread(() -> AudioCallReceiver.iniciarRecepcion(puerto)).start();
                        } else {
                            System.out.println(linea);
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Conexión cerrada por el servidor.");
                }
            });
            recibir.start();

            while (true) {
                

                String linea;

                synchronized (enterLock) {
                    if (esperandoEnter) {
                        continue;
                    }
                    linea = scanner.nextLine();
                }

                if (linea.equals("5")) {
                    System.out.println("Usuarios disponibles:");
                    out.println("5");
                    Thread.sleep(500);
                    System.out.println("Ingresa el nombre del usuario:");
                    String usuario = scanner.nextLine();
                    out.println(usuario);
                    grabarYEnviarAudio(dataOut);
                    continue;
                }

                if (linea.equals("6")) {
                    System.out.println("Grupos disponibles:");
                    out.println("6");
                    Thread.sleep(500);
                    System.out.println("Ingresa el nombre del grupo:");
                    String grupo = scanner.nextLine();
                    out.println(grupo);
                    grabarYEnviarAudio(dataOut);
                    continue;
                }

                if (linea.equals("9")) {
                    System.out.print("Nombre del usuario a llamar: ");
                    String destinatario = scanner.nextLine();
                    System.out.print("IP del usuario: ");
                    String ip = scanner.nextLine();
                    System.out.print("Puerto receptor del usuario: ");
                    int puerto = Integer.parseInt(scanner.nextLine());

                    System.out.println("Iniciando llamada...");
                    new Thread(() -> AudioCallSender.iniciarLlamada(ip, puerto)).start();
                    new Thread(() -> AudioCallReceiver.iniciarRecepcion(puerto)).start();
                    continue;
                }

                if (linea.equals("10")) {
                out.println("10");
                System.out.println("Nombre del usuario receptor:");
                String receptor = scanner.nextLine();
                out.println(receptor);

                ImageSender.enviarImagen(socket, scanner);
                continue;
}


                out.println(linea);

                if (linea.equals("4")) break;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void recibirYReproducirAudio(DataInputStream dataIn) {
        try {
            String emisor = dataIn.readUTF();
            String nombreArchivo = dataIn.readUTF();
            long tamanoArchivo = dataIn.readLong();

            System.out.println("\n Has recibido una nota de voz de " + emisor + " (" + tamanoArchivo + " bytes)");

            File carpeta = new File("audios_recibidos");
            String nombreUnico = System.currentTimeMillis() + "_de_" + emisor + "_" + nombreArchivo;
            File archivoAudio = new File(carpeta, nombreUnico);

            try (FileOutputStream fos = new FileOutputStream(archivoAudio)) {
                byte[] buffer = new byte[4096];
                long bytesRecibidos = 0;

                while (bytesRecibidos < tamanoArchivo) {
                    int bytesParaLeer = (int) Math.min(buffer.length, tamanoArchivo - bytesRecibidos);
                    int bytesLeidos = dataIn.read(buffer, 0, bytesParaLeer);

                    if (bytesLeidos == -1) {
                        throw new IOException("Conexión cerrada inesperadamente");
                    }

                    fos.write(buffer, 0, bytesLeidos);
                    bytesRecibidos += bytesLeidos;
                }
                fos.flush();
            }

            System.out.println("Presiona ENTER para reproducir...");

            System.out.println("Reproduciendo audio...");
            if (reproducirAudio(archivoAudio)) {
                System.out.println("Reproducción completada.");
            } else {
                System.out.println(" Error en la reproducción.");
            }

        } catch (Exception e) {
            System.err.println("Error al recibir audio: " + e.getMessage());
        } finally {
            synchronized (enterLock) {
                esperandoEnter = false;
            }
        }
    }

    private static void grabarYEnviarAudio(DataOutputStream dataOut) {
        try {
            Scanner scanner = new Scanner(System.in);

            AudioFormat formato = new AudioFormat(44100.0f, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, formato);

            if (!AudioSystem.isLineSupported(info)) {
                System.out.println(" Micrófono no soportado");
                return;
            }

            TargetDataLine microfono = (TargetDataLine) AudioSystem.getLine(info);
            microfono.open(formato);
            microfono.start();

            System.out.println("Grabando... Presiona ENTER para detener");
            ByteArrayOutputStream audioBuffer = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];

            final boolean[] grabando = {true};

            Thread controlGrabacion = new Thread(() -> {
                try {
                    scanner.nextLine();
                    grabando[0] = false;
                    microfono.stop();
                    microfono.close();
                } catch (Exception e) {
                    System.err.println("Error en control de grabación: " + e.getMessage());
                }
            });
            controlGrabacion.start();

            while (grabando[0] && microfono.isOpen()) {
                int bytesLeidos = microfono.read(buffer, 0, buffer.length);
                if (bytesLeidos > 0) {
                    audioBuffer.write(buffer, 0, bytesLeidos);
                }
            }

            controlGrabacion.join();
            System.out.println("Grabación detenida");

            byte[] audioData = audioBuffer.toByteArray();
            if (audioData.length == 0) {
                System.out.println("No se grabó audio");
                return;
            }

            File archivoTemporal = new File("temp_audio_" + System.currentTimeMillis() + ".wav");

            try (ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
                 AudioInputStream ais = new AudioInputStream(bais, formato, audioData.length / formato.getFrameSize())) {
                AudioSystem.write(ais, AudioFileFormat.Type.WAVE, archivoTemporal);
            }

            dataOut.writeUTF(archivoTemporal.getName());
            dataOut.writeLong(archivoTemporal.length());

            try (FileInputStream fis = new FileInputStream(archivoTemporal)) {
                byte[] bufferEnvio = new byte[4096];
                int bytesLeidos;
                while ((bytesLeidos = fis.read(bufferEnvio)) > 0) {
                    dataOut.write(bufferEnvio, 0, bytesLeidos);
                }
                dataOut.flush();
            }

            if (archivoTemporal.delete()) {
            }

        } catch (Exception e) {
            System.err.println(" Error al grabar/enviar audio: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static boolean reproducirAudio(File archivoAudio) {
        try {
            if (!archivoAudio.exists() || archivoAudio.length() == 0) {
                System.err.println("El archivo de audio no existe o está vacío");
                return false;
            }

            try (AudioInputStream audioStream = AudioSystem.getAudioInputStream(archivoAudio)) {
                AudioFormat format = audioStream.getFormat();

                Clip clip = AudioSystem.getClip();
                clip.open(audioStream);

                clip.start();

                System.out.println("Duración: " + (clip.getMicrosecondLength() / 1000000.0) + " segundos");

                while (clip.isRunning()) {
                    Thread.sleep(100);
                }

                clip.close();
                return true;

            } catch (UnsupportedAudioFileException e) {
                System.err.println("Formato de audio no soportado: " + e.getMessage());
                return false;
            } catch (LineUnavailableException e) {
                System.err.println("Línea de audio no disponible: " + e.getMessage());
                return false;
            }

        } catch (Exception e) {
            System.err.println("Error al reproducir audio: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
