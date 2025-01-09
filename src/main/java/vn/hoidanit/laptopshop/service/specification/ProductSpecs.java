package vn.hoidanit.laptopshop.service.specification;

import org.springframework.data.jpa.domain.Specification;
import vn.hoidanit.laptopshop.domain.Product;
import vn.hoidanit.laptopshop.domain.Product_;

import java.util.List;

// viết các tiêu chí muốn filter
public class ProductSpecs {
    //này là lọc sp (filter)
    public static Specification<Product> nameLike(String name) {
        return (root, query, criteriaBuilder)
                -> criteriaBuilder.like(root.get(Product_.NAME), "%" + name + "%");
    }

    public static Specification<Product> priceMin(double minPrice) {
        return (root, query, criteriaBuilder)
                // Hàm .gt kiểm tra xem số thứ nhất có lớn hơn số thứ hai hay không.
                // Hàm .ge kiểm tra xem số thứ nhất có lớn hơn hoặc bằng số thứ hai hay không.
                -> criteriaBuilder.ge(root.get(Product_.PRICE), minPrice);
    }

    public static Specification<Product> priceMax(double maxPrice) {
        return (root, query, criteriaBuilder)
                -> criteriaBuilder.le(root.get(Product_.PRICE), maxPrice);
    }

    public static Specification<Product> factory(String fac) {
        return (root, query, criteriaBuilder)
                -> criteriaBuilder.equal(root.get(Product_.FACTORY), fac);
    }

    public static Specification<Product> matchListFactory(List<String> fac) {
        return (root, query, criteriaBuilder)
                -> criteriaBuilder.in(root.get(Product_.FACTORY)).value(fac); //ktra xem Product_.FACTORY có nằm trong chuỗi fac được truyền vào ko
    }

    public static Specification<Product> matchListTarget(List<String> tar) {
        return (root, query, criteriaBuilder)
                -> criteriaBuilder.in(root.get(Product_.TARGET)).value(tar); //ktra xem Product_.FACTORY có nằm trong chuỗi fac được truyền vào ko
    }

    public static Specification<Product> matchPrice(double min, double max) {
        return (root, query, criteriaBuilder)
                // Hàm .gt kiểm tra xem số thứ nhất có lớn hơn số thứ hai hay không.
                // Hàm .ge kiểm tra xem số thứ nhất có lớn hơn hoặc bằng số thứ hai hay không.
                -> criteriaBuilder.and(criteriaBuilder.gt(root.get(Product_.PRICE), min), criteriaBuilder.le(root.get(Product_.PRICE), max));
    }

    public static Specification<Product> matchMultiPrice(double min, double max) {
        return (root, query, criteriaBuilder)
                -> criteriaBuilder.between(root.get(Product_.PRICE), min, max);
    }
}
