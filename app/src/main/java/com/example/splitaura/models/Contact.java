package com.example.splitaura.models;

// Simple model class for a contact
public class Contact {
    public String name;
    public String email;
    public int profileImageRes;

    public Contact(String name, String email, int profileImageRes) {
        this.name = name;
        this.email = email;
        this.profileImageRes = profileImageRes;
    }

    // Use email as unique identifier
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Contact contact = (Contact) obj;
        return email.equals(contact.email);
    }

    @Override
    public int hashCode() {
        return email.hashCode();
    }
}
