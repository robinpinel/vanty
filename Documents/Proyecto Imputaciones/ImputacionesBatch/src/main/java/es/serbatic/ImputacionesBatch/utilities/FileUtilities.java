package es.serbatic.ImputacionesBatch.utilities;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;

public class FileUtilities {

	public static boolean moveFileWithRetries(Path sourceFilePath, Path tempDirPath, int maxAttempts, long delayInMillis) {

        Path targetPath = tempDirPath.resolve(sourceFilePath.getFileName());
        
        try {
            Files.createDirectories(tempDirPath);
        } catch (IOException e) {
            System.err.println("Error al crear el directorio temporal: " + e.getMessage());
            return false;
        }
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                Files.move(sourceFilePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Archivo movido exitosamente a: " + targetPath);
                return true; 
            } catch (IOException e) {
                System.err.println("Intento " + attempt + " de mover el archivo fallido: " + e.getMessage());
                
                // Espera antes de reintentar
                try {
                    TimeUnit.MILLISECONDS.sleep(delayInMillis);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    System.err.println("Operación interrumpida durante la espera.");
                    return false;
                }
            }
        }
        
        System.err.println("No se pudo mover el archivo después de " + maxAttempts + " intentos.");
        return false;
    }
}
