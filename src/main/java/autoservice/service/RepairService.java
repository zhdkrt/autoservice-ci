package autoservice.service;

import autoservice.model.Car;
import autoservice.model.Customer;
import autoservice.model.RepairOrder;
import autoservice.model.RepairStatus;
import autoservice.repository.RepairRepository;

public class RepairService {
    private final RepairRepository repository;

    public RepairService(RepairRepository repository) {
        this.repository = repository;
    }

    public RepairOrder createOrder(int id, Customer customer, Car car, String description) {
        if (car.getCustomerId() != customer.getId()) {
            throw new IllegalArgumentException("Автомобиль не принадлежит этому клиенту");
        }

        RepairOrder order = new RepairOrder(id, customer.getId(), car.getId(), description);
        repository.save(order);
        return order;
    }

    public void changeStatus(int orderId, RepairStatus newStatus) {
        RepairOrder order = requireOrder(orderId);

        if (newStatus == RepairStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Заказ нельзя закрыть через смену статуса. Используйте completeOrder"
            );
        }

        if (!isAllowedRepairTransition(order.getStatus(), newStatus)) {
            throw new IllegalStateException(
                    "Недопустимый переход статуса: " + order.getStatus() + " → " + newStatus
            );
        }

        order.setStatus(newStatus);
        repository.save(order);
    }

    public void completeOrder(int orderId) {
        RepairOrder order = requireOrder(orderId);

        if (order.getStatus() != RepairStatus.REPAIRED) {
            throw new IllegalStateException(
                    "Нельзя завершить заказ: ремонт ещё не выполнен"
            );
        }

        order.setStatus(RepairStatus.COMPLETED);
        repository.save(order);
    }

    public void cancelOrder(int orderId) {
        RepairOrder order = requireOrder(orderId);

        if (order.getStatus() != RepairStatus.NEW) {
            throw new IllegalStateException(
                    "Отменить можно только новый заказ"
            );
        }

        order.setStatus(RepairStatus.CANCELLED);
        repository.save(order);
    }
    
    public RepairOrder findOrder(int orderId) {
        return requireOrder(orderId);
    }

    private RepairOrder requireOrder(int orderId) {
        RepairOrder order = repository.findById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Заказ не найден: " + orderId);
        }
        return order;
    }

    private boolean isAllowedRepairTransition(RepairStatus current, RepairStatus next) {
        return (current == RepairStatus.NEW && next == RepairStatus.IN_PROGRESS)
                || (current == RepairStatus.IN_PROGRESS && next == RepairStatus.REPAIRED);
    }
}
