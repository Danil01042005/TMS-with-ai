package ru.tms.user.database.config;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Наполняет БД осмысленными демо-данными (банки → тесты → вопросы → варианты).
 * Включается только при {@code app.seed.demo-data=true} (в Docker по умолчанию).
 * Идемпотентность: в описание каждого банка добавляется маркер {@link #SEED_MARKER}.
 * Повторный запуск пропускается, пока не задано {@code app.seed.force=true} — тогда демо-банки
 * этого владельца удаляются каскадно и вставляются заново.
 */
@Component
@Order(10)
@ConditionalOnProperty(name = "app.seed.demo-data", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    static final String SEED_MARKER = " [demo-seed-v1]";

    private final JdbcTemplate jdbc;

    @Value("${app.seed.owner-id:1}")
    private String ownerId;

    @Value("${app.seed.force:false}")
    private boolean force;

    @Value("${app.seed.bulk-data:false}")
    private boolean bulkDataEnabled;

    @Value("${app.seed.bulk.banks:25}")
    private int bulkBanks;

    @Value("${app.seed.bulk.tests-per-bank:12}")
    private int bulkTestsPerBank;

    @Value("${app.seed.bulk.questions-per-test:10}")
    private int bulkQuestionsPerTest;

    public DemoDataSeeder(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (force) {
                int removed = jdbc.update(
                        """
                        DELETE FROM question_banks
                        WHERE owner_id = ? AND description LIKE ?
                        """,
                        ownerId,
                        "%" + SEED_MARKER);
                log.info("Demo seed force: removed {} demo banks for owner_id={}", removed, ownerId);
            } else {
                Integer existing =
                        jdbc.queryForObject(
                                """
                                SELECT COUNT(*)::int FROM question_banks
                                WHERE owner_id = ? AND description LIKE ?
                                """,
                                Integer.class,
                                ownerId,
                                "%" + SEED_MARKER);
                if (existing != null && existing > 0) {
                    log.info("Demo seed skipped: already present for owner_id={}", ownerId);
                    return;
                }
            }

            seedJavaOop();
            seedSql();
            seedAlgorithms();
            seedHttpRest();
            seedDiscreteMath();
            if (bulkDataEnabled) {
                seedBulkValidationData();
            }
            log.info("Demo seed completed for owner_id={}", ownerId);
        } catch (Exception e) {
            log.error("Demo seed failed: {}", e.getMessage(), e);
        }
    }

    private long insertBank(String name, String description) {
        Long id =
                jdbc.queryForObject(
                        """
                        INSERT INTO question_banks (name, description, owner_id, is_active)
                        VALUES (?, ?, ?, TRUE)
                        RETURNING bank_id
                        """,
                        Long.class,
                        name,
                        description + SEED_MARKER,
                        ownerId);
        return Objects.requireNonNull(id);
    }

    private long insertTest(
            long bankId,
            String name,
            String description,
            String difficulty,
            int timeLimit,
            int attempts,
            String status,
            LocalDate publishedDate) {
        Long id =
                jdbc.queryForObject(
                        """
                        INSERT INTO tests (bank_id, name, description, difficulty, time_limit,
                            num_questions, attempts, status, owner_id, published_date)
                        VALUES (?, ?, ?, ?, ?, 0, ?, ?, ?, ?)
                        RETURNING test_id
                        """,
                        Long.class,
                        bankId,
                        name,
                        description,
                        difficulty,
                        timeLimit,
                        attempts,
                        status,
                        ownerId,
                        publishedDate == null ? null : Date.valueOf(publishedDate));
        return Objects.requireNonNull(id);
    }

    private void insertQuestion(long testId, int order, String text, String qType, String diff, Opt... options) {
        Long qid =
                jdbc.queryForObject(
                        """
                        INSERT INTO questions (test_id, question_text, question_type, difficulty, question_order)
                        VALUES (?, ?, ?, ?, ?)
                        RETURNING question_id
                        """,
                        Long.class,
                        testId,
                        text,
                        qType,
                        diff,
                        order);
        Objects.requireNonNull(qid);
        for (int i = 0; i < options.length; i++) {
            Opt o = options[i];
            jdbc.update(
                    """
                    INSERT INTO answer_options (question_id, option_text, is_correct, display_order)
                    VALUES (?, ?, ?, ?)
                    """,
                    qid,
                    o.text,
                    o.correct,
                    i);
        }
    }

    private void syncNumQuestions(long testId) {
        jdbc.update(
                """
                UPDATE tests SET num_questions = (
                    SELECT COUNT(*)::int FROM questions WHERE test_id = ?
                ) WHERE test_id = ?
                """,
                testId,
                testId);
    }

    private record Opt(String text, boolean correct) {}

    private void seedJavaOop() {
        long bank =
                insertBank(
                        "Java SE: ООП и платформа",
                        "Синтаксис, JVM, классы, исключения, коллекции — для курса введения в Java.");
        long t1 =
                insertTest(
                        bank,
                        "Модуль 1: JVM и базовый синтаксис",
                        "Типы, операторы, строки, массивы.",
                        "EASY",
                        25,
                        3,
                        "DRAFT",
                        null);
        insertQuestion(
                t1,
                0,
                "Где физически выполняется байткод Java-приложения?",
                "SINGLE_CHOICE",
                "EASY",
                new Opt("В операционной системе напрямую, без посредников", false),
                new Opt("На виртуальной машине JVM", true),
                new Opt("Только в браузере", false),
                new Opt("В компиляторе javac", false));
        insertQuestion(
                t1,
                1,
                "Какой тип лучше использовать для денежных расчётов без потери точности?",
                "SINGLE_CHOICE",
                "MEDIUM",
                new Opt("double", false),
                new Opt("float", false),
                new Opt("BigDecimal", true),
                new Opt("int в копейках — всегда хуже BigDecimal", false));
        insertQuestion(
                t1,
                2,
                "Что верно про `String` в Java?",
                "MULTIPLE_CHOICE",
                "MEDIUM",
                new Opt("Строки неизменяемы (immutable)", true),
                new Opt("Оператор `==` сравнивает содержимое строк", false),
                new Opt("Метод `equals` сравнивает содержимое", true),
                new Opt("Конкатенация в цикле через `+` всегда оптимальна", false));
        insertQuestion(
                t1,
                3,
                "Что выведет код: `int x = 7 / 2; System.out.println(x);`?",
                "SINGLE_CHOICE",
                "EASY",
                new Opt("3.5", false),
                new Opt("3", true),
                new Opt("4", false),
                new Opt("Ошибка компиляции", false));
        insertQuestion(
                t1,
                4,
                "Как объявить константу на уровне класса?",
                "SINGLE_CHOICE",
                "EASY",
                new Opt("const int MAX = 100;", false),
                new Opt("final static int MAX = 100;", true),
                new Opt("static final int MAX = 100; — недопустимо", false),
                new Opt("#define MAX 100", false));
        syncNumQuestions(t1);

        long t2 =
                insertTest(
                        bank,
                        "Модуль 2: Классы, наследование, полиморфизм",
                        "Инкапсуляция, переопределение, абстрактные классы и интерфейсы.",
                        "MEDIUM",
                        40,
                        2,
                        "PUBLISHED",
                        LocalDate.now().minusDays(12));
        insertQuestion(
                t2,
                0,
                "Ключевое слово, запрещающее переопределение метода в подклассе?",
                "SINGLE_CHOICE",
                "MEDIUM",
                new Opt("static", false),
                new Opt("final", true),
                new Opt("abstract", false),
                new Opt("private", false));
        insertQuestion(
                t2,
                1,
                "Что такое полиморфизм в контексте Java?",
                "SINGLE_CHOICE",
                "MEDIUM",
                new Opt("Возможность хранить несколько значений в одной переменной без типа", false),
                new Opt(
                        "Один интерфейс — разные реализации; вызов метода зависит от фактического типа объекта",
                        true),
                new Opt("Только перегрузка методов", false),
                new Opt("Использование generics", false));
        insertQuestion(
                t2,
                2,
                "Какие утверждения про интерфейсы в Java 8+ верны?",
                "MULTIPLE_CHOICE",
                "HARD",
                new Opt("Интерфейс может содержать default-методы", true),
                new Opt("Интерфейс может содержать static-методы", true),
                new Opt("Интерфейс может иметь конструктор с параметрами", false),
                new Opt("Поля интерфейса по умолчанию public static final", true));
        insertQuestion(
                t2,
                3,
                "Что произойдёт при `Parent p = new Child(); p.m();`, если `m` переопределён в Child?",
                "SINGLE_CHOICE",
                "MEDIUM",
                new Opt("Вызовется версия Parent", false),
                new Opt("Вызовется версия Child", true),
                new Opt("Ошибка компиляции", false),
                new Opt("Исключение при выполнении", false));
        insertQuestion(
                t2,
                4,
                "Зачем нужен метод `equals` наряду с `hashCode`?",
                "SINGLE_CHOICE",
                "HARD",
                new Opt("Только для красоты кода", false),
                new Opt(
                        "Согласованный контракт: равные объекты должны иметь одинаковый hashCode для коллекций",
                        true),
                new Opt("hashCode нужен только для примитивов", false),
                new Opt("equals вызывается GC", false));
        insertQuestion(
                t2,
                5,
                "Checked vs unchecked исключения — что верно?",
                "MULTIPLE_CHOICE",
                "HARD",
                new Opt("Checked должны быть объявлены или перехвачены", true),
                new Opt("RuntimeException — подкласс Exception и обычно unchecked", true),
                new Opt("Все исключения в Java checked", false),
                new Opt("Error наследует Exception", false));
        insertQuestion(
                t2,
                6,
                "Шаблон проектирования «одиночка» (Singleton) — основная цель?",
                "SINGLE_CHOICE",
                "MEDIUM",
                new Opt("Ускорить любой цикл for", false),
                new Opt("Гарантировать единственный экземпляр класса в приложении", true),
                new Opt("Заменить все интерфейсы", false),
                new Opt("Автоматически освобождать память", false));
        insertQuestion(
                t2,
                7,
                "Что выведет: `try { return 1; } finally { return 2; }`?",
                "SINGLE_CHOICE",
                "HARD",
                new Opt("1", false),
                new Opt("2", true),
                new Opt("Ошибка компиляции", false),
                new Opt("Зависит от JIT", false));
        syncNumQuestions(t2);

        long t3 =
                insertTest(
                        bank,
                        "Итоговый зачёт: коллекции и Generics",
                        "List, Set, Map, итераторы, типобезопасность.",
                        "HARD",
                        55,
                        1,
                        "PUBLISHED",
                        LocalDate.now().minusDays(3));
        insertQuestion(
                t3,
                0,
                "Какая коллекция гарантирует уникальность элементов и неупорядоченность (типичный контракт)?",
                "SINGLE_CHOICE",
                "MEDIUM",
                new Opt("ArrayList", false),
                new Opt("HashSet", true),
                new Opt("LinkedList", false),
                new Opt("PriorityQueue", false));
        insertQuestion(
                t3,
                1,
                "Сложность в среднем для `HashMap.get(key)` при хорошей хэш-функции?",
                "SINGLE_CHOICE",
                "HARD",
                new Opt("O(n)", false),
                new Opt("O(log n)", false),
                new Opt("O(1)", true),
                new Opt("O(n²)", false));
        insertQuestion(
                t3,
                2,
                "Что верно про `ConcurrentHashMap`?",
                "MULTIPLE_CHOICE",
                "HARD",
                new Opt("Потокобезопасная альтернатива HashMap для многих сценариев чтения", true),
                new Opt("Всегда блокирует всю таблицу при любой записи", false),
                new Opt("Итераторы слабой согласованности не бросают ConcurrentModificationException", true),
                new Opt("Заменяет synchronized на каждый метод HashMap один в один", false));
        insertQuestion(
                t3,
                3,
                "Зачем `Comparable` vs `Comparator`?",
                "SINGLE_CHOICE",
                "MEDIUM",
                new Opt("Это одно и то же с разными именами", false),
                new Opt(
                        "Comparable — естественный порядок внутри класса; Comparator — внешняя стратегия сравнения",
                        true),
                new Opt("Comparator только для примитивов", false),
                new Opt("Comparable используется только в TreeSet", false));
        insertQuestion(
                t3,
                4,
                "Опасность `ConcurrentModificationException` при обходе `ArrayList`?",
                "SINGLE_CHOICE",
                "MEDIUM",
                new Opt("При удалении элементов во время итерации без итератора", true),
                new Opt("При любом чтении из другого потока", false),
                new Opt("Только если список пуст", false),
                new Opt("Не бывает в Java", false));
        insertQuestion(
                t3,
                5,
                "Wildcard `List<? extends Number>` позволяет:",
                "MULTIPLE_CHOICE",
                "HARD",
                new Opt("Читать элементы как Number", true),
                new Opt("Добавлять произвольные Number без ограничений", false),
                new Opt("Присвоить List<Integer> переменной этого типа", true),
                new Opt("Забыть про стирание типов", false));
        insertQuestion(
                t3,
                6,
                "Когда предпочесть `ArrayDeque` очереди на базе `LinkedList`?",
                "SINGLE_CHOICE",
                "HARD",
                new Opt("Никогда — LinkedList всегда быстрее", false),
                new Opt("ArrayDeque обычно эффективнее как deque без лишних узлов", true),
                new Opt("Только если нужны null-элементы в очереди", false),
                new Opt("Только для стеков в однопоточном коде", false));
        insertQuestion(
                t3,
                7,
                "Stream API: промежуточная vs терминальная операция?",
                "SINGLE_CHOICE",
                "MEDIUM",
                new Opt("filter — терминальная", false),
                new Opt("collect — терминальная", true),
                new Opt("map запускает вычисление сразу", false),
                new Opt("forEach всегда промежуточная", false));
        insertQuestion(
                t3,
                8,
                "Какие коллекции основаны на хэш-таблице?",
                "MULTIPLE_CHOICE",
                "MEDIUM",
                new Opt("HashMap", true),
                new Opt("HashSet", true),
                new Opt("TreeMap", false),
                new Opt("LinkedHashMap", true));
        insertQuestion(
                t3,
                9,
                "Почему важно переопределить `equals`/`hashCode` для ключей в HashMap?",
                "SINGLE_CHOICE",
                "HARD",
                new Opt("Чтобы ключи сортировались автоматически", false),
                new Opt("Чтобы поиск и вставка по значению ключа работали предсказуемо", true),
                new Opt("Чтобы ускорить GC", false),
                new Opt("Это требование только для String", false));
        syncNumQuestions(t3);

        long t4 =
                insertTest(
                        bank,
                        "Архив: вариант весны 2025",
                        "Старый набор вопросов, оставлен для истории.",
                        "MEDIUM",
                        30,
                        0,
                        "ARCHIVED",
                        LocalDate.now().minusMonths(6));
        insertQuestion(
                t4,
                0,
                "Что такое classpath?",
                "SINGLE_CHOICE",
                "EASY",
                new Opt("Путь к исходникам .java", false),
                new Opt("Набор мест, где JVM ищет .class и ресурсы", true),
                new Opt("Только каталог JDK", false),
                new Opt("Переменная для Docker", false));
        insertQuestion(
                t4,
                1,
                "Модификатор `protected` даёт доступ:",
                "SINGLE_CHOICE",
                "MEDIUM",
                new Opt("Только внутри того же пакета", false),
                new Opt("В пакете и в подклассах", true),
                new Opt("Только в подклассах других пакетов", false),
                new Opt("Везде", false));
        syncNumQuestions(t4);
    }

    private void seedSql() {
        long bank =
                insertBank(
                        "PostgreSQL и SQL",
                        "Запросы, JOIN, индексы, транзакции — практика для бэкенд-разработчика.");
        long t1 =
                insertTest(
                        bank,
                        "SELECT и фильтрация",
                        "Основы реляционной алгебры в SQL.",
                        "EASY",
                        20,
                        5,
                        "PUBLISHED",
                        LocalDate.now().minusDays(20));
        insertQuestion(
                t1,
                0,
                "Какой оператор SQL используют для удаления строк из таблицы?",
                "SINGLE_CHOICE",
                "EASY",
                new Opt("REMOVE", false),
                new Opt("DELETE", true),
                new Opt("DROP", false),
                new Opt("TRUNCATE TABLE — то же что DELETE с WHERE", false));
        insertQuestion(
                t1,
                1,
                "Разница между WHERE и HAVING?",
                "SINGLE_CHOICE",
                "MEDIUM",
                new Opt("Нет разницы", false),
                new Opt("HAVING фильтрует группы после GROUP BY, WHERE — строки до группировки", true),
                new Opt("HAVING только для UPDATE", false),
                new Opt("WHERE запрещён с агрегатами", false));
        insertQuestion(
                t1,
                2,
                "Что делает `INNER JOIN`?",
                "SINGLE_CHOICE",
                "EASY",
                new Opt("Все строки левой таблицы", false),
                new Opt("Только пары с совпадающим ключом в обеих таблицах", true),
                new Opt("Только правую таблицу целиком", false),
                new Opt("Декартово произведение без условия", false));
        insertQuestion(
                t1,
                3,
                "Как избежать SQL-инъекции при динамических параметрах?",
                "SINGLE_CHOICE",
                "MEDIUM",
                new Opt("Экранировать кавычки вручную", false),
                new Opt("Использовать параметризованные запросы (prepared statements)", true),
                new Opt("Хранить пароли в открытом виде", false),
                new Opt("Отключить логирование", false));
        insertQuestion(
                t1,
                4,
                "NULL в SQL: что верно?",
                "MULTIPLE_CHOICE",
                "MEDIUM",
                new Opt("`WHERE col = NULL` почти никогда не сработает как ожидают — нужен IS NULL", true),
                new Opt("NULL означает «значение неизвестно»", true),
                new Opt("NULL == NULL в SQL даёт TRUE", false),
                new Opt("COUNT(*) считает строки с NULL в любом столбце", true));
        syncNumQuestions(t1);

        long t2 =
                insertTest(
                        bank,
                        "Индексы и план запроса",
                        "B-Tree, составные индексы, последствия для INSERT.",
                        "HARD",
                        45,
                        2,
                        "PUBLISHED",
                        LocalDate.now().minusDays(5));
        insertQuestion(
                t2,
                0,
                "Основная цель индекса B-Tree в PostgreSQL?",
                "SINGLE_CHOICE",
                "MEDIUM",
                new Opt("Увеличить размер таблицы", false),
                new Opt("Ускорить поиск и сортировку по индексируемым столбцам", true),
                new Opt("Заменить PRIMARY KEY", false),
                new Opt("Автоматически шифровать данные", false));
        insertQuestion(
                t2,
                1,
                "Составной индекс (a, b) эффективен для условий:",
                "MULTIPLE_CHOICE",
                "HARD",
                new Opt("WHERE a = ? AND b = ?", true),
                new Opt("WHERE b = ? без условия на a", false),
                new Opt("WHERE a = ?", true),
                new Opt("ORDER BY b, a при равенстве по a", false));
        insertQuestion(
                t2,
                2,
                "Что такое MVCC в PostgreSQL в двух словах?",
                "SINGLE_CHOICE",
                "HARD",
                new Opt("Модель, где читатели не блокируют писателей за счёт версий строк", true),
                new Opt("Только репликация на другой сервер", false),
                new Opt("Шифрование на уровне таблицы", false),
                new Opt("Кэширование в Redis", false));
        insertQuestion(
                t2,
                3,
                "Почему `SELECT *` может быть проблемой в продакшене?",
                "MULTIPLE_CHOICE",
                "MEDIUM",
                new Opt("Тянет лишние столбцы и нагружает I/O", true),
                new Opt("Ломает покрытие индексом (index-only scan)", true),
                new Opt("Всегда запрещён стандартом SQL", false),
                new Opt("Ускоряет любой запрос", false));
        insertQuestion(
                t2,
                4,
                "Уровень изоляции READ COMMITTED в PostgreSQL означает:",
                "SINGLE_CHOICE",
                "HARD",
                new Opt("Видны только закоммиченные данные на момент каждого оператора", true),
                new Opt("Полное отсутствие фантомов", false),
                new Opt("Снимок на всю транзакцию как в REPEATABLE READ", false),
                new Opt("Блокировка всей БД", false));
        insertQuestion(
                t2,
                5,
                "EXPLAIN ANALYZE показывает:",
                "SINGLE_CHOICE",
                "MEDIUM",
                new Opt("Только синтаксис", false),
                new Opt("План и фактические времена/строки выполнения", true),
                new Opt("Список пользователей", false),
                new Opt("Размер WAL", false));
        insertQuestion(
                t2,
                6,
                "Что верно про ограничение FOREIGN KEY?",
                "MULTIPLE_CHOICE",
                "MEDIUM",
                new Opt("Поддерживает ссылочную целостность", true),
                new Opt("Может заменить индекс на родительской таблице полностью", false),
                new Opt("ON DELETE CASCADE удаляет зависимые строки", true),
                new Opt("Работает только в MySQL", false));
        syncNumQuestions(t2);

        long t3 =
                insertTest(
                        bank,
                        "Черновик: оконные функции",
                        "ROW_NUMBER, RANK, LAG — в разработке.",
                        "HARD",
                        40,
                        1,
                        "DRAFT",
                        null);
        insertQuestion(
                t3,
                0,
                "Что делает `ROW_NUMBER() OVER (PARTITION BY dept ORDER BY salary DESC)`?",
                "SINGLE_CHOICE",
                "HARD",
                new Opt("Суммирует зарплаты", false),
                new Opt("Нумерует строки внутри каждой группы dept по убыванию salary", true),
                new Opt("Удаляет дубликаты", false),
                new Opt("Создаёт индекс", false));
        insertQuestion(
                t3,
                1,
                "Отличие RANK от DENSE_RANK?",
                "SINGLE_CHOICE",
                "HARD",
                new Opt("Нет отличий", false),
                new Opt("RANK оставляет пропуски при равных значениях, DENSE_RANK — нет", true),
                new Opt("DENSE_RANK только для строк", false),
                new Opt("RANK работает без ORDER BY", false));
        syncNumQuestions(t3);
    }

    private void seedAlgorithms() {
        long bank =
                insertBank(
                        "Алгоритмы и структуры данных",
                        "Асимптотика, графы, сортировки, хэширование.");
        long t1 =
                insertTest(
                        bank,
                        "Асимптотика и базовые структуры",
                        "O-нотация, стек, очередь, список.",
                        "EASY",
                        25,
                        4,
                        "PUBLISHED",
                        LocalDate.now().minusDays(30));
        insertQuestion(
                t1,
                0,
                "Средняя временная сложность поиска в сбалансированном BST из n элементов?",
                "SINGLE_CHOICE",
                "MEDIUM",
                new Opt("O(1)", false),
                new Opt("O(log n)", true),
                new Opt("O(n)", false),
                new Opt("O(n log n)", false));
        insertQuestion(
                t1,
                1,
                "Стек — принцип доступа?",
                "SINGLE_CHOICE",
                "EASY",
                new Opt("FIFO", false),
                new Opt("LIFO", true),
                new Opt("Случайный", false),
                new Opt("По приоритету", false));
        insertQuestion(
                t1,
                2,
                "Худший случай quicksort при плохом выборе опорного?",
                "SINGLE_CHOICE",
                "HARD",
                new Opt("O(n log n)", false),
                new Opt("O(n²)", true),
                new Opt("O(n)", false),
                new Opt("O(1)", false));
        insertQuestion(
                t1,
                3,
                "Для чего хэш-таблица в среднем даёт O(1) вставку/поиск?",
                "SINGLE_CHOICE",
                "MEDIUM",
                new Opt("Магия компилятора", false),
                new Opt("Функция хэша + массив корзин с разрешением коллизий", true),
                new Opt("Сортировка ключей", false),
                new Opt("Бинарный поиск по файлу", false));
        insertQuestion(
                t1,
                4,
                "Обход графа в ширину (BFS) обычно использует:",
                "SINGLE_CHOICE",
                "MEDIUM",
                new Opt("Стек", false),
                new Opt("Очередь", true),
                new Opt("Кучу", false),
                new Opt("Union-Find", false));
        syncNumQuestions(t1);

        long t2 =
                insertTest(
                        bank,
                        "Графы: кратчайшие пути",
                        "Дейкстра, отрицательные рёбра, релаксация.",
                        "HARD",
                        50,
                        1,
                        "PUBLISHED",
                        LocalDate.now().minusDays(8));
        insertQuestion(
                t2,
                0,
                "Алгоритм Дейкстры применим когда?",
                "SINGLE_CHOICE",
                "HARD",
                new Opt("Есть рёбра с отрицательным весом", false),
                new Opt("Веса рёбер неотрицательны", true),
                new Opt("Только для невзвешенных графов", false),
                new Opt("Только для DAG", false));
        insertQuestion(
                t2,
                1,
                "Bellman-Ford vs Дейкстра?",
                "MULTIPLE_CHOICE",
                "HARD",
                new Opt("Bellman-Ford допускает отрицательные веса (с осторожностью к циклам)", true),
                new Opt("Дейкстра быстрее на разреженных графах с кучей", true),
                new Opt("Оба требуют неотрицательных весов", false),
                new Opt("Bellman-Ford всегда O(V)", false));
        insertQuestion(
                t2,
                2,
                "Что такое релаксация ребра (u,v) с весом w?",
                "SINGLE_CHOICE",
                "HARD",
                new Opt("Удаление ребра", false),
                new Opt("Попытка улучшить dist[v] через dist[u] + w", true),
                new Opt("Сортировка смежности", false),
                new Opt("Краска вершины", false));
        insertQuestion(
                t2,
                3,
                "Топологическая сортировка возможна для:",
                "SINGLE_CHOICE",
                "MEDIUM",
                new Opt("Любого графа", false),
                new Opt("Ориентированного ациклического графа (DAG)", true),
                new Opt("Только деревьев", false),
                new Opt("Полного графа с циклами", false));
        insertQuestion(
                t2,
                4,
                "Минимальное остовное дерево: классический жадный алгоритм на рёбрах?",
                "SINGLE_CHOICE",
                "MEDIUM",
                new Opt("Дейкстра", false),
                new Opt("Краскала или Прима", true),
                new Opt("Флойд-Уоршелл", false),
                new Opt("KMP", false));
        insertQuestion(
                t2,
                5,
                "Union-Find с path compression и union by rank даёт амортизированно:",
                "SINGLE_CHOICE",
                "HARD",
                new Opt("O(n) на операцию", false),
                new Opt("Почти константу (α(n))", true),
                new Opt("O(log n) только в худшем без оптимизаций", false),
                new Opt("O(n²)", false));
        insertQuestion(
                t2,
                6,
                "Задача о рюкзаке 0/1 динамическим программированием по времени?",
                "SINGLE_CHOICE",
                "HARD",
                new Opt("O(n)", false),
                new Opt("O(nW) где W — вместимость", true),
                new Opt("O(n²)", false),
                new Opt("O(2^n) всегда лучше DP", false));
        syncNumQuestions(t2);

        long t3 =
                insertTest(
                        bank,
                        "Сортировки и устойчивость",
                        "Сравнение merge sort, quicksort, counting sort.",
                        "MEDIUM",
                        35,
                        2,
                        "DRAFT",
                        null);
        insertQuestion(
                t3,
                0,
                "Какая из перечисленных сортировок устойчива по умолчанию (типичная реализация)?",
                "SINGLE_CHOICE",
                "MEDIUM",
                new Opt("Quicksort", false),
                new Opt("Merge sort", true),
                new Opt("Heap sort", false),
                new Opt("Selection sort", false));
        insertQuestion(
                t3,
                1,
                "Counting sort подходит когда:",
                "SINGLE_CHOICE",
                "HARD",
                new Opt("Ключи — вещественные числа произвольной точности", false),
                new Opt("Диапазон целочисленных ключей относительно невелик", true),
                new Opt("n = 10^9", false),
                new Opt("Нужна сортировка на месте O(1) памяти", false));
        insertQuestion(
                t3,
                2,
                "Нижняя граница для сортировок сравнением в худшем случае?",
                "SINGLE_CHOICE",
                "HARD",
                new Opt("O(n)", false),
                new Opt("Ω(n log n)", true),
                new Opt("O(n²)", false),
                new Opt("O(log n)", false));
        syncNumQuestions(t3);
    }

    private void seedHttpRest() {
        long bank =
                insertBank(
                        "HTTP и REST API",
                        "Методы, коды ответа, идемпотентность, кэш, безопасность.");
        long t1 =
                insertTest(
                        bank,
                        "HTTP 1.1: основы",
                        "Заголовки, тело, коды состояния.",
                        "EASY",
                        20,
                        5,
                        "PUBLISHED",
                        LocalDate.now().minusDays(15));
        insertQuestion(
                t1,
                0,
                "Какой код обычно означает «ресурс успешно создан» при POST?",
                "SINGLE_CHOICE",
                "EASY",
                new Opt("200", false),
                new Opt("201 Created", true),
                new Opt("204", false),
                new Opt("302", false));
        insertQuestion(
                t1,
                1,
                "Идемпотентные методы HTTP (в типичной семантике)?",
                "MULTIPLE_CHOICE",
                "MEDIUM",
                new Opt("GET", true),
                new Opt("PUT", true),
                new Opt("POST", false),
                new Opt("DELETE", true));
        insertQuestion(
                t1,
                2,
                "Что означает 401 vs 403?",
                "SINGLE_CHOICE",
                "MEDIUM",
                new Opt("Оба «не найдено»", false),
                new Opt("401 — не аутентифицирован; 403 — аутентифицирован, но нет прав", true),
                new Opt("403 — не залогинен", false),
                new Opt("401 — нет прав", false));
        insertQuestion(
                t1,
                3,
                "Зачем заголовок `Content-Type`?",
                "SINGLE_CHOICE",
                "EASY",
                new Opt("Указать кодировку URL", false),
                new Opt("Сообщить формат тела запроса/ответа", true),
                new Opt("Задать метод", false),
                new Opt("Включить сжатие", false));
        insertQuestion(
                t1,
                4,
                "REST: ресурс «заказ» — какой URI более RESTful для получения заказа 42?",
                "SINGLE_CHOICE",
                "MEDIUM",
                new Opt("/getOrder?id=42", false),
                new Opt("/orders/42", true),
                new Opt("/orders?action=get&orderId=42", false),
                new Opt("/order42", false));
        syncNumQuestions(t1);

        long t2 =
                insertTest(
                        bank,
                        "Безопасность API",
                        "JWT, CORS, rate limiting на концептуальном уровне.",
                        "HARD",
                        40,
                        2,
                        "PUBLISHED",
                        LocalDate.now().minusDays(2));
        insertQuestion(
                t2,
                0,
                "JWT access token обычно:",
                "SINGLE_CHOICE",
                "MEDIUM",
                new Opt("Хранится только в HttpOnly cookie — единственный вариант", false),
                new Opt("Короткоживущий; refresh — отдельным механизмом", true),
                new Opt("Не подписывается", false),
                new Opt("Содержит пароль пользователя", false));
        insertQuestion(
                t2,
                1,
                "CORS защищает прежде всего:",
                "SINGLE_CHOICE",
                "HARD",
                new Opt("Сервер от DDoS", false),
                new Opt("Браузерный код: ограничивает кросс-доменные запросы с JS", true),
                new Opt("Шифрование диска", false),
                new Opt("SQL-инъекции", false));
        insertQuestion(
                t2,
                2,
                "Rate limiting на API помогает от:",
                "MULTIPLE_CHOICE",
                "MEDIUM",
                new Opt("Перегрузки и простых DoS/брутфорса", true),
                new Opt("Всех логических багов в коде", false),
                new Opt("Неконтролируемого числа запросов от одного клиента", true),
                new Opt("Утечки памяти JVM", false));
        insertQuestion(
                t2,
                3,
                "Почему GET с телом — плохая идея для REST?",
                "SINGLE_CHOICE",
                "MEDIUM",
                new Opt("Запрещено RFC и ломает кэши/прокси", false),
                new Opt("Семантика GET — безопасный и идемпотентный запрос без побочных эффектов; тело игнорируется многими клиентами", true),
                new Opt("GET быстрее POST", false),
                new Opt("Нельзя передать query string", false));
        insertQuestion(
                t2,
                4,
                "HATEOAS в REST — это про:",
                "SINGLE_CHOICE",
                "HARD",
                new Opt("Только JSON", false),
                new Opt("Гипермедиа-ссылки на следующие допустимые действия в ответе", true),
                new Opt("Версионирование через заголовки", false),
                new Opt("GraphQL", false));
        insertQuestion(
                t2,
                5,
                "OAuth2: роль refresh token?",
                "SINGLE_CHOICE",
                "HARD",
                new Opt("Заменяет пароль в каждом запросе навсегда", false),
                new Opt("Получение новых access token без повторного логина пользователя", true),
                new Opt("Шифрует трафик TLS", false),
                new Opt("Хранится только в localStorage — best practice всегда", false));
        syncNumQuestions(t2);

        long t3 =
                insertTest(
                        bank,
                        "Черновик: версионирование API",
                        "Пути /v1, заголовки Accept.",
                        "MEDIUM",
                        25,
                        1,
                        "DRAFT",
                        null);
        insertQuestion(
                t3,
                0,
                "Плюс версии в URL `/api/v1/users`?",
                "SINGLE_CHOICE",
                "MEDIUM",
                new Opt("Явно для клиентов и кэшей", true),
                new Opt("Запрещено REST", false),
                new Opt("Убирает необходимость в тестах", false),
                new Opt("Автоматически шифрует ответ", false));
        syncNumQuestions(t3);
    }

    private void seedDiscreteMath() {
        long bank =
                insertBank(
                        "Дискретная математика",
                        "Логика, комбинаторика, графы — база для CS.");
        long t1 =
                insertTest(
                        bank,
                        "Логика и множества",
                        "Импликация, законы де Моргана.",
                        "EASY",
                        25,
                        5,
                        "PUBLISHED",
                        LocalDate.now().minusDays(40));
        insertQuestion(
                t1,
                0,
                "Отрицание выражения A ∧ B по де Моргану?",
                "SINGLE_CHOICE",
                "MEDIUM",
                new Opt("¬A ∧ ¬B", false),
                new Opt("¬A ∨ ¬B", true),
                new Opt("A ∨ B", false),
                new Opt("¬(A ∨ B)", false));
        insertQuestion(
                t1,
                1,
                "Когда импликация A → B ложна?",
                "SINGLE_CHOICE",
                "MEDIUM",
                new Opt("A истинно, B истинно", false),
                new Opt("A истинно, B ложно", true),
                new Opt("A ложно, B истинно", false),
                new Opt("A ложно, B ложно", false));
        insertQuestion(
                t1,
                2,
                "Мощность декартова произведения |A×B|?",
                "SINGLE_CHOICE",
                "EASY",
                new Opt("|A|+|B|", false),
                new Opt("|A|·|B|", true),
                new Opt("max(|A|,|B|)", false),
                new Opt("2^|A∪B|", false));
        insertQuestion(
                t1,
                3,
                "Сколько подмножеств у множества из n элементов?",
                "SINGLE_CHOICE",
                "MEDIUM",
                new Opt("n²", false),
                new Opt("2^n", true),
                new Opt("n!", false),
                new Opt("2n", false));
        insertQuestion(
                t1,
                4,
                "Контрапозиция к A → B?",
                "SINGLE_CHOICE",
                "HARD",
                new Opt("B → A", false),
                new Opt("¬B → ¬A", true),
                new Opt("¬A → ¬B", false),
                new Opt("A ∧ ¬B", false));
        syncNumQuestions(t1);

        long t2 =
                insertTest(
                        bank,
                        "Комбинаторика",
                        "Перестановки, сочетания, размещения.",
                        "MEDIUM",
                        35,
                        2,
                        "PUBLISHED",
                        LocalDate.now().minusDays(6));
        insertQuestion(
                t2,
                0,
                "Число перестановок n различных элементов?",
                "SINGLE_CHOICE",
                "EASY",
                new Opt("n²", false),
                new Opt("n!", true),
                new Opt("2^n", false),
                new Opt("C(n,2)", false));
        insertQuestion(
                t2,
                1,
                "Число сочетаний из n по k (без повторений)?",
                "SINGLE_CHOICE",
                "MEDIUM",
                new Opt("n!/k!", false),
                new Opt("n!/(k!(n-k)!)", true),
                new Opt("n^k", false),
                new Opt("P(n,k) = n!/(n-k)!", false));
        insertQuestion(
                t2,
                2,
                "Принцип Дирихле: если k+1 кролик в k клетках, то",
                "SINGLE_CHOICE",
                "MEDIUM",
                new Opt("все клетки пусты", false),
                new Opt("хотя бы одна клетка содержит ≥ 2 кроликов", true),
                new Opt("кролики — граф", false),
                new Opt("k = 0", false));
        insertQuestion(
                t2,
                3,
                "Сколько бит нужно для кодирования N различных состояний в двоичном коде (⌈log2 N⌉)?",
                "SINGLE_CHOICE",
                "HARD",
                new Opt("N", false),
                new Opt("⌈log2 N⌉", true),
                new Opt("log10 N", false),
                new Opt("2^N", false));
        insertQuestion(
                t2,
                4,
                "Число рёбер в полном неориентированном графе Kn?",
                "SINGLE_CHOICE",
                "HARD",
                new Opt("n", false),
                new Opt("n(n-1)/2", true),
                new Opt("n(n-1)", false),
                new Opt("2^n", false));
        insertQuestion(
                t2,
                5,
                "Оценка O для суммы 1+2+...+n?",
                "SINGLE_CHOICE",
                "MEDIUM",
                new Opt("O(n²)", false),
                new Opt("O(n)", true),
                new Opt("O(log n)", false),
                new Opt("O(1)", false));
        insertQuestion(
                t2,
                6,
                "Гамильтонов цикл vs эйлеров цикл?",
                "MULTIPLE_CHOICE",
                "HARD",
                new Opt("Эйлеров — каждое ребро ровно один раз", true),
                new Opt("Гамильтонов — каждая вершина ровно один раз", true),
                new Opt("Всегда существуют в одном и том же графе", false),
                new Opt("Оба NP-полные в общем случае поиска", false));
        syncNumQuestions(t2);

        long t3 =
                insertTest(
                        bank,
                        "Архив: вступительный тест",
                        "Устаревший набор.",
                        "EASY",
                        15,
                        0,
                        "ARCHIVED",
                        LocalDate.now().minusMonths(10));
        insertQuestion(
                t3,
                0,
                "Чётное число делится на 2 без остатка. Верно?",
                "SINGLE_CHOICE",
                "EASY",
                new Opt("Да", true),
                new Opt("Нет", false),
                new Opt("Только для простых", false),
                new Opt("Зависит от системы счисления", false));
        syncNumQuestions(t3);
    }

    private void seedBulkValidationData() {
        int banks = Math.max(1, bulkBanks);
        int testsPerBank = Math.max(1, bulkTestsPerBank);
        int questionsPerTest = Math.max(1, bulkQuestionsPerTest);

        log.info(
                "Bulk demo seed started: banks={}, testsPerBank={}, questionsPerTest={}, owner_id={}",
                banks,
                testsPerBank,
                questionsPerTest,
                ownerId);

        String[] diffs = {"EASY", "MEDIUM", "HARD"};
        String[] statuses = {"DRAFT", "PUBLISHED", "ARCHIVED"};

        for (int b = 1; b <= banks; b++) {
            long bankId =
                    insertBank(
                            "Bulk Bank #" + b,
                            "Large validation dataset for filters/sorting/pagination and CRUD checks.");

            for (int t = 1; t <= testsPerBank; t++) {
                String diff = diffs[(t - 1) % diffs.length];
                String status = statuses[(t - 1) % statuses.length];
                LocalDate published = "PUBLISHED".equals(status) ? LocalDate.now().minusDays((b + t) % 30) : null;

                long testId =
                        insertTest(
                                bankId,
                                "Bulk Test #" + b + "-" + t,
                                "Auto-generated for validation scenarios",
                                diff,
                                10 + (t % 8) * 5,
                                (t % 4) + 1,
                                status,
                                published);

                for (int q = 1; q <= questionsPerTest; q++) {
                    String qType = (q % 3 == 0) ? "OPEN" : (q % 2 == 0 ? "MULTIPLE_CHOICE" : "SINGLE_CHOICE");
                    String qDiff = diffs[(q - 1) % diffs.length];
                    insertQuestion(
                            testId,
                            q - 1,
                            "Bulk question " + b + "-" + t + "-" + q + "?",
                            qType,
                            qDiff,
                            new Opt("Option A", true),
                            new Opt("Option B", false),
                            new Opt("Option C", false),
                            new Opt("Option D", false));
                }

                syncNumQuestions(testId);
            }
        }

        log.info("Bulk demo seed completed for owner_id={}", ownerId);
    }
}
