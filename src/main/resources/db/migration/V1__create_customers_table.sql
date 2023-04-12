CREATE TABLE "customer"
(
    "id"         SERIAL,
    "first_name" VARCHAR NOT NULL,
    "last_name"  VARCHAR NOT NULL
);

INSERT INTO "customer" (first_name, last_name)
VALUES  ('jane', 'doe'),
        ('john', 'doe');