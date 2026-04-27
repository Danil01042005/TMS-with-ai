package ru.tms.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.upload")
public class UploadProperties {

    /**
     * Максимальный размер загружаемого файла (байт).
     */
    private long maxBytes = 10 * 1024 * 1024;

    /**
     * Допустимые content-type (через запятую).
     * Используется как основное ограничение; дополнительно проверяем расширение.
     */
    private String allowedContentTypes =
            "application/pdf,text/plain,image/png,image/jpeg," +
            "application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    /**
     * Допустимые расширения файлов (через запятую, нижний регистр, без точки).
     */
    private String allowedExtensions = "pdf,txt,png,jpg,jpeg,doc,docx";
}

