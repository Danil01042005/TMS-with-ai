package ru.tms.user.ai;

/**
 * Абстракция над AI-провайдером.
 * Позволяет подменять реализацию (Groq, OpenAI, локальная модель и т.д.).
 */
public interface AIClient {

    /**
     * Выполнить запрос к AI-модели и вернуть «сырое» текстовое содержимое ответа.
     *
     * @param prompt текст запроса/промпт
     * @return содержимое ответа модели (обычно JSON или текст)
     */
    String complete(String prompt);
}













