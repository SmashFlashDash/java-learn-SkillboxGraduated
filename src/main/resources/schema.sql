# ALTER TABLE page ADD UNIQUE KEY path_index (path(100));
ALTER TABLE page ADD CONSTRAINT UK_path_index unique (path(100))