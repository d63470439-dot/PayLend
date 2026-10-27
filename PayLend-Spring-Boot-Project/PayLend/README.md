# PayLend — Java + Spring Boot + Website

A local educational demo of the PayLend project described in the assignment: registration, wallet top-ups, peer-to-peer transfers, mobile recharge, micro-loans, instalment repayments, and transaction history.

## Requirements
- Java 17 or newer
- Maven 3.6+ (`mvn -version` should work)
- VS Code with Extension Pack for Java (recommended)

## Run it
1. Extract the `PayLend` folder.
2. Open **the PayLend folder** in VS Code (the folder containing `pom.xml`).
3. Open a terminal in VS Code.
4. Run:
   ```bash
   mvn spring-boot:run
   ```
5. Wait until Spring Boot says it started, then open:
   `http://localhost:8080`

The frontend is served by Spring Boot itself, so you do **not** need Live Server or a separate frontend command.

## Demo workflow
1. Register at least two users using the Register panel.
2. Select a user in the top selector.
3. Add money to that user's wallet.
4. Use Transfer to send money to another registered user.
5. Request a micro-loan and repay it in instalments.
6. Open Transactions to review records.

## Data storage
The app saves its local demo data to `data/paylend-data.json` in the project working directory. Stop the app before manually editing this file. To reset the demo, stop the app and delete that file; it will be recreated on the next run.

## Important limitations
- This is a **college project/demo**, not a real banking or payment system.
- It does not connect to a bank, payment gateway, credit bureau, or telecom operator. Add Money and Mobile Recharge are simulated.
- No real authentication, authorization, encryption, production database, or regulatory compliance is implemented. Do not enter real personal or financial information.
- The starter project uses a configurable demo loan limit and flat interest rate; adjust them in `WalletService.java` to match the exact rules in your original console application if they differ.
