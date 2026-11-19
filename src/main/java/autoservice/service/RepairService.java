package autoservice.service;

import autoservice.model.Car;
import autoservice.model.Customer;
import autoservice.model.RepairOrder;
import autoservice.model.RepairStatus;
import autoservice.repository.RepairRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

public class RepairService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RepairService.class);

    private final RepairRepository repository;
    private final Counter operationsTotal;
    private final Counter operationsSuccess;
    private final Counter operationsError;
    private final Counter ordersCreated;
    private final Timer operationDuration;

    public RepairService(RepairRepository repository) {
        this(repository, new SimpleMeterRegistry());
    }

    public RepairService(RepairRepository repository, MeterRegistry registry) {
        this.repository = repository;
        this.operationsTotal = counter(
                registry,
                "application_operations_total",
                "Количество выполненных операций"
        );
        this.operationsSuccess = counter(
                registry,
                "application_operations_success_total",
                "Количество успешных операций"
        );
        this.operationsError = counter(
                registry,
                "application_operations_error_total",
                "Количество ошибочных операций"
        );
        this.ordersCreated = counter(
                registry,
                "autoservice_created_orders",
                "Количество успешно созданных заказов на обслуживание"
        );
        this.operationDuration = Timer.builder("application_operation_duration")
                .description("Длительность операций автосервиса")
                .register(registry);
    }

    public RepairOrder createOrder(int id, Customer customer, Car car, String description) {
        return measure(true, () -> createOrderInternal(id, customer, car, description));
    }

    private RepairOrder createOrderInternal(int id, Customer customer, Car car, String description) {
        LOGGER.info("Начало создания заказа на обслуживание: orderId={}", id);

        if (car.getCustomerId() != customer.getId()) {
            LOGGER.warn(
                    "Создание заказа отклонено: orderId={}, customerId={}, carId={}, ownerId={}",
                    id,
                    customer.getId(),
                    car.getId(),
                    car.getCustomerId()
            );

            throw new IllegalArgumentException("Автомобиль не принадлежит этому клиенту");
        }

        RepairOrder order = new RepairOrder(id, customer.getId(), car.getId(), description);

        try {
            repository.save(order);
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Ошибка сохранения заказа на обслуживание: orderId={}, carId={}",
                    id,
                    car.getId(),
                    exception
            );

            throw exception;
        }

        LOGGER.info(
                "Заказ на обслуживание создан: orderId={}, customerId={}, carId={}",
                id,
                customer.getId(),
                car.getId()
        );

        return order;
    }

    public void changeStatus(int orderId, RepairStatus newStatus) {
        measure(false, () -> {
            changeStatusInternal(orderId, newStatus);
            return null;
        });
    }

    private void changeStatusInternal(int orderId, RepairStatus newStatus) {
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
        measure(false, () -> {
            completeOrderInternal(orderId);
            return null;
        });
    }

    private void completeOrderInternal(int orderId) {
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
        measure(false, () -> {
            cancelOrderInternal(orderId);
            return null;
        });
    }

    private void cancelOrderInternal(int orderId) {
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

    private <T> T measure(boolean createdOrder, Supplier<T> action) {
        operationsTotal.increment();
        Timer.Sample sample = Timer.start();
        try {
            T result = action.get();
            operationsSuccess.increment();
            if (createdOrder) {
                ordersCreated.increment();
            }
            return result;
        } catch (RuntimeException exception) {
            operationsError.increment();
            throw exception;
        } finally {
            sample.stop(operationDuration);
        }
    }

    private static Counter counter(MeterRegistry registry, String name, String description) {
        return Counter.builder(name).description(description).register(registry);
    }
}