package com.fixmate.mapper;

import com.fixmate.dto.user.UserProfileDto;
import com.fixmate.dto.user.UserSummaryDto;
import com.fixmate.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserSummaryDto toSummaryDto(User user);

    @Mapping(target = "staffCategory", expression = "java(user.getStaffProfile() != null ? user.getStaffProfile().getCategory() : null)")
    @Mapping(target = "isOnDuty", expression = "java(user.getStaffProfile() != null ? user.getStaffProfile().getIsOnDuty() : null)")
    @Mapping(target = "shiftStart", expression = "java(user.getStaffProfile() != null ? user.getStaffProfile().getShiftStart() : null)")
    @Mapping(target = "shiftEnd", expression = "java(user.getStaffProfile() != null ? user.getStaffProfile().getShiftEnd() : null)")
    @Mapping(target = "avgRating", expression = "java(user.getStaffProfile() != null ? user.getStaffProfile().getAvgRating() : null)")
    UserProfileDto toProfileDto(User user);
}
