CREATE TABLE IF NOT EXISTS `SoccerPassing` (
  `from` VARCHAR(50) NOT NULL,
  `to` VARCHAR(50) NOT NULL,
  `eventID` INT NOT NULL,
  `passes` INT NOT NULL,
  PRIMARY KEY (`from`, `to`, `eventID`));

CREATE TABLE IF NOT EXISTS `SoccerStats` (
  `eventID` INT NOT NULL,
  `player` VARCHAR(50) NOT NULL,
  `goals` INT NOT NULL,
  `shots` INT NOT NULL,
  `shotsongoal` INT NOT NULL,
  `assists` INT NOT NULL,
  `goalsagainst` INT NOT NULL,
  `minutes` INT NOT NULL,
  `fouls` INT NOT NULL,
  `red` INT NOT NULL,
  `yellow` INT NOT NULL,
  PRIMARY KEY (`eventID`, `player`));

