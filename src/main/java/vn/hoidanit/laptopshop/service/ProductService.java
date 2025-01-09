package vn.hoidanit.laptopshop.service;

import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import vn.hoidanit.laptopshop.domain.*;
import vn.hoidanit.laptopshop.domain.dto.ProductCriteriaDTO;
import vn.hoidanit.laptopshop.repository.*;
import vn.hoidanit.laptopshop.service.specification.ProductSpecs;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;
    private final CartDetailRepository cartDetailRepository;
    private final UserService userService;
    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;

    public ProductService(OrderDetailRepository orderDetailRepository, OrderRepository orderRepository, UserService userService, ProductRepository productRepository, CartRepository cartRepository, CartDetailRepository cartDetailRepository) {
        this.productRepository = productRepository;
        this.cartRepository = cartRepository;
        this.cartDetailRepository = cartDetailRepository;
        this.userService = userService;
        this.orderRepository = orderRepository;
        this.orderDetailRepository = orderDetailRepository;
    }

    //hàm save và findAll là có sẵn của JpaRepository, ko cần định nghĩa thêm bên class Repository
    public Product handleSaveProduct(Product product) {
        return productRepository.save(product);
    }

    public Page<Product> getAllProductsWithSpec(Pageable pageable, ProductCriteriaDTO productCriteriaDTO) {
        //chạy lần đầu thì chưa có lọc j hết tức là các tham số == null => thì sẽ lỗi, nên phải có hàm if để check
        if (productCriteriaDTO.getTarget() == null && productCriteriaDTO.getFactory() == null && productCriteriaDTO.getPrice() == null) {
            return productRepository.findAll(pageable);
        }

        Specification<Product> combinedSpec = Specification.where(null); //thay cho disjunction();

        //ktra xem ng dùng có truyền lên biến target ko
        if (productCriteriaDTO.getTarget() != null && productCriteriaDTO.getTarget().isPresent()) {
            Specification<Product> currentSpecs = ProductSpecs.matchListTarget(productCriteriaDTO.getTarget().get());
            combinedSpec = combinedSpec.and(currentSpecs); //cộng gộp vào combinedSpec
        }
        if (productCriteriaDTO.getFactory() != null && productCriteriaDTO.getFactory().isPresent()) {
            Specification<Product> currentSpecs = ProductSpecs.matchListFactory(productCriteriaDTO.getFactory().get());
            combinedSpec = combinedSpec.and(currentSpecs);
        }

        if (productCriteriaDTO.getPrice() != null && productCriteriaDTO.getPrice().isPresent()) {
            Specification<Product> currentSpecs = this.buildPriceSpecification(productCriteriaDTO.getPrice().get());
            combinedSpec = combinedSpec.and(currentSpecs);
        }

        return this.productRepository.findAll(combinedSpec, pageable);
    }

    public Specification<Product> buildPriceSpecification(List<String> price) {
        Specification<Product> combinedSpec = Specification.where(null); //thay cho disjunction();
//        Specification<Product> combinedSpec = (root, query, criteriaBuilder) -> criteriaBuilder.disjunction();
        //nếu chạy lần đầu thì combinedSpec sẽ = null, nên dùng .disjunction() để khỏi null mới có thể thao tác được với các toán tử
        for (String p : price) {
            double min = 0, max = 0;
            switch (p) {
                case "duoi-10-trieu":
                    min = 1;
                    max = 10000000;
                    break;
                case "10-15-trieu":
                    min = 10000000;
                    max = 15000000;
                    break;
                case "15-20-trieu":
                    min = 15000000;
                    max = 20000000;
                    break;
                case "tren-20-trieu":
                    min = 20000000;
                    max = 200000000;
                    break;
            }

            //nếu rơi vào 1 trong các case trên
            if (min != 0 && max != 0) {
                Specification<Product> rangeSpec = ProductSpecs.matchMultiPrice(min, max); //nếu chỉ như này thì nó sẽ query riêng lẻ
                combinedSpec = combinedSpec.or(rangeSpec); //hàm or để chỉ cần rơi vào 1 case thì tự động cộng gộp vào cái case còn lại
                // nếu hàm and thì cần tất cả case đều phải đúng
            }
        }

        return combinedSpec;
    }

    // này để filter (name)
//    public Page<Product> getAllProductsWithSpec(Pageable pageable, String name) {
//        return this.productRepository.findAll(ProductSpecs.nameLike(name), pageable); //ProductSpecs.nameLike() ở folder service/specification
//    }

    //min-price
