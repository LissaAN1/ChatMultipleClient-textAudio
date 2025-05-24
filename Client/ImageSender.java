package Client;

import java.io.*;
import java.nio.file.Files;

public class ImageSender {
    public static void enviar(File archivoImagen, DataOutputStream dataOut, String destinatario) throws IOException {
        if (!archivoImagen.exists() || !archivoImagen.isFile()) {
            throw new FileNotFoundException("Archivo de imagen no encontrado: " + archivoImagen.getAbsolutePath());
        }

        byte[] datos = Files.readAllBytes(archivoImagen.toPath());

        dataOut.writeUTF("IMAGEN");
        dataOut.writeUTF(destinatario);
        dataOut.writeUTF(archivoImagen.getName());
        dataOut.writeLong(datos.length);
        dataOut.write(datos);
        dataOut.flush();
    }
}

