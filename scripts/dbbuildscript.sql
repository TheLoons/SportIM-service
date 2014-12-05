CREATE TABLE Player(
    Login VARCHAR(50) PRIMARY KEY, 
    Password CHAR(64) NOT NULL, 
    FirstName VARCHAR(255) NOT NULL, 
    LastName VARCHAR(255) NOT NULL, 
    Phone INTEGER, 
    Salt CHAR(20) NOT NULL
);

CREATE TABLE Team(
    TeamId INTEGER NOT NULL AUTO_INCREMENT, 
    TeamName VARCHAR(50), 
    TeamOwner VARCHAR(50), 
        FOREIGN KEY (TeamOwner) 
        REFERENCES Player(Login), 
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
    EventName VARCHAR (50), 
    StartDate BIGINT NOT NULL, 
    EndDate BIGINT NOT NULL, 
    TournamentId INTEGER, 
        FOREIGN KEY (TournamentId) REFERENCES Tournament (TournamentId), 
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
    LoginVARCHAR (50), 
        FOREIGN KEY (Login) REFERENCES Player (Login)
        ON UPDATE CASCADE
        ON DELETE CASCADE, 
    PRIMARY KEY (EventId, Login)
);
