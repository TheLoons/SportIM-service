CREATE TABLE IF NOT EXISTS `StatSessions` (
  `eventID` INT NOT NULL,
  `sessionID` VARCHAR(255),
  PRIMARY KEY (`eventID`)
);

CREATE TABLE IF NOT EXISTS `Passing` (
  `from` VARCHAR(50) NOT NULL,
  `to` VARCHAR(50) NOT NULL,
  `eventID` INT NOT NULL,
  `passes` INT NOT NULL,
  PRIMARY KEY (`from`, `to`, `eventID`));
