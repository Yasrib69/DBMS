# Mini DBMS (Java)

## Overview

This project is a **Java-based Mini Database Management System (DBMS)** that supports a subset of SQL-like commands. It allows users to create databases and tables, insert and query data, and perform updates and deletions with validation and constraint enforcement.

---

## How to Run

### Option 1 — Command Line (Recommended)

1. Compile all Java files:

```bash
javac *.java
```

2. Run the program:

```bash
java DBMS
```

3. Execute commands manually or use the input command:

```sql
INPUT sample-project-test-data.txt OUTPUT file.output;
```

---

### Option 2 — Using Input Redirection

```bash
javac *.java
java DBMS < sample-project-test-data.txt > file1.output
```

---

### Option 3 — Using IDE

1. Open the project in your IDE (IntelliJ / Eclipse / VS Code)
2. Run `DBMS.java`
3. In the console, execute:

```sql
INPUT sample-project-test-data.txt OUTPUT file.output;
```

---

## Features

* Create and use databases
* Create, rename, and delete tables
* Insert records with type checking
* Select data with optional `WHERE` conditions
* Update records using `SET` and `WHERE`
* Delete records or entire tables
* Aggregation functions:

  * `COUNT`
  * `MIN`
  * `MAX`
  * `AVG`
* Cartesian product for multi-table queries
* Primary key constraint enforcement
* File-based storage and retrieval

---

## Supported Data Types

* `INTEGER`
* `FLOAT`
* `TEXT`

---

## Notes

* All commands must end with a semicolon (`;`)
* The system validates:

  * Unknown columns
  * Invalid data types
  * Duplicate primary keys
  * Syntax errors
* Errors are handled gracefully and printed to output

---

## Project Structure

```
DBMS/
├── DBMS.java
├── Parser.java
├── Table.java
├── Database.java
├── Record.java
├── Condition.java
├── Tokenizer.java
├── FileManager.java
├── BST.java
├── Node.java
```

---

## Author

Fahad Akon
Yasrib Yasir Farook
Salma Ibrahim
Eastern Michigan University
Course: COSC 471 – Project