# 📂 grafCloud

## 📑 Содержание

- [📝 О проекте](#о-проекте)
- [⚙️ Функционал сервиса](#функционал-сервиса)
- [🛠️ Технологии](#технологии)
- [🚀 Демонстрация](#демонстрация)
- [💻 Локальный запуск приложения](#локальный-запуск-приложения)
    - [Требования](#требования)
    - [Установка](#установка)
- [🔧 Конфигурация](#конфигурация)
- [👨‍💻 Автор](#автор)
- [📋 О разработке](#о-разработке)

## 📝 О проекте

grafCloud - это новое web-приложение для хранения файлов в облаке.
Когда OneDrive покинет российский рынок, а платить за подписку на Яндекс диск вам покажется невыгодно,
предлагаю перейти на grafCloud.
По своему функционалу сервис полностью (или не очень) повторяет возможности таких гигантов как YandexDisk, Mail и
OneDrive.

**Для знакомства просто нажмите сюда:** [grafCloud.com](http://188.120.242.155:8080/)

## ⚙️ Функционал сервиса

### 👤 Работа с пользователем:

- Регистрация пользователей
- Авторизация в системе
- Хранение сессий
- Email уведомления при регистрации

### 📤 Работа с файлами и папками:

- Загрузка файлов и папок
- Создание новых директорий
- Перемещение данных
- Скачивание данных

### 🔍 Поиск и навигация:

- Поиск файлов и папок по имени
- Навигация между папками
- Просмотр содержимого папок

### 🗑️ Управление данными:

- Корзина для удаленных файлов
- Автоматическая очистка корзины
- Возможность восстановления файлов

### 💾 Организация хранилища:

- Изолированное хранение данных пользователя
- Доступ к содержимому хранилища по текущей сессии
- Сохранение структуры вложенности папок

## 🛠️ Технологии

  <div style="display: flex; align-items: center; gap: 10px; margin-bottom: 5px;">
  <img alt="java.icon" src="https://skillicons.dev/icons?i=java"/>
  <span>Java 21</span>
</div>
<div style="display: flex; align-items: center; gap: 10px; margin-bottom: 5px;">
    <img alt="spring.icon" src="https://skillicons.dev/icons?i=spring"/>
    Spring Boot 3.3
</div>
<div style="display: flex; align-items: center; gap: 10px; margin-bottom: 5px;">
  <img alt="spring.icon" src="https://skillicons.dev/icons?i=spring"/>
  Spring Security
</div>
<div style="display: flex; align-items: center; gap: 10px; margin-bottom: 5px;">
  <img alt="spring.icon" src="https://skillicons.dev/icons?i=spring"/>
  Spring Data JPA
</div>
  <div style="display: flex; align-items: center; gap: 10px; margin-bottom: 5px;">
    <img alt="mysql.icon" src="https://skillicons.dev/icons?i=mysql"/>
    MySQL 9.0
  </div>
<div style="display: flex; align-items: center; gap: 10px; margin-bottom: 5px;">
  <img alt="liquibase.icon" src="https://icon.icepanel.io/Technology/svg/Liquibase.svg" 
    width="48" height="42" style="background-color: #242938; border-radius: 10px; padding: 2px;"/>
  Liquibase
</div>
  <div style="display: flex; align-items: center; gap: 10px; margin-bottom: 5px;">
    <img alt="redis.icon" src="https://skillicons.dev/icons?i=redis"/>
    Redis
  </div>
  <div style="display: flex; align-items: center; gap: 10px; margin-bottom: 5px;">
    <img alt="docker.icon" src="https://skillicons.dev/icons?i=docker"/>
    Docker
  </div>
<div style="display: flex; align-items: center; gap: 10px; margin-bottom: 5px;">
  <img alt="minio.icon" src="https://www.vectorlogo.zone/logos/minioio/minioio-icon.svg" 
    width="48" height="43" style="background-color: #242938; border-radius: 10px; padding: 2px;"/>
  MinIO
</div>

## 🚀 Демонстрация

- **Рабочая версия:** [http://188.120.242.155:8080/](http://188.120.242.155:8080/)

Для ознакомления с приложением вы можете:

- Зарегистрировать собственный аккаунт
- Использовать тестовый аккаунт:
  ```
  Логин: User
  Пароль: 12345
  ```

## 💻 Локальный запуск приложения

### 📌 Требования

- Docker и Docker Compose
- JDK 21
- Maven

### 📦 Установка

```bash
# Клонируйте репозиторий
git clone [URL репозитория]

# Создайте файл .env на основе .env.example
nano .env

# Запустите через Docker Compose
docker-compose up -d
```

## 🔧 Конфигурация

Необходимые параметры для `.env`:

```properties
# Основные настройки
PORT=your_port
APPLICATION_URL=your_url
APPLICATION_DATE_TIME_FORMAT_PATTERN=yyyy-MM-dd HH:mm:ss
TRASH_RETENTION_DAYS=your_count
# База данных
DB_HOST=db
DB_NAME=cloud_files_storage
DB_USERNAME=your_username
DB_PASSWORD=your_password
DB_SCHEMA=cloud_files_storage
# Redis
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=your_redis_password
# MinIO
MINIO_URL=http://minio:9000
MINIO_ACCESS_KEY=your_minio_access_key
MINIO_SECRET_KEY=your_minio_secret_key
MINIO_BUCKET=cloud-files-storage
# Почтовый сервер
SPRING_MAIL_HOST=your_mail_host
SPRING_MAIL_PORT=your_mail_port
SPRING_MAIL_USERNAME=your_mail_username
SPRING_MAIL_PASSWORD=your_mail_password
```

## 👨‍💻 Автор

Связаться со мной можно в Telegram: [grafkust](https://t.me/grafkust)

## 📋 О разработке <a name="о-разработке"></a>

Проект разработан по техническому заданию из
курса [Java Backend Learning Course](https://zhukovsd.github.io/java-backend-learning-course/projects/cloud-file-storage/)
