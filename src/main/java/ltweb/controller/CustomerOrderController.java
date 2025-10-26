package ltweb.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import ltweb.entity.CustomerOrder;
import ltweb.entity.Order;
import ltweb.entity.Warehouse;
import ltweb.repository.WarehouseRepository;
import ltweb.service.CustomerOrderService;

@Controller
@RequestMapping("/customer/orders")
@RequiredArgsConstructor
public class CustomerOrderController {
    private final CustomerOrderService customerOrderService;
    private final WarehouseRepository warehouseRepository;

    // CustomerOrderController.java - Sửa method acceptCustomerOrder
    @PostMapping("/accept-customer-order/{id}")
    public String acceptCustomerOrder(@PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        try {
            Order order = customerOrderService.acceptSingleOrder(id);
            redirectAttributes.addFlashAttribute("success", "Đã tiếp nhận đơn hàng " + order.getOrderCode());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            e.printStackTrace();
        }
        return "redirect:/warehouse/orders/pending";
    }

    @GetMapping("/create")
    public String createOrderForm(Model model) {
        List<Warehouse> warehouses = warehouseRepository.findAll();
        model.addAttribute("warehouses", warehouses);
        model.addAttribute("customerOrder", new CustomerOrder());
        return "customer/order-form";
    }

    @PostMapping("/create")
    public String createOrder(@ModelAttribute CustomerOrder customerOrder,
            RedirectAttributes redirectAttributes) {
        try {
            customerOrderService.createCustomerOrder(customerOrder);
            redirectAttributes.addFlashAttribute("success", "Đơn hàng đã được tạo. Vui lòng chờ xác nhận.");
            return "redirect:/customer/orders/success";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/customer/orders/create";
        }
    }

    @GetMapping("/success")
    public String orderSuccess() {
        return "customer/order-success";
    }
}