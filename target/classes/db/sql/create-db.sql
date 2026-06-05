CREATE TABLE IF NOT EXISTS flights (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(255)    NOT NULL,
    description VARCHAR(500),
    price       DECIMAL(19, 4)  NOT NULL
);

CREATE TABLE IF NOT EXISTS seats (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    seat_number VARCHAR(10)     NOT NULL,
    price       DECIMAL(19, 4)  NOT NULL,
    is_sold     BOOLEAN         NOT NULL DEFAULT FALSE,
    version     BIGINT          NOT NULL DEFAULT 0,
    flight_id   BIGINT          NOT NULL,
    CONSTRAINT fk_seat_flight FOREIGN KEY (flight_id) REFERENCES flights(id)
);

CREATE TABLE IF NOT EXISTS payment (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    price         DECIMAL(30, 8)  NOT NULL,
    bank_response VARCHAR(255)
);