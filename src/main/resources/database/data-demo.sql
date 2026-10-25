-- Frameload Cash - données de démonstration
-- À exécuter APRÈS database/schema.sql. Les soldes sont strictement égaux à la somme des transactions.

USE frameloadcash;

INSERT INTO clients (id, client_number, first_name, last_name, email, phone, address, status, created_at) VALUES
(1,  'CL-000001', 'Awa',     'Nkoulou', 'awa.nkoulou@example.test',    '+237 600 00 00 01', 'Yaoundé',   'ACTIVE',   '2026-09-01'),
(2,  'CL-000002', 'Paul',    'Essomba', 'paul.essomba@example.test',   '+237 600 00 00 02', 'Douala',    'ACTIVE',   '2026-09-01'),
(3,  'CL-000003', 'Marie',   'Mballa',  'marie.mballa@example.test',   '+237 600 00 00 03', 'Yaoundé',   'ACTIVE',   '2026-09-01'),
(4,  'CL-000004', 'Jean',    'Fotso',   'jean.fotso@example.test',     '+237 600 00 00 04', 'Bafoussam', 'ACTIVE',   '2026-09-01'),
(5,  'CL-000005', 'Carine',  'Ngono',   'carine.ngono@example.test',   '+237 600 00 00 05', 'Yaoundé',   'ACTIVE',   '2026-09-01'),
(6,  'CL-000006', 'Serge',   'Tchamba', 'serge.tchamba@example.test',  '+237 600 00 00 06', 'Douala',    'ACTIVE',   '2026-09-01'),
(7,  'CL-000007', 'Estelle', 'Biyong',  'estelle.biyong@example.test', '+237 600 00 00 07', NULL,        'ACTIVE',   '2026-09-01'),
(8,  'CL-000008', 'Boris',   'Kamga',   'boris.kamga@example.test',    '+237 600 00 00 08', 'Garoua',    'ACTIVE',   '2026-09-01'),
(9,  'CL-000009', 'Nadège',  'Onana',   'nadege.onana@example.test',   '+237 600 00 00 09', 'Yaoundé',   'INACTIVE', '2026-09-01'),
(10, 'CL-000010', 'Franck',  'Mvondo',  'franck.mvondo@example.test',  '+237 600 00 00 10', 'Kribi',     'ACTIVE',   '2026-09-01');

INSERT INTO accounts (id, account_number, client_id, account_type, balance, currency, status, created_at) VALUES
(1,  'FC-000001', 1, 'CURRENT', 350000.00, 'XAF', 'ACTIVE',  '2026-09-02'),
(2,  'FC-000002', 1, 'SAVINGS', 350000.00, 'XAF', 'ACTIVE',  '2026-09-02'),
(3,  'FC-000003', 2, 'CURRENT', 270000.00, 'XAF', 'ACTIVE',  '2026-09-02'),
(4,  'FC-000004', 3, 'CURRENT',  80000.00, 'XAF', 'ACTIVE',  '2026-09-02'),
(5,  'FC-000005', 4, 'CURRENT', 680000.00, 'XAF', 'ACTIVE',  '2026-09-02'),
(6,  'FC-000006', 5, 'CURRENT', 250000.00, 'XAF', 'ACTIVE',  '2026-09-02'),
(7,  'FC-000007', 6, 'CURRENT',  60000.00, 'XAF', 'BLOCKED', '2026-09-02'),
(8,  'FC-000008', 7, 'SAVINGS',  20000.00, 'XAF', 'ACTIVE',  '2026-09-02'),
(9,  'FC-000009', 3, 'SAVINGS',      0.00, 'XAF', 'ACTIVE',  '2026-09-02'),
(10, 'FC-000010', 8, 'CURRENT',      0.00, 'XAF', 'CLOSED',  '2026-09-02'),
(11, 'FC-000011', 2, 'SAVINGS',  45000.00, 'XAF', 'ACTIVE',  '2026-09-02'),
(12, 'FC-000012', 9, 'CURRENT',      0.00, 'XAF', 'BLOCKED', '2026-09-02');

INSERT INTO transfers (id, reference, source_account_id, destination_account_id, amount, status, created_at) VALUES
(1, 'TR-000001', 1, 2,  50000.00, 'COMPLETED', '2026-10-05 11:00:00.000000'),
(2, 'TR-000002', 5, 3, 120000.00, 'COMPLETED', '2026-10-06 15:30:00.000000');

