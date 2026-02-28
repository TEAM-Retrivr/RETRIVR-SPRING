package retrivr.retrivrspring.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import retrivr.retrivrspring.domain.entity.rental.ReturnEvent;

public interface ReturnEventRepository extends JpaRepository<ReturnEvent, Long> {

}
