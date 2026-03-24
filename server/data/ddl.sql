/*M!999999\- enable the sandbox mode */ 
-- MariaDB dump 10.19  Distrib 10.11.15-MariaDB, for debian-linux-gnu (aarch64)
--
-- Host: host.docker.internal    Database: JGITKINS
-- ------------------------------------------------------
-- Server version	11.4.9-MariaDB-ubu2404

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Current Database: `JGITKINS`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `JGITKINS` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */;

USE `JGITKINS`;

--
-- Table structure for table `BRANCH`
--

DROP TABLE IF EXISTS `BRANCH`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `BRANCH` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `REPOSITORY_ID` bigint(20) NOT NULL,
  `NAME` varchar(255) NOT NULL,
  `IS_LOCKED` tinyint(1) NOT NULL DEFAULT 0,
  `IS_CI` tinyint(1) NOT NULL DEFAULT 0,
  `IS_DEFAULT` tinyint(1) NOT NULL DEFAULT 0,
  `LOCKED_BY` bigint(20) DEFAULT NULL,
  `LOCKED_AT` timestamp NULL DEFAULT NULL,
  `CREATED_AT` timestamp NOT NULL DEFAULT current_timestamp(),
  `UPDATED_AT` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`ID`),
  UNIQUE KEY `UK_BRANCH_REPOSITORY_NAME` (`REPOSITORY_ID`,`NAME`)
) ENGINE=InnoDB AUTO_INCREMENT=32 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `JOB`
--

DROP TABLE IF EXISTS `JOB`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `JOB` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `REPOSITORY_ID` bigint(20) NOT NULL,
  `COMMIT_HASH` varchar(64) NOT NULL,
  `BRANCH_NAME` varchar(255) NOT NULL,
  `TRIGGERED_BY` bigint(20) NOT NULL,
  `CREATED_AT` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `JOB_HISTORY`
--

DROP TABLE IF EXISTS `JOB_HISTORY`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `JOB_HISTORY` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `JOB_ID` bigint(20) NOT NULL,
  `RUNNER_ID` bigint(20) DEFAULT NULL,
  `STATUS` varchar(32) NOT NULL,
  `LOG_PATH` varchar(1024) DEFAULT NULL,
  `STARTED_AT` timestamp NULL DEFAULT NULL,
  `FINISHED_AT` timestamp NULL DEFAULT NULL,
  `CREATED_AT` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ORGANIZE`
--

DROP TABLE IF EXISTS `ORGANIZE`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `ORGANIZE` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `NAME` varchar(255) NOT NULL,
  `PATH` varchar(255) NOT NULL,
  `DESCRIPTION` text DEFAULT NULL,
  `OWNER_ID` bigint(20) NOT NULL,
  `CREATED_AT` timestamp NOT NULL DEFAULT current_timestamp(),
  `UPDATED_AT` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`ID`),
  UNIQUE KEY `UK_ORGANIZE_PATH` (`PATH`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ORGANIZE_MEMBER`
--

