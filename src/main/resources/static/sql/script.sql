use telecom;

CREATE TABLE user (
    id INT PRIMARY KEY AUTO_INCREMENT,
    login VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role_type VARCHAR(255) NOT NULL,
    is_banned BOOLEAN NOT NULL
);

CREATE TABLE tariffplan (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    type VARCHAR(255) NOT NULL,
    monthly_payment DECIMAL(5,2) NOT NULL,
    description VARCHAR(250) NOT NULL
);

CREATE TABLE subscriber (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL UNIQUE,
    CONSTRAINT fk_subscriber_user FOREIGN KEY (user_id) REFERENCES user(id)
);

CREATE TABLE persondata (
    user_id INT PRIMARY KEY,
    full_name VARCHAR(50) NOT NULL,
    phone VARCHAR(13) NOT NULL UNIQUE,
    email VARCHAR(30) NOT NULL UNIQUE,
    address VARCHAR(50) NOT NULL,
    birth_date DATE NOT NULL,
    CONSTRAINT fk_persondata_user FOREIGN KEY (user_id) REFERENCES user(id)
);

CREATE TABLE connectedservices (
    id INT PRIMARY KEY AUTO_INCREMENT,
    start_date DATE NOT NULL,
    next_billing_date DATE,
    status VARCHAR(255) NOT NULL,
    subscriber_id INT NOT NULL,
    tariff_plan_id INT NOT NULL,
    CONSTRAINT fk_connected_subscriber FOREIGN KEY (subscriber_id) REFERENCES subscriber(id),
    CONSTRAINT fk_connected_tariff FOREIGN KEY (tariff_plan_id) REFERENCES tariffplan(id)
);

CREATE TABLE billingaccount (
    subscriber_id INT PRIMARY KEY,
    number VARCHAR(50) NOT NULL UNIQUE,
    balance DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_billing_subscriber FOREIGN KEY (subscriber_id) REFERENCES subscriber(id)
);