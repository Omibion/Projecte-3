-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Servidor: 127.0.0.1
-- Temps de generació: 29-05-2025 a les 16:04:43
-- Versió del servidor: 10.4.28-MariaDB
-- Versió de PHP: 8.2.4

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Base de dades: `risk`
--

-- --------------------------------------------------------

--
-- Estructura de la taula `carta`
--

CREATE TABLE `carta` (
  `carta_id` int(11) DEFAULT NULL,
  `id` bigint(20) NOT NULL,
  `tipus` enum('ARTILLERIA','CABALLERIA','COMODIN','INFANTERIA') DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Estructura de la taula `continent`
--

CREATE TABLE `continent` (
  `id` int(11) NOT NULL,
  `reforç` int(11) DEFAULT NULL,
  `nom` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Bolcament de dades per a la taula `continent`
--

INSERT INTO `continent` (`id`, `reforç`, `nom`) VALUES
(1, 5, 'North America'),
(2, 2, 'South America'),
(3, 5, 'Europe'),
(4, 3, 'Africa'),
(5, 7, 'Asia'),
(6, 2, 'Oceania');

-- --------------------------------------------------------

--
-- Estructura de la taula `frontera`
--

CREATE TABLE `frontera` (
  `pais1_id` bigint(20) NOT NULL,
  `pais2_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Bolcament de dades per a la taula `frontera`
--

INSERT INTO `frontera` (`pais1_id`, `pais2_id`) VALUES
(1, 2),
(1, 3),
(1, 30),
(2, 3),
(2, 4),
(2, 9),
(3, 4),
(3, 6),
(4, 5),
(4, 6),
(4, 7),
(4, 9),
(5, 7),
(5, 9),
(6, 7),
(6, 8),
(7, 8),
(8, 10),
(9, 14),
(10, 11),
(10, 12),
(11, 12),
(11, 13),
(12, 13),
(12, 21),
(13, 21),
(14, 15),
(14, 16),
(15, 16),
(15, 17),
(15, 18),
(16, 17),
(16, 20),
(17, 18),
(17, 19),
(17, 20),
(18, 19),
(18, 21),
(19, 20),
(19, 21),
(19, 22),
(19, 36),
(20, 27),
(20, 34),
(20, 36),
(21, 18),
(21, 22),
(21, 23),
(21, 24),
(21, 34),
(22, 23),
(22, 36),
(23, 24),
(23, 25),
(23, 26),
(23, 36),
(24, 25),
(25, 26),
(27, 28),
(27, 34),
(27, 35),
(28, 29),
(28, 31),
(28, 32),
(28, 35),
(29, 30),
(29, 31),
(30, 31),
(30, 33),
(31, 32),
(32, 33),
(32, 35),
(33, 39),
(34, 35),
(34, 36),
(34, 37),
(35, 37),
(35, 38),
(36, 37),
(37, 38),
(38, 39),
(38, 40),
(39, 40),
(39, 41),
(40, 42),
(41, 42);

-- --------------------------------------------------------

--
-- Estructura de la taula `jugador`
--

CREATE TABLE `jugador` (
  `estado` bit(1) DEFAULT NULL,
  `partida_id` int(11) DEFAULT NULL,
  `user_id` int(11) DEFAULT NULL,
  `id` bigint(20) NOT NULL,
  `colors` varchar(255) DEFAULT NULL,
  `nombre` varchar(255) DEFAULT NULL,
  `token` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Bolcament de dades per a la taula `jugador`
--

INSERT INTO `jugador` (`estado`, `partida_id`, `user_id`, `id`, `colors`, `nombre`, `token`) VALUES
(b'1', 201, 3, 380, 'VERMELL', 'test3', '708f45dd-a8c5-400a-abc4-fc9d18021e20'),
(b'1', 201, 1, 381, 'VERD', 'test1', '96b8c158-bf32-4ecb-ab41-84de2e2019f7'),
(b'1', 203, 3, 384, 'VERMELL', 'test3', '20dec8d7-16d1-4118-98e4-3143dd54ae87'),
(b'1', 208, 3, 393, 'BLAU', 'test3', 'dabead47-c61f-4a30-aeb4-fa4a440161d9'),
(b'1', 208, 1, 394, 'VERD', 'test1', 'a311eae2-54fb-4280-a4f0-7c6c77a1f6e4'),
(b'1', 208, 2, 395, 'VIOLETA', 'test2', 'a0864a34-f906-4349-b93a-edda3b61b503'),
(b'1', 210, 1, 397, 'GROC', 'test1', '0fb7070d-6de9-47d9-8f30-f50bebe493be'),
(b'1', 210, 2, 398, 'VERD', 'test2', '91879119-b508-4e71-bf31-a1f13abc09b7'),
(b'0', 211, 3, 399, NULL, 'test3', '7c0d3e39-6145-44eb-bff8-5e998e056136'),
(b'0', 212, 3, 400, NULL, 'test3', '7c0d3e39-6145-44eb-bff8-5e998e056136'),
(b'1', 214, 2, 402, 'BLAU', 'test2', '51441f46-3451-4a2c-8ad0-f238b4603d4f'),
(b'1', 214, 1, 403, 'VERD', 'test1', '69438558-abae-4956-8aba-c5385bdeb796'),
(b'1', 214, 3, 404, 'VERMELL', 'test3', '1dc6f147-7024-48a9-a799-7ae4e7f5cc8c'),
(b'1', 222, 1, 416, 'VERMELL', 'test1', '576e48ed-4790-4bdf-80ed-9fc672929112'),
(b'1', 222, 2, 417, 'VERD', 'test2', '6b93ac25-b8eb-4c86-8eef-ddc6814b058e'),
(b'1', 227, 3, 424, 'VERMELL', 'test3', '0ef35a5b-1a12-419b-8f10-cbc1cf1b9ad4'),
(b'1', 228, 1, 425, 'VIOLETA', 'test1', 'faeb1f61-3f18-41ea-91a6-f37050ed4bc0'),
(b'1', 228, 2, 426, 'GROC', 'test2', 'c1970327-3dac-4c0f-aeeb-d37d6c08e734'),
(b'1', 228, 3, 427, 'BLAU', 'test3', 'e36dd57f-20be-4e63-800d-912199c81572'),
(b'1', 233, 1, 435, 'TARONJA', 'test1', '43a7ef59-3a2c-46c2-bf89-1a228462ca36'),
(b'1', 233, 3, 436, 'GROC', 'test3', '26c8370c-2dda-444c-91e8-962203ba1c10'),
(b'1', 234, 2, 438, 'VERMELL', 'test2', 'd56d18be-fd99-4f4b-88a7-930a5a60f1fa'),
(b'1', 234, 3, 439, 'BLAU', 'test3', '7e58115d-e435-4e31-b767-01ed9b49e257'),
(b'1', 236, 1, 442, 'BLAU', 'test1', 'ad0df228-20d5-4d27-979c-dec51c29051e'),
(b'1', 238, 2, 447, 'BLAU', 'test2', 'e75c073c-ee45-4734-a013-9a67a353598c'),
(b'1', 238, 1, 448, 'VERMELL', 'test1', '6f9784ce-c0a6-4d7e-bc7e-9fbe22be9646'),
(b'0', 238, 1, 452, NULL, 'test1', 'c11fe32c-65b0-4bcd-9586-eff3d5095d14'),
(b'1', 241, 2, 454, 'VERMELL', 'test2', '1886f34f-6a68-424c-9f03-c45b96a60b28'),
(b'1', 241, 1, 455, 'BLAU', 'test1', '61ea977f-8884-4a6b-a254-960f34e0e413'),
(b'0', 242, 3, 456, NULL, 'test3', 'a1e7c22f-9c27-41f6-9b79-e13c8211b788'),
(b'0', 242, 1, 457, NULL, 'test1', '3b2f7c98-5159-4468-a0f8-241e2f09c72d'),
(b'0', 248, 1, 470, 'VERMELL', 'test1', 'd4785dfd-e773-4f43-b445-f547983162d7'),
(b'0', 249, 2, 472, NULL, 'test2', '6ec19081-ae53-4f57-9a88-55482e86a8cb'),
(b'1', 254, 3, 480, 'VERMELL', 'test3', 'f8199b20-e868-42e9-ad54-5953d0daaa9d'),
(b'1', 254, 1, 484, 'VIOLETA', 'test1', '8bda54e4-da36-462e-9eea-81441cc3dcb6'),
(b'1', 259, 3, 493, 'VIOLETA', 'test3', '4e15a13b-a0e3-439c-a85c-a7b96f2f7214'),
(b'1', 260, 3, 495, 'VERMELL', 'test3', 'b700c2e0-2300-408f-8516-ca7656f67fa6');

-- --------------------------------------------------------

--
-- Estructura de la taula `ma`
--

CREATE TABLE `ma` (
  `id` int(11) NOT NULL,
  `jugador_id` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Estructura de la taula `okupa`
--

CREATE TABLE `okupa` (
  `jugador_id` bigint(20) NOT NULL,
  `pais_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Bolcament de dades per a la taula `okupa`
--

INSERT INTO `okupa` (`jugador_id`, `pais_id`) VALUES
(393, 2),
(393, 3),
(393, 6),
(393, 10),
(393, 13),
(393, 16),
(393, 17),
(393, 19),
(393, 25),
(393, 28),
(393, 31),
(393, 36),
(393, 38),
(393, 40),
(394, 1),
(394, 4),
(394, 7),
(394, 12),
(394, 15),
(394, 18),
(394, 20),
(394, 21),
(394, 26),
(394, 29),
(394, 32),
(394, 34),
(394, 37),
(394, 41),
(395, 5),
(395, 8),
(395, 9),
(395, 11),
(395, 14),
(395, 22),
(395, 23),
(395, 24),
(395, 27),
(395, 30),
(395, 33),
(395, 35),
(395, 39),
(395, 42),
(397, 2),
(397, 4),
(397, 6),
(397, 8),
(397, 9),
(397, 12),
(397, 13),
(397, 16),
(397, 17),
(397, 18),
(397, 22),
(397, 24),
(397, 26),
(397, 27),
(397, 30),
(397, 31),
(397, 33),
(397, 36),
(397, 38),
(397, 39),
(397, 42),
(398, 1),
(398, 3),
(398, 5),
(398, 7),
(398, 10),
(398, 11),
(398, 14),
(398, 15),
(398, 19),
(398, 20),
(398, 21),
(398, 23),
(398, 25),
(398, 28),
(398, 29),
(398, 32),
(398, 34),
(398, 35),
(398, 37),
(398, 40),
(398, 41),
(402, 4),
(402, 6),
(402, 9),
(402, 10),
(402, 13),
(402, 16),
(402, 18),
(402, 22),
(402, 25),
(402, 27),
(402, 31),
(402, 32),
(402, 38),
(402, 40),
(403, 1),
(403, 2),
(403, 5),
(403, 12),
(403, 14),
(403, 19),
(403, 20),
(403, 23),
(403, 26),
(403, 28),
(403, 30),
(403, 35),
(403, 36),
(403, 41),
(404, 3),
(404, 7),
(404, 8),
(404, 11),
(404, 15),
(404, 17),
(404, 21),
(404, 24),
(404, 29),
(404, 33),
(404, 34),
(404, 37),
(404, 39),
(404, 42),
(425, 6),
(425, 7),
(426, 7),
(426, 34),
(427, 6),
(427, 35),
(435, 18),
(436, 21),
(438, 22),
(438, 24),
(438, 26),
(438, 37),
(439, 21),
(439, 23),
(439, 25),
(442, 18),
(442, 39),
(447, 1),
(447, 3),
(447, 5),
(447, 7),
(447, 10),
(447, 13),
(447, 15),
(447, 16),
(447, 18),
(447, 19),
(447, 22),
(447, 23),
(447, 26),
(447, 28),
(447, 31),
(447, 32),
(447, 34),
(447, 35),
(447, 38),
(447, 40),
(447, 42),
(448, 2),
(448, 4),
(448, 6),
(448, 8),
(448, 9),
(448, 11),
(448, 12),
(448, 14),
(448, 17),
(448, 20),
(448, 21),
(448, 24),
(448, 25),
(448, 27),
(448, 29),
(448, 30),
(448, 33),
(448, 36),
(448, 37),
(448, 39),
(448, 41),
(454, 14),
(454, 17),
(454, 18),
(455, 15),
(455, 19),
(484, 1),
(484, 2),
(484, 3),
(484, 4),
(484, 5),
(484, 6),
(484, 7),
(484, 8),
(484, 10),
(484, 14),
(484, 15),
(484, 16),
(484, 17),
(484, 20),
(484, 27),
(484, 28),
(484, 29),
(484, 31),
(484, 33),
(484, 34),
(495, 21),
(495, 22);

-- --------------------------------------------------------

--
-- Estructura de la taula `pais`
--

CREATE TABLE `pais` (
  `continent_id` int(11) DEFAULT NULL,
  `id` bigint(20) NOT NULL,
  `nom` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Bolcament de dades per a la taula `pais`
--

INSERT INTO `pais` (`continent_id`, `id`, `nom`) VALUES
(1, 1, 'Alaska'),
(1, 2, 'Northwest Territory'),
(1, 3, 'Alberta'),
(1, 4, 'Ontario'),
(1, 5, 'Quebec'),
(1, 6, 'Western United States'),
(1, 7, 'Eastern United States'),
(1, 8, 'Central America'),
(1, 9, 'Greenland'),
(2, 10, 'Venezuela'),
(2, 11, 'Peru'),
(2, 12, 'Brazil'),
(2, 13, 'Argentina'),
(3, 14, 'Iceland'),
(3, 15, 'Great Britain'),
(3, 16, 'Scandinavia'),
(3, 17, 'Northern Europe'),
(3, 18, 'Western Europe'),
(3, 19, 'Southern Europe'),
(3, 20, 'Ukraine'),
(4, 21, 'North Africa'),
(4, 22, 'Egypt'),
(4, 23, 'East Africa'),
(4, 24, 'Congo'),
(4, 25, 'South Africa'),
(4, 26, 'Madagascar'),
(5, 27, 'Ural'),
(5, 28, 'Siberia'),
(5, 29, 'Yakutsk'),
(5, 30, 'Kamchatka'),
(5, 31, 'Irkutsk'),
(5, 32, 'Mongolia'),
(5, 33, 'Japan'),
(5, 34, 'Afghanistan'),
(5, 35, 'China'),
(5, 36, 'Middle East'),
(5, 37, 'India'),
(5, 38, 'Siam'),
(6, 39, 'Indonesia'),
(6, 40, 'New Guinea'),
(6, 41, 'Western Australia'),
(6, 42, 'Eastern Australia');

-- --------------------------------------------------------

--
-- Estructura de la taula `partida`
--

CREATE TABLE `partida` (
  `admin_id` int(11) DEFAULT NULL,
  `estado` bit(1) DEFAULT NULL,
  `id` int(11) NOT NULL,
  `max_players` int(11) DEFAULT NULL,
  `torn_playes_id` int(11) DEFAULT NULL,
  `date` datetime(6) DEFAULT NULL,
  `nom` varchar(255) DEFAULT NULL,
  `token` varchar(255) DEFAULT NULL,
  `estat_torn` enum('COL_LOCAR_INICIAL','COMBAT','FINAL','RECOL_LOCACIO','REFORC_PAIS','REFORC_TROPES','WAIT') DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Bolcament de dades per a la taula `partida`
--

INSERT INTO `partida` (`admin_id`, `estado`, `id`, `max_players`, `torn_playes_id`, `date`, `nom`, `token`, `estat_torn`) VALUES
(3, b'0', 201, 3, 380, '2025-05-26 17:34:18.000000', 'fsdfdfdsdsf', 'ab6ff589-72bc-44be-8ac0-c7bf235d022b', 'COL_LOCAR_INICIAL'),
(3, b'0', 203, 3, 384, '2025-05-26 17:41:06.000000', 'dfsdsfdsd', '307485ae-4828-47e8-8d10-ff67e47838f5', 'COL_LOCAR_INICIAL'),
(3, b'0', 208, 3, 393, '2025-05-27 16:08:55.000000', 'Nombres', '1ba3ac4e-f97d-482b-bc91-1dd24f79df28', 'COMBAT'),
(1, b'0', 210, 3, 398, '2025-05-27 16:13:41.000000', 'Nombressssaaa', '905a084e-890f-40ab-b274-149d6e09ce21', 'COMBAT'),
(3, b'0', 211, 3, 0, '2025-05-27 16:29:12.000000', 'Nombreqqqqqqqqqqqqqqqqq', 'f735d045-7764-4efa-912c-6d264f663fdf', NULL),
(3, b'0', 212, 3, 0, '2025-05-27 16:29:14.000000', 'Nombreqqqqqqqqqqqqqqqqq', '8a60db91-a92d-4f5d-9ef9-07731589a56a', NULL),
(2, b'0', 214, 3, 404, '2025-05-27 16:29:33.000000', 'Nombrewwww', '642d6204-1827-45a8-b238-00626fb0751f', 'REFORC_TROPES'),
(1, b'0', 222, 3, 416, '2025-05-27 19:10:13.000000', 'Nombre', '213c664c-37b5-4f20-8247-5a1226b9ebc4', 'COMBAT'),
(3, b'0', 227, 3, 0, '2025-05-27 19:19:49.000000', 'fdgfddfsg', '6b39e080-86f7-4219-bf70-662a35b58271', NULL),
(1, b'0', 228, 3, 425, '2025-05-27 19:20:44.000000', 'Nombre', '733c23cb-c3df-4e9c-a62d-245a23fa5e2b', 'COMBAT'),
(1, b'0', 233, 3, 435, '2025-05-27 19:26:47.000000', 'Nombre', 'f017abf8-17f8-407d-9c01-1f838393ec4d', 'COL_LOCAR_INICIAL'),
(10, b'0', 234, 3, 439, '2025-05-27 19:29:03.000000', 'fdfgsdfgd', '302a180f-4d46-49a2-9a08-e352a7492289', 'COL_LOCAR_INICIAL'),
(1, b'0', 236, 3, 442, '2025-05-27 19:33:50.000000', 'Nombre', '1bd62d14-60b2-465a-8df6-9172cf2fb0b9', 'COMBAT'),
(2, b'0', 238, 3, 448, '2025-05-27 19:42:11.000000', 'Nombre', '6d49fb77-03ca-4830-9853-c351f5320162', 'COMBAT'),
(2, b'0', 241, 3, 455, '2025-05-27 19:45:54.000000', 'Nombre', '578b6077-af12-493c-b935-b08442b86c10', 'COL_LOCAR_INICIAL'),
(3, b'0', 242, 3, 0, '2025-05-27 19:46:44.000000', 'dfsadsddfd', '73ba595c-1401-4c68-b7d6-9c6f2d4de82d', NULL),
(2, b'0', 248, 3, 0, '2025-05-27 19:55:32.000000', 'NomdscACcwsdcdcdbre', 'a4a848fc-229f-42bd-8f86-0ff414f1d97c', NULL),
(2, b'0', 249, 3, 0, '2025-05-27 19:58:01.000000', 'efde3wdfefrfcr', '595413ba-a9fc-4a6d-bbf7-775ab44d2748', NULL),
(2, b'0', 254, 3, 480, '2025-05-27 20:12:55.000000', 'dwfwrferwfef', '53a07844-b814-4e9e-9fdd-a27404d255ca', 'COMBAT'),
(3, b'0', 259, 3, 493, '2025-05-29 15:54:25.000000', 'sdfdsd', '3bb997fe-0afd-4d3e-9a61-06c68ff56b10', 'COL_LOCAR_INICIAL'),
(1, b'0', 260, 3, 494, '2025-05-29 15:55:16.000000', 'Nombre', 'ef894a0e-20fd-4e03-b437-ad7987154177', 'COL_LOCAR_INICIAL');

-- --------------------------------------------------------

--
-- Estructura de la taula `usuaris`
--

CREATE TABLE `usuaris` (
  `games` int(11) DEFAULT NULL,
  `id` int(11) NOT NULL,
  `wins` int(11) DEFAULT NULL,
  `avatar` varchar(255) DEFAULT NULL,
  `login` varchar(255) DEFAULT NULL,
  `nom` varchar(255) DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Bolcament de dades per a la taula `usuaris`
--

INSERT INTO `usuaris` (`games`, `id`, `wins`, `avatar`, `login`, `nom`, `password`) VALUES
(0, 1, 0, NULL, 'test1', 'test1', '81DC9BDB52D04DC20036DBD8313ED055'),
(0, 2, 0, NULL, 'test2', 'test2', '81DC9BDB52D04DC20036DBD8313ED055'),
(0, 3, 0, NULL, 'test3', 'test3', '81DC9BDB52D04DC20036DBD8313ED055');

--
-- Índexs per a les taules bolcades
--

--
-- Índexs per a la taula `carta`
--
ALTER TABLE `carta`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FK1jip98d2hrtb9a2o10osjq1fo` (`carta_id`);

--
-- Índexs per a la taula `continent`
--
ALTER TABLE `continent`
  ADD PRIMARY KEY (`id`);

--
-- Índexs per a la taula `frontera`
--
ALTER TABLE `frontera`
  ADD PRIMARY KEY (`pais1_id`,`pais2_id`),
  ADD KEY `FKeyi80p812c1kphv64yg050i9x` (`pais2_id`);

--
-- Índexs per a la taula `jugador`
--
ALTER TABLE `jugador`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FK6yitgep48ek3mgx46s6oscky1` (`partida_id`);

--
-- Índexs per a la taula `ma`
--
ALTER TABLE `ma`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UKm9dcy0o2b1pt6q1cppo65qxib` (`jugador_id`);

--
-- Índexs per a la taula `okupa`
--
ALTER TABLE `okupa`
  ADD PRIMARY KEY (`jugador_id`,`pais_id`),
  ADD KEY `FK8j490ke09qqqf7nm5k2f883kg` (`pais_id`);

--
-- Índexs per a la taula `pais`
--
ALTER TABLE `pais`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKlciwc45nltv3gugk484puyean` (`continent_id`);

--
-- Índexs per a la taula `partida`
--
ALTER TABLE `partida`
  ADD PRIMARY KEY (`id`);

--
-- Índexs per a la taula `usuaris`
--
ALTER TABLE `usuaris`
  ADD PRIMARY KEY (`id`);

--
-- AUTO_INCREMENT per les taules bolcades
--

--
-- AUTO_INCREMENT per la taula `carta`
--
ALTER TABLE `carta`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT per la taula `jugador`
--
ALTER TABLE `jugador`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=496;

--
-- AUTO_INCREMENT per la taula `partida`
--
ALTER TABLE `partida`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=261;

--
-- AUTO_INCREMENT per la taula `usuaris`
--
ALTER TABLE `usuaris`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- Restriccions per a les taules bolcades
--

--
-- Restriccions per a la taula `carta`
--
ALTER TABLE `carta`
  ADD CONSTRAINT `FK1jip98d2hrtb9a2o10osjq1fo` FOREIGN KEY (`carta_id`) REFERENCES `ma` (`id`);

--
-- Restriccions per a la taula `frontera`
--
ALTER TABLE `frontera`
  ADD CONSTRAINT `FKeyi80p812c1kphv64yg050i9x` FOREIGN KEY (`pais2_id`) REFERENCES `pais` (`id`),
  ADD CONSTRAINT `FKiffh16s61b4e5bibf9sbp8n19` FOREIGN KEY (`pais1_id`) REFERENCES `pais` (`id`);

--
-- Restriccions per a la taula `jugador`
--
ALTER TABLE `jugador`
  ADD CONSTRAINT `FK6yitgep48ek3mgx46s6oscky1` FOREIGN KEY (`partida_id`) REFERENCES `partida` (`id`);

--
-- Restriccions per a la taula `ma`
--
ALTER TABLE `ma`
  ADD CONSTRAINT `FK3qpl1or8x1ij3p7yq3waqs7oy` FOREIGN KEY (`jugador_id`) REFERENCES `jugador` (`id`);

--
-- Restriccions per a la taula `okupa`
--
ALTER TABLE `okupa`
  ADD CONSTRAINT `FK2qmtr6k1n6t5ahv0oanmxtct2` FOREIGN KEY (`jugador_id`) REFERENCES `jugador` (`id`),
  ADD CONSTRAINT `FK8j490ke09qqqf7nm5k2f883kg` FOREIGN KEY (`pais_id`) REFERENCES `pais` (`id`);

--
-- Restriccions per a la taula `pais`
--
ALTER TABLE `pais`
  ADD CONSTRAINT `FKlciwc45nltv3gugk484puyean` FOREIGN KEY (`continent_id`) REFERENCES `continent` (`id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
