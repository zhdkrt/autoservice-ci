package autoservice;

import autoservice.metrics.MetricsServer;
import autoservice.model.Car;
import autoservice.model.Customer;
import autoservice.model.RepairOrder;
import autoservice.model.RepairStatus;
import autoservice.repository.InMemoryRepairRepository;
import autoservice.repository.RepairRepository;
import autoservice.service.RepairService;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

import java.util.Scanner;

public class Application {
    public static void main(String[] args) throws Exception {
        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        MetricsServer metricsServer = new MetricsServer(registry, 8080);

        RepairRepository repository = new InMemoryRepairRepository();
        RepairService service = new RepairService(repository, registry);

        Customer customer = new Customer(1, "Иван Петров", "+123456789");
        Car car = new Car(10, "Toyota", "1234BC", customer.getId());

        System.out.println("Клиент: " + customer.getName() + ", телефон " + customer.getPhone());
        System.out.println("Автомобиль: " + car.getBrand() + ", номер " + car.getLicensePlate());
        System.out.println();

        RepairOrder order = service.createOrder(100, customer, car, "Замена тормозных колодок");
        System.out.println("Создан заказ #" + order.getId() + ": " + order.getDescription());
        printOrder(order);

        System.out.println();
        System.out.println("Попытка закрыть заказ сразу после создания");
        tryComplete(service, order.getId());
        printOrder(service.findOrder(order.getId()));

        System.out.println();
        changeStatus(service, order.getId(), RepairStatus.IN_PROGRESS);
        System.out.println("Попытка закрыть заказ, пока ремонт ещё идёт");
        tryComplete(service, order.getId());
        printOrder(service.findOrder(order.getId()));

        System.out.println();
        changeStatus(service, order.getId(), RepairStatus.REPAIRED);
        printOrder(service.findOrder(order.getId()));

        System.out.println();
        System.out.println("Ремонт выполнен, заказ можно закрыть");
        service.completeOrder(order.getId());
        printOrder(service.findOrder(order.getId()));

        System.out.println();
        System.out.println("Ошибочный сценарий: заказ на автомобиль другого клиента");
        rejectForeignCar(service);

        System.out.println();
        System.out.println("Метрики: http://localhost:8080/metrics");
        System.out.println("Команды: 1 — новый заказ, 2 — ошибочный заказ, 0 — выход");

        Scanner scanner = new Scanner(System.in);
        int nextOrderId = 300;
        boolean running = true;
        while (running) {
            System.out.print("> ");
            String command = scanner.nextLine().trim();
            if ("1".equals(command)) {
                createExtraOrder(service, customer, car, nextOrderId);
                nextOrderId = nextOrderId + 1;
            } else if ("2".equals(command)) {
                rejectForeignCar(service);
            } else if ("0".equals(command)) {
                running = false;
            } else {
                System.out.println("Неизвестная команда. 1 — заказ, 2 — ошибка, 0 — выход");
            }
        }

        metricsServer.stop();
    }

    private static void createExtraOrder(
            RepairService service,
            Customer customer,
            Car car,
            int orderId
    ) {
        RepairOrder order = service.createOrder(orderId, customer, car, "Дополнительное обслуживание");
        System.out.println("Создан заказ #" + order.getId());
    }

    private static void rejectForeignCar(RepairService service) {
        Customer customer = new Customer(2, "Пётр Сидоров", "+987654321");
        Car foreignCar = new Car(11, "BMW", "7777XX", 99);
        try {
            service.createOrder(200, customer, foreignCar, "Замена масла");
            System.out.println("Заказ создан");
        } catch (IllegalArgumentException exception) {
            System.out.println("Отказ. " + exception.getMessage());
        }
    }

    private static void changeStatus(RepairService service, int orderId, RepairStatus next) {
        RepairStatus before = service.findOrder(orderId).getStatus();
        service.changeStatus(orderId, next);
        RepairStatus after = service.findOrder(orderId).getStatus();
        System.out.println("Смена статуса: " + label(before) + " → " + label(after));
    }

    private static void tryComplete(RepairService service, int orderId) {
        RepairStatus current = service.findOrder(orderId).getStatus();
        try {
            service.completeOrder(orderId);
            System.out.println("Завершение при статусе " + label(current) + ": разрешено");
        } catch (IllegalStateException e) {
            System.out.println("Завершение при статусе " + label(current) + ": отказ. " + e.getMessage());
        }
    }

    private static void printOrder(RepairOrder order) {
        System.out.println("Заказ #" + order.getId() + " сейчас: " + label(order.getStatus()));
    }

    private static String label(RepairStatus status) {
        return switch (status) {
            case NEW -> "NEW (принят)";
            case IN_PROGRESS -> "IN_PROGRESS (в ремонте)";
            case REPAIRED -> "REPAIRED (ремонт выполнен)";
            case COMPLETED -> "COMPLETED (закрыт)";
            case CANCELLED -> "CANCELLED (отменён)";
        };
    }
}
