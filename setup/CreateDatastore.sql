CREATE DATABASE if not exists onlinebookstore;

USE onlinebookstore;

CREATE TABLE if not exists books 
  ( 
     barcode   VARCHAR(100) PRIMARY KEY, 
     name      TEXT NOT NULL, 
     author    VARCHAR(100) NOT NULL, 
     price     INT, 
     quantity  REAL
  ); 
  
  CREATE TABLE if not exists users
  ( 
     username  VARCHAR(100) PRIMARY KEY, 
     password  VARCHAR(100) NOT NULL, 
     firstname VARCHAR(100) NOT NULL, 
     lastname  VARCHAR(100) NOT NULL, 
     address   TEXT NOT NULL, 
     phone     VARCHAR(100) NOT NULL, 
     mailid    VARCHAR(100) NOT NULL,
     usertype  INT
  ); 

  CREATE TABLE if not exists orders (
     order_id VARCHAR(100) PRIMARY KEY,
     username VARCHAR(100) NOT NULL,
     order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
     total_amount DOUBLE NOT NULL,
     status VARCHAR(50) DEFAULT 'PENDING',
     cancel_reason TEXT NULL,
     cancelled_by ENUM('CUSTOMER', 'SELLER', 'SYSTEM') NULL,
     cancelled_at TIMESTAMP NULL,
     shipped_at TIMESTAMP NULL,
     FOREIGN KEY (username) REFERENCES users(username)
  );

  CREATE TABLE if not exists order_details (
     id INT AUTO_INCREMENT PRIMARY KEY,
     order_id VARCHAR(100) NOT NULL,
     book_barcode VARCHAR(100) NOT NULL,
     quantity INT NOT NULL,
     amount DOUBLE NOT NULL,
     FOREIGN KEY (order_id) REFERENCES orders(order_id),
     FOREIGN KEY (book_barcode) REFERENCES books(barcode)
  );
