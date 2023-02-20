PRAGMA strict=ON;
CREATE TABLE IF NOT EXISTS `{PREFIX}players` (
    id UUID PRIMARY KEY NOT NULL,
    `language` CHAR(2) NOT NULL,
    kingdom UUID NULL,
    rank RANK_NODE NULL,
    nationalRank RANK_NODE NULL
    joinedAt LONG NOT NULL,
    lastPowerCheckup LONG NOT NULL,
    lastDonationTime LONG NOT NULL,
    lastDonationAmount LONG NOT NULL,
    totalDonations LONG NOT NULL,
    admin BOOL NOT NULL,
    pvp BOOL NOT NULL,
    spy BOOL NOT NULL,
    markers BOOL NOT NULL,
    sneakMode BOOL NOT NULL,
    markersType VARCHAR(50) NOT NULL,
    chatChannel VARCHAR(50) NOT NULL,
    power DOUBLE NOT NULL,
    metadata JSON NULL,
) STRICT;
CREATE TABLE IF NOT EXISTS `{PREFIX}players_readMails` (
    id UUID REFERENCES `{PREFIX}players`(id) not null,
    entry UUID NOT NULL
) STRICT;
CREATE TABLE IF NOT EXISTS `{PREFIX}players_claims` (
    id UUID REFERENCES `{PREFIX}players`(id) NOT NULL,
    world WORLD NOT NULL, x INT NOT NULL, z INT NOT NULL
) STRICT;
CREATE TABLE IF NOT EXISTS `{PREFIX}players_mapSize` (
    id UUID REFERENCES `{PREFIX}players`(id) NOT NULL,
    height INT NOT NULL, width INT NOT NULL
) STRICT;
CREATE TABLE IF NOT EXISTS `{PREFIX}players_jailCell` (
    id UUID REFERENCES `{PREFIX}players`(id) NOT NULL,
    world WORLD NOT NULL, x INT NOT NULL, y INT NOT NULL, z INT NOT NULL
) STRICT;
CREATE TABLE IF NOT EXISTS `{PREFIX}players_protectedBlocks` (
    id UUID REFERENCES `{PREFIX}players`(id) not null,
    world WORLD NOT NULL, x INT NOT NULL, y INT NOT NULL, z INT NOT NULL
) STRICT;
CREATE TABLE IF NOT EXISTS `{PREFIX}players_mutedChannels` (
    id UUID REFERENCES `{PREFIX}players`(id) not null,
    entry VARCHAR(50) NOT NULL
) STRICT;