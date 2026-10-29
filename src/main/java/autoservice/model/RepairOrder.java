package autoservice.model;

public class RepairOrder {
    private final int id;
    private final int customerId;
    private final int carId;
    private final String description;
    private RepairStatus status;

    public RepairOrder(int id, int customerId, int carId, String description) {
        this.id = id;
        this.customerId = customerId;
        this.carId = carId;
        this.description = description;
        this.status = RepairStatus.NEW;
    }

    public int getId() {
        return id;
    }

    public int getCustomerId() {
        return customerId;
    }

    public int getCarId() {
        return carId;
    }

    public String getDescription() {
        return description;
    }

    public RepairStatus getStatus() {
        return status;
    }

    public void setStatus(RepairStatus status) {
        this.status = status;
    }
}
