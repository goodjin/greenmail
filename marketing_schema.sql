-- SQL Schema for Marketing Application

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0; -- Disable checks temporarily for table creation order

-- 1. Enterprises Table
CREATE TABLE `enterprises` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(255) NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Members Table
CREATE TABLE `members` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `enterprise_id` BIGINT UNSIGNED NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `email` VARCHAR(255) NOT NULL,
  `password_hash` VARCHAR(255) NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_members_email` (`email`),
  KEY `idx_members_enterprise_id` (`enterprise_id`),
  CONSTRAINT `fk_members_enterprise_id` FOREIGN KEY (`enterprise_id`) REFERENCES `enterprises` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Email Accounts Table
CREATE TABLE `email_accounts` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `member_id` BIGINT UNSIGNED NOT NULL,
  `email_address` VARCHAR(255) NOT NULL,
  `smtp_host` VARCHAR(255) NOT NULL,
  `smtp_port` INT UNSIGNED NOT NULL,
  `smtp_username` VARCHAR(255) NOT NULL,
  `smtp_password` VARCHAR(255) NOT NULL, -- In a real app, ensure this is encrypted at application level before storing
  `smtp_protocol` VARCHAR(50) NOT NULL COMMENT 'e.g., smtp, smtps',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_email_accounts_member_id` (`member_id`),
  CONSTRAINT `fk_email_accounts_member_id` FOREIGN KEY (`member_id`) REFERENCES `members` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Campaigns Table
CREATE TABLE `campaigns` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `member_id` BIGINT UNSIGNED NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `description` TEXT DEFAULT NULL,
  `status` VARCHAR(50) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT, ACTIVE, PAUSED, COMPLETED, ARCHIVED',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_campaigns_member_id` (`member_id`),
  KEY `idx_campaigns_status` (`status`),
  CONSTRAINT `fk_campaigns_member_id` FOREIGN KEY (`member_id`) REFERENCES `members` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Email Templates Table
CREATE TABLE `email_templates` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `member_id` BIGINT UNSIGNED NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `content_html` TEXT DEFAULT NULL,
  `content_text` TEXT DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_email_templates_member_id` (`member_id`),
  CONSTRAINT `fk_email_templates_member_id` FOREIGN KEY (`member_id`) REFERENCES `members` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Campaign Rounds Table
CREATE TABLE `campaign_rounds` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `campaign_id` BIGINT UNSIGNED NOT NULL,
  `round_number` INT UNSIGNED NOT NULL COMMENT '1 to 4 typically',
  `email_template_id` BIGINT UNSIGNED NOT NULL,
  `subject_template` VARCHAR(255) NOT NULL,
  `time_interval_days` INT UNSIGNED DEFAULT NULL COMMENT 'Days after previous round (or campaign start for round 1)',
  `scheduled_send_time` TIMESTAMP NULL DEFAULT NULL COMMENT 'Absolute time for sending this round',
  `status` VARCHAR(50) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, CONFIGURING, SENDING, SENT, FAILED, SKIPPED',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_campaign_rounds_campaign_round` (`campaign_id`, `round_number`),
  KEY `idx_campaign_rounds_email_template_id` (`email_template_id`),
  KEY `idx_campaign_rounds_status` (`status`),
  CONSTRAINT `fk_campaign_rounds_campaign_id` FOREIGN KEY (`campaign_id`) REFERENCES `campaigns` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_campaign_rounds_email_template_id` FOREIGN KEY (`email_template_id`) REFERENCES `email_templates` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Target Lists Table
CREATE TABLE `target_lists` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `member_id` BIGINT UNSIGNED NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_target_lists_member_id` (`member_id`),
  CONSTRAINT `fk_target_lists_member_id` FOREIGN KEY (`member_id`) REFERENCES `members` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. Target List Contacts Table
CREATE TABLE `target_list_contacts` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `target_list_id` BIGINT UNSIGNED NOT NULL,
  `email_address` VARCHAR(255) NOT NULL,
  `first_name` VARCHAR(255) DEFAULT NULL,
  `last_name` VARCHAR(255) DEFAULT NULL,
  `custom_fields` JSON DEFAULT NULL,
  `subscribed` BOOLEAN NOT NULL DEFAULT TRUE,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_target_list_contacts_list_email` (`target_list_id`, `email_address`),
  KEY `idx_target_list_contacts_email` (`email_address`), -- For searching contacts across lists
  KEY `idx_target_list_contacts_subscribed` (`subscribed`),
  CONSTRAINT `fk_target_list_contacts_target_list_id` FOREIGN KEY (`target_list_id`) REFERENCES `target_lists` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. Campaign Target Lists (Many-to-Many)
CREATE TABLE `campaign_target_lists` (
  `campaign_id` BIGINT UNSIGNED NOT NULL,
  `target_list_id` BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (`campaign_id`, `target_list_id`),
  CONSTRAINT `fk_campaign_target_lists_campaign_id` FOREIGN KEY (`campaign_id`) REFERENCES `campaigns` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_campaign_target_lists_target_list_id` FOREIGN KEY (`target_list_id`) REFERENCES `target_lists` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. Sent Emails Log Table
CREATE TABLE `sent_emails_log` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `campaign_round_id` BIGINT UNSIGNED NOT NULL,
  `contact_id` BIGINT UNSIGNED NOT NULL,
  `email_account_id` BIGINT UNSIGNED NOT NULL,
  `sent_at` TIMESTAMP NULL DEFAULT NULL,
  `status` VARCHAR(50) NOT NULL COMMENT 'PREPARING, SENT, FAILED, BOUNCED, OPENED, CLICKED',
  `error_message` TEXT DEFAULT NULL,
  `message_id_header` VARCHAR(255) DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_sent_emails_log_campaign_round_id` (`campaign_round_id`),
  KEY `idx_sent_emails_log_contact_id` (`contact_id`),
  KEY `idx_sent_emails_log_email_account_id` (`email_account_id`),
  KEY `idx_sent_emails_log_status` (`status`),
  KEY `idx_sent_emails_log_message_id_header` (`message_id_header`),
  CONSTRAINT `fk_sent_emails_log_campaign_round_id` FOREIGN KEY (`campaign_round_id`) REFERENCES `campaign_rounds` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_sent_emails_log_contact_id` FOREIGN KEY (`contact_id`) REFERENCES `target_list_contacts` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_sent_emails_log_email_account_id` FOREIGN KEY (`email_account_id`) REFERENCES `email_accounts` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1; -- Re-enable checks
