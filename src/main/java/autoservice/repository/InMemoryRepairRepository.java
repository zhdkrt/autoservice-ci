package autoservice.repository;

import autoservice.model.RepairOrder;

import java.util.HashMap;
import java.util.Map;

public class InMemoryRepairRepository implements RepairRepository {
    private final Map<Integer, RepairOrder> storage = new HashMap<>();

    @Override
    public void save(RepairOrder order) {
        storage.put(order.getId(), order);
    }

    @Override
    public RepairOrder findById(int id) {
        return storage.get(id);
    }
}
