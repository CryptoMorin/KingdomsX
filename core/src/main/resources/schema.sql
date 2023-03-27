-- Stupid SQL Syntax Notes:
---- Composite primary keys must always appear as the last column definition
---- Trailing commas are not supported.
---- Constraints are always non-null and cannot be explicitly defined.
---- Strict tables are only supported from 3.36+

----------------------------------------------------------------------------------------------------
---------------------------------------   Players  -------------------------------------------------
----------------------------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS `{PREFIX}players` (
    `id` UUID NOT NULL,
    `lang` CHAR(2) NOT NULL,
    `kingdom` UUID NULL,
    `rank` RANK_NODE NULL,
    `nationalRank` RANK_NODE NULL,
    `joinedAt` LONG NULL,
    `lastPowerCheckup` LONG NULL,
    `lastDonationTime` LONG NULL,
    `lastDonationAmount` LONG NULL,
    `totalDonations` LONG NULL,
    `admin` BOOL NULL,
    `pvp` BOOL NULL,
    `spy` BOOL NULL,
    `markers` BOOL NULL,
    `sneakMode` BOOL NULL,
    `markersType` VARCHAR(50) NULL,
    `chatChannel` VARCHAR(50) NULL,
    `power` DOUBLE NULL,
    `mapSize_height` INT NULL, `mapSize_width` INT NULL,
    `readMails` JSON NULL,
    `claims` JSON NULL,
    `mutedChannels` JSON NULL,
    `invites` JSON NULL,
    SimpleChunkLocation(jailCell) NULL,
    `metadata` JSON NULL,
    CONSTRAINT `{PREFIX}players_pkey` PRIMARY KEY (`id`)
) STRICT;
--CREATE TABLE IF NOT EXISTS `{PREFIX}players_readMails` (
--    id UUID REFERENCES `{PREFIX}players`(id) NOT NULL,
--    entry UUID NOT NULL
--) STRICT;
--CREATE TABLE IF NOT EXISTS `{PREFIX}players_claims` (
--    id UUID REFERENCES `{PREFIX}players`(id) NOT NULL,
--    world WORLD NOT NULL, x INT NOT NULL, z INT NOT NULL
--) STRICT;
--CREATE TABLE IF NOT EXISTS `{PREFIX}players_invites` (
--    id UUID REFERENCES `{PREFIX}players`(id) NOT NULL,
--    sender UUID NOT NULL,
--    acceptTime LONG NOT NULL, timestamp LONG NOT NULL
--) STRICT;
--CREATE TABLE IF NOT EXISTS `{PREFIX}players_jailCell` (
--    id UUID REFERENCES `{PREFIX}players`(id) NOT NULL,
--    world WORLD NOT NULL, x INT NOT NULL, y INT NOT NULL, z INT NOT NULL
--) STRICT;
--CREATE TABLE IF NOT EXISTS `{PREFIX}players_protectedBlocks` (
--    id UUID REFERENCES `{PREFIX}players`(id) not null,
--    world WORLD NOT NULL, x INT NOT NULL, y INT NOT NULL, z INT NOT NULL
--) STRICT;
--CREATE TABLE IF NOT EXISTS `{PREFIX}players_mutedChannels` (
--    id UUID REFERENCES `{PREFIX}players`(id) not null,
--    entry VARCHAR(50) NOT NULL
--) STRICT;


----------------------------------------------------------------------------------------------------
----------------------------------------   Lands  --------------------------------------------------
----------------------------------------------------------------------------------------------------


