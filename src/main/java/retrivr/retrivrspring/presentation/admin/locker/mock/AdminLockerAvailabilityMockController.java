package retrivr.retrivrspring.presentation.admin.locker.mock;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import retrivr.retrivrspring.global.auth.AuthOrg;
import retrivr.retrivrspring.global.auth.AuthUser;
import retrivr.retrivrspring.presentation.locker.mock.LockerMockContract.Action;
import retrivr.retrivrspring.presentation.locker.mock.LockerMockContract.AvailabilityRequest;
import retrivr.retrivrspring.presentation.locker.mock.LockerMockContract.BatchAction;
import retrivr.retrivrspring.presentation.locker.mock.LockerMockData;

@RestController
@RequestMapping("/api/admin/v1/lockers")
@Tag(name = "MOCK / 2.4 사물함 대여관리", description = LockerMockData.DESCRIPTION)
public class AdminLockerAvailabilityMockController {

  @PatchMapping("/availability")
  @Operation(summary = "[MOCK] 선택한 사물함 대여 가능/불가 처리", description = "고정 UNAVAILABLE 예시를 반환합니다.")
  public BatchAction availability(@Valid @RequestBody AvailabilityRequest request,
      @Parameter(hidden = true) @AuthOrg AuthUser authUser) {
    return new BatchAction(List.of(new Action(LockerMockData.LOCKER_ID, "UNAVAILABLE", LockerMockData.NOW)));
  }
}
