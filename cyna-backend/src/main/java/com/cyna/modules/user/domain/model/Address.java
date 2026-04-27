package com.cyna.modules.user.domain.model;

import java.time.Instant;
import java.util.UUID;

public class Address {

    private final UUID id;
    private final UUID userId;
    private final String firstName;
    private final String lastName;
    private final String label;
    private final String address;
    private final String address2;
    private final String zipCode;
    private final String city;
    private final String region;
    private final String countryCode;
    private final String phone;
    private final String company;
    private final String vatNumber;
    private final boolean isDefault;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Address(UUID id, UUID userId, String firstName, String lastName, String label,
                    String address, String address2, String zipCode, String city, String region,
                    String countryCode, String phone, String company, String vatNumber,
                    boolean isDefault, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.label = label;
        this.address = address;
        this.address2 = address2;
        this.zipCode = zipCode;
        this.city = city;
        this.region = region;
        this.countryCode = countryCode;
        this.phone = phone;
        this.company = company;
        this.vatNumber = vatNumber;
        this.isDefault = isDefault;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Address create(UUID userId, String firstName, String lastName, String label,
                                 String address, String address2, String zipCode, String city,
                                 String region, String countryCode, String phone,
                                 String company, String vatNumber, boolean isDefault) {
        var now = Instant.now();
        return new Address(UUID.randomUUID(), userId, firstName, lastName, label, address, address2,
                zipCode, city, region, countryCode, phone, company, vatNumber, isDefault, now, now);
    }

    public static Address reconstitute(UUID id, UUID userId, String firstName, String lastName,
                                       String label, String address, String address2, String zipCode,
                                       String city, String region, String countryCode, String phone,
                                       String company, String vatNumber, boolean isDefault,
                                       Instant createdAt, Instant updatedAt) {
        return new Address(id, userId, firstName, lastName, label, address, address2, zipCode, city,
                region, countryCode, phone, company, vatNumber, isDefault, createdAt, updatedAt);
    }

    public Address update(String firstName, String lastName, String label, String address,
                          String address2, String zipCode, String city, String region,
                          String countryCode, String phone, String company, String vatNumber) {
        return new Address(this.id, this.userId, firstName, lastName, label, address, address2,
                zipCode, city, region, countryCode, phone, company, vatNumber,
                this.isDefault, this.createdAt, Instant.now());
    }

    public Address withDefault(boolean isDefault) {
        return new Address(this.id, this.userId, this.firstName, this.lastName, this.label,
                this.address, this.address2, this.zipCode, this.city, this.region,
                this.countryCode, this.phone, this.company, this.vatNumber,
                isDefault, this.createdAt, Instant.now());
    }

    public UUID getId()            { return id; }
    public UUID getUserId()        { return userId; }
    public String getFirstName()   { return firstName; }
    public String getLastName()    { return lastName; }
    public String getLabel()       { return label; }
    public String getAddress()     { return address; }
    public String getAddress2()    { return address2; }
    public String getZipCode()     { return zipCode; }
    public String getCity()        { return city; }
    public String getRegion()      { return region; }
    public String getCountryCode() { return countryCode; }
    public String getPhone()       { return phone; }
    public String getCompany()     { return company; }
    public String getVatNumber()   { return vatNumber; }
    public boolean isDefault()     { return isDefault; }
    public Instant getCreatedAt()  { return createdAt; }
    public Instant getUpdatedAt()  { return updatedAt; }
}
