package ru.tms.user.ai;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Компонент для логирования информации о модели Ollama.
 * Модель должна быть предварительно установлена в Ollama.
 */
@Component
public class OllamaModelImporter {

    private static final Logger logger = LoggerFactory.getLogger(OllamaModelImporter.class);

    @Value("${spring.ai.ollama.chat.options.model:llama3}")
    private String modelName;

    @PostConstruct
    public void ensureModelAvailable() {
        logger.info("[OllamaModelImporter] Using Ollama model: '{}'", modelName);
        logger.info("[OllamaModelImporter] Ensure that model '{}' is installed in Ollama. " +
                    "If not, run: ollama pull {}", modelName, modelName);
    }
}



