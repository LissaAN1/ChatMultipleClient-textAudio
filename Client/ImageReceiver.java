package Client;

import java.awt.Desktop;
import java.io.*;

public class ImageReceiver{
    public static void recibir(DataInputStream dataIn, String emisor) throws IOException {
        String nombreArchivo = dataIn.readUTF();
        long tamano = dataIn.readLong();

        File carpeta = new File("imagenes_recibidas");
        if (!carpeta.exists()) carpeta.mkdirs();

        File imagen = new File(carpeta, System.currentTimeMillis() + "_" + nombreArchivo);

        try (FileOutputStream fos = new FileOutputStream(imagen)) {
            byte[] buffer = new byte[4096];
            long bytesRecibidos = 0;

            while (bytesRecibidos < tamano) {
                int bytesParaLeer = (int) Math.min(buffer.length, tamano - bytesRecibidos);
                int bytesLeidos = dataIn.read(buffer, 0, bytesParaLeer);
                if (bytesLeidos == -1) break;
                fos.write(buffer, 0, bytesLeidos);
                bytesRecibidos += bytesLeidos;
            }
        }

        System.out.println("\nImagen recibida de " + emisor);

        try {
            Desktop.getDesktop().open(imagen);
        } catch (IOException e) {
            System.out.println("No se pudo abrir automáticamente la imagen.");
        }
    }
}