//    public Page<Product> getAllProductsWithSpec(Pageable pageable, double min) {
//        return this.productRepository.findAll(ProductSpecs.priceMin(min), pageable);
//    }

    //max-price
//    public Page<Product> getAllProductsWithSpec(Pageable pageable, double max) {
//        return this.productRepository.findAll(ProductSpecs.priceMax(max), pageable);
//    }

    //factory
//    public Page<Product> getAllProductsWithSpec(Pageable pageable, String fac) {
//        return this.productRepository.findAll(ProductSpecs.factory(fac), pageable);
//    }

    //multiFactory
//    public Page<Product> getAllProductsWithSpec(Pageable pageable, List<String> fac) {
//        return this.productRepository.findAll(ProductSpecs.matchListFactory(fac), pageable);
//    }

    //filter giá từ 10 - 15 củ
//    public Page<Product> getAllProductsWithSpec(Pageable pageable, String price) {
//        // do ko biết được chuỗi string sẽ là giá tiền từ bnhieu tới bnhieu nên phải hard code
//        if (price.equals("10-toi-15-trieu")) {
//            double min = 10000000, max = 15000000;
//            return this.productRepository.findAll(ProductSpecs.matchPrice(min, max), pageable);
//        } else if (price.equals("15-toi-30-trieu")) {
//            double min = 15000000, max = 30000000;
//            return this.productRepository.findAll(ProductSpecs.matchPrice(min, max), pageable);
//        } else {
//            return this.productRepository.findAll(pageable);
//        }
//    }

    //filter 10-toi-15-trieu,16-toi-20trieu
//    public Page<Product> getAllProductsWithSpec(Pageable pageable, List<String> price) {
//        Specification<Product> combinedSpec = (root, query, criteriaBuilder) -> criteriaBuilder.disjunction();
//        //nếu chạy lần đầu thì combinedSpec sẽ = null, nên dùng .disjunction() để khỏi null mới có thể thao tác được với các toán tử
//        int count = 0;
//        for (String p : price) {
//            double min = 0, max = 0;
//            switch (p) {
//                case "10-toi-15-trieu":
//                    min = 10000000;
//                    max = 15000000;
//                    count++;
//                    break;
//                case "15-toi-20-trieu":
//                    min = 15000000;
//                    max = 20000000;
//                    count++;
//                    break;
//                case "20-toi-30-trieu":
//                    min = 20000000;
//                    max = 30000000;
//                    count++;
//                    break;
//            }
//
//            //nếu rơi vào 1 trong các case trên
//            if (min != 0 && max != 0) {
//                Specification<Product> rangeSpec = ProductSpecs.matchMultiPrice(min, max); //nếu chỉ như này thì nó sẽ query riêng lẻ
//                combinedSpec = combinedSpec.or(rangeSpec); //hàm or để chỉ cần rơi vào 1 case thì tự động cộng gộp vào cái case còn lại
//                // nếu hàm and thì cần tất cả case đều phải đúng
//            }
//        }
//        if (count == 0) {
//            return this.productRepository.findAll(pageable);
//        }
//        return this.productRepository.findAll(combinedSpec, pageable);
//    }

    //này là hàm cũ
//    public List<Product> getAllProducts() {
//        return this.productRepository.findAll();
//    }

    //hàm mới sau khi thêm phần phân trang
    public Page<Product> getAllProducts(Pageable pageable) {
        return this.productRepository.findAll(pageable);
    }