INSERT INTO transactions (id, reference, account_id, transaction_type, amount, description, status, created_at, transfer_id) VALUES
(1,  'TX-000001', 1,  'DEPOSIT',      500000.00, 'Dépôt initial',          'COMPLETED', '2026-10-01 09:00:00.000000', NULL),
(2,  'TX-000002', 2,  'DEPOSIT',      300000.00, 'Dépôt initial',          'COMPLETED', '2026-10-01 09:10:00.000000', NULL),
(3,  'TX-000003', 3,  'DEPOSIT',      150000.00, 'Dépôt initial',          'COMPLETED', '2026-10-01 09:20:00.000000', NULL),
(4,  'TX-000004', 4,  'DEPOSIT',       80000.00, 'Dépôt initial',          'COMPLETED', '2026-10-01 09:30:00.000000', NULL),
(5,  'TX-000005', 5,  'DEPOSIT',     1000000.00, 'Dépôt initial',          'COMPLETED', '2026-10-01 09:40:00.000000', NULL),
(6,  'TX-000006', 6,  'DEPOSIT',      250000.00, 'Dépôt initial',          'COMPLETED', '2026-10-01 09:50:00.000000', NULL),
(7,  'TX-000007', 7,  'DEPOSIT',       60000.00, 'Dépôt initial',          'COMPLETED', '2026-10-01 10:00:00.000000', NULL),
(8,  'TX-000008', 8,  'DEPOSIT',       20000.00, 'Dépôt initial',          'COMPLETED', '2026-10-01 10:10:00.000000', NULL),
(9,  'TX-000009', 1,  'WITHDRAWAL',   100000.00, 'Retrait au guichet',     'COMPLETED', '2026-10-03 14:00:00.000000', NULL),
(10, 'TX-000010', 5,  'WITHDRAWAL',   200000.00, 'Retrait au guichet',     'COMPLETED', '2026-10-03 16:20:00.000000', NULL),
(11, 'TX-000011', 1,  'TRANSFER_OUT',  50000.00, 'Épargne mensuelle',      'COMPLETED', '2026-10-05 11:00:00.000000', 1),
(12, 'TX-000012', 2,  'TRANSFER_IN',   50000.00, 'Épargne mensuelle',      'COMPLETED', '2026-10-05 11:00:00.000000', 1),
(13, 'TX-000013', 5,  'TRANSFER_OUT', 120000.00, 'Règlement fournisseur',  'COMPLETED', '2026-10-06 15:30:00.000000', 2),
(14, 'TX-000014', 3,  'TRANSFER_IN',  120000.00, 'Règlement fournisseur',  'COMPLETED', '2026-10-06 15:30:00.000000', 2),
(15, 'TX-000015', 10, 'DEPOSIT',       10000.00, 'Dépôt avant clôture',    'COMPLETED', '2026-10-02 08:00:00.000000', NULL),
(16, 'TX-000016', 10, 'WITHDRAWAL',    10000.00, 'Retrait avant clôture',  'COMPLETED', '2026-10-02 08:30:00.000000', NULL),
(17, 'TX-000017', 11, 'DEPOSIT',       45000.00, 'Dépôt de démonstration', 'COMPLETED', '2026-10-04 10:00:00.000000', NULL);


INSERT INTO bank_cards (id, card_reference, account_id, card_type, expiration_date, status, created_at) VALUES
(1, 'CD-000001', 1, 'DEBIT',   '2029-09-03', 'ACTIVE',  '2026-09-03'),
(2, 'CD-000002', 2, 'PREPAID', '2029-09-03', 'ACTIVE',  '2026-09-03'),
(3, 'CD-000003', 3, 'DEBIT',   '2029-09-03', 'ACTIVE',  '2026-09-03'),
(4, 'CD-000004', 5, 'PREPAID', '2029-09-03', 'ACTIVE',  '2026-09-03'),
(5, 'CD-000005', 6, 'DEBIT',   '2029-09-03', 'BLOCKED', '2026-09-03'),
(6, 'CD-000006', 7, 'DEBIT',   '2026-10-01', 'EXPIRED', '2026-09-03');