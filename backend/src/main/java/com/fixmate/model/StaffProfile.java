package com.fixmate.model;

import com.fixmate.model.enums.StaffCategory;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "staff_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StaffCategory category;

    @Column(name = "is_on_duty", nullable = false)
    @Builder.Default
    private Boolean isOnDuty = true;

    @Column(name = "shift_start")
    private LocalTime shiftStart;

    @Column(name = "shift_end")
    private LocalTime shiftEnd;

    @Column(name = "avg_rating")
    @Builder.Default
    private Double avgRating = 0.0;

    @Column(name = "last_assigned_at")
    private java.time.LocalDateTime lastAssignedAt;
}
