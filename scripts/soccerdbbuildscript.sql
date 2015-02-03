CREATE TABLE IF NOT EXISTS `SoccerPassing` (
  `from` VARCHAR(50) NOT NULL,
  `to` VARCHAR(50) NOT NULL,
  `eventID` INT NOT NULL,
  `passes` INT NOT NULL,
  PRIMARY KEY (`from`, `to`, `eventID`));

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
  PRIMARY KEY (`eventID`, `teamID`, `player`));

CREATE TABLE IF NOT EXISTS `SoccerSessions` (
  `eventID` INT NOT NULL,
  `sessionID` VARCHAR(255),
  PRIMARY KEY (`eventID`)
);
