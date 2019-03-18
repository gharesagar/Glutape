package model;

public class Dealer {
    private int dealerId;
    private String delarName;
    private String address;
    private String contactNo;
    private String emailId;


    public Dealer(int dealerId, String delarName, String address, String contactNo, String emailId) {
        this.dealerId = dealerId;
        this.delarName = delarName;
        this.address = address;
        this.contactNo = contactNo;
        this.emailId = emailId;
    }

    public int getDealerId() {
        return dealerId;
    }

    public String getDelarName() {
        return delarName;
    }

    public String getAddress() {
        return address;
    }

    public String getContactNo() {
        return contactNo;
    }

    public String getEmailId() {
        return emailId;
    }
}
