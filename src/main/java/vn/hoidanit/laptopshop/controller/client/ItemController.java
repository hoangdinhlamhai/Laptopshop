package vn.hoidanit.laptopshop.controller.client;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.hoidanit.laptopshop.domain.*;
import vn.hoidanit.laptopshop.domain.dto.ProductCriteriaDTO;
import vn.hoidanit.laptopshop.service.ProductService;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Controller
public class ItemController {
    private final ProductService productService;

    public ItemController(final ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/product/{id}")
    public String getProductPage(@PathVariable long id, Model model) {
        Product product = this.productService.handleGetProductById(id);
        model.addAttribute("product", product);
        model.addAttribute("id", id);
        return "client/product/detail";
    }

    @PostMapping("/add-product-to-cart/{id}")
    public String addProductToCart(@PathVariable long id, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        long productId = id;
        String email = (String) session.getAttribute("email"); //mặc định của getAttribute là kiểu obj nên phải chuyển về String
        this.productService.handleAddProductToCart(email, productId, session, 1); // truyền giá trị của session thông qua controller, đã thêm attribute ở line 91 file ProductService
        return "redirect:/";
    }

    @GetMapping("/cart")
    public String getCartPage(Model model, HttpServletRequest request) {
        User currentUser = new User(); //vì hàm fetchByUser phải truyền vào đối tượng user nên phải tạo mới
        //mà tạo mới thì id = null => 2 table cart và cartdetail ko join đc
        HttpSession session = request.getSession(false);
        long id = (long) session.getAttribute("id"); // lấy id của người dùng hiện tại qua session
        currentUser.setId(id); //vì table cart và cartdetail join với nhau bằng id người dùng nên phải lấy id

        Cart cart = this.productService.fetchByUser(currentUser); //nếu là người dùng mới thì cart sẽ = null => lỗi

        //lấy cartdetail sau khi đã lấy cart
        // thay vì để cart = null thì sẽ tạo mới 1 cart rỗng thì sẽ hết lỗi
        List<CartDetail> cartDetails = cart == null ? new ArrayList<CartDetail>() : cart.getCartDetails(); //có quan hệ 1-N ở domain cart

        double totalPrice = 0;
        for (CartDetail cartDetail : cartDetails) {
            totalPrice += cartDetail.getPrice() * cartDetail.getQuantity();
        }

        model.addAttribute("cartDetails", cartDetails);
        model.addAttribute("totalPrice", totalPrice);

        model.addAttribute("cart", cart); //truyền qua để dùng vòng lặp ở file show trong folder cart

        return "client/cart/show";
    }

    @PostMapping("/delete-cart-product/{id}")
    public String deleteCartDetail(@PathVariable long id, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        this.productService.handleRemoveCartDetail(id, session);
        return "redirect:/cart";
    }

    @GetMapping("/checkout")
    public String getCheckOutPage(Model model, HttpServletRequest request) {
        User currentUser = new User();// null
        HttpSession session = request.getSession(false);
        long id = (long) session.getAttribute("id");
        currentUser.setId(id);

        Cart cart = this.productService.fetchByUser(currentUser);

        List<CartDetail> cartDetails = cart == null ? new ArrayList<CartDetail>() : cart.getCartDetails();

        double totalPrice = 0;
        for (CartDetail cd : cartDetails) {
            totalPrice += cd.getPrice() * cd.getQuantity();
        }

        model.addAttribute("cartDetails", cartDetails);
        model.addAttribute("totalPrice", totalPrice);

        return "client/cart/checkout";
    }

    //lưu sự tăng giảm số lượng trước khi checkout thanh toán
    @PostMapping("/confirm-checkout")
    public String getCheckOutPage(@ModelAttribute("cart") Cart cart) {
        List<CartDetail> cartDetails = cart == null ? new ArrayList<CartDetail>() : cart.getCartDetails();
        this.productService.handleUpdateCartBeforeCheckout(cartDetails);
        return "redirect:/checkout";
    }


    @PostMapping("/place-order")
    public String handlePlaceOrder(HttpServletRequest request,
                                   @RequestParam("receiverName") String receiverName, //lấy ra tham số ở file jsp(file checkout line 134)
                                   @RequestParam("receiverAddress") String receiverAddress,
                                   @RequestParam("receiverPhone") String receiverPhone) {
        //lay ra user
        User currentUser = new User();// null
        HttpSession session = request.getSession(false);
        long id = (long) session.getAttribute("id");
        currentUser.setId(id);

        this.productService.handlePlaceOrder(currentUser, session, receiverName, receiverAddress, receiverPhone);

        return "redirect:/thanks";
    }

    @GetMapping("/thanks")
    public String getThanksPage(Model model, HttpServletRequest request) {
        return "client/cart/thanks";
    }

    @PostMapping("/add-product-from-view-detail")
    public String addProductFromViewDetail(@ModelAttribute("product") Product product, HttpServletRequest request,
                                           //lấy trực tiếp thuộc tính từ view vì thuộc tính được định nghĩa theo kiểu name thay vì path
                                           // file product/detail line 127
                                           @RequestParam("id") long id,
                                           @RequestParam("quantity") int quantity) {
        HttpSession session = request.getSession(false);
        String email = (String) session.getAttribute("email");

        //tái sd lại hàm handleAddProductToCart
        this.productService.handleAddProductToCart(email, id, session, quantity);
        return "redirect:/product/" + id;
    }

    @GetMapping("/products")
    public String getProductsPage(Model model,
                                  //thay vì làm 1 nùi @RequestParam thì hãy:
                                  ProductCriteriaDTO productCriteriaDTO,
                                  HttpServletRequest request
                                  //lấy tham số page ở client ở query String file product/show line 76
                                  //dùng Optional để ko truyền vào thì ko lỗi
                                  //kiểu string lỡ người dùng nhập chữ
//                                  @RequestParam("page") Optional<String> pageOptional,

                                  // lấy tham số name để gõ vào url: http://8080/products?name=asus để lọc sp
                                  // biến name này ko lấy từ view, mà ở url gõ như này là tự có http://8080/products?name=asus
//                                  @RequestParam("name") Optional<String> nameOptional,

                                  //tương tự name lấy min-price
//                                  @RequestParam("min-price") Optional<String> minPriceOption,
//                                  @RequestParam("max-price") Optional<String> maxPriceOption,
//                                  @RequestParam("factory") Optional<String> factoryOption,
//                                  @RequestParam("price") Optional<String> priceOption,
//                                  @RequestParam("target") Optional<String> targetOption,
//                                  @RequestParam("sort") Optional<String> sortOption

    ) {
        //mặc định vào trang 1 nếu ng dùng ko truyền hoặc truyền sai
        int page = 1;
        try {
            if (productCriteriaDTO.getPage().isPresent()
                // người dùng có gõ vào tham số page thì dùng
            ) {
                //convert string to int
                page = Integer.parseInt(productCriteriaDTO.getPage().get());
            }
        } catch (Exception e) {
            //trường hợp người dùng nhập page = chữ => ko convert sang int được thì chạy vào đây => ứng dụng ko bị dừng
            // => page vẫn = 1
        }

        //check sort(phần sort nằm chung API của Pageable chỉ cần thêm đối số)
        //Pageable pageable = null; nếu để như này thì khi click sang trang 2 thì pageable = null => lỗi
        Pageable pageable = PageRequest.of(page - 1, 10);
        if (productCriteriaDTO.getSort() != null && productCriteriaDTO.getSort().isPresent()) {
            String sort = productCriteriaDTO.getSort().get();
            if (sort.equals("gia-tang-dan")) {
                //pageNumber phải là page-1 vì page bắt đầu từ 0
                pageable = PageRequest.of(page - 1, 10, Sort.by(Product_.PRICE).ascending()); //(lấy trang bao nhiêu, số ptu lấy lên)
            } else if (sort.equals("gia-giam-dan")) {
                //pageNumber phải là page-1 vì page bắt đầu từ 0
                pageable = PageRequest.of(page - 1, 10, Sort.by(Product_.PRICE).descending()); //(lấy trang bao nhiêu, số ptu lấy lên)
            }
        }

        //lay name (ng dùng có truyền lên thì mới lấy)
        // nếu ko ktra đkien thì lúc gõ http://localhost8080/products => lỗi vì ko truyền lên biến name
//        String name = nameOptional.isPresent() ? nameOptional.get() : "";

        //filter >= min-price
//        double minPrice = minPriceOption.isPresent() ? Double.parseDouble(minPriceOption.get()) : 0.0;
//        Page<Product> prds = this.productService.getAllProductsWithSpec(pageable, minPrice);

        //filter <= max-price
//        double maxPrice = maxPriceOption.isPresent() ? Double.parseDouble(maxPriceOption.get()) : 0.0;
//        Page<Product> prds = this.productService.getAllProductsWithSpec(pageable, maxPrice);

        //filter factory
//        String factory = factoryOption.isPresent() ? factoryOption.get() : "";
//        Page<Product> prds = this.productService.getAllProductsWithSpec(pageable, factory);

        //filter multiFactory
//        List<String> factory = Arrays.asList(factoryOption.get().split(",")); //lấy chuỗi query gồm nhiều factory ngăn cách bởi dấu phẩy
//        Page<Product> prds = this.productService.getAllProductsWithSpec(pageable, factory);

        //filter từ 10 - 15 củ
//        String price = priceOption.isPresent() ? priceOption.get() : "";
//        Page<Product> prds = this.productService.getAllProductsWithSpec(pageable, price);

        //filter 10-toi-15-trieu,16-toi-20trieu
//        List<String> price = Arrays.asList(priceOption.get().split(",")); //lấy chuỗi query gồm nhiều price ngăn cách bởi dấu phẩy
//        Page<Product> prds = this.productService.getAllProductsWithSpec(pageable, price);

        Page<Product> prds = this.productService.getAllProductsWithSpec(pageable, productCriteriaDTO);
        //đổi qua dạng List
        List<Product> productList = prds.getContent().size() > 0 ? prds.getContent() : new ArrayList<Product>(); //hàm getContent nằm trong interface Page<>

        // do ở view mỗi lần click đã có sẵn tham số page: "/products?page=${loop.index + 1}" nên mỗi lần click qua trang khác phải bỏ nó đi
        String qs = request.getParameter("qs");
        if (qs != null && !qs.isBlank()) {
            //remove page
            qs = qs.replace("page" + page, "");
        }

        model.addAttribute("products", productList);

        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", prds.getTotalPages());

        model.addAttribute("queryString", qs);

        return "client/product/show";
    }
}
