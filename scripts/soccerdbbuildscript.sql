CREATE TABLE IF NOT EXISTS `SoccerStats` (
  `eventID` INT NOT NULL,
  `teamID` INT NOT NULL,
  `player` VARCHAR(50) NOT NULL,
  `goals` INT NOT NULL DEFAULT 0,
  `shots` INT NOT NULL DEFAULT 0,
  `shotsongoal` INT NOT NULL DEFAULT 0,
  `assists` INT NOT NULL DEFAULT 0,
  `goalsagainst` INT NOT NULL DEFAULT 0,
  `minutes` INT NOT NULL DEFAULT 0,
  `fouls` INT NOT NULL DEFAULT 0,
  `red` INT NOT NULL DEFAULT 0,
  `yellow` INT NOT NULL DEFAULT 0,
  `timeOn` INT NOT NULL DEFAULT -1,
  `saves` INT NOT NULL DEFAULT 0,
  PRIMARY KEY (`eventID`, `teamID`, `player`));

CREATE TABLE IF NOT EXISTS `SoccerTime` (
  `eventID` INT NOT NULL,
  `start` BIGINT NOT NULL DEFAULT -1,
  `half_end` BIGINT NOT NULL DEFAULT -1,
  `half_start` BIGINT NOT NULL DEFAULT -1,
  `end` BIGINT NOT NULL DEFAULT -1,
  PRIMARY KEY (`eventID`)
);
