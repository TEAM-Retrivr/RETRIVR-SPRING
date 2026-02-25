package retrivr.retrivrspring.application.service.admin.home;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.entity.rental.Rental;
import retrivr.retrivrspring.domain.entity.rental.enumerate.RentalStatus;
import retrivr.retrivrspring.domain.repository.OrganizationRepository;
import retrivr.retrivrspring.infrastructure.repository.rental.RentalRepository;
import retrivr.retrivrspring.presentation.admin.home.res.AdminHomeRequestSummary;
import retrivr.retrivrspring.presentation.admin.home.res.AdminHomeResponse;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminHomeService {

    private final OrganizationRepository organizationRepository;
    private final RentalRepository rentalRepository;

    public AdminHomeResponse getHome(Long organizationId) {

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow();

        int requestCount = rentalRepository.countByOrganization_IdAndStatus(
                organizationId,
                RentalStatus.REQUESTED
        );

        // 엔티티 조회 (fetch join)
        List<Rental> rentals = rentalRepository.findRecentHomeRentals(
                organizationId,
                RentalStatus.REQUESTED,
                PageRequest.of(0, 2)
        );

        // Service 레벨에서 DTO 매핑
        List<AdminHomeRequestSummary> recentRequests =
                rentals.stream()
                        .map(rental -> {
                            var rentalItem = rental.getRentalItems().get(0);
                            var item = rentalItem.getItem();
                            var borrower = rental.getBorrower();

                            return new AdminHomeRequestSummary(
                                    rental.getId(),
                                    item.getName(),
                                    item.getAvailableQuantity(),
                                    item.getTotalQuantity(),
                                    borrower.getName(),
                                    borrower.getMajor(),
                                    rental.getRequestedAt()
                            );
                        })
                        .toList();

        return new AdminHomeResponse(
                organization.getName(),
                organization.getProfileImageKey(), // todo: 추후 s3 연동 후 수정
                requestCount,
                recentRequests
        );
    }
}