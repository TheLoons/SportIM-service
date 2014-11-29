package org.sportim.service.beans;

/**
 * Created by hannah on 11/29/14.
 */
public class UserBean {
    private String firstName;
    private String lastName;
    private String login;
    private String password;
    private String phone;

    /**
     * Zero-argument constructor required by Jersey.
     */
    public UserBean() {
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
        return null;
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

    public void setLogin(String login) {
        this.login = login;
    }

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
}
