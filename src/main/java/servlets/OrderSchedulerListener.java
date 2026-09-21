package servlets;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

import com.bittercode.service.OrderService;
import com.bittercode.service.impl.OrderServiceImpl;
import com.bittercode.util.EmailConfig;
import com.bittercode.util.OrderEmailService;

public class OrderSchedulerListener implements ServletContextListener {

    private ScheduledExecutorService scheduler;
    private final OrderService orderService = new OrderServiceImpl();

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        EmailConfig.initialize(sce.getServletContext().getRealPath("/"));
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Order-Auto-Complete-Thread");
            t.setDaemon(true);
            return t;
        });

        // Run auto-complete check every 30 minutes (initial delay 1 minute)
        scheduler.scheduleAtFixedRate(() -> {
            try {
                int count = orderService.checkAndAutoCompleteOrders();
                if (count > 0) {
                    System.out.println("[OrderScheduler] Auto-completed " + count + " shipping orders past 3 days.");
                }
            } catch (Exception e) {
                System.err.println("[OrderScheduler] Error checking auto-complete orders: " + e.getMessage());
            }
        }, 1, 30, TimeUnit.MINUTES);

        System.out.println("[OrderSchedulerListener] Order auto-complete scheduler initialized.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
            System.out.println("[OrderSchedulerListener] Order auto-complete scheduler shut down.");
        }
        OrderEmailService.getInstance().shutdown();
    }
}
