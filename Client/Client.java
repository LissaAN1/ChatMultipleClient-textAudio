package Client;

import javax.sound.sampled.*;
import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 6789;
    
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
            // Crear directorio para audios recibidos
            new File("audios_recibidos").mkdirs();

            // Hilo para recibir mensajes y audios
            Thread recibir = new Thread(() -> {
                try {
                    String linea;
                    while ((linea = in.readLine()) != null) {
                        if (linea.equals("AUDIO_INCOMING")) {
                            // Se va a recibir un audio
                            recibirYReproducirAudio(dataIn);
                        } else {
                            // Mostrar mensaje de texto normal
                            System.out.println(linea);
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Conexión cerrada por el servidor.");
                }
            });
            recibir.start();

            // Hilo principal para enviar mensajes y audios
            while (true) {
                String linea;
                
                // Si estamos esperando ENTER para reproducir audio, usar un scanner diferente
                synchronized (enterLock) {
                    if (esperandoEnter) {
                        continue; // Esperar hasta que se termine de manejar el audio
                    }
                    linea = scanner.nextLine();
                }

                if (linea.equals("5")) { 
                    // Nota de voz privada
                    System.out.println("Usuarios disponibles:");
                    out.println("5"); // Enviar opción primero
                    
                    // Dar tiempo para recibir la lista
                    Thread.sleep(500);
                    
                    System.out.println("Ingresa el nombre del usuario:");
                    String usuario = scanner.nextLine();
                    out.println(usuario); // Enviar destinatario
                    
                    // Grabar y enviar audio
                    grabarYEnviarAudio(dataOut);
                    continue;
                }
                
                if (linea.equals("6")) { 
                    // Nota de voz a grupo
                    System.out.println("Grupos disponibles:");
                    out.println("6"); // Enviar opción primero
                    
                    // Dar tiempo para recibir la lista
                    Thread.sleep(500);
                    
                    System.out.println("Ingresa el nombre del grupo:");
                    String grupo = scanner.nextLine();
                    out.println(grupo); // Enviar destinatario
                    
                    // Grabar y enviar audio
                    grabarYEnviarAudio(dataOut);
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
            synchronized (enterLock) {
                esperandoEnter = true;
            }
            
            // Recibir información del audio
            String emisor = dataIn.readUTF();
            String nombreArchivo = dataIn.readUTF();
            long tamanoArchivo = dataIn.readLong();

            System.out.println("\n📢 Has recibido una nota de voz de " + emisor + " (" + tamanoArchivo + " bytes)");

            // Crear archivo para guardar el audio
            File carpeta = new File("audios_recibidos");
            String nombreUnico = System.currentTimeMillis() + "_de_" + emisor + "_" + nombreArchivo;
            File archivoAudio = new File(carpeta, nombreUnico);

            // Recibir y guardar el archivo
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

            System.out.println("🎵 Audio guardado como: " + archivoAudio.getName());
            System.out.println("Presiona ENTER para reproducir...");
            
            // Esperar a que el usuario presione ENTER (usar un nuevo Scanner para evitar conflictos)
            try (Scanner enterScanner = new Scanner(System.in)) {
                enterScanner.nextLine();
            }

            // Reproducir el audio
            System.out.println("🔊 Reproduciendo audio...");
            if (reproducirAudio(archivoAudio)) {
                System.out.println("✅ Reproducción completada.");
            } else {
                System.out.println("❌ Error en la reproducción.");
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

            // Configurar formato de audio
            AudioFormat formato = new AudioFormat(44100.0f, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, formato);
            
            if (!AudioSystem.isLineSupported(info)) {
                System.out.println("❌ Micrófono no soportado");
                return;
            }

            TargetDataLine microfono = (TargetDataLine) AudioSystem.getLine(info);
            microfono.open(formato);
            microfono.start();

            System.out.println("🎤 Grabando... Presiona ENTER para detener");
            ByteArrayOutputStream audioBuffer = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];

            // Variable para controlar la grabación
            final boolean[] grabando = {true};

            // Hilo para detener grabación al presionar ENTER
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

            // Grabar audio mientras esté activo
            while (grabando[0] && microfono.isOpen()) {
                int bytesLeidos = microfono.read(buffer, 0, buffer.length);
                if (bytesLeidos > 0) {
                    audioBuffer.write(buffer, 0, bytesLeidos);
                }
            }

            controlGrabacion.join();
            System.out.println("🛑 Grabación detenida");

            // Crear archivo WAV temporal
            byte[] audioData = audioBuffer.toByteArray();
            if (audioData.length == 0) {
                System.out.println("❌ No se grabó audio");
                return;
            }
            
            File archivoTemporal = new File("temp_audio_" + System.currentTimeMillis() + ".wav");
            
            try (ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
                 AudioInputStream ais = new AudioInputStream(bais, formato, audioData.length / formato.getFrameSize())) {
                AudioSystem.write(ais, AudioFileFormat.Type.WAVE, archivoTemporal);
            }

            System.out.println("📤 Enviando audio (" + archivoTemporal.length() + " bytes)...");

            // Enviar archivo al servidor
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

            // Eliminar archivo temporal
            if (archivoTemporal.delete()) {
                System.out.println("🗑️ Archivo temporal eliminado");
            }

            System.out.println("✅ Audio enviado correctamente");

        } catch (Exception e) {
            System.err.println("❌ Error al grabar/enviar audio: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static boolean reproducirAudio(File archivoAudio) {
        try {
            // Verificar que el archivo existe y no está vacío
            if (!archivoAudio.exists() || archivoAudio.length() == 0) {
                System.err.println("El archivo de audio no existe o está vacío");
                return false;
            }

            try (AudioInputStream audioStream = AudioSystem.getAudioInputStream(archivoAudio)) {
                AudioFormat format = audioStream.getFormat();
                
                // Crear clip
                Clip clip = AudioSystem.getClip();
                clip.open(audioStream);
                
                // Reproducir
                clip.start();
                
                System.out.println("Duración: " + (clip.getMicrosecondLength() / 1000000.0) + " segundos");

                // Esperar a que termine la reproducción
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