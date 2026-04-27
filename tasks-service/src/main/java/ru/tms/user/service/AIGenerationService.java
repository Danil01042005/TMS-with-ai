package ru.tms.user.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.tms.user.ai.AIClient;
import ru.tms.user.entity.AnswerOptionEntity;
import ru.tms.user.entity.QuestionEntity;
import ru.tms.user.entity.TestEntity;
import ru.tms.user.entity.enums.DifficultyLevel;
import ru.tms.user.entity.enums.QuestionType;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIGenerationService {

    private final AIClient aiClient;
    private final ObjectMapper objectMapper;

    public List<QuestionEntity> generateQuestions(
            TestEntity test,
            String mode,
            String topic,
            String material,
            String difficulty,
            int questionCount,
            List<String> questionTypes
    ) {
        log.info("[AIGenerationService] Generating {} questions for test {} (mode={}, difficulty={})", 
                questionCount, test.getTestId(), mode, difficulty);

        String prompt = buildPrompt(mode, topic, material, difficulty, questionCount, questionTypes);
        log.debug("[AIGenerationService] Prompt: {}", prompt);

        try {
            log.info("[AIGenerationService] Calling AI client...");
            String response = aiClient.complete(prompt);
            log.info("[AIGenerationService] Received response from Groq (length: {})", response != null ? response.length() : 0);
            log.debug("[AIGenerationService] AI Response: {}", response);

            if (response == null || response.trim().isEmpty()) {
                log.error("[AIGenerationService] Empty response from Groq");
                throw new RuntimeException("Groq returned empty response");
            }

            List<QuestionEntity> questions = parseQuestions(response, test, difficulty, questionTypes);
            log.info("[AIGenerationService] Generated {} questions", questions.size());
            return questions;
        } catch (RuntimeException e) {
            log.error("[AIGenerationService] Runtime error generating questions: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("[AIGenerationService] Error generating questions: {}", e.getMessage(), e);
            log.error("[AIGenerationService] Exception class: {}", e.getClass().getName());
            if (e.getCause() != null) {
                log.error("[AIGenerationService] Cause: {}", e.getCause().getMessage());
            }
            throw new RuntimeException("Failed to generate questions: " + e.getMessage() + 
                    (e.getCause() != null ? " (cause: " + e.getCause().getMessage() + ")" : ""), e);
        }
    }

    private String buildPrompt(String mode, String topic, String material, String difficulty, 
                              int questionCount, List<String> questionTypes) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Создай тест с вопросами в формате JSON. Каждый вопрос должен быть в следующем формате:\n");
        prompt.append("{\n");
        prompt.append("  \"questionText\": \"Текст вопроса\",\n");
        prompt.append("  \"questionType\": \"SINGLE_CHOICE\" | \"MULTIPLE_CHOICE\" | \"OPEN\",\n");
        prompt.append("  \"answerOptions\": [\n");
        prompt.append("    {\"optionText\": \"Вариант ответа\", \"isCorrect\": true/false, \"displayOrder\": 1}\n");
        prompt.append("  ]\n");
        prompt.append("}\n\n");
        prompt.append("Требования:\n");
        prompt.append("- Количество вопросов: ").append(questionCount).append("\n");
        prompt.append("- Сложность: ").append(difficulty).append("\n");
        prompt.append("- Типы вопросов: ").append(String.join(", ", questionTypes)).append("\n");
        
        if ("topic".equals(mode)) {
            prompt.append("- Тема: ").append(topic).append("\n");
            prompt.append("Создай вопросы на эту тему.\n");
        } else {
            prompt.append("- Учебный материал:\n").append(material).append("\n");
            prompt.append("Создай вопросы на основе этого материала.\n");
        }
        
        prompt.append("\nВерни только JSON массив вопросов, без дополнительного текста. Формат:\n");
        prompt.append("[{\"questionText\": \"...\", \"questionType\": \"...\", \"answerOptions\": [...]}, ...]\n");

        return prompt.toString();
    }

    private List<QuestionEntity> parseQuestions(String response, TestEntity test, 
                                                String difficulty, List<String> questionTypes) {
        List<QuestionEntity> questions = new ArrayList<>();
        
        try {
            // Извлекаем JSON массив из ответа
            String jsonArray = extractJsonArray(response);
            if (jsonArray == null) {
                log.warn("[AIGenerationService] Could not extract JSON array from response");
                return questions;
            }

            // Парсим JSON через Jackson
            JsonNode rootNode = objectMapper.readTree(jsonArray);
            if (!rootNode.isArray()) {
                log.warn("[AIGenerationService] Response is not a JSON array");
                return questions;
            }

            DifficultyLevel difficultyLevel = parseDifficulty(difficulty);
            
            int order = 1;
            for (JsonNode questionNode : rootNode) {
                QuestionEntity question = new QuestionEntity();
                question.setTest(test);
                question.setQuestionText(questionNode.get("questionText").asText());
                question.setQuestionType(parseQuestionType(
                        questionNode.get("questionType").asText(), questionTypes));
                question.setDifficulty(difficultyLevel);
                question.setQuestionOrder(order++);
                
                List<AnswerOptionEntity> options = new ArrayList<>();
                JsonNode optionsNode = questionNode.get("answerOptions");
                if (optionsNode != null && optionsNode.isArray()) {
                    int optionOrder = 1;
                    for (JsonNode optionNode : optionsNode) {
                        AnswerOptionEntity option = new AnswerOptionEntity();
                        option.setQuestion(question);
                        option.setOptionText(optionNode.get("optionText").asText());
                        option.setCorrect(optionNode.get("isCorrect").asBoolean());
                        option.setDisplayOrder(optionNode.has("displayOrder") 
                                ? optionNode.get("displayOrder").asInt() 
                                : optionOrder);
                        options.add(option);
                        optionOrder++;
                    }
                }
                question.setAnswerOptions(options);
                
                questions.add(question);
            }
        } catch (Exception e) {
            log.error("[AIGenerationService] Error parsing questions", e);
            throw new RuntimeException("Failed to parse generated questions: " + e.getMessage(), e);
        }
        
        return questions;
    }

    private String extractJsonArray(String response) {
        // Ищем JSON массив в ответе (может быть обернут в markdown code blocks или текст)
        int startIdx = response.indexOf('[');
        int endIdx = response.lastIndexOf(']');
        if (startIdx >= 0 && endIdx > startIdx) {
            return response.substring(startIdx, endIdx + 1);
        }
        return null;
    }

    private QuestionType parseQuestionType(String type, List<String> allowedTypes) {
        String upperType = type.toUpperCase().replace("-", "_").replace(" ", "_");
        if (upperType.contains("SINGLE_CHOICE") || upperType.contains("SINGLE") || upperType.contains("ONE")) {
            return QuestionType.SINGLE_CHOICE;
        } else if (upperType.contains("MULTIPLE_CHOICE") || upperType.contains("MULTIPLE") || upperType.contains("SEVERAL")) {
            return QuestionType.MULTIPLE_CHOICE;
        } else {
            return QuestionType.OPEN;
        }
    }

    private DifficultyLevel parseDifficulty(String difficulty) {
        try {
            return DifficultyLevel.valueOf(difficulty.toUpperCase());
        } catch (Exception e) {
            log.warn("[AIGenerationService] Unknown difficulty: {}, using MEDIUM", difficulty);
            return DifficultyLevel.MEDIUM;
        }
    }

}
