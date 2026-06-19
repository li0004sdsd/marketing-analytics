package com.analytics.service;

import com.analytics.annotation.SlowQueryLog;
import com.analytics.dto.ReportQuery;
import com.analytics.entity.Campaign;
import com.analytics.entity.RoiReport;
import com.analytics.entity.User;
import com.analytics.repository.CampaignRepository;
import com.analytics.repository.RoiReportRepository;
import com.analytics.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReportQueryService {
    private final RoiReportRepository roiReportRepository;
    private final UserRepository userRepository;
    private final CampaignRepository campaignRepository;

    public ReportQueryService(RoiReportRepository roiReportRepository,
                              UserRepository userRepository,
                              CampaignRepository campaignRepository) {
        this.roiReportRepository = roiReportRepository;
        this.userRepository = userRepository;
        this.campaignRepository = campaignRepository;
    }

    @SlowQueryLog(thresholdMs = 500)
    public List<RoiReport> buildQuery(ReportQuery query) {
        User currentUser = getCurrentUser();
        String role = currentUser.getRole();

        Specification<RoiReport> spec = buildBaseSpecification(query);
        spec = spec.and(applyRoleBasedFilter(currentUser, role));

        return roiReportRepository.findAll(spec);
    }

    private Specification<RoiReport> buildBaseSpecification(ReportQuery query) {
        return (root, cb, cq) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (query.getCampaignIds() != null && !query.getCampaignIds().isEmpty()) {
                predicates.add(root.get("campaignId").in(query.getCampaignIds()));
            }

            if (query.getStartDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("reportDate"), query.getStartDate()));
            }

            if (query.getEndDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("reportDate"), query.getEndDate()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Specification<RoiReport> applyRoleBasedFilter(User currentUser, String role) {
        return (root, cb, cq) -> {
            if ("ROLE_ADMIN".equals(role)) {
                return cb.conjunction();
            }

            if ("ROLE_TEAM_MANAGER".equals(role)) {
                Long teamId = currentUser.getTeamId();
                if (teamId == null) {
                    return cb.disjunction();
                }
                List<Long> teamCampaignIds = campaignRepository.findByTeamId(teamId)
                        .stream()
                        .map(Campaign::getId)
                        .collect(Collectors.toList());
                if (teamCampaignIds.isEmpty()) {
                    return cb.disjunction();
                }
                return root.get("campaignId").in(teamCampaignIds);
            }

            if ("ROLE_MEMBER".equals(role) || "ROLE_USER".equals(role)) {
                return cb.equal(root.get("createdBy"), currentUser.getId());
            }

            return cb.disjunction();
        };
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }

        String username = null;
        for (GrantedAuthority ga : authentication.getAuthorities()) {
            String authority = ga.getAuthority();
            if (authority.startsWith("ROLE_")) {
                username = authentication.getName();
                break;
            }
        }

        if (username == null) {
            username = authentication.getName();
        }

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Current user not found"));
    }
}
