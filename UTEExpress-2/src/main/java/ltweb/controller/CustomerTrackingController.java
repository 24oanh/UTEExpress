// CustomerTrackingController.java
package ltweb.controller;

import ltweb.entity.*;
import ltweb.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import java.util.List;

@Controller
@RequestMapping("/customer")
@PreAuthorize("hasRole('CUSTOMER')")
@RequiredArgsConstructor
public class CustomerTrackingController {

    private final OrderService orderService;
    private final TrackingService trackingService;
    private final ShipmentService shipmentService;

    @GetMapping("/tracking")
    public String trackingPage(@RequestParam(required = false) String orderCode, Model model, HttpSession session) {
        if (orderCode != null && !orderCode.isEmpty()) {
            try {
                Customer customer = (Customer) session.getAttribute("currentCustomer");
                Order order = orderService.getOrderByCode(orderCode);
                
                if (!order.getCustomer().getId().equals(customer.getId())) {
                    model.addAttribute("error", "Không tìm thấy đơn hàng");
                    return "customer/tracking";
                }

                Shipment shipment = shipmentService.getShipmentByOrderId(order.getId());
                List<Tracking> trackings = shipment != null ? 
                    trackingService.getTrackingsByShipmentId(shipment.getId()) : 
                    List.of();

                model.addAttribute("order", order);
                model.addAttribute("shipment", shipment);
                model.addAttribute("trackings", trackings);
            } catch (Exception e) {
                model.addAttribute("error", "Không tìm thấy đơn hàng: " + orderCode);
            }
        }
        return "customer/tracking";
    }

    @GetMapping("/tracking/{orderCode}")
    public String trackOrderByPath(@PathVariable String orderCode, HttpSession session) {
        return "redirect:/customer/tracking?orderCode=" + orderCode;
    }
}