package org.sportim.service.util;

/**
 * Created by Doug on 2/16/15.
 */


import com.sendgrid.*;
import com.twilio.sdk.TwilioRestClient;
import com.twilio.sdk.TwilioRestException;
import com.twilio.sdk.resource.factory.MessageFactory;
import com.twilio.sdk.resource.instance.Message;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class SendNotification implements Job
{
    private static Logger logger = Logger.getLogger(SendNotification.class.getName());
    private ConnectionProvider provider;

    public SendNotification() { provider = ConnectionManager.getInstance();}

//    public static void main(String[] args)
//    {
//        System.out.println(sendEmail("valnir1@gmail.com", "doug.hitchcock@aruplab.com", "Test Send Grid", "This is a test email from SendGrid"));
//    }

    /*
        Simple function to send email and return true if it successfully tried to send an email
     */
    public static boolean sendEmail(String toAddress, String fromAddress, String subject, String body)
    {
        String username = System.getenv("SENDGRID_USERNAME");
        String password = System.getenv("SENDGRID_PASSWORD");

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
        Simple function to send email and return true if it successfully tried to send an email
     */
    public static boolean sendText(String toPhone, String fromPhone, String body)
    {


        boolean sentText = true;
        String twilio_acct_id = System.getenv("TWILIO_ACCOUNT_SID");
        String twilio_auth_token = System.getenv("TWILIO_AUTH_TOKEN");
        try {
            TwilioRestClient client = new TwilioRestClient(twilio_acct_id, twilio_auth_token);

            // Build a filter for the MessageList
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("Body", body));
            params.add(new BasicNameValuePair("To", "+1" + toPhone));
            params.add(new BasicNameValuePair("From", "+1" + fromPhone));

            MessageFactory messageFactory = client.getAccount().getMessageFactory();
            Message message = messageFactory.create(params);
            sentText = true;
        } catch (TwilioRestException e) {
            System.out.println(e.getMessage());
        }


        return sentText;
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
                    stmt = conn.prepareStatement("select test3.Login, test3.GameAlert, test3.PracticeAlert, test3.MeetingAlert, test3.OtherAlert, test3.ReceiveEmail, test3.ReceiveText, test3.TeamId, test3.EventId, test3.StartDate, test3.EventName, test3.Location, test3.EventType  from " +
                            "(select test2.Login, test2.GameAlert, test2.PracticeAlert, test2.MeetingAlert, test2.OtherAlert, test2.ReceiveEmail, test2.ReceiveText, test2.TeamId, test2.EventId, e.StartDate, e.EventName, e.Location, e.EventType from " +
                            "(select test.Login, test.GameAlert, test.PracticeAlert, test.MeetingAlert, test.OtherAlert, test.ReceiveEmail, test.ReceiveText, test.TeamId, te.EventId from " +
                            "(select p.Login, p.GameAlert, p.PracticeAlert, p.MeetingAlert, p.OtherAlert, p.ReceiveEmail, p.ReceiveText, pf.TeamId " +
                            "from Player p join PlaysFor pf where p.Login = pf.Login) as test " +
                            "join TeamEvent te where te.TeamId = test.TeamId) as test2 " +
                            "join Event e where e.EventId = test2.EventId AND e.StartDate >= ?) as test3");
                    stmt.setLong(1, currentTime);
                    res = stmt.executeQuery();
                    List<PreparedStatement> stmts = new LinkedList<PreparedStatement>();
                    boolean receiveText;
                    boolean receiveEmail;
                    boolean game;
                    boolean practice;
                    boolean meeting;
                    boolean other;
                    while(res.next())
                    {
                        receiveText = (res.getInt("test3.ReceiveText") == 1);
                        receiveEmail = (res.getInt("test3.ReceiveText") == 1);
                        game = ((res.getLong("test3.StartDate") < (res.getLong("test3.GameAlert") + currentTime + millisPerHour ) && res.getString("test3.Type").equals("Game")));
                        practice = ((res.getLong("test3.StartDate") < (res.getLong("test3.PracticeAlert") + currentTime + millisPerHour ) && res.getString("test3.Type").equals("Practice")));
                        meeting = ((res.getLong("test3.StartDate") < (res.getLong("test3.MeetingAlert") + currentTime + millisPerHour ) && res.getString("test3.Type").equals("Meeting")));
                        other = ((res.getLong("test3.StartDate") < (res.getLong("test3.OtherAlert") + currentTime + millisPerHour ) && res.getString("test3.Type").equals("Other")));
                        if(game || practice || meeting || other)
                        {
                            String login = res.getString("test3.Login");
                            Date eventDate = new Date(res.getLong("test3.StartDate"));

                            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");
                            String body = "This is your notification for Event: " + res.getString("test3.EventName") + " on " + dateFormat.format(eventDate);
                            if(receiveEmail) {
                                if (sendEmail(res.getString("test3.Login"), "theloons.sportim@gmail.com", res.getString("test3.EventName"), body)) {
                                    stmt = conn.prepareStatement("INSERT INTO alertssent (eventId, login, start) VALUES (?,?,?)");
                                    stmt.setInt(1, res.getInt("test3.eventId"));
                                    stmt.setString(2, login);
                                    stmt.setLong(3, currentTime);
                                    stmt.addBatch();
                                    stmts.add(stmt);

                                }
                            }
                            if(receiveText) {
                                String fromNumber = "3853991636";
                                if (sendText(res.getString("test3.Phone"), fromNumber, body)) {
                                    stmt = conn.prepareStatement("INSERT INTO alertssent (eventId, login, start) VALUES (?,?,?)");
                                    stmt.setInt(1, res.getInt("test3.eventId"));
                                    stmt.setString(2, login);
                                    stmt.setLong(3, currentTime);
                                    stmt.addBatch();
                                    stmts.add(stmt);
                                }
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
            status = 500;
            message = "Issue with checking \"IsRunning\" query";
            logger.error(message + ": " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
        } finally {
            APIUtils.closeResource(res);
            APIUtils.closeResource(stmt);
            APIUtils.closeResource(conn);
        }
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
//        emailAlerts();
        System.out.println("Test Job");
//        sendText("8016948286", "3853991636", "body test");
//        sendEmail("valnir1@gmail.com", "doug.hitchcock@aruplab.com", "Test Send Grid", "This is a test email from SendGrid");
    }
}


