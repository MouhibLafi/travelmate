
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

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `travelmate` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci */;

USE `travelmate`;
DROP TABLE IF EXISTS `preference_interests`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `preference_interests` (
  `preferences_id` bigint(20) NOT NULL,
  `interest` enum('AVENTURE','CULTURE','DETENTE','GASTRONOMIE','HISTOIRE','NATURE','PLAGE','SHOPPING','SPORT','VIE_NOCTURNE') DEFAULT NULL,
  KEY `FK8a55hv6t9qr2eeskowemmfjxv` (`preferences_id`),
  CONSTRAINT `FK8a55hv6t9qr2eeskowemmfjxv` FOREIGN KEY (`preferences_id`) REFERENCES `travel_preferences` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `preference_interests` WRITE;
/*!40000 ALTER TABLE `preference_interests` DISABLE KEYS */;
INSERT INTO `preference_interests` VALUES (2,'CULTURE'),(2,'GASTRONOMIE'),(3,'PLAGE'),(3,'GASTRONOMIE'),(4,'AVENTURE'),(4,'NATURE'),(5,'HISTOIRE'),(5,'CULTURE'),(5,'GASTRONOMIE'),(6,'SPORT'),(6,'PLAGE'),(6,'VIE_NOCTURNE'),(7,'DETENTE'),(7,'GASTRONOMIE'),(8,'AVENTURE'),(8,'SPORT'),(9,'SHOPPING'),(9,'CULTURE'),(9,'GASTRONOMIE'),(10,'SPORT'),(10,'NATURE'),(11,'HISTOIRE'),(11,'NATURE'),(11,'CULTURE'),(13,'PLAGE'),(13,'DETENTE'),(14,'SHOPPING'),(14,'CULTURE'),(14,'VIE_NOCTURNE'),(15,'SHOPPING'),(15,'DETENTE'),(15,'GASTRONOMIE'),(16,'AVENTURE'),(16,'NATURE'),(17,'CULTURE'),(18,'HISTOIRE'),(18,'CULTURE'),(18,'GASTRONOMIE'),(19,'SPORT'),(19,'VIE_NOCTURNE'),(3,'SPORT'),(3,'SHOPPING');
/*!40000 ALTER TABLE `preference_interests` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `travel_preferences`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `travel_preferences` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `ai_generated` bit(1) NOT NULL,
  `budget_level` enum('ECONOMIQUE','LUXE','MOYEN') DEFAULT NULL,
  `description` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `travel_preferences` WRITE;
/*!40000 ALTER TABLE `travel_preferences` DISABLE KEYS */;
INSERT INTO `travel_preferences` VALUES (1,'\0',NULL,NULL),(2,'','LUXE','Je rêve du Japon : temples, jardins et cuisine raffinée, je peux mettre le prix.'),(3,'','ECONOMIQUE','Je suis étudiante, j\'adore la plage et la street food et un peut de footing et aussi je faire shooping un peu\n'),(4,'','MOYEN','Passionné de randonnée et de photo en montagne, avec un budget raisonnable.'),(5,'','MOYEN','Musées, vieilles médinas et cafés culturels, en hôtel confortable.'),(6,'','MOYEN','Je veux faire du surf la journée et sortir le soir.'),(7,'','LUXE','Spa, hammam et repos total dans des hôtels haut de gamme.'),(8,'','ECONOMIQUE','Plongée, escalade et sensations fortes, avec un petit budget.'),(9,'','MOYEN','J\'aime chiner dans les souks et goûter la cuisine traditionnelle.'),(10,'','LUXE','Ski en hiver, vélo en été, et je ne compte pas.'),(11,'','ECONOMIQUE','Sites romains, histoire ancienne et désert du Sud, pas cher.'),(12,'\0',NULL,NULL),(13,'','MOYEN','Plage, détente et couchers de soleil, budget moyen.'),(14,'','MOYEN','Concerts, bars et vie nocturne dans les grandes villes.'),(15,'','LUXE','Boutiques de créateurs, grands restaurants et hôtels de luxe.'),(16,'','ECONOMIQUE','Camping dans le désert et randonnée, le moins cher possible.'),(17,'','MOYEN','Art contemporain, théâtre et musées européens.'),(18,'','LUXE','Châteaux, histoire et gastronomie française, avec du confort.'),(19,'','ECONOMIQUE','Football et matchs à l\'étranger, petit budget.'),(20,'\0',NULL,NULL);
/*!40000 ALTER TABLE `travel_preferences` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `trip_interests`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `trip_interests` (
  `trip_id` bigint(20) NOT NULL,
  `interest` enum('AVENTURE','CULTURE','DETENTE','GASTRONOMIE','HISTOIRE','NATURE','PLAGE','SHOPPING','SPORT','VIE_NOCTURNE') DEFAULT NULL,
  KEY `FKgpdplwil0butd8nsia14nvs5` (`trip_id`),
  CONSTRAINT `FKgpdplwil0butd8nsia14nvs5` FOREIGN KEY (`trip_id`) REFERENCES `trips` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `trip_interests` WRITE;
/*!40000 ALTER TABLE `trip_interests` DISABLE KEYS */;
INSERT INTO `trip_interests` VALUES (1,'CULTURE'),(1,'GASTRONOMIE'),(2,'PLAGE'),(2,'GASTRONOMIE'),(3,'PLAGE'),(4,'AVENTURE'),(4,'NATURE'),(5,'SPORT'),(5,'NATURE'),(6,'HISTOIRE'),(6,'CULTURE'),(7,'HISTOIRE'),(7,'SHOPPING'),(7,'GASTRONOMIE'),(8,'PLAGE'),(8,'VIE_NOCTURNE'),(9,'DETENTE'),(9,'PLAGE'),(10,'DETENTE'),(11,'AVENTURE'),(11,'SPORT'),(12,'SHOPPING'),(12,'CULTURE'),(13,'SPORT'),(13,'NATURE'),(14,'HISTOIRE'),(14,'NATURE'),(15,'HISTOIRE'),(16,'PLAGE'),(16,'DETENTE'),(17,'CULTURE'),(17,'VIE_NOCTURNE'),(18,'SHOPPING'),(18,'GASTRONOMIE'),(19,'SHOPPING'),(19,'DETENTE'),(20,'AVENTURE'),(20,'NATURE'),(21,'CULTURE'),(22,'HISTOIRE'),(22,'GASTRONOMIE'),(23,'SPORT'),(23,'SHOPPING'),(23,'PLAGE'),(23,'GASTRONOMIE');
/*!40000 ALTER TABLE `trip_interests` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `trips`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `trips` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `budget` double DEFAULT NULL,
  `budget_level` enum('ECONOMIQUE','LUXE','MOYEN') DEFAULT NULL,
  `destination` varchar(255) DEFAULT NULL,
  `end_date` date DEFAULT NULL,
  `start_date` date DEFAULT NULL,
  `user_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK8wb14dx6ed0bpp3planbay88u` (`user_id`),
  CONSTRAINT `FK8wb14dx6ed0bpp3planbay88u` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `trips` WRITE;
/*!40000 ALTER TABLE `trips` DISABLE KEYS */;
INSERT INTO `trips` VALUES (1,2500,'LUXE','Kyoto, Japon','2027-05-21','2027-05-12',1),(2,350,'ECONOMIQUE','Djerba','2026-12-08','2026-12-03',3),(3,150,'ECONOMIQUE','Hammamet','2026-08-27','2026-08-25',3),(4,400,'MOYEN','Aïn Draham','2026-11-21','2026-11-18',4),(5,1400,'MOYEN','Chamonix, France','2027-02-27','2027-02-21',4),(6,800,'MOYEN','Rome, Italie','2026-11-27','2026-11-23',5),(7,600,'MOYEN','Istanbul, Turquie','2026-07-29','2026-07-26',5),(8,1100,'LUXE','Ibiza, Espagne','2027-01-06','2027-01-02',6),(9,5200,'LUXE','Maldives','2027-03-30','2027-03-23',7),(10,900,'LUXE','Tozeur','2026-09-26','2026-09-24',7),(11,180,'ECONOMIQUE','Tabarka','2026-11-10','2026-11-08',8),(12,600,'MOYEN','Marrakech, Maroc','2026-12-24','2026-12-18',9),(13,3000,'LUXE','Zermatt, Suisse','2027-02-06','2027-02-01',10),(14,250,'ECONOMIQUE','Douz','2026-11-16','2026-11-13',11),(15,40,'ECONOMIQUE','Carthage','2026-10-09','2026-10-09',11),(16,500,'MOYEN','Mahdia','2026-12-12','2026-12-08',13),(17,700,'MOYEN','Berlin, Allemagne','2026-12-26','2026-12-23',14),(18,2400,'LUXE','Paris, France','2026-12-01','2026-11-28',15),(19,3500,'LUXE','Dubaï','2026-09-13','2026-09-09',15),(20,200,'ECONOMIQUE','Ksar Ghilane','2026-11-05','2026-11-03',16),(21,750,'MOYEN','Barcelone, Espagne','2027-01-16','2027-01-12',17),(22,1500,'LUXE','Lyon, France','2027-01-27','2027-01-22',18),(23,300,'MOYEN','Bengarden','2026-11-02','2026-10-31',3);
/*!40000 ALTER TABLE `trips` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `users` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL,
  `email` varchar(255) NOT NULL,
  `first_name` varchar(255) DEFAULT NULL,
  `last_name` varchar(255) DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL,
  `phone` varchar(255) DEFAULT NULL,
  `role` enum('ADMIN','VOYAGEUR') DEFAULT NULL,
  `preferences_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`),
  UNIQUE KEY `UKp500u51iqqij1u0b4eqv13lwx` (`preferences_id`),
  CONSTRAINT `FKbf0mv7jdwhx488wsahlw60669` FOREIGN KEY (`preferences_id`) REFERENCES `travel_preferences` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'','admin@travelmate.tn','Admin','TravelMate','$2a$10$T49HxG3b62dk4NbqygBqs.0iII2eIFVkD96suVQv7z4OB05MtB2pC',NULL,'ADMIN',1),(2,'','mouhib@travelmate.tn','Mouhib','Lafi','$2a$10$UcsAVcV5FLQnWEF6aCLbh.K4BGTEiL/pGTl70pIX5kl4eh6V6M4dq','+216 22 410 815','ADMIN',2),(3,'','yasmine@travelmate.tn','Yasmine','Trabelsi','$2a$10$esGPCPz7L3LYWc5p53sKJu0By/ruXWZDmyyzuSm1MbLzCd1pFHEEW','+216 25 301 774','VOYAGEUR',3),(4,'','karim@travelmate.tn','Karim','Ben Salah','$2a$10$jNL9XVEB1vDw6tjql0URuuwePdm4EoHPIPmVs7b.fdITEmPJmK7MS','+216 98 112 430','VOYAGEUR',4),(5,'','amira@travelmate.tn','Amira','Jlassi','$2a$10$PDFzXGThh/ZgHIL4MKZB7.KacLvDE2HSgsub/g1qHn.TRsow5Nau6','+216 52 678 019','VOYAGEUR',5),(6,'','mehdi@travelmate.tn','Mehdi','Bouazizi','$2a$10$/HhK6ltYomJCjvedGX256uTdmS3cLdHepKXPp9ET8ThmzI6fU/0IW',NULL,'VOYAGEUR',6),(7,'','ines@travelmate.tn','Ines','Hammami','$2a$10$drsjBWZz2T8s4FRigVLpke78ch77M7/8M0zhNOBaEzyEJf5/xAU7e','+216 20 555 901','VOYAGEUR',7),(8,'','youssef@travelmate.tn','Youssef','Mansour','$2a$10$d2Vqh7wZK2H5htkwKlqiSuBEUjZfqGABG6.ABhknL7LDefpIql22a','+216 29 870 342','VOYAGEUR',8),(9,'','nour@travelmate.tn','Nour','Ayari','$2a$10$RIMC0dEFy.Z70jfCjGr.9eN.EhtfAsAVFEVRVIa3ytfkRhueFcRpi',NULL,'VOYAGEUR',9),(10,'','hamza@travelmate.tn','Hamza','Chaabane','$2a$10$ORxhWZx66qHNKLZfM0IlnOo2oI1.F5YE2sws2bzL1cGJyhDfwSkhu','+216 55 204 668','VOYAGEUR',10),(11,'','rania@travelmate.tn','Rania','Khelifi','$2a$10$zfq1kJEyUAeRo76P6WjQNusmMwp5ciTGE/MHPSx1FXZjAIZnvH6Z6','+216 23 987 120','VOYAGEUR',11),(12,'','oussama@travelmate.tn','Oussama','Ferchichi','$2a$10$mMcn5m683yrPGuT9OR6M4OuQOGN3xhJ1JhY2vVO7.l.vMWU80zn.i',NULL,'VOYAGEUR',12),(13,'','mariem@travelmate.tn','Mariem','Zouari','$2a$10$0HW2KV.s18Yd/y.qqKV3H.F8XFh2q9uKMDKY9m/sU.ZZ0UYTRAxOS','+216 97 330 456','VOYAGEUR',13),(14,'','aymen@travelmate.tn','Aymen','Sassi','$2a$10$6.vJ0Qz0NpENiJFOqAWci.wxP/dH8/LvTxZ.sygqlc9AulUs5tQ1O',NULL,'VOYAGEUR',14),(15,'','sarra@travelmate.tn','Sarra','Ben Amor','$2a$10$sRTwUpZZVmCluTHvPnfwP.FA2x3uI3bMJNZnfOudUSo3NtAeqvAgy','+216 50 118 902','VOYAGEUR',15),(16,'','fares@travelmate.tn','Fares','Dridi','$2a$10$TX4AlDHPrpOgAdM/LCPtyeDtbt8G6htAfiprRvLvAx1I4BrG0Dmlu','+216 26 743 015','VOYAGEUR',16),(17,'','lina@travelmate.tn','Lina','Mejri','$2a$10$7IyarN.2WZYkscMyZpphp.Fj41ex8FmkkoExIuwZoapwvHUkEiiaG',NULL,'VOYAGEUR',17),(18,'','walid@travelmate.tn','Walid','Toumi','$2a$10$9BsNBNyX.VGCKtRod.0RR.NoGQC.kD0ReM6WtjbB8EOmMfb1YGq.q','+216 92 605 287','VOYAGEUR',18),(19,'\0','bilel@travelmate.tn','Bilel','Haddad','$2a$10$mOg4L.O4grpUc4HRG6dwMuw9bae.uqD1L.YYE2BqWXZFTUs/KKCWC','+216 24 019 863','VOYAGEUR',19),(20,'\0','emna@travelmate.tn','Emna','Rekik','$2a$10$D.S9V3c9UCjm2C.Twqj57eYVMR7rV0Wzf1LkxTwhBGaEeq46rC2S2',NULL,'VOYAGEUR',20);
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

