package com.quertimizer.user.adapter.in.http;

import com.quertimizer.user.application.input.BlockedAccountPageInput;
import com.quertimizer.user.application.input.UserAnomalyTrendSearchInput;
import com.quertimizer.user.application.port.in.BlockUserUseCase;
import com.quertimizer.user.application.port.in.GetBlockedIpsUseCase;
import com.quertimizer.user.application.port.in.GetBlockedUsersUseCase;
import com.quertimizer.user.application.port.in.GetUserAnomalyTrendsUseCase;
import com.quertimizer.user.application.port.in.UnblockIpUseCase;
import com.quertimizer.user.application.port.in.UnblockUserUseCase;
import com.quertimizer.user.adapter.in.http.response.BlockedIpPageRes;
import com.quertimizer.user.adapter.in.http.response.BlockedUserPageRes;
import com.quertimizer.user.adapter.in.http.response.UserAnomalyTrendPageRes;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserAnomalyController {

    private final GetUserAnomalyTrendsUseCase getUserAnomalyTrends;
    private final GetBlockedUsersUseCase getBlockedUsers;
    private final GetBlockedIpsUseCase getBlockedIps;
    private final BlockUserUseCase blockUser;
    private final UnblockUserUseCase unblockUser;
    private final UnblockIpUseCase unblockIp;

    /**
     * 관리자 이상 제출 추세 페이지를 반환한다.
     *
     * <ol>
     *   <li>이상 제출 추세 검색 입력 생성
     *   <li>이상 제출 추세 페이지 응답 생성
     * </ol>
     *
     * @param range 조회 시간 범위
     * @param startedAt 직접 지정한 조회 시작 시각
     * @param endedAt 직접 지정한 조회 종료 시각
     * @param page 요청 페이지 번호
     * @param pageSize 요청 페이지 크기
     */
    @GetMapping("/admin/anomaly-accounts/trends")
    public ResponseEntity<UserAnomalyTrendPageRes> getAnomalyTrends(@RequestParam(defaultValue = "10m") String range,
                                                                    @RequestParam(required = false) String startedAt,
                                                                    @RequestParam(required = false) String endedAt,
                                                                    @RequestParam(defaultValue = "1") int page,
                                                                    @RequestParam(required = false) Integer pageSize) {
        UserAnomalyTrendSearchInput input = new UserAnomalyTrendSearchInput(range, startedAt, endedAt, page, pageSize);

        return ResponseEntity.ok(UserAnomalyTrendPageRes.from(
                getUserAnomalyTrends.execute(input)
        ));
    }

    /**
     * 관리자 차단 사용자 페이지를 반환한다.
     *
     * @param page 요청 페이지 번호
     * @param pageSize 요청 페이지 크기
     */
    @GetMapping("/admin/anomaly-accounts/blocked-users")
    public ResponseEntity<BlockedUserPageRes> getBlockedUsers(@RequestParam(defaultValue = "1") int page,
                                                              @RequestParam(required = false) Integer pageSize) {
        return ResponseEntity.ok(BlockedUserPageRes.from(
                getBlockedUsers.execute(new BlockedAccountPageInput(page, pageSize))
        ));
    }

    /**
     * 관리자 차단 IP 페이지를 반환한다.
     *
     * @param page 요청 페이지 번호
     * @param pageSize 요청 페이지 크기
     */
    @GetMapping("/admin/anomaly-accounts/blocked-ips")
    public ResponseEntity<BlockedIpPageRes> getBlockedIps(@RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(required = false) Integer pageSize) {
        return ResponseEntity.ok(BlockedIpPageRes.from(
                getBlockedIps.execute(new BlockedAccountPageInput(page, pageSize))
        ));
    }

    /**
     * 관리자가 사용자를 차단한다.
     *
     * @param handle 차단할 사용자 handle
     */
    @PostMapping("/admin/anomaly-accounts/users/{handle}/block")
    public ResponseEntity<Void> blockUser(@PathVariable String handle) {
        blockUser.execute(handle);
        return ResponseEntity.noContent().build();
    }

    /**
     * 관리자가 사용자 차단을 해제한다.
     *
     * @param handle 차단 해제할 사용자 handle
     */
    @DeleteMapping("/admin/anomaly-accounts/users/{handle}/block")
    public ResponseEntity<Void> unblockUser(@PathVariable String handle) {
        unblockUser.execute(handle);
        return ResponseEntity.noContent().build();
    }

    /**
     * 관리자가 IP 차단을 해제한다.
     *
     * @param ipAddress 차단 해제할 IP 주소
     */
    @DeleteMapping("/admin/anomaly-accounts/ips/{ipAddress}/block")
    public ResponseEntity<Void> unblockIp(@PathVariable String ipAddress) {
        unblockIp.execute(ipAddress);
        return ResponseEntity.noContent().build();
    }
}