DROP TABLE IF EXISTS `ORGANIZE_MEMBER`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `ORGANIZE_MEMBER` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `ORGANIZE_ID` bigint(20) NOT NULL,
  `USER_ID` bigint(20) NOT NULL,
  `ROLE` varchar(32) NOT NULL,
  `JOINED_AT` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`ID`),
  UNIQUE KEY `UK_ORGANIZE_MEMBER_USER` (`ORGANIZE_ID`,`USER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `REPOSITORY`
--

DROP TABLE IF EXISTS `REPOSITORY`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `REPOSITORY` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `NAME` varchar(255) NOT NULL,
  `PATH` varchar(255) NOT NULL,
  `REPOSITORY_TYPE` varchar(32) NOT NULL DEFAULT 'GIT',
  `OWNER_TYPE` varchar(32) DEFAULT NULL,
  `OWNER_ID` bigint(20) DEFAULT NULL,
  `CREDENTIAL_ID` varchar(128) DEFAULT NULL,
  `CLONE_PATH` varchar(512) DEFAULT NULL,
  `DESCRIPTION` text DEFAULT NULL,
  `DEFAULT_BRANCH` varchar(255) NOT NULL,
  `VISIBILITY` varchar(32) NOT NULL,
  `STATUS` varchar(32) NOT NULL DEFAULT 'REGISTERED',
  `LAST_SYNCED_AT` timestamp NULL DEFAULT NULL,
  `CREATED_AT` timestamp NOT NULL DEFAULT current_timestamp(),
  `UPDATED_AT` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`ID`),
  UNIQUE KEY `UK_REPOSITORY_PATH` (`PATH`)
) ENGINE=InnoDB AUTO_INCREMENT=33 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `REPOSITORY_MEMBER`
--

DROP TABLE IF EXISTS `REPOSITORY_MEMBER`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `REPOSITORY_MEMBER` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `REPOSITORY_ID` bigint(20) NOT NULL,
  `USER_ID` bigint(20) NOT NULL,
  `ROLE` varchar(32) NOT NULL,
  `ADDED_AT` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`ID`),
  UNIQUE KEY `UK_REPOSITORY_MEMBER_USER` (`REPOSITORY_ID`,`USER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `RUNNER`
--

DROP TABLE IF EXISTS `RUNNER`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `RUNNER` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `TOKEN` varchar(255) NOT NULL,
  `DESCRIPTION` varchar(512) DEFAULT NULL,
  `STATUS` varchar(32) NOT NULL,
  `IP_ADDRESS` varchar(45) DEFAULT NULL,
  `LAST_HEARTBEAT_AT` timestamp NULL DEFAULT NULL,
  `CREATED_AT` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `RUNNER_ASSIGNMENT`
--

DROP TABLE IF EXISTS `RUNNER_ASSIGNMENT`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `RUNNER_ASSIGNMENT` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `RUNNER_ID` bigint(20) NOT NULL,
  `TARGET_TYPE` varchar(32) NOT NULL,
  `TARGET_ID` bigint(20) DEFAULT NULL,
  `ASSIGNED_AT` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `USER`
--

DROP TABLE IF EXISTS `USER`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `USER` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `USERNAME` varchar(150) NOT NULL,
  `EMAIL` varchar(254) DEFAULT NULL,
  `DISPLAY_NAME` varchar(255) DEFAULT NULL,
  `AVATAR_URL` varchar(1024) DEFAULT NULL,
  `AUTHORITY` varchar(32) NOT NULL DEFAULT 'USER',
  `STATUS` varchar(32) NOT NULL DEFAULT 'ACTIVE',
  `LAST_LOGIN_AT` timestamp NULL DEFAULT NULL,
  `CREATED_AT` timestamp NOT NULL DEFAULT current_timestamp(),
  `UPDATED_AT` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`ID`),
  UNIQUE KEY `UK_USERS_USERNAME` (`USERNAME`),
  UNIQUE KEY `UK_USERS_EMAIL` (`EMAIL`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `USER_CREDENTIALS`
--

DROP TABLE IF EXISTS `USER_CREDENTIALS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `USER_CREDENTIALS` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `USER_ID` bigint(20) NOT NULL,
  `PROVIDER` varchar(32) NOT NULL DEFAULT 'LOCAL',
  `NAME` varchar(128) NOT NULL,
  `DESCRIPTION` text DEFAULT NULL,
  `PASSWORD_HASH` varchar(255) NOT NULL,
  `CREATED_AT` timestamp NOT NULL DEFAULT current_timestamp(),
  `UPDATED_AT` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `USER_IDENTITIES`
--

DROP TABLE IF EXISTS `USER_IDENTITIES`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `USER_IDENTITIES` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `USER_ID` bigint(20) NOT NULL,
  `PROVIDER_NAME` varchar(64) NOT NULL,
  `PROVIDER_SUB` varchar(255) NOT NULL,
  `EMAIL` varchar(254) DEFAULT NULL,
  `EMAIL_VERIFIED` tinyint(1) NOT NULL DEFAULT 0,
  `NAME` varchar(255) DEFAULT NULL,
  `AVATAR_URL` varchar(1024) DEFAULT NULL,
  `CREATED_AT` timestamp NOT NULL DEFAULT current_timestamp(),
  `UPDATED_AT` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`ID`),
  UNIQUE KEY `UK_USER_IDENTITIES_PROVIDER` (`PROVIDER_NAME`,`PROVIDER_SUB`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-02-04 13:58:19