CREATE TABLE IF NOT EXISTS `{PREFIX}lands` (
    SimpleChunkLocation(id) NOT NULL,
    `kingdom` UUID NULL,
    `claimedBy` UUID NULL,
    `structures` JSON NULL, `turrets` JSON NULL, `protectedBlocks` JSON NULL,
    `since` LONG NOT NULL,
    `metadata` JSON NULL,
    CONSTRAINT `{PREFIX}lands_pkey` PRIMARY KEY (`id_world`, `id_x`, `id_z`)
) STRICT;
--CREATE TABLE `{PREFIX}lands_turrets` (
--    id_world WORLD, id_x INT, id_y INT, id_z INT,
--    land_world WORLD, land_x INT, land_z INT,
--    entry JSON,
--    CONSTRAINT location PRIMARY KEY (id_world, id_x, id_y, id_z)
--) STRICT;
--CREATE TABLE `{PREFIX}lands_structures` (
--    id_world WORLD, id_x INT, id_y INT, id_z INT,
--    land_world WORLD, land_x INT, land_z INT,
--    json JSON,
--    CONSTRAINT location PRIMARY KEY (id_world, id_x, id_y, id_z)
--) STRICT;
--CREATE TABLE `{PREFIX}lands_protectedBlocks` (
--    id_world WORLD, id_x INT, id_y INT, id_z INT,
--    land_world WORLD, land_x INT, land_z INT,
--    sign_world WORLD NOT NULL, sign_x INT NOT NULL, sign_y INT NOT NULL, sign_z INT NOT NULL,
--    since LONG NOT NULL,
--    password NVARCHAR(100) NULL,
--    owner UUID NOT NULL,
--    protectionType VARCHAR(30) NOT NULL,
--    metadata JSON NOT NULL,
--    CONSTRAINT location PRIMARY KEY (id_world, id_x, id_y, id_z)
--    --private @NonNull Map<UUID, Boolean> players, kingdoms;
--) STRICT;


--CREATE INDEX `{PREFIX}lands_turrets_lands` ON `{PREFIX}lands_turrets`(land_world, land_x, land_z);
--CREATE INDEX `{PREFIX}lands_structures_lands` ON `{PREFIX}lands_structures`(land_world, land_x, land_z);
--CREATE INDEX `{PREFIX}lands_protectedBlocks_lands` ON `{PREFIX}lands_protectedBlocks`(land_world, land_x, land_z);

----------------------------------------------------------------------------------------------------
-----------------------------------------  Group  --------------------------------------------------
----------------------------------------------------------------------------------------------------

{{ GROUP
    `id` UUID NOT NULL,
  --owner UUID NOT NULL,
    `name` NVARCHAR(100) NOT NULL,
    `tag` NVARCHAR(50) NULL,
    `requiresInvite` BOOL NULL, `publicHome` BOOL NULL, `permanent` BOOL NULL, `hidden` BOOL NULL,
    `resourcePoints` LONG NOT NULL,
    `bank` DOUBLE NOT NULL,
    `tax` VARCHAR(300),
    `shieldSince` LONG NOT NULL, `shieldTime` LONG NOT NULL,
    `publicHomeCost` DOUBLE NOT NULL,
    `since` LONG NOT NULL,
    `color` INT NULL,
    `flag` NVARCHAR(1000) NULL,
    SimpleLocation(nexus) NULL,
    Location(home) NULL,
    `ranks` JSON NULL,
    `relationshipRequests` JSON NULL,
    `relations` JSON NULL,
    `attributes` JSON NULL,
    `logs` JSON NULL,
    `metadata` JSON NULL
}}

----------------------------------------------------------------------------------------------------
---------------------------------------  Kingdoms  -------------------------------------------------
----------------------------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS `{PREFIX}kingdoms` (
    [[GROUP]],
    `king` UUID NOT NULL,
    `nation` UUID NULL,
    `lore` NVARCHAR(300) NULL,
    `pacifist` BOOL NULL,
    `championType` VARCHAR(50) NULL,
    `maxLandsModifier` LONG NULL,
    `lastInvasion` LONG NULL,
    `members` JSON NOT NULL,
    `lands` JSON NULL,
    `upgrades_misc` JSON NULL,
    `upgrades_powerups` JSON NULL,
    `upgrades_champions` JSON NULL,
    `nexusChest` JSON NULL,
    `book` JSON NULL,
    `mails` JSON NULL,
    `challenges` JSON NULL,
    `inviteCodes` JSON NULL,
    CONSTRAINT `{PREFIX}kingdoms_pkey` PRIMARY KEY (`id`)
) STRICT;

