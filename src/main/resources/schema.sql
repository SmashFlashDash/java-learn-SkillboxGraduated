-- ALTER TABLE page ADD UNIQUE KEY path_index (path(100));
ALTER TABLE page ADD CONSTRAINT UK_path_index unique (path(100));

# $ mysql -u USERNAME -p set global log_bin_trust_function_creators=1;

#  не получается создать trigger, можно использовать @SQLCongin @SQL или HibernateInterceptor,
#  или добавить свзь между page и lemmas через таблицу index
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
