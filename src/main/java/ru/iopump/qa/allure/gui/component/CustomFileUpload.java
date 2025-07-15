package ru.iopump.qa.allure.gui.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.EventData;
import com.vaadin.flow.shared.Registration;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import lombok.Getter;
import lombok.Setter;
import org.atmosphere.config.service.Get;


import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Tag("custom-file-upload")
@JsModule("./custom-file-upload.js")
public class CustomFileUpload extends Component implements HasSize, HasStyle {

    // Хранит список всех загруженных файлов (имя и данные)
    private final Map<String, byte[]> uploadedFiles = new HashMap<>();

    public CustomFileUpload() {
        getElement().setProperty("accept", ".json");
        setAcceptedFileTypes(".json");
    }

    public void setAcceptedFileTypes(String... acceptedFileTypes) {
        if (acceptedFileTypes == null || acceptedFileTypes.length == 0) {
            getElement().removeAttribute("accept");
        } else {
            String accepted = String.join(",", acceptedFileTypes);
            getElement().setAttribute("accept", accepted);
        }
    }

    @Getter
    @DomEvent("files-selected")
    public static class FilesSelectedEvent extends ComponentEvent<CustomFileUpload> {
        private final List<String> fileNames;
        private final Map<String, byte[]> files;

        public FilesSelectedEvent(CustomFileUpload source, boolean fromClient,
                                  @EventData("event.detail.files") JsonArray filesData) {
            super(source, fromClient);
            files = new HashMap<>();
            fileNames = new ArrayList<>();

            if (filesData != null) {
                source.uploadedFiles.clear();

                // Process each file in the JsonArray
                for (int i = 0; i < filesData.length(); i++) {
                    JsonObject fileObj = filesData.getObject(i);
                    String fileName = fileObj.getString("name");
                    String base64Data = fileObj.getString("base64Data");

                    if (fileName != null && base64Data != null) {
                        fileNames.add(fileName);
                        // Split the data URI and get the base64 part (after the comma)
                        byte[] data = Base64.getDecoder().decode(base64Data.split(",")[1]);
                        files.put(fileName, data);
                        source.uploadedFiles.put(fileName, data);
                    }
                }
            }
        }

        public boolean hasFiles() {
            return !fileNames.isEmpty();
        }
    }


    // Добавить слушатель для событий выбора файлов
    public Registration addFilesSelectedListener(ComponentEventListener<FilesSelectedEvent> listener) {
        return addListener(FilesSelectedEvent.class, listener);
    }

    // Получить список всех имен загруженных файлов
    public List<String> getFileNames() {
        return new ArrayList<>(uploadedFiles.keySet());
    }

    // Получить данные определенного файла по имени
    public InputStream getInputStream(String fileName) {
        byte[] data = uploadedFiles.get(fileName);
        return (data != null) ? new ByteArrayInputStream(data) : null;
    }

    // Проверить, есть ли загруженные файлы
    public boolean hasFiles() {
        return !uploadedFiles.isEmpty();
    }

    // Получить данные первого файла (для обратной совместимости)
    public InputStream getInputStream() {
        return uploadedFiles.values().stream()
                .findFirst()
                .map(ByteArrayInputStream::new)
                .orElse(null);
    }

    // Получить имя первого файла (для обратной совместимости)
    public String getFileName() {
        return uploadedFiles.keySet().stream().findFirst().orElse(null);
    }

    // Очистить все загруженные файлы
    public void clear() {
        getElement().executeJs("this.clearFiles()");
        uploadedFiles.clear();
    }
}