--CREATE TABLE IF NOT EXISTS `{PREFIX}kingdoms_members` (
--    id UUID PRIMARY KEY NOT NULL,
--    id UUID NOT NULL
--) STRICT;
--CREATE TABLE IF NOT EXISTS `{PREFIX}kingdoms_lands` (
--    id UUID NOT NULL,
--    SimpleChunkLocation(id) NOT NULL,
--    CONSTRAINT location PRIMARY KEY (id_world, id_x, id_z)
--) STRICT;
---- protected @NonNull Map<UUID, KingdomRelationshipRequest> relationshipRequests;
---- protected @NonNull Map<UUID, KingdomRelation> relations;
--CREATE TABLE IF NOT EXISTS `{PREFIX}kingdoms_relations` (
--    id UUID PRIMARY KEY NOT NULL,
--    id UUID NOT NULL,
--    relation VARCHAR(50) NOT NULL
--) STRICT;
---- Map<KingdomRelation, Set<RelationAttribute>> attributes;
--CREATE TABLE IF NOT EXISTS `{PREFIX}kingdoms_attributes` (
--    entry VARCHAR(50) PRIMARY KEY NOT NULL,
--    relation VARCHAR(50) NOT NULL,
--    kingdom UUID NOT NULL
--) STRICT;
---- LinkedList<AuditLog> logs;
--CREATE TABLE IF NOT EXISTS `{PREFIX}kingdoms_logs` (
--    kingdom UUID NOT NULL,
--    json JSON NOT NULL
--) STRICT;
--CREATE TABLE IF NOT EXISTS `{PREFIX}kingdoms_ranks` (
--    kingdom UUID NOT NULL,
--    node VARCHAR(50) PRIMARY KEY NOT NULL,
--    name NVARCHAR(100),
--    material VARCHAR(100),
--    priority INT NOT NULL,
--    maxClaims INT NOT NULL,
--    symbol NVARCHAR(50) NOT NULL,
--    color COLOR NOT NULL
--) STRICT;
--CREATE TABLE IF NOT EXISTS `{PREFIX}kingdoms_ranks_permissions` (
--    node VARCHAR(50) NOT NULL,
--    permission VARCHAR(100) NOT NULL
--) STRICT;
---- Map<String, BookChapter> book;
--CREATE TABLE IF NOT EXISTS `{PREFIX}kingdoms_book` (
--    chapter VARCHAR(100) NOT NULL,
--    kingdom UUID NOT NULL,
--
--    -- https://minecraft.fandom.com/wiki/Book_and_Quill
--    -- Let's just be safe...
--    page NVARCHAR(32767)
--) STRICT;
---- Map<UUID, Mail> mails;
--CREATE TABLE IF NOT EXISTS `{PREFIX}kingdoms_mails` (
--    id UUID PRIMARY KEY NOT NULL,
--    kingdom UUID NOT NULL
--) STRICT;

----------------------------------------------------------------------------------------------------
----------------------------------------  Nations  -------------------------------------------------
----------------------------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS `{PREFIX}nations` (
    [[GROUP]],
    `capital` UUID NOT NULL,
    `kingdoms` JSON NOT NULL,
    CONSTRAINT `{PREFIX}nations_pkey` PRIMARY KEY (`id`)
) STRICT;

----------------------------------------------------------------------------------------------------
-----------------------------------------  Mails  --------------------------------------------------
----------------------------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS `{PREFIX}mails` (
    `id` UUID NOT NULL,
    `fromGroup` UUID NOT NULL,
    `sender` UUID NOT NULL,
    `inReplyTo` UUID NULL,
    `sent` LONG NOT NULL,
    `subject` NVARCHAR(300) NOT NULL,
    `recipients` JSON NOT NULL,
    `message` JSON NOT NULL,
    `metadata` JSON NULL,
    CONSTRAINT `{PREFIX}mails_pkey` PRIMARY KEY (`id`)
) STRICT;

----------------------------------------------------------------------------------------------------
---------------------------------------  Versions  -------------------------------------------------
----------------------------------------------------------------------------------------------------

-- We will add this once we really need it
--CREATE TABLE IF NOT EXISTS `{PREFIX}versions` (
--    `version` INT NOT NULL,
--    `migration_time` INT NOT NULL,
--    `comment` NVARCHAR(5000),
--    CONSTRAINT `{PREFIX}versions_pkey` PRIMARY KEY (`version`)
--) STRICT;
