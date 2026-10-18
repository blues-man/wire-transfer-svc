package com.acme.finance.transfer.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.acme.finance.transfer.model.Account;

/**
 * Integration tests for the account data layer.
 *
 * These run under failsafe rather than surefire because they cross a real
 * boundary: every assertion below goes through JDBC into an actual H2
 * database, schema and all. That matters for this application -- H2 is the
 * dependency Lightwell remediates, so a bump that broke SQL behaviour would
 * show up here and nowhere in the unit tests.
 */
class AccountDAOIT {

    private AccountDAO dao;

    @BeforeEach
    void setUp() {
        dao = new AccountDAO();
    }

    private Account newAccount(BigDecimal balance) {
        Account account = new Account();
        // Unique per run: the schema has a UNIQUE constraint on account_number
        // and DB_CLOSE_DELAY=-1 keeps the in-memory database alive for the
        // whole JVM, so rows outlive individual tests.
        account.setAccountNumber("IT-" + UUID.randomUUID().toString().substring(0, 8));
        account.setHolderName("Integration Test");
        account.setAccountType("CHECKING");
        account.setBalance(balance);
        account.setEmail("it@acme.example");
        account.setPhone("555-0100");
        return account;
    }

    @Test
    @DisplayName("schema is created and seeded on first connection")
    void seedDataIsPresent() throws SQLException {
        List<Account> accounts = dao.findAll();
        assertNotNull(accounts, "findAll() must not return null");
        assertFalse(accounts.isEmpty(), "DatabaseConnection should seed demo accounts");
    }

    @Test
    @DisplayName("an account survives a create/read round trip")
    void createThenReadBack() throws SQLException {
        Account created = dao.create(newAccount(new BigDecimal("250.00")));
        assertNotNull(created.getId(), "create() must return the generated id");

        Account found = dao.findByAccountNumber(created.getAccountNumber());
        assertNotNull(found, "the account must be readable by its number");
        assertEquals(created.getId(), found.getId());
        assertEquals("Integration Test", found.getHolderName());
        // DECIMAL(15,2) round trip: compareTo, not equals, so 250 == 250.00
        assertEquals(0, new BigDecimal("250.00").compareTo(found.getBalance()),
                "balance must survive the DECIMAL(15,2) round trip");
    }

    @Test
    @DisplayName("deposit increases the stored balance")
    void depositIsPersisted() throws SQLException {
        Account account = dao.create(newAccount(new BigDecimal("100.00")));
        dao.deposit(account.getId(), new BigDecimal("25.50"));

        Account after = dao.findById(account.getId());
        assertEquals(0, new BigDecimal("125.50").compareTo(after.getBalance()),
                "deposit must be committed to the database");
    }

    @Test
    @DisplayName("withdraw succeeds with sufficient funds and is refused without")
    void withdrawRespectsBalance() throws SQLException {
        Account account = dao.create(newAccount(new BigDecimal("80.00")));

        assertTrue(dao.withdraw(account.getId(), new BigDecimal("30.00")),
                "a withdrawal within the balance must succeed");
        assertEquals(0, new BigDecimal("50.00").compareTo(dao.findById(account.getId()).getBalance()));

        assertFalse(dao.withdraw(account.getId(), new BigDecimal("500.00")),
                "an overdraft must be refused");
        assertEquals(0, new BigDecimal("50.00").compareTo(dao.findById(account.getId()).getBalance()),
                "a refused withdrawal must leave the balance untouched");
    }

    @Test
    @DisplayName("delete deactivates rather than removing")
    void deleteIsASoftDelete() throws SQLException {
        Account account = dao.create(newAccount(new BigDecimal("10.00")));
        String number = account.getAccountNumber();
        assertTrue(dao.findAll().stream().anyMatch(a -> number.equals(a.getAccountNumber())),
                "a new account must be listed before deletion");

        dao.delete(account.getId());

        // delete() sets active = FALSE. findAll() filters on active = TRUE,
        // findById() does not -- so the row is still addressable by id. That
        // asymmetry is deliberate in this application; assert it rather than
        // assuming a hard delete.
        assertFalse(dao.findAll().stream().anyMatch(a -> number.equals(a.getAccountNumber())),
                "a deactivated account must drop out of findAll()");
        assertNotNull(dao.findById(account.getId()),
                "a deactivated account must still be addressable by id");
    }
}
