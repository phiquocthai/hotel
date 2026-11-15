-- MySQL dump 10.13  Distrib 8.0.45, for Win64 (x86_64)
--
-- Host: localhost    Database: test
-- ------------------------------------------------------
-- Server version	8.0.45

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `ca_lam_viec`
--

DROP TABLE IF EXISTS `ca_lam_viec`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ca_lam_viec` (
  `ma_ca` bigint NOT NULL AUTO_INCREMENT,
  `ten_ca` varchar(20) NOT NULL,
  `gio_bat_dau` time NOT NULL,
  `gio_ket_thuc` time NOT NULL,
  `dang_hoat_dong` tinyint(1) NOT NULL DEFAULT '1',
  `ngay_tao` datetime DEFAULT CURRENT_TIMESTAMP,
  `ngay_sua` datetime DEFAULT NULL,
  PRIMARY KEY (`ma_ca`),
  UNIQUE KEY `ten_ca` (`ten_ca`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ca_lam_viec`
--

LOCK TABLES `ca_lam_viec` WRITE;
/*!40000 ALTER TABLE `ca_lam_viec` DISABLE KEYS */;
INSERT INTO `ca_lam_viec` VALUES (1,'Ca Sáng','06:00:00','14:00:00',1,'2026-10-26 02:25:47',NULL),(2,'Ca Chiều','14:00:00','22:00:00',1,'2026-10-26 02:25:47',NULL),(3,'Ca Đêm','22:00:00','06:00:00',1,'2026-10-26 02:25:47',NULL),(4,'Ca hành chính','08:00:00','17:00:00',1,NULL,NULL);
/*!40000 ALTER TABLE `ca_lam_viec` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cham_cong`
--

DROP TABLE IF EXISTS `cham_cong`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cham_cong` (
  `ma_cham_cong` bigint NOT NULL AUTO_INCREMENT,
  `ma_phan_ca` bigint NOT NULL,
  `thoi_gian_vao` datetime DEFAULT NULL,
  `thoi_gian_ra` datetime DEFAULT NULL,
  `trang_thai` enum('DU_CONG','DI_TRE','VE_SOM','VANG_MAT') NOT NULL DEFAULT 'DU_CONG',
  `ghi_chu` varchar(255) DEFAULT NULL,
  `ngay_tao` datetime DEFAULT CURRENT_TIMESTAMP,
  `ngay_sua` datetime DEFAULT NULL,
  PRIMARY KEY (`ma_cham_cong`),
  UNIQUE KEY `ma_phan_ca` (`ma_phan_ca`),
  CONSTRAINT `cham_cong_ibfk_1` FOREIGN KEY (`ma_phan_ca`) REFERENCES `phan_ca` (`ma_phan_ca`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cham_cong`
--

LOCK TABLES `cham_cong` WRITE;
/*!40000 ALTER TABLE `cham_cong` DISABLE KEYS */;
INSERT INTO `cham_cong` VALUES (1,1,'2026-10-20 07:55:00','2026-10-20 16:00:00','DU_CONG','Đi làm đúng giờ','2026-10-26 02:25:48',NULL),(2,2,'2026-10-20 08:15:00','2026-10-20 16:00:00','DI_TRE','Đi làm trễ 15 phút','2026-10-26 02:25:48',NULL),(3,3,'2026-10-20 07:50:00','2026-10-20 15:30:00','VE_SOM','Về sớm','2026-10-26 02:25:48',NULL),(4,4,'2026-10-20 08:00:00','2026-10-20 16:00:00','DU_CONG','Hoàn thành ca làm','2026-10-26 02:25:48',NULL),(5,5,NULL,NULL,'VANG_MAT','Nhân viên vắng mặt','2026-10-26 02:25:48',NULL),(6,6,'2026-10-21 07:58:00','2026-10-21 16:00:00','DU_CONG','Đi làm đúng giờ','2026-10-26 02:25:48',NULL),(7,7,'2026-10-21 08:20:00','2026-10-21 16:00:00','DI_TRE','Đến muộn 20 phút','2026-10-26 02:25:48',NULL),(8,8,'2026-10-21 08:00:00','2026-10-21 15:00:00','VE_SOM','Có việc cá nhân','2026-10-26 02:25:48',NULL),(9,9,'2026-10-21 07:55:00','2026-10-21 16:05:00','DU_CONG','Hoàn thành ca làm','2026-10-26 02:25:48',NULL),(10,10,NULL,NULL,'VANG_MAT','Nghỉ không có mặt','2026-10-26 02:25:48',NULL);
/*!40000 ALTER TABLE `cham_cong` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `chi_tiet_dich_vu`
--

DROP TABLE IF EXISTS `chi_tiet_dich_vu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chi_tiet_dich_vu` (
  `ma_chi_tiet` bigint NOT NULL AUTO_INCREMENT,
  `ma_dat_phong` bigint NOT NULL,
  `ma_dich_vu` bigint NOT NULL,
  `so_luong` int NOT NULL,
  `don_gia` decimal(12,0) NOT NULL,
  `thoi_gian_su_dung` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`ma_chi_tiet`),
  KEY `ma_dat_phong` (`ma_dat_phong`),
  KEY `ma_dich_vu` (`ma_dich_vu`),
  CONSTRAINT `chi_tiet_dich_vu_ibfk_1` FOREIGN KEY (`ma_dat_phong`) REFERENCES `dat_phong` (`ma_dat_phong`),
  CONSTRAINT `chi_tiet_dich_vu_ibfk_2` FOREIGN KEY (`ma_dich_vu`) REFERENCES `dich_vu` (`ma_dich_vu`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `chi_tiet_dich_vu`
--

LOCK TABLES `chi_tiet_dich_vu` WRITE;
/*!40000 ALTER TABLE `chi_tiet_dich_vu` DISABLE KEYS */;
INSERT INTO `chi_tiet_dich_vu` VALUES (1,1,1,2,50000,'2026-10-20 09:00:00'),(2,1,2,1,100000,'2026-10-20 10:30:00'),(3,2,3,2,80000,'2026-10-20 11:00:00'),(4,2,4,1,150000,'2026-10-20 14:00:00'),(5,3,1,3,50000,'2026-10-20 15:00:00'),(6,3,5,1,200000,'2026-10-20 16:30:00'),(7,4,2,2,100000,'2026-10-21 08:30:00'),(8,4,3,1,80000,'2026-10-21 10:00:00'),(9,5,4,2,150000,'2026-10-21 13:00:00'),(10,5,5,1,200000,'2026-10-21 15:30:00');
/*!40000 ALTER TABLE `chi_tiet_dich_vu` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dat_phong`
--

DROP TABLE IF EXISTS `dat_phong`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dat_phong` (
  `ma_dat_phong` bigint NOT NULL AUTO_INCREMENT,
  `ma_khach_hang` bigint NOT NULL,
  `ma_phong` bigint NOT NULL,
  `ngay_nhan_phong` date NOT NULL,
  `ngay_tra_phong_du_kien` date NOT NULL,
  `thoi_gian_nhan_thuc_te` datetime DEFAULT NULL,
  `thoi_gian_tra_thuc_te` datetime DEFAULT NULL,
  `so_nguoi_o` int NOT NULL,
  `ghi_chu` text,
  `trang_thai` enum('DAT_TRUOC','DANG_O','DA_TRA','DA_HUY') NOT NULL DEFAULT 'DAT_TRUOC',
  `ngay_tao` datetime DEFAULT CURRENT_TIMESTAMP,
  `ngay_sua` datetime DEFAULT NULL,
  PRIMARY KEY (`ma_dat_phong`),
  KEY `ma_khach_hang` (`ma_khach_hang`),
  KEY `ma_phong` (`ma_phong`),
  CONSTRAINT `dat_phong_ibfk_1` FOREIGN KEY (`ma_khach_hang`) REFERENCES `khach_hang` (`ma_khach_hang`),
  CONSTRAINT `dat_phong_ibfk_2` FOREIGN KEY (`ma_phong`) REFERENCES `phong` (`ma_phong`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dat_phong`
--

LOCK TABLES `dat_phong` WRITE;
/*!40000 ALTER TABLE `dat_phong` DISABLE KEYS */;
INSERT INTO `dat_phong` VALUES (1,1,1,'2026-10-20','2026-10-22','2026-10-20 14:00:00',NULL,1,'Khách lưu trú 2 ngày','DANG_O','2026-10-26 02:25:48',NULL),(2,2,2,'2026-10-21','2026-10-23','2026-10-21 14:10:00',NULL,2,'Khách nhận phòng đúng giờ','DANG_O','2026-10-26 02:25:48',NULL),(3,3,3,'2026-10-22','2026-10-25','2026-10-22 14:00:00',NULL,4,'Gia đình lưu trú 3 ngày','DANG_O','2026-10-26 02:25:48',NULL),(4,4,4,'2026-10-23','2026-10-25',NULL,NULL,2,'Khách đặt trước','DAT_TRUOC','2026-10-26 02:25:48',NULL),(5,5,5,'2026-10-24','2026-10-27','2026-11-05 15:35:37',NULL,3,'Khách đặt phòng VIP','DANG_O','2026-10-26 02:25:48',NULL),(6,6,6,'2026-10-18','2026-10-20','2026-10-18 14:00:00','2026-10-20 11:00:00',1,'Khách đã trả phòng','DA_TRA','2026-10-26 02:25:48',NULL),(7,7,7,'2026-10-19','2026-10-21','2026-10-19 14:00:00','2026-10-21 10:30:00',2,'Khách đã hoàn tất lưu trú','DA_TRA','2026-10-26 02:25:48',NULL),(8,8,8,'2026-10-25','2026-10-27',NULL,NULL,2,'Khách đã hủy đặt phòng','DA_HUY','2026-10-26 02:25:48',NULL),(9,9,9,'2026-10-26','2026-10-29',NULL,NULL,3,'Khách đặt phòng cho chuyến công tác','DAT_TRUOC','2026-10-26 02:25:48',NULL),(10,10,10,'2026-10-27','2026-10-30','2026-11-05 15:24:55',NULL,4,'Khách đặt phòng gia đình','DANG_O','2026-10-26 02:25:48',NULL);
/*!40000 ALTER TABLE `dat_phong` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dich_vu`
--

DROP TABLE IF EXISTS `dich_vu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dich_vu` (
  `ma_dich_vu` bigint NOT NULL AUTO_INCREMENT,
  `ten_dich_vu` varchar(100) NOT NULL,
  `don_gia` decimal(12,0) NOT NULL,
  `duong_dan_anh` varchar(255) DEFAULT NULL,
  `dang_hoat_dong` tinyint(1) NOT NULL DEFAULT '1',
  `ngay_tao` datetime DEFAULT CURRENT_TIMESTAMP,
  `ngay_sua` datetime DEFAULT NULL,
  PRIMARY KEY (`ma_dich_vu`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dich_vu`
--

LOCK TABLES `dich_vu` WRITE;
/*!40000 ALTER TABLE `dich_vu` DISABLE KEYS */;
INSERT INTO `dich_vu` VALUES (1,'Giặt ủi quần áo',50000,'images/giat-ui.jpg',1,'2026-10-26 02:25:48',NULL),(2,'Dịch vụ đưa đón sân bay',250000,'images/dua-don-san-bay.jpg',1,'2026-10-26 02:25:48',NULL),(3,'Ăn sáng tại phòng',100000,'images/an-sang.jpg',1,'2026-10-26 02:25:48',NULL),(4,'Thuê xe máy',150000,'images/thue-xe-may.jpg',1,'2026-10-26 02:25:48',NULL),(5,'Dịch vụ spa và massage',300000,'images/spa-massage.jpg',1,'2026-10-26 02:25:48',NULL);
/*!40000 ALTER TABLE `dich_vu` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `hoa_don`
--

DROP TABLE IF EXISTS `hoa_don`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hoa_don` (
  `ma_hoa_don` bigint NOT NULL AUTO_INCREMENT,
  `ma_dat_phong` bigint NOT NULL,
  `ma_khuyen_mai` bigint DEFAULT NULL,
  `tien_phong` decimal(12,0) NOT NULL DEFAULT '0',
  `tien_dich_vu` decimal(12,0) NOT NULL DEFAULT '0',
  `tien_phat` decimal(12,0) NOT NULL DEFAULT '0',
  `tien_giam` decimal(12,0) NOT NULL DEFAULT '0',
  `tong_tien` decimal(12,0) NOT NULL,
  `trang_thai_thanh_toan` enum('CHUA_THANH_TOAN','THANH_TOAN_MOT_PHAN','DA_THANH_TOAN','DA_HUY') NOT NULL DEFAULT 'CHUA_THANH_TOAN',
  `ngay_tao` datetime DEFAULT CURRENT_TIMESTAMP,
  `ngay_sua` datetime DEFAULT NULL,
  `ma_nhan_vien` bigint DEFAULT NULL,
  PRIMARY KEY (`ma_hoa_don`),
  KEY `ma_dat_phong` (`ma_dat_phong`),
  KEY `ma_khuyen_mai` (`ma_khuyen_mai`),
  KEY `fk_hoa_don_nhan_vien` (`ma_nhan_vien`),
  CONSTRAINT `fk_hoa_don_nhan_vien` FOREIGN KEY (`ma_nhan_vien`) REFERENCES `nhan_vien` (`ma_nhan_vien`),
  CONSTRAINT `hoa_don_ibfk_1` FOREIGN KEY (`ma_dat_phong`) REFERENCES `dat_phong` (`ma_dat_phong`),
  CONSTRAINT `hoa_don_ibfk_2` FOREIGN KEY (`ma_khuyen_mai`) REFERENCES `khuyen_mai` (`ma_khuyen_mai`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `hoa_don`
--

LOCK TABLES `hoa_don` WRITE;
/*!40000 ALTER TABLE `hoa_don` DISABLE KEYS */;
INSERT INTO `hoa_don` VALUES (1,1,1,900000,200000,0,90000,1010000,'THANH_TOAN_MOT_PHAN','2026-10-26 02:25:48',NULL,3),(2,2,2,1300000,250000,0,195000,1355000,'DA_THANH_TOAN','2026-10-26 02:25:48',NULL,3),(3,3,3,2850000,300000,0,570000,2580000,'CHUA_THANH_TOAN','2026-10-26 02:25:48',NULL,3),(4,4,4,2400000,150000,0,600000,1950000,'CHUA_THANH_TOAN','2026-10-26 02:25:48',NULL,4),(5,5,5,5400000,300000,0,1620000,4080000,'DA_THANH_TOAN','2026-10-26 02:25:48',NULL,4),(6,6,6,900000,100000,50000,50000,1000000,'DA_THANH_TOAN','2026-10-26 02:25:48',NULL,4),(7,7,7,1300000,150000,0,100000,1350000,'DA_THANH_TOAN','2026-10-26 02:25:48',NULL,1),(8,8,8,1900000,0,0,200000,1700000,'CHUA_THANH_TOAN','2026-10-26 02:25:48',NULL,1),(9,9,9,3600000,250000,0,300000,3550000,'CHUA_THANH_TOAN','2026-10-26 02:25:48',NULL,1),(10,10,10,5400000,300000,0,300000,5400000,'DA_THANH_TOAN','2026-10-26 02:25:48',NULL,1);
/*!40000 ALTER TABLE `hoa_don` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `khach_hang`
--

DROP TABLE IF EXISTS `khach_hang`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `khach_hang` (
  `ma_khach_hang` bigint NOT NULL AUTO_INCREMENT,
  `ho_ten` varchar(30) NOT NULL,
  `so_CCCD` varchar(12) DEFAULT NULL,
  `email` varchar(100) NOT NULL,
  `SDT` varchar(15) DEFAULT NULL,
  `duong_dan_anh` varchar(255) DEFAULT NULL,
  `dang_hoat_dong` tinyint(1) NOT NULL DEFAULT '1',
  `ngay_tao` datetime DEFAULT NULL,
  `ngay_sua` datetime DEFAULT NULL,
  PRIMARY KEY (`ma_khach_hang`),
  UNIQUE KEY `email` (`email`),
  UNIQUE KEY `so_CCCD` (`so_CCCD`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `khach_hang`
--

LOCK TABLES `khach_hang` WRITE;
/*!40000 ALTER TABLE `khach_hang` DISABLE KEYS */;
INSERT INTO `khach_hang` VALUES (1,'Nguyễn Văn An','079201000001','an.nguyen@gmail.com','0901111111',NULL,1,'2026-10-26 02:25:48',NULL),(2,'Trần Thị Bình','079201000002','binh.tran@gmail.com','0902222222',NULL,1,'2026-10-26 02:25:48',NULL),(3,'Lê Hoàng Nam','079201000003','nam.le@gmail.com','0903333333',NULL,1,'2026-10-26 02:25:48',NULL),(4,'Phạm Thị Hoa','079201000004','hoa.pham@gmail.com','0904444444',NULL,1,'2026-10-26 02:25:48',NULL),(5,'Đỗ Minh Tuấn','079201000005','tuan.do@gmail.com','0905555555',NULL,1,'2026-10-26 02:25:48',NULL),(6,'Võ Ngọc Lan','079201000006','lan.vo@gmail.com','0906666666',NULL,1,'2026-10-26 02:25:48',NULL),(7,'Bùi Quốc Huy','079201000007','huy.bui@gmail.com','0907777777',NULL,1,'2026-10-26 02:25:48',NULL),(8,'Đặng Thùy Linh','079201000008','linh.dang@gmail.com','0908888888',NULL,1,'2026-10-26 02:25:48',NULL),(9,'Hồ Thanh Phong','079201000009','phong.ho@gmail.com','0909999999',NULL,1,'2026-10-26 02:25:48',NULL),(10,'Ngô Mỹ Duyên','079201000010','duyen.ngo@gmail.com','0910000000',NULL,1,'2026-10-26 02:25:48',NULL);
/*!40000 ALTER TABLE `khach_hang` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `khuyen_mai`
--

DROP TABLE IF EXISTS `khuyen_mai`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `khuyen_mai` (
  `ma_khuyen_mai` bigint NOT NULL AUTO_INCREMENT,
  `ma_khuyen_mai_code` varchar(30) NOT NULL,
  `ten_khuyen_mai` varchar(100) NOT NULL,
  `loai_giam` enum('PHAN_TRAM','SO_TIEN') NOT NULL,
  `gia_tri_giam` decimal(12,0) NOT NULL,
  `ngay_bat_dau` date NOT NULL,
  `ngay_ket_thuc` date NOT NULL,
  `dang_hoat_dong` tinyint(1) NOT NULL DEFAULT '1',
  `ngay_tao` datetime DEFAULT CURRENT_TIMESTAMP,
  `ngay_sua` datetime DEFAULT NULL,
  PRIMARY KEY (`ma_khuyen_mai`),
  UNIQUE KEY `ma_khuyen_mai_code` (`ma_khuyen_mai_code`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `khuyen_mai`
--

LOCK TABLES `khuyen_mai` WRITE;
/*!40000 ALTER TABLE `khuyen_mai` DISABLE KEYS */;
INSERT INTO `khuyen_mai` VALUES (1,'WELCOME10','Chào mừng khách hàng mới','PHAN_TRAM',10,'2026-01-01','2027-12-31',1,'2026-10-26 02:25:48',NULL),(2,'SUMMER15','Ưu đãi mùa hè','PHAN_TRAM',15,'2026-05-01','2026-08-31',1,'2026-10-26 02:25:48',NULL),(3,'WEEKEND20','Ưu đãi cuối tuần','PHAN_TRAM',20,'2026-01-01','2027-12-31',1,'2026-10-26 02:25:48',NULL),(4,'HOLIDAY25','Ưu đãi ngày lễ','PHAN_TRAM',25,'2026-01-01','2027-12-31',1,'2026-10-26 02:25:48',NULL),(5,'VIP30','Ưu đãi khách hàng VIP','PHAN_TRAM',30,'2026-01-01','2027-12-31',1,'2026-10-26 02:25:48',NULL),(6,'OFF50000','Giảm trực tiếp 50.000 đồng','SO_TIEN',50000,'2026-01-01','2027-12-31',1,'2026-10-26 02:25:48',NULL),(7,'OFF100000','Giảm trực tiếp 100.000 đồng','SO_TIEN',100000,'2026-01-01','2027-12-31',1,'2026-10-26 02:25:48',NULL),(8,'OFF200000','Giảm trực tiếp 200.000 đồng','SO_TIEN',200000,'2026-01-01','2027-12-31',1,'2026-10-26 02:25:48',NULL),(9,'EARLYBIRD','Ưu đãi đặt phòng sớm','PHAN_TRAM',12,'2026-01-01','2027-12-31',1,'2026-10-26 02:25:48',NULL),(10,'LONGSTAY','Ưu đãi khách lưu trú dài ngày','SO_TIEN',300000,'2026-01-01','2027-12-31',1,'2026-10-26 02:25:48',NULL);
/*!40000 ALTER TABLE `khuyen_mai` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `loai_phong`
--

DROP TABLE IF EXISTS `loai_phong`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `loai_phong` (
  `ma_loai_phong` bigint NOT NULL AUTO_INCREMENT,
  `ten_loai_phong` varchar(50) NOT NULL,
  `gia_co_ban` decimal(12,0) NOT NULL,
  `so_nguoi_toi_da` int NOT NULL,
  `duong_dan_anh` varchar(100) DEFAULT NULL,
  `dang_hoat_dong` tinyint(1) NOT NULL DEFAULT '1',
  `ngay_tao` datetime DEFAULT CURRENT_TIMESTAMP,
  `ngay_sua` datetime DEFAULT NULL,
  PRIMARY KEY (`ma_loai_phong`),
  UNIQUE KEY `ten_loai_phong` (`ten_loai_phong`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `loai_phong`
--

LOCK TABLES `loai_phong` WRITE;
/*!40000 ALTER TABLE `loai_phong` DISABLE KEYS */;
INSERT INTO `loai_phong` VALUES (1,'Single',450000,1,'images/single.jpg',1,'2026-10-26 02:25:47',NULL),(2,'Double',650000,2,'images/double.jpg',1,'2026-10-26 02:25:47',NULL),(3,'Family',950000,4,'images/family.jpg',1,'2026-10-26 02:25:47',NULL),(4,'Deluxe',1200000,3,'images/deluxe.jpg',1,'2026-10-26 02:25:47',NULL),(5,'VIP',1800000,4,'images/vip.jpg',1,'2026-10-26 02:25:47',NULL);
/*!40000 ALTER TABLE `loai_phong` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `nhan_vien`
--

DROP TABLE IF EXISTS `nhan_vien`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `nhan_vien` (
  `ma_nhan_vien` bigint NOT NULL AUTO_INCREMENT,
  `ten_dang_nhap` varchar(30) NOT NULL,
  `email` varchar(100) NOT NULL,
  `mat_khau` varchar(255) NOT NULL,
  `SDT` varchar(10) DEFAULT NULL,
  `ho_ten` varchar(30) NOT NULL,
  `vai_tro` enum('ADMIN','MANAGER','RECEPTION') NOT NULL,
  `duong_dan_anh` varchar(255) DEFAULT NULL,
  `dang_hoat_dong` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`ma_nhan_vien`),
  UNIQUE KEY `ten_dang_nhap` (`ten_dang_nhap`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `nhan_vien`
--

LOCK TABLES `nhan_vien` WRITE;
/*!40000 ALTER TABLE `nhan_vien` DISABLE KEYS */;
INSERT INTO `nhan_vien` VALUES (1,'quocthai_admin','quocthai.admin@hotel.vn','$2a$10$4iDZeSoVuS7C74JYJkcyiet5clIbH1ahf6xj6HpsjrOUiPfR68K3O','0901234567','Phí Quốc Thái','ADMIN','/api/staff/images/1793723901923_6ff25477.png',1),(2,'thimai_mgr','thimai.mgr@hotel.vn','$2a$10$4iDZeSoVuS7C74JYJkcyiet5clIbH1ahf6xj6HpsjrOUiPfR68K3O','0912345678','Nguyễn Thị Mai','MANAGER','/api/staff/images/1793723999200_2fefd076.jpg',1),(3,'vanhung_rec','vanhung.rec@hotel.vn','$2a$10$4iDZeSoVuS7C74JYJkcyiet5clIbH1ahf6xj6HpsjrOUiPfR68K3O','0923456789','Trần Văn Hùng','RECEPTION','',1),(4,'thithao_rec','thithao.rec@hotel.vn','$2a$10$4iDZeSoVuS7C74JYJkcyiet5clIbH1ahf6xj6HpsjrOUiPfR68K3O','0934567890','Lê Thị Thảo','RECEPTION','',1),(5,'manager_01','mgr01@hotel.vn','$2a$10$GIjcI0lkO35MfwoDO/dlmOr73S1IqgFseQ7ezL.iVYJmrVzo39oRm','0900000001','Lê Văn Quản','MANAGER',NULL,1),(6,'manager_02','mgr02@hotel.vn','$2a$10$GIjcI0lkO35MfwoDO/dlmOr73S1IqgFseQ7ezL.iVYJmrVzo39oRm','0900000002','Nguyễn Thị Lý','MANAGER',NULL,1),(7,'manager_03','mgr03@hotel.vn','$2a$10$GIjcI0lkO35MfwoDO/dlmOr73S1IqgFseQ7ezL.iVYJmrVzo39oRm','0900000003','Trần Đình Trọng','MANAGER',NULL,1),(8,'staff_01','reception01@hotel.vn','$2a$10$gvUXqdWvrKBg4fNU9dvZ9upqcTf71ateKiu.US6C0TTUUUL4L1J86','0900000004','Phạm Xuân Staff','RECEPTION',NULL,1),(9,'staff_02','reception02@hotel.vn','$2a$10$gvUXqdWvrKBg4fNU9dvZ9upqcTf71ateKiu.US6C0TTUUUL4L1J86','0900000005','Đỗ Minh Tân','RECEPTION',NULL,1),(10,'staff_03','reception03@hotel.vn','$2a$10$gvUXqdWvrKBg4fNU9dvZ9upqcTf71ateKiu.US6C0TTUUUL4L1J86','0900000006','Vũ Ngọc Hân','RECEPTION',NULL,1),(11,'staff_04','reception04@hotel.vn','$2a$10$gvUXqdWvrKBg4fNU9dvZ9upqcTf71ateKiu.US6C0TTUUUL4L1J86','0900000007','Hoàng Kim Hữu','RECEPTION',NULL,1),(12,'staff_05','reception05@hotel.vn','$2a$10$gvUXqdWvrKBg4fNU9dvZ9upqcTf71ateKiu.US6C0TTUUUL4L1J86','0900000008','Ngô Thanh Vân','RECEPTION',NULL,1),(13,'staff_06','reception06@hotel.vn','$2a$10$gvUXqdWvrKBg4fNU9dvZ9upqcTf71ateKiu.US6C0TTUUUL4L1J86','0900000009','Lý Hải Nhai','RECEPTION',NULL,1),(14,'staff_07','reception07@hotel.vn','$2a$10$gvUXqdWvrKBg4fNU9dvZ9upqcTf71ateKiu.US6C0TTUUUL4L1J86','0900000010','Trịnh Quốc Bảo','RECEPTION',NULL,1);
/*!40000 ALTER TABLE `nhan_vien` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `phan_ca`
--

DROP TABLE IF EXISTS `phan_ca`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `phan_ca` (
  `ma_phan_ca` bigint NOT NULL AUTO_INCREMENT,
  `ma_nhan_vien` bigint NOT NULL,
  `ma_ca` bigint NOT NULL,
  `ngay_lam_viec` date NOT NULL,
  `ngay_tao` datetime DEFAULT CURRENT_TIMESTAMP,
  `ngay_sua` datetime DEFAULT NULL,
  PRIMARY KEY (`ma_phan_ca`),
  UNIQUE KEY `uk_phan_ca` (`ma_nhan_vien`,`ma_ca`,`ngay_lam_viec`),
  UNIQUE KEY `UKq31o0xi0popup0nco8x7h6vy7` (`ma_nhan_vien`,`ma_ca`,`ngay_lam_viec`),
  KEY `ma_ca` (`ma_ca`),
  CONSTRAINT `phan_ca_ibfk_1` FOREIGN KEY (`ma_nhan_vien`) REFERENCES `nhan_vien` (`ma_nhan_vien`),
  CONSTRAINT `phan_ca_ibfk_2` FOREIGN KEY (`ma_ca`) REFERENCES `ca_lam_viec` (`ma_ca`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `phan_ca`
--

LOCK TABLES `phan_ca` WRITE;
/*!40000 ALTER TABLE `phan_ca` DISABLE KEYS */;
INSERT INTO `phan_ca` VALUES (1,3,1,'2026-10-24','2026-10-26 02:25:48',NULL),(2,4,2,'2026-10-24','2026-10-26 02:25:48',NULL),(3,5,1,'2026-10-24','2026-10-26 02:25:48',NULL),(4,6,2,'2026-10-24','2026-10-26 02:25:48',NULL),(5,7,3,'2026-10-24','2026-10-26 02:25:48',NULL),(6,8,1,'2026-10-25','2026-10-26 02:25:48',NULL),(7,9,2,'2026-10-25','2026-10-26 02:25:48',NULL),(8,10,3,'2026-10-25','2026-10-26 02:25:48',NULL),(9,11,1,'2026-10-25','2026-10-26 02:25:48',NULL),(10,12,2,'2026-10-25','2026-10-26 02:25:48',NULL);
/*!40000 ALTER TABLE `phan_ca` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `phong`
--

DROP TABLE IF EXISTS `phong`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `phong` (
  `ma_phong` bigint NOT NULL AUTO_INCREMENT,
  `ma_loai_phong` bigint NOT NULL,
  `so_phong` varchar(5) NOT NULL,
  `tang` int DEFAULT NULL,
  `trang_thai` enum('TRONG','DANG_DON','DANG_O','DANG_BAO_TRI') NOT NULL DEFAULT 'TRONG',
  `ghi_chu` text,
  `dang_hoat_dong` tinyint(1) NOT NULL DEFAULT '1',
  `ngay_tao` datetime DEFAULT CURRENT_TIMESTAMP,
  `ngay_sua` datetime DEFAULT NULL,
  PRIMARY KEY (`ma_phong`),
  UNIQUE KEY `so_phong` (`so_phong`),
  KEY `ma_loai_phong` (`ma_loai_phong`),
  CONSTRAINT `phong_ibfk_1` FOREIGN KEY (`ma_loai_phong`) REFERENCES `loai_phong` (`ma_loai_phong`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `phong`
--

LOCK TABLES `phong` WRITE;
/*!40000 ALTER TABLE `phong` DISABLE KEYS */;
INSERT INTO `phong` VALUES (1,1,'103',1,'TRONG','Phòng Single',1,'2026-10-26 02:25:47',NULL),(2,2,'203',2,'TRONG','Phòng Double có ban công',1,'2026-10-26 02:25:47',NULL),(3,3,'302',3,'TRONG','Phòng Family rộng rãi',1,'2026-10-26 02:25:47',NULL),(4,4,'401',4,'TRONG','Phòng Deluxe view thành phố',1,'2026-10-26 02:25:47',NULL),(5,5,'402',4,'DANG_O','Phòng VIP cao cấp',1,'2026-10-26 02:25:47',NULL),(6,1,'104',1,'DANG_DON','Phòng đang dọn vệ sinh',1,'2026-10-26 02:25:47',NULL),(7,2,'204',2,'DANG_O','Phòng đang có khách lưu trú',1,'2026-10-26 02:25:47',NULL),(8,3,'303',3,'TRONG','Phòng Family dành cho gia đình',1,'2026-10-26 02:25:47',NULL),(9,4,'403',4,'DANG_BAO_TRI','Phòng đang bảo trì',1,'2026-10-26 02:25:47',NULL),(10,5,'404',4,'DANG_O','Phòng VIP có tiện nghi cao cấp',1,'2026-10-26 02:25:47',NULL);
/*!40000 ALTER TABLE `phong` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `phuong_thuc_thanh_toan`
--

DROP TABLE IF EXISTS `phuong_thuc_thanh_toan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `phuong_thuc_thanh_toan` (
  `ma_phuong_thuc` bigint NOT NULL AUTO_INCREMENT,
  `ten_phuong_thuc` varchar(50) NOT NULL,
  `anh_qr` varchar(255) DEFAULT NULL,
  `ten_ngan_hang` varchar(100) DEFAULT NULL,
  `so_tai_khoan` varchar(30) DEFAULT NULL,
  `ten_chu_tai_khoan` varchar(100) DEFAULT NULL,
  `dang_hoat_dong` tinyint(1) NOT NULL DEFAULT '1',
  `ngay_tao` datetime DEFAULT CURRENT_TIMESTAMP,
  `ngay_sua` datetime DEFAULT NULL,
  PRIMARY KEY (`ma_phuong_thuc`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `phuong_thuc_thanh_toan`
--

LOCK TABLES `phuong_thuc_thanh_toan` WRITE;
/*!40000 ALTER TABLE `phuong_thuc_thanh_toan` DISABLE KEYS */;
INSERT INTO `phuong_thuc_thanh_toan` VALUES (1,'Tiền mặt',NULL,NULL,NULL,NULL,1,'2026-10-26 02:25:48',NULL),(2,'Ví MoMo',NULL,NULL,NULL,NULL,1,'2026-10-26 02:25:48',NULL),(3,'Chuyển khoản ngân hàng','images/qr-ngan-hang.png','Vietcombank','0123456789','KHACH SAN ABC',1,'2026-10-26 02:25:48',NULL);
/*!40000 ALTER TABLE `phuong_thuc_thanh_toan` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `thanh_toan`
--

DROP TABLE IF EXISTS `thanh_toan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `thanh_toan` (
  `ma_thanh_toan` bigint NOT NULL AUTO_INCREMENT,
  `ma_hoa_don` bigint NOT NULL,
  `ma_phuong_thuc` bigint NOT NULL,
  `so_tien` decimal(12,0) NOT NULL,
  `ma_giao_dich` varchar(100) DEFAULT NULL,
  `trang_thai` enum('DANG_XU_LY','THANH_CONG','THAT_BAI') NOT NULL DEFAULT 'DANG_XU_LY',
  `thoi_gian_thanh_toan` datetime DEFAULT NULL,
  `ghi_chu` varchar(255) DEFAULT NULL,
  `ma_nhan_vien` bigint DEFAULT NULL,
  `ngay_tao` datetime DEFAULT CURRENT_TIMESTAMP,
  `ngay_sua` datetime DEFAULT NULL,
  PRIMARY KEY (`ma_thanh_toan`),
  KEY `idx_tt_hoa_don` (`ma_hoa_don`),
  KEY `idx_tt_phuong_thuc` (`ma_phuong_thuc`),
  KEY `fk_thanh_toan_nhan_vien` (`ma_nhan_vien`),
  CONSTRAINT `tt_ibfk_1` FOREIGN KEY (`ma_hoa_don`) REFERENCES `hoa_don` (`ma_hoa_don`),
  CONSTRAINT `tt_ibfk_2` FOREIGN KEY (`ma_phuong_thuc`) REFERENCES `phuong_thuc_thanh_toan` (`ma_phuong_thuc`),
  CONSTRAINT `fk_thanh_toan_nhan_vien` FOREIGN KEY (`ma_nhan_vien`) REFERENCES `nhan_vien` (`ma_nhan_vien`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `thanh_toan`
--

LOCK TABLES `thanh_toan` WRITE;
/*!40000 ALTER TABLE `thanh_toan` DISABLE KEYS */;
INSERT INTO `thanh_toan` VALUES (1,1,1,1000000,NULL,'THANH_CONG','2026-10-22 10:30:00','Đặt cọc trước phòng 101',1,'2026-10-26 02:25:48',NULL),(2,2,2,1500000,'MM202610230001','THANH_CONG','2026-10-23 09:15:00','Thanh toán qua ví MoMo',1,'2026-10-26 02:25:48',NULL),(3,3,3,2000000,'CK202610230002','THANH_CONG','2026-10-23 11:20:00','Chuyển khoản Vietcombank',1,'2026-10-26 02:25:48',NULL),(4,4,1,650000,NULL,'THANH_CONG','2026-10-24 08:00:00','Thu tiền cọc',1,'2026-10-26 02:25:48',NULL),(5,5,2,950000,'MM202610240003','DANG_XU_LY',NULL,'Chờ xác nhận MoMo',1,'2026-10-26 02:25:48',NULL),(6,6,3,1200000,'CK202610240004','THAT_BAI',NULL,'Lỗi giao dịch ngân hàng',1,'2026-10-26 02:25:48',NULL),(7,7,1,1800000,NULL,'THANH_CONG','2026-10-24 14:30:00','Thanh toán toàn bộ tiền mặt',1,'2026-10-26 02:25:48',NULL),(8,8,2,450000,'MM202610250005','THANH_CONG','2026-10-25 10:10:00','Thu cọc',1,'2026-10-26 02:25:48',NULL),(9,9,3,750000,'CK202610250006','DANG_XU_LY',NULL,'Chuyển khoản đang kiểm tra',1,'2026-10-26 02:25:48',NULL),(10,10,1,500000,NULL,'THANH_CONG','2026-10-25 16:00:00','Đặt cọc ban đầu',1,'2026-10-26 02:25:48',NULL);
/*!40000 ALTER TABLE `thanh_toan` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-11-11  9:53:39