//    Dùng Optional sẽ trả về có kiểu hoặc ko, nếu có kiểu thì lúc sử dụng phải thêm .get();
//    public Optional<Product> handleGetProductById(long id) {
//        return this.productRepository.findById(id);
//    }

    public Product handleGetProductById(long id) {
        return this.productRepository.findById(id);
    }

    public void handleDeleteProduct(long id) {
        this.productRepository.deleteById(id);
    }

    //logic lưu giỏ hàng
    public void handleAddProductToCart(String email, long productId, HttpSession session, long quantity) {
        //lấy ra user
        User user = userService.getUserByEmail(email);
        if (user != null) {
            //check user đã có cart chưa, chưa có thì tạo mới
            Cart cart = this.cartRepository.findByUser(user);

            if (cart == null) {
                //tạo mới cart
                Cart otherCart = new Cart();
                otherCart.setUser(user);
                otherCart.setSum(0);

                cart = this.cartRepository.save(otherCart);
            }

            //save cart_detail
            //find product by id
            //mặc định của Jpa sẽ trả ra optional bọc ngoài ntn
            Optional<Product> productOptional = Optional.ofNullable(this.productRepository.findById(productId));
            //do có optional nên phải check điều kiện null
            if (productOptional.isPresent()) {
                Product realProduct = productOptional.get();

                //check sản phẩm đã có trong giỏ hàng chưa, nếu có chỉ cần tăng quantity thêm 1
                CartDetail oldDetail = this.cartDetailRepository.findByCartAndProduct(cart, realProduct);
                if (oldDetail == null) {
                    CartDetail cd = new CartDetail();
                    cd.setCart(cart);
                    cd.setProduct(realProduct);
                    cd.setPrice(realProduct.getPrice());
                    cd.setQuantity(quantity);
                    this.cartDetailRepository.save(cd);

                    //update table cart(row sum)
                    int s = cart.getSum() + 1;
                    cart.setSum(s);
                    this.cartRepository.save(cart);
                    session.setAttribute("sum", s);
                } else {
                    oldDetail.setQuantity(oldDetail.getQuantity() + quantity);
                    this.cartDetailRepository.save(oldDetail);
                }
            }

        }
    }

    public Cart fetchByUser(User user) {
        return this.cartRepository.findByUser(user);
    }

    public void handleRemoveCartDetail(long id, HttpSession session) {
        //mặc định của Jpa sẽ trả ra optional bọc ngoài ntn
        Optional<CartDetail> cartDetailOptional = this.cartDetailRepository.findById(id); //lấy ra cartdetail thông qua id
        //do có optional nên phải check điều kiện null
        if (cartDetailOptional.isPresent()) {
            CartDetail cartDetail = cartDetailOptional.get();

            //Từ cart-detail này, lấy ra đối tượng cart (giỏ hàng) tương ứng (2 bảng có relationship với nhau nên thằng spring tự join lại)
            Cart currentCart = cartDetail.getCart();

            //Xóa cart-detail
            this.cartDetailRepository.delete(cartDetail);

            if (currentCart.getSum() > 1) {
                currentCart.setSum(currentCart.getSum() - 1);
                session.setAttribute("sum", currentCart.getSum()); //cập nhật session luôn
                this.cartRepository.save(currentCart); // sau khi cập nhật sum thì lưu lại
            } else { //nếu sum == 1
                this.cartRepository.delete(currentCart);
                session.setAttribute("sum", 0);
            }
        }
    }

    public void handleUpdateCartBeforeCheckout(List<CartDetail> cartDetails) {
        for (CartDetail cartDetail : cartDetails) {
            Optional<CartDetail> cdOptional = this.cartDetailRepository.findById(cartDetail.getId());
            if (cdOptional.isPresent()) {
                CartDetail currentCartDetail = cdOptional.get();
                currentCartDetail.setQuantity(cartDetail.getQuantity());
                this.cartDetailRepository.save(currentCartDetail);
            }
        }
    }

    public void handlePlaceOrder(User user, HttpSession session, String receiverName, String receiverAddress, String receiverPhone) {
        //h đang cần lưu db thông tin của order_detail gồm (price, quantity, order_id, product_id)

        //create order_detail
        //step1: get cart by user
        Cart cart = this.cartRepository.findByUser(user);
        if (cart != null) {
            List<CartDetail> cartDetails = cart.getCartDetails(); //từ cart lấy ra cart_detail

            if (cartDetails != null) {
                //create order
                Order order = new Order();
                order.setUser(user);
                order.setReceiverName(receiverName);
                order.setReceiverAddress(receiverAddress);
                order.setReceiverPhone(receiverPhone);
                order.setStatus("PENDING");

                double sum = 0;
                for (CartDetail cartDetail : cartDetails) {
                    sum += cartDetail.getPrice();
                }
                order.setTotalPrice(sum);
                order = this.orderRepository.save(order); //lấy được id của order vừa lưu

                //create order_detail
                for (CartDetail cd : cartDetails) { //mỗi chi tiết giỏ hàng sẽ có 1 chi tiết đặt hàng tương ứng
                    OrderDetail orderDetail = new OrderDetail();
                    orderDetail.setOrder(order); // => đã có được order_id (gán đối tượng nhma trong db chỉ lưu id)
                    orderDetail.setProduct(cd.getProduct()); //=> đã có pro_id
                    orderDetail.setQuantity(cd.getQuantity());
                    orderDetail.setPrice(cd.getPrice());
                    this.orderDetailRepository.save(orderDetail);
                }

                //step2: delete cart v cart_detail
                for (CartDetail cd : cartDetails) {
                    this.cartDetailRepository.deleteById(cd.getId());
                }
                this.cartRepository.deleteById(cart.getId());

                //step3: update session
                session.setAttribute("sum", 0);
            }
        }
    }
}
