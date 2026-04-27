package ru.tms.user.service.exception;

/**
 * Используется, когда ресурс не найден или текущий пользователь не имеет доступа к нему.
 * Чтобы не раскрывать факт существования ресурса, обычно маппится в 404.
 */
public class NotFoundOrAccessDeniedException extends RuntimeException {

    public NotFoundOrAccessDeniedException(String message) {
        super(message);
    }
}


