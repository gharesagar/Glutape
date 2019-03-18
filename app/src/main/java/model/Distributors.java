package model;

public class Distributors {
    private int distributerId;
    private String distributerName;
    private String contactNo;
    private String emailId;
    private String address;

    public Distributors(int distributerId, String distributerName, String contactNo, String emailId, String address) {
        this.distributerId = distributerId;
        this.distributerName = distributerName;
        this.contactNo = contactNo;
        this.emailId = emailId;
        this.address = address;
    }

    public int getDistributerId() {
        return distributerId;
    }

    public String getDistributerName() {
        return distributerName;
    }

    public String getContactNo() {
        return contactNo;
    }

    public String getEmailId() {
        return emailId;
    }

    public String getAddress() {
        return address;
    }
}
