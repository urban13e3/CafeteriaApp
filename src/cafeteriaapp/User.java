package cafeteriaapp;

public class User {

    private int id;
    private String firstName;
    private String lastName;
    private String email;
    private float accountBalance;
    private String role;

    public User(int id, String firstName, String lastName, String email, float accountBalance, String role) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.accountBalance = accountBalance;
        this.role = role;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public float getAccountBalance() { return accountBalance; }
    public void setAccountBalance(float accountBalance) { this.accountBalance = accountBalance; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}