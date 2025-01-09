package vn.hoidanit.laptopshop.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import vn.hoidanit.laptopshop.domain.User;

@Repository
// public interface UserRepository extends CrudRepository<User, Long> {
public interface UserRepository extends JpaRepository<User, Long> {
    // ghi đè lại hàm save có sẵn trong interface CrudRepository < Datatype_Object,
    // Datatype_PrimaryKey >
    User save(User hoidanit);

    void deleteById(long id);

    // List<User> findByEmailAndAddress(String email, String address);

    List<User> findOneByEmail(String email);

    List<User> findAll();

    User findById(long id);

    boolean existsByEmail(String email);

    User findByEmail(String email);

    //ko cần khai báo vì hàm findAll có tham số này cũng có sẵn, truyền tham số vào sẽ tự hiểu
//    Page<User> findAll(Pageable pageable);
}
