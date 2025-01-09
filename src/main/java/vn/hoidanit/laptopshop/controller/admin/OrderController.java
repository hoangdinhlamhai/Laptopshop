package vn.hoidanit.laptopshop.controller.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.hoidanit.laptopshop.domain.Order;
import vn.hoidanit.laptopshop.domain.OrderDetail;
import vn.hoidanit.laptopshop.service.OrderService;

import java.util.List;
import java.util.Optional;

@Controller
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/admin/order")
    public String getDashboard(Model model,
                               //lấy tham số page ở client ở query String file product/show line 76
                               @RequestParam("page") Optional<String> pageOptional
                               //Lỗi ko bắt buộc thì dùng Optional
                               //kiểu string lỡ người dùng nhập chữ
    ) {
        //mặc định vào trang 1 nếu ng dùng ko truyền hoặc truyền sai
        int page = 1;
        try {
            if (pageOptional.isPresent()
                // người dùng có gõ vào tham số page thì dùng
            ) {
                //convert string to int
                page = Integer.parseInt(pageOptional.get());
            }
        } catch (Exception e) {
            //trường hợp người dùng nhập page = chữ => ko convert sang int được thì chạy vào đây => ứng dụng ko bị dừng
            // => page vẫn = 1
        }

        Pageable pageable = PageRequest.of(page - 1, 2); //(lấy trang bao nhiêu, số ptu lấy lên)
//        List<Order> orders = this.orderService.fetchAllOrders();
        Page<Order> orders = this.orderService.fetchAllOrders(pageable);
        List<Order> orderList = orders.getContent();
        model.addAttribute("orders", orderList);

        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", orders.getTotalPages());
        return "admin/order/show";
    }

    @GetMapping("/admin/order/{id}")
    public String getOrderViewPage(@PathVariable long id, Model model) {
        List<OrderDetail> orderDetails = this.orderService.fetchAllOrderDetail();
        model.addAttribute("orderDetails", orderDetails);
        model.addAttribute("id", id);
        return "admin/order/detail";
    }

    @GetMapping("/admin/order/delete/{id}")
    public String getDeleteOrderPage(Model model, @PathVariable long id) {
        model.addAttribute("id", id);
        model.addAttribute("newOrder", new Order());
        return "admin/order/delete";
    }

    @PostMapping("/admin/order/delete")
    public String postDeleteOrder(@ModelAttribute("newOrder") Order order) {
        this.orderService.deleteOrderById(order.getId());
        return "redirect:/admin/order";
    }

    @GetMapping("/admin/order/update/{id}")
    public String getUpdateOrderPage(Model model, @PathVariable long id) {
        Optional<Order> currentOrder = this.orderService.fetchOrderById(id);
        model.addAttribute("newOrder", currentOrder.get());
        return "admin/order/update";
    }

    @PostMapping("/admin/order/update")
    public String handleUpdateOrder(@ModelAttribute("newOrder") Order order) {
        this.orderService.updateOrder(order);
        return "redirect:/admin/order";
    }
}
