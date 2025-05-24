package Client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import java.nio.file.Files;

public class ImageSender {
    public static void enviarImagen(Socket socket, Scanner scanner) {
        try {
            DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());

            System.out.print("Ruta del archivo de imagen: ");
            String ruta = scanner.nextLine();
            File archivoImagen = new File(ruta);

            if (!archivoImagen.exists() || !archivoImagen.isFile()) {
                System.out.println("⚠️ Archivo no encontrado. Regresando al menú...");
                return; // vuelve al menú principal
            }

            byte[] datos = Files.readAllBytes(archivoImagen.toPath());

            dataOut.writeUTF(archivoImagen.getName());
            dataOut.writeLong(datos.length);
            dataOut.write(datos);
            dataOut.flush();

            System.out.println("✅ Imagen enviada correctamente.");

        } catch (IOException e) {
            System.err.println("❌ Error al enviar imagen: " + e.getMessage());
        }
    }
}
