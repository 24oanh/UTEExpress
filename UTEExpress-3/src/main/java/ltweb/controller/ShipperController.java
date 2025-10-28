// ShipperController.java
package ltweb.controller;

import ltweb.entity.*;
import ltweb.entity.Package;
import ltweb.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/shipper")
@PreAuthorize("hasRole('SHIPPER')")
@RequiredArgsConstructor
public class ShipperController {

    private final ShipmentService shipmentService;
    private final OrderService orderService;
    private final AuthService authService;
    private final CloudinaryService cloudinaryService;
    private final NotificationService notificationService;
    private final TrackingService trackingService;

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication auth, HttpSession session) {
        User user = authService.findByUsername(auth.getName());
        Shipper shipper = shipmentService.getShipperByUserId(user.getId());

        session.setAttribute("currentUser", user);
        session.setAttribute("currentShipper", shipper);

        List<Order> assignedOrders = orderService.getOrdersByShipperId(shipper.getId());
        List<Shipment> pendingShipments = shipmentService.getShipmentsByShipperAndStatus(shipper.getId(), ShipmentStatus.PENDING);
        List<Shipment> inTransitShipments = shipmentService.getShipmentsByShipperAndStatus(shipper.getId(), ShipmentStatus.IN_TRANSIT);

        long unreadNotifications = notificationService.countUnreadNotifications("SHIPPER", shipper.getId());

        model.addAttribute("shipper", shipper);
        model.addAttribute("assignedOrders", assignedOrders);
        model.addAttribute("pendingShipments", pendingShipments);
        model.addAttribute("inTransitShipments", inTransitShipments);
        model.addAttribute("unreadNotifications", unreadNotifications);

        return "shipper/dashboard";
    }

    @GetMapping("/orders")
    public String listOrders(Model model, HttpSession session) {
        Shipper shipper = (Shipper) session.getAttribute("currentShipper");
        List<Order> orders = orderService.getOrdersByShipperId(shipper.getId());
        model.addAttribute("orders", orders);
        return "shipper/orders";
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable Long id, Model model, HttpSession session) {
        Order order = orderService.getOrderById(id);
        Shipper shipper = (Shipper) session.getAttribute("currentShipper");
        
        // Kiểm tra quyền
        if (order.getShipper() == null || !order.getShipper().getId().equals(shipper.getId())) {
            return "redirect:/shipper/orders";
        }
        
        List<Package> packages = orderService.getPackagesByOrderId(id);
        Shipment shipment = shipmentService.getShipmentByOrderId(id);

        model.addAttribute("order", order);
        model.addAttribute("packages", packages);
        model.addAttribute("shipment", shipment);
        return "shipper/order-detail";
    }

    // ==================== CÁC ACTION GIAO HÀNG ====================

    /**
     * Xác nhận nhận hàng từ kho
     */
    @PostMapping("/shipments/{id}/receive")
    public String receiveShipment(@PathVariable Long id, 
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        try {
            Shipper shipper = (Shipper) session.getAttribute("currentShipper");
            Shipment shipment = shipmentService.getShipmentById(id);
            
            // Kiểm tra quyền
            if (shipment.getShipper() == null || !shipment.getShipper().getId().equals(shipper.getId())) {
                redirectAttributes.addFlashAttribute("error", "Không có quyền thao tác");
                return "redirect:/shipper/orders";
            }
            
            // Kiểm tra trạng thái
            if (shipment.getStatus() != ShipmentStatus.PENDING) {
                redirectAttributes.addFlashAttribute("error", "Đơn hàng không ở trạng thái chờ lấy");
                return "redirect:/shipper/orders/" + shipment.getOrder().getId();
            }
            
            // Cập nhật thời gian lấy hàng
            shipment.setPickupTime(LocalDateTime.now());
            shipmentService.updateShipment(id, shipment);
            
            // Tạo tracking
            trackingService.createTracking(
                shipment, 
                0.0, 
                0.0, 
                "Shipper " + shipper.getName() + " đã nhận hàng từ kho", 
                TrackingStatus.STARTED
            );
            
            // Thông báo cho kho
            if (shipment.getOrder().getWarehouse() != null && 
                shipment.getOrder().getWarehouse().getUser() != null) {
                notificationService.createNotification(
                    shipment.getOrder().getWarehouse().getUser().getId(),
                    "WAREHOUSE",
                    "Shipper đã nhận hàng: " + shipment.getShipmentCode(),
                    NotificationType.ORDER_ASSIGNED,
                    shipment.getOrder()
                );
            }
            
            // Thông báo cho customer
            if (shipment.getOrder().getCustomer() != null && 
                shipment.getOrder().getCustomer().getUser() != null) {
                notificationService.createNotification(
                    shipment.getOrder().getCustomer().getUser().getId(),
                    "CUSTOMER",
                    "Đơn hàng " + shipment.getOrder().getOrderCode() + " đã được shipper nhận từ kho",
                    NotificationType.ORDER_ASSIGNED,
                    shipment.getOrder()
                );
            }
            
            redirectAttributes.addFlashAttribute("success", "✓ Đã xác nhận nhận hàng");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        
        return "redirect:/shipper/orders/" + shipmentService.getShipmentById(id).getOrder().getId();
    }

    /**
     * Bắt đầu vận chuyển
     */
    @PostMapping("/shipments/{id}/start")
    public String startShipment(@PathVariable Long id,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        try {
            Shipper shipper = (Shipper) session.getAttribute("currentShipper");
            Shipment shipment = shipmentService.getShipmentById(id);
            
            // Kiểm tra quyền
            if (shipment.getShipper() == null || !shipment.getShipper().getId().equals(shipper.getId())) {
                redirectAttributes.addFlashAttribute("error", "Không có quyền thao tác");
                return "redirect:/shipper/orders";
            }
            
            // Kiểm tra trạng thái
            if (shipment.getStatus() != ShipmentStatus.PENDING) {
                redirectAttributes.addFlashAttribute("error", "Đơn hàng không ở trạng thái chờ lấy");
                return "redirect:/shipper/orders/" + shipment.getOrder().getId();
            }
            
            // Nếu chưa có pickupTime thì set
            if (shipment.getPickupTime() == null) {
                shipment.setPickupTime(LocalDateTime.now());
            }
            
            // Cập nhật trạng thái
            shipment.setStatus(ShipmentStatus.IN_TRANSIT);
            shipmentService.updateShipment(id, shipment);
            
            // Cập nhật trạng thái đơn hàng
            Order order = shipment.getOrder();
            order.setStatus(OrderStatus.DANG_GIAO);
            orderService.updateOrderStatus(order.getId(), OrderStatus.DANG_GIAO);
            
            // Tạo tracking
            trackingService.createTracking(
                shipment, 
                0.0, 
                0.0, 
                "Đơn hàng đang được vận chuyển bởi " + shipper.getName(), 
                TrackingStatus.IN_PROGRESS
            );
            
            // Thông báo cho customer
            if (order.getCustomer() != null && order.getCustomer().getUser() != null) {
                notificationService.createNotification(
                    order.getCustomer().getUser().getId(),
                    "CUSTOMER",
                    "Đơn hàng " + order.getOrderCode() + " đang trên đường giao đến bạn",
                    NotificationType.ORDER_ASSIGNED,
                    order
                );
            }
            
            redirectAttributes.addFlashAttribute("success", "✓ Đã bắt đầu vận chuyển");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        
        return "redirect:/shipper/orders/" + shipmentService.getShipmentById(id).getOrder().getId();
    }

    /**
     * Hoàn thành giao hàng (có upload ảnh)
     */
    @PostMapping("/shipments/{id}/complete")
    public String completeShipment(@PathVariable Long id,
                                   @RequestParam(required = false) MultipartFile proofImage,
                                   @RequestParam(required = false) String notes,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        try {
            Shipper shipper = (Shipper) session.getAttribute("currentShipper");
            Shipment shipment = shipmentService.getShipmentById(id);
            
            // Kiểm tra quyền
            if (shipment.getShipper() == null || !shipment.getShipper().getId().equals(shipper.getId())) {
                redirectAttributes.addFlashAttribute("error", "Không có quyền thao tác");
                return "redirect:/shipper/orders";
            }
            
            // Kiểm tra trạng thái
            if (shipment.getStatus() != ShipmentStatus.IN_TRANSIT) {
                redirectAttributes.addFlashAttribute("error", "Đơn hàng không ở trạng thái đang giao");
                return "redirect:/shipper/orders/" + shipment.getOrder().getId();
            }
            
            // Kiểm tra ảnh bắt buộc
            if (proofImage == null || proofImage.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "⚠️ Vui lòng upload ảnh chứng từ giao hàng");
                return "redirect:/shipper/orders/" + shipment.getOrder().getId();
            }
            
            // Upload ảnh lên Cloudinary
            String imageUrl = cloudinaryService.uploadProofImage(proofImage, shipment.getShipmentCode());
            shipment.setProofImageUrl(imageUrl);
            
            // Cập nhật ghi chú
            if (notes != null && !notes.isEmpty()) {
                shipment.setNotes(notes);
            }
            
            // Cập nhật thời gian giao hàng
            shipment.setDeliveryTime(LocalDateTime.now());
            shipment.setStatus(ShipmentStatus.DELIVERED);
            shipmentService.updateShipment(id, shipment);
            
            // Cập nhật trạng thái đơn hàng
            Order order = shipment.getOrder();
            orderService.updateOrderStatus(order.getId(), OrderStatus.HOAN_THANH);
            
            // Cập nhật trạng thái packages
            List<Package> packages = orderService.getPackagesByOrderId(order.getId());
            for (Package pkg : packages) {
                pkg.setStatus(PackageStatus.DA_GIAO);
                // Save package (cần thêm service method)
            }
            
            // Tạo tracking
            trackingService.createTracking(
                shipment, 
                0.0, 
                0.0, 
                "Đã giao hàng thành công", 
                TrackingStatus.COMPLETED
            );
            
            // Cập nhật thống kê shipper
            shipper.setTotalDeliveries(shipper.getTotalDeliveries() + 1);
            shipper.setSuccessfulDeliveries(shipper.getSuccessfulDeliveries() + 1);
            // Save shipper (cần service method)
            
            // Thông báo cho customer
            if (order.getCustomer() != null && order.getCustomer().getUser() != null) {
                notificationService.createNotification(
                    order.getCustomer().getUser().getId(),
                    "CUSTOMER",
                    "✓ Đơn hàng " + order.getOrderCode() + " đã được giao thành công! Cảm ơn bạn đã sử dụng dịch vụ.",
                    NotificationType.ORDER_COMPLETED,
                    order
                );
            }
            
            // Thông báo cho kho
            if (order.getWarehouse() != null && order.getWarehouse().getUser() != null) {
                notificationService.createNotification(
                    order.getWarehouse().getUser().getId(),
                    "WAREHOUSE",
                    "Đơn hàng " + order.getOrderCode() + " đã được giao thành công",
                    NotificationType.DELIVERY_COMPLETED,
                    order
                );
            }
            
            redirectAttributes.addFlashAttribute("success", "✓ Đã hoàn thành giao hàng thành công!");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/shipper/orders/" + shipmentService.getShipmentById(id).getOrder().getId();
        }
        
        return "redirect:/shipper/orders/" + shipmentService.getShipmentById(id).getOrder().getId();
    }

    /**
     * Báo giao hàng thất bại
     */
    @PostMapping("/shipments/{id}/fail")
    public String failShipment(@PathVariable Long id, 
                               @RequestParam String notes,
                               @RequestParam(required = false) MultipartFile proofImage,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        try {
            Shipper shipper = (Shipper) session.getAttribute("currentShipper");
            Shipment shipment = shipmentService.getShipmentById(id);
            
            // Kiểm tra quyền
            if (shipment.getShipper() == null || !shipment.getShipper().getId().equals(shipper.getId())) {
                redirectAttributes.addFlashAttribute("error", "Không có quyền thao tác");
                return "redirect:/shipper/orders";
            }
            
            // Kiểm tra lý do thất bại
            if (notes == null || notes.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Vui lòng nhập lý do giao hàng thất bại");
                return "redirect:/shipper/orders/" + shipment.getOrder().getId();
            }
            
            // Upload ảnh nếu có
            if (proofImage != null && !proofImage.isEmpty()) {
                String imageUrl = cloudinaryService.uploadProofImage(proofImage, shipment.getShipmentCode() + "_failed");
                shipment.setProofImageUrl(imageUrl);
            }
            
            // Cập nhật ghi chú
            shipment.setNotes("THẤT BẠI: " + notes);
            shipment.setStatus(ShipmentStatus.FAILED);
            shipmentService.updateShipment(id, shipment);
            
            // Cập nhật trạng thái đơn hàng
            Order order = shipment.getOrder();
            orderService.updateOrderStatus(order.getId(), OrderStatus.THAT_BAI);
            
            // Tạo tracking
            trackingService.createTracking(
                shipment, 
                0.0, 
                0.0, 
                "Giao hàng thất bại: " + notes, 
                TrackingStatus.COMPLETED
            );
            
            // Cập nhật thống kê shipper
            shipper.setTotalDeliveries(shipper.getTotalDeliveries() + 1);
            shipper.setFailedDeliveries(shipper.getFailedDeliveries() + 1);
            
            // Thông báo cho customer
            if (order.getCustomer() != null && order.getCustomer().getUser() != null) {
                notificationService.createNotification(
                    order.getCustomer().getUser().getId(),
                    "CUSTOMER",
                    "⚠️ Giao hàng thất bại: " + order.getOrderCode() + ". Lý do: " + notes,
                    NotificationType.ORDER_FAILED,
                    order
                );
            }
            
            // Thông báo cho kho
            if (order.getWarehouse() != null && order.getWarehouse().getUser() != null) {
                notificationService.createNotification(
                    order.getWarehouse().getUser().getId(),
                    "WAREHOUSE",
                    "Giao hàng thất bại: " + order.getOrderCode() + ". Lý do: " + notes,
                    NotificationType.ORDER_FAILED,
                    order
                );
            }
            
            redirectAttributes.addFlashAttribute("error", "Đã báo giao hàng thất bại");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        
        return "redirect:/shipper/orders/" + shipmentService.getShipmentById(id).getOrder().getId();
    }

    /**
     * Cập nhật vị trí realtime
     */
    @PostMapping("/location/update")
    @ResponseBody
    public String updateLocation(@RequestParam Double latitude,
                                 @RequestParam Double longitude,
                                 HttpSession session) {
        try {
            Shipper shipper = (Shipper) session.getAttribute("currentShipper");
            shipmentService.updateShipperLocation(shipper.getId(), latitude, longitude);
            
            // Cập nhật tracking cho các shipment đang giao
            List<Shipment> activeShipments = shipmentService.getShipmentsByShipperAndStatus(
                shipper.getId(), 
                ShipmentStatus.IN_TRANSIT
            );
            
            for (Shipment shipment : activeShipments) {
                trackingService.updateShipmentLocation(
                    shipment.getId(), 
                    latitude, 
                    longitude, 
                    "Đang vận chuyển"
                );
            }
            
            return "success";
        } catch (Exception e) {
            return "error";
        }
    }

    @GetMapping("/shipments")
    public String listShipments(Model model, HttpSession session) {
        Shipper shipper = (Shipper) session.getAttribute("currentShipper");
        List<Shipment> shipments = shipmentService.getShipmentsByShipperId(shipper.getId());
        model.addAttribute("shipments", shipments);
        return "shipper/shipments";
    }

    @GetMapping("/shipments/{id}")
    public String shipmentDetail(@PathVariable Long id, Model model) {
        Shipment shipment = shipmentService.getShipmentById(id);
        model.addAttribute("shipment", shipment);
        return "shipper/shipment-detail";
    }

    @GetMapping("/profile")
    public String profile(Model model, HttpSession session) {
        Shipper shipper = (Shipper) session.getAttribute("currentShipper");
        model.addAttribute("shipper", shipper);
        return "shipper/profile";
    }
}
