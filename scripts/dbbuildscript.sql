CREATE TABLE IF NOT EXISTS Player(
    Login VARCHAR(50) PRIMARY KEY, 
    Password CHAR(64) NOT NULL, 
    FirstName VARCHAR(255) NOT NULL, 
    LastName VARCHAR(255) NOT NULL, 
    Phone VARCHAR(20) NULL, 
    Salt CHAR(40) NOT NULL,
    GameAlert BIGINT(20),
    PracticeAlert BIGINT(20),
    MeetingAlert BIGINT(20),
    OtherAlert BIGINT(20)
);

CREATE TABLE IF NOT EXISTS Auth(
    Login VARCHAR(50) PRIMARY KEY, 
    Token VARCHAR(255) NOT NULL 
);

CREATE TABLE IF NOT EXISTS Team(
    TeamId INTEGER NOT NULL AUTO_INCREMENT, 
    TeamName VARCHAR(50), 
    TeamOwner VARCHAR(50), 
        FOREIGN KEY (TeamOwner) 
        REFERENCES Player(Login), 
    Sport VARCHAR(50),
    PRIMARY KEY(TeamId)
);

CREATE TABLE IF NOT EXISTS PlaysFor(
    Login VARCHAR(50) NOT NULL, 
        FOREIGN KEY (Login) REFERENCES Player (Login)
        ON UPDATE CASCADE
        ON DELETE CASCADE, 
    TeamID INTEGER NOT NULL, 
        FOREIGN KEY (TeamId) REFERENCES Team(TeamId)
        ON UPDATE CASCADE
        ON DELETE CASCADE, 
    PRIMARY KEY (Login, TeamId)
);

CREATE TABLE IF NOT EXISTS League(
    LeagueId INTEGER NOT NULL AUTO_INCREMENT, 
    LeagueName VARCHAR(50), 
    LeagueOwner VARCHAR(50), 
        FOREIGN KEY (LeagueOwner) REFERENCES Player (Login), 
    Sport VARCHAR(50),
    PRIMARY KEY (LeagueId)
);

CREATE TABLE IF NOT EXISTS TeamBelongsTo(
    TeamId INTEGER, 
        FOREIGN KEY (TeamId) REFERENCES Team (TeamId)
        ON UPDATE CASCADE
        ON DELETE CASCADE, 
    LeagueId INTEGER, 
        FOREIGN KEY(LeagueId) REFERENCES League (LeagueId)
        ON UPDATE CASCADE
        ON DELETE CASCADE, 
    PRIMARY KEY (TeamId, LeagueId)
);

CREATE TABLE IF NOT EXISTS Tournament(
    TournamentId INTEGER NOT NULL AUTO_INCREMENT, 
    TournamentName VARCHAR (50), 
    LeagueId INTEGER, 
    Description VARCHAR(255), 
    PRIMARY KEY (TournamentId), 
    FOREIGN KEY (LeagueId) REFERENCES League (LeagueId)
);

CREATE TABLE IF NOT EXISTS Event(
    EventId INTEGER NOT NULL AUTO_INCREMENT, 
	EventOwner VARCHAR(50),
    EventName VARCHAR (50), 
    StartDate BIGINT NOT NULL, 
    EndDate BIGINT NOT NULL, 
    TournamentId INTEGER, 
	FOREIGN KEY (TournamentId) REFERENCES Tournament (TournamentId), 
    EventType VARCHAR(10),
    Location VARCHAR(255),
    NextEventId INTEGER,
    FOREIGN KEY (EventOwner) REFERENCES Player (Login)
        ON UPDATE CASCADE,
    PRIMARY KEY(EventId)
);

ALTER TABLE `Event`
ADD INDEX `fk_event_1_idx` (`NextEventId` ASC);
ALTER TABLE Event 
ADD CONSTRAINT `fk_event_1`
  FOREIGN KEY (`NextEventId`)
  REFERENCES `Event` (`EventId`)
  ON DELETE SET NULL
  ON UPDATE CASCADE;

CREATE TABLE IF NOT EXISTS TeamEvent(
    EventId INTEGER, 
        FOREIGN KEY (EventId) REFERENCES Event (EventId) 
        ON UPDATE CASCADE 
        ON DELETE CASCADE, 
    TeamId INTEGER, 
        FOREIGN KEY (TeamId) REFERENCES Team (TeamId)
        ON UPDATE CASCADE 
        ON DELETE CASCADE, 
    PRIMARY KEY (EventId, TeamId)
);

CREATE TABLE IF NOT EXISTS PlayerEvent(
    EventId INTEGER, 
        FOREIGN KEY (EventId) REFERENCES Event (EventId)
        ON UPDATE CASCADE
        ON DELETE CASCADE, 
    Login VARCHAR (50), 
        FOREIGN KEY (Login) REFERENCES Player (Login)
        ON UPDATE CASCADE
        ON DELETE CASCADE, 
    PRIMARY KEY (EventId, Login)
);

CREATE TABLE IF NOT EXISTS AlertJob(
    IsRunning TINYINT(1) NOT NULL,
    PRIMARY KEY (IsRunning)
);

CREATE TABLE IF NOT EXISTS AlertsSent (
  eventId int(11) NOT NULL,
  login varchar(50) NOT NULL,
  start bigint(20) NOT NULL,
  PRIMARY KEY (eventId,login,start),
  KEY login_idx (login),
  CONSTRAINT eventId FOREIGN KEY (eventId) REFERENCES event (EventId) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT login FOREIGN KEY (login) REFERENCES player (Login) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS LeagueTable (
  LeagueId INTEGER,
      FOREIGN KEY (LeagueId) REFERENCES League (LeagueId)
      ON UPDATE CASCADE
      ON DELETE CASCADE,
  TournamentId INTEGER,
      FOREIGN KEY (TournamentId) REFERENCES Tournament (TournamentId)
      ON UPDATE CASCADE
      ON DELETE CASCADE,
  Description VARCHAR(255),
  PRIMARY KEY (LeagueId, TournamentId)
);

CREATE TABLE `TeamColors` (
  `TeamId` int(11) NOT NULL,
  `PrimaryColor` varchar(8) NOT NULL,
  `SecondaryColor` varchar(8) NOT NULL,
  `TertiaryColor` varchar(8) NOT NULL,
  PRIMARY KEY (`TeamId`),
  UNIQUE KEY `TeamId_UNIQUE` (`TeamId`),
  CONSTRAINT `TeamId` FOREIGN KEY (`TeamId`) REFERENCES `team` (`TeamId`) ON DELETE CASCADE ON UPDATE CASCADE
) ;
