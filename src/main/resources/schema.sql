-- ALTER TABLE page ADD UNIQUE KEY path_index (path(100));
ALTER TABLE page ADD CONSTRAINT UK_path_index unique (path(100));

--$ mysql -u USERNAME -p set global log_bin_trust_function_creators=1;

-- не получается создать trigger, можно использовать @SQLCongin @SQL
-- CREATE TRIGGER my_insert_trigger AFTER INSERT ON `index` FOR EACH ROW
-- BEGIN
--     UPDATE lemma SET frequency = frequency + 1 WHERE lemma_id = NEW.lemma_id;
-- END ^;

-- DROP TRIGGER IF EXISTS index_insert_trigger;
-- DELIMITER |
-- CREATE DEFINER=root@localhost TRIGGER my_insert_trigger AFTER INSERT ON `index` FOR EACH ROW
--     UPDATE lemma SET frequency = frequency + 1 WHERE lemma_id = NEW.lemma_id;
-- END$$
-- DELIMITER ;

-- DROP TRIGGER IF EXISTS index_delete_trigger
-- CREATE DEFINER=root@localhost TRIGGER my_insert_trigger
--     AFTER DELETE ON `index` FOR EACH ROW
-- BEGIN
--     IF (SELECT frequency FROM lemma WHERE lemma_id = NEW.lemma_id > 1) THEN
--         UPDATE lemma SET frequency = frequency - 1 WHERE lemma_id = NEW.lemma_id;
--     ELSE THEN
--         DELETE FROM lemma WHERE lemma_id = NEW.lemma_id;
--     ENDIF;
-- END;
