package org.sportim.service.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * A bean representing a user
 */
@JsonSerialize(include=JsonSerialize.Inclusion.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown=true)
public class UserBean {
    private String firstName;
    private String lastName;
    private String login;
    private String password;
    private String phone;
    private String salt;
    private long gameAlert = -1;
    private long practiceAlert = -1;
    private long meetingAlert = -1;
    private long otherAlert = -1;
    private int receiveEmail = 0;
    private int receiveText = 0;


    /**
     * Zero-argument constructor required by Jersey.
     */
    public UserBean() {
    }

    public UserBean(ResultSet rs) throws SQLException {
        login = rs.getString(1);
        firstName = rs.getString(2);
        lastName = rs.getString(3);
    }

    public UserBean(ResultSet rs, String email) throws SQLException {
        login = email;
        firstName = rs.getString(1);
        lastName = rs.getString(2);
        phone = rs.getString(3);
    }

    /**
     * Validates that this user is a valid instance.
     * @param forPost if this user is to be used in a post
     * @return If valid, null. Otherwise, a message about what is invalid
     */
    public String validate(boolean forPost) {
        if (firstName == null || firstName.isEmpty()) {
            return "User must have a first name.";
        }
        if (lastName == null || lastName.isEmpty()) {
            return "User must have a last name.";
        }
        if (login == null || login.isEmpty()) {
            return "User must have a login/email address.";
        }
        if (phone == null || phone.isEmpty()) {
            return "User must have a phone number.";
        }
        phone = phone.replace(" ", "");
        phone = phone.replace("(", "");
        phone = phone.replace(")", "");
        phone = phone.replace("-", "");
        if (!phone.matches("^[0-9]{10}$")) {
            return "Invalid phone number.";
        }
        if (forPost && (password == null || password.isEmpty())) {
            return "Password is required.";
        }
        return "";
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) { this.login = login; }

    public String getPassword() {
        return password;
    }

    public void setPassword(String pwd) {
        this.password = pwd;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public long getGameAlert() { return gameAlert;}

    public void setGameAlert(long gameAlert) { this.gameAlert = gameAlert;}

    public long getPracticeAlert() {return practiceAlert;}

    public void setPracticeAlert(long practiceAlert) { this.practiceAlert = practiceAlert;}

    public long getMeetingAlert() {return meetingAlert;}

    public void setMeetingAlert(long meetingAlert) { this.meetingAlert = meetingAlert;}

    public long getOtherAlert() {return otherAlert;}

    public void setOtherAlert(long otherAlert) { this.otherAlert = otherAlert;}

    public int getReceiveEmail() {return receiveEmail;}

    public void setReceiveEmail(int receiveEmail) { this.receiveEmail = receiveEmail;}

    public int getReceiveText() {return receiveText;}

    public void setReceiveText(int receiveText) { this.receiveText = receiveText;}
}
