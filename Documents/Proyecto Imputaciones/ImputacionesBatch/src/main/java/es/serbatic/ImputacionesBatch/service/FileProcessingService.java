package es.serbatic.ImputacionesBatch.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

@Service
@PropertySource("classpath:files.properties")
public class FileProcessingService {

    @Value("${base.directory}")
    private String inputDirectory;

    @Value("${processed.directory}")
    private String processedDirectory;

    @Value("${rejected.directory}")
    private String rejectedDirectory;

    @Value("${input.pattern}")
    private String filePattern;
    
    @Value("${NO_FILES}")
    private String NO_FILES;
    
    public void processFile(StepContribution contribution, ChunkContext chunkContext) {
        // Validar o crear los directorios antes de procesar los archivos
        validateOrCreateDirectories();
        
        System.out.println("Es momento de procesar el fichero: " + (String) chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext().get("fileName"));

        
    }

    // Método para validar y/o crear los directorios necesarios
    public void validateOrCreateDirectories() {
        createDirectoryIfNotExists(inputDirectory);
        createDirectoryIfNotExists(processedDirectory);
        createDirectoryIfNotExists(rejectedDirectory);
        
    }

    private void createDirectoryIfNotExists(String directoryPath) {
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            boolean isCreated = directory.mkdirs();  // Crea el directorio y cualquier directorio padre necesario
            if (isCreated) {
                System.out.println("Directorio creado: " + directoryPath);
            } else {
                System.out.println("Error al crear el directorio: " + directoryPath);
            }
        } else {
            System.out.println("Directorio ya existe: " + directoryPath);
        }
    }
    
    /**
     * Método que comprueba que existan archivos que cumplan el patrón definido y que el más antiguo se meta en el
     * contexto para ser procesado
     * @param chunkContext
     * @param contribution
     * @return
     */
    public boolean hasFilesMatchingPattern(ChunkContext chunkContext, StepContribution contribution) {
        try (
        	Stream<Path> files = Files.list(Paths.get(inputDirectory))) {
            List<Path> matchingFiles = files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().matches(filePattern))
                    .collect(Collectors.toList());
            
            if(matchingFiles.isEmpty()) {
            	contribution.setExitStatus(new ExitStatus(NO_FILES));
            	chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext().put("NO_FILES_TO_PROCESS", "true");
            } else {
            	setFileToWork(chunkContext);
            	return true;
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Recoge los ficheros que estén en el buzón de entrada
     * OJO: La colección está ordenada por fecha de los ficheros. Si deseamos cambiar esto para ordenar por algún otro factor tenemos que modificar el comparator
     * @return
     */
    private List<Path> getFiles() {
        try (
        	Stream<Path> files = Files.list(Paths.get(inputDirectory))) {
            	List<Path> matchingFiles = files.filter(Files::isRegularFile)
            									.filter(path -> path.getFileName().toString().matches(filePattern))
            									.sorted(Comparator.comparing(path -> {
            							            try {
            							                return Files.getLastModifiedTime(path);
            							            } catch (IOException e) {
            							                throw new RuntimeException("Error al obtener la fecha de modificación del archivo: " + path, e);
            							            }
            							        }))
            									.collect(Collectors.toList());
            return matchingFiles; // Devuelve true si hay al menos un archivo que cumple el patrón
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Método que registra en el contexto el nombre y ruta del fichero que hay que procesar y que será accesible por el resto de steps
     * @param chunkContext
     */
    public void setFileToWork(ChunkContext chunkContext) {
    	List<Path> files = getFiles();
    	
    	String fileName = files.get(0).getParent() + "\\" +  files.get(0).getFileName().toString();
    	
    	System.out.println("El fichero que metemos al conexto para procesarlo es: " + fileName);
    	
    	chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext().put("fileName", fileName);
    }
}
