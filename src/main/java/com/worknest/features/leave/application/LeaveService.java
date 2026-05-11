package com.worknest.features.leave.application;

import com.worknest.domain.enums.LeaveStatus;
import com.worknest.features.leave.dto.CreateLeaveRequestDto;
import com.worknest.features.leave.dto.LeaveBalanceDto;
import com.worknest.features.leave.dto.LeaveRequestDto;
import com.worknest.features.leave.dto.RejectLeaveRequestDto;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LeaveService {

    List<LeaveBalanceDto> getMyBalance();

    Page<LeaveRequestDto> getMyRequests(Pageable pageable);

    void submitRequest(CreateLeaveRequestDto request);

    Page<LeaveRequestDto> adminListRequests(String search, LeaveStatus status, Pageable pageable);

    LeaveRequestDto adminGetRequest(UUID requestId);

    void approveRequest(UUID requestId);

    void rejectRequest(UUID requestId, RejectLeaveRequestDto dto);
}
