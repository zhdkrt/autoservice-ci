package autoservice;

import autoservice.model.Car;
import autoservice.model.Customer;
import autoservice.model.RepairOrder;
import autoservice.model.RepairStatus;
import autoservice.repository.InMemoryRepairRepository;
import autoservice.service.RepairService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepairServiceTest {
    private RepairService service;

    @BeforeEach
    void setUp() {
        service = new RepairService(new InMemoryRepairRepository());
    }

    @Test
    @DisplayName("Успешный сценарий: заказ завершён после ремонта")
    void completesOrderAfterRepairIsDone() {
        Customer customer = new Customer(1, "Анна", "111");
        Car car = new Car(1, "Kia", "B001AA77", customer.getId());

        RepairOrder order = service.createOrder(1, customer, car, "Замена масла");
        assertEquals(RepairStatus.NEW, order.getStatus());

        service.changeStatus(1, RepairStatus.IN_PROGRESS);
        service.changeStatus(1, RepairStatus.REPAIRED);
        service.completeOrder(1);

        assertEquals(RepairStatus.COMPLETED, service.findOrder(1).getStatus());
    }

    @Test
    @DisplayName("Нельзя завершить заказ, если ремонт ещё не начат")
    void cannotCompleteNewOrder() {
        Customer customer = new Customer(2, "Олег", "222");
        Car car = new Car(2, "VW", "C002CC77", customer.getId());

        service.createOrder(2, customer, car, "Ремонт подвески");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.completeOrder(2));

        assertTrue(exception.getMessage().contains("ремонт ещё не выполнен"));
        assertEquals(RepairStatus.NEW, service.findOrder(2).getStatus());
    }

    @Test
    @DisplayName("Нельзя завершить заказ в ремонте, можно после REPAIRED")
    void cannotCompleteUntilRepairIsDone() {
        Customer customer = new Customer(3, "Мария", "333");
        Car car = new Car(3, "Hyundai", "E003EE77", customer.getId());

        service.createOrder(3, customer, car, "Покраска бампера");
        service.changeStatus(3, RepairStatus.IN_PROGRESS);
        assertEquals(RepairStatus.IN_PROGRESS, service.findOrder(3).getStatus());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.completeOrder(3));

        assertTrue(exception.getMessage().contains("ремонт ещё не выполнен"));
        assertEquals(RepairStatus.IN_PROGRESS, service.findOrder(3).getStatus());

        service.changeStatus(3, RepairStatus.REPAIRED);
        service.completeOrder(3);
        assertEquals(RepairStatus.COMPLETED, service.findOrder(3).getStatus());
    }
}
