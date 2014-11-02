package org.lazydoc.example.spring.entity;

import org.lazydoc.annotation.PropertyDescription;
import org.lazydoc.annotation.Sample;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class Customer {

    @Id
    @GeneratedValue
    @PropertyDescription(description = "customerId")
    private long customerId;
    @PropertyDescription(description = "lastname")
    @Sample("Müller")
    private String lastname;
    @PropertyDescription(description = "lastname")
    @Sample("Maxi")
    private String firstname;
    @PropertyDescription(description = "lastname")
    private String street;
    @PropertyDescription(description = "lastname")
    private String zipCode;
    @PropertyDescription(description = "lastname")
    private String city;

    public long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(long customerId) {
        this.customerId = customerId;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }
}
