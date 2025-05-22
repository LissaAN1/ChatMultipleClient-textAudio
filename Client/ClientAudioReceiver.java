package Client;
import java.io.*;
import java.net.Socket;

public class ClientAudioReceiver {
    private Socket socket;
    private DataInputStream dataIn;

    public ClientAudioReceiver(Socket socket) throws IOException {
        this.socket = socket;
        this.dataIn = new DataInputStream(socket.getInputStream());
    }

    public File recibirArchivoAudio() throws IOException {
        String nombreArchivo = dataIn.readUTF();
        long tamArchivo = dataIn.readLong();
        
        File archivo = new File("client_received_" + nombreArchivo);
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
}
