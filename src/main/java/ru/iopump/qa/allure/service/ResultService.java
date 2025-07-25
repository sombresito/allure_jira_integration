package ru.iopump.qa.allure.service;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.iopump.qa.allure.helper.MoveFileVisitor;
import ru.iopump.qa.allure.model.ResultResponse;
import ru.iopump.qa.allure.properties.AllureProperties;
import ru.iopump.qa.util.FileUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.nio.file.Files.isDirectory;

@Getter
@Component
@Slf4j
public class ResultService {
    private final Path storagePath;

    @Autowired
    public ResultService(AllureProperties cfg) {
        this(Paths.get(cfg.resultsDir()));
    }

    ResultService(final Path storagePath) {
        this.storagePath = storagePath;
    }

    public ResultResponse internalDeleteByUUID(String uuid) throws IOException {
        var p = storagePath.resolve(uuid);
        long size = FileUtils.sizeOfDirectory(p.toFile()) / 1024;
        LocalDateTime localDateTime = LocalDateTime.MIN;
        try {
            BasicFileAttributes attr = Files.readAttributes(p, BasicFileAttributes.class);
            localDateTime = LocalDateTime.ofInstant(attr.creationTime().toInstant(), ZoneId.systemDefault());
        } catch (IOException e) {
            if (log.isErrorEnabled()) {
                log.error("Error getting created date time of " + p, e);
            }
        }
        var res = ResultResponse.builder().uuid(p.getFileName().toString()).created(localDateTime).size(size).build();

        FileUtils.deleteDirectory(storagePath.resolve(uuid).toFile());
        return res;
    }

    public void deleteAll() throws IOException {
        FileUtils.deleteDirectory(storagePath.toFile());
    }

    public Collection<Path> getAll() throws IOException {
        if (!Files.exists(storagePath)) {
            return Collections.emptySet();
        }
        return Files.walk(storagePath, 1).skip(1)
            .filter(p -> isDirectory(p))
            .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Проверьте архив, разархивируйте и сохраните в файловую систему.
     * Каталог с именем uuid будет содержать содержимое архива.
     *
     * @param archiveInputStream будет закрыт автоматически.
     * @return Каталог, содержащий содержимое архива.
     * @throws IOException Ошибка ввода-вывода
     */
    @NonNull
    public Path unzipAndStore(@NonNull InputStream archiveInputStream) throws IOException {
        Preconditions.checkArgument(archiveInputStream.available() > 0,
            "Passed InputStream is empty");
        Path tmpResultDirectory = null;
        Path resultDirectory = null;
        try (InputStream io = archiveInputStream) {
            final String uuid = UUID.randomUUID().toString();
            tmpResultDirectory = storagePath.resolve(uuid + "_tmp");
            resultDirectory = storagePath.resolve(uuid);
            Files.createDirectories(resultDirectory);
            checkAndUnzipTo(io, tmpResultDirectory);
            move(tmpResultDirectory, resultDirectory);
        } catch (Exception ex) {
            if (resultDirectory != null) {
                // Clean on error
                FileUtils.deleteQuietly(resultDirectory.toFile());
            }
            if (tmpResultDirectory != null) {
                // Clean on error
                FileUtils.deleteQuietly(tmpResultDirectory.toFile());
            }
            throw ex; // And re-throw
        }
        log.info("Archive content saved to '{}'", resultDirectory); // лог сохранения файлов
        return resultDirectory;
    }

    private void checkAndUnzipTo(InputStream zipArchiveIo, Path unzipTo) throws IOException {
        ZipInputStream zis = new ZipInputStream(zipArchiveIo);
        byte[] buffer = new byte[1024];
        ZipEntry zipEntry = zis.getNextEntry();
        if (zipEntry == null) {
            throw new IllegalArgumentException("Passed InputStream is not a Zip Archive or empty");
        }
        while (zipEntry != null) {             // цикл загрузки файлов
            final Path newFile = fromZip(unzipTo, zipEntry);
            try (final OutputStream fos = Files.newOutputStream(newFile)) {
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
            }
            log.info("Unzip new entry '{}'", newFile); //лог загрузки файлов
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();

        log.info("Unzipping successfully finished to '{}'", unzipTo);  // лог информирования о завершения загрузки
    }

    private void move(Path from, Path to) throws IOException {
        Files.find(from,
                        1,
                        (path, basicFileAttributes)
                                -> basicFileAttributes.isDirectory() && (path.getFileName().toString()
                                .matches("allure-.+|report.*")))
            .forEach(
                nestedResultDir -> {
                    try {
                        Files.walkFileTree(nestedResultDir, new MoveFileVisitor(to));
                    } catch (IOException e) {
                        throw new RuntimeException("Walk error " + nestedResultDir, e);
                    }
                }
            );
        Files.walkFileTree(from, new MoveFileVisitor(to));
    }

    private Path fromZip(Path unzipTo, ZipEntry zipEntry) {
        final Path entryPath = Paths.get(zipEntry.getName());
        final Path destinationFileOrDir = unzipTo.resolve(entryPath);

        if (isDirectory(destinationFileOrDir)) {
            FileUtil.createDir(destinationFileOrDir);
        } else {
            FileUtil.createFile(destinationFileOrDir);
        }

        return destinationFileOrDir;
    }

}
