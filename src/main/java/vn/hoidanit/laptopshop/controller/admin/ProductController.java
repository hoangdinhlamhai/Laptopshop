package vn.hoidanit.laptopshop.controller.admin;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.hoidanit.laptopshop.domain.Product;
import vn.hoidanit.laptopshop.domain.User;
import vn.hoidanit.laptopshop.repository.ProductRepository;
import vn.hoidanit.laptopshop.service.ProductService;
import vn.hoidanit.laptopshop.service.UploadService;

import java.util.List;
import java.util.Optional;

@Controller
public class ProductController {
    private final ProductService productService;
    private final UploadService uploadService;

    public ProductController(ProductService productService, UploadService uploadService) {
        this.productService = productService;
        this.uploadService = uploadService;
    }

    //nếu có phân trang thì ở phần url phải có thêm ?page=...
    //nhưng ở đây chỉ có "http:localhost8080/admin/product"
    @GetMapping("/admin/product")
    public String getProductPage(Model model,
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

        //client: page
        //db: offset + limit
        //pagenumber phải là page-1 vì page bắt đầu từ 0
        Pageable pageable = PageRequest.of(page - 1, 2); //(lấy trang bao nhiêu, số ptu lấy lên)
//        List<Product> prds = this.productService.getAllProducts();
        Page<Product> prds = this.productService.getAllProducts(pageable);
        //đổi qua dạng List
        List<Product> productList = prds.getContent(); //hàm getContent nằm trong interface Page<>
        model.addAttribute("products", productList);

        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", prds.getTotalPages());

        return "admin/product/show";
    }

    @GetMapping("/admin/product/create")
    public String getCreateProductPage(Model model) {
        model.addAttribute("newProduct", new Product());
        return "admin/product/create";
    }

    @PostMapping("/admin/product/create")
    public String handleCreateProduct(@ModelAttribute("newProduct") @Valid Product pr,
                                      BindingResult newUserBindingResult,
                                      @RequestParam("hoidanitFile") MultipartFile file) {
        //validate data
        List<FieldError> errors = newUserBindingResult.getFieldErrors();
        for (FieldError error : errors) {
            System.out.println(error.getField() + " - " + error.getDefaultMessage());
        }
        //validate
        if (newUserBindingResult.hasErrors()) {
            return "admin/product/create";
        }

        //upload file vao folder
        //image đang là kiểu String => đổi tên thành: thời gian up + tên file ảnh ban đầu
        String image = this.uploadService.handleSaveUploadFile(file, "product");
        pr.setImage(image);

        //luu vao db
        this.productService.handleSaveProduct(pr);
        return "redirect:/admin/product";
    }

    @GetMapping("/admin/product/{id}")
    public String getProductById(@PathVariable long id, Model model) {
        Product product = this.productService.handleGetProductById(id);
        model.addAttribute("product", product);
        model.addAttribute("id", id);
        return "admin/product/detail";
    }

    @GetMapping("/admin/product/delete/{id}")
    public String getDeleteProductPage(Model model, @PathVariable long id) {
        model.addAttribute("newProduct", new Product());
        model.addAttribute("id", id);
        return "admin/product/delete";
    }

    @PostMapping("/admin/product/delete")
    public String postDeleteProduct(Model model, @ModelAttribute("newProduct") Product heri) {
        this.productService.handleDeleteProduct(heri.getId());
        return "redirect:/admin/product";
    }

    @GetMapping("/admin/product/update/{id}")
    public String getUpdateProductPage(Model model, @PathVariable long id) {
        model.addAttribute("newProduct", this.productService.handleGetProductById(id));
        return "admin/product/update";
    }

    @PostMapping("/admin/product/update")
    public String handleUpdateProduct(@ModelAttribute("newProduct") @Valid Product heri,
                                      BindingResult newUserBindingResult,
                                      @RequestParam("hoidanitFile") MultipartFile file) {
        //validate
        if (newUserBindingResult.hasErrors()) {
            return "admin/product/update";
        }

        Product currentProduct = this.productService.handleGetProductById(heri.getId());
        if (currentProduct != null) {
            //nếu user có ảnh thì mới update ảnh
            if (!file.isEmpty()) {
                String image = this.uploadService.handleSaveUploadFile(file, "product");
                currentProduct.setImage(image);
            }

            currentProduct.setName(heri.getName());
            currentProduct.setPrice(heri.getPrice());
            currentProduct.setQuantity(heri.getQuantity());
            currentProduct.setDetailDesc(heri.getDetailDesc());
            currentProduct.setShortDesc(heri.getShortDesc());
            currentProduct.setFactory(heri.getFactory());
            currentProduct.setTarget(heri.getTarget());

            //luu vao db
            this.productService.handleSaveProduct(currentProduct);
        }
        return "redirect:/admin/product";
    }
}
