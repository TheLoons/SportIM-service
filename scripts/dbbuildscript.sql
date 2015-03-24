CREATE TABLE Player(
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

CREATE TABLE Auth(
    Login VARCHAR(50) PRIMARY KEY, 
    Token VARCHAR(255) NOT NULL 
);

CREATE TABLE Team(
    TeamId INTEGER NOT NULL AUTO_INCREMENT, 
    TeamName VARCHAR(50), 
    TeamOwner VARCHAR(50), 
        FOREIGN KEY (TeamOwner) 
        REFERENCES Player(Login), 
    Sport VARCHAR(50),
    PRIMARY KEY(TeamId)
);

CREATE TABLE PlaysFor(
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

CREATE TABLE League(
    LeagueId INTEGER NOT NULL AUTO_INCREMENT, 
    LeagueName VARCHAR(50), 
    LeagueOwner VARCHAR(50), 
        FOREIGN KEY (LeagueOwner) REFERENCES Player (Login), 
    Sport VARCHAR(50),
    PRIMARY KEY (LeagueId)
);

CREATE TABLE TeamBelongsTo(
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

CREATE TABLE Tournament(
    TournamentId INTEGER NOT NULL AUTO_INCREMENT, 
    TournamentName VARCHAR (50), 
    LeagueId INTEGER, 
    Description VARCHAR(255), 
    PRIMARY KEY (TournamentId), 
    FOREIGN KEY (LeagueId) REFERENCES League (LeagueId)
);

CREATE TABLE Event(
    EventId INTEGER NOT NULL AUTO_INCREMENT, 
	EventOwner VARCHAR(50),
    EventName VARCHAR (50), 
    StartDate BIGINT NOT NULL, 
    EndDate BIGINT NOT NULL, 
    TournamentId INTEGER, 
	FOREIGN KEY (TournamentId) REFERENCES Tournament (TournamentId), 
    NextEventId INTEGER, 
	FOREIGN KEY (EventId) REFERENCES Event (EventId)
        ON DELETE SET NULL
        ON UPDATE CASCADE, 
    FOREIGN KEY (EventOwner) REFERENCES Player (Login)
        ON UPDATE CASCADE,
    PRIMARY KEY(EventId)
);

CREATE TABLE TeamEvent(
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

CREATE TABLE PlayerEvent(
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

CREATE TABLE AlertJob(
    IsRunning TINYINT(1) PRIMARY KEY (IsRunning) NOT NULL
);

CREATE TABLE AlertsSent (
  eventId int(11) NOT NULL,
  login varchar(50) NOT NULL,
  start bigint(20) NOT NULL,
  PRIMARY KEY (eventId,login,start),
  KEY login_idx (login),
  CONSTRAINT eventId FOREIGN KEY (eventId) REFERENCES event (EventId) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT login FOREIGN KEY (login) REFERENCES player (Login) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE LeagueTable (
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
