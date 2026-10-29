package autoservice.repository;

import autoservice.model.RepairOrder;

public interface RepairRepository {
    void save(RepairOrder order);

    RepairOrder findById(int id);
}
