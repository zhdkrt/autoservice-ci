package autoservice;

import autoservice.model.Car;
import autoservice.model.Customer;
import autoservice.model.RepairOrder;
import autoservice.model.RepairStatus;
import autoservice.repository.InMemoryRepairRepository;
import autoservice.repository.RepairRepository;
import autoservice.service.RepairService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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
                () -> service.completeOrder(2)
        );

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
                () -> service.completeOrder(3)
        );

        assertTrue(exception.getMessage().contains("ремонт ещё не выполнен"));
        assertEquals(RepairStatus.IN_PROGRESS, service.findOrder(3).getStatus());

        service.changeStatus(3, RepairStatus.REPAIRED);
        service.completeOrder(3);

        assertEquals(RepairStatus.COMPLETED, service.findOrder(3).getStatus());
    }

    @Test
    @DisplayName("Новый заказ можно отменить")
    void cancelsNewOrder() {
        Customer customer = new Customer(4, "Иван", "444");
        Car car = new Car(4, "Toyota", "A004AA77", customer.getId());

        service.createOrder(4, customer, car, "Замена тормозных колодок");
        service.cancelOrder(4);

        assertEquals(
                RepairStatus.CANCELLED,
                service.findOrder(4).getStatus()
        );
    }

    @Test
    @DisplayName("Нельзя отменить заказ после начала ремонта")
    void cannotCancelOrderInProgress() {
        Customer customer = new Customer(5, "Сергей", "555");
        Car car = new Car(5, "Ford", "A005AA77", customer.getId());

        service.createOrder(5, customer, car, "Ремонт двигателя");
        service.changeStatus(5, RepairStatus.IN_PROGRESS);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.cancelOrder(5)
        );

        assertTrue(
                exception.getMessage().contains("Отменить можно только новый заказ")
        );

        assertEquals(
                RepairStatus.IN_PROGRESS,
                service.findOrder(5).getStatus()
        );
    }

    @Test
    @DisplayName("Отменённый заказ нельзя отправить в ремонт")
    void cannotStartCancelledOrder() {
        Customer customer = new Customer(6, "Елена", "666");
        Car car = new Car(6, "Honda", "A006AA77", customer.getId());

        service.createOrder(6, customer, car, "Замена аккумулятора");
        service.cancelOrder(6);

        assertThrows(
                IllegalStateException.class,
                () -> service.changeStatus(6, RepairStatus.IN_PROGRESS)
        );

        assertEquals(
                RepairStatus.CANCELLED,
                service.findOrder(6).getStatus()
        );
    }

    @Test
    @DisplayName("WARN: нельзя создать заказ на автомобиль другого клиента")
    void rejectsCarOwnedByAnotherCustomer() {
        InMemoryRepairRepository repository = new InMemoryRepairRepository();
        RepairService localService = new RepairService(repository);

        Customer customer = new Customer(7, "Алексей", "777");
        Car car = new Car(7, "BMW", "A007AA77", 999);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> localService.createOrder(7, customer, car, "Диагностика")
        );

        assertTrue(
                exception.getMessage().contains("Автомобиль не принадлежит этому клиенту")
        );

        assertNull(repository.findById(7));
    }

    @Test
    @DisplayName("ERROR: ошибка сохранения заказа передаётся вызывающему коду")
    void propagatesRepositorySaveFailure() {
        IllegalStateException storageFailure =
                new IllegalStateException("Хранилище заказов недоступно");

        RepairRepository failingRepository = new RepairRepository() {
            @Override
            public void save(RepairOrder order) {
                throw storageFailure;
            }

            @Override
            public RepairOrder findById(int id) {
                return null;
            }
        };

        RepairService localService = new RepairService(failingRepository);

        Customer customer = new Customer(8, "Дмитрий", "888");
        Car car = new Car(8, "Audi", "A008AA77", customer.getId());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> localService.createOrder(8, customer, car, "Ремонт тормозов")
        );

        assertSame(storageFailure, exception);
    }
}