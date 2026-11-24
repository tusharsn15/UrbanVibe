package com.urbanvibe.model;

public class Admin extends User {

    private String employeeCode;

    public Admin() {
        super();
        setRole(ROLE_ADMIN);
    }

    public Admin(int userId, String name, String email, String passwordHash, String salt,
                 String phone, String city, String employeeCode) {
        super(userId, name, email, passwordHash, salt, phone, ROLE_ADMIN, city);
        this.employeeCode = employeeCode;
    }

    public Admin(String name, String email, String phone, String employeeCode) {
        this(0, name, email, null, null, phone, "", employeeCode);
    }

    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }

    public boolean canManageExperiences() {
        return true;
    }

    @Override
    public String getDashboardTitle() {
        return "ADMIN CONSOLE - " + getName() + " (" + employeeCode + ")";
    }
}
