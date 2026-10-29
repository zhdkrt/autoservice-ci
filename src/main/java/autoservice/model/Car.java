package autoservice.model;

public class Car {
    private final int id;
    private final String brand;
    private final String licensePlate;
    private final int customerId;

    public Car(int id, String brand, String licensePlate, int customerId) {
        this.id = id;
        this.brand = brand;
        this.licensePlate = licensePlate;
        this.customerId = customerId;
    }

    public int getId() {
        return id;
    }

    public String getBrand() {
        return brand;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public int getCustomerId() {
        return customerId;
    }
}
