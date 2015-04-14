CREATE TABLE IF NOT EXISTS `UltimateStats` (
  `eventID` INT NOT NULL,
  `teamID` INT NOT NULL,
  `player` VARCHAR(50) NOT NULL,
  `pointsreceived` INT NOT NULL DEFAULT 0,
  `pointsthrown` INT NOT NULL DEFAULT 0,
  `fouls` INT NOT NULL DEFAULT 0,
  PRIMARY KEY (`eventID`, `teamID`, `player`));

CREATE TABLE IF NOT EXISTS `UltimateTeamStats` (
  `eventID` INT NOT NULL,
  `teamID` INT NOT NULL,
  `pointsagainst` INT NOT NULL DEFAULT 0,
  PRIMARY KEY (`eventID`, `teamID`));
