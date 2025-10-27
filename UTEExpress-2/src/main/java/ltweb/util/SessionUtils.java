package ltweb.util;

import ltweb.entity.Shipper;
import ltweb.entity.User;
import ltweb.entity.Warehouse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

@Component
public class SessionUtils {
    
    public static User getCurrentUser(HttpSession session) {
        return (User) session.getAttribute("currentUser");
    }
    
    public static void setCurrentUser(HttpSession session, User user) {
        session.setAttribute("currentUser", user);
    }
    
    public static Warehouse getCurrentWarehouse(HttpSession session) {
        return (Warehouse) session.getAttribute("currentWarehouse");
    }
    
    public static void setCurrentWarehouse(HttpSession session, Warehouse warehouse) {
        session.setAttribute("currentWarehouse", warehouse);
    }
    
    public static Shipper getCurrentShipper(HttpSession session) {
        return (Shipper) session.getAttribute("currentShipper");
    }
    
    public static void setCurrentShipper(HttpSession session, Shipper shipper) {
        session.setAttribute("currentShipper", shipper);
    }
    
    public static Long getCurrentUserId(HttpSession session) {
        User user = getCurrentUser(session);
        return user != null ? user.getId() : null;
    }
    
    public static Long getCurrentWarehouseId(HttpSession session) {
        Warehouse warehouse = getCurrentWarehouse(session);
        return warehouse != null ? warehouse.getId() : null;
    }
    
    public static Long getCurrentShipperId(HttpSession session) {
        Shipper shipper = getCurrentShipper(session);
        return shipper != null ? shipper.getId() : null;
    }
    
    public static String getCurrentUsername(HttpSession session) {
        User user = getCurrentUser(session);
        return user != null ? user.getUsername() : null;
    }
    
    public static void clearSession(HttpSession session) {
        session.removeAttribute("currentUser");
        session.removeAttribute("currentWarehouse");
        session.removeAttribute("currentShipper");
    }
    
    public static boolean isUserLoggedIn(HttpSession session) {
        return getCurrentUser(session) != null;
    }
    
    public static boolean hasWarehouse(HttpSession session) {
        return getCurrentWarehouse(session) != null;
    }
    
    public static boolean hasShipper(HttpSession session) {
        return getCurrentShipper(session) != null;
    }
}