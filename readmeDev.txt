# создать контенйер бд с пользователем и паролем и кодировкой
    docker run --name mysql -p 3306:3306
    -e LANG=C.UTF-8
    --env MYSQL_DATABASE=search_engine --env MYSQL_USER=user --env MYSQL_PASSWORD=pass --env MYSQL_ROOT_PASSWORD=root
    mysql --character-set-server utf8mb4

# настройки БД
SHOW STATUS;

-------второй варинат создать бд----------
# создать контейнер с бд
docker run --name mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=root -d mysql/mysql-server:5.7
еще кодировку надо было в utf8 или уже в бд
$ show variables like 'char%';      - посмотреть кодировки
$ set character_set_client='utf8';  - изменить кодировку
# cоздать пользователя для подключения
docker exec -it mysql mysql -h localhost -u root -p
или docker exec -it mysql bash и потом написать mysql -u root -p
CREATE USER 'rootDocker' IDENTIFIED BY 'java';
grant all on *.* to 'demo_java'@'%' identified by 'root';	- root это пароль
FLUSH PRIVILEGES;
после этого подключаться не root пользователем а 'rootDocker'

------- настройка idea-------
включить в IDE Build-AnnotationPocessor-enable
установить в IDE lombock-plugin чтобы видел методы сгенрированные аннотациями

-------подключить лемматизатор--------------
в pom.xml добавим проект
<repositories>
    <repository>
        <id>skillbox-gitlab</id>
        <url>https://gitlab.skillbox.ru/api/v4/projects/263574/packages/maven</url>
    </repository>
</repositories>
Теперь вам необходимо указать токен
Maven-репозиторию, поскольку GitLab запрещает публичный доступ к
библиотекам. Для указания токена найдите или создайте файл settings.xml.
● В Windows он располагается в директории
C:/Users/<Имя вашего пользователя>/.m2
● В Linux — в директории
/home/<Имя вашего пользователя>/.m2
● В macOs — по адресу
/Users/<Имя вашего пользователя>/.m2
для доступа к данному

Вставьте в него код
    <servers>
        <server>
            <id>skillbox-gitlab</id>
            <configuration>
                <httpHeaders>
                    <property>
                        <name>Private-Token</name>
                        <value>wtb5axJDFX9Vm_W1Lexg</value>
                    </property>
                </httpHeaders>
            </configuration>
        </server>
    </servers>
</settings>

В блоке <value> находится уникальный токен доступа. Если у вас возникнет
«401 Ошибка Авторизации» при попытке получения зависимостей, возьмите
актуальный токен доступа из документа по ссылке
Обязательно почистите кэш maven. Самый надёжный способ — удалить
директорию: .m2
Затем обновите зависимости в проекте при помощи
Если не обновляется зависимости в idea, заново вставить зависимости в pom.xml