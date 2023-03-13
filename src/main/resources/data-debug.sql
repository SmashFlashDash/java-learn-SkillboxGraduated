INSERT INTO site (last_error, name, status, status_time, url)
VALUES ("Индексация остановлена пользователем", "bequiet.com", "FAILED", "2023-03-08 19:03:10.051000",
        "https://www.bequiet.com/ru");

INSERT INTO page (code, content, path, site_id)
VALUES (404, "content this 1", "url this 1", 1),
       (505, "content this 11", "url this 11", 1),
       (1001, "content this 111", "url this 111", 1);

INSERT INTO lemma (frequency, lemma, site_id)
VALUES (1, "word 1", 1),
       (2, "word 11", 1),
       (1, "word 111", 1);

INSERT INTO `index` (lemma_id, page_id, `rank`)
VALUES (1, 1, 1),
       (2, 1, 1),
       (3, 2, 2);