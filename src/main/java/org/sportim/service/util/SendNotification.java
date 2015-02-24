package org.sportim.service.util;



/**
 * Created by Doug on 2/16/15.
 */


import com.sendgrid.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class SendNotification
{
    private ConnectionProvider provider;

    public SendNotification() { provider = ConnectionManager.getInstance();}

    public static void main(String[] args)
    {
        System.out.println(sendEmail("valnir1@gmail.com", "doug.hitchcock@aruplab.com", "Test Send Grid", "This is a test email from SendGrid"));
    }

    /*
        Simple function to send email and return true if it successfully tried to send an email
     */
    public static boolean sendEmail(String toAddress, String fromAddress, String subject, String body)
    {
        String username = System.getenv("SENDGRID_USERNAME");
        String password = System.getenv("SENDGRID_PASSWORD");
        username = "app33243554@heroku.com";
        password = "caade2au";

        boolean sentEmail = true;
        SendGrid sendgrid = new SendGrid(username, password);
        SendGrid.Email email = new SendGrid.Email();
        email.addTo(toAddress);
        email.setFrom(fromAddress);
        email.setSubject(subject);
        email.setText(body);

        try {
            SendGrid.Response response = sendgrid.send(email);
        }
        catch (SendGridException e) {
            System.out.println(e);
            sentEmail = false;
        }


        return sentEmail;
    }

    /*
    1. Make sure job isn't running (AlertJob table (Column "isRunning" (true/false?))) (bail out if running)
    2. Grab Events from now to 24 hours + participants + preferences (from teams/participants) (AKA Nasty Join)
    3. Make sure not already fired (via Alerts Table (Columns: eventId, login, start))
    4. For each login/event pair fire alert and add to alerts
    5. Clear out old alerts
    6. Change job to "not running"
     */
    public void emailAlerts()
    {
        int status = 200;
        String message = "";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet res = null;
        long currentTime = System.currentTimeMillis();
        try {

            conn = provider.getConnection();
            stmt = conn.prepareStatement("Select IsRunning FROM alertjob where IsRunning = ?");
            stmt.setInt(1, 1);
            res = stmt.executeQuery();
            // Check if job is currently running
            if (res.next()) {
                status = 406;
                message = "Job is running"; // Bail out
            }
            else // Begin running job, if it isn't running
            {
                // UPDATE Team " + "SET TeamName = ?, TeamOwner = ?, Sport = ? " + "WHERE TeamId = ?");
                stmt = conn.prepareStatement("UPDATE AlertJob SET IsRunning = ? WHERE IsRunning = ? ");
                stmt.setInt(1, 1);
                stmt.setInt(2, 0);
                int result = stmt.executeUpdate();
                if(result < 1)
                {
                    message = "Job is already Running.";
                    status = 406;
                }
                else
                {
                    long millisPerHour = 3600000;
                    stmt = conn.prepareStatement("select test3.Login, test3.GameAlert, test3.TeamId, test3.EventId, test3.StartDate, test3.EventName from " +
                            "(select test2.Login, test2.GameAlert, test2.TeamId, test2.EventId, e.StartDate, e.EventName from " +
                            "(select test.Login, test.GameAlert, test.PracticeAlert, test.MeetingAlert, test.OtherAlert, test.TeamId, te.EventId from " +
                            "(select p.Login, p.GameAlert, p.PracticeAlert, p.MeetingAlert, p.OtherAlert, pf.TeamId " +
                            "from Player p join PlaysFor pf where p.Login = pf.Login) as test " +
                            "join TeamEvent te where te.TeamId = test.TeamId) as test2 " +
                            "join Event e where e.EventId = test2.EventId AND e.StartDate >= ?) as test3");
                    stmt.setLong(1, currentTime);
                    res = stmt.executeQuery();
                    List<PreparedStatement> stmts = new LinkedList<PreparedStatement>();
                    while(res.next())
                    {

                        if(res.getLong("test3.StartDate") < (res.getLong("test3.GameAlert") + currentTime + millisPerHour ) )
                        {
                            String login = res.getString("test3.Login");
                            Date eventDate = new Date(res.getLong("test3.StartDate"));

                            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");
                            if(sendEmail(res.getString("test3.Login"), "theloons.sportim@gmail.com", res.getString("test3.EventName"), "This is your notification for Event: " + res.getString("test3.EventName") + " on " + dateFormat.format(eventDate)))
                            {
                                stmt = conn.prepareStatement("INSERT INTO alertssent (eventId, login, start) VALUES (?,?,?)");
                                stmt.setInt(1, res.getInt("test3.eventId"));
                                stmt.setString(2, login);
                                stmt.setLong(3, currentTime);
                                stmt.addBatch();
                                stmts.add(stmt);

                            }

                        }
                    }
                    for(PreparedStatement s : stmts)
                    {
                        stmt = s;
                        s.executeBatch();
                    }
                }

            }

            stmt = conn.prepareStatement("UPDATE AlertJob SET IsRunning = ? WHERE IsRunning = ? ");
            stmt.setInt(1, 0);
            stmt.setInt(2, 1);
            int result = stmt.executeUpdate();
            if(result < 1)
            {
                //Insert into Alerts Sent table
            }
        } catch (SQLException e) {
            // TODO log actual error
            e.printStackTrace();
            status = 500;
            message = "Issue with checking \"IsRunning\" query";
        } finally {
            APIUtils.closeResource(res);
            APIUtils.closeResource(stmt);
            APIUtils.closeResource(conn);
        }
    }
}


