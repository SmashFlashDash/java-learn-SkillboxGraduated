ALTER TABLE page ADD CONSTRAINT UK_path_index unique (path(100));

# DROP TRIGGER IF EXISTS delete_index_trigger;
# CREATE TRIGGER delete_index_trigger
#     BEFORE DELETE ON `index` FOR EACH ROW
# BEGIN
#     DECLARE tmp_frequency integer;
#     SET tmp_frequency := (SELECT frequency FROM lemma WHERE id = OLD.lemma_id) - 1;
#     IF tmp_frequency > 0 THEN
#         UPDATE lemma SET frequency = tmp_frequency WHERE lemma_id = OLD.lemma_id;
#     ELSE
#         DELETE FROM lemma WHERE lemma_id = OLD.lemma_id;
#     END IF;
# END ^;


# CREATE TABLE `site` (
#                         `id` bigint NOT NULL AUTO_INCREMENT,
#                         `last_error` text,
#                         `name` varchar(255) NOT NULL,
#                         `status` varchar(255) NOT NULL,
#                         `status_time` datetime(6) NOT NULL,
#                         `url` varchar(255) NOT NULL,
#                         PRIMARY KEY (`id`)
# ) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
#
# CREATE TABLE `page` (
#                         `id` bigint NOT NULL AUTO_INCREMENT,
#                         `code` int NOT NULL,
#                         `content` mediumtext NOT NULL,
#                         `path` text NOT NULL,
#                         `site_id` bigint NOT NULL,
#                         PRIMARY KEY (`id`),
#                         UNIQUE KEY `UK_path_index` (`path`(100)),
#                         KEY `FKj2jx0gqa4h7wg8ls0k3y221h2` (`site_id`),
#                         CONSTRAINT `FKj2jx0gqa4h7wg8ls0k3y221h2` FOREIGN KEY (`site_id`) REFERENCES `site` (`id`)
# ) ENGINE=InnoDB AUTO_INCREMENT=55 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
#
# CREATE TABLE `lemma` (
#                          `id` bigint NOT NULL AUTO_INCREMENT,
#                          `frequency` int NOT NULL,
#                          `lemma` varchar(255) NOT NULL,
#                          `site_id` bigint NOT NULL,
#                          PRIMARY KEY (`id`),
#                          UNIQUE KEY `UK7ogalxpu2t6pogbj8sbbpk0of` (`site_id`,`lemma`),
#                          CONSTRAINT `FKfbq251d28jauqlxirb1k2cjag` FOREIGN KEY (`site_id`) REFERENCES `site` (`id`)
# ) ENGINE=InnoDB AUTO_INCREMENT=3722 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
#
# CREATE TABLE `index` (
#                          `id` bigint NOT NULL AUTO_INCREMENT,
#                          `rank` float NOT NULL,
#                          `lemma_id` bigint NOT NULL,
#                          `page_id` bigint NOT NULL,
#                          PRIMARY KEY (`id`),
#                          UNIQUE KEY `UKnsmx98a4vtc4f059p78lhucwr` (`page_id`,`lemma_id`),
#                          KEY `FKiqgm34dkvjdt7kobg71xlbr33` (`lemma_id`),
#                          CONSTRAINT `FK3uxy5s82mxfodai0iafb232cs` FOREIGN KEY (`page_id`) REFERENCES `page` (`id`),
#                          CONSTRAINT `FKiqgm34dkvjdt7kobg71xlbr33` FOREIGN KEY (`lemma_id`) REFERENCES `lemma` (`id`)
# ) ENGINE=InnoDB AUTO_INCREMENT=9149 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;