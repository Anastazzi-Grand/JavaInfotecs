# Простой сервис-хранилище "ключ-значение" на Java

Это простой сервис, написанный на Java, который реализует хранилище "ключ-значение" в оперативной памяти. Основные возможности:

- Чтение, запись и удаление данных по ключу
- Автоматическое удаление данных по истечении настраиваемого времени жизни (TTL)
- Сохранение и загрузка текущего состояния хранилища

## Технологии

- Java 19
- Spring Boot
- Maven
- ConcurrentHashMap для хранения данных
- Scheduled Executor для автоматического удаления данных

## Возможности

1. Операция чтения (get):
   - Принимает ключ для хранилища.
   - Возвращает данные, хранящиеся по переданному ключу, или метку отсутствия данных.

2. Операция записи (set):
   - Принимает ключ, данные для хранения и опциональное время жизни (TTL).
   - Если по ключу уже хранятся данные, они заменяются, а также обновляется TTL.
   - Возвращает метку успешности или неуспешности операции.

3. Операция удаления (remove):
   - Принимает ключ для хранилища.
   - Удаляет данные, хранящиеся по переданному ключу.
   - Возвращает данные, хранившиеся по ключу, или метку отсутствия данных.

4. Операция сохранения текущего состояния (dump):
   - Сохраняет текущее состояние хранилища и возвращает его в виде загружаемого файла.

5. Операция загрузки состояния хранилища (load):
   - Загружает состояние хранилища из файла, созданного операцией dump.
  
6. Получение всех элементов хранилища:
   - Возвращает копию всех элементов, хранящихся в хранилище.

  ## Требования

Для запуска данного проекта на вашем компьютере необходимо установить следующее:

1. Java Development Kit (JDK) версии 19:
   - Проект использует Java 19, поэтому необходимо иметь установленный JDK 19 или более новой версии.
   - Скачать JDK 19 можно с официального сайта Oracle: [https://www.oracle.com/java/technologies/downloads/#java19](https://www.oracle.com/java/technologies/downloads/#java19)

2. Maven:
   - Проект использует Maven для сборки и управления зависимостями.
   - Maven можно скачать с официального сайта: [https://maven.apache.org/download.cgi](https://maven.apache.org/download.cgi)
   - После установки Maven убедитесь, что переменная окружения MAVEN_HOME указывает на папку, куда был установлен Maven, а PATH включает путь к bin-директории Maven.

## Использование

1. Скачайте zip-архив проекта.
2. Распакуйте архив в директорию по вашему выбору.
3. Перейдите в директорию проекта: cd JavaInfotecs
4. Соберите и запустите приложение: mvn spring-boot:run
5. Используйте API, предоставляемое сервисом, для взаимодействия с хранилищем.

## Тестирование

В проекте реализованы Unit-тесты для проверки основных функциональных возможностей сервиса. Для запуска тестов выполните: mvn test

